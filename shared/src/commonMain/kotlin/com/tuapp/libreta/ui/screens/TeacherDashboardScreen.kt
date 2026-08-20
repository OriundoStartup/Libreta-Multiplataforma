package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.tuapp.libreta.ui.components.AdaptiveGrid
import com.tuapp.libreta.ui.components.AppDrawer
import com.tuapp.libreta.ui.components.AppEntityHeader
import com.tuapp.libreta.ui.components.EmptyStateView
import com.tuapp.libreta.ui.components.FullScreenError
import com.tuapp.libreta.ui.components.FullScreenLoading
import com.tuapp.libreta.ui.components.StatusCard
import com.tuapp.libreta.ui.theme.StatusTheme
import com.tuapp.libreta.ui.util.LocalWindowSize
import com.tuapp.libreta.ui.util.WindowSizeClass
import kotlinx.coroutines.launch

import com.tuapp.libreta.domain.model.UserRole

object TeacherDashboardScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: TeacherDashboardScreenModel = koinScreenModel()
        val state by model.state.collectAsState()
        val generatedCode by model.generatedCode.collectAsState()
        val colleagueCode by model.colleagueCode.collectAsState()
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var showCreateDialog by remember { mutableStateOf(false) }
        var showJoinDialog by remember { mutableStateOf(false) }

        // REFRESCAR AL VOLVER A LA PANTALLA
        LaunchedEffect(Unit) { model.load() }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawer(
                    onClose = { scope.launch { drawerState.close() } },
                    onNavigateToDashboard = { },
                    onNavigateToMessages = { navigator.push(AppNavigation.messages()) },
                    onNavigateToCompose = { navigator.push(AppNavigation.composeNotice()) },
                    onNavigateToProfile = { navigator.push(AppNavigation.profile()) },
                    onLogout = { model.logout() },
                    onSwitchAccount = { 
                        navigator.replaceAll(RoleSelectionScreen(isSwitchingRole = true)) 
                    },
                    userRole = UserRole.TEACHER
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
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                floatingActionButton = {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    navigator.push(AppNavigation.messages())
                                }
                            },
                            icon = { Icon(Icons.Default.Email, null) },
                            text = { Text("Ver Mensajes") },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        ExtendedFloatingActionButton(
                            onClick = { showJoinDialog = true },
                            icon = { Icon(Icons.Default.Person, null) },
                            text = { Text("Colaborar en Curso") },
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        ExtendedFloatingActionButton(
                            onClick = { showCreateDialog = true },
                            icon = { Icon(Icons.Default.Add, null) },
                            text = { Text("Nuevo Curso") }
                        )
                    }
                }
            ) { padding ->
                when (val s = state) {
                    is TeacherDashboardUiState.Loading -> FullScreenLoading(padding)
                    is TeacherDashboardUiState.Error -> FullScreenError(
                        s.message,
                        padding
                    ) { model.load() }

                    is TeacherDashboardUiState.Success -> {
                        val windowSize = LocalWindowSize.current
                        val columns = when (windowSize.widthSizeClass) {
                            WindowSizeClass.COMPACT -> 1
                            else -> 2
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AppEntityHeader(
                                title = s.profile.name,
                                subtitle = "Profesor",
                                initial = s.profile.name.firstOrNull() ?: 'P'
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatusCard(
                                    icon = Icons.Default.Done,
                                    value = if (s.pendingJustificationsCount > 0) s.pendingJustificationsCount.toString() else "Al día",
                                    label = "Trámites",
                                    containerColor = if (s.pendingJustificationsCount > 0)
                                        StatusTheme.warningBackground
                                    else StatusTheme.warningBackground.copy(alpha = 0.5f),
                                    contentColor = StatusTheme.warningContent,
                                    modifier = Modifier.weight(1f).clickable {
                                        scope.launch {
                                            drawerState.close()
                                            navigator.push(AppNavigation.globalJustificationReview())
                                        }
                                    }
                                )

                                StatusCard(
                                    icon = Icons.Default.Email,
                                    value = if (s.unreadMessagesCount > 0) "${s.unreadMessagesCount}" else "Al día",
                                    label = if (s.unreadMessagesCount > 0) "Mensajes Nuevos" else "Mensajes",
                                    containerColor = if (s.unreadMessagesCount > 0)
                                        StatusTheme.infoBackground
                                    else StatusTheme.infoBackground.copy(alpha = 0.5f),
                                    contentColor = StatusTheme.infoContent,
                                    modifier = Modifier.weight(1f).clickable {
                                        scope.launch {
                                            drawerState.close()
                                            navigator.push(AppNavigation.messages())
                                        }
                                    }
                                )

                                if (columns > 1) {
                                    StatusCard(
                                        icon = Icons.Default.Person,
                                        value = "${s.courses.size}",
                                        label = "Mis Cursos",
                                        containerColor = StatusTheme.purpleBackground,
                                        contentColor = StatusTheme.purpleContent,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Text(
                                text = "Mis Cursos (${s.courses.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            if (s.courses.isEmpty()) {
                                EmptyStateView(
                                    icon = Icons.Default.Person,
                                    title = "Aún no tienes cursos",
                                    description = "Toca + para crear tu primer curso"
                                )
                            } else {
                                val grouped =
                                    s.courses.groupBy { it.schoolName ?: "Institución General" }
                                grouped.forEach { (school, schoolCourses) ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(
                                            alpha = 0.5f
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Text(
                                            text = school.uppercase(),
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 4.dp
                                            ),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }

                                    AdaptiveGrid(
                                        items = schoolCourses,
                                        columns = columns
                                    ) { course ->
                                        CourseCard(
                                            course = course,
                                            onClick = {
                                                navigator.push(
                                                    AppNavigation.courseDashboard(
                                                        courseId = course.id,
                                                        courseName = course.name
                                                    )
                                                )
                                            },
                                            onGenerateCode = {
                                                model.generateInviteCodeForCourse(
                                                    course
                                                )
                                            },
                                            onTakeAttendance = {
                                                navigator.push(
                                                    AppNavigation.attendance(
                                                        courseId = course.id,
                                                        courseName = course.name
                                                    )
                                                )
                                            },
                                            onEdit = {
                                                navigator.push(
                                                    AppNavigation.courseEdit(
                                                        courseId = course.id,
                                                        courseName = course.name,
                                                        course = course
                                                    )
                                                )
                                            },
                                            onSendMessage = {
                                                navigator.push(
                                                    AppNavigation.composeNotice(
                                                        classId = course.id,
                                                        className = course.name
                                                    )
                                                )
                                            },
                                            onInviteColleague = {
                                                model.generateColleagueInvite(
                                                    course
                                                )
                                            },
                                            onShowReport = {
                                                navigator.push(
                                                    AppNavigation.attendanceReport(
                                                        courseId = course.id,
                                                        courseName = course.name
                                                    )
                                                )
                                            },
                                            onShowStats = {
                                                navigator.push(
                                                    AppNavigation.courseStats(
                                                        classId = course.id,
                                                        className = course.name
                                                    )
                                                )
                                            },
                                            onReviewJustifications = {
                                                navigator.push(
                                                    AppNavigation.justificationReview(
                                                        classId = course.id
                                                    )
                                                )
                                            },
                                            onMassiveGrades = {
                                                navigator.push(
                                                    AppNavigation.massiveGrades(
                                                        courseId = course.id,
                                                        courseName = course.name
                                                    )
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(80.dp))
                        }
                    }
                }
            }

            if (showCreateDialog) {
                CreateCourseDialog(
                    onDismiss = { showCreateDialog = false },
                    onCreate = { name, grade, school ->
                        model.createCourse(name, grade, school)
                        showCreateDialog = false
                    }
                )
            }

            if (showJoinDialog) {
                JoinCourseDialog(
                    onDismiss = { showJoinDialog = false },
                    onJoin = { code ->
                        model.joinCourse(code)
                        showJoinDialog = false
                    }
                )
            }

            generatedCode?.let { code ->
                InviteCodeDialog(
                    title = "Código para Apoderados",
                    code = code,
                    onDismiss = { model.clearGeneratedCode() }
                )
            }

            colleagueCode?.let { code ->
                InviteCodeDialog(
                    title = "Código para Colega",
                    description = "Comparte este código con otro profesor para que pueda gestionar este curso contigo:",
                    code = code,
                    onDismiss = { model.clearGeneratedCode() }
                )
            }
        }
    }

    @Composable
    private fun CourseCard(
        course: Course,
        onClick: () -> Unit,
        onGenerateCode: () -> Unit,
        onTakeAttendance: () -> Unit,
        onEdit: () -> Unit,
        onSendMessage: () -> Unit,
        onInviteColleague: () -> Unit,
        onShowReport: () -> Unit,
        onShowStats: () -> Unit,
        onReviewJustifications: () -> Unit,
        onMassiveGrades: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = course.name.firstOrNull()?.uppercaseChar()?.toString() ?: "C",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            course.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        LinearProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(6.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )

                        Text(
                            text = "${course.grade ?: "Nivel N/A"} • ${course.schoolName ?: "Institución"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = onTakeAttendance,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.HowToReg, contentDescription = "Pasar Asistencia")
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onSendMessage) {
                            Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onShowReport) {
                            Icon(Icons.AutoMirrored.Filled.List, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onReviewJustifications) {
                            Icon(Icons.Default.Done, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onMassiveGrades) {
                            Icon(Icons.Default.Grade, null, tint = StatusTheme.purpleContent, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onShowStats) {
                            Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onInviteColleague) {
                            Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onGenerateCode) {
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }

                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun JoinCourseDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Colaborar en Curso", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Ingresa el código compartido por tu colega para colaborar gestionando su curso.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        label = { Text("Código de Colaboración") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = { if (code.isNotBlank()) onJoin(code) }, enabled = code.isNotBlank()) { Text("Unirse") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        )
    }

    @Composable
    private fun InviteCodeDialog(title: String = "Código", description: String? = null, code: String, onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(description ?: "Comparte este código con los apoderados:", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(20.dp))
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(text = code, style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 8.sp, fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { com.tuapp.libreta.data.util.ClipboardHelper.copyToClipboard(code) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.Create, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Copiar")
                        }
                        Button(onClick = { com.tuapp.libreta.data.util.ShareHelper.shareText("¡Hola! Únete a mi curso en LibretApp usando el código: $code") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Compartir")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido") } }
        )
    }

    @Composable
    private fun CreateCourseDialog(onDismiss: () -> Unit, onCreate: (name: String, grade: String, school: String) -> Unit) {
        var schoolName by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var grade by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Nuevo Curso", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = schoolName, onValueChange = { schoolName = it }, label = { Text("Nombre del Colegio") }, placeholder = { Text("Ej: Colegio San Patricio") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = grade, onValueChange = { grade = it }, label = { Text("Nivel (ej: 4° Básico)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Letra / Nombre (ej: A)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = { if (schoolName.isNotBlank() && grade.isNotBlank()) onCreate(name, grade, schoolName) }, enabled = schoolName.isNotBlank() && grade.isNotBlank()) { Text("Crear") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
        )
    }
}
