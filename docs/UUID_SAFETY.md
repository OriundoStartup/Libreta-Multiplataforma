# 🛡️ Guía de Seguridad y Resiliencia de UUIDs

Esta guía establece el estándar obligatorio para el manejo de identificadores (UUID) en el proyecto, con el fin de erradicar permanentemente el error de base de datos **22P02** (invalid input syntax for type uuid).

## 🚫 Prohibiciones Estrictas

1.  **NO usar strings vacíos (`""`)** como valores por defecto para IDs.
2.  **NO usar `.orEmpty()`** sobre variables que representen un UUID.
3.  **NO usar el operador elvis con un string vacío (`?: ""`)** para resolver nulidad en IDs.
4.  **NO pasar strings crudos (`String`)** entre capas del dominio o repositorios.

## ✅ Estándar Obligatorio: `UuidString`

Para garantizar la integridad, se debe utilizar la clase `UuidString` (Value Class) en todos los modelos de dominio y firmas de repositorio.

### 1. En el Dominio
Todos los IDs en los modelos de datos deben ser de tipo `UuidString` o `UuidString?`.

```kotlin
// CORRECTO
data class Student(
    val id: UuidString,
    val courseId: UuidString?
)

// INCORRECTO
data class Student(
    val id: String = "" // Jamás inicializar con ""
)
```

### 2. Conversión en el Perímetro (UI / Navegación)
Cualquier entrada externa debe ser validada inmediatamente usando la extensión `.toUuidOrNull()`.

```kotlin
// CORRECTO (en ScreenModel o ViewModel)
fun onIdReceived(idFromInput: String) {
    val safeId = idFromInput.toUuidOrNull() ?: run {
        AppLogger.uuid("flow", "field", idFromInput, "INVALID")
        return // O manejar error de UI
    }
    // Continuar con safeId (que es UuidString)
}

// INCORRECTO
fun onIdReceived(idFromInput: String) {
    repository.fetch(idFromInput) // Riesgo de crash si idFromInput es ""
}
```

### 3. Serialización (DTOs)
Los DTOs deben usar `String?` con `@EncodeDefault(Mode.NEVER)` para que `kotlinx.serialization` omita el campo en el JSON si es nulo, permitiendo que Supabase genere el ID automáticamente.

```kotlin
@Serializable
data class AttendanceDto(
    @EncodeDefault(Mode.NEVER) val id: String? = null,
    @SerialName("student_id") val studentId: String
)
```

## 🔍 Automatización (CI)

El proyecto incluye un script de "Anti-Pattern Lint" (`scripts/check-uuid-safety.sh`) que falla el build si detecta:
-   Uso de `orEmpty()` en campos sospechosos de ser IDs.
-   Uso de `?: ""` en mappers o repositorios.

---
**Recuerda:** Un ID inválido en el cliente es un bug de red en el backend. Filtra siempre en la entrada.
