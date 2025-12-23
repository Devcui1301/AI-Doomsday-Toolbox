package com.example.llamadroid.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import com.example.llamadroid.util.DebugLog
import com.example.llamadroid.data.binary.BinaryRepository

class LlamaService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val processController = ProcessController()
    private var notificationTaskId: Int? = null
    override fun onBind(intent: Intent?): IBinder? = null

    // Helper for updating global state
    // private fun updateState(newState: ServerState) {
    //    Companion.updateState(newState)
    // }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            DebugLog.log("LlamaService: onStartCommand action=${intent?.action}")
            when (intent?.action) {
                ACTION_START -> {
                    val modelPath = intent.getStringExtra(EXTRA_MODEL_PATH)
                    val isEmbedding = intent.getBooleanExtra(EXTRA_IS_EMBEDDING, false)
                    val mmprojPath = intent.getStringExtra(EXTRA_MMPROJ_PATH)
                    DebugLog.log("LlamaService: MODEL_PATH=$modelPath")
                    if (mmprojPath != null) {
                        DebugLog.log("LlamaService: MMPROJ_PATH=$mmprojPath")
                    }
                    if (modelPath.isNullOrEmpty()) {
                        DebugLog.log("LlamaService: ERROR - No model path provided!")
                        DebugLog.log("LlamaService: ERROR - No model path provided!")
                        DebugLog.log("LlamaService: ERROR - No model path provided!")
                        Companion.updateState(ServerState.Error("No model selected"))
                        stopSelf()
                        stopSelf()
                    } else {
                        startServer(modelPath, isEmbedding, mmprojPath)
                    }
                }
                ACTION_STOP -> stopServer()
            }
        } catch (e: Exception) {
            DebugLog.log("LlamaService: CRASH in onStartCommand: ${e.message}")
            e.printStackTrace()
        }
        return START_NOT_STICKY
    }
    
    private fun startServer(modelPath: String, isEmbedding: Boolean, mmprojPath: String? = null) {
        val (taskId, notification) = UnifiedNotificationManager.startTaskForForeground(
            UnifiedNotificationManager.TaskType.LLAMA_SERVER,
            "LLM Server"
        )
        notificationTaskId = taskId
        startForeground(taskId, notification)
        
        Companion.updateState(ServerState.Starting)
        DebugLog.log("LlamaService: Starting server for model: $modelPath")
        
        // Read settings from repository
        val settingsRepo = com.example.llamadroid.data.SettingsRepository(applicationContext)
        val threads = settingsRepo.threads.value
        val contextSize = settingsRepo.contextSize.value
        val temperature = settingsRepo.temperature.value
        val remoteAccess = settingsRepo.remoteAccess.value
        val host = if (remoteAccess) "0.0.0.0" else "127.0.0.1"
        val enableVision = settingsRepo.enableVision.value
        val selectedMmprojPath = settingsRepo.selectedMmprojPath.value
        
        // KV Cache settings for server
        val kvCacheEnabled = settingsRepo.serverKvCacheEnabled.value
        val kvCacheTypeK = settingsRepo.serverKvCacheTypeK.value
        val kvCacheTypeV = settingsRepo.serverKvCacheTypeV.value
        val kvCacheReuse = settingsRepo.serverKvCacheReuse.value
        
        // Use mmproj if vision is enabled AND we have a mmproj path (either from intent or settings)
        val effectiveMmprojPath = if (enableVision) {
            mmprojPath ?: selectedMmprojPath
        } else null
        
        DebugLog.log("LlamaService: Settings - threads=$threads, ctx=$contextSize, temp=$temperature, host=$host")
        DebugLog.log("LlamaService: Vision enabled=$enableVision, mmproj=$effectiveMmprojPath")
        if (kvCacheEnabled) {
            DebugLog.log("LlamaService: KV cache enabled - K=$kvCacheTypeK, V=$kvCacheTypeV, reuse=$kvCacheReuse")
        }
        
        serviceScope.launch {
            try {
                // Get binary from BinaryRepository
                val binaryRepo = BinaryRepository(applicationContext)
                val binaryFile = binaryRepo.getExecutable()
                
                if (binaryFile == null || !binaryFile.exists()) {
                    throw Exception("Binary not found. Please ensure binaries are extracted.")
                }
                
                val binary = binaryFile.absolutePath
                val config = LlamaConfig(
                    modelPath = modelPath, 
                    isEmbedding = isEmbedding,
                    threads = threads,
                    contextSize = contextSize,
                    temperature = temperature,
                    host = host,
                    mmprojPath = effectiveMmprojPath,
                    kvCacheEnabled = kvCacheEnabled,
                    kvCacheTypeK = kvCacheTypeK,
                    kvCacheTypeV = kvCacheTypeV,
                    kvCacheReuse = kvCacheReuse
                )
                
                DebugLog.log("LlamaService: Binary found at $binary")
                DebugLog.log("LlamaService: Starting on port ${config.port}")
                
                // Show loading state while model loads
                Companion.updateState(ServerState.Loading(0f, "Loading model..."))
                updateNotification("Loading model...")
                updateNotification("Llama Server Running on port ${config.port}")

                processController.start(binary, config, filesDir)
                
                // If process exits
                DebugLog.log("LlamaService: Process exited")
                if (!processController.stoppedIntentionally) {
                    // Process exited unexpectedly
                    DebugLog.log("LlamaService: Process terminated unexpectedly")
                }
                stopServer()
            } catch (e: Exception) {
                // Only show error if not intentionally stopped
                if (!processController.stoppedIntentionally) {
                    DebugLog.log("LlamaService ERROR: ${e.message}")
                    Companion.updateState(ServerState.Error(e.message ?: "Unknown error"))
                } else {
                    DebugLog.log("LlamaService: Stopped by user")
                    Companion.updateState(ServerState.Stopped)
                }
                stopSelf()
            }
        }
    }
    
    private fun stopServer() {
        processController.stop()
        Companion.updateState(ServerState.Stopped)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun updateNotification(content: String) {
        notificationTaskId?.let {
            UnifiedNotificationManager.updateProgress(it, 1f, content)
        }
    }

    companion object {
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val EXTRA_MODEL_PATH = "MODEL_PATH"
        const val EXTRA_IS_EMBEDDING = "IS_EMBEDDING"
        const val EXTRA_MMPROJ_PATH = "MMPROJ_PATH"
        
        // Global state for simple observation
        private val _state = MutableStateFlow<ServerState>(ServerState.Stopped)
        val state = _state.asStateFlow()
        
        fun updateState(newState: ServerState) {
            _state.value = newState
        }
    }
}
