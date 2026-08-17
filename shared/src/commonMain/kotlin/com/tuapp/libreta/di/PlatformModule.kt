package com.tuapp.libreta.di

import org.koin.core.module.Module
import kotlinx.coroutines.CompletableDeferred

expect val platformModule: Module

/**
 * Signal to ensure the database is initialized (schema created) before use.
 * Especially critical for Wasm where schema creation is asynchronous.
 */
expect val dbReady: CompletableDeferred<Unit>
