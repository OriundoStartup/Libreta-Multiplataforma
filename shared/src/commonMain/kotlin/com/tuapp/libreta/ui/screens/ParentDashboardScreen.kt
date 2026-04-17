package com.tuapp.libreta.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.tuapp.libreta.ui.components.ShimmerCard
import com.tuapp.libreta.ui.components.StatusCard
import com.tuapp.libreta.ui.components.TimelineItem

data class ParentDashboardScreen(val parentId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val model: ParentDashboardScreenModel = koinScreenModel()
        val uiState by model.uiState.collectAsState()
        val listState = rememberLazyListState()
        val fabExpanded by remember { derivedStateOf { !listState.canScrollBackward } }
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(parentId) { model.load(parentId) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Panel Apoderado", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notificaciones")
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Perfil", modifier = Modifier.size(28.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            floatingActionButton = {
                if (uiState is ParentDashboardUiState.Success) {
                    ExtendedFloatingActionButton(
                        onClick        = { navigator.push(AppNavigation.messages()) },
                        expanded       = fabExpanded,
                        icon           = { Icon(Icons.Default.Send, contentDescription = null) },
                        text           = { Text("Enviar Comunicación") },
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ) { padding ->
            when (val state = uiState) {
                ParentDashboardUiState.Loading  -> ParentShimmer(padding)
                is ParentDashboardUiState.Error -> ErrorState(state.message, padding)
                is ParentDashboardUiState.Success -> {
                    val student = state.students[state.selectedIndex]

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
                        if (state.students.size > 1) {
                            item {
                                ScrollableTabRow(
                                    selectedTabIndex = state.selectedIndex,
                                    edgePadding      = 0.dp,
                                    containerColor   = Color.Transparent,
                                    divider          = {}
                                ) {
                                    state.students.forEachIndexed { index, s ->
                                        Tab(
                                            selected = index == state.selectedIndex,
                                            onClick  = { model.selectStudent(index) },
                                            text     = { Text(s.name.split(" ").first()) }
                                        )
                                    }
                                }
                            }
                        }

                        // ── Header del estudiante ─────────────────────────
                        item {
                            StudentHeader(name = student.name, percent = student.attendancePercent)
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
                                    icon           = Icons.Default.MailOutline,
                                    value          = "${student.pendingMessages}",
                                    label          = "Mensajes",
                                    containerColor = Color(0xFFE3F2FD),
                                    contentColor   = Color(0xFF1565C0),
                                    modifier       = Modifier.weight(1f)
                                )
                                StatusCard(
                                    icon           = Icons.Default.EditNote,
                                    value          = "1",
                                    label          = "Anotaciones",
                                    containerColor = Color(0xFFFFF8E1),
                                    contentColor   = Color(0xFFF57F17),
                                    modifier       = Modifier.weight(1f)
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

                        itemsIndexed(state.timeline, key = { _, e -> e.id }) { index, event ->
                            TimelineItem(
                                event    = event,
                                isLast   = index == state.timeline.lastIndex,
                                onJustify = { navigator.push(AppNavigation.justificationForm()) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Student header ────────────────────────────────────────────────────────────

@Composable
private fun StudentHeader(name: String, percent: Int) {
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
                text  = name.first().uppercaseChar().toString(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Column {
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

// ── Error state ───────────────────────────────────────────────────────────────

@Composable
private fun ErrorState(message: String, padding: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.error)
    }
}
