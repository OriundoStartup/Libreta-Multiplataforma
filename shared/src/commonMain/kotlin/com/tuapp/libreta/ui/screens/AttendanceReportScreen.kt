package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.usecase.AttendanceReport
import com.tuapp.libreta.presentation.ReportScreenModel
import com.tuapp.libreta.presentation.ReportUiState

data class AttendanceReportScreen(val courseId: String, val courseName: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: ReportScreenModel = koinScreenModel()
        val state by model.state.collectAsState()

        LaunchedEffect(courseId) { model.load(courseId) }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    },
                    title = { Text("Reporte: $courseName") },
                    actions = {
                        IconButton(onClick = { /* Export functionality could be added here */ }) {
                            Icon(Icons.Default.ArrowDropDown, "Exportar")
                        }
                    }
                )
            }
        ) { padding ->
            when (val s = state) {
                ReportUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is ReportUiState.Error -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                }
                is ReportUiState.Success -> ReportTable(s.data, padding)
            }
        }
    }

    @Composable
    private fun ReportTable(report: AttendanceReport, padding: PaddingValues) {
        val horizontalScrollState = rememberScrollState()

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Header: Dates
            Row(modifier = Modifier.horizontalScroll(horizontalScrollState).background(MaterialTheme.colorScheme.surfaceVariant)) {
                // Fixed corner
                Box(Modifier.width(150.dp).padding(8.dp)) {
                    Text("Estudiante", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                report.dates.forEach { date ->
                    Box(Modifier.width(60.dp).padding(8.dp), contentAlignment = Alignment.Center) {
                        Text(date.takeLast(5), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }

            // Body: Students
            LazyColumn(modifier = Modifier.fillMaxSize().horizontalScroll(horizontalScrollState)) {
                items(report.students) { studentName ->
                    Row(modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        // Student Name (Sticky-like)
                        Box(Modifier.width(150.dp).background(MaterialTheme.colorScheme.surface).padding(8.dp)) {
                            Text(studentName, fontSize = 11.sp, maxLines = 1)
                        }
                        
                        // Attendance status for each date
                        report.dates.forEach { date ->
                            val status = report.matrix[studentName]?.get(date)
                            StatusCell(status)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun StatusCell(status: AttendanceStatus?) {
        val color = when (status) {
            AttendanceStatus.PRESENT -> Color(0xFFE8F5E9)
            AttendanceStatus.ABSENT -> Color(0xFFFFEBEE)
            AttendanceStatus.LATE -> Color(0xFFFFF3E0)
            else -> Color.Transparent
        }
        val text = when (status) {
            AttendanceStatus.PRESENT -> "P"
            AttendanceStatus.ABSENT -> "A"
            AttendanceStatus.LATE -> "T"
            else -> "-"
        }
        val textColor = when (status) {
            AttendanceStatus.PRESENT -> Color(0xFF2E7D32)
            AttendanceStatus.ABSENT -> Color(0xFFC62828)
            AttendanceStatus.LATE -> Color(0xFFE65100)
            else -> Color.Gray
        }

        Box(
            modifier = Modifier
                .width(60.dp)
                .height(40.dp)
                .background(color)
                .border(0.2.dp, MaterialTheme.colorScheme.outlineVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
