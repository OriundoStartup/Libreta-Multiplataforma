-- ============================================================
-- MIGRACIÓN 006: Alineación de producción (rol + RPC muerta)
-- Fecha: 2026-06-20
-- Autor: senior-dev
--
-- OBJETIVO:
--   Resolver dos landmines de producción detectados en la auditoría de
--   la simbiosis app ↔ Supabase:
--
--   1. CONFLICTO DE TRIGGER handle_new_user:
--      - 001/002 insertan profiles.role = 'TEACHER' al registrarse.
--      - 20260427 lo cambió a 'PARENT'.
--      Combinado con el trigger protect_role_change (bloquea cualquier
--      cambio de rol una vez asignado), un usuario nuevo queda CLAVADO en
--      el rol que el trigger le puso y la pantalla de selección de rol
--      (RoleSelectionScreen → updateRole) FALLA silenciosamente si el
--      usuario elige el otro rol.
--      FIX: el trigger inserta role = NULL. El rol lo decide el usuario UNA
--      vez en la app; protect_role_change permite NULL→valor y bloquea
--      valor→valor-distinto (anti escalada de privilegios). Es exactamente
--      el comportamiento que la UI espera.
--
--   2. RPC claim_course_invitation() MUERTA:
--      Referencia courses.school_name y profiles.course_id, AMBAS eliminadas
--      en 002_normalize_3nf. Cualquier llamada revienta. El flujo vigente es
--      enrollments + claim_invitation_code(). Se elimina para quitar el filo.
--
-- IDEMPOTENTE Y SEGURA: solo redefine funciones y dropea una RPC muerta.
--   No toca datos existentes (el trigger solo afecta INSERTs nuevos).
-- ============================================================

-- ── 1. handle_new_user → role NULL (deja que la app elija) ───────────────────
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    INSERT INTO public.profiles (id, full_name, email, role)
    VALUES (
        NEW.id,
        COALESCE(
            NEW.raw_user_meta_data->>'full_name',
            NEW.raw_user_meta_data->>'name',
            split_part(NEW.email, '@', 1)
        ),
        NEW.email,
        NULL   -- ← el rol se asigna en la app (RoleSelectionScreen), no por defecto
    )
    ON CONFLICT (id) DO UPDATE
        SET email = EXCLUDED.email
        WHERE profiles.email IS NULL;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ── 2. protect_role_change: permitir NULL→valor, bloquear valor→otro ─────────
-- (Re-afirmamos la definición correcta; ya existía desde 20260427.)
CREATE OR REPLACE FUNCTION public.protect_role_change()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    -- Solo se bloquea si YA había un rol y se intenta cambiar a uno distinto.
    -- NULL → 'PARENT'/'TEACHER' está permitido (asignación inicial en la app).
    IF OLD.role IS NOT NULL AND OLD.role <> NEW.role THEN
        RAISE EXCEPTION 'SEGURIDAD: No se permite cambiar el rol una vez asignado.';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS tr_protect_role ON public.profiles;
CREATE TRIGGER tr_protect_role
    BEFORE UPDATE OF role ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.protect_role_change();

-- ── 3. Eliminar RPC muerta claim_course_invitation ──────────────────────────
-- Referencia columnas eliminadas en 002 (courses.school_name, profiles.course_id).
-- El flujo vigente: enrollStudent() inserta en enrollments + trigger
-- enrollment_to_student() crea el student; claim_invitation_code() para códigos.
DROP FUNCTION IF EXISTS public.claim_course_invitation(text, uuid);

-- ── 4. Verificación post-migración (ejecutar y revisar a mano) ───────────────
-- Debe devolver role = NULL en el cuerpo de la función:
--   SELECT pg_get_functiondef('public.handle_new_user()'::regprocedure);
-- Debe devolver 0 filas (función eliminada):
--   SELECT 1 FROM pg_proc WHERE proname = 'claim_course_invitation';

-- ── FIN MIGRACIÓN 006 ────────────────────────────────────────────────────────
