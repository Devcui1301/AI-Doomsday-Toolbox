package com.example.llamadroid.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    fun copyUriToInternalStorage(context: Context, uri: Uri, destDir: String): File? {
        val contentResolver = context.contentResolver
        val fileName = getFileName(context, uri) ?: "temp_${System.currentTimeMillis()}"
        val dir = File(context.filesDir, destDir).apply { mkdirs() }
        val destFile = File(dir, fileName)
        
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        // Simplified: just taking last path segment or null. 
        // Real app should query OpenableColumns.DISPLAY_NAME
        return uri.path?.split("/")?.last()
    }
}
