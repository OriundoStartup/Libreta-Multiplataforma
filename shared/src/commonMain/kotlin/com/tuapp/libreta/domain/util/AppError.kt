package com.tuapp.libreta.domain.util

sealed class AppError : Throwable() {
    data class NetworkError(val code: Int? = null, override val message: String) : AppError()
    data class ValidationError(val field: String, override val message: String) : AppError()
    data class NotFound(val resource: String) : AppError()
    data class UnknownError(override val message: String) : AppError()
}

typealias AppResult<T> = Result<T>

fun Throwable.toAppError(): AppError {
    val msg = this.message ?: "Unknown error"
    return when {
        msg.contains("22P02") -> AppError.ValidationError("uuid", "Formato de ID inválido detectado por el servidor")
        msg.contains("404") || msg.contains("not found") -> AppError.NotFound("Recurso")
        msg.contains("network") || msg.contains("connect") -> AppError.NetworkError(null, "Error de conexión con el servidor")
        else -> AppError.UnknownError(msg)
    }
}
