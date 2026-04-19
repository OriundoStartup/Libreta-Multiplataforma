-- ============================================================
-- Migration: multi-school + invitation codes + auth trigger
-- Run in Supabase → SQL Editor
-- ============================================================

-- ── Schools ───────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS schools (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       TEXT NOT NULL,
    address    TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ── Course assignments (teacher ↔ course ↔ school) ───────────────────────────
CREATE TABLE IF NOT EXISTS course_assignments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id      UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    course_id       TEXT NOT NULL,   -- matches ClassEntity.id
    school_id       UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    is_head_teacher BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ DEFAULT now(),
    UNIQUE (teacher_id, course_id, school_id)
);

-- ── Invitation codes (teacher generates → parent claims) ─────────────────────
CREATE TABLE IF NOT EXISTS invitation_codes (
    code        TEXT PRIMARY KEY,           -- 6-char alphanumeric
    student_id  TEXT NOT NULL,              -- matches StudentEntity.id
    teacher_id  UUID NOT NULL REFERENCES profiles(id),
    claimed_by  UUID REFERENCES profiles(id),   -- NULL until parent claims
    expires_at  TIMESTAMPTZ NOT NULL DEFAULT (now() + INTERVAL '7 days'),
    created_at  TIMESTAMPTZ DEFAULT now()
);

-- ── RLS Policies ─────────────────────────────────────────────────────────────
ALTER TABLE schools            ENABLE ROW LEVEL SECURITY;
ALTER TABLE course_assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE invitation_codes   ENABLE ROW LEVEL SECURITY;

-- Teachers see only their own assignments
CREATE POLICY "teacher_own_assignments" ON course_assignments
    FOR ALL USING (teacher_id = auth.uid());

-- Parents see only codes they claimed or that target their student
CREATE POLICY "parent_own_codes" ON invitation_codes
    FOR SELECT USING (claimed_by = auth.uid() OR claimed_by IS NULL);

CREATE POLICY "teacher_manage_codes" ON invitation_codes
    FOR ALL USING (teacher_id = auth.uid());

-- ── Trigger: auto-create profile on Google Sign-In ───────────────────────────
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    INSERT INTO public.profiles (id, full_name, role)
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'full_name', NEW.raw_user_meta_data->>'name', split_part(NEW.email, '@', 1)),
        'TEACHER'
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
