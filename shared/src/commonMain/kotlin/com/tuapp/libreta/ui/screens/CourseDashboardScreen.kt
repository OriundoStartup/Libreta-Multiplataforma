package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tuapp.libreta.navigation.AppNavigation
import com.tuapp.libreta.ui.components.AppEntityHeader
import com.tuapp.libreta.presentation.CourseDashboardScreenModel
import com.tuapp.libreta.presentation.CourseDashboardUiState
import com.tuapp.libreta.ui.components.FullScreenError
import com.tuapp.libreta.ui.components.FullScreenLoading
import com.tuapp.libreta.ui.components.StatusCard
import com.tuapp.libreta.ui.components.AdaptiveGrid
import com.tuapp.libreta.ui.theme.StatusTheme
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
                        val courseToEdit = s.course ?: com.tuapp.libreta.domain.model.Course(
                            id = courseId,
                            name = courseName,
                            teacherId = "",
                            description = null,
                            subject = null,
                            grade = null,
                            schoolName = null,
                            inviteCode = "",
                            isActive = true,
                            createdAt = ""
                        )
                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. Header del Curso
                            AppEntityHeader(
                                title = courseName,
                                subtitle = "Centro de Gestión del Curso",
                                initial = courseName.firstOrNull() ?: 'C'
                            )

                            // 2. KPIs Rápidos
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatusCard(
                                    icon = Icons.Default.Person,
                                    value = "${s.studentCount}",
                                    label = "Alumnos",
                                    containerColor = StatusTheme.infoBackground,
                                    contentColor = StatusTheme.infoContent,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusCard(
                                    icon = Icons.Default.List,
                                    value = "${(s.attendanceRate * 100).toInt()}%",
                                    label = "Asistencia",
                                    containerColor = StatusTheme.successBackground,
                                    contentColor = StatusTheme.successContent,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusCard(
                                    icon = Icons.Default.Done,
                                    value = "${s.pendingJustificationsCount}",
                                    label = "Pendientes",
                                    containerColor = StatusTheme.warningBackground,
                                    contentColor = StatusTheme.warningContent,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // 3. Panel de Acciones (El Hub)
                            Text(
                                "Acciones Rápidas",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            val columns = when (windowSize.widthSizeClass) {
                                WindowSizeClass.COMPACT -> 1
                                else -> 2
                            }

                            val actions = listOf(
                                ActionItem("Lista de Alumnos", "Ver y gestionar estudiantes", Icons.Default.Person, MaterialTheme.colorScheme.primaryContainer) {
                                    navigator.push(AppNavigation.studentList(courseId))
                                },
                                ActionItem("Pasar Asistencia", "Registro diario de clase", Icons.Default.HowToReg, MaterialTheme.colorScheme.secondaryContainer) {
                                    navigator.push(AppNavigation.attendance(courseId, courseName))
                                },
                                ActionItem("Estadísticas", "Analíticas de asistencia", Icons.Default.List, MaterialTheme.colorScheme.tertiaryContainer) {
                                    navigator.push(AppNavigation.courseStats(courseId, courseName))
                                },
                                ActionItem("Enviar Aviso", "Comunicar a todo el curso", Icons.Default.Email, MaterialTheme.colorScheme.surfaceVariant) {
                                    navigator.push(AppNavigation.composeNotice(classId = courseId))
                                },
                                ActionItem("Historial de Avisos", "Ver comunicaciones enviadas", Icons.Default.Refresh, MaterialTheme.colorScheme.surfaceVariant) {
                                    navigator.push(AppNavigation.noticeList(com.tuapp.libreta.data.util.UuidString(courseId)))
                                },
                                ActionItem("Editar Curso", "Modificar datos básicos", Icons.Default.Edit, MaterialTheme.colorScheme.errorContainer) {
                                    navigator.push(AppNavigation.courseEdit(courseId, courseName, courseToEdit))
                                }
                            )

                            AdaptiveGrid(
                                items = actions,
                                columns = columns
                            ) { action ->
                                ActionButton(
                                    title = action.title,
                                    subtitle = action.subtitle,
                                    icon = action.icon,
                                    color = action.color,
                                    onClick = action.onClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private data class ActionItem(
        val title: String,
        val subtitle: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val color: Color,
        val onClick: () -> Unit
    )

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
