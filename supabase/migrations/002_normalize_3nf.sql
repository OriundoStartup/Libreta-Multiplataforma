-- ============================================================
-- Migration 002: Normalización 3NF + Índices + RLS completo
-- Ejecutar en Supabase → SQL Editor
-- ============================================================
-- ORDEN SEGURO: extensiones → tablas base → tablas dependientes
--               → ALTER existentes → índices → RLS → RPCs
-- ============================================================

-- ── 0. Extensiones ────────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid() ya existe en Supabase

-- ============================================================
-- SECCIÓN 1 – TABLAS NUEVAS (no definidas en migración 001)
-- ============================================================

-- ── 1.1 courses (consolida 'courses' + 'classrooms') ─────────────────────────
-- Violación 3NF corregida: school_name eliminado (era id → school_id → school_name)
CREATE TABLE IF NOT EXISTS courses (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT        NOT NULL,
    description TEXT,
    subject     TEXT,
    grade       TEXT,
    section     TEXT,
    class_code  TEXT        UNIQUE,
    school_id   UUID        REFERENCES schools(id) ON DELETE SET NULL,
    teacher_id  UUID        NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    invite_code TEXT        UNIQUE,
    is_active   BOOLEAN     NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMENT ON TABLE courses IS '3NF: school_name eliminado — derivar con JOIN a schools.';

-- ── 1.2 students ──────────────────────────────────────────────────────────────
-- course_id es UUID FK (en código legacy era TEXT — corregido)
-- parent_id referencia profiles, no nullable porque todo alumno tiene padre/tutor
CREATE TABLE IF NOT EXISTS students (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name   TEXT        NOT NULL,
    student_rut TEXT,
    course_id   UUID        NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    parent_id   UUID        NOT NULL REFERENCES profiles(id) ON DELETE RESTRICT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (student_rut, course_id)   -- mismo RUT no puede repetirse en el mismo curso
);

-- ── 1.3 attendance ────────────────────────────────────────────────────────────
-- Violación 3NF corregida: course_id eliminado (student_id → course_id es transitivo)
-- Un alumno sólo puede tener UN registro de asistencia por día
CREATE TABLE IF NOT EXISTS attendance (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID        NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    date       DATE        NOT NULL,
    status     TEXT        NOT NULL
                           CHECK (status IN ('PRESENT', 'ABSENT', 'LATE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (student_id, date)
);
COMMENT ON TABLE attendance IS '3NF: course_id eliminado — derivar con students.course_id.';

-- ── 1.4 justifications ───────────────────────────────────────────────────────
-- Un alumno sólo puede tener UNA justificación por día
CREATE TABLE IF NOT EXISTS justifications (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID        NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    date       DATE        NOT NULL,
    reason     TEXT        NOT NULL CHECK (trim(reason) <> ''),
    status     TEXT        NOT NULL DEFAULT 'PENDING'
                           CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (student_id, date)
);

-- ── 1.5 messages (mensajes P2P: docente ↔ apoderado) ─────────────────────────
CREATE TABLE IF NOT EXISTS messages (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id    UUID        NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    receiver_id  UUID        NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    message_text TEXT        NOT NULL CHECK (trim(message_text) <> ''),
    read_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (sender_id <> receiver_id)   -- no mensajes a sí mismo
);

-- ── 1.6 communications (avisos de curso — broadcast del docente) ──────────────
CREATE TABLE IF NOT EXISTS communications (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id    UUID        NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    course_id    UUID        NOT NULL REFERENCES courses(id)  ON DELETE CASCADE,
    message_text TEXT        NOT NULL CHECK (trim(message_text) <> ''),
    category     TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ── 1.7 enrollments (apoderado inscribe alumno a un curso por invite_code) ───
CREATE TABLE IF NOT EXISTS enrollments (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id    UUID        NOT NULL REFERENCES courses(id)   ON DELETE CASCADE,
    parent_id    UUID        NOT NULL REFERENCES profiles(id)  ON DELETE CASCADE,
    student_id   UUID        REFERENCES students(id)           ON DELETE SET NULL,
    student_name TEXT        NOT NULL CHECK (trim(student_name) <> ''),
    student_rut  TEXT,
    enrolled_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (course_id, student_name)   -- evita duplicados del mismo alumno
);

-- ============================================================
-- SECCIÓN 2 – CORRECCIONES EN TABLAS EXISTENTES (001)
-- ============================================================

-- ── 2.1 profiles: eliminar course_id (dependencia transitiva) ────────────────
-- El curso de un docente se gestiona en course_assignments o en courses.teacher_id
ALTER TABLE profiles DROP COLUMN IF EXISTS course_id;

-- Agregar email con UNIQUE si no existe
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'profiles' AND column_name = 'email'
    ) THEN
        ALTER TABLE profiles ADD COLUMN email TEXT;
    END IF;
END $$;

-- Garantizar unicidad de email (ignorando NULLs)
CREATE UNIQUE INDEX IF NOT EXISTS uq_profiles_email
    ON profiles (email) WHERE email IS NOT NULL;

-- Constraint de rol válido
ALTER TABLE profiles
    DROP CONSTRAINT IF EXISTS chk_profiles_role,
    ADD  CONSTRAINT chk_profiles_role
         CHECK (role IN ('TEACHER', 'PARENT') OR role IS NULL);

-- ── 2.2 courses: eliminar school_name (dependencia transitiva vía school_id) ─
-- Si la tabla ya existía con school_name, la eliminamos
ALTER TABLE courses DROP COLUMN IF EXISTS school_name;

-- ── 2.3 course_assignments: course_id TEXT → UUID FK ─────────────────────────
-- Sólo aplica si la columna es TEXT; si ya es UUID el bloque no falla
DO $$
BEGIN
    -- Borrar FK y constraint anteriores para re-crearlos tipados
    ALTER TABLE course_assignments DROP CONSTRAINT IF EXISTS course_assignments_course_id_fkey;
    ALTER TABLE course_assignments ALTER COLUMN course_id TYPE UUID USING course_id::UUID;
    ALTER TABLE course_assignments
        ADD CONSTRAINT course_assignments_course_id_fkey
        FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE;
EXCEPTION WHEN others THEN
    -- Si la columna no existe o ya es UUID, continúa
    NULL;
END $$;

-- is_head_teacher debe existir en course_assignments
ALTER TABLE course_assignments
    ADD COLUMN IF NOT EXISTS is_head_teacher BOOLEAN NOT NULL DEFAULT false;

-- ── 2.4 invitation_codes: student_id TEXT → UUID FK ─────────────────────────
DO $$
BEGIN
    ALTER TABLE invitation_codes DROP CONSTRAINT IF EXISTS invitation_codes_student_id_fkey;
    ALTER TABLE invitation_codes ALTER COLUMN student_id TYPE UUID USING student_id::UUID;
    ALTER TABLE invitation_codes
        ADD CONSTRAINT invitation_codes_student_id_fkey
        FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE;
EXCEPTION WHEN others THEN
    NULL;
END $$;

-- ============================================================
-- SECCIÓN 3 – ÍNDICES (rendimiento de queries críticas)
-- ============================================================

-- profiles
CREATE INDEX IF NOT EXISTS idx_profiles_role       ON profiles (role);

-- courses
CREATE INDEX IF NOT EXISTS idx_courses_teacher     ON courses (teacher_id);
CREATE INDEX IF NOT EXISTS idx_courses_school      ON courses (school_id);
CREATE INDEX IF NOT EXISTS idx_courses_invite      ON courses (invite_code) WHERE invite_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_courses_active      ON courses (is_active) WHERE is_active = true;

-- students
CREATE INDEX IF NOT EXISTS idx_students_course     ON students (course_id);
CREATE INDEX IF NOT EXISTS idx_students_parent     ON students (parent_id);

-- attendance — la query más frecuente: por alumno, por curso (via join), por fecha
CREATE INDEX IF NOT EXISTS idx_attendance_student  ON attendance (student_id);
CREATE INDEX IF NOT EXISTS idx_attendance_date     ON attendance (date DESC);
CREATE INDEX IF NOT EXISTS idx_attendance_status   ON attendance (status);
-- Índice compuesto para dashboard de asistencia por curso
CREATE INDEX IF NOT EXISTS idx_attendance_student_date
    ON attendance (student_id, date DESC);

-- justifications
CREATE INDEX IF NOT EXISTS idx_justif_student      ON justifications (student_id);
CREATE INDEX IF NOT EXISTS idx_justif_status       ON justifications (status);
CREATE INDEX IF NOT EXISTS idx_justif_student_date ON justifications (student_id, date DESC);

-- messages — inbox query: OR(sender_id, receiver_id)
CREATE INDEX IF NOT EXISTS idx_messages_sender     ON messages (sender_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_receiver   ON messages (receiver_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_unread     ON messages (receiver_id, read_at) WHERE read_at IS NULL;

-- communications
CREATE INDEX IF NOT EXISTS idx_comms_course        ON communications (course_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_comms_sender        ON communications (sender_id);

-- course_assignments
CREATE INDEX IF NOT EXISTS idx_ca_teacher          ON course_assignments (teacher_id);
CREATE INDEX IF NOT EXISTS idx_ca_course           ON course_assignments (course_id);
CREATE INDEX IF NOT EXISTS idx_ca_school           ON course_assignments (school_id);

-- invitation_codes
CREATE INDEX IF NOT EXISTS idx_invite_teacher      ON invitation_codes (teacher_id);
CREATE INDEX IF NOT EXISTS idx_invite_student      ON invitation_codes (student_id);
CREATE INDEX IF NOT EXISTS idx_invite_expires      ON invitation_codes (expires_at) WHERE claimed_by IS NULL;

-- enrollments
CREATE INDEX IF NOT EXISTS idx_enroll_course       ON enrollments (course_id);
CREATE INDEX IF NOT EXISTS idx_enroll_parent       ON enrollments (parent_id);

-- ============================================================
-- SECCIÓN 4 – ROW LEVEL SECURITY (tablas nuevas)
-- ============================================================

ALTER TABLE courses        ENABLE ROW LEVEL SECURITY;
ALTER TABLE students       ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance     ENABLE ROW LEVEL SECURITY;
ALTER TABLE justifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages       ENABLE ROW LEVEL SECURITY;
ALTER TABLE communications ENABLE ROW LEVEL SECURITY;
ALTER TABLE enrollments    ENABLE ROW LEVEL SECURITY;

-- courses: docente ve/edita sus propios cursos; apoderado ve los de sus alumnos
CREATE POLICY IF NOT EXISTS "teacher_own_courses" ON courses
    FOR ALL USING (teacher_id = auth.uid());

CREATE POLICY IF NOT EXISTS "parent_enrolled_courses" ON courses
    FOR SELECT USING (
        id IN (
            SELECT course_id FROM students
            WHERE parent_id = auth.uid()
        )
    );

-- students: docente ve alumnos de sus cursos; apoderado ve sólo sus hijos
CREATE POLICY IF NOT EXISTS "teacher_course_students" ON students
    FOR ALL USING (
        course_id IN (
            SELECT id FROM courses WHERE teacher_id = auth.uid()
        )
    );

CREATE POLICY IF NOT EXISTS "parent_own_children" ON students
    FOR SELECT USING (parent_id = auth.uid());

-- attendance: docente gestiona; apoderado sólo lee la de sus hijos
CREATE POLICY IF NOT EXISTS "teacher_manage_attendance" ON attendance
    FOR ALL USING (
        student_id IN (
            SELECT s.id FROM students s
            JOIN courses c ON c.id = s.course_id
            WHERE c.teacher_id = auth.uid()
        )
    );

CREATE POLICY IF NOT EXISTS "parent_read_attendance" ON attendance
    FOR SELECT USING (
        student_id IN (
            SELECT id FROM students WHERE parent_id = auth.uid()
        )
    );

-- justifications: apoderado crea/lee las de sus hijos; docente gestiona
CREATE POLICY IF NOT EXISTS "parent_own_justifications" ON justifications
    FOR ALL USING (
        student_id IN (
            SELECT id FROM students WHERE parent_id = auth.uid()
        )
    );

CREATE POLICY IF NOT EXISTS "teacher_manage_justifications" ON justifications
    FOR ALL USING (
        student_id IN (
            SELECT s.id FROM students s
            JOIN courses c ON c.id = s.course_id
            WHERE c.teacher_id = auth.uid()
        )
    );

-- messages P2P: cada usuario ve sólo sus propios mensajes
CREATE POLICY IF NOT EXISTS "user_own_messages" ON messages
    FOR ALL USING (
        sender_id = auth.uid() OR receiver_id = auth.uid()
    );

-- communications: docente publica; apoderados ven las de sus cursos
CREATE POLICY IF NOT EXISTS "teacher_publish_comms" ON communications
    FOR ALL USING (sender_id = auth.uid());

CREATE POLICY IF NOT EXISTS "parent_read_comms" ON communications
    FOR SELECT USING (
        course_id IN (
            SELECT course_id FROM students WHERE parent_id = auth.uid()
        )
    );

-- enrollments: apoderado gestiona sus inscripciones; docente ve las de su curso
CREATE POLICY IF NOT EXISTS "parent_own_enrollments" ON enrollments
    FOR ALL USING (parent_id = auth.uid());

CREATE POLICY IF NOT EXISTS "teacher_read_enrollments" ON enrollments
    FOR SELECT USING (
        course_id IN (SELECT id FROM courses WHERE teacher_id = auth.uid())
    );

-- ============================================================
-- SECCIÓN 5 – FUNCIONES RPC
-- ============================================================

-- ── 5.1 claim_invitation_code: atómica (evita race condition SELECT+UPDATE) ──
-- Reemplaza el patrón SELECT → check → UPDATE que tenía race condition
CREATE OR REPLACE FUNCTION public.claim_invitation_code(
    p_code      TEXT,
    p_parent_id UUID
)
RETURNS TABLE (
    code        TEXT,
    student_id  UUID,
    teacher_id  UUID,
    claimed_by  UUID,
    expires_at  TIMESTAMPTZ
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    RETURN QUERY
    UPDATE invitation_codes ic
    SET    claimed_by = p_parent_id
    WHERE  ic.code       = upper(p_code)
      AND  ic.claimed_by IS NULL
      AND  ic.expires_at > now()
    RETURNING
        ic.code,
        ic.student_id,
        ic.teacher_id,
        ic.claimed_by,
        ic.expires_at;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Código inválido, ya reclamado o expirado'
            USING ERRCODE = 'P0001';
    END IF;
END;
$$;

-- ── 5.2 generate_invite_code: genera código único para un curso ───────────────
CREATE OR REPLACE FUNCTION public.generate_invite_code()
RETURNS TEXT
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_code TEXT;
    v_chars TEXT := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    v_exists BOOLEAN;
BEGIN
    LOOP
        -- Genera 6 caracteres aleatorios
        v_code := '';
        FOR i IN 1..6 LOOP
            v_code := v_code || substr(v_chars, floor(random() * length(v_chars) + 1)::int, 1);
        END LOOP;

        -- Verifica unicidad en invitation_codes y courses
        SELECT EXISTS(
            SELECT 1 FROM invitation_codes WHERE code = v_code
            UNION ALL
            SELECT 1 FROM courses WHERE invite_code = v_code
        ) INTO v_exists;

        EXIT WHEN NOT v_exists;
    END LOOP;
    RETURN v_code;
END;
$$;

-- ── 5.3 get_course_attendance_summary: batch para evitar N+1 en analytics ────
-- Carga toda la asistencia de un curso en UNA query (reemplaza N queries por alumno)
CREATE OR REPLACE FUNCTION public.get_course_attendance_summary(p_course_id UUID)
RETURNS TABLE (
    student_id UUID,
    date       DATE,
    status     TEXT
)
LANGUAGE sql
STABLE
SECURITY DEFINER
AS $$
    SELECT a.student_id, a.date, a.status
    FROM   attendance a
    JOIN   students   s ON s.id = a.student_id
    WHERE  s.course_id = p_course_id
    ORDER  BY a.date DESC;
$$;

-- ── 5.4 Actualiza trigger handle_new_user para incluir email ─────────────────
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
        'TEACHER'
    )
    ON CONFLICT (id) DO UPDATE
        SET email = EXCLUDED.email
        WHERE profiles.email IS NULL;
    RETURN NEW;
END;
$$;

-- ============================================================
-- SECCIÓN 6 – PERMISOS PARA ANON / AUTHENTICATED
-- ============================================================

GRANT EXECUTE ON FUNCTION public.claim_invitation_code  TO authenticated;
GRANT EXECUTE ON FUNCTION public.generate_invite_code   TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_course_attendance_summary TO authenticated;
