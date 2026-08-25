-- MIGRACIÓN 010: Corrección de Trigger de Inscripción y Backfill de Datos Huérfanos.

-- 1. Función de Gatillo Corregida
CREATE OR REPLACE FUNCTION public.handle_new_enrollment()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.students (id, full_name, student_rut, course_id, parent_id)
    VALUES (
        gen_random_uuid(),
        NEW.student_name,
        NEW.student_rut,
        NEW.course_id,
        NEW.parent_id
    )
    ON CONFLICT (id) DO UPDATE
    SET parent_id = EXCLUDED.parent_id,
        course_id = EXCLUDED.course_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 2. Trigger
DROP TRIGGER IF EXISTS tr_on_enrollment ON public.enrollments;
CREATE TRIGGER tr_on_enrollment
AFTER INSERT ON public.enrollments
FOR EACH ROW EXECUTE FUNCTION public.handle_new_enrollment();

-- 3. Backfill Manual
INSERT INTO public.students (id, full_name, student_rut, course_id, parent_id)
SELECT gen_random_uuid(), student_name, student_rut, course_id, parent_id
FROM public.enrollments
WHERE parent_id = '5e28c41d-5da3-4606-9d4b-7246e40ed70c'
ON CONFLICT (id) DO NOTHING;
