package com.example.game.graphics

import com.example.data.GameSettingsEntity

/**
 * Unified real-time graphics configuration controlling resolution scaling,
 * shadow fidelity, procedural PBR texture resolution, anti-aliasing/filtering,
 * draw distance LOD, particle pool caps, and target frame pacing.
 */
data class GraphicsConfig(
    val presetName: String = "High",
    val resolutionScale: Float = 0.90f,
    val shadowQuality: Int = 2,      // 0 = Off, 1 = Contact Blob, 2 = Soft Multi-Pass
    val textureQuality: Int = 1,     // 0 = Low (128px), 1 = Medium (256px), 2 = High (512px)
    val antiAliasing: Boolean = true,
    val viewDistance: Int = 285,     // meters in GL world space
    val effectsQuality: Int = 2,     // 0 = Low, 1 = Medium, 2 = Ultra
    val targetFps: Int = 60
) {
    val maxParticles: Int
        get() = when (effectsQuality) {
            0 -> 72
            1 -> 170
            else -> 310
        }

    val grassDensityStep: Int
        get() = when {
            presetName.equals("Low", true) || presetName.equals("Smooth", true) -> 0 // disabled
            effectsQuality == 0 -> 0
            effectsQuality == 1 -> 2
            else -> 1
        }

    val textureResolutionPx: Int
        get() = when (textureQuality.coerceIn(0, 2)) {
            0 -> 128
            1 -> 256
            else -> 512
        }

    companion object {
        fun fromPreset(preset: String): GraphicsConfig = when (preset.lowercase()) {
            "low", "smooth" -> GraphicsConfig(
                presetName = "Low",
                resolutionScale = 0.65f,
                shadowQuality = 0,
                textureQuality = 0,
                antiAliasing = false,
                viewDistance = 165,
                effectsQuality = 0,
                targetFps = 30
            )
            "medium", "balanced" -> GraphicsConfig(
                presetName = "Medium",
                resolutionScale = 0.80f,
                shadowQuality = 1,
                textureQuality = 1,
                antiAliasing = false,
                viewDistance = 225,
                effectsQuality = 1,
                targetFps = 60
            )
            "ultra", "ultra hd" -> GraphicsConfig(
                presetName = "Ultra",
                resolutionScale = 1.00f,
                shadowQuality = 2,
                textureQuality = 2,
                antiAliasing = true,
                viewDistance = 365,
                effectsQuality = 2,
                targetFps = 90
            )
            else -> GraphicsConfig(
                presetName = "High",
                resolutionScale = 0.90f,
                shadowQuality = 2,
                textureQuality = 1,
                antiAliasing = true,
                viewDistance = 285,
                effectsQuality = 2,
                targetFps = 60
            )
        }

        fun fromSettings(settings: GameSettingsEntity?): GraphicsConfig {
            if (settings == null) return fromPreset("High")
            return GraphicsConfig(
                presetName = normalizePresetName(settings.graphicsQuality),
                resolutionScale = settings.resolutionScale.coerceIn(0.60f, 1.00f),
                shadowQuality = settings.shadowQuality.coerceIn(0, 2),
                textureQuality = settings.textureQuality.coerceIn(0, 2),
                antiAliasing = settings.antiAliasing,
                viewDistance = settings.viewDistance.coerceIn(140, 380),
                effectsQuality = settings.effectsQuality.coerceIn(0, 2),
                targetFps = settings.targetFps.coerceIn(30, 120)
            )
        }

        fun normalizePresetName(raw: String): String = when (raw.lowercase()) {
            "low", "smooth" -> "Low"
            "medium", "balanced" -> "Medium"
            "ultra", "ultra hd" -> "Ultra"
            else -> "High"
        }
    }
}
