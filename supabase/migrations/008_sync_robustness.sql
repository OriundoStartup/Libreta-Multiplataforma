-- ============================================================
-- 🛠 MIGRACIÓN 008: Robustez de Sincronización (updated_at + Triggers)
-- OBJETIVO:
--   Añadir columnas de metadata faltantes para PULL incremental
--   y asegurar que updated_at se actualice automáticamente.
-- ============================================================

-- ── 1. Función Helper para updated_at ────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ── 2. Actualizar Tabla: profiles ───────────────────────────────────────────
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'profiles' AND column_name = 'updated_at') THEN
        ALTER TABLE public.profiles ADD COLUMN updated_at TIMESTAMPTZ DEFAULT now();
    END IF;
END $$;

DROP TRIGGER IF EXISTS tr_profiles_updated_at ON public.profiles;
CREATE TRIGGER tr_profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ── 3. Actualizar Tabla: courses ────────────────────────────────────────────
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'courses' AND column_name = 'updated_at') THEN
        ALTER TABLE public.courses ADD COLUMN updated_at TIMESTAMPTZ DEFAULT now();
    END IF;
END $$;

DROP TRIGGER IF EXISTS tr_courses_updated_at ON public.courses;
CREATE TRIGGER tr_courses_updated_at
    BEFORE UPDATE ON public.courses
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ── 4. Actualizar Tabla: students ───────────────────────────────────────────
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'students' AND column_name = 'updated_at') THEN
        ALTER TABLE public.students ADD COLUMN updated_at TIMESTAMPTZ DEFAULT now();
    END IF;
END $$;

DROP TRIGGER IF EXISTS tr_students_updated_at ON public.students;
CREATE TRIGGER tr_students_updated_at
    BEFORE UPDATE ON public.students
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ── 5. Actualizar Tabla: attendance ─────────────────────────────────────────
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'attendance' AND column_name = 'updated_at') THEN
        ALTER TABLE public.attendance ADD COLUMN updated_at TIMESTAMPTZ DEFAULT now();
    END IF;
END $$;

DROP TRIGGER IF EXISTS tr_attendance_updated_at ON public.attendance;
CREATE TRIGGER tr_attendance_updated_at
    BEFORE UPDATE ON public.attendance
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ── 6. Actualizar Tabla: justifications ─────────────────────────────────────
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'justifications' AND column_name = 'updated_at') THEN
        ALTER TABLE public.justifications ADD COLUMN updated_at TIMESTAMPTZ DEFAULT now();
    END IF;
END $$;

DROP TRIGGER IF EXISTS tr_justifications_updated_at ON public.justifications;
CREATE TRIGGER tr_justifications_updated_at
    BEFORE UPDATE ON public.justifications
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ── 7. Actualizar Tabla: grades ──────────────────────────────────────────────
-- Nota: La tabla grades podría no existir aún o llamarse distinto en algunos schemas.
-- Aplicamos el mismo patrón por seguridad.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'grades') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'grades' AND column_name = 'updated_at') THEN
            ALTER TABLE public.grades ADD COLUMN updated_at TIMESTAMPTZ DEFAULT now();
        END IF;

        EXECUTE 'DROP TRIGGER IF EXISTS tr_grades_updated_at ON public.grades;';
        EXECUTE 'CREATE TRIGGER tr_grades_updated_at BEFORE UPDATE ON public.grades FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();';
    END IF;
END $$;

-- ── FIN MIGRACIÓN 008 ────────────────────────────────────────────────────────
