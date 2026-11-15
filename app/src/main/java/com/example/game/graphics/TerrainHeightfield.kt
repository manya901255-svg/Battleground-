package com.example.game.graphics

import com.example.game.map.Building
import com.example.game.map.MapConfig
import com.example.game.map.WaterBody
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class BridgeSpec(
    val centerX: Float, // Engine coords (0..5000)
    val startY: Float,
    val endY: Float,
    val halfWidthEngine: Float = 58f,
    val deckHeightGl: Float = 0.72f
)

data class Vec3(val x: Float, val y: Float, val z: Float)

/**
 * Shared deterministic heightfield, bridge spans, road flattening, and camera-obstacle collision
 * helper used identically by Terrain Mesh generation, entity grounding, water immersion, and
 * third-person camera collision resolution.
 */
object TerrainHeightfield {
    const val WORLD_SCALE = 0.1f // 5000 engine units -> 500m OpenGL world

    fun bridgesForMap(mapConfig: MapConfig): List<BridgeSpec> {
        if (mapConfig.waterBodies.isEmpty()) return emptyList()
        val bridges = mutableListOf<BridgeSpec>()
        mapConfig.waterBodies.forEach { wb ->
            val midX1 = wb.x + wb.width * 0.28f
            val midX2 = wb.x + wb.width * 0.50f
            val midX3 = wb.x + wb.width * 0.74f
            listOf(midX1, midX2, midX3).distinct().forEach { bx ->
                bridges.add(
                    BridgeSpec(
                        centerX = bx,
                        startY = wb.y - 35f,
                        endY = wb.y + wb.height + 35f,
                        halfWidthEngine = 56f,
                        deckHeightGl = 0.75f
                    )
                )
            }
        }
        return bridges
    }

    fun isOnBridge(mapConfig: MapConfig, engineX: Float, engineY: Float): Boolean {
        val bridges = bridgesForMap(mapConfig)
        for (b in bridges) {
            if (abs(engineX - b.centerX) <= b.halfWidthEngine &&
                engineY in b.startY..b.endY
            ) {
                return true
            }
        }
        return false
    }

    fun isInWaterNotOnBridge(mapConfig: MapConfig, engineX: Float, engineY: Float): Boolean {
        if (isOnBridge(mapConfig, engineX, engineY)) return false
        for (wb in mapConfig.waterBodies) {
            if (engineX in wb.x..(wb.x + wb.width) &&
                engineY in wb.y..(wb.y + wb.height)
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Returns terrain surface elevation in GL world meters at engine coordinates (engineX, engineY).
     * Combines undulating hills, perimeter mountain ridges, flattened roads, flat building pads,
     * river depressions, and elevated bridge decks.
     */
    fun groundHeightGl(mapConfig: MapConfig, engineX: Float, engineY: Float): Float {
        // Bridge deck takes top priority over river channel
        val bridges = bridgesForMap(mapConfig)
        for (b in bridges) {
            if (abs(engineX - b.centerX) <= b.halfWidthEngine &&
                engineY in b.startY..b.endY
            ) {
                val span = (b.endY - b.startY).coerceAtLeast(1f)
                val t = ((engineY - b.startY) / span).coerceIn(0f, 1f)
                // Gentle arch peak at river center
                val arch = sin(t * Math.PI).toFloat() * 0.65f
                return b.deckHeightGl + arch
            }
        }

        val gx = engineX * WORLD_SCALE
        val gz = engineY * WORLD_SCALE

        // Base multi-octave organic hills mapped per biome
        val hillAmp = when (mapConfig.id) {
            "snow_mountain" -> 3.6f
            "kalahari_dunes" -> 2.4f
            "jungle_battleground" -> 2.1f
            "urban_city" -> 0.55f
            else -> 1.85f
        }
        var h = (sin(gx * 0.036f) * cos(gz * 0.034f) * 0.65f +
            sin(gx * 0.082f + 1.2f) * sin(gz * 0.075f - 0.6f) * 0.35f) * hillAmp

        // Surrounding horizon mountain rim outside core combat zone (r > 185m from center)
        val dxC = gx - 250f
        val dzC = gz - 250f
        val distCenter = sqrt(dxC * dxC + dzC * dzC)
        if (distCenter > 175f) {
            val mountainFactor = ((distCenter - 175f) / 70f).coerceIn(0f, 1.5f)
            val ridge = abs(sin(gx * 0.05f + gz * 0.04f)) * 12f + abs(cos(gx * 0.09f - gz * 0.07f)) * 5.5f
            h += mountainFactor * mountainFactor * ridge
        }

        // Smooth flatten along the cross-island highway roads (x=250, z=250) and center Pochinki hub
        val distRoadZ = abs(gz - 250f)
        val distRoadX = abs(gx - 250f)
        val distCenterHub = sqrt((gx - 250f) * (gx - 250f) + (gz - 250f) * (gz - 250f))
        val roadFlatten = min(
            min((distRoadZ / 13f).coerceIn(0f, 1f), (distRoadX / 13f).coerceIn(0f, 1f)),
            (distCenterHub / 32f).coerceIn(0f, 1f)
        )
        h *= roadFlatten

        // Flatten around every building so foundations never float or sink
        for (b in mapConfig.buildings) {
            val bx = (b.x + b.width * 0.5f) * WORLD_SCALE
            val bz = (b.y + b.height * 0.5f) * WORLD_SCALE
            val halfW = b.width * WORLD_SCALE * 0.65f + 2.5f
            val halfD = b.height * WORLD_SCALE * 0.65f + 2.5f
            val dx = abs(gx - bx) - halfW
            val dz = abs(gz - bz) - halfD
            if (dx < 5f && dz < 5f) {
                val edgeDist = max(max(dx, dz), 0f)
                val factor = (edgeDist / 5f).coerceIn(0f, 1f)
                h *= factor
            }
        }

        // Carve riverbed channels smoothly below water level (0.04f)
        for (wb in mapConfig.waterBodies) {
            val rx0 = wb.x * WORLD_SCALE
            val rx1 = (wb.x + wb.width) * WORLD_SCALE
            val rz0 = wb.y * WORLD_SCALE
            val rz1 = (wb.y + wb.height) * WORLD_SCALE
            val margin = 4.0f
            if (gx in (rx0 - margin)..(rx1 + margin) && gz in (rz0 - margin)..(rz1 + margin)) {
                val dxEdge = min(gx - (rx0 - margin), (rx1 + margin) - gx).coerceAtLeast(0f)
                val dzEdge = min(gz - (rz0 - margin), (rz1 + margin) - gz).coerceAtLeast(0f)
                val bankFactor = (min(dxEdge, dzEdge) / (margin * 1.6f)).coerceIn(0f, 1f)
                h = h * (1f - bankFactor) - 0.85f * bankFactor
            }
        }

        return h
    }

    /**
     * Smooth surface normal in GL space computed from finite differences of groundHeightGl.
     */
    fun terrainNormalGl(mapConfig: MapConfig, engineX: Float, engineY: Float): Vec3 {
        val stepEngine = 12f
        val hL = groundHeightGl(mapConfig, engineX - stepEngine, engineY)
        val hR = groundHeightGl(mapConfig, engineX + stepEngine, engineY)
        val hD = groundHeightGl(mapConfig, engineX, engineY - stepEngine)
        val hU = groundHeightGl(mapConfig, engineX, engineY + stepEngine)
        val nx = hL - hR
        val ny = 2.0f * (stepEngine * WORLD_SCALE)
        val nz = hD - hU
        val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
        return Vec3(nx / len, ny / len, nz / len)
    }

    /**
     * Third-person camera collision resolver:
     * Traces from player pivot (pivotGlX, pivotGlY, pivotGlZ) toward desired camera position
     * and pulls camera inward if the ray intersects any Building or dips into Terrain geometry.
     */
    fun resolveCameraCollision(
        mapConfig: MapConfig,
        pivotGlX: Float,
        pivotGlY: Float,
        pivotGlZ: Float,
        desiredCamX: Float,
        desiredCamY: Float,
        desiredCamZ: Float
    ): Vec3 {
        val steps = 12
        var safeX = desiredCamX
        var safeY = desiredCamY
        var safeZ = desiredCamZ

        for (i in 1..steps) {
            val t = i.toFloat() / steps.toFloat()
            val sampleX = pivotGlX + (desiredCamX - pivotGlX) * t
            val sampleY = pivotGlY + (desiredCamY - pivotGlY) * t
            val sampleZ = pivotGlZ + (desiredCamZ - pivotGlZ) * t

            val sampleEngineX = sampleX / WORLD_SCALE
            val sampleEngineY = sampleZ / WORLD_SCALE

            // Check terrain ground clearance
            val minGroundY = groundHeightGl(mapConfig, sampleEngineX, sampleEngineY) + 0.65f
            if (sampleY < minGroundY) {
                safeY = max(safeY, minGroundY)
            }

            // Check intersection against solid building bounds (with 0.45m camera wall padding)
            val hitBuilding = isPointInsideAnyBuilding(mapConfig.buildings, sampleX, sampleY, sampleZ, paddingGl = 0.42f)
            if (hitBuilding) {
                val safeT = ((i - 1).toFloat() / steps.toFloat()).coerceAtLeast(0.18f)
                safeX = pivotGlX + (desiredCamX - pivotGlX) * safeT
                safeZ = pivotGlZ + (desiredCamZ - pivotGlZ) * safeT
                safeY = max(pivotGlY + (desiredCamY - pivotGlY) * safeT, minGroundY)
                break
            }
        }

        val finalGround = groundHeightGl(mapConfig, safeX / WORLD_SCALE, safeZ / WORLD_SCALE) + 0.65f
        return Vec3(safeX, max(safeY, finalGround), safeZ)
    }

    private fun isPointInsideAnyBuilding(
        buildings: List<Building>,
        gx: Float,
        gy: Float,
        gz: Float,
        paddingGl: Float
    ): Boolean {
        for (b in buildings) {
            val bx0 = b.x * WORLD_SCALE - paddingGl
            val bx1 = (b.x + b.width) * WORLD_SCALE + paddingGl
            val bz0 = b.y * WORLD_SCALE - paddingGl
            val bz1 = (b.y + b.height) * WORLD_SCALE + paddingGl
            val bw = b.width * WORLD_SCALE
            val bTop = if (bw > 9f) 7.4f else 5.6f
            if (gx in bx0..bx1 && gz in bz0..bz1 && gy in 0f..bTop) {
                return true
            }
        }
        return false
    }
}
