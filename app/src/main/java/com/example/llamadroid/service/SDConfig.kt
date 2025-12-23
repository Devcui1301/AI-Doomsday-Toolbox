package com.example.llamadroid.service

/**
 * Configuration for stable-diffusion.cpp image generation
 */
data class SDConfig(
    val modelPath: String,
    val prompt: String,
    val negativePrompt: String = "",
    val width: Int = 512,
    val height: Int = 512,
    val steps: Int = 20,
    val cfgScale: Float = 7.0f,
    val seed: Long = -1, // -1 for random
    val samplingMethod: SamplingMethod = SamplingMethod.EULER_A,
    val outputPath: String,
    // img2img specific
    val initImage: String? = null,
    val strength: Float = 0.75f,
    // Upscale specific
    val upscaleModel: String? = null,
    val upscaleRepeats: Int = 1,
    // Mode
    val mode: SDMode = SDMode.TXT2IMG,
    // Performance
    val threads: Int = -1, // -1 for auto
    val vaeTiling: Boolean = false, // For low memory
    val vaeTileOverlap: Float = 0.5f,
    val vaeTileSize: String = "32x32",
    val vaeRelativeTileSize: String = "",
    val tensorTypeRules: String = "",
    // FLUX-specific components (required when isFluxModel = true)
    val isFluxModel: Boolean = false,
    val vaePath: String? = null,      // --vae
    val clipLPath: String? = null,    // --clip_l
    val t5xxlPath: String? = null,    // --t5xxl
    // ControlNet (optional)
    val controlNetPath: String? = null,
    val controlImagePath: String? = null,
    val controlStrength: Float = 0.9f,
    // LoRA (optional)
    val loraPath: String? = null,
    val loraStrength: Float = 1.0f,
    // Quantization type for stable-diffusion.cpp (--type)
    val quantizationType: String = ""
)

enum class SamplingMethod(val cliName: String) {
    EULER("euler"),
    EULER_A("euler_a"),
    HEUN("heun"),
    DPM2("dpm2"),
    DPM_PP_2S_A("dpm++2s_a"),
    DPM_PP_2M("dpm++2m"),
    DPM_PP_2M_V2("dpm++2mv2"),
    LCM("lcm"),
    DDIM_TRAILING("ddim_trailing")
}
