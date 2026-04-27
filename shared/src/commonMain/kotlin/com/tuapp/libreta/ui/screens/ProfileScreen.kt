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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tuapp.libreta.domain.model.UserRole
import com.tuapp.libreta.presentation.LinkedStudentInfo
import com.tuapp.libreta.presentation.ProfileScreenModel
import com.tuapp.libreta.presentation.ProfileUiState
import com.tuapp.libreta.presentation.TeacherCourseInfo
import com.tuapp.libreta.ui.components.FullScreenError
import com.tuapp.libreta.ui.components.FullScreenLoading

object ProfileScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: ProfileScreenModel = koinScreenModel()
        val state by model.state.collectAsState()
        var showDeleteDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            model.load()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Mi Perfil", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                )
            }
        ) { padding ->
            when (val s = state) {
                ProfileUiState.Loading -> FullScreenLoading()
                is ProfileUiState.Success -> {
                    ProfileContent(
                        padding = padding,
                        state = s,
                        model = model,
                        onSignOut = { 
                            model.signOut() 
                            navigator.popUntilRoot()
                        },
                        onDeleteAccount = { model.deleteAccount() },
                        showDeleteDialog = showDeleteDialog,
                        onShowDeleteDialog = { showDeleteDialog = it }
                    )
                }
                is ProfileUiState.Error -> FullScreenError(s.message, padding) { model.load() }
                ProfileUiState.Saved -> {
                   // Ya se manejó con el load() automático en el model
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    padding: PaddingValues,
    state: ProfileUiState.Success,
    model: ProfileScreenModel,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    showDeleteDialog: Boolean,
    onShowDeleteDialog: (Boolean) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 8.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Text(state.profile.fullName.firstOrNull()?.uppercaseChar()?.toString() ?: "U", 
                            style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(state.profile.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(state.profile.role.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item { SectionLabel("Mis Datos") }
        
        item {
            var nameText by remember { mutableStateOf(state.profile.fullName) }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Nombre Completo") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (nameText != state.profile.fullName) {
                            TextButton(onClick = { model.saveName(nameText) }) {
                                Text("Guardar")
                            }
                        }
                    }
                )
            }
        }

        if (state.profile.role == UserRole.TEACHER) {
            item { SectionLabel("Mis Cursos") }
            items(state.teacherCourses) { course ->
                TeacherCourseCard(course, 
                    onGenerateCode = { model.generateCodeForCourse(course.courseId.value) },
                    onClearCode = { model.clearCourseCode(course.courseId.value) }
                )
            }
        } else {
            item { SectionLabel("Hijos Vinculados") }
            items(state.linkedStudents) { student ->
                ParentStudentCard(student)
            }
        }

        item { SectionLabel("Zona de Peligro") }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Acciones permanentes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(
                        onClick = { onShowDeleteDialog(true) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Eliminar mi cuenta") }
                }
            }
        }

        item {
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { onShowDeleteDialog(false) },
            title = { Text("¿Eliminar cuenta?") },
            text = { Text("Se borrarán todos tus datos. Esta acción es irreversible.") },
            confirmButton = {
                TextButton(onClick = { onShowDeleteDialog(false); onDeleteAccount() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowDeleteDialog(false) }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun TeacherCourseCard(course: TeacherCourseInfo, onGenerateCode: () -> Unit, onClearCode: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Curso ID: ${course.courseId.value.take(8)}...", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onGenerateCode) { Icon(Icons.Default.Key, null) }
            }
            course.generatedCode?.let { code ->
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(code, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onClearCode) { Text("Cerrar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentStudentCard(student: LinkedStudentInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                Text(student.studentName[0].toString())
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(student.studentName, fontWeight = FontWeight.Bold)
                Text(student.courseName, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
