package com.example.llamadroid.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.llamadroid.service.LlamaService
import com.example.llamadroid.service.ServerState
import com.example.llamadroid.util.SystemMonitor
import com.example.llamadroid.util.SystemStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.llamadroid.util.DebugLog

class DashboardViewModel(
    private val systemMonitor: SystemMonitor
    // private val llamaService: LlamaService (Using singleton/static for MVP or manual DI)
) : ViewModel() {

    private val _stats = MutableStateFlow(SystemStats(0, 0, 0f, 0f))
    val stats = _stats.asStateFlow()
    
    // In real app, bind to service. For now assume we poll or observe static singleton
    // Bind to service state
    val serverState = LlamaService.state 
    
    init {
        viewModelScope.launch {
            systemMonitor.observeStats().collect {
                _stats.value = it
            }
        }
        startPolling()
    }
    
    private fun startPolling() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            while (true) {
                try {
                    val url = java.net.URL("http://127.0.0.1:8080/health")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 1000
                    connection.readTimeout = 1000
                    connection.requestMethod = "GET"
                    
                    val code = connection.responseCode
                    if (code == 200) {
                        // Silently update state - no logging to avoid spam
                        com.example.llamadroid.service.LlamaService.Companion.updateState(ServerState.Running(8080))
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    // Server unreachable - don't log to avoid spam
                }
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    fun startServer(context: Context, modelPath: String? = null) {
        try {
            DebugLog.log("Dashboard: Starting server...")
            val intent = android.content.Intent(context, LlamaService::class.java).apply {
                action = "START"
                // If modelPath is null, service should handle it (e.g., use default or show error)
                putExtra("MODEL_PATH", modelPath ?: "")
            }
            context.startForegroundService(intent)
            DebugLog.log("Dashboard: Intent sent")
        } catch (e: Exception) {
            DebugLog.log("Dashboard: startServer FAILED: ${e.message}")
        }
    }

    fun stopServer(context: Context) {
        val intent = android.content.Intent(context, LlamaService::class.java).apply {
            action = "STOP"
        }
        context.startService(intent)
    }
}
