package com.example.llamadroid

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.llamadroid.ui.theme.LlamaDroidTheme
import com.example.llamadroid.ui.LlamaApp

/**
 * Shared file data from intent
 */
data class SharedFileData(
    val uri: Uri,
    val mimeType: String
)

class MainActivity : ComponentActivity() {
    
    // Share intent data
    private val sharedFileData = mutableStateOf<SharedFileData?>(null)
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result - notifications will work if granted
    }
    
    /**
     * Override to apply locale setting to the Activity context
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LlamaApplication.updateLocale(newBase))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Handle share intent
        handleShareIntent(intent)
        
        setContent {
            LlamaDroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LlamaApp(
                        sharedFileData = sharedFileData.value,
                        onSharedFileHandled = { sharedFileData.value = null }
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }
    
    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            val mimeType = intent.type ?: ""
            
            if (uri != null && mimeType.isNotEmpty()) {
                sharedFileData.value = SharedFileData(uri, mimeType)
            }
        }
    }
}
