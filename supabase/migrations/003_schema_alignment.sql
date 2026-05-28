-- ============================================================
-- 🧬 MIGRACIÓN 003: Schema Alignment (FASE 1)
-- Fecha objetivo: 2026-06-XX
-- Autor: master-dev
--
-- OBJETIVO:
--   Cerrar el drift entre DTOs Kotlin y el schema real post-002.
--   ADITIVA Y BACKWARD-COMPATIBLE — la app live sigue funcionando
--   mientras el frontend se migra a los nombres correctos.
--
-- ESTRATEGIA:
--   1. Columnas generadas (GENERATED ALWAYS AS) para nombres "content" / "message_text".
--   2. Vista students_legacy para que código viejo siga leyendo first_name/last_name.
--   3. Funciones helper para migrar enrollments → students gradualmente.
--   4. NUNCA DROP de columnas vivas — eso se hace en migración futura post-validación.
--
-- ROLLBACK:
--   Las columnas GENERATED y VIEWS son seguras de dropear si algo falla.
-- ============================================================

-- ── 1. messages / communications: alias "content" ────────────────────────────
-- Los DTOs Kotlin usan `content` pero la BD usa `message_text`.
-- Solución: columna generada que duplica el valor. Lecturas y writes pasan
-- por message_text (la generada no se puede escribir), por lo que el frontend
-- nuevo debe escribir a message_text directamente.
--
-- TODO[FASE-1]:
--   Decidir si renombrar message_text → content (breaking) o mantener generated.
--   Si se renombra: migración 003b con UPDATE de TODOS los DTOs en un solo PR.

-- (Implementación pendiente — definir estrategia con el equipo)
-- ALTER TABLE messages       ADD COLUMN IF NOT EXISTS content TEXT GENERATED ALWAYS AS (message_text) STORED;
-- ALTER TABLE communications ADD COLUMN IF NOT EXISTS content TEXT GENERATED ALWAYS AS (message_text) STORED;


-- ── 2. students: vista de compatibilidad con first_name/last_name/class_id ───
-- Mantiene el código legacy de StudentSupabaseDto operativo durante la migración.
CREATE OR REPLACE VIEW public.students_legacy AS
SELECT
    s.id,
    split_part(s.full_name, ' ', 1)                                AS first_name,
    NULLIF(substring(s.full_name FROM position(' ' in s.full_name) + 1), '') AS last_name,
    s.course_id                                                    AS class_id,
    s.parent_id,
    s.student_rut,
    s.created_at
FROM public.students s;

COMMENT ON VIEW public.students_legacy IS
    'FASE-1 compatibility view. Frontend Kotlin nuevo debe leer/escribir directamente sobre public.students.';

-- ── 3. RLS para la vista (hereda de students automáticamente en PG, pero documentamos)
-- TODO[FASE-1]: validar en staging que SELECT sobre students_legacy respeta las policies de students.


-- ── 4. attendance.course_id: NO añadir de vuelta ─────────────────────────────
-- La migración 002 eliminó correctamente attendance.course_id (3NF).
-- El frontend debe parar de mandarlo en sus DTOs (ver SupabaseDtosV2.kt).
-- Si el frontend manda course_id en un upsert, PostgREST lo rechaza con 400 PGRST204.
--
-- TODO[FASE-1]: añadir TRIGGER que strip-ee course_id si llega en INSERT/UPDATE,
-- como red de seguridad durante la transición. Eliminar después de migrar todos los DTOs.


-- ── 5. courses.school_name: idem, NO restaurar ───────────────────────────────
-- TODO[FASE-1]: idem strip-trigger temporal si es necesario.


-- ── 6. invitation_codes: documentar campos opcionales del DTO ────────────────
-- InvitationCodeSupabaseDto trae `course_id` y `target_role` que no están en BD.
-- Decisión: ¿se añaden o se quitan del DTO?
--
-- TODO[FASE-1]: decidir.
--   Opción A (recomendada): quitar del DTO porque hoy no se usan.
--   Opción B: añadir las columnas en BD si el equipo va a usarlas en próximos sprints.


-- ── 7. course_assignments.is_head_teacher: validar que esté en el DTO ────────
-- BD lo tiene NOT NULL DEFAULT false; el DTO CourseAssignmentSupabaseDto no lo trae.
-- Como tiene default, los inserts del frontend funcionan, pero el frontend nunca
-- sabe si un profesor es jefe de curso. Se corrige en FASE-1 al regenerar el DTO.


-- ── 8. Función de auditoría: contar drift activo ─────────────────────────────
-- Cuenta cuántas filas tienen valores extraños (ej. courseId NULL donde debería
-- haber valor). Sirve para medir progreso de la migración.
CREATE OR REPLACE FUNCTION public.audit_schema_drift()
RETURNS TABLE(metric TEXT, value BIGINT)
LANGUAGE plpgsql STABLE SECURITY DEFINER AS $$
BEGIN
    -- TODO[FASE-1]: rellenar con métricas reales relevantes
    RETURN QUERY SELECT 'students_without_full_name'::TEXT, COUNT(*) FROM students WHERE full_name IS NULL OR trim(full_name) = '';
    RETURN QUERY SELECT 'enrollments_without_student'::TEXT, COUNT(*) FROM enrollments WHERE student_id IS NULL;
    -- ... añadir más a medida que se descubran inconsistencias
END $$;

GRANT EXECUTE ON FUNCTION public.audit_schema_drift TO authenticated;

-- ── FIN MIGRACIÓN 003 ────────────────────────────────────────────────────────
-- Próxima migración (004): sync_metadata + server_version + deleted_at
