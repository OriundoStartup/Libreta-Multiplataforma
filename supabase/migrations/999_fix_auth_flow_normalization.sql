-- ============================================================
-- 🛠️ MIGRACIÓN DE NORMALIZACIÓN Y REPARACIÓN DE FLUJO AUTH
-- Fecha: 2026-08-06
-- Objetivo: Resolver conflicto de triggers y sincronizar con 3NF.
-- ============================================================

-- 1. UNIFICAR CREACIÓN DE PERFIL (handle_new_user)
-- Seteamos role = NULL para permitir que la App gestione la selección.
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
BEGIN
  INSERT INTO public.profiles (id, full_name, email, role, created_at)
  VALUES (
    new.id,
    COALESCE(
        new.raw_user_meta_data->>'full_name',
        new.raw_user_meta_data->>'name',
        split_part(new.email, '@', 1)
    ),
    new.email,
    NULL, -- ROL INICIAL NULO
    NOW()
  )
  ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    full_name = COALESCE(profiles.full_name, EXCLUDED.full_name);
  RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Re-aplicar trigger
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();


-- 2. AJUSTAR PROTECCIÓN DE ROL (protect_role_change)
-- Permite el cambio SI Y SOLO SI el rol actual es NULL.
CREATE OR REPLACE FUNCTION public.protect_role_change()
RETURNS trigger AS $$
BEGIN
  IF OLD.role IS NOT NULL AND OLD.role <> NEW.role THEN
    RAISE EXCEPTION 'SEGURIDAD: No se permite cambiar el rol una vez asignado.';
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- 3. REPARAR RPC DE INVITACIONES (claim_course_invitation)
-- Eliminamos referencia a profiles.course_id (que no existe en 3NF).
-- Ahora esta función solo valida y retorna el curso; la vinculación se hace vía enrollments.
CREATE OR REPLACE FUNCTION public.claim_course_invitation(p_code text, p_user_id uuid)
RETURNS json AS $$
DECLARE
    v_course_id uuid;
BEGIN
    -- Buscar el curso asociado al código
    SELECT id INTO v_course_id
    FROM public.courses
    WHERE UPPER(invite_code) = UPPER(p_code) AND is_active = true
    LIMIT 1;

    IF v_course_id IS NULL THEN
        RETURN json_build_object('success', false, 'message', 'Código de invitación inválido o curso inactivo');
    END IF;

    -- En 3NF no actualizamos profiles.course_id.
    -- Retornamos el ID para que el cliente proceda con la inscripción (enrollment).
    RETURN json_build_object(
        'success', true,
        'course_id', v_course_id
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- 4. POLÍTICAS DE RLS PARA UPDATE
-- Aseguramos que el usuario pueda actualizar su propio perfil (para setear el rol inicial).
DROP POLICY IF EXISTS "profiles_self_update" ON public.profiles;
CREATE POLICY "profiles_self_update" ON public.profiles
FOR UPDATE TO authenticated
USING (id = auth.uid())
WITH CHECK (id = auth.uid());

-- Asegurar lectura de perfil propio
DROP POLICY IF EXISTS "profiles_self_read" ON public.profiles;
CREATE POLICY "profiles_self_read" ON public.profiles
FOR SELECT TO authenticated
USING (id = auth.uid());

-- 5. PERMISOS
GRANT ALL ON public.profiles TO authenticated;
GRANT ALL ON public.profiles TO service_role;
