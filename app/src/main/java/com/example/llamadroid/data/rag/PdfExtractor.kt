package com.example.llamadroid.data.rag

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class PdfExtractor(private val context: Context) {
    
    init {
        PDFBoxResourceLoader.init(context)
    }

    suspend fun extractText(pdfFile: File): String = withContext(Dispatchers.IO) {
         try {
             PDDocument.load(pdfFile).use { document ->
                 val stripper = PDFTextStripper()
                 stripper.getText(document)
             }
         } catch (e: Exception) {
             "Error extracting text: ${e.message}"
         }
    }
    
    suspend fun extractText(inputStream: InputStream): String = withContext(Dispatchers.IO) {
        try {
            PDDocument.load(inputStream).use { document ->
                val stripper = PDFTextStripper()
                stripper.getText(document)
            }
        } catch (e: Exception) {
             "Error extracting text: ${e.message}"
        }
    }
}
