package com.example.game.graphics

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
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
 * OpenGL ES 2.0 2D textures with Mipmaps.
 *
 * Uses a shared pixel cache + fast 32px bootstrap on Frame 1 followed by progressive 2-material/frame
 * upgrades so onSurfaceCreated + Frame 1 always finishes well within Android's 1000ms SurfaceSyncGroup window.
 */
class PbrTextureAtlas {
    private val textures = mutableMapOf<MaterialId, TexturePair>()
    private var targetResolution = 0
    private var currentTrilinear = true
    private val pendingUpgradeQueue = ArrayDeque<MaterialId>()

    fun ensureInitialized(resolutionPx: Int, trilinearFiltering: Boolean) {
        val safeTarget = resolutionPx.coerceIn(64, 512)
        if (textures.isEmpty()) {
            targetResolution = safeTarget
            currentTrilinear = trilinearFiltering
            pendingUpgradeQueue.clear()

            val allCachedAtTarget = MaterialId.entries.all { mat ->
                sharedBufferCache.containsKey(cacheKey(mat, safeTarget))
            }
            val initialRes = if (allCachedAtTarget) safeTarget else BOOTSTRAP_RES_PX

            MaterialId.entries.forEach { mat ->
                val pair = getOrSynthesizeBuffers(mat, initialRes)
                val albedoId = createAndUploadTexture(pair.albedoBytes, initialRes, trilinearFiltering)
                val normalId = createAndUploadTexture(pair.normalBytes, initialRes, trilinearFiltering)
                textures[mat] = TexturePair(albedoId, normalId)
                if (initialRes < safeTarget) {
                    pendingUpgradeQueue.addLast(mat)
                }
            }
            return
        }

        if (targetResolution != safeTarget || currentTrilinear != trilinearFiltering) {
            targetResolution = safeTarget
            currentTrilinear = trilinearFiltering
            pendingUpgradeQueue.clear()
            pendingUpgradeQueue.addAll(MaterialId.entries)
        }

        // Progressively upgrade up to 2 materials per frame so framerate stays silky smooth
        var budget = 2
        while (budget > 0 && pendingUpgradeQueue.isNotEmpty()) {
            val mat = pendingUpgradeQueue.removeFirst()
            val pair = textures[mat] ?: continue
            val buffers = getOrSynthesizeBuffers(mat, targetResolution)
            updateExistingTexture(pair.albedoTexId, buffers.albedoBytes, targetResolution, currentTrilinear)
            updateExistingTexture(pair.normalTexId, buffers.normalBytes, targetResolution, currentTrilinear)
            budget--
        }
    }

    fun get(materialId: MaterialId): TexturePair? = textures[materialId]

    fun release() {
        pendingUpgradeQueue.clear()
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

    private fun getOrSynthesizeBuffers(mat: MaterialId, size: Int): CachedMaterialBuffers {
        val key = cacheKey(mat, size)
        sharedBufferCache[key]?.let { return it }
        if (sharedBufferCache.size > 36) {
            val staleKeys = sharedBufferCache.keys.filter { !it.endsWith("_$BOOTSTRAP_RES_PX") && !it.endsWith("_$size") }
            staleKeys.forEach { sharedBufferCache.remove(it) }
        }
        val synthesized = synthesizeMaterialBuffers(mat, size)
        sharedBufferCache[key] = synthesized
        return synthesized
    }

    private fun smoothNoise2d(u: Float, v: Float, period: Int, seed: Int): Float {
        val fx = u * period
        val fy = v * period
        val ix = fx.toInt().let { ((it % period) + period) % period }
        val iy = fy.toInt().let { ((it % period) + period) % period }
        val tx = fx - kotlin.math.floor(fx)
        val ty = fy - kotlin.math.floor(fy)
        val sx = tx * tx * (3f - 2f * tx)
        val sy = ty * ty * (3f - 2f * ty)
        val nx1 = (ix + 1) % period
        val ny1 = (iy + 1) % period
        val c00 = hash2d(ix, iy, seed)
        val c10 = hash2d(nx1, iy, seed)
        val c01 = hash2d(ix, ny1, seed)
        val c11 = hash2d(nx1, ny1, seed)
        val a = c00 + (c10 - c00) * sx
        val b = c01 + (c11 - c01) * sx
        return a + (b - a) * sy
    }

    private fun smoothEdge(wave: Float): Float {
        val t = abs(wave).coerceIn(0f, 0.25f) / 0.25f
        return t * t * (3f - 2f * t)
    }

    private fun synthesizeMaterialBuffers(mat: MaterialId, size: Int): CachedMaterialBuffers {
        val albedoBytes = ByteArray(size * size * 4)
        val heightField = FloatArray(size * size)
        val inv = 1f / size.toFloat()
        val seed = mat.ordinal * 31 + 7

        for (y in 0 until size) {
            val v = y * inv
            for (x in 0 until size) {
                val u = x * inv
                val idx = y * size + x
                val px = idx * 4

                val fbm = smoothNoise2d(u, v, 6, seed) * 0.55f +
                    smoothNoise2d(u, v, 14, seed + 19) * 0.30f +
                    smoothNoise2d(u, v, 28, seed + 43) * 0.15f
                val waveU = sin(u * Math.PI * 2.0 * 4.0).toFloat()
                val waveV = cos(v * Math.PI * 2.0 * 4.0).toFloat()
                val macro = sin(u * Math.PI * 2.0 * 2.0 + waveV * 0.7f).toFloat() * 0.5f + 0.5f

                var r = 200f
                var g = 200f
                var b = 200f
                var height = 0.5f

                when (mat) {
                    MaterialId.GRASS_TERRAIN -> {
                        val tuft = sin((u * 12.0 + fbm * 2.2) * Math.PI).toFloat() * 0.5f + 0.5f
                        r = 152f + fbm * 56f + macro * 22f
                        g = 190f + tuft * 34f + fbm * 28f
                        b = 138f + fbm * 34f
                        height = 0.36f + tuft * 0.34f + fbm * 0.30f
                    }
                    MaterialId.DESERT_SAND -> {
                        val dune = sin((u * 5.0 + sin(v * Math.PI * 2.0) * 0.35 + fbm * 0.25) * Math.PI * 2.0).toFloat() * 0.5f + 0.5f
                        r = 224f + dune * 24f - fbm * 14f
                        g = 208f + dune * 20f - fbm * 14f
                        b = 178f + fbm * 18f
                        height = 0.35f + dune * 0.50f + fbm * 0.15f
                    }
                    MaterialId.SNOW_ALPINE -> {
                        val drift = sin((u * 4.0 + v * 2.0 + fbm * 0.4) * Math.PI * 2.0).toFloat() * 0.5f + 0.5f
                        r = 224f + drift * 24f
                        g = 232f + drift * 18f
                        b = 244f + fbm * 10f
                        height = 0.44f + drift * 0.42f + fbm * 0.14f
                    }
                    MaterialId.ASPHALT_ROAD -> {
                        val aggregate = (fbm - 0.5f) * 38f
                        r = 172f + aggregate
                        g = 175f + aggregate
                        b = 182f + aggregate
                        height = 0.35f + fbm * 0.65f
                    }
                    MaterialId.CONCRETE_WALL -> {
                        val seamV = smoothEdge(sin(v * Math.PI * 4.0).toFloat())
                        val seamU = smoothEdge(sin(u * Math.PI * 2.0).toFloat())
                        val seam = 0.80f + 0.20f * seamU * seamV
                        val plaster = 0.90f + fbm * 0.12f
                        r = (208f + fbm * 26f) * seam * plaster
                        g = (210f + fbm * 24f) * seam * plaster
                        b = (214f + fbm * 22f) * seam * plaster
                        height = 0.25f * (1f - seamU * seamV) + (0.45f + fbm * 0.45f)
                    }
                    MaterialId.CORRUGATED_METAL -> {
                        val rib = sin(u * Math.PI * 2.0 * 10.0).toFloat() * 0.5f + 0.5f
                        val shade = 178f + rib * 64f + fbm * 14f
                        r = shade
                        g = shade + 3f
                        b = shade + 8f
                        height = 0.24f + rib * 0.72f
                    }
                    MaterialId.WOOD_CRATE -> {
                        val plankGroove = 0.64f + 0.36f * smoothEdge(sin(v * Math.PI * 4.0).toFloat())
                        val grain = sin((u * 4.0 + fbm * 1.4) * Math.PI * 2.0).toFloat() * 0.5f + 0.5f
                        r = (198f + grain * 40f) * plankGroove
                        g = (182f + grain * 34f) * plankGroove
                        b = (154f + grain * 26f) * plankGroove
                        height = 0.32f * plankGroove + grain * 0.45f
                    }
                    MaterialId.TREE_BARK -> {
                        val furrow = sin(u * Math.PI * 2.0 * 8.0 + sin(v * Math.PI * 2.0) * 0.8f + fbm * 1.6f).toFloat() * 0.5f + 0.5f
                        r = 158f + furrow * 68f + fbm * 20f
                        g = 144f + furrow * 56f + fbm * 18f
                        b = 132f + furrow * 46f
                        height = 0.20f + furrow * 0.75f
                    }
                    MaterialId.PINE_FOLIAGE -> {
                        val needle = abs(sin((u * 12.0 + v * 8.0 + fbm * 1.2) * Math.PI)).toFloat()
                        r = 152f + needle * 48f
                        g = 192f + needle * 52f + fbm * 16f
                        b = 148f + needle * 42f
                        height = 0.30f + needle * 0.62f
                    }
                    MaterialId.TACTICAL_FABRIC -> {
                        val twill = sin((u * 24.0 + v * 24.0) * Math.PI * 2.0).toFloat() * 0.5f + 0.5f
                        val weave = 198f + twill * 28f + (fbm - 0.5f) * 14f
                        r = weave
                        g = weave
                        b = weave + 6f
                        height = 0.40f + twill * 0.36f
                    }
                    MaterialId.SKIN_DETAIL -> {
                        // Subtle organic subsurface warmth variation, zero blocky noise
                        val pore = (fbm - 0.5f) * 10f
                        r = 238f + pore
                        g = 222f + pore * 0.85f
                        b = 216f + pore * 0.75f
                        height = 0.48f + fbm * 0.08f
                    }
                    MaterialId.VEHICLE_COAT -> {
                        val panel = 0.82f + 0.18f * smoothEdge(sin(u * Math.PI * 2.0).toFloat()) * smoothEdge(sin(v * Math.PI * 2.0).toFloat())
                        val flake = 228f + (fbm - 0.5f) * 14f
                        r = flake * panel
                        g = flake * panel
                        b = (flake + 4f) * panel
                        height = 0.70f * panel
                    }
                    MaterialId.WATER_RIPPLE -> {
                        val rip1 = sin((u * 4.0 + v * 3.0 + fbm * 0.3) * Math.PI * 2.0).toFloat()
                        val rip2 = cos((u * 3.0 - v * 5.0 - fbm * 0.3) * Math.PI * 2.0).toFloat()
                        val foam = ((rip1 + rip2) * 0.25f + 0.5f).coerceIn(0f, 1f)
                        r = 178f + foam * 66f
                        g = 216f + foam * 36f
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

        return CachedMaterialBuffers(albedoBytes, normalBytes)
    }

    private fun createAndUploadTexture(bytes: ByteArray, size: Int, trilinear: Boolean): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        val texId = ids[0]
        updateExistingTexture(texId, bytes, size, trilinear)
        return texId
    }

    private fun updateExistingTexture(texId: Int, bytes: ByteArray, size: Int, trilinear: Boolean) {
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
    }

    private fun hash2d(x: Int, y: Int, seed: Int): Float {
        var n = x * 374761393 + y * 668265263 + seed * 1442695041
        n = (n xor (n shr 13)) * 1274126177
        return ((n xor (n shr 16)) and 0x7FFFFFFF).toFloat() / 0x7FFFFFFF.toFloat()
    }

    private data class CachedMaterialBuffers(
        val albedoBytes: ByteArray,
        val normalBytes: ByteArray
    )

    private companion object {
        const val BOOTSTRAP_RES_PX = 32
        val sharedBufferCache = ConcurrentHashMap<String, CachedMaterialBuffers>()
        fun cacheKey(mat: MaterialId, size: Int): String = "${mat.name}_$size"
    }
}
