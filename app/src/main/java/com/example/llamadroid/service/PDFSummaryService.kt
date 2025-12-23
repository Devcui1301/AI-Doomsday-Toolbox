package com.example.llamadroid.service

import android.content.Context
import android.os.PowerManager
import android.widget.Toast
import com.example.llamadroid.data.binary.BinaryRepository
import com.example.llamadroid.data.db.AppDatabase
import com.example.llamadroid.data.SettingsRepository
import com.example.llamadroid.data.db.NoteEntity
import com.example.llamadroid.data.db.NoteType
import com.example.llamadroid.util.DebugLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * Service to summarize PDF text using llama-cli with chunked processing.
 * Uses its own CoroutineScope to survive composition changes.
 * Shows progress notifications via UnifiedNotificationManager.
 * Acquires WakeLock to prevent screen lock from stopping the process.
 */
object PDFSummaryService {
    
    // Service's own scope - survives screen recomposition
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentJob: Job? = null
    
    private val _isSummarizing = MutableStateFlow(false)
    val isSummarizing: StateFlow<Boolean> = _isSummarizing
    
    private val _currentChunk = MutableStateFlow(0)
    val currentChunk: StateFlow<Int> = _currentChunk
    
    private val _totalChunks = MutableStateFlow(0)
    val totalChunks: StateFlow<Int> = _totalChunks
    
    private val _currentPhase = MutableStateFlow("")
    val currentPhase: StateFlow<String> = _currentPhase
    
    // Result is published via StateFlow
    private val _result = MutableStateFlow<Result<String>?>(null)
    val result: StateFlow<Result<String>?> = _result
    
    private var currentProcess: Process? = null
    private var isCancelled = false
    private var notificationTaskId: Int? = null
    private var currentPdfName: String = ""
    private var wakeLock: PowerManager.WakeLock? = null
    
    // ~4096 tokens ≈ 16,000 characters
    private const val CHUNK_SIZE_CHARS = 16000
    
    const val DEFAULT_SUMMARY_PROMPT = """You are an expert document summarizer. Create a concise summary of this text excerpt.

Instructions:
- Identify the main points and key information
- Extract important facts, findings, or conclusions
- Use bullet points for clarity
- Keep it under 500 words

Summary:"""

    const val DEFAULT_UNIFICATION_PROMPT = """You are an expert at combining multiple summaries into one comprehensive document summary.

Instructions:
1. Merge these chunk summaries into a single coherent summary
2. Start with a clear TITLE that captures the document's main topic
3. Remove redundancy while preserving all unique information
4. Organize by themes with clear sections

Format:
# [Document Title]

## Overview
[Brief intro]

## Key Points
[Main content]

## Conclusions
[Final takeaways]

Chunk summaries to unify:"""

    /**
     * Cancel the current summarization process
     */
    fun cancel() {
        isCancelled = true
        currentProcess?.destroyForcibly()
        currentProcess = null
        currentJob?.cancel()
        currentJob = null
        _isSummarizing.value = false
        _currentPhase.value = ""
        _currentChunk.value = 0
        _totalChunks.value = 0
        
        // Release wake lock
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {}
        wakeLock = null
        
        // Dismiss notification
        notificationTaskId?.let { 
            UnifiedNotificationManager.dismissTask(it) 
        }
        notificationTaskId = null
    }
    
    /**
     * Clear the result after consuming it
     */
    fun clearResult() {
        _result.value = null
    }
    
    /**
     * Start summarization in service's own scope.
     * Result will be published to result StateFlow.
     * Shows progress notifications.
     */
    fun startSummarization(
        context: Context,
        modelPath: String,
        text: String,
        pdfFileName: String = "PDF",
        temperature: Float = 0.3f,
        contextSize: Int = 4096,
        threads: Int = 4,
        maxTokens: Int = 1024
    ) {
        // Cancel any existing job
        currentJob?.cancel()
        _result.value = null
        currentPdfName = pdfFileName
        
        // Acquire wake lock to prevent screen lock from stopping process
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LlamaDroid:PDFSummary")
            wakeLock?.acquire(60 * 60 * 1000L) // Max 1 hour
            DebugLog.log("[PDF-AI] Wake lock acquired")
        } catch (e: Exception) {
            DebugLog.log("[PDF-AI] Failed to acquire wake lock: ${e.message}")
        }
        
        // Start notification
        notificationTaskId = UnifiedNotificationManager.startTask(
            UnifiedNotificationManager.TaskType.PDF_SUMMARY,
            "Summarizing: $pdfFileName"
        )
        
        currentJob = serviceScope.launch {
            val result = summarizeInternal(
                context, modelPath, text,
                temperature, contextSize, threads, maxTokens
            )
            
            // AUTO-SAVE to Notes immediately on success (in service, not UI)
            result.onSuccess { summaryText ->
                try {
                    val db = AppDatabase.getDatabase(context)
                    db.noteDao().insert(
                        NoteEntity(
                            title = "Summary: $pdfFileName",
                            content = summaryText,
                            type = NoteType.PDF_SUMMARY,
                            sourceFile = pdfFileName
                        )
                    )
                    DebugLog.log("[PDF-AI] Summary auto-saved to Notes: $pdfFileName - ${summaryText.length} chars")
                    
                    // Show toast on main thread
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Summary saved to Notes!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    DebugLog.log("[PDF-AI] Failed to auto-save: ${e.message}")
                }
            }
            
            _result.value = result
        }
    }
    
    /**
     * Internal summarization logic
     */
    private suspend fun summarizeInternal(
        context: Context,
        modelPath: String,
        text: String,
        temperature: Float,
        contextSize: Int,
        threads: Int,
        maxTokens: Int
    ): Result<String> {
        _isSummarizing.value = true
        isCancelled = false
        
        try {
            val binaryRepo = BinaryRepository(context)
            val llamaCliBinary = binaryRepo.getLlamaCliBinary()
            
            if (llamaCliBinary == null || !llamaCliBinary.exists()) {
                return Result.failure(Exception("LLM binary not found."))
            }
            val nativeDir = llamaCliBinary.parentFile?.absolutePath ?: context.applicationInfo.nativeLibraryDir
            
            setupSymlinks(context, nativeDir)
            
            val settingsRepo = SettingsRepository(context)
            val summaryPrompt = settingsRepo.pdfSummaryPrompt.first() ?: DEFAULT_SUMMARY_PROMPT
            val unificationPrompt = settingsRepo.pdfUnificationPrompt.first() ?: DEFAULT_UNIFICATION_PROMPT
            
            val chunks = splitIntoChunks(text, CHUNK_SIZE_CHARS)
            _totalChunks.value = chunks.size
            
            DebugLog.log("[PDF-AI] Text: ${text.length} chars, ${chunks.size} chunks")
            
            // Single chunk - just summarize
            if (chunks.size == 1) {
                _currentPhase.value = "Summarizing..."
                _currentChunk.value = 1
                
                notificationTaskId?.let {
                    UnifiedNotificationManager.updateProgress(it, 0.5f, "Processing...")
                }
                
                val result = runLlamaCli(
                    llamaCliBinary, nativeDir, context,
                    modelPath, summaryPrompt, chunks[0],
                    temperature, contextSize, threads, maxTokens
                )
                
                return if (result != null) {
                    notificationTaskId?.let {
                        UnifiedNotificationManager.completeTask(it, "Summary complete!")
                    }
                    Result.success("# Document Summary\n\n$result")
                } else {
                    notificationTaskId?.let {
                        UnifiedNotificationManager.failTask(it, "Failed to generate summary")
                    }
                    Result.failure(Exception("Failed to generate summary"))
                }
            }
            
            // Multi-chunk processing
            val chunkSummaries = mutableListOf<String>()
            _currentPhase.value = "Summarizing chunks..."
            
            for ((index, chunk) in chunks.withIndex()) {
                if (isCancelled) return Result.failure(Exception("Cancelled"))
                
                _currentChunk.value = index + 1
                val progress = (index.toFloat() / chunks.size) * 0.8f // 0-80% for chunks
                
                notificationTaskId?.let {
                    UnifiedNotificationManager.updateProgress(it, progress, "Chunk ${index + 1}/${chunks.size}")
                }
                
                DebugLog.log("[PDF-AI] Chunk ${index + 1}/${chunks.size}")
                
                val summary = runLlamaCli(
                    llamaCliBinary, nativeDir, context,
                    modelPath, summaryPrompt, chunk,
                    temperature, contextSize, threads, maxTokens / 2
                )
                
                if (summary != null && summary.length > 20) {
                    chunkSummaries.add("### Chunk ${index + 1}:\n$summary")
                }
            }
            
            if (chunkSummaries.isEmpty()) {
                notificationTaskId?.let {
                    UnifiedNotificationManager.failTask(it, "Failed to summarize")
                }
                return Result.failure(Exception("Failed to summarize any chunks"))
            }
            
            // Unify
            _currentPhase.value = "Unifying summaries..."
            _currentChunk.value = 0
            
            notificationTaskId?.let {
                UnifiedNotificationManager.updateProgress(it, 0.9f, "Combining summaries...")
            }
            
            val combined = chunkSummaries.joinToString("\n\n")
            DebugLog.log("[PDF-AI] Unifying ${chunkSummaries.size} summaries")
            
            val finalSummary = runLlamaCli(
                llamaCliBinary, nativeDir, context,
                modelPath, unificationPrompt, combined,
                temperature, contextSize, threads, maxTokens
            )
            
            return if (finalSummary != null && finalSummary.length > 50) {
                notificationTaskId?.let {
                    UnifiedNotificationManager.completeTask(it, "Summary complete!")
                }
                Result.success(finalSummary)
            } else {
                notificationTaskId?.let {
                    UnifiedNotificationManager.completeTask(it, "Summary complete!")
                }
                Result.success("# Document Summary\n\n$combined")
            }
            
        } catch (e: CancellationException) {
            DebugLog.log("[PDF-AI] Cancelled")
            notificationTaskId?.let {
                UnifiedNotificationManager.dismissTask(it)
            }
            return Result.failure(Exception("Cancelled"))
        } catch (e: Exception) {
            DebugLog.log("[PDF-AI] Error: ${e.message}")
            notificationTaskId?.let {
                UnifiedNotificationManager.failTask(it, e.message ?: "Error")
            }
            return Result.failure(e)
        } finally {
            _isSummarizing.value = false
            _currentPhase.value = ""
            _currentChunk.value = 0
            _totalChunks.value = 0
            
            // Release wake lock
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                    DebugLog.log("[PDF-AI] Wake lock released")
                }
            } catch (_: Exception) {}
            wakeLock = null
        }
    }
    
    private fun splitIntoChunks(text: String, chunkSize: Int): List<String> {
        if (text.length <= chunkSize) return listOf(text)
        
        val chunks = mutableListOf<String>()
        var remaining = text
        
        while (remaining.isNotEmpty()) {
            if (remaining.length <= chunkSize) {
                chunks.add(remaining)
                break
            }
            
            var breakPoint = chunkSize
            val paragraphBreak = remaining.lastIndexOf("\n\n", chunkSize)
            if (paragraphBreak > chunkSize / 2) {
                breakPoint = paragraphBreak + 2
            } else {
                val sentenceBreak = remaining.lastIndexOf(". ", chunkSize)
                if (sentenceBreak > chunkSize / 2) {
                    breakPoint = sentenceBreak + 2
                }
            }
            
            chunks.add(remaining.substring(0, breakPoint).trim())
            remaining = remaining.substring(breakPoint).trim()
        }
        return chunks
    }
    
    private fun runLlamaCli(
        binary: File,
        nativeDir: String,
        context: Context,
        modelPath: String,
        systemPrompt: String,
        userPrompt: String,
        temperature: Float,
        contextSize: Int,
        threads: Int,
        maxTokens: Int
    ): String? {
        val symlinkDir = File(context.filesDir, "llama_libs")
        val outputFile = File(context.cacheDir, "pdf_llm_output.txt")
        outputFile.delete()  // Clear any previous output
        
        val command = mutableListOf(
            binary.absolutePath,
            "-m", modelPath,
            "-sys", systemPrompt,
            "-p", userPrompt,
            "-n", maxTokens.toString(),
            "--no-display-prompt",
            "-no-cnv",
            "--simple-io",
            "-st",
            "-t", threads.toString(),
            "--temp", temperature.toString(),
            "-c", contextSize.toString(),
            "--log-disable"
        )
        
        // Add KV cache quantization flags if enabled
        val settingsRepo = SettingsRepository(context)
        if (settingsRepo.pdfKvCacheEnabled.value) {
            command.add("--cache-type-k")
            command.add(settingsRepo.pdfKvCacheTypeK.value)
            command.add("--cache-type-v")
            command.add(settingsRepo.pdfKvCacheTypeV.value)
            DebugLog.log("[PDF-AI] KV cache enabled: K=${settingsRepo.pdfKvCacheTypeK.value}, V=${settingsRepo.pdfKvCacheTypeV.value}")
        }
        
        val pb = ProcessBuilder(command)
            .directory(File(nativeDir))
        
        pb.environment()["LD_LIBRARY_PATH"] = "$nativeDir:${symlinkDir.absolutePath}"
        pb.environment()["HOME"] = context.cacheDir.absolutePath
        pb.redirectOutput(outputFile)
        pb.redirectErrorStream(true)
        
        // VERBOSE LOGGING for debugging
        val fullCommand = command.mapIndexed { i, arg ->
            if ((i == 3 || i == 5) && arg.length > 100) "\"${arg.take(100)}...[${arg.length} chars]\"" else "\"$arg\""
        }.joinToString(" ")
        DebugLog.log("[PDF-AI] ===== LLM CALL START =====")
        DebugLog.log("[PDF-AI] FULL COMMAND: $fullCommand")
        DebugLog.log("[PDF-AI] Model: ${modelPath.substringAfterLast("/")}")
        DebugLog.log("[PDF-AI] Settings: threads=$threads, ctx=$contextSize, temp=$temperature, tokens=$maxTokens")
        
        return try {
            val process = pb.start()
            currentProcess = process
            process.outputStream.close()  // No stdin needed
            DebugLog.log("[PDF-AI] Process started with file output...")
            
            // Wait with periodic monitoring for interactive mode spam
            val startTime = System.currentTimeMillis()
            var lastGoodSize = 0L
            var spamDetected = false
            var extractedGoodContent: String? = null
            
            while (process.isAlive) {
                val waitResult = process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
                
                if (waitResult) break
                if (isCancelled) {
                    process.destroyForcibly()
                    break
                }
                
                val elapsed = (System.currentTimeMillis() - startTime) / 1000
                
                // Log progress every 10s
                if (elapsed % 10 == 0L && elapsed > 0) {
                    val currentSize = if (outputFile.exists()) outputFile.length() else 0
                    DebugLog.log("[PDF-AI] LLM: Running ${elapsed}s, output=$currentSize bytes")
                }
                
                // Check for interactive mode spam
                try {
                    if (outputFile.exists()) {
                        val currentSize = outputFile.length()
                        
                        if (currentSize - lastGoodSize > 1_000_000) {
                            // Read LAST 1KB to check for spam
                            val last1KB = java.io.RandomAccessFile(outputFile, "r").use { raf ->
                                val readStart = maxOf(0L, currentSize - 1024)
                                raf.seek(readStart)
                                val buffer = ByteArray(minOf(1024, currentSize.toInt()))
                                raf.read(buffer)
                                String(buffer)
                            }
                            
                            val promptCount = last1KB.windowed(2).count { it == "> " }
                            
                            if (promptCount > 50) {
                                DebugLog.log("[PDF-AI] LLM: Detected interactive mode spam! Stopping...")
                                spamDetected = true
                                
                                // Extract good content BEFORE destroying process
                                extractedGoodContent = java.io.RandomAccessFile(outputFile, "r").use { raf ->
                                    val readSize = minOf(lastGoodSize.toInt(), 100_000)
                                    val buffer = ByteArray(readSize)
                                    raf.read(buffer)
                                    String(buffer)
                                }
                                
                                DebugLog.log("[PDF-AI] LLM: Extracted ${extractedGoodContent.length} chars before spam")
                                outputFile.delete()
                                process.destroyForcibly()
                                break
                            }
                        }
                        
                        if (!spamDetected && currentSize < lastGoodSize + 500_000) {
                            lastGoodSize = currentSize
                        }
                    }
                } catch (e: Exception) {
                    DebugLog.log("[PDF-AI] LLM: Error checking spam: ${e.message}")
                }
                
                // 10 minute timeout
                if (elapsed > 600) {
                    DebugLog.log("[PDF-AI] LLM: Timeout after 10 minutes")
                    process.destroyForcibly()
                    return null
                }
            }
            
            // Wait for process to fully terminate
            if (spamDetected) {
                try { process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS) } catch (_: Exception) {}
            }
            
            val totalTime = (System.currentTimeMillis() - startTime) / 1000
            DebugLog.log("[PDF-AI] LLM: Completed after ${totalTime}s (spam=$spamDetected)")
            
            // Get output
            val output = if (spamDetected && extractedGoodContent != null) {
                DebugLog.log("[PDF-AI] LLM: Using extracted content: ${extractedGoodContent.length} chars")
                extractedGoodContent
            } else {
                val fileContent = if (outputFile.exists()) outputFile.readText() else ""
                outputFile.delete()
                DebugLog.log("[PDF-AI] LLM: Read from file: ${fileContent.length} chars")
                fileContent
            }
            
            if (!isCancelled && output.isNotBlank()) {
                val cleaned = cleanLlamaOutput(output)
                DebugLog.log("[PDF-AI] Cleaned output length: ${cleaned.length} chars")
                DebugLog.log("[PDF-AI] ===== LLM CALL END =====")
                cleaned.ifBlank { null }
            } else {
                DebugLog.log("[PDF-AI] No output (cancelled=$isCancelled, blank=${output.isBlank()})")
                DebugLog.log("[PDF-AI] ===== LLM CALL END (FAILED) =====")
                null
            }
        } catch (e: Exception) {
            DebugLog.log("[PDF-AI] Process error: ${e.message}")
            DebugLog.log("[PDF-AI] ===== LLM CALL END (ERROR) =====")
            outputFile.delete()
            null
        }
    }
    
    private fun setupSymlinks(context: Context, nativeDir: String) {
        val symlinkDir = File(context.filesDir, "llama_libs").apply { mkdirs() }
        val versionedLibs = listOf(
            "libllama.so.0" to "libllama.so.0.so",
            "libggml.so.0" to "libggml.so.0.so", 
            "libggml-base.so.0" to "libggml-base.so.0.so",
            "libggml-cpu.so.0" to "libggml-cpu.so.0.so"
        )
        
        for ((linkName, targetName) in versionedLibs) {
            val symlink = File(symlinkDir, linkName)
            val target = File(nativeDir, targetName)
            if (!symlink.exists() && target.exists()) {
                try {
                    Runtime.getRuntime().exec(arrayOf("ln", "-sf", target.absolutePath, symlink.absolutePath)).waitFor()
                } catch (_: Exception) {}
            }
        }
    }
    private fun cleanLlamaOutput(output: String): String {
        // Strategy 0: If output contains "(truncated)", the response starts right after it
        // This handles the case where long prompts are echoed with "(truncated)" marker
        val truncatedIndex = output.indexOf("(truncated)")
        if (truncatedIndex >= 0) {
            val afterTruncated = output.substring(truncatedIndex + "(truncated)".length).trimStart()
            
            val endMarkers = listOf(
                "common_perf_print:", "llama_perf_", "llama_memory_breakdown",
                "sampling time =", "ms per token", "tokens per second",
                "Exiting...", "\n> \n"
            )
            var responseEnd = afterTruncated.length
            for (marker in endMarkers) {
                val markerIdx = afterTruncated.indexOf(marker)
                if (markerIdx > 0) {
                    responseEnd = minOf(responseEnd, markerIdx)
                }
            }
            
            val result = afterTruncated.substring(0, responseEnd).trim()
            DebugLog.log("[PDF-AI] Extracted after (truncated): ${result.length} chars")
            if (result.isNotBlank()) {
                return result
            }
        }
        
        // Comprehensive list of llama-cli debug patterns to filter out
        val debugPatterns = listOf(
            // Build and init
            "build:", "main:", "print_info:", "llama_", "ggml_",
            "load:", "[INFO]", "[DEBUG]", "[WARNING]",
            "llama_model_loader:", "common_", "system_info:",
            "generate:", "llama_context:", "llama_kv_cache:",
            "load_tensors:", "tokenizer", "...",
            
            // Context and batch
            "n_ctx", "n_batch", "n_ubatch", "n_predict", "n_keep",
            "n_seq", "n_layer", "n_head", "n_embd", "n_ff",
            "n_vocab", "n_expert", "n_rot", "n_swa", "n_gqa",
            "n_merges", "n_group", "flash_attn", "kv_", "cache", "warmup",
            "f_norm", "f_clamp", "f_max", "f_logit", "f_attn",
            "causal attn", "pooling type", "freq_base", "freq_scale",
            "is_swa", "vocab_only", "rope", "general.name",
            
            // Sampler settings
            "sampler", "repeat_last_n", "repeat_penalty", 
            "frequency_penalty", "presence_penalty",
            "dry_multiplier", "dry_base", "dry_allowed", "dry_penalty",
            "top_k", "top_p", "min_p", "typical", "temp",
            "mirostat", "xtc_", "logit", "top_n_sigma",
            
            // Info and formatting
            "IMPORTANT:", "***", "===", "---", "Running in",
            "file format", "file type", "file size",
            "EOG", "EOS", "BOS", "EOT", "LF token", "UNK token", "PAD token",
            "special tokens", "token to piece", "max token",
            "timings:", "total time", "prompt eval", "eval time",
            
            // Model info
            "model type", "model params", "vocab type", "arch",
            "quant", "CPU", "GPU", "VRAM", "GiB", "BPW",
            "graph nodes", "graph splits",
            
            // Misc patterns
            "interactive", "Press Ctrl", "Press Return",
            "chat template", "- kv", "- type",
            
            // Performance/timing stats
            "common_perf_print:", "sampling time", "samplers time",
            "load time", "ms per token", "tokens per second",
            "unaccounted", "graphs reused",
            "llama_memory_breakdown", "memory breakdown",
            "Host", "CPU_REPACK", "MiB",
            
            // Pattern matching for timing lines
            "ms /", "runs (", "tokens (",
            
            // llama-cli update notices  
            "New llama-cli", "enhanced features", "improved user experience",
            "More info:", "https://", "github.com", "discussions",
            "ggml-org", "llama.cpp", "llama-completion"
        )
        
        val lines = output.lines()
        val generatedLines = mutableListOf<String>()
        var foundStart = false
        
        for (line in lines) {
            val trimmed = line.trim()
            
            // Skip blank lines unless we've started collecting
            if (trimmed.isBlank()) {
                if (foundStart && generatedLines.isNotEmpty()) {
                    generatedLines.add("") // Keep paragraph breaks
                }
                continue
            }
            
            // Skip lines that only contain pipe characters and digits (progress bars)
            if (trimmed.matches(Regex("^[|\\s\\d]+$"))) {
                continue
            }
            
            val isDebug = debugPatterns.any { pattern ->
                trimmed.startsWith(pattern, ignoreCase = true) || 
                trimmed.contains(pattern, ignoreCase = true)
            }
            
            // Also filter lines that look like timing stats
            val looksLikeTiming = trimmed.matches(Regex(".*\\d+\\.\\d+\\s*ms.*")) && 
                                  trimmed.contains("=")
            
            if (!isDebug && !looksLikeTiming) {
                foundStart = true
                generatedLines.add(trimmed)
            }
        }
        
        return generatedLines
            .dropWhile { it.isBlank() }
            .dropLastWhile { it.isBlank() }
            .joinToString("\n")
            .trim()
    }
}
