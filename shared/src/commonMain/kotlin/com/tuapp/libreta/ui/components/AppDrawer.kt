package com.tuapp.libreta.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AppDrawer(
    onClose: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToCompose: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    onSwitchAccount: () -> Unit = {}
) {
    ModalDrawerSheet {
        Spacer(Modifier.height(24.dp))
        Text(
            text     = "LibretApp",
            style    = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Text(
            text     = "Panel del Profesor",
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        
        SectionHeader("Gestión")
        NavigationDrawerItem(
            icon    = { Icon(Icons.Default.Dashboard, null) },
            label   = { Text("Inicio / Mis Cursos") },
            selected = false,
            onClick  = { onNavigateToDashboard(); onClose() }
        )
        NavigationDrawerItem(
            icon    = { Icon(Icons.Default.AccountCircle, null) },
            label   = { Text("Mi Perfil") },
            selected = false,
            onClick  = { onNavigateToProfile(); onClose() }
        )

        SectionHeader("Comunicación")
        NavigationDrawerItem(
            icon    = { Icon(Icons.Default.MailOutline, null) },
            label   = { Text("Bandeja de Entrada") },
            selected = false,
            onClick  = { onNavigateToMessages(); onClose() }
        )
        NavigationDrawerItem(
            icon    = { Icon(Icons.Default.EditNote, null) },
            label   = { Text("Redactar Mensaje") },
            selected = false,
            onClick  = { onNavigateToCompose(); onClose() }
        )

        Spacer(Modifier.weight(1f))
        HorizontalDivider()
        NavigationDrawerItem(
            icon    = { Icon(Icons.Default.ManageAccounts, null) },
            label   = { Text("Cambiar de Perfil (Rol)") },
            selected = false,
            onClick  = { onSwitchAccount(); onClose() }
        )
        NavigationDrawerItem(
            icon    = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error) },
            label   = { Text("Cerrar Sesión / Salir", color = MaterialTheme.colorScheme.error) },
            selected = false,
            onClick  = { onLogout(); onClose() },
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}
