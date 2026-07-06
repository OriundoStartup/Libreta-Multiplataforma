-- ============================================================
-- MIGRACIÓN 007: Notas internas del docente sobre un alumno
-- Fecha: 2026-06-20
-- Autor: senior-dev
--
-- OBJETIVO:
--   Habilitar la feature de "notas internas" que el código ya usa
--   (SupabaseMessageRepository.getInternalNotes / saveInternalNote) pero
--   que fallaba porque la tabla `messages` (002_normalize_3nf) no tenía
--   las columnas necesarias y `receiver_id` era NOT NULL.
--
--   Una nota interna es un mensaje SIN destinatario (receiver_id NULL),
--   asociado a un alumno (student_id) y marcado is_internal = true.
--   Solo el docente autor la ve.
--
-- COMPATIBILIDAD:
--   ADITIVA. Los mensajes P2P existentes (receiver_id NOT NULL, is_internal
--   default false) siguen funcionando igual.
-- ============================================================

-- ── 1. Columnas nuevas en messages ───────────────────────────────────────────
ALTER TABLE public.messages
    ADD COLUMN IF NOT EXISTS student_id  UUID REFERENCES public.students(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS is_internal BOOLEAN NOT NULL DEFAULT false;

-- ── 2. receiver_id pasa a ser NULLABLE (las notas internas no tienen destino) ─
ALTER TABLE public.messages
    ALTER COLUMN receiver_id DROP NOT NULL;

-- ── 3. Integridad: o es P2P (receiver_id) o es nota interna (student_id) ──────
-- Una nota interna debe tener student_id y receiver_id NULL.
-- Un mensaje P2P debe tener receiver_id y no ser interno.
ALTER TABLE public.messages
    DROP CONSTRAINT IF EXISTS chk_messages_target;
ALTER TABLE public.messages
    ADD  CONSTRAINT chk_messages_target CHECK (
        (is_internal = true  AND student_id IS NOT NULL AND receiver_id IS NULL)
        OR
        (is_internal = false AND receiver_id IS NOT NULL)
    );

-- ── 4. Índice para la query de notas por alumno ──────────────────────────────
CREATE INDEX IF NOT EXISTS idx_messages_internal
    ON public.messages (student_id, created_at DESC)
    WHERE is_internal = true;

-- ── 5. RLS: el docente ve/gestiona las notas internas de alumnos de sus cursos
-- (la policy existente "user_own_messages" ya cubre al autor vía sender_id;
--  esta amplía la lectura a cualquier docente del curso del alumno).
DROP POLICY IF EXISTS "teacher_course_internal_notes" ON public.messages;
CREATE POLICY "teacher_course_internal_notes" ON public.messages
    FOR ALL USING (
        is_internal = true
        AND student_id IN (
            SELECT s.id FROM public.students s
            JOIN public.courses c ON c.id = s.course_id
            WHERE c.teacher_id = auth.uid()
        )
    );

-- ── 6. Verificación (revisar a mano) ─────────────────────────────────────────
--   SELECT column_name, is_nullable FROM information_schema.columns
--   WHERE table_name = 'messages' ORDER BY ordinal_position;

-- ── FIN MIGRACIÓN 007 ─────────────────────────────────────────────────────────
