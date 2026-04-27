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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.tuapp.libreta.ui.components.EmptyStateView
import com.tuapp.libreta.ui.components.FullScreenError
import com.tuapp.libreta.ui.components.FullScreenLoading
import com.tuapp.libreta.ui.components.AppDrawer
import com.tuapp.libreta.ui.util.LocalWindowSize
import com.tuapp.libreta.ui.util.WindowSizeClass
import kotlinx.coroutines.launch

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
                    onSwitchAccount = { model.logout() }
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
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExtendedFloatingActionButton(
                        onClick = { navigator.push(AppNavigation.messages()) },
                        icon    = { Icon(Icons.Default.MailOutline, null) },
                        text    = { Text("Ver Mensajes") },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    ExtendedFloatingActionButton(
                        onClick = { showJoinDialog = true },
                        icon    = { Icon(Icons.Default.Groups, null) },
                        text    = { Text("Unirse a Curso") },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    ExtendedFloatingActionButton(
                        onClick = { showCreateDialog = true },
                        icon    = { Icon(Icons.Default.Add, null) },
                        text    = { Text("Nuevo Curso") }
                    )
                }
            }
        ) { padding ->
            when (val s = state) {
                is TeacherDashboardUiState.Loading -> FullScreenLoading(padding)
                is TeacherDashboardUiState.Error -> FullScreenError(s.message, padding) { model.load() }

                is TeacherDashboardUiState.Success -> {
                    val windowSize = LocalWindowSize.current
                    val columns = when (windowSize.widthSizeClass) {
                        WindowSizeClass.COMPACT -> 1
                        WindowSizeClass.MEDIUM -> 2
                        WindowSizeClass.EXPANDED -> 2 // O incluso 3 si el max-width lo permite
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier       = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item(span = { GridItemSpan(columns) }) { ProfileHeader(name = s.profile.name) }

                        item(span = { GridItemSpan(columns) }) {
                            Card(
                                onClick = { navigator.push(AppNavigation.globalJustificationReview()) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AssignmentTurnedIn, null, tint = MaterialTheme.colorScheme.primary)
                                    Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                        Text("Bandeja de Trámites", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        Text("Revisar todas las justificaciones pendientes", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        item(span = { GridItemSpan(columns) }) {
                            Text(
                                text  = "Mis Cursos (${s.courses.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (s.courses.isEmpty()) {
                            item(span = { GridItemSpan(columns) }) {
                                EmptyStateView(
                                    icon = Icons.Default.Groups,
                                    title = "Aún no tienes cursos",
                                    description = "Toca + para crear tu primer curso"
                                )
                            }
                        } else {
                            val grouped = s.courses.groupBy { it.schoolName ?: "Institución General" }
                            grouped.forEach { (school, courses) ->
                                item(span = { GridItemSpan(columns) }) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Text(
                                            text = school.uppercase(),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }

                                items(courses, key = { it.id }) { course ->
                                    CourseCard(
                                        course         = course,
                                        onClick        = {
                                            navigator.push(
                                                StudentListScreen(classId = course.id, className = course.name)
                                            )
                                        },
                                        onGenerateCode = { model.generateInviteCodeForCourse(course) },
                                        onTakeAttendance = {
                                            navigator.push(
                                                AppNavigation.attendance(courseId = course.id, courseName = course.name)
                                            )
                                        },
                                        onEdit = {
                                            navigator.push(
                                                AppNavigation.courseEdit(courseId = course.id, courseName = course.name, course = course)
                                            )
                                        },
                                        onSendMessage = {
                                            navigator.push(
                                                AppNavigation.composeNotice(classId = course.id)
                                            )
                                        },
                                        onInviteColleague = { model.generateColleagueInvite(course) },
                                        onShowReport = {
                                            navigator.push(
                                                AppNavigation.attendanceReport(courseId = course.id, courseName = course.name)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        item(span = { GridItemSpan(columns) }) { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
            }
        }

        if (showCreateDialog) {
            CreateCourseDialog(
                onDismiss = { showCreateDialog = false },
                onCreate  = { name, grade, school ->
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
private fun ProfileHeader(name: String) {
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
                    Text("Profesor",
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
private fun CourseCard(
    course: Course,
    onClick: () -> Unit,
    onGenerateCode: () -> Unit,
    onTakeAttendance: () -> Unit,
    onEdit: () -> Unit,
    onSendMessage: () -> Unit,
    onInviteColleague: () -> Unit,
    onShowReport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
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
                Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(course.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text("Nivel: ${course.grade ?: "N/A"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                course.schoolName?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onSendMessage) {
                    Icon(Icons.Default.EditNote, contentDescription = "Redactar", tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = onTakeAttendance) {
                    Icon(Icons.Default.HowToReg, contentDescription = "Tomar asistencia", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onInviteColleague) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Invitar colega", tint = MaterialTheme.colorScheme.tertiary)
                }
                IconButton(onClick = onGenerateCode) {
                    Icon(Icons.Default.Add, contentDescription = "Ver código", tint = MaterialTheme.colorScheme.tertiary)
                }
                IconButton(onClick = onShowReport) {
                    Icon(Icons.Default.TableChart, contentDescription = "Ver reporte", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar curso", tint = MaterialTheme.colorScheme.outline)
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
        title = { Text("Unirse a Curso", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Ingresa el código compartido por tu colega para colaborar en su curso.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Código de Invitación") },
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
        text  = {
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
                        Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
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
