package com.tuapp.libreta.data.util

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.tuapp.libreta.util.getIoDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlin.coroutines.CoroutineContext

/**
 * Encapsula el flujo común de SQLDelight: Query -> Flow -> List -> Domain Mapping -> Error Handling
 */
fun <T : Any, R : Any> Query<T>.toDomainList(
    context: CoroutineContext = getIoDispatcher(),
    mapper: (T) -> R
): Flow<List<R>> = asFlow()
    .mapToList(context)
    .map { list -> list.map(mapper) }
    .catch { emit(emptyList()) }

/**
 * Encapsula el flujo común para un solo elemento opcional
 */
fun <T : Any, R : Any> Query<T>.toDomainSingle(
    context: CoroutineContext = getIoDispatcher(),
    mapper: (T) -> R
): Flow<R?> = asFlow()
    .mapToOneOrNull(context)
    .map { it?.let(mapper) }
    .catch { emit(null) }
