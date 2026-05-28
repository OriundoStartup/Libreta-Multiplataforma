-- ============================================================
-- 🔄 MIGRACIÓN 004: Sync Metadata + Tombstones + Optimistic Locking (FASE 3)
-- Fecha objetivo: 2026-07-XX
--
-- OBJETIVO:
--   Habilitar PULL incremental, soft-delete y resolución de conflictos
--   en el SyncManager v2.
--
-- COMPATIBILIDAD:
--   ADITIVA. Las columnas tienen defaults para que los registros viejos
--   queden marcados como server_version=1, deleted_at=NULL.
--   La app live no necesita conocer estas columnas para seguir funcionando.
--
-- ROLLBACK:
--   Las columnas pueden eliminarse con ALTER TABLE ... DROP COLUMN si v2 falla.
--   El trigger se puede DROPear sin perder datos.
-- ============================================================

-- ── 1. Helper genérico: trigger function que bumps server_version + updated_at ─
CREATE OR REPLACE FUNCTION public.bump_sync_version()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.server_version := COALESCE(OLD.server_version, 0) + 1;
    NEW.updated_at     := now();
    RETURN NEW;
END $$;

-- ── 2. Columnas de sync por tabla ────────────────────────────────────────────
-- Aplicar a: attendance, students, justifications, grades, profiles, courses,
--            messages, communications.

-- TODO[FASE-3]: aplicar el bloque siguiente por cada tabla mutable.

-- ALTER TABLE attendance
--   ADD COLUMN IF NOT EXISTS server_version BIGINT      NOT NULL DEFAULT 1,
--   ADD COLUMN IF NOT EXISTS updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
--   ADD COLUMN IF NOT EXISTS deleted_at     TIMESTAMPTZ;
--
-- CREATE INDEX IF NOT EXISTS idx_attendance_updated_at ON attendance (updated_at DESC);
-- CREATE INDEX IF NOT EXISTS idx_attendance_deleted_at ON attendance (deleted_at) WHERE deleted_at IS NULL;
--
-- DROP TRIGGER IF EXISTS trg_bump_version_attendance ON attendance;
-- CREATE TRIGGER trg_bump_version_attendance
--     BEFORE UPDATE ON attendance
--     FOR EACH ROW EXECUTE FUNCTION public.bump_sync_version();

-- (Repetir para students, justifications, grades, profiles, courses, messages, communications)


-- ── 3. RPC: pull incremental por tabla y usuario ─────────────────────────────
-- Devuelve filas modificadas desde `p_since`, ya filtradas por RLS.
-- Cliente lo invoca al inicio del ciclo sync.

CREATE OR REPLACE FUNCTION public.sync_pull_attendance(p_since TIMESTAMPTZ)
RETURNS TABLE(
    id              UUID,
    student_id      UUID,
    date            DATE,
    status          TEXT,
    server_version  BIGINT,
    updated_at      TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ
)
LANGUAGE sql STABLE SECURITY INVOKER AS $$
    SELECT id, student_id, date, status, server_version, updated_at, deleted_at
    FROM   attendance
    WHERE  updated_at > p_since
    ORDER  BY updated_at ASC
    LIMIT  500
$$;

-- TODO[FASE-3]: crear sync_pull_* análogo para cada tabla mutable.


-- ── 4. RPC: push con optimistic locking ──────────────────────────────────────
-- El cliente envía la fila + last_known_server_version.
-- Si el server_version en BD != last_known_server_version → CONFLICT (P0001).

CREATE OR REPLACE FUNCTION public.sync_push_attendance(
    p_id                       UUID,
    p_student_id               UUID,
    p_date                     DATE,
    p_status                   TEXT,
    p_last_known_server_version BIGINT
)
RETURNS TABLE(
    id              UUID,
    server_version  BIGINT,
    updated_at      TIMESTAMPTZ
)
LANGUAGE plpgsql SECURITY INVOKER AS $$
DECLARE
    v_current BIGINT;
BEGIN
    SELECT server_version INTO v_current FROM attendance WHERE id = p_id;

    IF v_current IS NULL THEN
        -- INSERT path
        INSERT INTO attendance (id, student_id, date, status)
        VALUES (p_id, p_student_id, p_date, p_status);
    ELSIF v_current = p_last_known_server_version THEN
        -- UPDATE path (server_version coincide → no hubo cambio remoto entremedio)
        UPDATE attendance
        SET    student_id = p_student_id,
               date       = p_date,
               status     = p_status
        WHERE  attendance.id = p_id;
    ELSE
        RAISE EXCEPTION 'SYNC_CONFLICT: server_version % differs from client % for attendance %', v_current, p_last_known_server_version, p_id
            USING ERRCODE = 'P0001';
    END IF;

    RETURN QUERY
    SELECT a.id, a.server_version, a.updated_at FROM attendance a WHERE a.id = p_id;
END $$;

-- TODO[FASE-3]: análogo para cada tabla mutable.


-- ── 5. RPC: soft-delete (en vez de DELETE físico) ────────────────────────────
CREATE OR REPLACE FUNCTION public.sync_soft_delete_attendance(p_id UUID)
RETURNS VOID LANGUAGE sql SECURITY INVOKER AS $$
    UPDATE attendance SET deleted_at = now() WHERE id = p_id AND deleted_at IS NULL;
$$;

-- TODO[FASE-3]: idem por tabla.


-- ── 6. Permisos ──────────────────────────────────────────────────────────────
GRANT EXECUTE ON FUNCTION public.sync_pull_attendance        TO authenticated;
GRANT EXECUTE ON FUNCTION public.sync_push_attendance        TO authenticated;
GRANT EXECUTE ON FUNCTION public.sync_soft_delete_attendance TO authenticated;

-- ── 7. Tabla de auditoría de sync (opcional pero útil) ───────────────────────
CREATE TABLE IF NOT EXISTS public.sync_audit (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    table_name TEXT        NOT NULL,
    operation  TEXT        NOT NULL CHECK (operation IN ('PULL', 'PUSH', 'CONFLICT', 'DELETE')),
    row_id     UUID,
    details    JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_sync_audit_user_time ON public.sync_audit (user_id, created_at DESC);

ALTER TABLE public.sync_audit ENABLE ROW LEVEL SECURITY;
CREATE POLICY IF NOT EXISTS "user_own_sync_audit" ON public.sync_audit
    FOR ALL USING (user_id = auth.uid());

-- ── FIN MIGRACIÓN 004 ────────────────────────────────────────────────────────
