# Auditoría Técnica: Sincronización, Concurrencia y Esquema

## 1. Diagnóstico del Flujo de Sincronización Actual
Actualmente, el `SyncManager` utiliza una estrategia de **"Ciego-Push"**:
*   **Problema:** Envía datos locales (`upsert`) sin verificar si la versión en el servidor es más reciente.
*   **Riesgo de Concurrencia:** Si un profesor edita una nota desde la Web y luego sincroniza su App móvil que tenía datos antiguos, **se perderá la edición de la Web** (Last Write Wins descontrolado).

---

## 2. Propuesta de Mejora de Esquema (DB)
Para solucionar la concurrencia y mejorar la trazabilidad, se deben aplicar los siguientes cambios tanto en `LibretaApp.sq` como en Supabase:

### A. Control de Versiones y Soft-Delete
1.  **`server_version` (INT):** Contador de cambios para cada registro.
2.  **`is_deleted` (BOOL):** En lugar de borrar registros físicos, marcamos como eliminados para que el cliente sepa qué borrar localmente al descargar datos.
3.  **`client_id` (UUID):** Para identificar qué dispositivo originó el cambio y evitar bucles de sincronización.

### B. Tabla de Auditoría Global (Recomendado)
Crear una tabla `sync_metadata` en SQLite para guardar el `last_sync_timestamp`, permitiendo descargas incrementales (solo traer lo nuevo desde la última conexión).

---

## 3. Protocolo de Sincronización Robusto (Step-by-Step)
He rediseñado el flujo para evitar errores de concurrencia:

1.  **PULL (Descarga):** El cliente pide al servidor: *"Dame todo lo modificado después de mi último timestamp"*.
2.  **RESOLVE (Resolución Local):** 
    *   Si el dato local es `SYNCED`, se actualiza con lo del servidor.
    *   Si el dato local es `PENDING_UPDATE`, se compara el `updated_at`. Si el servidor es más nuevo, se marca como **CONFLICTO** o se prioriza el servidor.
3.  **PUSH (Subida):** El cliente envía solo lo que está pendiente, usando un `IF updated_at = original_updated_at` (Optimistic Locking) para asegurar que no sobreescribe cambios ajenos.

---

## 4. Matriz de Riesgos de Concurrencia

| Escenario | Riesgo | Solución Propuesta |
| :--- | :--- | :--- |
| **Edición Dual** | Sobreescritura de datos más recientes. | Optimistic Locking mediante timestamps. |
| **Eliminación remota** | El registro reaparece en el servidor al sincronizar un cliente antiguo. | Implementación de `is_deleted` (Tombstones). |
| **Falla de Red media** | Transacción parcial (ej. nota sube, pero asistencia no). | Uso de transacciones atómicas en SQLDelight y batching en Supabase. |
| **Cambio de Rol** | Acceso a datos antiguos de un rol anterior. | Purga de caché local al detectar cambio de sesión/rol. |

---

## 5. Próximos Pasos Técnicos
1.  **Actualizar `LibretaApp.sq`** para incluir la columna `is_deleted`.
2.  **Modificar `SyncManager`** para implementar la lógica de "Descarga Incremental" antes de la "Subida".
3.  **Refactorizar los Repositorios Symbiotic** para que el `delete` no borre la fila, sino que la marque como `is_deleted = 1`.
