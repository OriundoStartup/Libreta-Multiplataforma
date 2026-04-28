package com.tuapp.libreta.navigation

import cafe.adriel.voyager.core.screen.Screen
import com.tuapp.libreta.ui.screens.LoginScreen
import com.tuapp.libreta.ui.screens.ProfileScreen
import com.tuapp.libreta.ui.screens.TeacherDashboardScreen
import com.tuapp.libreta.ui.screens.ParentDashboardScreen
import com.tuapp.libreta.ui.screens.MessageListScreen
import com.tuapp.libreta.ui.screens.StudentListScreen

object WebPathMapper {
    fun fromPath(path: String): Screen {
        // Limpiamos la ruta de posibles prefijos de hash o slashes duplicados
        val cleanPath = path.removePrefix("#").removePrefix("/")
        val segments = cleanPath.split("/").filter { it.isNotEmpty() }
        
        return when {
            segments.isEmpty() -> LoginScreen
            segments[0] == "login" -> LoginScreen
            segments[0] == "profile" -> ProfileScreen
            segments[0] == "teacher" -> TeacherDashboardScreen
            segments[0] == "parent" -> ParentDashboardScreen(parentId = segments.getOrNull(1) ?: "user-current")
            segments[0] == "messages" -> MessageListScreen
            segments[0] == "students" && segments.size > 1 -> StudentListScreen(classId = segments[1])
            else -> LoginScreen
        }
    }

    fun toPath(screen: Screen): String {
        return when (screen) {
            is ProfileScreen -> "/profile"
            is TeacherDashboardScreen -> "/teacher"
            is ParentDashboardScreen -> "/parent/${screen.parentId}"
            is MessageListScreen -> "/messages"
            is StudentListScreen -> "/students/${screen.classId}"
            is LoginScreen -> "/login"
            else -> "/"
        }
    }
}
