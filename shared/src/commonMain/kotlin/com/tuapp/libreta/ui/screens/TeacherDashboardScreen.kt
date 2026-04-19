package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tuapp.libreta.domain.model.Course
import com.tuapp.libreta.navigation.AppNavigation
import com.tuapp.libreta.presentation.TeacherDashboardScreenModel
import com.tuapp.libreta.presentation.TeacherDashboardUiState

object TeacherDashboardScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: TeacherDashboardScreenModel = koinScreenModel()
        val state by model.state.collectAsState()
        val generatedCode by model.generatedCode.collectAsState()
        var showCreateDialog by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Mi Dashboard", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { navigator.push(AppNavigation.profile()) }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Perfil")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    icon    = { Icon(Icons.Default.Add, null) },
                    text    = { Text("Nuevo Curso") }
                )
            }
        ) { padding ->
            when (val s = state) {
                is TeacherDashboardUiState.Loading -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is TeacherDashboardUiState.Error -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp))
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { model.load() }) { Text("Reintentar") }
                    }
                }

                is TeacherDashboardUiState.Success -> LazyColumn(
                    modifier       = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ProfileHeader(
                            name = s.profile.name,
                            role = "Profesor"
                        )
                    }

                    item {
                        Text(
                            text  = "Mis Cursos (${s.courses.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (s.courses.isEmpty()) {
                        item {
                            EmptyCoursesState()
                        }
                    } else {
                        items(s.courses, key = { it.id }) { course ->
                            CourseCard(
                                course         = course,
                                onClick        = {
                                    navigator.push(
                                        StudentListScreen(classId = course.id, className = course.name)
                                    )
                                },
                                onGenerateCode = { model.generateInviteCodeForCourse(course) }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        if (showCreateDialog) {
            CreateCourseDialog(
                onDismiss = { },
                onCreate  = { name, grade, school ->
                    model.createCourse(name, grade, school)
                    showCreateDialog = false
                }
            )
        }

        generatedCode?.let { code ->
            InviteCodeDialog(code = code, onDismiss = { model.clearGeneratedCode() })
        }
    }
}

@Composable
private fun ProfileHeader(name: String, role: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(64.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = name.firstOrNull()?.uppercaseChar()?.toString() ?: "P",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape  = RoundedCornerShape(8.dp),
                    color  = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(role,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CourseCard(course: Course, onClick: () -> Unit, onGenerateCode: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Groups, null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(course.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text("Nivel: ${course.grade ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onGenerateCode) {
                Icon(Icons.Default.Add, contentDescription = "Ver código",
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun EmptyCoursesState() {
    Box(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Groups, null,
                Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))
            Text("Aún no tienes cursos",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Toca + para crear tu primer curso",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun InviteCodeDialog(code: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¡Curso Creado!", fontWeight = FontWeight.Bold) },
        text  = {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()) {
                Text("Comparte este código con los apoderados:",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text     = code,
                        style    = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 8.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color    = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick  = { com.tuapp.libreta.data.util.ClipboardHelper.copyToClipboard(code) },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Copiar")
                    }
                    Button(
                        onClick  = { 
                            com.tuapp.libreta.data.util.ShareHelper.shareText(
                                "¡Hola! Únete a mi curso en LibretApp usando el código: $code"
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Compartir")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Entendido") }
        }
    )
}

@Composable
private fun CreateCourseDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, grade: String, school: String) -> Unit
) {
    var schoolName by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Curso", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = schoolName,
                    onValueChange = { schoolName = it },
                    label = { Text("Nombre del Colegio") },
                    placeholder = { Text("Ej: Colegio San Patricio") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = grade,
                    onValueChange = { grade = it },
                    label = { Text("Nivel (ej: 4° Básico)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Letra / Nombre (ej: A)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (schoolName.isNotBlank() && grade.isNotBlank()) {
                        onCreate(name, grade, schoolName)
                    }
                },
                enabled = schoolName.isNotBlank() && grade.isNotBlank()
            ) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
