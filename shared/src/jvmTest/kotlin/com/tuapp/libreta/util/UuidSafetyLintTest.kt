package com.tuapp.libreta.util

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

class UuidSafetyLintTest {

    private val targetDirectories = listOf(
        "shared/src/commonMain/kotlin/com/tuapp/libreta/data/remote",
        "shared/src/commonMain/kotlin/com/tuapp/libreta/data/mapper"
    )

    private val forbiddenPatterns = listOf(
        Regex("""\?\:\s*\"\""""),     // ?: ""
        Regex("""\.orEmpty\(\)""")    // .orEmpty()
    )

    @Test
    fun `check for forbidden uuid anti-patterns in data layer`() {
        val violations = mutableListOf<String>()

        targetDirectories.forEach { dirPath ->
            val root = File("../../../$dirPath") // Ajuste de ruta según el entorno de test
            if (!root.exists()) return@forEach

            root.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
                val content = file.readText()
                forbiddenPatterns.forEach { pattern ->
                    if (pattern.containsMatchIn(content)) {
                        violations.add("Fallo en ${file.name}: Encontrado patrón prohibido '${pattern.pattern}'")
                    }
                }
            }
        }

        if (violations.isNotEmpty()) {
            fail("Se detectaron malas prácticas de UUID:\n" + violations.joinToString("\n"))
        }
    }
}
