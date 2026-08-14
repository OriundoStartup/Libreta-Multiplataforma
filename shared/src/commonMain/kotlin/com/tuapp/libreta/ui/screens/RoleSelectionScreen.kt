package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tuapp.libreta.domain.model.UserRole
import com.tuapp.libreta.navigation.AppNavigation
import com.tuapp.libreta.presentation.RoleSelectionScreenModel
import com.tuapp.libreta.presentation.RoleSelectionUiState
import libretamultiplataformaws.shared.generated.resources.Res
import libretamultiplataformaws.shared.generated.resources.icono_libreta
import org.jetbrains.compose.resources.painterResource

private val ChileBlue = Color(0xFF0039A6)
private val ChileRed  = Color(0xFFD52B1E)

data class RoleSelectionScreen(val isSwitchingRole: Boolean = false) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: RoleSelectionScreenModel = koinScreenModel()
        val state by model.state.collectAsState()

        var selectedRole by remember { mutableStateOf<UserRole?>(null) }
        var invitationCode by remember { mutableStateOf("") }
        var studentName by remember { mutableStateOf("") }
        var isExistingParent by remember { mutableStateOf(false) }
        var isExistingTeacher by remember { mutableStateOf(false) }
        var userEmail by remember { mutableStateOf("") }

        // CARGA INICIAL: Solo auto-redigir si NO estamos cambiando de rol manualmente
        LaunchedEffect(Unit) {
            model.checkExistingProfile(forceShowSelection = isSwitchingRole)
        }

        // DETECTAR ESTADO DEL PERFIL
        LaunchedEffect(state) {
            when (val s = state) {
                is RoleSelectionUiState.ProfileStatus -> {
                    isExistingParent = s.hasStudents
                    isExistingTeacher = s.hasTeacherRole
                    userEmail = s.userEmail
                }
                is RoleSelectionUiState.Success -> {
                    // Mantenemos la pantalla en éxito visualmente, 
                    // App.kt se encargará de la navegación real.
                    println("UI: Selección confirmada, esperando redirección global.")
                }
                else -> {}
            }
        }



        Box(Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(Res.drawable.icono_libreta),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.60f)))

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "¿Cuál es tu rol?",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Selecciona cómo usarás LibretApp",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(40.dp))

                // Role cards
                RoleCard(
                    title = "Soy Profesor",
                    subtitle = "Gestiono cursos, asistencia y comunicaciones",
                    selected = selectedRole == UserRole.TEACHER,
                    onClick = { selectedRole = UserRole.TEACHER }
                )
                Spacer(Modifier.height(16.dp))
                RoleCard(
                    title = "Soy Apoderado",
                    subtitle = "Sigo el progreso de mi hijo/a",
                    selected = selectedRole == UserRole.PARENT,
                    onClick = { selectedRole = UserRole.PARENT }
                )

                // Campos de validación según el rol seleccionado
                if (selectedRole == UserRole.PARENT && !isExistingParent) {
                    Spacer(Modifier.height(24.dp))
                    OutlinedTextField(
                        value = invitationCode,
                        onValueChange = { invitationCode = it.uppercase().take(6) },
                        label = { Text("Código de invitación", color = Color.White.copy(alpha = 0.75f)) },
                        placeholder = { Text("Ej: FBD007", color = Color.White.copy(alpha = 0.4f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ChileBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        label = { Text("Nombre del alumno", color = Color.White.copy(alpha = 0.75f)) },
                        placeholder = { Text("Nombre completo de tu hijo/a", color = Color.White.copy(alpha = 0.4f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ChileBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (selectedRole == UserRole.TEACHER && !isExistingTeacher) {
                    Spacer(Modifier.height(24.dp))
                    OutlinedTextField(
                        value = invitationCode,
                        onValueChange = { invitationCode = it.uppercase() },
                        label = { Text("Código de autorización docente", color = Color.White.copy(alpha = 0.75f)) },
                        placeholder = { Text("Requerido para nuevos profesores", color = Color.White.copy(alpha = 0.4f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ChileBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (selectedRole != null) {
                    val message = if (selectedRole == UserRole.PARENT) 
                        "Ya tienes alumnos vinculados a esta cuenta ($userEmail)."
                    else 
                        "Ya estás registrado como profesor con esta cuenta ($userEmail)."
                    
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "$message Puedes entrar directamente.",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(32.dp))

                when (state) {
                    is RoleSelectionUiState.Loading -> CircularProgressIndicator(color = Color.White)
                    else -> {
                        val isButtonEnabled = selectedRole != null && when (selectedRole) {
                            UserRole.PARENT -> isExistingParent || (invitationCode.length == 6 && studentName.isNotBlank())
                            UserRole.TEACHER -> isExistingTeacher || invitationCode.isNotBlank()
                            else -> false
                        }

                        Button(
                            onClick = { selectedRole?.let { model.confirmRole(it, invitationCode, studentName) } },
                            enabled = isButtonEnabled,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChileBlue,
                                contentColor = Color.White
                            )
                        ) {
                            val buttonText = if ((selectedRole == UserRole.PARENT && isExistingParent) || 
                                               (selectedRole == UserRole.TEACHER && isExistingTeacher)) 
                                               "Entrar a mi Cuenta" else "Continuar"
                            Text(
                                text = buttonText, 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 16.sp,
                                color = Color.White // Doble refuerzo de color para el texto
                            )
                        }
                        
                        // OPCIÓN PARA CAMBIAR DE CUENTA SI SE LOGUEÓ CON EL MAIL EQUIVOCADO
                        if (userEmail.isNotEmpty()) {
                            Spacer(Modifier.height(20.dp))
                            androidx.compose.material3.TextButton(
                                onClick = { model.signOut() },
                                enabled = state !is RoleSelectionUiState.Loading
                            ) {
                                Text(
                                    "¿No eres $userEmail? Usar otra cuenta",
                                    color = Color.White.copy(alpha = if (state is RoleSelectionUiState.Loading) 0.3f else 0.6f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        if (state is RoleSelectionUiState.Error) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                (state as RoleSelectionUiState.Error).message,
                                color = ChileRed,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleCard(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) ChileBlue else Color.White.copy(alpha = 0.3f)
    val bgColor     = if (selected) ChileBlue.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = ChileBlue, unselectedColor = Color.White)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif, fontSize = 16.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.7f),
                fontFamily = FontFamily.SansSerif, fontSize = 12.sp)
        }
    }
}
