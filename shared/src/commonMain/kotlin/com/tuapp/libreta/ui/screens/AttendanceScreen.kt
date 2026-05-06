package com.tuapp.libreta.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.tuapp.libreta.presentation.AttendanceScreenModel
import com.tuapp.libreta.presentation.AttendanceUiState
import com.tuapp.libreta.ui.theme.StatusError
import com.tuapp.libreta.ui.theme.StatusSuccess
import com.tuapp.libreta.ui.theme.StatusWarning
import com.tuapp.libreta.ui.components.FullScreenError
import com.tuapp.libreta.ui.components.ShimmerCard
import com.tuapp.libreta.data.util.UuidString
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

data class AttendanceScreen(
    val courseId: String,
    val courseName: String
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: AttendanceScreenModel = koinScreenModel(
            parameters = {
                org.koin.core.parameter.parametersOf(
                    UuidString(courseId),
                    courseName
                )
            }
        )
        val state by model.state.collectAsState()
        var showDatePicker by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    title = {
                        Column {
                            Text(
                                "Asistencia",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (state is AttendanceUiState.Success) {
                                Text(
                                    (state as AttendanceUiState.Success).courseName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Cambiar fecha")
                        }
                    }
                )
            }
        ) { padding ->
            when (val s = state) {
                AttendanceUiState.Loading -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(8) { ShimmerCard() }
                    }
                }

                is AttendanceUiState.Success -> {
                    Column(modifier = Modifier.padding(padding)) {
                        DateSelectorHeader(
                            date = s.date,
                            onDateClick = { showDatePicker = true }
                        )

                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(s.students, key = { it.studentId.value }) { student ->
                                AttendanceRow(
                                    name = student.studentName,
                                    status = student.status,
                                    hasJustification = student.hasJustification,
                                    onStatusChange = { newStatus ->
                                        model.markAttendance(student.studentId, newStatus)
                                    }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                AttendanceSummary(s.students)
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { navigator.pop() },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.Check, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Guardar y Salir", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                }

                is AttendanceUiState.Error -> FullScreenError(s.message, padding) { model.refresh() }
            }
        }

        if (showDatePicker && state is AttendanceUiState.Success) {
            AttendanceDatePickerDialog(
                initialDateStr = (state as AttendanceUiState.Success).date,
                onDateSelected = { 
                    model.changeDate(it)
                    showDatePicker = false 
                },
                onDismiss = { showDatePicker = false }
            )
        }
    }
}

@Composable
private fun DateSelectorHeader(date: String, onDateClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                onClick = onDateClick,
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Fecha: $date",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceSummary(students: List<com.tuapp.libreta.presentation.AttendanceStudent>) {
    val presentCount = students.count { it.status == AttendanceStatus.PRESENT }
    val absentCount = students.count { it.status == AttendanceStatus.ABSENT }
    val lateCount = students.count { it.status == AttendanceStatus.LATE }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("Presentes", presentCount, StatusSuccess)
            StatItem("Ausentes", absentCount, StatusError)
            StatItem("Atrasos", lateCount, StatusWarning)
        }
    }
}

@Composable
private fun StatItem(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceDatePickerDialog(
    initialDateStr: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = remember(initialDateStr) {
        try {
            val parts = initialDateStr.split("-")
            val local = kotlinx.datetime.LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            local.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        } catch (_: Exception) {
            null
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let {
                    val date = com.tuapp.libreta.data.util.epochMsToIso(it).take(10)
                    onDateSelected(date)
                }
            }) { Text("Seleccionar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun AttendanceRow(
    name: String,
    status: AttendanceStatus,
    hasJustification: Boolean,
    onStatusChange: (AttendanceStatus) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (status != AttendanceStatus.PRESENT) 
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) 
            else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                if (hasJustification) {
                    Text("Cuenta con justificación", style = MaterialTheme.typography.labelSmall, color = StatusSuccess)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusButton(
                    selected = status == AttendanceStatus.PRESENT,
                    icon = Icons.Default.Check,
                    label = "P",
                    color = StatusSuccess,
                    onClick = { onStatusChange(AttendanceStatus.PRESENT) }
                )
                StatusButton(
                    selected = status == AttendanceStatus.LATE,
                    icon = Icons.Default.Edit,
                    label = "T",
                    color = StatusWarning,
                    onClick = { onStatusChange(AttendanceStatus.LATE) }
                )
                StatusButton(
                    selected = status == AttendanceStatus.ABSENT,
                    icon = Icons.Default.Close,
                    label = "A",
                    color = StatusError,
                    onClick = { onStatusChange(AttendanceStatus.ABSENT) }
                )
            }
        }
    }
}

@Composable
private fun StatusButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) color else color.copy(alpha = 0.08f),
        label = "bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else color,
        label = "content"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(22.dp))
    }
}
