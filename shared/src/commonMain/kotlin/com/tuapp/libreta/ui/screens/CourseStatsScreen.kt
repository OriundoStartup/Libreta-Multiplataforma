package com.tuapp.libreta.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tuapp.libreta.domain.usecase.CourseAnalytics
import com.tuapp.libreta.domain.usecase.DailyAttendance
import com.tuapp.libreta.domain.usecase.StudentAbsences
import com.tuapp.libreta.presentation.StatsScreenModel
import com.tuapp.libreta.presentation.StatsUiState
import com.tuapp.libreta.ui.components.ShimmerCard

private val ColorPresent    = Color(0xFF2E7D32)
private val ColorAbsent     = Color(0xFFC62828)
private val ColorJustified  = Color(0xFFF57F17)
private val ColorBarFill    = Color(0xFF1565C0)

data class CourseStatsScreen(
    val classId: String,
    val className: String = "Mi Clase"
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: StatsScreenModel = koinScreenModel()
        val uiState by model.uiState.collectAsState()

        LaunchedEffect(classId) { model.load(classId) }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = { IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }},
                    title  = { Text("Estadísticas · $className",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ) { padding ->
            when (val s = uiState) {
                StatsUiState.Loading -> LazyColumn(
                    contentPadding = PaddingValues(16.dp).plus(padding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    userScrollEnabled = false
                ) { items(5) { ShimmerCard() } }

                is StatsUiState.Error -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                }

                is StatsUiState.Success -> StatsContent(s.data, padding)
            }
        }
    }
}

// ── Main content ──────────────────────────────────────────────────────────────

@Composable
private fun StatsContent(data: CourseAnalytics, padding: PaddingValues) {
    // Animate charts on entry
    val animProgress by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label         = "chartAnim"
    )

    LazyColumn(
        contentPadding      = PaddingValues(16.dp).plus(padding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Overall % ─────────────────────────────────────────────────────────
        item { OverallCard(data.overallAttendancePercent, animProgress) }

        // ── Pie chart ─────────────────────────────────────────────────────────
        item { SectionTitle("Distribución de Hoy") }
        item {
            PieChartCard(
                present    = data.presentCount,
                absent     = data.absentCount,
                justified  = data.justifiedCount,
                animProgress = animProgress
            )
        }

        // ── Bar chart ─────────────────────────────────────────────────────────
        item { SectionTitle("Asistencia — Últimos 5 Días") }
        item { BarChartCard(data.last5Days, animProgress) }

        // ── At risk ───────────────────────────────────────────────────────────
        if (data.atRisk.isNotEmpty()) {
            item { SectionTitle("⚠️ Alumnos en Riesgo (< 75%)") }
            items(data.atRisk) { AtRiskCard(it) }
        }

        // ── Top absentees ─────────────────────────────────────────────────────
        if (data.topAbsentees.any { it.absenceCount > 0 }) {
            item { SectionTitle("Top Inasistencias") }
            items(data.topAbsentees.filter { it.absenceCount > 0 }) { AbsenteeRow(it) }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ── Overall card ──────────────────────────────────────────────────────────────

@Composable
private fun OverallCard(percent: Float, anim: Float) {
    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Donut indicator
            Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(Color.White.copy(alpha = 0.2f), 0f, 360f, false, style = stroke)
                    drawArc(Color.White, -90f, 360f * percent * anim, false, style = stroke)
                }
                Text(
                    text  = "${(percent * 100 * anim).toInt()}%",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
            Column {
                Text("Asistencia General",
                    style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                Text("${(percent * 100).toInt()}% del curso",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White)
            }
        }
    }
}

// ── Pie chart ─────────────────────────────────────────────────────────────────

@Composable
private fun PieChartCard(present: Int, absent: Int, justified: Int, animProgress: Float) {
    val total = (present + absent + justified).coerceAtLeast(1).toFloat()
    val slices = listOf(
        present   / total to ColorPresent,
        absent    / total to ColorAbsent,
        justified / total to ColorJustified
    )

    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Canvas(Modifier.size(120.dp)) {
                var startAngle = -90f
                slices.forEach { (fraction, color) ->
                    val sweep = fraction * 360f * animProgress
                    drawArc(color, startAngle, sweep, useCenter = true,
                        size = Size(size.width, size.height))
                    startAngle += sweep
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendItem(ColorPresent,   "Presentes",    present)
                LegendItem(ColorAbsent,    "Ausentes",     absent)
                LegendItem(ColorJustified, "Justificados", justified)
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Text("$label: $count", style = MaterialTheme.typography.bodySmall)
    }
}

// ── Bar chart ─────────────────────────────────────────────────────────────────

@Composable
private fun BarChartCard(days: List<DailyAttendance>, animProgress: Float) {
    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(20.dp).height(120.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            days.forEach { day ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    val barHeight = (80 * day.percent * animProgress).dp
                    Box(
                        Modifier
                            .fillMaxWidth(0.5f)
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(ColorBarFill)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(day.label, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(day.percent * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

// ── At risk card ──────────────────────────────────────────────────────────────

@Composable
private fun AtRiskCard(sa: StudentAbsences) {
    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null,
                tint = ColorAbsent, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text("${sa.student.fullName}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Text("${sa.absenceCount} inasistencias registradas",
                    style = MaterialTheme.typography.bodySmall, color = ColorAbsent)
            }
        }
    }
}

// ── Absentee row ──────────────────────────────────────────────────────────────

@Composable
private fun AbsenteeRow(sa: StudentAbsences) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(sa.student.fullName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onErrorContainer)
        }
        Text("${sa.student.fullName}",
            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFFEBEE)) {
            Text("${sa.absenceCount} faltas",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = ColorAbsent,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(start = 4.dp, top = 4.dp))
}

private fun PaddingValues.plus(other: PaddingValues): PaddingValues = PaddingValues(
    start  = calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr) + other.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
    end    = calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)   + other.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
    top    = calculateTopPadding()    + other.calculateTopPadding(),
    bottom = calculateBottomPadding() + other.calculateBottomPadding()
)
