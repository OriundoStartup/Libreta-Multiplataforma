-- ============================================================
-- 🛡️ FASE 5 — Tests de políticas RLS con pgTAP
--
-- Ejecutar con:
--   psql -d $SUPABASE_DB_URL -f supabase/tests/rls_policies_test.sql
--
-- Requiere extensión:
--   CREATE EXTENSION IF NOT EXISTS pgtap;
--
-- TODO[FASE-5]:
--   1. Habilitar pgtap en Supabase (issue a soporte si no está disponible).
--   2. Crear usuarios de prueba con auth.users + JWT mock.
--   3. Para cada policy en supabase/CLAUDE.md sección 3:
--      a. Test de acceso permitido
--      b. Test de acceso denegado
--   4. Integrar en CI: GitHub Actions con servicio postgres + pgtap.
-- ============================================================

BEGIN;

-- SELECT plan(N);  -- declarar cuántos asserts vamos a correr

-- ── Setup de usuarios mock ───────────────────────────────────────────────────
-- TODO: crear 2 profiles (1 TEACHER, 1 PARENT) y simular auth.uid()
-- mediante set_config('request.jwt.claims', ...)

-- ── Test: TEACHER ve sus cursos ──────────────────────────────────────────────
-- SELECT ok(
--   EXISTS(SELECT 1 FROM courses WHERE teacher_id = 'TEACHER_UID'),
--   'TEACHER can see own courses'
-- );

-- ── Test: TEACHER NO ve cursos ajenos ────────────────────────────────────────
-- SELECT ok(
--   NOT EXISTS(SELECT 1 FROM courses WHERE teacher_id = 'OTHER_TEACHER_UID'),
--   'TEACHER cannot see courses of other teachers'
-- );

-- ── Test: PARENT ve solo a sus hijos ─────────────────────────────────────────
-- ── Test: PARENT NO ve hijos ajenos ──────────────────────────────────────────
-- ── Test: messages — sender o receiver puede leer ────────────────────────────
-- ── Test: messages — terceros NO pueden leer ─────────────────────────────────
-- ── Test: invitation_codes.claim_invitation_code() — race condition ─────────
-- ── Test: storage justifications/ — folder por auth.uid() ───────────────────

-- SELECT * FROM finish();
ROLLBACK;
