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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.AnnotatedString
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

object ProfileScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator  = LocalNavigator.currentOrThrow
        val model: ProfileScreenModel = koinScreenModel()
        val state by model.state.collectAsState()

        LaunchedEffect(state) {
            if (state is ProfileUiState.Saved) {
                // El model.load() ya se llamó en el ScreenModel tras guardar,
                // pero si queremos volver atrás:
                // navigator.pop() 
            }
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
                is ProfileUiState.Loading -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is ProfileUiState.Error -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 32.dp))
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { model.load() }) { Text("Reintentar") }
                    }
                }

                is ProfileUiState.Success, is ProfileUiState.Saved -> {
                    val success = (s as? ProfileUiState.Success) ?: return@Scaffold
                    ProfileContent(
                        state   = success,
                        padding = padding,
                        onSave  = { model.saveName(it) },
                        onGenerateCode = { model.generateCodeForCourse(it) },
                        onClearCode    = { model.clearCourseCode(it) },
                        onSignOut = {
                            model.signOut()
                            navigator.replaceAll(LoginScreen)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    state: ProfileUiState.Success,
    padding: PaddingValues,
    onSave: (String) -> Unit,
    onGenerateCode: (String) -> Unit,
    onClearCode: (String) -> Unit,
    onSignOut: () -> Unit
) {
    var nameInput by remember(state.profile.fullName) { mutableStateOf(state.profile.fullName) }

    LazyColumn(
        modifier       = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Avatar + badge ────────────────────────────────────────────────────
        item {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(80.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = state.profile.fullName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text     = if (state.profile.role == UserRole.TEACHER) "Profesor" else "Apoderado",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style    = MaterialTheme.typography.labelMedium,
                        color    = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ── Editable name ─────────────────────────────────────────────────────
        item {
            OutlinedTextField(
                value         = nameInput,
                onValueChange = { nameInput = it },
                label         = { Text("Nombre completo") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
        }

        // ── Email (read-only) ────────────────────────────────────────────────
        item {
            OutlinedTextField(
                value = state.profile.email,
                onValueChange = {},
                label = { Text("Correo electrónico") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        // ── Save button ───────────────────────────────────────────────────────
        item {
            Button(
                onClick  = { onSave(nameInput) },
                enabled  = nameInput.isNotBlank() && nameInput != state.profile.fullName,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Guardar cambios") }
        }

        // ── Role-specific section ─────────────────────────────────────────────
        if (state.profile.role == UserRole.TEACHER) {
            item {
                Text(
                    "Mis Cursos",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            if (state.teacherCourses.isEmpty()) {
                item {
                    Text(
                        "No tienes cursos asignados aún.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.teacherCourses, key = { it.courseId.value }) { course ->
                    TeacherCourseCard(
                        course         = course,
                        onGenerateCode = { onGenerateCode(course.courseId.toString()) },
                        onClearCode    = { onClearCode(course.courseId.toString()) }
                    )
                }
            }
        } else {
            // PRIORIDAD 3: Soporte multi-hijo
            item {
                Text(
                    if (state.linkedStudents.size > 1) "Mis Hijos/as" else "Mi Hijo/a",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            if (state.linkedStudents.isEmpty()) {
                item { ParentStudentCard(null) }
            } else {
                items(state.linkedStudents) { student ->
                    ParentStudentCard(student)
                }
            }
        }

        // ── Sign out ──────────────────────────────────────────────────────────
        item { Spacer(Modifier.height(8.dp)) }
        item {
            OutlinedButton(
                onClick  = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Cerrar sesión") }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

// ── Teacher course card ───────────────────────────────────────────────────────

@Composable
private fun TeacherCourseCard(
    course: TeacherCourseInfo,
    onGenerateCode: () -> Unit,
    onClearCode: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Groups, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(course.courseId.value,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text("${course.studentCount} alumno${if (course.studentCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onGenerateCode) {
                    Icon(Icons.Default.Key, contentDescription = "Generar código",
                        tint = MaterialTheme.colorScheme.primary)
                }
            }

            course.generatedCode?.let { code ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text  = code,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight    = FontWeight.Bold,
                                letterSpacing = 4.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row {
                            IconButton(onClick = {
                                com.tuapp.libreta.data.util.ClipboardHelper.copyToClipboard(code)
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
                Text("Válido por 7 días",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onClearCode) { Text("Cerrar") }
            }
        }
    }
}

// ── Parent student card ───────────────────────────────────────────────────────

@Composable
private fun ParentStudentCard(linked: LinkedStudentInfo?) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape    = RoundedCornerShape(12.dp)
    ) {
        if (linked == null) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sin alumno vinculado",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text(
                    "Para vincular a tu hijo/a, pide al profesor el código de invitación " +
                    "e ingrésalo en la pantalla de selección de rol.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = linked.studentName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Column {
                    Text(linked.studentName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(linked.courseName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
