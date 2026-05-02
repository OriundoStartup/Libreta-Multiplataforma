package com.tuapp.libreta.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get

@Composable
actual fun rememberFilePickerLauncher(
    onFileSelected: (ByteArray, String) -> Unit
): () -> Unit {
    return remember {
        {
            val input = document.createElement("input") as HTMLInputElement
            input.type = "file"
            // Aceptamos imágenes y PDFs como certificados médicos
            input.accept = "image/*,.pdf"
            
            input.onchange = {
                val file = input.files?.get(0)
                if (file != null) {
                    val reader = FileReader()
                    reader.onload = {
                        val result = reader.result
                        if (result != null) {
                            val arrayBuffer = result.unsafeCast<ArrayBuffer>()
                            val int8Array = Int8Array(arrayBuffer)
                            val bytes = ByteArray(int8Array.length) { i -> int8Array[i] }
                            onFileSelected(bytes, file.name)
                        }
                    }
                    reader.readAsArrayBuffer(file)
                }
            }
            input.click()
        }
    }
}
