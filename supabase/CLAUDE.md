# CLAUDE.md — supabase/

> Schema autoritativo de la base de datos. No inventar tablas ni columnas.
> Antes de cualquier query o migración, verificar aquí que la tabla/columna existe.

---

## 1. SCHEMA COMPLETO (estado post-002_normalize_3nf.sql)

### `schools`
```sql
id         UUID        PK DEFAULT gen_random_uuid()
name       TEXT        NOT NULL
address    TEXT
created_at TIMESTAMPTZ NOT NULL DEFAULT now()
```

### `profiles`
```sql
id         UUID        PK  -- mismo ID que auth.users.id
full_name  TEXT
email      TEXT        UNIQUE (índice parcial WHERE email IS NOT NULL)
role       TEXT        CHECK (role IN ('TEACHER','PARENT') OR role IS NULL)
created_at TIMESTAMPTZ NOT NULL DEFAULT now()
```
> ⚠️ `course_id` fue **eliminado** en migración 002 — usar `course_assignments` o `courses.teacher_id`

### `courses`
```sql
id          UUID        PK DEFAULT gen_random_uuid()
name        TEXT        NOT NULL
description TEXT
subject     TEXT
grade       TEXT
section     TEXT
class_code  TEXT        UNIQUE
school_id   UUID        FK → schools(id) ON DELETE SET NULL
teacher_id  UUID        NOT NULL FK → profiles(id) ON DELETE CASCADE
invite_code TEXT        UNIQUE
is_active   BOOLEAN     NOT NULL DEFAULT true
created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
```
> ⚠️ `school_name` fue **eliminado** en migración 002 — usar `JOIN schools` para obtener el nombre

### `students`
```sql
id          UUID   PK DEFAULT gen_random_uuid()
full_name   TEXT   NOT NULL
student_rut TEXT
course_id   UUID   NOT NULL FK → courses(id) ON DELETE CASCADE
parent_id   UUID   NOT NULL FK → profiles(id) ON DELETE RESTRICT
created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
UNIQUE (student_rut, course_id)
```

### `attendance`
```sql
id         UUID   PK DEFAULT gen_random_uuid()
student_id UUID   NOT NULL FK → students(id) ON DELETE CASCADE
date       DATE   NOT NULL
status     TEXT   NOT NULL CHECK (status IN ('PRESENT','ABSENT','LATE'))
created_at TIMESTAMPTZ NOT NULL DEFAULT now()
UNIQUE (student_id, date)   -- un registro por alumno por día
```
> ⚠️ `course_id` fue **eliminado** en migración 002 — derivar via `students.course_id`

### `justifications`
```sql
id         UUID   PK DEFAULT gen_random_uuid()
student_id UUID   NOT NULL FK → students(id) ON DELETE CASCADE
date       DATE   NOT NULL
reason     TEXT   NOT NULL CHECK (trim(reason) <> '')
status     TEXT   NOT NULL DEFAULT 'PENDING'
           CHECK (status IN ('PENDING','APPROVED','REJECTED'))
created_at TIMESTAMPTZ NOT NULL DEFAULT now()
UNIQUE (student_id, date)   -- una justificación por alumno por día
```

### `messages` (P2P — docente ↔ apoderado)
```sql
id           UUID   PK DEFAULT gen_random_uuid()
sender_id    UUID   NOT NULL FK → profiles(id) ON DELETE CASCADE
receiver_id  UUID   NOT NULL FK → profiles(id) ON DELETE CASCADE
message_text TEXT   NOT NULL CHECK (trim(message_text) <> '')
read_at      TIMESTAMPTZ   -- NULL = no leído
created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
CHECK (sender_id <> receiver_id)
```

### `communications` (avisos de clase — broadcast del docente)
```sql
id           UUID   PK DEFAULT gen_random_uuid()
sender_id    UUID   NOT NULL FK → profiles(id) ON DELETE CASCADE
course_id    UUID   NOT NULL FK → courses(id) ON DELETE CASCADE
message_text TEXT   NOT NULL CHECK (trim(message_text) <> '')
category     TEXT
created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
```

### `enrollments` (apoderado inscribe alumno via invite_code)
```sql
id           UUID   PK DEFAULT gen_random_uuid()
course_id    UUID   NOT NULL FK → courses(id) ON DELETE CASCADE
parent_id    UUID   NOT NULL FK → profiles(id) ON DELETE CASCADE
student_id   UUID   FK → students(id) ON DELETE SET NULL
student_name TEXT   NOT NULL CHECK (trim(student_name) <> '')
student_rut  TEXT
enrolled_at  TIMESTAMPTZ NOT NULL DEFAULT now()
UNIQUE (course_id, student_name)
```

### `course_assignments` (docente ↔ curso ↔ escuela)
```sql
id              UUID    PK DEFAULT gen_random_uuid()
teacher_id      UUID    NOT NULL FK → profiles(id) ON DELETE CASCADE
course_id       UUID    NOT NULL FK → courses(id) ON DELETE CASCADE   -- era TEXT en v001
school_id       UUID    NOT NULL FK → schools(id) ON DELETE CASCADE
is_head_teacher BOOLEAN NOT NULL DEFAULT false
created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
UNIQUE (teacher_id, course_id)
```

### `invitation_codes` (docente genera → apoderado reclama)
```sql
code       TEXT        PK              -- 6 caracteres alfanuméricos uppercase
student_id UUID        NOT NULL FK → students(id) ON DELETE CASCADE  -- era TEXT en v001
teacher_id UUID        NOT NULL FK → profiles(id)
claimed_by UUID        FK → profiles(id)   -- NULL hasta que el apoderado reclame
expires_at TIMESTAMPTZ NOT NULL DEFAULT (now() + INTERVAL '7 days')
created_at TIMESTAMPTZ NOT NULL DEFAULT now()
```

---

## 2. ÍNDICES EXISTENTES

```sql
-- profiles
uq_profiles_email (UNIQUE parcial WHERE email IS NOT NULL)
idx_profiles_role

-- courses
idx_courses_teacher, idx_courses_school
idx_courses_invite (WHERE invite_code IS NOT NULL)
idx_courses_active (WHERE is_active = true)

-- students
idx_students_course, idx_students_parent

-- attendance
idx_attendance_student, idx_attendance_date, idx_attendance_status
idx_attendance_student_date (student_id, date DESC)

-- justifications
idx_justif_student, idx_justif_status
idx_justif_student_date (student_id, date DESC)

-- messages
idx_messages_sender (sender_id, created_at DESC)
idx_messages_receiver (receiver_id, created_at DESC)
idx_messages_unread (receiver_id, read_at) WHERE read_at IS NULL

-- communications
idx_comms_course (course_id, created_at DESC)
idx_comms_sender

-- course_assignments
idx_ca_teacher, idx_ca_course, idx_ca_school

-- invitation_codes
idx_invite_teacher, idx_invite_student
idx_invite_expires (expires_at) WHERE claimed_by IS NULL

-- enrollments
idx_enroll_course, idx_enroll_parent
```

---

## 3. RLS — POLÍTICAS POR TABLA

| Tabla | Rol | Operación | Condición |
|---|---|---|---|
| `courses` | TEACHER | ALL | `teacher_id = auth.uid()` |
| `courses` | PARENT | SELECT | `id IN (SELECT course_id FROM students WHERE parent_id = auth.uid())` |
| `students` | TEACHER | ALL | `course_id IN (SELECT id FROM courses WHERE teacher_id = auth.uid())` |
| `students` | PARENT | SELECT | `parent_id = auth.uid()` |
| `attendance` | TEACHER | ALL | via `students JOIN courses WHERE courses.teacher_id = auth.uid()` |
| `attendance` | PARENT | SELECT | via `students WHERE parent_id = auth.uid()` |
| `justifications` | PARENT | ALL | via `students WHERE parent_id = auth.uid()` |
| `justifications` | TEACHER | ALL | via `students JOIN courses WHERE teacher_id = auth.uid()` |
| `messages` | ANY | ALL | `sender_id = auth.uid() OR receiver_id = auth.uid()` |
| `communications` | TEACHER | ALL | `sender_id = auth.uid()` |
| `communications` | PARENT | SELECT | via `students WHERE parent_id = auth.uid()` |
| `enrollments` | PARENT | ALL | `parent_id = auth.uid()` |
| `enrollments` | TEACHER | SELECT | via `courses WHERE teacher_id = auth.uid()` |
| `schools` | TEACHER | — | policy "teacher_own_assignments" en `course_assignments` |
| `course_assignments` | TEACHER | ALL | `teacher_id = auth.uid()` |
| `invitation_codes` | TEACHER | ALL | `teacher_id = auth.uid()` |
| `invitation_codes` | PARENT | SELECT | `claimed_by = auth.uid() OR claimed_by IS NULL` |

---

## 4. RPCs DISPONIBLES (llamar desde Kotlin con `supabase.postgrest.rpc(...)`)

### `claim_invitation_code(p_code TEXT, p_parent_id UUID)`
**Propósito:** Reclama un código de invitación de forma atómica (evita race condition).
**Retorna:** Fila de `invitation_codes` actualizada.
**Errores:** Lanza excepción si código inválido, ya reclamado, o expirado (`ERRCODE P0001`).

```kotlin
// Uso en Kotlin:
supabase.postgrest.rpc(
    "claim_invitation_code",
    mapOf("p_code" to code.uppercase(), "p_parent_id" to parentId.value)
).decodeSingle<InvitationCodeDto>()
```

### `generate_invite_code()`
**Propósito:** Genera código único de 6 caracteres alfanuméricos, verificando unicidad contra `invitation_codes` y `courses`.
**Retorna:** `TEXT` (el código).

```kotlin
// Uso en Kotlin:
val code = supabase.postgrest.rpc("generate_invite_code").decodeAs<String>()
```

### `get_course_attendance_summary(p_course_id UUID)`
**Propósito:** Retorna toda la asistencia de un curso en 1 query (evita N+1).
**Retorna:** `TABLE(student_id UUID, date DATE, status TEXT)` ordenado por `date DESC`.

```kotlin
// Uso en Kotlin:
data class AttendanceSummaryRow(
    @SerialName("student_id") val studentId: String,
    val date: String,
    val status: String
)
val rows = supabase.postgrest
    .rpc("get_course_attendance_summary", mapOf("p_course_id" to courseId.value))
    .decodeList<AttendanceSummaryRow>()
```

---

## 5. TRIGGER AUTOMÁTICO

```sql
-- Se ejecuta al crear usuario en auth.users (Google Sign-In)
-- Crea perfil automáticamente con rol TEACHER por defecto
-- El email se rellena desde auth.users.email
on_auth_user_created → public.handle_new_user()
```

---

## 6. REGLAS 3NF — QUÉ NO ALMACENAR

| ❌ No almacenar | ✅ Derivar con |
|---|---|
| `school_name` en `courses` | `JOIN schools ON schools.id = courses.school_id` |
| `course_id` en `profiles` | `courses.teacher_id = profiles.id` |
| `course_id` en `attendance` | `JOIN students ON students.id = attendance.student_id` |
| `attendance_percentage` en `students` | Calcular en la app / RPC analítica |
| Nombre de escuela en `course_assignments` | `JOIN schools` |

---

## 7. QUERIES FRECUENTES (referencia)

```sql
-- Alumnos de un curso con su último estado de asistencia
SELECT s.id, s.full_name,
       a.date, a.status
FROM   students s
LEFT JOIN LATERAL (
    SELECT date, status FROM attendance
    WHERE  student_id = s.id
    ORDER  BY date DESC LIMIT 1
) a ON true
WHERE  s.course_id = $courseId;

-- Resumen de asistencia de un curso (para analytics)
SELECT student_id, date, status
FROM   attendance a
JOIN   students   s ON s.id = a.student_id
WHERE  s.course_id = $courseId
ORDER  BY date DESC;
-- → Usar RPC get_course_attendance_summary($courseId)

-- Inbox: último mensaje por conversación
SELECT DISTINCT ON (contact_id)
    CASE WHEN sender_id = $uid THEN receiver_id ELSE sender_id END AS contact_id,
    message_text, created_at, read_at
FROM   messages
WHERE  sender_id = $uid OR receiver_id = $uid
ORDER  BY contact_id, created_at DESC;
```

---

## 8. CONVENCIONES DE MIGRACIONES

- Archivo: `supabase/migrations/00N_descripcion_corta.sql`
- Ejecutar en orden numérico en Supabase → SQL Editor
- Siempre usar `CREATE TABLE IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`, `ALTER TABLE ... DROP COLUMN IF EXISTS`
- Toda migración incluye: tablas → FK → índices → RLS → RPCs
- Nunca modificar migraciones ya ejecutadas — crear nueva migración incremental
- Secciones del archivo: comentar con `-- ── Nombre ─────`

---

## 9. ACCESO DESDE KOTLIN — PATRONES CORRECTOS

```kotlin
// SELECT con columnas específicas (nunca SELECT *)
supabase.from("attendance")
    .select("id,student_id,date,status") {
        filter { eq("student_id", id.value) }
        limit(100)
    }
    .decodeList<AttendanceSupabaseDto>()

// INSERT con UPSERT
supabase.from("attendance").upsert(dto)

// UPDATE filtrado
supabase.from("attendance")
    .update({ set("status", "PRESENT") }) {
        filter { eq("id", id.value) }
    }

// RPC
supabase.postgrest.rpc("nombre_funcion", mapOf("param" to valor))

// Llamar supabase.from() o supabase.postgrest[] son equivalentes:
// supabase.from("tabla") == supabase.postgrest["tabla"]
```
