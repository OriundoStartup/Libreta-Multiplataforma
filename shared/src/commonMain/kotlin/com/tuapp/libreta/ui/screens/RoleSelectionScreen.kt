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

object RoleSelectionScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: RoleSelectionScreenModel = koinScreenModel()
        val state by model.state.collectAsState()

        var selectedRole by remember { mutableStateOf<UserRole?>(null) }
        var invitationCode by remember { mutableStateOf("") }



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

                // Invitation code field — only for PARENT
                if (selectedRole == UserRole.PARENT) {
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
                }

                Spacer(Modifier.height(32.dp))

                when (state) {
                    is RoleSelectionUiState.Loading -> CircularProgressIndicator(color = Color.White)
                    else -> {
                        Button(
                            onClick = { selectedRole?.let { model.confirmRole(it, invitationCode) } },
                            enabled = selectedRole != null &&
                                    (selectedRole != UserRole.PARENT || invitationCode.length == 6),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ChileBlue)
                        ) {
                            Text("Continuar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
