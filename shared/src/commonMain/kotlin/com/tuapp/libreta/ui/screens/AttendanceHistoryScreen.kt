package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import kotlinx.coroutines.flow.first
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.presentation.AttendanceHistoryScreenModel
import com.tuapp.libreta.presentation.AttendanceHistoryUiState
import com.tuapp.libreta.presentation.AttendanceRecord
import com.tuapp.libreta.ui.components.EmptyStateView
import com.tuapp.libreta.ui.components.FullScreenError
import com.tuapp.libreta.ui.components.ShimmerCard

data class AttendanceHistoryScreen(
    val studentId: String,
    val studentName: String
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: AttendanceHistoryScreenModel = koinScreenModel(
            parameters = {
                org.koin.core.parameter.parametersOf(
                    com.tuapp.libreta.data.util.UuidString(studentId),
                    studentName
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
                                "Historial de Asistencia",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (state is AttendanceHistoryUiState.Success) {
                                Text(
                                    (state as AttendanceHistoryUiState.Success).studentName,
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
                AttendanceHistoryUiState.Loading -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(6) { ShimmerCard() }
                    }
                }

                is AttendanceHistoryUiState.Success -> {
                    if (s.records.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.EditNote,
                            title = "Sin registros",
                            description = "No hay asistencia registrada",
                            modifier = Modifier.padding(padding)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp,
                                top = padding.calculateTopPadding() + 8.dp,
                                bottom = padding.calculateBottomPadding() + 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                SummaryRow(records = s.records)
                            }

                            items(s.records) { record ->
                                AttendanceHistoryRow(
                                    record = record,
                                    onUpdateStatus = { model.updateStatus(record, it) }
                                )
                            }
                        }
                    }
                }

                is AttendanceHistoryUiState.Error -> FullScreenError(s.message, padding) { model.refresh() }
            }
        }
    }
}

@Composable
private fun SummaryRow(records: List<AttendanceRecord>) {
    val presentCount = records.count { it.status == AttendanceStatus.PRESENT }
    val absentCount = records.count { it.status == AttendanceStatus.ABSENT }
    val lateCount = records.count { it.status == AttendanceStatus.LATE }
    val percentage = if (records.isNotEmpty()) (presentCount * 100 / records.size) else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "$percentage%",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Asistencia general",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryChip("Presentes", presentCount, Color(0xFF4CAF50))
                SummaryChip("Ausentes", absentCount, Color(0xFFF44336))
                SummaryChip("Tardes", lateCount, Color(0xFFFF9800))
            }
        }
    }
}

@Composable
private fun SummaryChip(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "$count",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun AttendanceHistoryRow(
    record: AttendanceRecord,
    onUpdateStatus: (AttendanceStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when (record.status) {
        AttendanceStatus.PRESENT -> Color(0xFF4CAF50)
        AttendanceStatus.ABSENT -> Color(0xFFF44336)
        AttendanceStatus.LATE -> Color(0xFFFF9800)
    }

    val statusIcon = when (record.status) {
        AttendanceStatus.PRESENT -> Icons.Default.Check
        AttendanceStatus.ABSENT -> Icons.Default.Close
        AttendanceStatus.LATE -> Icons.Default.EditNote
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = record.date,
                style = MaterialTheme.typography.bodyLarge
            )
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(statusColor.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 4.dp).clickable { expanded = true }
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = record.status.name,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = when (record.status) {
                            AttendanceStatus.PRESENT -> "Presente"
                            AttendanceStatus.ABSENT -> "Ausente"
                            AttendanceStatus.LATE -> "Tarde"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    AttendanceStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    when(status) {
                                        AttendanceStatus.PRESENT -> "Presente"
                                        AttendanceStatus.ABSENT -> "Ausente"
                                        AttendanceStatus.LATE -> "Tarde"
                                    }
                                )
                            },
                            onClick = {
                                expanded = false
                                onUpdateStatus(status)
                            },
                            leadingIcon = {
                                Icon(
                                    when(status) {
                                        AttendanceStatus.PRESENT -> Icons.Default.Check
                                        AttendanceStatus.ABSENT -> Icons.Default.Close
                                        AttendanceStatus.LATE -> Icons.Default.EditNote
                                    },
                                    contentDescription = null,
                                    tint = when(status) {
                                        AttendanceStatus.PRESENT -> Color(0xFF4CAF50)
                                        AttendanceStatus.ABSENT -> Color(0xFFF44336)
                                        AttendanceStatus.LATE -> Color(0xFFFF9800)
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}