# CLAUDE.md — LibretaMultiplataformAws

> **Guía autoritativa para el agente AI.** Leer este archivo completo antes de cualquier tarea.
> No inventar APIs, tablas, ni patrones que no estén documentados aquí.

---

## 1. QUÉ ES ESTE PROYECTO

Sistema educativo multiplatforma (Android · iOS · Web) para gestión de asistencia y comunicación entre **docentes** y **apoderados**. Backend 100% Supabase (PostgreSQL + Auth + Realtime).

**Usuarios del sistema:**
- `TEACHER` — crea cursos, marca asistencia, revisa justificaciones, envía avisos
- `PARENT` — ve asistencia de sus hijos, envía justificaciones, recibe mensajes

---

## 2. STACK EXACTO (no asumir versiones)

| Librería | Versión | Propósito |
|---|---|---|
| Kotlin | 2.3.10 | Lenguaje principal |
| Compose Multiplatform | 1.7.1 | UI compartida |
| Supabase Kotlin | 3.3.0 | Backend (Auth, Postgrest, Realtime) |
| Ktor | 3.0.1 | HTTP client |
| Koin | 4.0.0 | Inyección de dependencias |
| Voyager | 1.1.0-beta03 | Navegación |
| SQLDelight | 2.3.2 | Base de datos local |
| kotlinx.serialization | 1.7.3 | DTOs JSON |
| kotlinx.datetime | 0.6.0 | Fechas multiplataforma |
| AGP | 8.9.1 | Android Gradle Plugin |
| Android compileSdk | 36 | |
| Android minSdk | 24 | Android 7.0+ |
| Android targetSdk | 35 | Requerido Google Play 2025 |

Versiones definidas en `gradle/libs.versions.toml`. **Nunca hardcodear versiones en build.gradle.**

---

## 3. ESTRUCTURA DE MÓDULOS

```
LibretaMultiplataformAws/
├── composeApp/                  # Entry points por plataforma + configuración Android
│   ├── src/androidMain/         # MainActivity, LibretaApplication, AndroidManifest
│   ├── src/iosMain/             # MainViewController
│   ├── src/wasmJsMain/          # main.kt web
│   ├── src/commonMain/          # App.kt (root composable)
│   └── src/webMain/resources/   # index.html, styles.css, manifest.json
│
├── shared/                      # TODO el código de negocio compartido
│   └── src/
│       ├── commonMain/          # Dominio, datos, UI, presentación, navegación, DI
│       ├── androidMain/         # Implementaciones Android (SQLite driver, dispatchers)
│       ├── iosMain/             # Implementaciones iOS
│       └── wasmJsMain/          # Implementaciones Web (InMemory repos, dispatchers)
│
├── supabase/migrations/         # SQL autoritativo del schema (ejecutar en orden)
│   ├── 001_multi_school.sql
│   └── 002_normalize_3nf.sql
│
└── gradle/libs.versions.toml   # Catálogo central de versiones
```

### Packages en `shared/src/commonMain/`:
```
com.tuapp.libreta/
├── data/
│   ├── remote/          # Repositorios Supabase + AuthService
│   │   └── dto/         # DTOs @Serializable
│   ├── repository/      # Implementaciones locales (SQLDelight / InMemory)
│   ├── util/            # UuidString, TimeUtil, Logger, DataSeeder, Mappers
│   └── mapper/          # Mappers dominio ↔ local DB
├── domain/
│   ├── model/           # Modelos de dominio (Models.kt, Enums.kt)
│   ├── repository/      # Interfaces de repositorios (Repositories.kt)
│   └── usecase/         # Casos de uso (lógica de negocio)
├── presentation/        # ScreenModels (MVVM con Voyager)
├── ui/
│   ├── screens/         # Composables de pantalla
│   └── components/      # Componentes reutilizables
├── navigation/          # AppNavigation.kt
└── di/                  # AppModule.kt + PlatformModule.kt (expect/actual)
```

---

## 4. ARQUITECTURA — REGLAS QUE NUNCA ROMPER

### Capas y dependencias (flujo unidireccional):
```
UI (screens) → Presentation (ScreenModels) → Domain (UseCases) → Data (Repos) → Supabase
```

**Prohibido:**
- UI accediendo a repositorios directamente (siempre via ScreenModel → UseCase)
- UseCase con lógica de UI (nada de strings de colores, formatos visuales)
- ScreenModel importando clases de `data.remote` (sólo usa interfaces del `domain`)
- Datos calculados/derivados almacenados en BD (ver Sección 8 Reglas DB)

### ScreenModel pattern (Voyager):
```kotlin
class MiScreenModel(
    private val miUseCase: MiUseCase
) : ScreenModel {

    private val _state = MutableStateFlow<MiUiState>(MiUiState.Loading)
    val state: StateFlow<MiUiState> = _state.asStateFlow()

    init { cargarDatos() }

    fun accion() {
        screenModelScope.launch {
            _state.value = MiUiState.Loading
            runCatching { miUseCase() }
                .onSuccess { _state.value = MiUiState.Success(it) }
                .onFailure { _state.value = MiUiState.Error(it.message ?: "Error") }
        }
    }
}
```

### Screen pattern (Voyager + Koin):
```kotlin
// Cada Screen es un object o data class Serializable
object MiScreen : Screen {
    @Composable
    override fun Content() {
        val model = rememberScreenModel<MiScreenModel>()
        val state by model.state.collectAsState()
        // UI aquí
    }
}
```

---

## 5. PLATAFORMAS — QUÉ VA DÓNDE

| Código | Plataforma | Dispatcher IO | Supabase Auth | Persistencia local |
|---|---|---|---|---|
| `androidMain` | Android | `Dispatchers.IO` | PKCE + CustomTabs | SQLDelight (SQLite) |
| `iosMain` | iOS | `Dispatchers.Default` | PKCE + SafariVC | SQLDelight (native) |
| `wasmJsMain` | Web | `Dispatchers.Default` | PKCE (redirect) | InMemory (sin SQLite) |

**Regla:** Código platform-specific va en `*Main` con `expect/actual`. Nada Android en `commonMain`.

**Web — limitaciones conocidas:**
- Sin SQLite: `ProfileRepository` y `ClassRoomRepository` son `InMemoryProfileRepository` / `InMemoryClassRoomRepository`
- Los datos en memoria se pierden al recargar la página (comportamiento esperado)
- Realtime aún no habilitado en `MessageScreenModel` (TODO pendiente)

---

## 6. CONVENCIONES DE CÓDIGO

### Kotlin
- Kotlin idiomático; no Java-style verboso
- `data class` para modelos inmutables; `sealed class` para estados UI
- `Flow<List<T>>` para listas reactivas; `suspend fun` para operaciones únicas
- `runCatching { }` para capturar errores sin try/catch verboso
- `UuidString` (value class) para IDs — **nunca usar `String` raw para UUIDs**

### Nombrado
| Elemento | Convención | Ejemplo |
|---|---|---|
| ScreenModel | `NombreScreenModel` | `AttendanceScreenModel` |
| UseCase | `VerbNombreUseCase` | `GetStudentsByClassUseCase` |
| Repository interface | `NombreRepository` | `StudentRepository` |
| Supabase impl | `SupabaseNombreRepository` | `SupabaseStudentRepository` |
| DTO | `NombreSupabaseDto` | `AttendanceSupabaseDto` |
| Screen | `NombreScreen` | `AttendanceScreen` |
| Estado UI | `NombreUiState` (sealed) | `AttendanceUiState` |

### UuidString — OBLIGATORIO para IDs
```kotlin
// CORRECTO
val studentId: UuidString = UuidString("550e8400-...")
supabase.from("students").select { filter { eq("id", studentId.value) } }

// INCORRECTO — nunca usar String raw para UUID
val studentId: String = "550e8400-..."
```

### Serialization de DTOs
```kotlin
@Serializable
data class MiDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("campo_snake_case") val campoKotlin: String
)
```

---

## 7. ENUMS DEL DOMINIO (valores exactos, no inventar)

```kotlin
enum class UserRole        { TEACHER, PARENT }
enum class AttendanceStatus{ PRESENT, ABSENT, LATE }
enum class JustificationStatus { PENDING, APPROVED, REJECTED }
enum class SyncStatus      { SYNCED, PENDING_INSERT, PENDING_UPDATE, PENDING_DELETE }
```

**En base de datos:** almacenados como `TEXT` con el mismo nombre exacto del enum (ej: `"PRESENT"`, `"TEACHER"`).
**Mapeo seguro:** siempre usar `runCatching { Enum.valueOf(str) }.getOrElse { default }`.

---

## 8. REGLAS DE BASE DE DATOS (3NF — no romper)

Ver `supabase/CLAUDE.md` para schema completo.

**Reglas absolutas:**
1. **Nunca almacenar datos derivables por JOIN** (ej: no guardar `school_name` en `courses` — viene de `JOIN schools`)
2. **Nunca duplicar course/class en profiles** — el curso de un docente está en `course_assignments` o `courses.teacher_id`
3. **Siempre usar UUID FK** — nunca `TEXT` para referencias entre tablas
4. **Toda FK tiene índice** — crear `idx_tabla_columna` en cada migración nueva
5. **Toda tabla tiene RLS** — habilitar y definir policies antes de usar en producción
6. **Operaciones race-condition** van en RPCs atómicas (ej: `claim_invitation_code`)

**Para nuevas migraciones:** numeración secuencial `00N_descripcion.sql`, ejecutar en orden.

---

## 9. INYECCIÓN DE DEPENDENCIAS (Koin)

**AppModule** (commonMain) — contiene todo lo multiplatforma:
- Repositorios Supabase como `single` (stateless, 1 instancia)
- UseCases como `factory` (nueva instancia por pantalla)
- ScreenModels como `factory` (nueva instancia por pantalla)

**PlatformModule** (expect/actual) — por plataforma:
- `SupabaseClient` (single, configurado por plataforma)
- Driver SQLDelight (single, por plataforma)
- Repos locales (single)

**Regla:** Nunca usar `get()` de Koin fuera del módulo DI. Inyectar via constructor.

---

## 10. MANEJO DE ERRORES — ESTÁNDAR

```kotlin
// En repositorios Supabase — siempre try/catch, siempre emit emptyList() en Flow
override fun getByStudent(id: UuidString): Flow<List<T>> = flow {
    try {
        emit(supabase.from("tabla").select { ... }.decodeList<Dto>().map { it.toDomain() })
    } catch (e: Exception) {
        AppLogger.e("Tag", "Mensaje: ${e.message}")
        emit(emptyList())
    }
}

// En ScreenModels — usar runCatching y mapear a estado de error
screenModelScope.launch {
    _state.value = UiState.Loading
    runCatching { useCase() }
        .onSuccess { _state.value = UiState.Success(it) }
        .onFailure { _state.value = UiState.Error(it.message ?: "Error desconocido") }
}
```

**Nunca:** propagar excepciones sin atrapar en Flows, ni dejar `println()` en producción (usar `AppLogger`).

---

## 11. NAVEGACIÓN

Toda navegación pasa por `AppNavigation` (objeto singleton en `navigation/AppNavigation.kt`).

```kotlin
// Para navegar
navigator.push(AppNavigation.attendance(courseId, courseName))
navigator.push(AppNavigation.studentDetail(studentId, name, courseId, parentId))

// Para volver
navigator.pop()
```

**Nunca instanciar Screens directamente** fuera de `AppNavigation` — centralizar para evitar parámetros dispersos.

---

## 12. QUÉ NO HACER (anti-patterns prohibidos)

| ❌ Prohibido | ✅ Correcto |
|---|---|
| `String` raw para UUIDs | `UuidString` siempre |
| SELECT * en Supabase | Especificar columnas: `.select("id,name,status")` |
| Query sin `.limit()` para listas grandes | `.limit(100)` en inbox/conversations |
| N+1 queries en loops | `async/await` paralelo o RPC batch |
| Datos ficticios en analytics | Calcular desde datos reales de la BD |
| `Dispatchers.Unconfined` en web | `Dispatchers.Default` |
| Hardcodear versiones de dependencias | Usar `libs.xxx` del catálogo |
| `school_name` en tabla `courses` | JOIN con `schools` |
| `course_id` en `profiles` | Usar `course_assignments` |
| `println()` en código de producción | `AppLogger.d/e/w()` |
| SELECT+UPDATE separados para operaciones atómicas | RPC con transacción |
| `claimedBy == null` check en Kotlin antes de UPDATE | RPC `claim_invitation_code` |

---

## 13. FLUJO PARA AGREGAR NUEVA FUNCIONALIDAD

1. **DB** (si necesita nueva tabla/columna): agregar migración `00N_descripcion.sql` en `supabase/migrations/`, con índices y RLS
2. **Modelo de dominio**: agregar/modificar en `domain/model/Models.kt`
3. **Interfaz de repositorio**: agregar método en `domain/repository/Repositories.kt`
4. **DTO**: agregar en `data/remote/dto/SupabaseDtos.kt`
5. **Mapper**: agregar en `data/remote/dto/SupabaseMappers.kt`
6. **Implementación Supabase**: implementar en `data/remote/Supabase*.kt`
7. **UseCase**: crear en `domain/usecase/*.kt`
8. **Registrar en DI**: `AppModule.kt`
9. **ScreenModel**: crear en `presentation/`
10. **Screen**: crear en `ui/screens/`
11. **Ruta**: agregar en `AppNavigation.kt`

---

## 14. SEGURIDAD

- `SUPABASE_URL` y `SUPABASE_KEY` viven en `local.properties` (no en el repo)
- `BuildKonfig` inyecta las credenciales en tiempo de build
- `SUPABASE_KEY` es la `anon key` — es pública por diseño, pero **RLS** protege los datos
- Toda tabla en Supabase **debe** tener `ENABLE ROW LEVEL SECURITY`
- Rotar la key si se compromete en el repositorio git

---

## 15. TESTS

Correr tests multiplatforma:
```bash
./gradlew :shared:allTests
./gradlew :shared:wasmJsTest
```

Tests existentes en `shared/src/commonTest/`:
- `GetCourseAnalyticsUseCaseTest`
- `ReviewJustificationUseCaseTest`
- `SendMessageUseCaseTest`
- `SubmitJustificationUseCaseTest`
- `SupabaseIntegrationTest` (requiere `.env` con credenciales de staging)
- `DtoSerializationTest`
- `UuidStringTest`

**FakeRepositories** disponibles en `commonTest/kotlin/.../test/FakeRepositories.kt` para usar en tests unitarios sin Supabase.
