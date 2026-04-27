package com.tuapp.libreta.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AssignmentLate
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tuapp.libreta.navigation.AppNavigation
import com.tuapp.libreta.presentation.StudentListEvent
import com.tuapp.libreta.presentation.StudentListScreenModel
import com.tuapp.libreta.presentation.StudentListUiState
import com.tuapp.libreta.ui.components.EmptyStateView
import com.tuapp.libreta.ui.components.ShimmerCard
import com.tuapp.libreta.ui.components.StudentCard
import com.tuapp.libreta.ui.components.AppDrawer
import kotlinx.coroutines.launch

data class StudentListScreen(
    val classId: String,
    val className: String = "Mi Clase"
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val model: StudentListScreenModel = koinScreenModel()
        val uiState by model.uiState.collectAsState()
        val listState = rememberLazyListState()
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val navigator = LocalNavigator.currentOrThrow
        var searchQuery by remember { mutableStateOf("") }

        // FAB se encoge al hacer scroll
        val fabExpanded by remember { derivedStateOf { !listState.canScrollBackward } }

        ModalNavigationDrawer(
            drawerState   = drawerState,
            drawerContent = { 
                AppDrawer(
                    onClose = { scope.launch { drawerState.close() } },
                    onNavigateToDashboard = { navigator.popUntilRoot() },
                    onNavigateToMessages = { navigator.push(AppNavigation.messages()) },
                    onNavigateToCompose = { navigator.push(AppNavigation.composeNotice(classId = classId)) },
                    onNavigateToProfile = { navigator.push(AppNavigation.profile()) },
                    onLogout = { model.logout() }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menú")
                            }
                        },
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text  = className,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text  = "Lista de Alumnos",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { navigator.push(AppNavigation.profile()) }) {
                                Icon(Icons.Default.AccountCircle, contentDescription = "Perfil",
                                    modifier = Modifier.size(28.dp))
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                floatingActionButton = {
                    AnimatedVisibility(
                        visible = uiState !is StudentListUiState.Loading,
                        enter   = fadeIn() + scaleIn(),
                        exit    = fadeOut() + scaleOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.End) {
                            ExtendedFloatingActionButton(
                                onClick        = { navigator.push(AppNavigation.composeNotice(classId = classId)) },
                                expanded       = fabExpanded,
                                icon           = { Icon(Icons.Default.EditNote, contentDescription = null) },
                                text           = { Text("Redactar") },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor   = MaterialTheme.colorScheme.onPrimary,
                                elevation      = FloatingActionButtonDefaults.elevation(8.dp)
                            )
                            ExtendedFloatingActionButton(
                                onClick        = { navigator.push(AppNavigation.attendance(classId, className)) },
                                expanded       = fabExpanded,
                                icon           = { Icon(Icons.Default.HowToReg, contentDescription = null) },
                                text           = { Text("Pasar Asistencia") },
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    when (val state = uiState) {
                        StudentListUiState.Loading -> ShimmerList()
                        StudentListUiState.Empty   -> EmptyStateView(
                            icon = Icons.Default.Groups,
                            title = "Sin alumnos registrados",
                            description = "Agrega alumnos para comenzar a pasar asistencia"
                        )

                        is StudentListUiState.Success -> {
                            val filteredStudents = state.filteredStudents

                            LazyColumn(
                                state               = listState,
                                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = {
                                            searchQuery = it
                                            model.onEvent(StudentListEvent.Search(it))
                                        },
                                        placeholder = { Text("Buscar alumno...") },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                item {
                                    Text(
                                        text     = if (searchQuery.isEmpty()) "Todos los alumnos (${state.students.size})" 
                                                   else "Resultados (${filteredStudents.size})",
                                        style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color    = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                    )
                                }
                                if (filteredStudents.isEmpty() && searchQuery.isNotEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "No se encontraron alumnos",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                items(filteredStudents, key = { it.id.value }) { student ->
                                    StudentCard(
                                        student        = student,
                                        onMarkPresent  = { model.onEvent(StudentListEvent.ToggleAttendance(it)) },
                                        onMarkAbsent   = { model.onEvent(StudentListEvent.ToggleAttendance(it)) },
                                        onClick        = { navigator.push(AppNavigation.studentDetail(student.id.value, student.fullName, student.courseId.value, student.parentId.value)) }
                                    )
                                }
                                item { Spacer(Modifier.height(88.dp)) }
                            }
                        }

                        is StudentListUiState.Error -> {
                            Snackbar(
                                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                            ) { Text(state.message) }
                        }
                    }
                }
            }
        }

        // Carga inicial
        LaunchedEffect(classId) { model.onEvent(StudentListEvent.LoadClass(classId)) }
    }
}

// ── Shimmer list ──────────────────────────────────────────────────────────────

@Composable
private fun ShimmerList() {
    LazyColumn(
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled   = false
    ) {
        items(7) { ShimmerCard() }
    }
}

