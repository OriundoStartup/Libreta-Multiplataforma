-- ============================================================
-- 🛡️ MIGRACIÓN DE SEGURIDAD Y ENDURECIMIENTO DE PRODUCCIÓN
-- Fecha: 2026-05-01
-- Objetivo: Asegurar datos sensibles y configurar almacenamiento privado.
-- ============================================================

-- ── 1. Almacenamiento Privado (Storage) ──────────────────────────────────────

-- Asegurar que el bucket 'justifications' sea PRIVADO
-- Esto impide el acceso vía URL pública. Requiere Signed URLs o RLS de Storage.
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types) 
VALUES (
    'justifications', 
    'justifications', 
    false, 
    5242880, -- Límite de 5MB por archivo
    '{image/*,application/pdf}' -- Solo imágenes y PDFs
)
ON CONFLICT (id) DO UPDATE SET 
    public = false,
    file_size_limit = 5242880,
    allowed_mime_types = '{image/*,application/pdf}';

-- Políticas de RLS para Storage (Bucket: justifications)
-- Permitir a los padres subir archivos (INSERT)
CREATE POLICY "Apoderados pueden subir justificativos"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'justifications' 
    AND (storage.foldername(name))[1] = auth.uid()::text
);

-- Permitir a los profesores y al dueño leer archivos (SELECT)
CREATE POLICY "Profesores y dueños pueden leer justificativos"
ON storage.objects FOR SELECT
TO authenticated
USING (
    bucket_id = 'justifications'
    AND (
        -- Es el dueño del archivo
        auth.uid()::text = (storage.foldername(name))[1]
        OR 
        -- Es un profesor (simplificado: cualquier profesor por ahora, 
        -- idealmente filtrar por curso del alumno)
        EXISTS (
            SELECT 1 FROM public.profiles 
            WHERE id = auth.uid() AND role = 'TEACHER'
        )
    )
);

-- ── 2. Endurecimiento de Tablas (Public Schema) ──────────────────────────────

-- Asegurar que la tabla justifications tenga la columna document_path (o document_url)
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'justifications' AND column_name = 'document_url') THEN
        ALTER TABLE public.justifications ADD COLUMN document_url TEXT;
    END IF;
END $$;

-- RLS para perfiles: nadie puede leer perfiles ajenos excepto profesores
DROP POLICY IF EXISTS "profiles_self_read" ON public.profiles;
CREATE POLICY "profiles_read_policy" ON public.profiles
FOR SELECT TO authenticated
USING (
    id = auth.uid() 
    OR role = 'TEACHER' 
    OR EXISTS (
        SELECT 1 FROM public.courses 
        WHERE teacher_id = auth.uid()
    )
);

-- ── 3. Ofuscación de Datos en Vistas (Opcional pero recomendado) ─────────────
-- Nota: Esto se maneja mejor en el Frontend, pero aquí aseguramos que el RUT 
-- no sea el identificador primario de búsqueda pública.

COMMENT ON COLUMN public.students.student_rut IS 'Dato sensible: Solo accesible vía RLS por apoderado o docente.';

-- ── 4. Limpieza de Invitaciones Expiradas ────────────────────────────────────
-- Función para que el sistema mantenga la higiene de la BD
CREATE OR REPLACE FUNCTION public.cleanup_expired_invitations()
RETURNS void AS $$
BEGIN
    DELETE FROM public.invitation_codes WHERE expires_at < NOW();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
