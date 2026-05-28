# 🗺️ ROADMAP MAESTRO — LibretApp a Producción

> Estado: **esqueleto generado**. Ningún archivo de producción funcional fue modificado.
> Próximo paso: rellenar TODOs por fase en orden, validar en `prod-hardening` branch, mergear a `production`.

---

## 📦 INVENTARIO DE ARCHIVOS GENERADOS

### FASE 0 — Blindaje + Observabilidad
| Archivo | Propósito |
|---|---|
| `composeApp/src/wasmJsMain/resources/health.html` | Endpoint `/health.html` con versión, commit, build time |
| `shared/.../data/util/CrashReporter.kt` | Interfaz + NoOp para reporter remoto |
| `shared/.../data/util/AppMetrics.kt` | Stub de métricas (referenciado por `Logger.kt`) |

### FASE 1 — Schema Alignment
| Archivo | Propósito |
|---|---|
| `supabase/migrations/003_schema_alignment.sql` | Vista `students_legacy`, función `audit_schema_drift` |
| `shared/.../data/remote/dto/SupabaseDtosV2.kt` | DTOs alineados con schema real post-002 |
| `shared/src/jvmTest/.../contract/SchemaContractTest.kt` | Test que falla si hay drift DTO↔BD |

### FASE 2 — UuidString + Outcome
| Archivo | Propósito |
|---|---|
| `shared/.../data/util/UuidStringV2.kt` | `UuidV2` sellado con validación estricta |
| `shared/.../domain/util/Outcome.kt` | Sealed Loading/Success/Failure tri-modal |

### FASE 3 — Sync v2
| Archivo | Propósito |
|---|---|
| `supabase/migrations/004_sync_metadata.sql` | server_version, deleted_at, RPCs sync_pull/push/soft_delete |
| `shared/.../data/sync/SyncManagerV2.kt` | Clase PULL→MERGE→PUSH→CLEANUP behind flag |
| `shared/.../sqldelight/.../SyncMetadata.sq` | Tabla local de last_pull_at por tabla |

### FASE 4 — Consolidación
| Archivo | Propósito |
|---|---|
| `supabase/migrations/005_unify_enrollment.sql` | Trigger enrollment→student automático |
| `shared/.../presentation/AuthFlow.kt` | State machine que reemplaza pirámide `if` en App.kt |

### FASE 5 — Tests
| Archivo | Propósito |
|---|---|
| `shared/src/commonTest/.../SupabaseMappersV2Test.kt` | Tests de mappers V2 |
| `shared/src/commonTest/.../SyncManagerV2Test.kt` | Tests de PULL/MERGE/PUSH |
| `shared/src/commonTest/.../AuthFlowTest.kt` | Tests del state machine de auth |
| `supabase/tests/rls_policies_test.sql` | Tests pgTAP de RLS policies |

### FASE 6 — DX & Ops
| Archivo | Propósito |
|---|---|
| `gradle-vercel.properties` | Properties solo para CI Vercel |
| `shared/.../data/remote/SupabaseConfigValidation.kt` | `SupabaseConfig.validate()` cross-platform |

---

## 🚦 ORDEN DE EJECUCIÓN RECOMENDADO

1. **Semana 1** — FASE 0
   - Crear branch `prod-hardening` desde `main`.
   - Implementar `CrashReporter` real (Sentry / GlitchTip / tabla `crash_logs` en Supabase).
   - Inyectar `CrashReporter` en `AppLogger.e()` via Koin.
   - Configurar `health.html` con valores reales en `build-vercel.sh` (sed con `$VERCEL_GIT_COMMIT_SHA`).
   - Configurar PITR en Supabase (Settings → Database → Point in Time Recovery).
   - Deploy a preview, validar `/health.html` y captura de errores funciona.

2. **Semanas 2-3** — FASE 1
   - Ejecutar `003_schema_alignment.sql` en staging primero, validar `audit_schema_drift()`.
   - Implementar `SupabaseMappersV2.kt` (extension funs DTO V2 ↔ Domain).
   - Migrar `SupabaseStudentRepository` → V2 en un PR aislado.
   - Migrar repos uno por uno (1 PR cada uno).
   - Rellenar `SchemaContractTest` con parser real de schema canonical JSON.
   - Cuando ningún caller use `SupabaseDtos.kt` → marcar `@Deprecated` y borrar en cleanup final.

3. **Semana 4** — FASE 2
   - Migrar call-sites a `UuidV2.of()` / `UuidV2.random()`.
   - Reemplazar fallback de `SupabaseStudentRepository:27`.
   - Refactor de repos remotos para retornar `Flow<Outcome<List<X>>>`.
   - ScreenModels actualizan UiState para distinguir Loading/Success/Failure/Empty.

4. **Semanas 5-7** — FASE 3
   - Ejecutar `004_sync_metadata.sql` en staging — verificar triggers funcionan.
   - Rellenar SQLDelight migrations (`shared/src/commonMain/sqldelight/migrations/1.sqm`) con ALTER TABLE de cada entity.
   - Implementar `SyncManagerV2.syncAttendance()` completo.
   - Activar feature flag `ENABLE_SYNC_V2` solo en builds internas.
   - Validar con 2 devices simultáneos: edit en uno, ver propagación al otro.
   - Tabla por tabla: attendance → students → justifications → grades → ...
   - Cuando todas las tablas migradas y métricas estables → eliminar v1.

5. **Semana 8** — FASE 4
   - Ejecutar `005_unify_enrollment.sql` con backfill validado.
   - Refactor `SupabaseStudentRepository.getStudentsByClass()` para leer de `students`.
   - Implementar `AuthFlow.from(...)` completo.
   - Reemplazar bloque en `App.kt:85-127` por consumo del state machine.

6. **Semana 9** — FASE 5
   - Rellenar todos los `@Ignore` con tests reales.
   - Habilitar pgTAP en Supabase (issue a soporte si necesario).
   - Configurar GitHub Actions: `gradle :shared:allTests` + `psql -f rls_policies_test.sql`.
   - Meta: cobertura ≥ 60% en `domain` + `data`.

7. **Semana 10** — FASE 6
   - Renombrar `gradle.properties` → mantener para dev local con daemon=true.
   - Modificar `build-vercel.sh`: `cp gradle-vercel.properties gradle.properties` antes de `./gradlew`.
   - Invocar `SupabaseConfig.requireValid()` en los 3 puntos de entrada.
   - Versionado: leer `versionName` de `git describe --tags`.
   - Limpiar imports muertos (PlatformModule.ios/wasmJs), reemplazar `println` por `AppLogger.d()`.

---

## 🆘 PLAN DE ROLLBACK POR FASE

| Fase | Cómo deshacer |
|---|---|
| 0 | Eliminar archivos. Sin impacto en BD ni en código de negocio. |
| 1 | `DROP VIEW students_legacy; DROP FUNCTION audit_schema_drift;` Borrar `SupabaseDtosV2.kt`. |
| 2 | Borrar `UuidStringV2.kt` y `Outcome.kt`. No reemplazan código existente hasta que se migre call-sites. |
| 3 | `ENABLE_SYNC_V2 = false` (sin redeploy de BD). Si ya se ejecutó 004: `ALTER TABLE ... DROP COLUMN server_version, deleted_at;` |
| 4 | `DROP TRIGGER trg_enrollment_to_student;` Revertir refactor de `App.kt` (git). |
| 5 | Solo tests, sin riesgo. |
| 6 | Restaurar `gradle.properties` original. |

---

## 🎯 KPIs DE ÉXITO

- ✅ 0 errores 400/PGRST204 por columna inexistente en logs de Supabase (post-FASE 1).
- ✅ 0 reportes de pérdida de datos sync en 60 días (post-FASE 3).
- ✅ MTTR < 15 min con observabilidad (post-FASE 0).
- ✅ Cobertura tests ≥ 60% en `domain` + `data` (post-FASE 5).
- ✅ Build local incremental < 30 s (post-FASE 6).

---

## ⚠️ NOTAS CRÍTICAS

1. **Las migraciones SQL están en modo "dry-run"** — los bloques destructivos están comentados. Descomentarlos solo después de validar en staging y tener snapshot reciente.

2. **`SchemaContractTest` está rojo intencionalmente** — falla hasta que se implemente `loadCanonicalSchema()`. Esto previene falsos verdes durante FASE 1.

3. **`SyncManagerV2.syncAttendance()` lanza `TODO()`** — feature flag `ENABLE_SYNC_V2` debe estar en `false` hasta que el método se implemente.

4. **Coexistencia V1/V2** — los archivos nuevos no rompen los viejos. Es seguro mergear el esqueleto sin migrar call-sites.

5. **CrashReporter en `NoOpCrashReporter`** — el sistema compila, pero crashes no se reportan a ningún backend hasta implementar el `actual` reporter.

---

## 📞 PUNTOS DE DECISIÓN PENDIENTES (consensuar con el equipo)

- [ ] **Backend de crash reporting**: Sentry self-hosted vs GlitchTip vs tabla Supabase propia.
- [ ] **Renombrar `message_text` → `content`** (breaking) vs mantener columna generada.
- [ ] **InvitationCode.target_role / course_id**: ¿añadir a BD o quitar de DTO?
- [ ] **Conflict resolution policy**: server-wins por defecto vs prompt al usuario.
- [ ] **Frecuencia de sync background**: ¿1 min / 5 min / on-demand?
- [ ] **Realtime vs polling**: ¿usar Supabase Realtime para suscripciones live?
