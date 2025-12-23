package com.example.llamadroid.data.binary

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.llamadroid.util.CpuFeatures
import com.example.llamadroid.util.DebugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Repository for managing native binaries with CPU tier support.
 * 
 * Binaries are stored with tier suffixes: libname_baseline.so, libname_dotprod.so, libname_armv9.so
 * At runtime, the best available tier is selected based on CPU features.
 */
class BinaryRepository(private val context: Context) {

    companion object {
        private const val TAG = "BinaryRepository"
        
        // Required files for llama.cpp server (for custom binary upload screen)
        val REQUIRED_FILES = listOf(
            "llama-server" to "libllama_server.so",
            "libllama.so" to "libllama.so",
            "libggml.so" to "libggml.so",
            "libggml-base.so" to "libggml-base.so",
            "libggml-cpu.so" to "libggml-cpu.so",
            "libmtmd.so" to "libmtmd.so"
        )
        
        // Binary names (without lib prefix and tier suffix)
        private val TIERED_BINARIES = listOf(
            "ffmpeg",
            "ffprobe",
            "whisper-cli",
            "llama-cli",
            "llama_server",
            "mtmd",
            "sd"
        )
        
        // Required shared libraries (not tiered, always same version)
        val SHARED_LIBS = listOf(
            "libllama.so",
            "libllama.so.0.so",
            "libggml.so",
            "libggml.so.0.so",
            "libggml-base.so",
            "libggml-base.so.0.so",
            "libggml-cpu.so",
            "libggml-cpu.so.0.so",
            "libwhisper.so.1.so"
        )
    }
    
    private var cachedTier: String? = null
    
    /**
     * Get the current CPU tier (cached).
     */
    fun getTier(): String {
        if (cachedTier == null) {
            cachedTier = CpuFeatures.getTier()
            Log.i(TAG, "Detected CPU tier: $cachedTier")
        }
        return cachedTier!!
    }

    /**
     * Get path to a tiered binary, with fallback to lower tiers.
     * 
     * @param name Binary name without "lib" prefix (e.g., "ffmpeg", "llama-cli")
     * @return File path to the binary, or null if not found
     */
    fun getTieredBinary(name: String): File? {
        val tier = getTier()
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        
        // Try tiers from best to worst (fallback chain)
        val tiersToTry = when (tier) {
            "armv9" -> listOf("armv9", "dotprod", "baseline")
            "dotprod" -> listOf("dotprod", "baseline")
            else -> listOf("baseline")
        }
        
        for (tryTier in tiersToTry) {
            val libName = "lib${name}_${tryTier}.so"
            val file = File(nativeLibDir, libName)
            
            if (file.exists()) {
                DebugLog.log("$TAG: Found $name at ${file.absolutePath} (tier: $tryTier)")
                return file
            }
        }
        
        // Fallback: try non-tiered version (legacy)
        val legacyName = "lib${name}.so"
        val legacyFile = File(nativeLibDir, legacyName)
        if (legacyFile.exists()) {
            DebugLog.log("$TAG: Found legacy $name at ${legacyFile.absolutePath}")
            return legacyFile
        }
        
        Log.w(TAG, "Binary not found: $name (tried tiers: $tiersToTry)")
        return null
    }
    
    /**
     * Get the llama-server executable (tiered).
     */
    suspend fun getExecutable(): File? = withContext(Dispatchers.IO) {
        // First check for user-uploaded binary
        val customBinDir = File(context.filesDir, "binaries")
        val customServer = File(customBinDir, "libllama_server.so")
        
        if (customServer.exists() && customServer.canExecute()) {
            DebugLog.log("$TAG: Using custom binary: ${customServer.absolutePath}")
            return@withContext customServer
        }
        
        // Use tiered binary
        return@withContext getTieredBinary("llama_server")
    }

    /**
     * Get the library directory path - needed for LD_LIBRARY_PATH
     */
    fun getLibraryDir(): String {
        val customBinDir = File(context.filesDir, "binaries")
        if (customBinDir.exists() && customBinDir.listFiles()?.isNotEmpty() == true) {
            return customBinDir.absolutePath
        }
        return context.applicationInfo.nativeLibraryDir
    }
    
    /**
     * Get ffmpeg binary (tiered).
     */
    fun getFFmpegBinary(): File? = getTieredBinary("ffmpeg")
    
    /**
     * Get ffprobe binary (tiered).
     */
    fun getFFprobeBinary(): File? = getTieredBinary("ffprobe")
    
    /**
     * Get whisper-cli binary (tiered).
     */
    fun getWhisperCliBinary(): File? = getTieredBinary("whisper-cli")
    
    /**
     * Get llama-cli binary (tiered).
     */
    fun getLlamaCliBinary(): File? = getTieredBinary("llama-cli")
    
    /**
     * Get llama-server binary (tiered).
     */
    fun getLlamaServerBinary(): File? = getTieredBinary("llama_server")
    
    /**
     * Get mtmd (multimodal) binary (tiered).
     */
    fun getMtmdBinary(): File? = getTieredBinary("mtmd")
    
    /**
     * Get stable-diffusion binary (tiered).
     */
    fun getSdBinary(): File? = getTieredBinary("sd")
    
    /**
     * Get kiwix-serve binary (for serving ZIM files).
     * Note: kiwix binaries may not be tiered yet - fall back to non-tiered if needed
     */
    fun getKiwixServeBinary(): File? = getTieredBinary("kiwix-serve") 
        ?: File(context.applicationInfo.nativeLibraryDir, "libkiwix-serve.so").takeIf { it.exists() }
    
    /**
     * Get kiwix-manage binary (for managing ZIM libraries).
     */
    fun getKiwixManageBinary(): File? = getTieredBinary("kiwix-manage") 
        ?: File(context.applicationInfo.nativeLibraryDir, "libkiwix-manage.so").takeIf { it.exists() }
    
    /**
     * Gets the llama-embedding executable from the native library directory.
     */
    suspend fun getEmbeddingExecutable(): File? = withContext(Dispatchers.IO) {
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val embeddingFile = File(nativeLibDir, "libllama_embedding.so")
        
        if (embeddingFile.exists()) {
            return@withContext embeddingFile
        }
        return@withContext null
    }
    
    fun getLocalVersion(): String? {
        val customBinDir = File(context.filesDir, "binaries")
        val customServer = File(customBinDir, "libllama_server.so")
        if (customServer.exists()) return "Custom"
        
        val serverFile = getTieredBinary("llama_server")
        if (serverFile == null) return null
        return "Bundled (${getTier()})"
    }
    
    /**
     * Check if custom binaries are installed
     */
    fun hasCustomBinaries(): Boolean {
        val customBinDir = File(context.filesDir, "binaries")
        val customServer = File(customBinDir, "libllama_server.so")
        return customServer.exists()
    }
    
    /**
     * Check availability of all tiered binaries.
     */
    fun checkBinaries(): Map<String, Boolean> {
        return TIERED_BINARIES.associateWith { name ->
            getTieredBinary(name) != null
        }
    }
    
    /**
     * Log all binary paths for debugging.
     */
    fun logBinaryPaths() {
        Log.i(TAG, "CPU Tier: ${getTier()}")
        Log.i(TAG, "Native lib dir: ${context.applicationInfo.nativeLibraryDir}")
        
        TIERED_BINARIES.forEach { name ->
            val file = getTieredBinary(name)
            if (file != null) {
                Log.i(TAG, "  $name: ${file.absolutePath}")
            } else {
                Log.w(TAG, "  $name: NOT FOUND")
            }
        }
    }
    
    /**
     * Install a custom binary file from a Uri
     */
    suspend fun installBinaryFromUri(uri: Uri, targetName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val customBinDir = File(context.filesDir, "binaries")
            customBinDir.mkdirs()
            
            val destFile = File(customBinDir, targetName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Make executable
            destFile.setExecutable(true, false)
            
            DebugLog.log("$TAG: Installed custom binary: $targetName (${destFile.length()} bytes)")
            return@withContext true
        } catch (e: Exception) {
            DebugLog.log("$TAG: Failed to install $targetName: ${e.message}")
            return@withContext false
        }
    }
    
    /**
     * Delete all custom binaries
     */
    suspend fun deleteCustomBinaries() = withContext(Dispatchers.IO) {
        val customBinDir = File(context.filesDir, "binaries")
        if (customBinDir.exists()) {
            customBinDir.deleteRecursively()
            DebugLog.log("$TAG: Deleted custom binaries")
        }
    }
}
