package com.tuapp.libreta.domain.util

/**
 * FASE 2 — Estado de resultado tri-modal para repos y use cases.
 *
 * Reemplaza el anti-patrón `catch { emit(emptyList()) }` que mezcla "no hay datos"
 * con "falló la red". Los ScreenModel obtienen diferenciación real.
 *
 * Uso típico:
 * ```
 * fun observeStudents(): Flow<Outcome<List<Student>>> = flow {
 *     emit(Outcome.Loading)
 *     try { emit(Outcome.Success(repo.fetch())) }
 *     catch (e: Throwable) { emit(Outcome.Failure(e.toAppError())) }
 * }
 * ```
 *
 * TODO[FASE-2]:
 *   1. Refactor de Repos remotos: cambiar firma `Flow<List<X>>` → `Flow<Outcome<List<X>>>`.
 *   2. ScreenModels: mapear Outcome → UiState propio.
 *   3. UI: en `Outcome.Failure`, mostrar snackbar con `error.userMessage()`.
 */
sealed interface Outcome<out T> {
    data object Loading : Outcome<Nothing>
    data class Success<T>(val data: T) : Outcome<T>
    data class Failure(val error: AppError) : Outcome<Nothing>

    fun getOrNull(): T? = (this as? Success)?.data
    fun isSuccess(): Boolean = this is Success
    fun isFailure(): Boolean = this is Failure
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Loading -> Outcome.Loading
    is Outcome.Success -> Outcome.Success(transform(data))
    is Outcome.Failure -> this
}

inline fun <T> Outcome<T>.onSuccess(block: (T) -> Unit): Outcome<T> {
    if (this is Outcome.Success) block(data)
    return this
}

inline fun <T> Outcome<T>.onFailure(block: (AppError) -> Unit): Outcome<T> {
    if (this is Outcome.Failure) block(error)
    return this
}

/** User-facing message para mostrar en UI. Mapea errores técnicos a copy amigable. */
fun AppError.userMessage(): String = when (this) {
    is AppError.NetworkError -> "No hay conexión con el servidor. Intenta de nuevo."
    is AppError.ValidationError -> "Dato inválido: $message"
    is AppError.NotFound -> "$resource no encontrado."
    is AppError.UnknownError -> "Algo salió mal. Si persiste, contacta soporte."
}
