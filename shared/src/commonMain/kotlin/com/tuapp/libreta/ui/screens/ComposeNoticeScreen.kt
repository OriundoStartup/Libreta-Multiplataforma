package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.toUuidOrNull
import com.tuapp.libreta.navigation.AppConfig
import com.tuapp.libreta.presentation.NoticeCategory
import com.tuapp.libreta.presentation.NoticeScreenModel
import com.tuapp.libreta.presentation.NoticeUiState

object ComposeNoticeScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: NoticeScreenModel = koinScreenModel()
        val state   by model.state.collectAsState()
        val classes by model.classes.collectAsState()

        // Fallback seguro para IDs de demo que podrían no ser UUIDs válidos
        val defaultClassId = AppConfig.DEMO_CLASS_ID.toUuidOrNull() ?: UuidString("00000000-0000-0000-0000-000000000000")
        val defaultTeacherId = AppConfig.DEMO_TEACHER_ID.toUuidOrNull() ?: UuidString("00000000-0000-0000-0000-000000000000")

        var content          by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf(NoticeCategory.INFO) }
        var classExpanded    by remember { mutableStateOf(false) }
        
        // Consistencia: Usamos UuidString para el estado local del ID
        var selectedClassId   by remember { mutableStateOf(defaultClassId) }
        var selectedClassName by remember { mutableStateOf(AppConfig.DEMO_CLASS_NAME) }

        // Auto-select first class when loaded
        LaunchedEffect(classes) {
            classes.firstOrNull()?.let { 
                selectedClassId = it.id 
                selectedClassName = it.name 
            }
        }

        // Navigate back on success
        LaunchedEffect(state) {
            if (state is NoticeUiState.Sent) { navigator.pop(); model.resetState() }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = { IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }},
                    title  = { Text("Enviar Comunicación",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Selector de curso ─────────────────────────────────────────
                SectionLabel("Curso destinatario")
                ExposedDropdownMenuBox(
                    expanded         = classExpanded,
                    onExpandedChange = { classExpanded = it }
                ) {
                    OutlinedTextField(
                        value         = selectedClassName,
                        onValueChange = {},
                        readOnly      = true,
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(classExpanded) },
                        modifier      = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded         = classExpanded,
                        onDismissRequest = { classExpanded = false }
                    ) {
                        // Siempre muestra al menos el curso demo usando tipado fuerte
                        val displayClasses = classes.ifEmpty {
                            listOf(com.tuapp.libreta.domain.model.ClassRoom(
                                id = defaultClassId, 
                                classCode = "DEMO", 
                                name = AppConfig.DEMO_CLASS_NAME, 
                                teacherId = defaultTeacherId
                            ))
                        }
                        displayClasses.forEach { cls ->
                            DropdownMenuItem(
                                text    = { Text(cls.name) },
                                onClick = { 
                                    selectedClassId = cls.id 
                                    selectedClassName = cls.name 
                                    classExpanded = false 
                                }
                            )
                        }
                    }
                }

                // ── Categorías ────────────────────────────────────────────────
                SectionLabel("Categoría")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoticeCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick  = { selectedCategory = cat },
                            label    = { Text("${cat.emoji} ${cat.label}") }
                        )
                    }
                }

                // ── Cuerpo del mensaje ────────────────────────────────────────
                SectionLabel("Mensaje")
                OutlinedTextField(
                    value         = content,
                    onValueChange = { content = it },
                    modifier      = Modifier.fillMaxWidth().height(140.dp),
                    placeholder   = { Text("Escribe el aviso para los apoderados...") },
                    maxLines      = 6
                )

                // ── Error ─────────────────────────────────────────────────────
                if (state is NoticeUiState.Error) {
                    Text(
                        text  = (state as NoticeUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // ── Botón enviar ──────────────────────────────────────────────
                val isSending = state is NoticeUiState.Sending
                Button(
                    onClick  = {
                        // Ahora selectedClassId ya es UuidString, cumpliendo el contrato del modelo
                        model.sendNotice(classId = selectedClassId, content = content, category = selectedCategory)
                    },
                    enabled  = content.isNotBlank() && !isSending,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (isSending) CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary
                    )
                    else Text("Enviar Comunicación", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text,
        style    = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color    = MaterialTheme.colorScheme.onSurfaceVariant)
}
