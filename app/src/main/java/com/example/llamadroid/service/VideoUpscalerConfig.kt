package com.example.llamadroid.service

/**
 * Configuration for Video Upscaler
 */
data class VideoUpscalerConfig(
    val inputPath: String,
    val outputPath: String,
    val engine: UpscalerEngine,
    val model: String,
    val scale: Int,
    val denoise: Int = -1, // -1 = no denoise, 0-3 for RealCUGAN
    val loadThreads: Int = 1,  // Threads for loading frames
    val procThreads: Int = 1,  // Threads for processing (keep low to reduce lag)
    val saveThreads: Int = 1   // Threads for saving frames
)

enum class UpscalerEngine(val binaryName: String) {
    REALSR("librealsr-ncnn.so"),
    REALCUGAN("librealcugan-ncnn.so")
}

/**
 * Model capability definitions for upscaler models
 */
data class UpscalerModelCapability(
    val name: String,
    val displayName: String,
    val engine: UpscalerEngine,
    val scales: List<Int>,
    val supportsDenoise: Boolean,
    val isAnime: Boolean = false,
    val description: String = ""
)

/**
 * All available upscaler models with their capabilities
 */
object UpscalerModels {
    val models = listOf(
        // RealSR models
        UpscalerModelCapability(
            name = "models-ESRGAN-Nomos8kSC",
            displayName = "ESRGAN Nomos8kSC",
            engine = UpscalerEngine.REALSR,
            scales = listOf(4),
            supportsDenoise = false,
            description = "High quality general purpose"
        ),
        UpscalerModelCapability(
            name = "models-Real-ESRGAN",
            displayName = "Real-ESRGAN",
            engine = UpscalerEngine.REALSR,
            scales = listOf(4),
            supportsDenoise = false,
            description = "General purpose"
        ),
        UpscalerModelCapability(
            name = "models-Real-ESRGAN-anime",
            displayName = "Real-ESRGAN Anime",
            engine = UpscalerEngine.REALSR,
            scales = listOf(4),
            supportsDenoise = false,
            isAnime = true,
            description = "Anime images"
        ),
        UpscalerModelCapability(
            name = "models-Real-ESRGAN-animevideov3",
            displayName = "Real-ESRGAN Anime Video v3",
            engine = UpscalerEngine.REALSR,
            scales = listOf(2, 3, 4),
            supportsDenoise = false,
            isAnime = true,
            description = "Anime video optimized"
        ),
        UpscalerModelCapability(
            name = "models-Real-ESRGANv2-anime",
            displayName = "Real-ESRGAN v2 Anime",
            engine = UpscalerEngine.REALSR,
            scales = listOf(2, 4),
            supportsDenoise = false,
            isAnime = true,
            description = "Anime v2"
        ),
        UpscalerModelCapability(
            name = "models-Real-ESRGANv3-anime",
            displayName = "Real-ESRGAN v3 Anime",
            engine = UpscalerEngine.REALSR,
            scales = listOf(2, 3, 4),
            supportsDenoise = false,
            isAnime = true,
            description = "Anime v3 (latest)"
        ),
        UpscalerModelCapability(
            name = "models-Real-ESRGAN-plus",
            displayName = "Real-ESRGAN+",
            engine = UpscalerEngine.REALSR,
            scales = listOf(4),
            supportsDenoise = false,
            description = "Enhanced general"
        ),
        UpscalerModelCapability(
            name = "models-Real-ESRGAN-plus-anime",
            displayName = "Real-ESRGAN+ Anime",
            engine = UpscalerEngine.REALSR,
            scales = listOf(4),
            supportsDenoise = false,
            isAnime = true,
            description = "Enhanced anime"
        ),
        UpscalerModelCapability(
            name = "models-RealeSR-general-v3",
            displayName = "RealeSR General v3",
            engine = UpscalerEngine.REALSR,
            scales = listOf(4),
            supportsDenoise = false,
            description = "General v3"
        ),
        UpscalerModelCapability(
            name = "models-RealeSR-general-v3-wdn",
            displayName = "RealeSR General v3 WDN",
            engine = UpscalerEngine.REALSR,
            scales = listOf(4),
            supportsDenoise = false,
            description = "General v3 with denoising"
        ),
        UpscalerModelCapability(
            name = "models-Real-ESRGAN-SourceBook",
            displayName = "Real-ESRGAN SourceBook",
            engine = UpscalerEngine.REALSR,
            scales = listOf(2),
            supportsDenoise = false,
            description = "Source book/manga"
        ),
        
        // RealCUGAN models
        UpscalerModelCapability(
            name = "models-nose",
            displayName = "RealCUGAN Nose",
            engine = UpscalerEngine.REALCUGAN,
            scales = listOf(2),
            supportsDenoise = false,
            description = "No denoise only"
        ),
        UpscalerModelCapability(
            name = "models-pro",
            displayName = "RealCUGAN Pro",
            engine = UpscalerEngine.REALCUGAN,
            scales = listOf(2, 3),
            supportsDenoise = true,
            description = "Pro quality with denoise"
        ),
        UpscalerModelCapability(
            name = "models-se",
            displayName = "RealCUGAN SE",
            engine = UpscalerEngine.REALCUGAN,
            scales = listOf(2, 3, 4),
            supportsDenoise = true,
            description = "SE quality with denoise"
        )
    )
    
    fun getByName(name: String): UpscalerModelCapability? = models.find { it.name == name }
    
    fun getForEngine(engine: UpscalerEngine): List<UpscalerModelCapability> = 
        models.filter { it.engine == engine }
}
