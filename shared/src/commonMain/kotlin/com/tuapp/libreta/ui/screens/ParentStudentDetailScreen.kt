package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tuapp.libreta.presentation.ParentStudentDetailScreenModel

data class ParentStudentDetailScreen(
    val studentId: String,
    val initialName: String,
    val initialRut: String?
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: ParentStudentDetailScreenModel = koinScreenModel()
        val state by model.state.collectAsState()

        var name by remember { mutableStateOf(initialName) }
        var rut by remember { mutableStateOf(initialRut ?: "") }
        var showDeleteDialog by remember { mutableStateOf(false) }

        LaunchedEffect(state.isDeleted, state.success) {
            if (state.isDeleted || state.success) navigator.pop()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    title = { Text("Gestionar Alumno", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Información del Estudiante", style = MaterialTheme.typography.labelLarge)
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre completo") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                )

                OutlinedTextField(
                    value = rut,
                    onValueChange = { rut = it },
                    label = { Text("RUT (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                )

                Button(
                    onClick = { model.updateStudent(studentId, name, rut.ifBlank { null }) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = name.isNotBlank() && !state.isLoading
                ) {
                    if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else {
                        Icon(Icons.Default.Save, null, modifier = Modifier.padding(end = 8.dp))
                        Text("Guardar Cambios")
                    }
                }

                TextButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    enabled = !state.isLoading
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.padding(end = 8.dp))
                    Text("Desvincular Alumno de mi cuenta")
                }

                if (state.error != null) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("¿Desvincular Alumno?") },
                text = { Text("Esta acción eliminará al alumno de tu lista y dejarás de recibir sus notificaciones. El profesor seguirá teniendo el registro pero tú ya no estarás vinculado.") },
                confirmButton = {
                    TextButton(
                        onClick = { model.unlinkStudent(studentId) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Confirmar") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
                }
            )
        }
    }
}
