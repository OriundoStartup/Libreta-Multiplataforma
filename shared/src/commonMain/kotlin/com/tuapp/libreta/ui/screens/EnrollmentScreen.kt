package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tuapp.libreta.presentation.EnrollmentScreenModel
import com.tuapp.libreta.presentation.EnrollmentUiState
import com.tuapp.libreta.ui.components.EmptyStateView
import com.tuapp.libreta.data.util.RutUtils

object EnrollmentScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: EnrollmentScreenModel = koinScreenModel()
        val state by model.state.collectAsState()

        var inviteCode by remember { mutableStateOf("") }
        var studentName by remember { mutableStateOf("") }
        var studentRut by remember { mutableStateOf("") }
        var rutError by remember { mutableStateOf<String?>(null) }

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
                            "Inscribir Hijo",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                when (val s = state) {
                    EnrollmentUiState.Loading -> {
                        Text(
                            "Ingresa el código de invitación que te entregó el profesor para inscribir a tu hijo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = inviteCode,
                            onValueChange = { inviteCode = it.uppercase() },
                            label = { Text("Código de invitación") },
                            placeholder = { Text("Ej: ABC123") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = { model.searchCourse(inviteCode) }
                            )
                        )

                        Button(
                            onClick = { model.searchCourse(inviteCode) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled = inviteCode.isNotBlank()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Buscar Curso")
                        }
                    }

                    EnrollmentUiState.Searching -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is EnrollmentUiState.CoursesFound -> {
                        Text(
                            "Curso encontrado:",
                            style = MaterialTheme.typography.labelLarge
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                s.courses.firstOrNull()?.let { course ->
                                    Text(
                                        course.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    course.grade?.let {
                                        Text(
                                            "Grado: $it",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }

                                HorizontalDivider()

                                OutlinedTextField(
                                    value = studentName,
                                    onValueChange = { studentName = it },
                                    label = { Text("Nombre del estudiante") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = studentRut,
                                    onValueChange = { 
                                        val formatted = RutUtils.format(it)
                                        if (formatted.length <= 12) {
                                            studentRut = formatted
                                            rutError = if (it.isNotEmpty() && !RutUtils.isValid(formatted)) "RUT inválido" else null
                                        }
                                    },
                                    label = { Text("RUT (opcional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    isError = rutError != null,
                                    supportingText = {
                                        if (rutError != null) {
                                            Text(rutError!!, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                )

                                Button(
                                    onClick = {
                                        s.courses.firstOrNull()?.let { course ->
                                            model.enrollStudent(course.id, studentName, studentRut.takeIf { it.isNotBlank() })
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    enabled = studentName.isNotBlank() && (studentRut.isBlank() || rutError == null)
                                ) {
                                    Text("Confirmar Inscripción")
                                }
                            }
                        }
                    }

                    EnrollmentUiState.Enrolling -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text("Inscribiendo...")
                            }
                        }
                    }

                    EnrollmentUiState.Success -> EmptyStateView(
                        icon = Icons.Default.Check,
                        title = "¡Inscripción exitosa!",
                        description = "Tu hijo ha sido inscrito correctamente en el curso.",
                        actionText = "Volver al inicio",
                        onAction = { navigator.pop() }
                    )

                    is EnrollmentUiState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                s.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        Button(
                            onClick = { model.reset() },
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text("Intentar de nuevo")
                        }
                    }
                }
            }
        }
    }
}