-- ============================================================
-- 🔐 MIGRACIÓN 009: Corrección de RLS para Students
-- OBJETIVO: Permitir que los apoderados vean a sus alumnos vinculados.
-- ============================================================

-- 1. Habilitar RLS (por si no lo estuviera)
ALTER TABLE public.students ENABLE ROW LEVEL SECURITY;

-- 2. Limpiar políticas antiguas que podrían estar filtrando erróneamente
DROP POLICY IF EXISTS "Users can view their own student data" ON public.students;
DROP POLICY IF EXISTS "Parents can view their students" ON public.students;
DROP POLICY IF EXISTS "Teachers can view students in their courses" ON public.students;
DROP POLICY IF EXISTS "Individuals can view their own student record" ON public.students;

-- 3. Política para Apoderados: Pueden ver alumnos donde sean el parent_id
CREATE POLICY "Parents can view their students"
ON public.students
FOR SELECT
TO authenticated
USING (parent_id = auth.uid());

-- 4. Política para Profesores: Pueden ver alumnos de sus cursos
-- (Asumiendo que existe la tabla courses con teacher_id)
CREATE POLICY "Teachers can view their students"
ON public.students
FOR SELECT
TO authenticated
USING (
  course_id IN (
    SELECT id FROM public.courses WHERE teacher_id = auth.uid()
  )
);

-- 5. Política para Mensajería/Sync: Permitir UPSERT si eres el dueño
CREATE POLICY "Parents can update their students"
ON public.students
FOR UPDATE
TO authenticated
USING (parent_id = auth.uid())
WITH CHECK (parent_id = auth.uid());

-- 6. Garantizar permisos de lectura
GRANT SELECT ON public.students TO authenticated;

-- Fin de migración
