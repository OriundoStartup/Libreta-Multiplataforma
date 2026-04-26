# CLAUDE.md — shared/

> Reglas específicas del módulo compartido. Leer junto al CLAUDE.md raíz.

---

## 1. MODELOS DE DOMINIO (fuente de verdad)

Definidos en `src/commonMain/kotlin/com/tuapp/libreta/domain/model/`:

```kotlin
// Models.kt

data class Profile(
    val id: UuidString,
    val role: UserRole,
    val fullName: String
    // email NO está aquí — viene del Auth de Supabase directamente
)

data class Course(
    val id: String,          // Viene de Supabase como String (UUID serializado)
    val teacherId: String,
    val name: String,
    val description: String?,
    val subject: String?,
    val grade: String?,
    val schoolName: String?, // SOLO para display — NO persistir en DB (viola 3NF)
    val inviteCode: String,
    val isActive: Boolean,
    val createdAt: String
)

data class ClassRoom(
    val id: UuidString,
    val classCode: String,
    val name: String,
    val teacherId: UuidString,
    val schoolId: UuidString? = null,
    val isHeadTeacher: Boolean = true
)

data class Student(
    val id: UuidString,
    val fullName: String,
    val courseId: UuidString,
    val parentId: UuidString,
    val attendancePercentage: Double = 0.0   // CALCULADO — no almacenar en DB
)

data class Attendance(
    val id: UuidString? = null,
    val studentId: UuidString,
    val date: String,              // formato ISO: "2024-03-15"
    val status: AttendanceStatus,
    val justificationId: UuidString? = null
)

data class Justification(
    val id: UuidString? = null,
    val studentId: UuidString,
    val date: Long,                // epoch ms
    val reason: String,
    val status: JustificationStatus
)

data class Message(
    val id: UuidString? = null,
    val senderId: UuidString,
    val receiverId: UuidString?,   // null = aviso de clase (broadcast)
    val content: String
)

data class School(
    val id: UuidString,
    val name: String,
    val address: String
)

data class CourseAssignment(
    val id: UuidString? = null,
    val teacherId: UuidString,
    val courseId: UuidString,
    val schoolId: UuidString,
    val isHeadTeacher: Boolean
)

data class InvitationCode(
    val code: String,              // 6 caracteres alfanuméricos uppercase
    val studentId: UuidString,
    val teacherId: UuidString,
    val claimedBy: UuidString?,    // null = no reclamado aún
    val expiresAt: Long            // epoch ms
)
```

```kotlin
// Enums.kt — valores exactos, no inventar nuevos sin migración de BD
enum class UserRole           { TEACHER, PARENT }
enum class AttendanceStatus   { PRESENT, ABSENT, LATE }
enum class JustificationStatus{ PENDING, APPROVED, REJECTED }
enum class SyncStatus         { SYNCED, PENDING_INSERT, PENDING_UPDATE, PENDING_DELETE }
```

---

## 2. INTERFACES DE REPOSITORIOS (contratos fijos)

```kotlin
// Repositories.kt — no cambiar firmas sin actualizar TODAS las implementaciones

interface ProfileRepository {
    fun getAll(): Flow<List<Profile>>
    suspend fun save(profile: Profile)
    suspend fun delete(id: UuidString)
}

interface ClassRoomRepository {
    fun getAll(): Flow<List<ClassRoom>>
    suspend fun save(classRoom: ClassRoom)
    suspend fun delete(id: UuidString)
}

interface StudentRepository {
    fun getStudentsByClass(classId: UuidString): Flow<List<Student>>
    fun getStudentsByParent(parentId: UuidString): Flow<List<Student>>
    suspend fun saveStudent(student: Student)
    suspend fun deleteStudent(id: UuidString)
}

interface AttendanceRepository {
    fun getByStudent(studentId: UuidString): Flow<List<Attendance>>
    suspend fun save(attendance: Attendance)
    suspend fun delete(id: UuidString)
}

interface JustificationRepository {
    fun getByStudent(studentId: UuidString): Flow<List<Justification>>
    suspend fun save(justification: Justification)
    suspend fun delete(id: UuidString)
}

interface MessageRepository {
    suspend fun getInbox(currentUserId: String): List<MessageThread>
    suspend fun getConversation(currentUserId: String, contactId: String): List<Message>
    suspend fun sendMessage(receiverId: String, content: String): Result<Unit>
    suspend fun markAsRead(senderId: String, currentUserId: String)
    suspend fun save(message: Message)
}

interface InvitationCodeRepository {
    suspend fun generate(studentId: UuidString, teacherId: UuidString): InvitationCode
    suspend fun claim(code: String, parentId: UuidString): Result<InvitationCode>
    fun getByTeacher(teacherId: UuidString): Flow<List<InvitationCode>>
}

interface CourseAssignmentRepository {
    fun getByTeacher(teacherId: UuidString): Flow<List<CourseAssignment>>
    suspend fun assign(assignment: CourseAssignment)
    suspend fun generateColleagueInvite(courseId: UuidString, schoolId: UuidString, issuedByTeacherId: UuidString): String
}

interface SchoolRepository {
    fun getByTeacher(teacherId: UuidString): Flow<List<School>>
}

interface CommunicationRepository {
    suspend fun sendGeneralNotice(senderId: UuidString, classId: UuidString, content: String)
    fun getByClass(classId: UuidString): Flow<List<Message>>
}

// CoursesRepository — interface en SupabaseCoursesRepository.kt (distinta del pattern normal)
interface CoursesRepository {
    suspend fun createCourse(name: String, description: String?, subject: String?, grade: String?, schoolName: String?): Result<Course>
    suspend fun getTeacherCourses(): Result<List<Course>>
    suspend fun getCourseByInviteCode(code: String): Result<Course?>
    suspend fun enrollStudent(courseId: String, studentName: String, studentRut: String?): Result<Unit>
    suspend fun updateCourse(courseId: String, name: String, description: String?, subject: String?, grade: String?): Result<Course>
}
```

---

## 3. DTOs SUPABASE (mapeo tabla ↔ Kotlin)

Todos en `data/remote/dto/SupabaseDtos.kt`. Usar `@EncodeDefault(NEVER)` en `id` para evitar enviar null en inserts.

```kotlin
ProfileSupabaseDto      → tabla: profiles      (id, full_name, email, role)
ClassRoomSupabaseDto    → tabla: classrooms     (id, name, grade, section, school_id)
StudentSupabaseDto      → tabla: students       (id, first_name, last_name, class_id, parent_id)
AttendanceSupabaseDto   → tabla: attendance     (id, student_id, date, status)
JustificationSupabaseDto→ tabla: justifications (id, student_id, date, reason, status)
CourseSupabaseDto       → tabla: courses        (id, name, teacher_id, grade, description, subject, is_active, invite_code)
EnrollmentSupabaseDto   → tabla: enrollments    (id, course_id, parent_id, student_name, student_rut, enrolled_at)
MessageSupabaseDto      → tabla: messages       (id, sender_id, receiver_id, message_text, created_at, read_at)
CommunicationSupabaseDto→ tabla: communications (id, sender_id, course_id, message_text, category, created_at)
InvitationCodeSupabaseDto→tabla: invitation_codes(code, student_id, teacher_id, claimed_by, expires_at)
CourseAssignmentSupabaseDto→tabla: course_assignments(id, course_id, teacher_id, school_id)
SchoolSupabaseDto       → tabla: schools        (id, name, address)
```

**Regla:** Nunca acceder a la BD con campos que no existan en el DTO correspondiente.

---

## 4. MAPPERS DTO ↔ DOMINIO

En `data/remote/dto/SupabaseMappers.kt`:

```kotlin
// DTO → Dominio
fun AttendanceSupabaseDto.toDomain() = Attendance(
    id = id?.toUuidOrNull(),
    studentId = UuidString(studentId),
    date = date,                              // "YYYY-MM-DD"
    status = runCatching { AttendanceStatus.valueOf(status.uppercase()) }
               .getOrElse { AttendanceStatus.ABSENT },
    justificationId = null
)

fun StudentSupabaseDto.toDomain() = Student(
    id = UuidString(id ?: ""),
    fullName = "$firstName $lastName",
    courseId = UuidString(classId ?: ""),     // class_id → courseId
    parentId = UuidString(parentId ?: ""),
    attendancePercentage = 0.0                // nunca viene de la BD
)

fun JustificationSupabaseDto.toDomain() = Justification(
    id = id?.toUuidOrNull(),
    studentId = UuidString(studentId),
    date = date.toLongOrNull() ?: 0L,         // date en BD es string epoch
    reason = reason,
    status = runCatching { JustificationStatus.valueOf(status.uppercase()) }
               .getOrElse { JustificationStatus.PENDING }
)

fun MessageSupabaseDto.toDomain() = Message(
    id = id?.toUuidOrNull(),
    senderId = senderId?.toUuidOrNull() ?: UuidString("00000000-0000-0000-0000-000000000000"),
    receiverId = receiverId?.toUuidOrNull(),
    content = messageText ?: ""
)

// Dominio → DTO
fun Attendance.toSupabaseDto() = AttendanceSupabaseDto(
    id = id?.value,
    studentId = studentId.value,
    date = date,
    status = status.name,
    courseId = null                           // courseId NO se almacena (3NF)
)

fun Student.toSupabaseDto() = StudentSupabaseDto(
    id = id.value,
    firstName = fullName.split(" ").firstOrNull() ?: "",
    lastName = fullName.split(" ").drop(1).joinToString(" "),
    classId = courseId.value,
    parentId = parentId.value
)
```

---

## 5. CASOS DE USO — REGLAS

Un UseCase = una operación de negocio. Estructura:

```kotlin
class NombreUseCase(private val repo: NombreRepository) {
    suspend operator fun invoke(param: Tipo): ResultadoTipo = repo.metodo(param)
}
// o Flow:
class NombreUseCase(private val repo: NombreRepository) {
    operator fun invoke(param: Tipo): Flow<ResultadoTipo> = repo.metodo(param)
}
```

**UseCases existentes:**
```
StudentUseCases.kt       → GetStudentsByClassUseCase, DeleteStudentUseCase
GetCourseAnalyticsUseCase.kt → CourseAnalytics (totalStudents, present%, absent, atRisk, last5Days)
JustificationUseCases.kt → SubmitJustificationUseCase, GetPendingJustificationsUseCase, ReviewJustificationUseCase
MessageUseCases.kt       → GetInboxUseCase, GetConversationUseCase, SendMessageUseCase, MarkAsReadUseCase
InvitationUseCases.kt    → GenerateInvitationCodeUseCase, ClaimInvitationCodeUseCase, GetTeacherInvitationsUseCase
```

**Reglas de negocio en UseCases:**
- `SubmitJustification`: estado siempre `PENDING` al crear
- `ReviewJustification`: aprobado→`APPROVED`, rechazado→`REJECTED`; también envía `Message` de notificación
- `ClaimInvitationCode`: usa RPC `claim_invitation_code` en Supabase (atómica)
- `GetCourseAnalytics`: `atRisk` = alumnos con asistencia < 75%; `last5Days` = datos reales agrupados por fecha ISO
- `GenerateInvitationCode`: código de 6 caracteres, expira en 7 días, generado con RPC del servidor

---

## 6. INYECCIÓN DE DEPENDENCIAS

### AppModule.kt (completo, no modificar sin actualizar aquí):
```kotlin
val appModule = module {
    // Auth
    single { SupabaseAuthService(get()) }

    // Repositorios Supabase (single = stateless, 1 instancia compartida)
    single<AttendanceRepository>       { SupabaseAttendanceDataSource(get()) }
    single<InvitationCodeRepository>   { SupabaseInvitationRepository(get()) }
    single<SchoolRepository>           { SupabaseSchoolRepository(get()) }
    single<CommunicationRepository>    { SupabaseCommunicationRepository(get()) }
    single<CourseAssignmentRepository> { SupabaseCourseAssignmentRepository(get()) }
    single<CoursesRepository>          { SupabaseCoursesRepository(get()) }
    single<StudentRepository>          { SupabaseStudentRepository(get()) }
    single<JustificationRepository>    { SupabaseJustificationRepository(get()) }
    single<MessageRepository>          { SupabaseMessageRepository(get()) }

    // UseCases (factory = nueva instancia, sin estado persistido)
    factory { GetStudentsByClassUseCase(get()) }
    factory { DeleteStudentUseCase(get()) }
    factory { GetCourseAnalyticsUseCase(get(), get()) }
    factory { SubmitJustificationUseCase(get()) }
    factory { GetPendingJustificationsUseCase(get()) }
    factory { ReviewJustificationUseCase(get(), get()) }
    factory { GetInboxUseCase(get()) }
    factory { GetConversationUseCase(get()) }
    factory { SendMessageUseCase(get()) }
    factory { MarkAsReadUseCase(get()) }
    factory { GenerateInvitationCodeUseCase(get()) }
    factory { ClaimInvitationCodeUseCase(get()) }
    factory { GetTeacherInvitationsUseCase(get()) }

    // ScreenModels (factory = nueva instancia por pantalla)
    factory { LoginScreenModel(get()) }
    factory { RoleSelectionScreenModel(get(), get()) }
    factory { TeacherDashboardScreenModel(get(), get(), get(), get()) }
    // ... etc.
}
```

### PlatformModule (expect/actual):
- Android: `SupabaseClient` con PKCE + `scheme="org.orinundo"` + `SQLiteDriver` + `DataSeeder`
- iOS: `SupabaseClient` con PKCE + `SQLiteNativeDriver`
- Web: `SupabaseClient` con PKCE + `InMemoryProfileRepository` + `InMemoryClassRoomRepository`

---

## 7. UI — PATRONES COMPOSE MULTIPLATFORM

### Estados de pantalla (sealed class estándar):
```kotlin
sealed class NombreUiState {
    data object Loading : NombreUiState()
    data class Success(val data: TipoDato) : NombreUiState()
    data class Error(val message: String) : NombreUiState()
}
```

### Pantallas existentes y sus ScreenModels:
| Screen | ScreenModel | Ruta AppNavigation |
|---|---|---|
| `LoginScreen` | `LoginScreenModel` | `startDestination()` |
| `RoleSelectionScreen` | `RoleSelectionScreenModel` | — |
| `TeacherDashboardScreen` | `TeacherDashboardScreenModel` | `teacherDashboard()` |
| `ParentDashboardScreen` | `ParentDashboardScreenModel` | `parentDashboard(userId)` |
| `StudentListScreen(classId)` | `StudentListScreenModel` | `studentList(classId)` |
| `StudentDetailScreen(...)` | `StudentDetailScreenModel` | `studentDetail(...)` |
| `AttendanceScreen(courseId, name)` | `AttendanceScreenModel` | `attendance(courseId, name)` |
| `AttendanceHistoryScreen(studentId, name)` | `AttendanceHistoryScreenModel` | `attendanceHistory(...)` |
| `JustificationScreen(...)` | `JustificationScreenModel` | `justificationForm(...)` |
| `JustificationReviewScreen` | — | — |
| `MessageListScreen` | `MessageScreenModel` | `messages()` |
| `MessageDetailScreen(contactId, name)` | `MessageScreenModel` | `messageDetail(...)` |
| `NewMessageScreen` | `MessageScreenModel` | `newMessage()` |
| `NoticeListScreen` | `NoticeListScreenModel` | — |
| `ComposeNoticeScreen` | `NoticeScreenModel` | — |
| `CourseStatsScreen(classId, name)` | `StatsScreenModel` | `courseStats(...)` |
| `EnrollmentScreen` | `EnrollmentScreenModel` | — |
| `CourseEditScreen` | `CourseEditScreenModel` | — |
| `ProfileScreen` | `ProfileScreenModel` | `profile()` |

### Componentes reutilizables existentes:
- `StudentCard` — tarjeta de alumno con nombre y estado
- `StatusCard` — tarjeta de estadísticas
- `TimelineItem` — ítem de historial con fecha
- `ShimmerCard` — placeholder de carga animado

### Reglas UI:
- Usar `Material3` (`androidx.compose.material3.*`)
- No hardcodear colores — usar `MaterialTheme.colorScheme.*`
- Textos en español (es-ES / es-419)
- Soporte de pantallas pequeñas (minSdk 24 = Android 7)
- `LazyColumn` para listas, nunca `Column` con scroll para >10 items

---

## 8. AUTENTICACIÓN — FLUJO

```
1. LoginScreen muestra botón "Continuar con Google"
2. rememberGoogleAuthLauncher() — plataforma-específico:
   - Android: abre CustomTab via AuthService.signInWithGoogle()
   - iOS: abre SafariViewController
   - Web: redirige window.location.href a URL OAuth
3. Supabase maneja PKCE y retorna sesión
4. LoginScreenModel observa sessionStatus via SupabaseAuthService.sessionStatusFlow
5. Si Authenticated → checkUserStatus() → si tiene rol → navegar a Dashboard
6. Si sin rol → navegar a RoleSelectionScreen
```

**Deep link Android:** `org.orinundo://login-callback` (registrado en AndroidManifest)

---

## 9. UTILIDADES CLAVE

### UuidString (value class)
```kotlin
@JvmInline
value class UuidString(val value: String) {
    // Valida que sea UUID v4 válido
}

// Helpers:
fun String.toUuidOrNull(): UuidString?
fun String.toUuidOrThrow(): UuidString
```

### AppLogger
```kotlin
AppLogger.d("Tag", "Mensaje debug")
AppLogger.e("Tag", "Mensaje error", throwable?)
AppLogger.w("Tag", "Mensaje warning")
```

### TimeUtil
```kotlin
fun currentEpochMs(): Long
fun epochMsToIso(epochMs: Long): String   // → "2024-03-15T10:30:00Z"
fun formatDisplayDate(isoDate: String): String
```

### DispatcherProvider (expect/actual por plataforma)
```kotlin
// commonMain (expect):
expect fun getIoDispatcher(): CoroutineDispatcher

// Android: Dispatchers.IO
// iOS: Dispatchers.Default
// Web: Dispatchers.Default
```
