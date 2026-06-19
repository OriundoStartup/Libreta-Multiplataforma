package com.tuapp.libreta.ui.util

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.tuapp.libreta.data.util.AppLogger

@Composable
actual fun rememberFilePickerLauncher(
    onFileSelected: (ByteArray, String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val resolver = context.contentResolver
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
                } ?: "archivo"
            if (bytes != null) onFileSelected(bytes, name)
        }.onFailure { e ->
            AppLogger.e("FilePicker", "No se pudo leer el archivo seleccionado", e)
        }
    }
    // El contrato GetContent acepta un MIME type; "*/*" permite cualquier archivo (PDF, imágenes…)
    return { launcher.launch("*/*") }
}
