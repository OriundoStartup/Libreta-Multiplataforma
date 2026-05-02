package com.tuapp.libreta.ui.util

import androidx.compose.runtime.Composable

@Composable
actual fun rememberFilePickerLauncher(
    onFileSelected: (ByteArray, String) -> Unit
): () -> Unit {
    // TODO: Implementar usando ActivityResultLauncher en Android
    return {
        println("FilePicker: No implementado aún en Android")
    }
}
