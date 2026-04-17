package com.tuapp.libreta.navigation

import cafe.adriel.voyager.core.screen.Screen
import com.tuapp.libreta.ui.screens.*

// ── Rol activo — cambiar aquí para alternar entre vistas ─────────────────────
// En el futuro esto vendrá de Supabase Auth
enum class AppRole { TEACHER, PARENT }

object AppConfig {
    val activeRole: AppRole = AppRole.TEACHER   // ← único punto de cambio

    // Demo IDs — se reemplazarán con Auth session
    const val CURRENT_USER_ID = "user-current"
    const val DEMO_CLASS_ID   = "clase-demo"
    const val DEMO_CLASS_NAME = "4° Básico A"
    const val DEMO_STUDENT_ID = "student-demo"
    const val DEMO_PARENT_ID  = "parent-demo"
    const val DEMO_TEACHER_ID = "teacher-demo"
}

// ── Screen factories — toda la navegación pasa por aquí ──────────────────────

object AppNavigation {

    fun initialScreen(): Screen = when (AppConfig.activeRole) {
        AppRole.TEACHER -> teacherDashboard()
        AppRole.PARENT  -> parentDashboard()
    }

    fun teacherDashboard(): Screen =
        StudentListScreen(classId = AppConfig.DEMO_CLASS_ID, className = AppConfig.DEMO_CLASS_NAME)

    fun parentDashboard(): Screen = ParentDashboardScreen(parentId = AppConfig.DEMO_PARENT_ID)

    fun messages(): Screen = MessageListScreen

    fun messageDetail(contactId: String, contactName: String): Screen =
        MessageDetailScreen(contactId = contactId, contactName = contactName)

    fun justificationForm(): Screen =
        JustificationScreen(
            studentId = AppConfig.DEMO_STUDENT_ID,
            parentId  = AppConfig.CURRENT_USER_ID,
            teacherId = AppConfig.DEMO_TEACHER_ID
        )

    fun justificationReview(studentId: String = AppConfig.DEMO_STUDENT_ID): Screen =
        JustificationReviewScreen(studentId = studentId, parentId = AppConfig.DEMO_PARENT_ID)

    fun courseStats(): Screen =
        CourseStatsScreen(classId = AppConfig.DEMO_CLASS_ID, className = AppConfig.DEMO_CLASS_NAME)
}
