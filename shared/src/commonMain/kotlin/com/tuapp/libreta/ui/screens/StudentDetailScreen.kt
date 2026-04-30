package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.navigation.AppNavigation
import com.tuapp.libreta.presentation.DailyAttendance
import com.tuapp.libreta.presentation.StudentDetailScreenModel
import com.tuapp.libreta.presentation.StudentDetailUiState
import com.tuapp.libreta.ui.components.FullScreenError
import com.tuapp.libreta.ui.theme.StatusError
import com.tuapp.libreta.ui.theme.StatusSuccess
import com.tuapp.libreta.ui.theme.StatusWarning

data class StudentDetailScreen(
    val studentId: String,
    val studentName: String,
    val courseId: String,
    val parentId: String
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: StudentDetailScreenModel = koinScreenModel(
            parameters = {
                org.koin.core.parameter.parametersOf(
                    com.tuapp.libreta.data.util.UuidString(studentId),
                    studentName,
                    com.tuapp.libreta.data.util.UuidString(courseId),
                    com.tuapp.libreta.data.util.UuidString(parentId)
                )
            }
        )
        val state by model.state.collectAsState()
        var showDeleteDialog by remember { mutableStateOf(false) }

        LaunchedEffect(state) {
            if (state is StudentDetailUiState.Deleted) {
                navigator.pop()
            }
        }

        LaunchedEffect(Unit) {
            model.loadStudentDetails()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    title = {
                        Text(
                            "Detalle Estudiante",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    actions = {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            }
        ) { padding ->
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("¿Eliminar estudiante?") },
                    text = { Text("Esta acción no se puede deshacer. Se eliminarán todos los registros de asistencia de $studentName.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                model.deleteStudent()
                                showDeleteDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Eliminar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
            when (val s = state) {
                StudentDetailUiState.Loading -> {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is StudentDetailUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp,
                            top = padding.calculateTopPadding() + 8.dp,
                            bottom = padding.calculateBottomPadding() + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            StudentHeader(
                                name = s.studentName,
                                attendancePercentage = s.attendancePercentage
                            )
                        }

                        item {
                            Text(
                                "Acciones Rápidas",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                QuickActionCard(
                                    icon = Icons.Default.EditNote,
                                    label = "Justificación",
                                    onClick = { navigator.push(AppNavigation.justificationForm(parentId = s.parentId, studentId = s.studentId)) },
                                    modifier = Modifier.weight(1f)
                                )
                                QuickActionCard(
                                    icon = Icons.AutoMirrored.Filled.Message,
                                    label = "Mensaje",
                                    onClick = { 
                                        navigator.push(
                                            AppNavigation.composeNotice(
                                                classId = s.courseId, 
                                                studentId = s.studentId
                                            )
                                        ) 
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                QuickActionCard(
                                    icon = Icons.Default.History,
                                    label = "Historial",
                                    onClick = { navigator.push(AppNavigation.attendanceHistory(s.studentId, s.studentName)) },
                                    modifier = Modifier.weight(1f)
                                )
                                QuickActionCard(
                                    icon = Icons.Default.Grade,
                                    label = "Notas",
                                    onClick = { navigator.push(AppNavigation.studentGrades(s.studentId, s.studentName, s.courseId, isTeacher = true)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Text(
                                "Asistencia Reciente",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        if (s.recentAttendance.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Text(
                                        "Sin registros de asistencia",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            items(s.recentAttendance) { attendance ->
                                AttendanceMiniRow(attendance = attendance)
                            }
                        }

                        item {
                            Text(
                                "Bitácora de Observaciones (Interno)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        item {
                            var showNoteDialog by remember { mutableStateOf(false) }
                            var noteText by remember { mutableStateOf("") }

                            if (showNoteDialog) {
                                AlertDialog(
                                    onDismissRequest = { showNoteDialog = false },
                                    title = { Text("Nueva Observación Interna") },
                                    text = {
                                        OutlinedTextField(
                                            value = noteText,
                                            onValueChange = { noteText = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            placeholder = { Text("Ej: El alumno ha mejorado su participación...") },
                                            minLines = 3
                                        )
                                    },
                                    confirmButton = {
                                        Button(onClick = {
                                            if (noteText.isNotBlank()) {
                                                model.addInternalNote(noteText)
                                                noteText = ""
                                                showNoteDialog = false
                                            }
                                        }) { Text("Guardar") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showNoteDialog = false }) { Text("Cancelar") }
                                    }
                                )
                            }

                            Button(
                                onClick = { showNoteDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.Default.EditNote, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Añadir Nota Interna")
                            }
                        }

                        if (s.internalNotes.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Text(
                                        "No hay observaciones registradas.",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(s.internalNotes) { note ->
                                InternalNoteRow(note = note)
                            }
                        }
                    }
                }

                is StudentDetailUiState.Error -> FullScreenError(s.message, padding) { model.refresh() }
                is StudentDetailUiState.Deleted -> {}
            }
        }
    }
}

@Composable
fun InternalNoteRow(note: com.tuapp.libreta.domain.model.Message) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = com.tuapp.libreta.data.util.formatIsoToTime(note.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StudentHeader(
    name: String,
    attendancePercentage: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name[0].uppercase().toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val color = when {
                        attendancePercentage >= 90 -> StatusSuccess
                        attendancePercentage >= 75 -> StatusWarning
                        else -> StatusError
                    }
                    CircularProgressIndicator(
                        progress = { attendancePercentage / 100f },
                        modifier = Modifier.size(48.dp),
                        color = color,
                        strokeWidth = 4.dp
                    )
                    Text(
                        "$attendancePercentage%",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = color
                    )
                }
                Text(
                    "Asistencia general",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun AttendanceMiniRow(attendance: DailyAttendance) {
    val statusColor = when (attendance.status) {
        AttendanceStatus.PRESENT -> StatusSuccess
        AttendanceStatus.ABSENT -> StatusError
        AttendanceStatus.LATE -> StatusWarning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = attendance.date,
                style = MaterialTheme.typography.bodyMedium
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
        }
    }
}
