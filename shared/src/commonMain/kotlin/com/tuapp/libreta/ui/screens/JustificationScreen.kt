package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.presentation.JustificationFormState
import com.tuapp.libreta.presentation.JustificationReason
import com.tuapp.libreta.presentation.JustificationScreenModel

data class JustificationScreen(
    val studentId: String,
    val parentId: String,
    val teacherId: String
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: JustificationScreenModel = koinScreenModel()
        val formState by model.formState.collectAsState()

        var selectedReason by remember { mutableStateOf(JustificationReason.HEALTH) }
        var description    by remember { mutableStateOf("") }
        var showDatePicker by remember { mutableStateOf(false) }
        var selectedDateMs by remember { mutableStateOf(currentEpochMs()) }
        var reasonExpanded by remember { mutableStateOf(false) }

        LaunchedEffect(formState) {
            if (formState is JustificationFormState.Sent) navigator.pop()
        }

        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMs)

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMs = it }
                        showDatePicker = false
                    }) { Text("Aceptar") }
                },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
            ) { DatePicker(state = datePickerState) }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = { IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }},
                    title = { Text("Justificar Inasistencia",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding).padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SectionLabel("Fecha de inasistencia")
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.padding(end = 8.dp))
                    Text("Seleccionar fecha (ms: $selectedDateMs)")
                }

                SectionLabel("Motivo")
                ExposedDropdownMenuBox(expanded = reasonExpanded, onExpandedChange = { reasonExpanded = it }) {
                    OutlinedTextField(
                        value = selectedReason.label, onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = reasonExpanded, onDismissRequest = { reasonExpanded = false }) {
                        JustificationReason.entries.forEach { reason ->
                            DropdownMenuItem(text = { Text(reason.label) },
                                onClick = { selectedReason = reason; reasonExpanded = false })
                        }
                    }
                }

                SectionLabel("Descripción")
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Describe brevemente el motivo...") }, maxLines = 5
                )

                OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AttachFile, null, modifier = Modifier.padding(end = 8.dp))
                    Text("Adjuntar certificado médico (opcional)")
                }

                val isSending = formState is JustificationFormState.Sending
                Button(
                    onClick = {
                        model.submitJustification(studentId, parentId, teacherId,
                            selectedDateMs, selectedReason, description)
                    },
                    enabled = description.isNotBlank() && !isSending,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (isSending) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Enviar Justificación", style = MaterialTheme.typography.labelLarge)
                }

                if (formState is JustificationFormState.Error) {
                    Text((formState as JustificationFormState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}
