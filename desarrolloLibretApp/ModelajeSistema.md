# Modelado Técnico del Sistema: LibretApp

## 1. Diagrama Entidad-Relación (ERD) - Lógica Central
El sistema utiliza una arquitectura **Hub-and-Spoke** donde el `Profile` y el `Course` son los nodos centrales.

### Entidades Principales:
*   **Profiles (Usuarios):** Almacena `id` (UUID de Auth), `full_name`, `email` y `role` (TEACHER/PARENT).
*   **Schools:** Instituciones educativas asociadas a los cursos.
*   **Courses:** El aula virtual. Relaciona a un `teacher_id` con una `school_id`.
*   **Students:** Alumnos vinculados a un `course_id` y a un `parent_id` (Apoderado).
*   **Attendance:** Registros diarios por `student_id`. (Normalizado: el `course_id` se obtiene vía JOIN con Students).
*   **Grades:** Calificaciones asociadas a un estudiante y curso, con soporte para `weight` (ponderación) y `term` (periodo).
*   **Justifications:** Solicitudes de los padres para validar inasistencias.
*   **Messages & Communications:** Mensajería P2P entre docente-apoderado y avisos masivos a cursos.

---

## 2. Diagrama de Casos de Uso

### Actor: Profesor (Teacher)
1.  **Gestionar Cursos:** Crear, editar y activar/desactivar aulas.
2.  **Control de Asistencia:** Pasar lista diaria (Modo Online/Offline).
3.  **Gestión Académica:** Registro masivo de notas y visualización de promedios.
4.  **Auditoría de Justificaciones:** Revisar, aprobar o rechazar evidencias de inasistencia.
5.  **Comunicación Directa:** Enviar avisos al curso o mensajes privados a apoderados.

### Actor: Apoderado (Parent)
1.  **Vinculación:** Reclamar código de invitación para asociar un alumno a su cuenta.
2.  **Seguimiento:** Visualizar asistencia y calificaciones en tiempo real.
3.  **Justificación:** Cargar motivos y documentos para inasistencias.
4.  **Recepción de Avisos:** Leer circulares y notificaciones del establecimiento.

---

## 3. Especificaciones de Sincronización (Local-First)
El sistema emplea una **Tabla de Entidades Espejo** en SQLite con una columna `sync_status`:

*   **SYNCED:** El dato está idéntico en el local y en Supabase.
*   **PENDING_INSERT:** Creado offline, pendiente de subir.
*   **PENDING_UPDATE:** Modificado localmente, pendiente de actualizar.
*   **PENDING_DELETE:** Marcado para borrar (Soft-delete local) hasta que se confirme en el servidor.

**Lógica de Sync:** El `SyncManager` procesa estas colas utilizando operaciones atómicas (UPSERT) en Supabase para evitar duplicados.

---

## 4. Seguridad y Privacidad (RLS)
Se implementa **Row Level Security (RLS)** en el backend para garantizar:
1.  **Aislamiento de Docente:** Un profesor solo puede leer/escribir datos de los cursos donde es `teacher_id`.
2.  **Privacidad del Apoderado:** Solo puede ver datos de `Students` donde su `auth.uid()` coincida con `parent_id`.
3.  **Integridad de Mensajería P2P:** Los mensajes solo son visibles si el usuario es el `sender_id` o `receiver_id`.

---

## 5. Arquitectura Técnica KMP
*   **Persistence:** SQLDelight (`LibretaApp.sq`) genera tipos de datos Kotlin seguros en tiempo de compilación.
*   **DI:** Koin inyecta los repositorios simbióticos que deciden cuándo leer de la DB local o llamar a Supabase.
*   **Use Cases:** La lógica de negocio (ej. `GetCourseAnalyticsUseCase`) es pura y compartida entre Android, iOS y Web.
