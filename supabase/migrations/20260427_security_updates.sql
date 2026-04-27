-- ==========================================
-- 🛡️ SCRIPTS DE SEGURIDAD Y ATOMICIDAD (Supabase)
-- Ejecutar estos scripts en el SQL Editor de tu proyecto
-- ==========================================

-- 1. CREACIÓN AUTOMÁTICA DE PERFILES
-- Asegura que cada usuario de Auth tenga una fila en public.profiles
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
BEGIN
  INSERT INTO public.profiles (id, full_name, role, created_at)
  VALUES (
    new.id, 
    COALESCE(new.raw_user_meta_data->>'full_name', 'Usuario Nuevo'), 
    'PARENT', -- Rol por defecto
    NOW()
  )
  ON CONFLICT (id) DO NOTHING;
  RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();


-- 2. PROTECCIÓN DE ROLES
-- Impide que un usuario malintencionado cambie su rol de Apoderado a Profesor vía API
CREATE OR REPLACE FUNCTION public.protect_role_change()
RETURNS trigger AS $$
BEGIN
  -- Si el rol ya está definido (no es nulo) y se intenta cambiar, lanzar error
  IF OLD.role IS NOT NULL AND OLD.role <> NEW.role THEN
    RAISE EXCEPTION 'SEGURIDAD: No se permite cambiar el rol una vez asignado.';
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_protect_role ON public.profiles;
CREATE TRIGGER tr_protect_role
  BEFORE UPDATE OF role ON public.profiles
  FOR EACH ROW EXECUTE FUNCTION public.protect_role_change();


-- 3. FUNCIÓN RPC PARA RECLAMO ATÓMICO DE INVITACIONES
-- Evita condiciones de carrera y asegura que el código sea válido
CREATE OR REPLACE FUNCTION public.claim_course_invitation(p_code text, p_user_id uuid)
RETURNS json AS $$
DECLARE
    v_course_id uuid;
    v_school_name text;
BEGIN
    -- Buscar el curso asociado al código
    SELECT id, school_name INTO v_course_id, v_school_name
    FROM public.courses
    WHERE UPPER(invite_code) = UPPER(p_code) AND is_active = true
    LIMIT 1;

    IF v_course_id IS NULL THEN
        RETURN json_build_object('success', false, 'message', 'Código de invitación inválido o curso inactivo');
    END IF;

    -- Actualizar el perfil del usuario con el course_id (si es apoderado)
    UPDATE public.profiles 
    SET course_id = v_course_id 
    WHERE id = p_user_id AND (role = 'PARENT' OR role IS NULL);

    RETURN json_build_object(
        'success', true, 
        'course_id', v_course_id,
        'school_name', v_school_name
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
