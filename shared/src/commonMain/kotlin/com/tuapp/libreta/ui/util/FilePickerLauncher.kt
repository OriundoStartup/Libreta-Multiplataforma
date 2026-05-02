package com.tuapp.libreta.ui.util

import androidx.compose.runtime.Composable

/**
 * Lanzador multiplataforma para seleccionar archivos.
 * @param onFileSelected Callback que recibe los bytes del archivo y su nombre original.
 * @return Una función lambda que al ser invocada dispara la UI nativa de selección.
 */
@Composable
expect fun rememberFilePickerLauncher(
    onFileSelected: (ByteArray, String) -> Unit
): () -> Unit
