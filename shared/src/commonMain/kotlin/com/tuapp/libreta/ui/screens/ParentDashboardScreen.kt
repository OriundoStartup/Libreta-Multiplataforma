package com.tuapp.libreta.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tuapp.libreta.navigation.AppNavigation
import com.tuapp.libreta.presentation.ParentDashboardScreenModel
import com.tuapp.libreta.presentation.ParentDashboardUiState
import com.tuapp.libreta.ui.components.EmptyStateView
import com.tuapp.libreta.ui.components.FullScreenError
import com.tuapp.libreta.ui.components.ShimmerCard
import com.tuapp.libreta.ui.components.StatusCard
import com.tuapp.libreta.ui.components.TimelineItem
import com.tuapp.libreta.ui.components.AppDrawer
import com.tuapp.libreta.ui.components.AppEntityHeader
import com.tuapp.libreta.ui.theme.StatusTheme
import com.tuapp.libreta.data.util.RutUtils
import com.tuapp.libreta.ui.components.AdaptiveGrid
import com.tuapp.libreta.ui.util.LocalWindowSize
import com.tuapp.libreta.ui.util.WindowSizeClass
import kotlinx.coroutines.launch

import com.tuapp.libreta.domain.model.UserRole

data class ParentDashboardScreen(val parentId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val model: ParentDashboardScreenModel = koinScreenModel()
        val state by model.state.collectAsState()
        val uiState = state.uiState
        val listState = rememberLazyListState()
        val fabExpanded by remember { derivedStateOf { !listState.canScrollBackward } }
        val navigator = LocalNavigator.currentOrThrow
        val snackbarHostState = remember { SnackbarHostState() }
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) { model.load() }

        LaunchedEffect(state.successMessage) {
            state.successMessage?.let { message ->
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
                model.clearSuccessMessage()
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawer(
                    onClose = { scope.launch { drawerState.close() } },
                    onNavigateToDashboard = { },
                    onNavigateToMessages = { navigator.push(AppNavigation.messages()) },
                    onNavigateToCompose = { /* Consistent UI */ },
                    onNavigateToProfile = { navigator.push(AppNavigation.profile()) },
                    onLogout = { model.logout() },
                    onSwitchAccount = { navigator.replaceAll(RoleSelectionScreen(isSwitchingRole = true)) },
                    userRole = UserRole.PARENT
                )
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menú")
                            }
                        },
                        title = { Text("Mi Dashboard", fontWeight = FontWeight.Bold) },
                        actions = {
                            IconButton(onClick = { 
                                if (uiState is ParentDashboardUiState.Success) {
                                    val ids = uiState.students.map { it.id.value }
                                    navigator.push(AppNavigation.notificationScreen(parentId, ids))
                                }
                            }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notificaciones")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    if (uiState is ParentDashboardUiState.Success || uiState is ParentDashboardUiState.NoStudents) {
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ExtendedFloatingActionButton(
                                onClick        = { navigator.push(AppNavigation.enrollment()) },
                                expanded       = fabExpanded,
                                icon           = { Icon(Icons.Default.School, null) },
                                text           = { Text("Inscribir Curso") },
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            if (uiState is ParentDashboardUiState.Success) {
                                ExtendedFloatingActionButton(
                                    onClick        = { navigator.push(AppNavigation.messages()) },
                                    expanded       = fabExpanded,
                                    icon           = { Icon(Icons.Default.Email, null) },
                                    text           = { Text("Ver Mensajes") },
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            ) { padding ->
                when (uiState) {
                    ParentDashboardUiState.Loading    -> ParentShimmer(padding)
                    ParentDashboardUiState.NoStudents -> EmptyStateView(
                        icon = Icons.Default.PersonAdd,
                        title = "Sin alumnos vinculados",
                        description = "Registra a tus hijos usando el código que te entregó el profesor para ver su progreso.",
                        actionText = "Inscribir a mi hijo",
                        onAction = { navigator.push(AppNavigation.enrollment()) },
                        modifier = Modifier.padding(padding)
                    )

                    is ParentDashboardUiState.Error   -> FullScreenError(uiState.message, padding) { model.load() }
                    is ParentDashboardUiState.Success -> {
                        val student = uiState.students[uiState.selectedIndex]
                        val windowSize = LocalWindowSize.current
                        val columns = when (windowSize.widthSizeClass) {
                            WindowSizeClass.COMPACT -> 2
                            else -> 4
                        }

                        LazyColumn(
                            state               = listState,
                            modifier            = Modifier.fillMaxSize(),
                            contentPadding      = PaddingValues(
                                start  = 16.dp, end = 16.dp,
                                top    = padding.calculateTopPadding() + 8.dp,
                                bottom = padding.calculateBottomPadding() + 88.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. Header del Apoderado
                            item {
                                AppEntityHeader(
                                    title = uiState.profile.name,
                                    subtitle = "Apoderado",
                                    initial = uiState.profile.name.firstOrNull() ?: 'A'
                                )
                            }

                            // 2. Selector de pupilo (si hay más de uno)
                            if (uiState.students.size > 1) {
                                item {
                                    ScrollableTabRow(
                                        selectedTabIndex = uiState.selectedIndex,
                                        edgePadding      = 0.dp,
                                        containerColor   = Color.Transparent,
                                        divider          = {}
                                    ) {
                                        uiState.students.forEachIndexed { index, pupil ->
                                            Tab(
                                                selected = index == uiState.selectedIndex,
                                                onClick  = { model.selectStudent(index) },
                                                text     = { 
                                                    Text(
                                                        text = pupil.name.split(" ")[0],
                                                        fontWeight = if (index == uiState.selectedIndex) FontWeight.Bold else FontWeight.Normal
                                                    ) 
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Status Cards (Resumen del Alumno) - AHORA ARRIBA
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    StatusCard(
                                        icon = Icons.Default.CheckCircle,
                                        value = "${student.attendancePercent}%",
                                        label = "Asistencia",
                                        containerColor = StatusTheme.successBackground,
                                        contentColor = StatusTheme.successContent,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatusCard(
                                        icon = Icons.Default.Email,
                                        value = if (student.pendingMessages > 0) "${student.pendingMessages}" else "Al día",
                                        label = "Mensajes",
                                        containerColor = if (student.pendingMessages > 0) StatusTheme.infoBackground else StatusTheme.infoBackground.copy(alpha = 0.5f),
                                        contentColor = StatusTheme.infoContent,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (columns > 2) {
                                        StatusCard(
                                            icon = Icons.Default.Grade,
                                            value = "Notas",
                                            label = "Libreta",
                                            containerColor = StatusTheme.purpleBackground,
                                            contentColor = StatusTheme.purpleContent,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // 4. Tarjeta Principal del Estudiante (Estilo Profesor)
                            item {
                                StudentMainCard(
                                    student = student,
                                    onNavigate = { nav -> navigator.push(nav) }
                                )
                            }

                            // 5. Timeline de Actividad Reciente
                            item {
                                Text(
                                    "Actividad Reciente",
                                    style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                                )
                            }

                            itemsIndexed(uiState.timeline, key = { _, e -> e.id }) { index, event ->
                                TimelineItem(
                                    event    = event,
                                    isLast   = index == uiState.timeline.lastIndex,
                                    onJustify = { navigator.push(AppNavigation.justificationForm(parentId = parentId)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddStudentDialog(
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var rut by remember { mutableStateOf("") }
    var rutError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Registrar Alumno") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Ingresa los datos del alumno para vincularlo a tu cuenta.", style = MaterialTheme.typography.bodySmall)
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre completo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )
                
                OutlinedTextField(
                    value = rut,
                    onValueChange = { 
                        val formatted = RutUtils.format(it)
                        if (formatted.length <= 12) {
                            rut = formatted
                            rutError = if (it.isNotEmpty() && !RutUtils.isValid(formatted)) "RUT inválido" else null
                        }
                    },
                    label = { Text("RUT (Opcional)") },
                    placeholder = { Text("12.345.678-9") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading,
                    isError = rutError != null,
                    supportingText = {
                        if (rutError != null) {
                            Text(rutError!!, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, rut.ifBlank { null }) },
                enabled = name.isNotBlank() && !isLoading && (rut.isBlank() || rutError == null)
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text("Registrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancelar") }
        }
    )
}

@Composable
private fun StudentMainCard(
    student: com.tuapp.libreta.presentation.StudentSummary,
    onNavigate: (cafe.adriel.voyager.core.screen.Screen) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Parte Superior: Avatar e Información básica
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = student.name.firstOrNull()?.uppercaseChar()?.toString() ?: "E",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    
                    Spacer(Modifier.height(4.dp))
                    
                    LinearProgressIndicator(
                        progress = { student.attendancePercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
                        color = when {
                            student.attendancePercent >= 85 -> Color(0xFF2E7D32)
                            student.attendancePercent >= 70 -> Color(0xFFF57F17)
                            else -> Color(0xFFC62828)
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    
                    Text(
                        text = "Alumno Regular • ${student.attendancePercent}% Asistencia",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                IconButton(
                    onClick = { onNavigate(AppNavigation.parentStudentDetail(student.id.value, student.name, student.rut)) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(20.dp))
                }
            }

            // Sección de Observación (Integrada)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(
                        text = student.lastNote.ifBlank { "Sin observaciones recientes" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Botones de Acción (Estilo Profesor)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { onNavigate(AppNavigation.studentGrades(student.id.value, student.name, "", isTeacher = false)) }) {
                        Icon(Icons.Default.Grade, null, tint = StatusTheme.purpleContent, modifier = Modifier.size(20.dp))
                    }
                    // Botón para ENVIAR justificación
                    IconButton(onClick = { onNavigate(AppNavigation.justificationForm(studentId = student.id.value)) }) {
                        Icon(Icons.Default.Edit, null, tint = StatusTheme.warningContent, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { onNavigate(AppNavigation.messages()) }) {
                        Icon(Icons.Default.Email, null, tint = StatusTheme.infoContent, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { onNavigate(AppNavigation.attendanceHistory(student.id.value, student.name)) }) {
                        Icon(Icons.Default.CheckCircle, null, tint = StatusTheme.successContent, modifier = Modifier.size(20.dp))
                    }
                }

                TextButton(onClick = { onNavigate(AppNavigation.justificationList(student.id.value)) }) {
                    Text("Ver Mis Trámites", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ── Shimmer ───────────────────────────────────────────────────────────────────

@Composable
private fun ParentShimmer(padding: PaddingValues) {
    LazyColumn(
        contentPadding      = PaddingValues(
            start  = 16.dp, end = 16.dp,
            top    = padding.calculateTopPadding() + 8.dp,
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled   = false
    ) {
        item { ShimmerBox(height = 56, widthFraction = 0.6f) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) { ShimmerBox(height = 80, modifier = Modifier.weight(1f)) }
            }
        }
        items(5) { ShimmerCard() }
    }
}

@Composable
private fun ShimmerBox(height: Int, widthFraction: Float = 1f, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "s")
    val x by transition.animateFloat(-400f, 1000f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "x")
    val brush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.surfaceContainerLow,
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.surfaceContainerLow
        ),
        start = Offset(x, 0f), end = Offset(x + 400f, 0f)
    )
    Box(modifier.fillMaxWidth(widthFraction).height(height.dp).clip(RoundedCornerShape(12.dp)).background(brush))
}
