package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tuapp.libreta.navigation.AppNavigation
import com.tuapp.libreta.presentation.CourseDashboardScreenModel
import com.tuapp.libreta.presentation.CourseDashboardUiState
import com.tuapp.libreta.ui.components.FullScreenError
import com.tuapp.libreta.ui.components.FullScreenLoading
import com.tuapp.libreta.ui.components.StatusCard
import com.tuapp.libreta.ui.util.LocalWindowSize
import com.tuapp.libreta.ui.util.WindowSizeClass

data class CourseDashboardScreen(val courseId: String, val courseName: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: CourseDashboardScreenModel = koinScreenModel()
        val state by model.state.collectAsState()
        val windowSize = LocalWindowSize.current

        LaunchedEffect(courseId) {
            model.load(courseId)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(courseName, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (val s = state) {
                    is CourseDashboardUiState.Loading -> FullScreenLoading()
                    is CourseDashboardUiState.Error -> FullScreenError(s.message) { model.load(courseId) }
                    is CourseDashboardUiState.Success -> {
                        // Usamos Column + verticalScroll en lugar de LazyGrid para estabilidad en Wasm
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. Header del Curso
                            CourseHeader(courseName)

                            // 2. KPIs Rápidos
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatusCard(
                                    icon = Icons.Default.Groups,
                                    value = "${s.studentCount}",
                                    label = "Alumnos",
                                    containerColor = Color(0xFFE3F2FD),
                                    contentColor = Color(0xFF1565C0),
                                    modifier = Modifier.weight(1f)
                                )
                                StatusCard(
                                    icon = Icons.Default.TableChart,
                                    value = "${(s.attendanceRate * 100).toInt()}%",
                                    label = "Asistencia",
                                    containerColor = Color(0xFFE8F5E9),
                                    contentColor = Color(0xFF2E7D32),
                                    modifier = Modifier.weight(1f)
                                )
                                StatusCard(
                                    icon = Icons.Default.AssignmentTurnedIn,
                                    value = "${s.pendingJustificationsCount}",
                                    label = "Pendientes",
                                    containerColor = Color(0xFFFFF8E1),
                                    contentColor = Color(0xFFF57F17),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // 3. Panel de Acciones (El Hub)
                            Text(
                                "Acciones Rápidas",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // Grid manual usando Rows para evitar errores de Lazy en Wasm
                            val isCompact = windowSize.widthSizeClass == WindowSizeClass.COMPACT
                            
                            if (isCompact) {
                                ActionButton("Lista de Alumnos", "Ver y gestionar estudiantes", Icons.Default.Groups, MaterialTheme.colorScheme.primaryContainer) {
                                    navigator.push(AppNavigation.studentList(courseId))
                                }
                                ActionButton("Pasar Asistencia", "Registro diario de clase", Icons.Default.HowToReg, MaterialTheme.colorScheme.secondaryContainer) {
                                    navigator.push(AppNavigation.attendance(courseId, courseName))
                                }
                                ActionButton("Estadísticas", "Analíticas de asistencia", Icons.Default.TableChart, MaterialTheme.colorScheme.tertiaryContainer) {
                                    navigator.push(AppNavigation.courseStats(courseId, courseName))
                                }
                                ActionButton("Enviar Aviso", "Comunicar a todo el curso", Icons.Default.MailOutline, MaterialTheme.colorScheme.surfaceVariant) {
                                    navigator.push(AppNavigation.composeNotice(classId = courseId))
                                }
                                ActionButton("Historial de Avisos", "Ver comunicaciones enviadas", Icons.Default.History, MaterialTheme.colorScheme.surfaceVariant) {
                                    navigator.push(AppNavigation.noticeList(com.tuapp.libreta.data.util.UuidString(courseId)))
                                }
                                ActionButton("Editar Curso", "Modificar datos básicos", Icons.Default.Edit, MaterialTheme.colorScheme.errorContainer) {
                                    s.course?.let { navigator.push(AppNavigation.courseEdit(courseId, courseName, it)) }
                                }
                            } else {
                                // Grid de 2 columnas manual
                                GridRow {
                                    ActionButton("Lista de Alumnos", "Ver y gestionar estudiantes", Icons.Default.Groups, MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f)) {
                                        navigator.push(AppNavigation.studentList(courseId))
                                    }
                                    ActionButton("Pasar Asistencia", "Registro diario de clase", Icons.Default.HowToReg, MaterialTheme.colorScheme.secondaryContainer, Modifier.weight(1f)) {
                                        navigator.push(AppNavigation.attendance(courseId, courseName))
                                    }
                                }
                                GridRow {
                                    ActionButton("Estadísticas", "Analíticas de asistencia", Icons.Default.TableChart, MaterialTheme.colorScheme.tertiaryContainer, Modifier.weight(1f)) {
                                        navigator.push(AppNavigation.courseStats(courseId, courseName))
                                    }
                                    ActionButton("Enviar Aviso", "Comunicar a todo el curso", Icons.Default.MailOutline, MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f)) {
                                        navigator.push(AppNavigation.composeNotice(classId = courseId))
                                    }
                                }
                                GridRow {
                                    ActionButton("Historial de Avisos", "Ver comunicaciones enviadas", Icons.Default.History, MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f)) {
                                        navigator.push(AppNavigation.noticeList(com.tuapp.libreta.data.util.UuidString(courseId)))
                                    }
                                    ActionButton("Editar Curso", "Modificar datos básicos", Icons.Default.Edit, MaterialTheme.colorScheme.errorContainer, Modifier.weight(1f)) {
                                        s.course?.let { navigator.push(AppNavigation.courseEdit(courseId, courseName, it)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun GridRow(content: @Composable RowScope.() -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }

    @Composable
    private fun CourseHeader(name: String) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                        text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "C",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Text("Centro de Gestión del Curso", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    @Composable
    private fun ActionButton(
        title: String, 
        subtitle: String, 
        icon: androidx.compose.ui.graphics.vector.ImageVector, 
        color: Color, 
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        Card(
            modifier = modifier.fillMaxWidth().clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = color)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
