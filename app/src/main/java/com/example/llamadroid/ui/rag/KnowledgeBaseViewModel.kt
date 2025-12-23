package com.example.llamadroid.ui.rag

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.llamadroid.data.db.KnowledgeBaseEntity
import com.example.llamadroid.data.db.ModelType
import com.example.llamadroid.data.db.RagDao
import com.example.llamadroid.data.db.ModelDao
import com.example.llamadroid.data.rag.KnowledgeBaseManager
import com.example.llamadroid.data.rag.PdfExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import com.example.llamadroid.util.DebugLog

class KnowledgeBaseViewModel(
    private val ragDao: RagDao,
    private val modelDao: ModelDao,
    private val kbManager: KnowledgeBaseManager
) : ViewModel() {

    val knowledgeBases = ragDao.getAllKbs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val embeddingModels = modelDao.getModelsByType(ModelType.EMBEDDING)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    fun createKb(name: String) {
        viewModelScope.launch {
            DebugLog.log("KB: Creating knowledge base: $name")
            kbManager.createKnowledgeBase(name)
            DebugLog.log("KB: Knowledge base '$name' created")
        }
    }

    fun addPdf(kbId: Long, file: File) {
        viewModelScope.launch {
            _isProcessing.value = true
            DebugLog.log("KB: Adding PDF to KB#$kbId: ${file.name}")
            try {
                // For MVP, we use naive random embedding if no model is loaded, 
                // or we'd spin up the service.
                // In this dummy impl, I'll use a placeholder embedding function.
                // Real impl would call LlamaService via an HTTP client to /embedding
                kbManager.addDocumentToKb(kbId, file) { text ->
                    FloatArray(384) { 0f } // Mock embedding
                }
                DebugLog.log("KB: PDF processed successfully: ${file.name}")
            } catch (e: Exception) {
                DebugLog.log("KB: FAILED to process PDF: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    // In real App, we need a way to copy Uri to File
}
