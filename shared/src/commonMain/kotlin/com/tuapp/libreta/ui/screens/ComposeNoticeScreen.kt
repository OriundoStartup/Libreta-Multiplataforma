package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.tuapp.libreta.domain.model.UserRole

data class ComposeNoticeScreen(
    val preselectedClassId: String? = null,
    val preselectedStudentId: String? = null,
    val preselectedClassName: String? = null
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: NoticeScreenModel = koinScreenModel()
        val state       by model.state.collectAsState()
        val classes     by model.classes.collectAsState()
        val students    by model.students.collectAsState()
        val isStudentsLoading by model.isStudentsLoading.collectAsState()
        val composeMode by model.composeMode.collectAsState()
        val userRole    by model.userRole.collectAsState()

        var content          by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf<NoticeCategory?>(null) }
        var classExpanded    by remember { mutableStateOf(false) }
        
        var selectedClassId   by remember { mutableStateOf<UuidString?>(preselectedClassId?.let { UuidString(it) }) }
        var selectedClassName by remember { mutableStateOf(preselectedClassName ?: "Cargando curso...") }
        var selectedParentId  by remember { mutableStateOf<UuidString?>(null) }
        var selectedStudentName by remember { mutableStateOf("") }

        // 1. CARGA DE ALUMNOS (Si ya tenemos el ID)
        LaunchedEffect(selectedClassId) {
            selectedClassId?.let { model.loadStudents(it) }
        }

        // 2. SINCRONIZACIÓN DE NOMBRE DE CURSO (Solo si no lo tenemos ya)
        LaunchedEffect(classes) {
            if (selectedClassId != null && (selectedClassName == "Cargando curso..." || selectedClassName.isEmpty())) {
                val found = classes.find { it.id == selectedClassId?.value }
                if (found != null) {
                    selectedClassName = found.name
                }
            } else if (classes.isNotEmpty() && selectedClassId == null) {
                selectedClassId = UuidString(classes.first().id)
                selectedClassName = classes.first().name
            }
        }

        // 3. SELECCIÓN DE ALUMNO PREESTABLECIDO
        LaunchedEffect(students) {
            if (preselectedStudentId != null && selectedParentId == null) {
                students.find { it.id.value == preselectedStudentId }?.let { std ->
                    selectedParentId = std.parentId
                    selectedStudentName = std.fullName
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
                    title  = { Text("Enviar Comunicación", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { padding ->
            if (userRole == null) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // ── PASO 1: CURSO ─────────────────────────────────────────
                    SectionLabel("1. Curso")
                    ExposedDropdownMenuBox(
                        expanded         = classExpanded,
                        onExpandedChange = { if (preselectedClassId == null) classExpanded = it }
                    ) {
                        OutlinedTextField(
                            value         = selectedClassName,
                            onValueChange = {},
                            readOnly      = true,
                            enabled       = preselectedClassId == null,
                            trailingIcon  = { if (preselectedClassId == null) ExposedDropdownMenuDefaults.TrailingIcon(classExpanded) },
                            modifier      = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape         = RoundedCornerShape(12.dp)
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

                    // ── PASO 2: DESTINATARIO ──────────────────────────────────
                    if (userRole == UserRole.TEACHER) {
                        SectionLabel("2. ¿A quién va dirigido?")
                        Row(
                            modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ComposeModeButton(
                                text = "Todo el Curso",
                                icon = Icons.Default.Person,
                                isSelected = composeMode == ComposeMode.GENERAL,
                                modifier = Modifier.weight(1f),
                                onClick = { model.setComposeMode(ComposeMode.GENERAL) }
                            )
                            ComposeModeButton(
                                text = "Alumno/a",
                                icon = Icons.Default.Person,
                                isSelected = composeMode == ComposeMode.DIRECT,
                                modifier = Modifier.weight(1f),
                                onClick = { model.setComposeMode(ComposeMode.DIRECT) }
                            )
                        }

                        // CATEGORÍAS (Ahora visibles en ambos modos para Profesores)
                        SectionLabel("Categoría (Opcional)")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedCategory == null,
                                    onClick  = { selectedCategory = null },
                                    label    = { Text("📝 Mensaje Simple") }
                                )
                            }
                            items(NoticeCategory.entries) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick  = { selectedCategory = cat },
                                    label    = { Text("${cat.emoji} ${cat.label}") }
                                )
                            }
                        }

                        if (composeMode == ComposeMode.DIRECT) {
                            if (selectedParentId == null) {
                                if (isStudentsLoading) {
                                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(Modifier.size(32.dp))
                                    }
                                } else {
                                    Text("Selecciona un alumno para el mensaje directo:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerLow
                                    ) {
                                        Column(Modifier.verticalScroll(rememberScrollState())) {
                                            if (students.isEmpty()) {
                                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                                    Text("No se encontraron alumnos.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            } else {
                                                students.forEach { std ->
                                                    ListItem(
                                                        headlineContent = { Text(std.fullName, fontWeight = FontWeight.Medium) },
                                                        leadingContent = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                                                        modifier = Modifier.clickable { 
                                                            selectedParentId = std.parentId
                                                            selectedStudentName = std.fullName
                                                        }
                                                    )
                                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                DestinatarioCard(
                                    name = selectedStudentName, 
                                    subtext = "Mensaje llegará al apoderado",
                                    onClear = { if (preselectedStudentId == null) { selectedParentId = null; selectedStudentName = "" } }
                                )
                            }
                        }
                    } else {
                        DestinatarioCard(name = "Profesor Jefe", subtext = "Mensaje directo al docente del curso")
                    }

                    // ── PASO 3: EL MENSAJE ──────────────────────────────────────
                    SectionLabel("3. Escribe tu mensaje")
                    OutlinedTextField(
                        value         = content,
                        onValueChange = { content = it },
                        modifier      = Modifier.fillMaxWidth().height(160.dp),
                        placeholder   = { Text("Contenido de la comunicación...") },
                        shape         = RoundedCornerShape(12.dp),
                        maxLines      = 10
                    )

                    if (state is NoticeUiState.Error) {
                        Text((state as NoticeUiState.Error).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    val isSending = state is NoticeUiState.Sending
                    
                    val isCourseSelected = selectedClassId != null
                    val isRecipientReady = if (userRole == UserRole.TEACHER) {
                        if (composeMode == ComposeMode.GENERAL) true else selectedParentId != null
                    } else true

                    val canSend = content.isNotBlank() && !isSending && isCourseSelected && isRecipientReady
                    
                    Button(
                        onClick  = {
                            selectedClassId?.let { classId ->
                                if (userRole == UserRole.TEACHER && composeMode == ComposeMode.GENERAL) {
                                    model.sendNotice(classId = classId, content = content, category = selectedCategory ?: NoticeCategory.INFO)
                                } else {
                                    model.sendDirectMessage(classId = classId, parentId = selectedParentId, content = content, category = selectedCategory)
                                }
                            }
                        },
                        enabled  = canSend,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape    = RoundedCornerShape(16.dp)
                    ) {
                        if (isSending) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Enviar Comunicación", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposeModeButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    
    Box(
        modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(24.dp)).background(bgColor).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = contentColor)
            Spacer(Modifier.width(8.dp))
            Text(text, color = contentColor, style = MaterialTheme.typography.labelLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
private fun DestinatarioCard(name: String, subtext: String, onClear: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Para: $name", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtext, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onClear != null) {
                IconButton(onClick = onClear) { Icon(Icons.Default.Close, null) }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
}
