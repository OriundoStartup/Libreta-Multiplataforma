package org.orinundo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import com.tuapp.libreta.data.remote.SupabaseAuthService

class MainActivity : ComponentActivity() {

    private val supabase: SupabaseClient by inject()
    private val authService: SupabaseAuthService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        com.tuapp.libreta.data.util.AndroidContextHolder.context = this
        Log.d("DEBUG_AUTH", "onCreate URI: ${intent?.data}")
        handleAuthCallback(intent)
        com.tuapp.libreta.ui.screens.GoogleAuthRegistry.launcher = { launchGoogleAuth() }
        setContent { App() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d("DEBUG_AUTH", "onNewIntent URI: ${intent.data}")
        handleAuthCallback(intent)
    }

    private fun handleAuthCallback(intent: Intent?) {
        val uri = intent?.data ?: return
        Log.d("DEBUG_AUTH", "handleAuthCallback procesando: $uri")
        Log.d("DEBUG_AUTH", "Código recibido: ${uri.getQueryParameter("code") != null}")
        supabase.handleDeeplinks(intent) { error ->
            Log.e("DEBUG_AUTH", "Error intercambiando código: ${error.message}", error)
        }
    }

    fun launchGoogleAuth() {
        CoroutineScope(Dispatchers.Main).launch {
            val url = authService.getGoogleOAuthUrl()
            Log.d("DEBUG_AUTH", "OAuth URL: $url")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.android.chrome")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            // Fallback if Chrome not available
            if (packageManager.resolveActivity(intent, 0) != null) {
                startActivity(intent)
            } else {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
    }
}
