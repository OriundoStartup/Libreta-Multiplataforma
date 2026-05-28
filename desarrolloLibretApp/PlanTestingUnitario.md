# Plan de Testing Unitario Integral: LibretApp

## 1. Estrategia y Herramientas
*   **Framework:** `kotlin.test` (Multiplatform).
*   **Mocking:** `MocKMP` o `MockK` (para simular Repositorios y SupabaseClient).
*   **Coroutines:** `kotlinx-coroutines-test` para manejar Dispatchers en tests.
*   **Flows:** `Turbine` para verificar estados de UI y flujos de datos.

---

## 2. Capa de Dominio (Business Rules)
*Esta es la prioridad #1. Debe tener un 100% de cobertura.*

### A. Use Cases de Asistencia (`AttendanceUseCase`)
*   **Test:** Validar que un registro de asistencia no pueda ser duplicado para el mismo alumno/fecha.
*   **Test:** Verificar que el cambio de estado (Presente -> Ausente) dispare el flujo de "Alerta Temprana" si el umbral de inasistencia supera el 20%.

### B. Use Cases de Calificaciones (`GradeUseCase`)
*   **Test:** Cálculo de promedios ponderados. Si una nota vale el 30% y otra el 70%, el resultado debe ser exacto.
*   **Test:** Impedir el ingreso de notas fuera del rango permitido (ej. 1.0 a 7.0).

### C. AuthFlow State Machine
*   **Test:** Si `SessionStatus` es Authenticated pero el `role` es NULL, el estado resultante debe ser `NeedsRole`.
*   **Test:** Si un `Parent` intenta acceder a una ruta de `Teacher`, el estado debe ser `Forbidden`.

---

## 3. Capa de Datos y Sincronización (The Plumbing)

### A. SyncManager V2 (Conflict Resolution)
*   **Test (LWW - Server Wins):** Si el registro local tiene `server_version = 2` y el remoto tiene `server_version = 3`, el merge debe sobreescribir el local.
*   **Test (Optimistic Locking):** Simular un error `409 Conflict` desde Supabase y verificar que el `SyncStatus` cambie a `PENDING_CONFLICT`.
*   **Test (Soft-Delete):** Verificar que al recibir un `deleted_at` no nulo, el registro local se marque como eliminado y no sea visible en las queries comunes.

### B. Mappers
*   **Test:** Conversión correcta de ISO-8601 (Supabase) a Epoch Millis (SQLite) y viceversa.
*   **Test:** Verificación de que los campos `null` del servidor se manejen con valores por defecto seguros en el dominio.

---

## 4. Capa de Presentación (ScreenModels)

### A. Dashboard State
*   **Test:** Al cargar el Dashboard del profesor, el estado debe pasar de `Loading` a `Success` con la lista de cursos inyectada.
*   **Test:** Si el repositorio falla, el estado debe capturar el error y exponer un mensaje amigable.

---

## 5. Matriz de Casos de Prueba Críticos

| Módulo | Escenario de Prueba | Resultado Esperado |
| :--- | :--- | :--- |
| **Auth** | Login con Google exitoso sin perfil previo. | Redirección a `RoleSelectionScreen`. |
| **Sync** | Registro de nota en modo offline. | Local DB: `PENDING_INSERT`, SyncManager: `Idle`. |
| **Sync** | Recuperación de conexión tras 2 días. | Ejecución de `syncAll()` procesando colas por orden de `updated_at`. |
| **Roles** | Padre intenta aprobar una justificación. | Excepción de Seguridad / RLS (Mocked). |
| **UI** | Cambio de orientación de pantalla. | El `ScreenModel` debe persistir el estado (Voyager behavior). |

---

## 6. Ejecución y CI/CD
*   Los tests se ejecutarán en cada Pull Request mediante **GitHub Actions**.
*   Comando base: `./gradlew allTests` (ejecuta tests en JVM, Android y simuladores iOS).
