package com.example.game.graphics

import android.opengl.GLES20
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class MaterialId {
    GRASS_TERRAIN,
    DESERT_SAND,
    SNOW_ALPINE,
    ASPHALT_ROAD,
    CONCRETE_WALL,
    CORRUGATED_METAL,
    WOOD_CRATE,
    TREE_BARK,
    PINE_FOLIAGE,
    TACTICAL_FABRIC,
    SKIN_DETAIL,
    VEHICLE_COAT,
    WATER_RIPPLE
}

data class TexturePair(
    val albedoTexId: Int,
    val normalTexId: Int
)

/**
 * Procedurally synthesizes tileable RGBA Albedo (with embedded AO detail) and Tangent-Space Normal
 * OpenGL ES 2.0 2D textures with Mipmaps. Texture resolution scales cleanly with TextureQuality.
 */
class PbrTextureAtlas {
    private val textures = mutableMapOf<MaterialId, TexturePair>()
    private var currentResolution = 0
    private var currentTrilinear = true

    fun ensureInitialized(resolutionPx: Int, trilinearFiltering: Boolean) {
        if (textures.isNotEmpty() && currentResolution == resolutionPx && currentTrilinear == trilinearFiltering) {
            return
        }
        release()
        currentResolution = resolutionPx
        currentTrilinear = trilinearFiltering

        MaterialId.entries.forEach { mat ->
            val (albedoId, normalId) = generateMaterialTextures(mat, resolutionPx, trilinearFiltering)
            textures[mat] = TexturePair(albedoId, normalId)
        }
    }

    fun get(materialId: MaterialId): TexturePair? = textures[materialId]

    fun release() {
        if (textures.isEmpty()) return
        val ids = IntArray(textures.size * 2)
        var i = 0
        textures.values.forEach { pair ->
            ids[i++] = pair.albedoTexId
            ids[i++] = pair.normalTexId
        }
        GLES20.glDeleteTextures(ids.size, ids, 0)
        textures.clear()
    }

    private fun generateMaterialTextures(mat: MaterialId, size: Int, trilinear: Boolean): Pair<Int, Int> {
        val albedoBytes = ByteArray(size * size * 4)
        val heightField = FloatArray(size * size)
        val inv = 1f / size.toFloat()

        for (y in 0 until size) {
            val v = y * inv
            for (x in 0 until size) {
                val u = x * inv
                val idx = y * size + x
                val px = idx * 4

                val h1 = hash2d(x, y, mat.ordinal * 31 + 7)
                val waveU = sin(u * Math.PI * 2.0 * 4.0).toFloat()
                val waveV = cos(v * Math.PI * 2.0 * 4.0).toFloat()
                val macro = sin(u * Math.PI * 2.0 * 2.0 + waveV * 0.7f).toFloat() * 0.5f + 0.5f

                var r = 200f
                var g = 200f
                var b = 200f
                var height = 0.5f

                when (mat) {
                    MaterialId.GRASS_TERRAIN -> {
                        val blade = sin(u * Math.PI * 32.0 + v * 12.0).toFloat() * 0.5f + 0.5f
                        r = 165f + h1 * 48f + macro * 28f
                        g = 198f + blade * 36f + h1 * 24f
                        b = 148f + h1 * 32f
                        height = 0.4f + blade * 0.35f + h1 * 0.25f
                    }
                    MaterialId.DESERT_SAND -> {
                        val dune = sin((u * 6.0 + sin(v * Math.PI * 2.0) * 0.35) * Math.PI * 2.0).toFloat() * 0.5f + 0.5f
                        r = 228f + dune * 22f - h1 * 12f
                        g = 212f + dune * 20f - h1 * 12f
                        b = 185f + h1 * 18f
                        height = 0.35f + dune * 0.50f + h1 * 0.15f
                    }
                    MaterialId.SNOW_ALPINE -> {
                        val drift = sin((u * 4.0 + v * 2.0) * Math.PI * 2.0).toFloat() * 0.5f + 0.5f
                        r = 225f + drift * 26f
                        g = 234f + drift * 18f
                        b = 245f + h1 * 10f
                        height = 0.45f + drift * 0.40f + h1 * 0.15f
                    }
                    MaterialId.ASPHALT_ROAD -> {
                        val speck = (h1 - 0.5f) * 48f
                        r = 175f + speck
                        g = 178f + speck
                        b = 184f + speck
                        height = 0.3f + h1 * 0.7f
                    }
                    MaterialId.CONCRETE_WALL -> {
                        val panelLine = if (y % (size / 4) < 3 || x % (size / 2) < 2) 0.72f else 1.0f
                        val grime = 0.85f + macro * 0.15f
                        r = (212f + h1 * 26f) * panelLine * grime
                        g = (214f + h1 * 24f) * panelLine * grime
                        b = (218f + h1 * 22f) * panelLine * grime
                        height = if (panelLine < 0.9f) 0.15f else 0.55f + h1 * 0.35f
                    }
                    MaterialId.CORRUGATED_METAL -> {
                        val rib = sin(u * Math.PI * 2.0 * 10.0).toFloat() * 0.5f + 0.5f
                        val shade = 175f + rib * 68f + h1 * 12f
                        r = shade
                        g = shade + 3f
                        b = shade + 8f
                        height = 0.25f + rib * 0.72f
                    }
                    MaterialId.WOOD_CRATE -> {
                        val plank = if (y % (size / 4) < 3) 0.55f else 1.0f
                        val grain = sin(u * Math.PI * 2.0 * 2.0 + h1 * 0.4f).toFloat() * 0.5f + 0.5f
                        r = (205f + grain * 36f) * plank
                        g = (190f + grain * 32f) * plank
                        b = (165f + grain * 24f) * plank
                        height = if (plank < 0.7f) 0.1f else 0.55f + grain * 0.35f
                    }
                    MaterialId.TREE_BARK -> {
                        val furrow = sin(u * Math.PI * 2.0 * 8.0 + sin(v * Math.PI * 2.0) * 0.8f).toFloat() * 0.5f + 0.5f
                        r = 165f + furrow * 65f + h1 * 18f
                        g = 152f + furrow * 55f + h1 * 16f
                        b = 140f + furrow * 45f
                        height = 0.2f + furrow * 0.75f
                    }
                    MaterialId.PINE_FOLIAGE -> {
                        val needle = abs(sin((u * 16.0 + v * 10.0) * Math.PI)).toFloat()
                        r = 158f + needle * 48f
                        g = 195f + needle * 52f + h1 * 14f
                        b = 155f + needle * 42f
                        height = 0.3f + needle * 0.6f
                    }
                    MaterialId.TACTICAL_FABRIC -> {
                        // Ripstop carbon kevlar diagonal diamond weave
                        val wx = ((x / 3) + (y / 3)) % 2
                        val weave = if (wx == 0) 222f else 192f
                        val fine = (h1 - 0.5f) * 16f
                        r = weave + fine
                        g = weave + fine
                        b = weave + 6f + fine
                        height = if (wx == 0) 0.72f else 0.42f
                    }
                    MaterialId.SKIN_DETAIL -> {
                        r = 232f + h1 * 14f
                        g = 218f + h1 * 12f
                        b = 212f + h1 * 10f
                        height = 0.5f + h1 * 0.12f
                    }
                    MaterialId.VEHICLE_COAT -> {
                        val seam = if (x % (size / 2) < 2 || y % (size / 2) < 2) 0.76f else 1.0f
                        val flake = 226f + (h1 - 0.5f) * 18f
                        r = flake * seam
                        g = flake * seam
                        b = (flake + 4f) * seam
                        height = if (seam < 0.85f) 0.2f else 0.85f
                    }
                    MaterialId.WATER_RIPPLE -> {
                        val rip1 = sin((u * 4.0 + v * 3.0) * Math.PI * 2.0).toFloat()
                        val rip2 = cos((u * 3.0 - v * 5.0) * Math.PI * 2.0).toFloat()
                        val foam = ((rip1 + rip2) * 0.25f + 0.5f).coerceIn(0f, 1f)
                        r = 180f + foam * 65f
                        g = 218f + foam * 36f
                        b = 242f + foam * 13f
                        height = 0.35f + foam * 0.65f
                    }
                }

                albedoBytes[px] = r.toInt().coerceIn(0, 255).toByte()
                albedoBytes[px + 1] = g.toInt().coerceIn(0, 255).toByte()
                albedoBytes[px + 2] = b.toInt().coerceIn(0, 255).toByte()
                albedoBytes[px + 3] = 255.toByte()
                heightField[idx] = height
            }
        }

        // Convert heightField to Tangent-Space Normal Map RGB
        val normalBytes = ByteArray(size * size * 4)
        val strength = when (mat) {
            MaterialId.CORRUGATED_METAL, MaterialId.TREE_BARK -> 3.4f
            MaterialId.WATER_RIPPLE, MaterialId.DESERT_SAND -> 2.8f
            MaterialId.CONCRETE_WALL, MaterialId.WOOD_CRATE -> 2.4f
            else -> 1.6f
        }
        for (y in 0 until size) {
            val yM = (y - 1 + size) % size
            val yP = (y + 1) % size
            for (x in 0 until size) {
                val xM = (x - 1 + size) % size
                val xP = (x + 1) % size
                val dX = (heightField[y * size + xP] - heightField[y * size + xM]) * strength
                val dY = (heightField[yP * size + x] - heightField[yM * size + x]) * strength
                var nx = -dX
                var ny = -dY
                var nz = 1.0f
                val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
                nx /= len; ny /= len; nz /= len

                val px = (y * size + x) * 4
                normalBytes[px] = ((nx * 0.5f + 0.5f) * 255f).toInt().coerceIn(0, 255).toByte()
                normalBytes[px + 1] = ((ny * 0.5f + 0.5f) * 255f).toInt().coerceIn(0, 255).toByte()
                normalBytes[px + 2] = ((nz * 0.5f + 0.5f) * 255f).toInt().coerceIn(0, 255).toByte()
                normalBytes[px + 3] = 255.toByte()
            }
        }

        val albedoId = uploadRgbaTexture(albedoBytes, size, trilinear)
        val normalId = uploadRgbaTexture(normalBytes, size, trilinear)
        return Pair(albedoId, normalId)
    }

    private fun uploadRgbaTexture(bytes: ByteArray, size: Int, trilinear: Boolean): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        val texId = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            if (trilinear) GLES20.GL_LINEAR_MIPMAP_LINEAR else GLES20.GL_LINEAR_MIPMAP_NEAREST
        )
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        val buf = ByteBuffer.allocateDirect(bytes.size).apply {
            put(bytes)
            position(0)
        }
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
            size, size, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf
        )
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        return texId
    }

    private fun hash2d(x: Int, y: Int, seed: Int): Float {
        var n = x * 374761393 + y * 668265263 + seed * 1442695041
        n = (n xor (n shr 13)) * 1274126177
        return ((n xor (n shr 16)) and 0x7FFFFFFF).toFloat() / 0x7FFFFFFF.toFloat()
    }
}
