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
import com.tuapp.libreta.data.util.RutUtils
import kotlinx.coroutines.launch

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
                    onSwitchAccount = { navigator.replaceAll(RoleSelectionScreen) }
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
                        title = { Text("Panel Apoderado", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        actions = {
                            IconButton(onClick = { 
                                if (uiState is ParentDashboardUiState.Success) {
                                    val ids = uiState.students.map { it.id.value }
                                    navigator.push(AppNavigation.notificationScreen(parentId, ids))
                                }
                            }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notificaciones")
                            }
                            IconButton(onClick = { navigator.push(AppNavigation.profile()) }) {
                                Icon(Icons.Default.AccountCircle, contentDescription = "Perfil", modifier = Modifier.size(28.dp))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    if (uiState is ParentDashboardUiState.Success || uiState is ParentDashboardUiState.NoStudents) {
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ExtendedFloatingActionButton(
                                onClick        = { navigator.push(AppNavigation.enrollment()) },
                                expanded       = fabExpanded,
                                icon           = { Icon(Icons.Default.School, contentDescription = null) },
                                text           = { Text("Inscribir Curso") },
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            if (uiState is ParentDashboardUiState.Success) {
                                ExtendedFloatingActionButton(
                                    onClick        = { navigator.push(AppNavigation.messages()) },
                                    expanded       = fabExpanded,
                                    icon           = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                                    text           = { Text("Enviar Comunicación") },
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ) { padding ->
                when (uiState) {
                    ParentDashboardUiState.Loading    -> ParentShimmer(padding)
                    ParentDashboardUiState.NoStudents -> EmptyStateView(
                        icon = Icons.Default.PersonAdd,
                        title = "Sin alumnos vinculados",
                        description = "Registra a tus hijos para ver su asistencia y comunicaciones escolares.",
                        actionText = "Ir a mi perfil",
                        onAction = { navigator.push(AppNavigation.profile()) },
                        modifier = Modifier.padding(padding)
                    )

                    is ParentDashboardUiState.Error   -> FullScreenError(uiState.message, padding) { model.load() }
                    is ParentDashboardUiState.Success -> {
                        val student = uiState.students[uiState.selectedIndex]

                        LazyColumn(
                            state               = listState,
                            contentPadding      = PaddingValues(
                                start  = 16.dp, end = 16.dp,
                                top    = padding.calculateTopPadding() + 8.dp,
                                bottom = padding.calculateBottomPadding() + 88.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // ── Selector de pupilo (tabs) ─────────────────────
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
                                                text     = { Text(pupil.name.split(" ")[0]) }
                                            )
                                        }
                                    }
                                }
                            }

                            // ── Header del estudiante ─────────────────────────
                            item {
                                StudentHeader(
                                    name = student.name,
                                    percent = student.attendancePercent,
                                    studentId = student.id.value,
                                    studentRut = student.rut,
                                    navigator = navigator,
                                    onViewHistory = {
                                        navigator.push(AppNavigation.attendanceHistory(student.id.value, student.name))
                                    }
                                )
                            }

                            // ── Status cards ──────────────────────────────────
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    StatusCard(
                                        icon           = Icons.Default.CheckCircle,
                                        value          = "${student.attendancePercent}%",
                                        label          = "Asistencia",
                                        containerColor = Color(0xFFE8F5E9),
                                        contentColor   = Color(0xFF2E7D32),
                                        modifier       = Modifier.weight(1f)
                                    )
                                    StatusCard(
                                        icon           = Icons.Default.Email,
                                        value          = "${student.pendingMessages}",
                                        label          = "Mensajes",
                                        containerColor = Color(0xFFE3F2FD),
                                        contentColor   = Color(0xFF1565C0),
                                        modifier       = Modifier.weight(1f)
                                    )
                                    StatusCard(
                                        icon           = Icons.Default.Grade,
                                        value          = "Notas",
                                        label          = "Libreta",
                                        containerColor = Color(0xFFF3E5F5),
                                        contentColor   = Color(0xFF7B1FA2),
                                        modifier       = Modifier.weight(1f).clickable { 
                                            navigator.push(AppNavigation.studentGrades(student.id.value, student.name, "", isTeacher = false)) 
                                        }
                                    )
                                    StatusCard(
                                        icon           = Icons.Default.Edit,
                                        value          = "Trámites",
                                        label          = "Justificaciones",
                                        containerColor = Color(0xFFFFF8E1),
                                        contentColor   = Color(0xFFF57F17),
                                        modifier       = Modifier.weight(1f).clickable { 
                                            navigator.push(AppNavigation.justificationList(student.id.value)) 
                                        }
                                    )
                                }
                            }

                            // ── Última anotación ──────────────────────────────
                            item {
                                Card(
                                    shape  = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Row(
                                        Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary)
                                        Column {
                                            Text("Última anotación",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(student.lastNote,
                                                style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }

                            // ── Timeline ──────────────────────────────────────
                            item {
                                Text(
                                    "Actividad Reciente",
                                    style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(start = 4.dp)
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
        if (state.showAddStudentDialog) {
            AddStudentDialog(
                isLoading = state.isActionLoading,
                error = state.error,
                onDismiss = { model.onDismissDialog() },
                onConfirm = { name, rut -> model.enrollStudent(name, rut) }
            )
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

// ── Student header ────────────────────────────────────────────────────────────

@Composable
private fun StudentHeader(
    name: String, 
    percent: Int, 
    studentId: String, 
    studentRut: String?,
    navigator: cafe.adriel.voyager.navigator.Navigator,
    onViewHistory: () -> Unit
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier         = Modifier.size(56.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = name[0].uppercaseChar().toString(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            LinearProgressIndicator(
                progress       = { percent / 100f },
                modifier       = Modifier.fillMaxWidth().padding(top = 6.dp).height(6.dp).clip(RoundedCornerShape(4.dp)),
                color          = when {
                    percent >= 85 -> Color(0xFF2E7D32)
                    percent >= 70 -> Color(0xFFF57F17)
                    else          -> Color(0xFFC62828)
                },
                trackColor     = MaterialTheme.colorScheme.surfaceContainerHigh
            )
            Text("$percent% de asistencia",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp))
        }
        IconButton(onClick = {
            navigator.push(AppNavigation.parentStudentDetail(studentId, name, studentRut))
        }) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Gestionar alumno",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onViewHistory) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Ver historial",
                tint = MaterialTheme.colorScheme.primary
            )
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
