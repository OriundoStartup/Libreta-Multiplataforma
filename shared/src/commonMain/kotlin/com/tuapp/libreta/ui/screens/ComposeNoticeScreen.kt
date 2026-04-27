package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuapp.libreta.domain.model.NoticeCategory
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.toUuidOrNull
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.navigation.AppConfig
import com.tuapp.libreta.presentation.ComposeMode
import com.tuapp.libreta.presentation.NoticeScreenModel
import com.tuapp.libreta.presentation.NoticeUiState

data class ComposeNoticeScreen(
    val preselectedClassId: String? = null,
    val preselectedStudentId: String? = null
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: NoticeScreenModel = koinScreenModel()
        val state       by model.state.collectAsState()
        val classes     by model.classes.collectAsState()
        val students    by model.students.collectAsState()
        val composeMode by model.composeMode.collectAsState()

        val defaultClassId = AppConfig.DEMO_CLASS_ID.toUuidOrNull() ?: UuidString("00000000-0000-0000-0000-000000000000")
        val defaultTeacherId = AppConfig.DEMO_TEACHER_ID.toUuidOrNull() ?: UuidString("00000000-0000-0000-0000-000000000000")

        var content          by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf(NoticeCategory.INFO) }
        var classExpanded    by remember { mutableStateOf(false) }
        
        var selectedClassId   by remember { mutableStateOf(defaultClassId) }
        var selectedClassName by remember { mutableStateOf(AppConfig.DEMO_CLASS_NAME) }

        var selectedParentId  by remember { mutableStateOf<UuidString?>(null) }
        var studentQuery      by remember { mutableStateOf("") }
        
        val filteredStudents by remember(studentQuery, students) {
            derivedStateOf {
                if (studentQuery.isEmpty()) students
                else students.filter { it.fullName.contains(studentQuery, ignoreCase = true) }
            }
        }

        LaunchedEffect(classes) {
            val preId = preselectedClassId?.let { UuidString(it) }
            val foundClass = classes.find { it.id == preId?.value } ?: classes.firstOrNull()
            
            foundClass?.let { 
                selectedClassId = UuidString(it.id) 
                selectedClassName = it.name 
            }
        }

        LaunchedEffect(selectedClassId) {
            model.loadStudents(selectedClassId)
            if (preselectedStudentId == null) {
                selectedParentId = null
                studentQuery = ""
            }
        }

        LaunchedEffect(students) {
            if (preselectedStudentId != null && selectedParentId == null) {
                students.find { it.id.value == preselectedStudentId }?.let { std ->
                    selectedParentId = std.parentId
                    studentQuery = std.fullName
                    model.setComposeMode(ComposeMode.DIRECT)
                }
            }
        }

        LaunchedEffect(state) {
            if (state is NoticeUiState.Sent) { navigator.pop(); model.resetState() }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = { IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }},
                    title  = { Text("Redactar Mensaje",
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
                        classes.forEach { cls ->
                            DropdownMenuItem(
                                text    = { Text(cls.name) },
                                onClick = { 
                                    selectedClassId = UuidString(cls.id)
                                    selectedClassName = cls.name 
                                    classExpanded = false 
                                }
                            )
                        }
                    }
                }

                // ── Modo de envío ─────────────────────────────────────────────
                TabRow(selectedTabIndex = if (composeMode == ComposeMode.GENERAL) 0 else 1) {
                    Tab(
                        selected = composeMode == ComposeMode.GENERAL,
                        onClick  = { model.setComposeMode(ComposeMode.GENERAL) },
                        text     = { Text("Aviso al Curso") }
                    )
                    Tab(
                        selected = composeMode == ComposeMode.DIRECT,
                        onClick  = { model.setComposeMode(ComposeMode.DIRECT) },
                        text     = { Text("Mensaje Directo") }
                    )
                }

                if (composeMode == ComposeMode.DIRECT) {
                    // ── Búsqueda de Alumno (Buscador Ligero) ───────────────────
                    SectionLabel("Buscar alumno")
                    OutlinedTextField(
                        value         = studentQuery,
                        onValueChange = { 
                            studentQuery = it
                            if (it.isEmpty()) selectedParentId = null
                        },
                        placeholder   = { Text("Escribe el nombre del alumno...") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        trailingIcon  = {
                            if (studentQuery.isNotEmpty()) {
                                IconButton(onClick = { studentQuery = ""; selectedParentId = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpiar")
                                }
                            }
                        }
                    )

                    if (selectedParentId == null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                if (filteredStudents.isEmpty()) {
                                    Text(
                                        "No hay alumnos en este curso", 
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    filteredStudents.forEach { std ->
                                        DropdownMenuItem(
                                            text    = { 
                                                Column {
                                                    Text(std.fullName, fontWeight = FontWeight.SemiBold)
                                                    Text("Apoderado vinculado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                }
                                            },
                                            onClick = { 
                                                selectedParentId = std.parentId 
                                                studentQuery = std.fullName
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Tarjeta de destinatario seleccionado
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Destinatario", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(studentQuery, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Mensaje llegará al apoderado", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { selectedParentId = null; studentQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cambiar")
                                }
                            }
                        }
                    }
                } else {
                    SectionLabel("Categoría")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (cat in NoticeCategory.entries) {
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick  = { selectedCategory = cat },
                                label    = { Text("${cat.emoji} ${cat.label}") }
                            )
                        }
                    }
                }

                SectionLabel("Mensaje")
                OutlinedTextField(
                    value         = content,
                    onValueChange = { content = it },
                    modifier      = Modifier.fillMaxWidth().height(140.dp),
                    placeholder   = { 
                        if (composeMode == ComposeMode.GENERAL) Text("Escribe el aviso para el curso...") 
                        else Text("Escribe un mensaje directo al apoderado...") 
                    },
                    maxLines      = 6
                )

                if (state is NoticeUiState.Error) {
                    Text(
                        text  = (state as NoticeUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                val isSending = state is NoticeUiState.Sending
                val canSend = content.isNotBlank() && !isSending && 
                               (composeMode == ComposeMode.GENERAL || selectedParentId != null)
                
                Button(
                    onClick  = {
                        if (composeMode == ComposeMode.GENERAL) {
                            model.sendNotice(classId = selectedClassId, content = content, category = selectedCategory)
                        } else {
                            selectedParentId?.let { parentId ->
                                model.sendDirectMessage(parentId = parentId, content = content)
                            }
                        }
                    },
                    enabled  = canSend,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (isSending) CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary
                    )
                    else Text("Enviar Mensaje", style = MaterialTheme.typography.labelLarge)
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
