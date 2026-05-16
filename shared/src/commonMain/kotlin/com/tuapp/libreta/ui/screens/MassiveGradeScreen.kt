package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.presentation.MassiveGradeScreenModel
import com.tuapp.libreta.presentation.MassiveGradeUiState
import com.tuapp.libreta.ui.components.FullScreenLoading

data class MassiveGradeScreen(
    val courseId: String,
    val courseName: String
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: MassiveGradeScreenModel = koinScreenModel(
            parameters = { org.koin.core.parameter.parametersOf(UuidString(courseId)) }
        )
        val state by model.state.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    },
                    title = {
                        Column {
                            Text("Carga Masiva de Notas", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(courseName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
            },
            bottomBar = {
                if (state is MassiveGradeUiState.Success) {
                    val s = state as MassiveGradeUiState.Success
                    Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
                        Button(
                            onClick = { model.saveAll() },
                            enabled = !s.isSaving && s.subject.isNotBlank() && s.title.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (s.isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            else {
                                Icon(Icons.Default.Check, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Guardar Todas las Notas", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        ) { padding ->
            when (val s = state) {
                MassiveGradeUiState.Loading -> FullScreenLoading(padding)
                is MassiveGradeUiState.Error -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { navigator.pop() }) { Text("Volver") }
                    }
                }
                is MassiveGradeUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header de la evaluación
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Datos de la Evaluación", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = s.subject,
                                        onValueChange = { model.updateHeader(subject = it) },
                                        label = { Text("Asignatura") },
                                        placeholder = { Text("Ej: Matemáticas") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = s.title,
                                        onValueChange = { model.updateHeader(title = it) },
                                        label = { Text("Título de la Evaluación") },
                                        placeholder = { Text("Ej: Prueba Coeficiente 1") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = s.weight,
                                        onValueChange = { model.updateHeader(weight = it) },
                                        label = { Text("Ponderación (0.0 a 1.0)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true
                                    )
                                }
                            }
                        }

                        item {
                            Text("Lista de Alumnos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        items(s.students) { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(entry.studentName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                OutlinedTextField(
                                    value = entry.score,
                                    onValueChange = { model.updateStudentScore(entry.studentId, it) },
                                    modifier = Modifier.width(80.dp),
                                    placeholder = { Text("1.0") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                        
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}
