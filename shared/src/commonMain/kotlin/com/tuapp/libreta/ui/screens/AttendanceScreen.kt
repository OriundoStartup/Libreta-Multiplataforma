package com.tuapp.libreta.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
                    com.tuapp.libreta.data.util.UuidString(courseId),
                    courseName
                )
            }
        )
        val state by model.state.collectAsState()

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
                                "Tomar Asistencia",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (state is AttendanceUiState.Success) {
                                Text(
                                    (state as AttendanceUiState.Success).courseName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp,
                            top = padding.calculateTopPadding() + 8.dp,
                            bottom = padding.calculateBottomPadding() + 80.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                "Fecha: ${s.date}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

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
                            Spacer(modifier = Modifier.height(8.dp))
                            val presentCount = s.students.count { it.status == AttendanceStatus.PRESENT }
                            val absentCount = s.students.count { it.status == AttendanceStatus.ABSENT }
                            val lateCount = s.students.count { it.status == AttendanceStatus.LATE }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatChip("Present: $presentCount", StatusSuccess)
                                StatChip("Ausentes: $absentCount", StatusError)
                                StatChip("Tardan: $lateCount", StatusWarning)
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { navigator.pop() },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Finalizar Registro", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                is AttendanceUiState.Error -> FullScreenError(s.message, padding) { model.refresh() }
            }
        }
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusButton(
                    selected = status == AttendanceStatus.PRESENT,
                    icon = Icons.Default.Check,
                    label = "P",
                    color = StatusSuccess,
                    onClick = { onStatusChange(AttendanceStatus.PRESENT) }
                )
                StatusButton(
                    selected = status == AttendanceStatus.LATE,
                    icon = Icons.Default.EditNote,
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
        targetValue = if (selected) color else Color.Transparent,
        label = "bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else color,
        label = "content"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, color, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun StatChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}