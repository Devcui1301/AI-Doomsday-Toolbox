package com.example.llamadroid.data

import android.content.Context
// import com.example.llamadroid.data.binary.BinaryRepository
// import com.example.llamadroid.data.model.ModelRepository
// import com.example.llamadroid.data.rag.KnowledgeBaseManager

interface AppContainer {
    // val binaryRepository: BinaryRepository
    // val modelRepository: ModelRepository
    // val knowledgeBaseManager: KnowledgeBaseManager
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    // Initialize repositories here
}
