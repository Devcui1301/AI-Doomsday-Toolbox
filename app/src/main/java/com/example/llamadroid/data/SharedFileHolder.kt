package com.example.llamadroid.data

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global holder for shared file URIs from Intent.ACTION_SEND.
 * Screens can check this and consume the pending file.
 */
object SharedFileHolder {
    
    data class PendingFile(
        val uri: Uri,
        val mimeType: String,
        val targetScreen: String? = null  // Specific tool if user chose from dialog
    )
    
    private val _pendingFile = MutableStateFlow<PendingFile?>(null)
    val pendingFile = _pendingFile.asStateFlow()
    
    fun setPendingFile(uri: Uri, mimeType: String, targetScreen: String? = null) {
        _pendingFile.value = PendingFile(uri, mimeType, targetScreen)
    }
    
    fun consumePendingFile(): PendingFile? {
        val file = _pendingFile.value
        _pendingFile.value = null
        return file
    }
    
    fun clear() {
        _pendingFile.value = null
    }
}
