package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tuapp.libreta.navigation.AppNavigation
import com.tuapp.libreta.presentation.LoginScreenModel
import com.tuapp.libreta.presentation.LoginUiState
import libretamultiplataformaws.shared.generated.resources.Res
import libretamultiplataformaws.shared.generated.resources.icono_libreta
import org.jetbrains.compose.resources.painterResource

import com.tuapp.libreta.ui.theme.ChileBlue
import com.tuapp.libreta.ui.theme.ChileRed

object LoginScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: LoginScreenModel = koinScreenModel()
        val state by model.state.collectAsState()
        val googleLauncher = rememberGoogleAuthLauncher()

        LaunchedEffect(state) {
            when (state) {
                is LoginUiState.Success          -> navigator.replace(AppNavigation.initialScreen((state as LoginUiState.Success).role, (state as LoginUiState.Success).userId.value))
                is LoginUiState.NeedsRoleSelection -> navigator.replace(RoleSelectionScreen)
                else -> Unit
            }
        }

        Box(Modifier.fillMaxSize()) {

            // ── Full-screen background image ──────────────────────────────────
            Image(
                painter            = painterResource(Res.drawable.icono_libreta),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize()
            )

            // ── Dark overlay for text legibility ──────────────────────────────
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.52f)))

            // ── Content ───────────────────────────────────────────────────────
            Column(
                modifier            = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.height(1.dp))

                // ── Top: App title ────────────────────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 80.dp)
                ) {
                    Text(
                        text  = "LibretApp",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color.White,
                            fontSize   = 64.sp,
                            letterSpacing = (-2).sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text  = "Estudiantil",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily    = FontFamily.SansSerif,
                            fontStyle     = FontStyle.Normal,
                            color         = Color.White.copy(alpha = 0.85f),
                            fontSize      = 14.sp,
                            letterSpacing = 4.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                // ── Bottom: Google button + brand ─────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Google button
                    when (state) {
                        LoginUiState.Loading -> CircularProgressIndicator(color = Color.White)
                        else -> {
                            GoogleSignInButton(
                                onClick  = { 
                                    println("LoginScreen: Botón Google presionado")
                                    model.signInWithGoogle(googleLauncher) 
                                },
                                enabled  = state !is LoginUiState.Loading
                            )
                            if (state is LoginUiState.Error) {
                                Text(
                                    (state as LoginUiState.Error).message,
                                    color     = ChileRed,
                                    style     = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Brand line
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = ChileBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)) { append("Oriundo") }
                            withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)) { append("  Startup") }
                            withStyle(SpanStyle(color = ChileRed,  fontWeight = FontWeight.SemiBold, fontSize = 14.sp)) { append("  Chile") }
                        },
                        fontFamily = FontFamily.SansSerif,
                        textAlign  = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ── Official Google Sign-In button ────────────────────────────────────────────

@Composable
private fun GoogleSignInButton(onClick: () -> Unit, enabled: Boolean) {
    val gBlue   = Color(0xFF4285F4)
    val gRed    = Color(0xFFEA4335)
    val gYellow = Color(0xFFFBBC05)
    val gGreen  = Color(0xFF34A853)

    Button(
        onClick   = onClick,
        enabled   = enabled,
        modifier  = Modifier.fillMaxWidth().height(52.dp),
        shape     = RoundedCornerShape(4.dp),
        colors    = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF3C4043)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            androidx.compose.foundation.Canvas(Modifier.size(20.dp)) {
                val cx = size.width / 2f; val cy = size.height / 2f; val r = size.width / 2f
                drawArc(gBlue,   -60f, 120f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.28f, cap = androidx.compose.ui.graphics.StrokeCap.Butt))
                drawArc(gRed,   -180f, 120f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.28f, cap = androidx.compose.ui.graphics.StrokeCap.Butt))
                drawArc(gYellow,  60f, 120f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.28f, cap = androidx.compose.ui.graphics.StrokeCap.Butt))
                drawArc(gGreen,  180f, 120f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.28f, cap = androidx.compose.ui.graphics.StrokeCap.Butt))
                drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(cx, cy - r * 0.14f), size = androidx.compose.ui.geometry.Size(r, r * 0.28f))
                drawCircle(Color.White, r * 0.44f, androidx.compose.ui.geometry.Offset(cx, cy))
            }
            Spacer(Modifier.width(12.dp))
            Text("Iniciar sesión con Google", style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, color = Color(0xFF3C4043), fontSize = 15.sp))
        }
    }
}
