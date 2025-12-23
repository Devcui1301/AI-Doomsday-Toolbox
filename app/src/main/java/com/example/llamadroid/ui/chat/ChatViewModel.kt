package com.example.llamadroid.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.llamadroid.data.api.CompletionRequest
import com.example.llamadroid.data.api.LlamaServerApi
import com.example.llamadroid.data.db.ChatDao
import com.example.llamadroid.data.db.ChatMessageEntity
import com.example.llamadroid.data.db.RagDao
import com.example.llamadroid.data.rag.KnowledgeBaseManager
import com.example.llamadroid.util.DebugLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class ChatMessage(val role: String, val content: String, val imagePath: String? = null)

class ChatViewModel(
    private val ragDao: RagDao,
    private val kbManager: KnowledgeBaseManager,
    private val chatDao: ChatDao
) : ViewModel() {

    // Local server client
    private val client = Retrofit.Builder()
        .baseUrl("http://127.0.0.1:8080/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(LlamaServerApi::class.java)

    // Messages from Room - converted to ChatMessage
    val messages = chatDao.getAllMessages()
        .map { entities -> entities.map { ChatMessage(it.role, it.content, it.imagePath) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val knowledgeBases = ragDao.getAllKbs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    private val _activeKbIds = kotlinx.coroutines.flow.MutableStateFlow<Set<Long>>(emptySet())
    val activeKbIds = _activeKbIds.asStateFlow()
    
    fun toggleKb(id: Long) {
        val current = _activeKbIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _activeKbIds.value = current
    }

    fun sendMessage(text: String, systemPrompt: String = "You are a helpful assistant.") {
        viewModelScope.launch {
            // Save user message to DB
            chatDao.insertMessage(ChatMessageEntity(role = "user", content = text))
            
            try {
                // RAG context
                val contextChunks = if (_activeKbIds.value.isNotEmpty()) {
                    kbManager.performSearch(_activeKbIds.value.toList(), FloatArray(384)) 
                } else {
                    emptyList()
                }
                
                val contextStr = contextChunks.joinToString("\n\n") { it.content }
                val finalPrompt = if (contextStr.isNotEmpty()) {
                    "$systemPrompt\n\nContext:\n$contextStr\n\nUser: $text\nAssistant:"
                } else {
                    "$systemPrompt\n\nUser: $text\nAssistant:"
                }
                
                val response = client.completion(CompletionRequest(prompt = finalPrompt))
                
                // Save assistant response to DB
                chatDao.insertMessage(ChatMessageEntity(role = "assistant", content = response.content))
            } catch (e: Exception) {
                chatDao.insertMessage(ChatMessageEntity(role = "system", content = "Error: ${e.message}"))
                DebugLog.log("Chat Error: ${e.message}")
            }
        }
    }
    
    fun sendImageMessage(imageUri: String, text: String = "") {
        viewModelScope.launch {
            chatDao.insertMessage(ChatMessageEntity(role = "user", content = text, imagePath = imageUri))
            DebugLog.log("Image added: $imageUri")
            // TODO: If server supports vision, send image to LLaVA endpoint
        }
    }
    
    fun clearHistory() {
        viewModelScope.launch {
            chatDao.clearAllMessages()
            DebugLog.log("Chat history cleared")
        }
    }
}

private fun <T> kotlinx.coroutines.flow.MutableStateFlow<T>.asStateFlow() = this as kotlinx.coroutines.flow.StateFlow<T>
