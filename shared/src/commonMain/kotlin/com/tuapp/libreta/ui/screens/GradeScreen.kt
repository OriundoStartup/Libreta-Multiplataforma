package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tuapp.libreta.domain.model.Grade
import com.tuapp.libreta.domain.model.SubjectAverage
import com.tuapp.libreta.presentation.GradeScreenModel
import com.tuapp.libreta.presentation.GradeUiState
import com.tuapp.libreta.ui.components.FullScreenLoading

data class GradeScreen(
    val studentId: String,
    val studentName: String,
    val courseId: String,
    val isTeacher: Boolean = false
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: GradeScreenModel = koinScreenModel()
        val state by model.state.collectAsState()
        var showAddDialog by remember { mutableStateOf(false) }
        var gradeToEdit by remember { mutableStateOf<Grade?>(null) }
        var gradeToDelete by remember { mutableStateOf<Grade?>(null) }

        LaunchedEffect(studentId) { model.load(studentId) }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    },
                    title = { Text("Notas: $studentName") }
                )
            },
            floatingActionButton = {
                if (isTeacher) {
                    FloatingActionButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Agregar Nota")
                    }
                }
            }
        ) { padding ->
            when (val s = state) {
                GradeUiState.Idle,
                GradeUiState.Loading -> FullScreenLoading(padding)
                is GradeUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                }
                is GradeUiState.Success -> GradeList(
                    averages = s.averages, 
                    padding  = padding,
                    onEdit   = { gradeToEdit = it },
                    onDelete = { gradeToDelete = it }
                )
            }
        }

        if (showAddDialog) {
            AddGradeDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, score, subject, weight ->
                    model.addGrade(studentId, courseId, title, score, subject, weight)
                    showAddDialog = false
                }
            )
        }

        gradeToEdit?.let { grade ->
            AddGradeDialog(
                initialGrade = grade,
                onDismiss = { gradeToEdit = null },
                onConfirm = { title, score, subject, weight ->
                    model.updateGrade(grade.copy(title = title, score = score, subject = subject, weight = weight))
                    gradeToEdit = null
                }
            )
        }

        gradeToDelete?.let { grade ->
            AlertDialog(
                onDismissRequest = { gradeToDelete = null },
                title = { Text("Eliminar Calificación") },
                text = { Text("¿Estás seguro que deseas eliminar la nota '${grade.title}'?") },
                confirmButton = {
                    Button(
                        onClick = { 
                            grade.id?.let { model.deleteGrade(it) }
                            gradeToDelete = null 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Eliminar") }
                },
                dismissButton = { TextButton(onClick = { gradeToDelete = null }) { Text("Cancelar") } }
            )
        }
    }

    @Composable
    private fun GradeList(averages: List<SubjectAverage>, padding: PaddingValues, onEdit: (Grade) -> Unit, onDelete: (Grade) -> Unit) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(averages) { subjectAvg ->
                SubjectCard(subjectAvg, onEdit, onDelete)
            }
        }
    }

    @Composable
    private fun SubjectCard(subjectAvg: SubjectAverage, onEdit: (Grade) -> Unit, onDelete: (Grade) -> Unit) {
        val subjectHash = subjectAvg.subject.hashCode()
        val cardColor = remember(subjectHash) {
            val colors = listOf(
                Color(0xFFE3F2FD), // Azul
                Color(0xFFE8F5E9), // Verde
                Color(0xFFF3E5F5), // Púrpura
                Color(0xFFFFF8E1), // Amarillo
                Color(0xFFFFE0B2)  // Naranja
            )
            colors[kotlin.math.abs(subjectHash) % colors.size]
        }
        
        val accentColor = remember(cardColor) {
            when(cardColor) {
                Color(0xFFE3F2FD) -> Color(0xFF1565C0)
                Color(0xFFE8F5E9) -> Color(0xFF2E7D32)
                Color(0xFFF3E5F5) -> Color(0xFF7B1FA2)
                Color(0xFFFFF8E1) -> Color(0xFFF57F17)
                Color(0xFFFFE0B2) -> Color(0xFFE65100)
                else -> Color.Gray
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.6f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = subjectAvg.subject, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Surface(
                        color = if (subjectAvg.average >= 4.0) Color(0xFF2E7D32) else Color(0xFFC62828),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Promedio: ${subjectAvg.average.toString().take(3)}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp), color = accentColor.copy(alpha = 0.2f))
                
                subjectAvg.grades.forEach { grade ->
                    GradeRow(grade, onEdit, onDelete)
                }
            }
        }
    }

    @Composable
    private fun GradeRow(grade: Grade, onEdit: (Grade) -> Unit, onDelete: (Grade) -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(grade.title, style = MaterialTheme.typography.bodyMedium)
                if (grade.weight != 1.0) {
                    Text("Ponderación: ${(grade.weight * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = grade.score.toString().take(3),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (grade.score >= 4.0) Color(0xFF2E7D32) else Color(0xFFC62828),
                    modifier = Modifier.padding(end = 8.dp)
                )
                if (isTeacher) {
                    IconButton(onClick = { onEdit(grade) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { onDelete(grade) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun AddGradeDialog(
        initialGrade: Grade? = null,
        onDismiss: () -> Unit, 
        onConfirm: (String, Double, String, Double) -> Unit
    ) {
        var title   by remember { mutableStateOf(initialGrade?.title ?: "") }
        var score   by remember { mutableStateOf(initialGrade?.score?.toString() ?: "") }
        var subject by remember { mutableStateOf(initialGrade?.subject ?: "") }
        var weight  by remember { mutableStateOf(initialGrade?.weight?.toString() ?: "1.0") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (initialGrade == null) "Nueva Calificación" else "Editar Calificación") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Asignatura") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título Evaluación") }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = score, onValueChange = { score = it }, label = { Text("Nota") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Peso (0-1)") }, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        val s = score.toDoubleOrNull() ?: 0.0
                        val w = weight.toDoubleOrNull() ?: 1.0
                        onConfirm(title, s, subject, w)
                    },
                    enabled = title.isNotBlank() && score.toDoubleOrNull() != null && subject.isNotBlank()
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
        )
    }
}
