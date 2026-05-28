-- ============================================================
-- 🔗 MIGRACIÓN 005: Unificación enrollments ↔ students (FASE 4)
-- Fecha objetivo: 2026-08-XX
--
-- OBJETIVO:
--   Hoy enrollments y students duplican datos de alumno. El frontend lee
--   alumnos desde `enrollments` (vía SupabaseStudentRepository.getStudentsByClass)
--   y esto causa drift de IDs y nombres.
--
-- ESTRATEGIA:
--   1. Trigger ON INSERT ON enrollments que cree students automáticamente
--      si no existe.
--   2. Backfill: para enrollments existentes sin students.id → crear y linkear.
--   3. Después de 4 semanas en producción con métricas estables, deprecar
--      lectura desde enrollments (queda solo como log de "quién inscribió a quién").
--
-- COMPATIBILIDAD:
--   El trigger no rompe inserts existentes. El backfill solo añade filas.
--   La lectura desde enrollments sigue funcionando.
-- ============================================================

-- ── 1. Trigger de auto-creación de student ───────────────────────────────────
CREATE OR REPLACE FUNCTION public.enrollment_to_student()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_student_id UUID;
BEGIN
    -- Si el enrollment ya tiene student_id linkeado, no hacemos nada
    IF NEW.student_id IS NOT NULL THEN
        RETURN NEW;
    END IF;

    -- Buscar si ya existe un student con el mismo nombre en el mismo curso
    SELECT id INTO v_student_id
    FROM   students
    WHERE  course_id = NEW.course_id
      AND  full_name = NEW.student_name
    LIMIT  1;

    -- Si no existe, lo creamos
    IF v_student_id IS NULL THEN
        INSERT INTO students (full_name, student_rut, course_id, parent_id)
        VALUES (NEW.student_name, NEW.student_rut, NEW.course_id, NEW.parent_id)
        RETURNING id INTO v_student_id;
    END IF;

    -- Linkeamos el enrollment con el student creado/encontrado
    NEW.student_id := v_student_id;
    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS trg_enrollment_to_student ON enrollments;
CREATE TRIGGER trg_enrollment_to_student
    BEFORE INSERT ON enrollments
    FOR EACH ROW EXECUTE FUNCTION public.enrollment_to_student();


-- ── 2. Backfill de enrollments huérfanos ─────────────────────────────────────
-- TODO[FASE-4]: ejecutar manualmente en staging primero, validar conteos:
--   SELECT COUNT(*) FROM enrollments WHERE student_id IS NULL;
--   (debería ser 0 después del UPDATE)

-- UPDATE enrollments e
-- SET    student_id = s.id
-- FROM   students s
-- WHERE  e.student_id IS NULL
--   AND  s.course_id = e.course_id
--   AND  s.full_name = e.student_name;
--
-- INSERT INTO students (full_name, student_rut, course_id, parent_id)
-- SELECT DISTINCT e.student_name, e.student_rut, e.course_id, e.parent_id
-- FROM   enrollments e
-- WHERE  e.student_id IS NULL
--   AND  NOT EXISTS (
--     SELECT 1 FROM students s
--     WHERE  s.course_id = e.course_id AND s.full_name = e.student_name
--   );
--
-- UPDATE enrollments e
-- SET    student_id = s.id
-- FROM   students s
-- WHERE  e.student_id IS NULL
--   AND  s.course_id = e.course_id
--   AND  s.full_name = e.student_name;


-- ── 3. Documentar deprecación de lecturas en enrollments ─────────────────────
COMMENT ON TABLE enrollments IS
    'FASE-4: log inmutable de inscripciones. Para listar alumnos de un curso, usar tabla students. enrollments queda como auditoría histórica.';

-- ── FIN MIGRACIÓN 005 ────────────────────────────────────────────────────────
