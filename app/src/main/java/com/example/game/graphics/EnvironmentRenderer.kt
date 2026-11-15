package com.example.game.graphics

import android.opengl.GLES20
import android.opengl.Matrix
import com.example.game.map.MapConfig
import com.example.game.map.ObstacleType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders the upgraded 3D environment: Height-mapped biome terrain & perimeter mountains,
 * paved roads & steel-truss river bridges, houses & industrial warehouses/factories with glass
 * windows, fluted bark conifer trees, jagged displaced boulders, bushes, wind-swaying grass
 * tufts, and animated water reflections.
 */
class EnvironmentRenderer(
    private val meshes: MeshLibrary,
    private val textures: PbrTextureAtlas,
    private val shader: PbrShaderProgram
) {
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var cachedMapId: String = ""
    private var terrainMesh: GlMesh? = null

    fun setViewProjection(vp: FloatArray) {
        System.arraycopy(vp, 0, vpMatrix, 0, 16)
    }

    fun ensureTerrainMesh(mapConfig: MapConfig) {
        if (terrainMesh == null || cachedMapId != mapConfig.id) {
            cachedMapId = mapConfig.id
            terrainMesh = meshes.buildTerrainMeshForMap(mapConfig, gridRes = 56)
        }
    }

    fun renderWorldEnvironment(
        mapConfig: MapConfig,
        focusX: Float,
        focusZ: Float,
        drawDist: Float,
        graphicsConfig: GraphicsConfig,
        frameTimeSec: Float
    ) {
        ensureTerrainMesh(mapConfig)

        // 1. Height-mapped Terrain & Horizon Mountains
        val (terrainMat, tr, tg, tb) = when (mapConfig.id) {
            "kalahari_dunes" -> MaterialTuple(MaterialId.DESERT_SAND, 0.88f, 0.72f, 0.48f)
            "snow_mountain" -> MaterialTuple(MaterialId.SNOW_ALPINE, 0.90f, 0.94f, 0.98f)
            "urban_city" -> MaterialTuple(MaterialId.GRASS_TERRAIN, 0.44f, 0.48f, 0.42f)
            "jungle_battleground" -> MaterialTuple(MaterialId.GRASS_TERRAIN, 0.34f, 0.62f, 0.32f)
            else -> MaterialTuple(MaterialId.GRASS_TERRAIN, 0.48f, 0.66f, 0.38f)
        }
        terrainMesh?.let { tm ->
            drawMesh(
                tm, terrainMat,
                0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 1f,
                tr, tg, tb, 1f,
                roughness = 0.88f, metallic = 0.02f, uvScaleX = 1f, uvScaleY = 1f
            )
        }

        // 2. Paved Asphalt Highway Cross & Crosswalk Stripes
        drawMesh(meshes.unitBox, MaterialId.ASPHALT_ROAD, 250f, 0.03f, 250f, 0f, 0f, 0f, 500f, 0.06f, 11f, 0.24f, 0.25f, 0.28f, 1f, 0.74f, 0.05f, uvScaleX = 28f, uvScaleY = 1.5f)
        drawMesh(meshes.unitBox, MaterialId.ASPHALT_ROAD, 250f, 0.035f, 250f, 0f, 0f, 0f, 11f, 0.06f, 500f, 0.24f, 0.25f, 0.28f, 1f, 0.74f, 0.05f, uvScaleX = 1.5f, uvScaleY = 28f)
        val snapX = ((focusX / 12f).toInt() * 12f).coerceIn(24f, 476f)
        val snapZ = ((focusZ / 12f).toInt() * 12f).coerceIn(24f, 476f)
        for (i in -6..6) {
            val sx = snapX + i * 12f
            val sz = snapZ + i * 12f
            if (abs(250f - focusZ) < drawDist) {
                drawMesh(meshes.unitBox, MaterialId.CONCRETE_WALL, sx, 0.065f, 250f, 0f, 0f, 0f, 4.4f, 0.02f, 0.42f, 0.96f, 0.82f, 0.16f, 1f, 0.45f, 0f, emissive = 0.25f, useTex = 0f)
            }
            if (abs(250f - focusX) < drawDist) {
                drawMesh(meshes.unitBox, MaterialId.CONCRETE_WALL, 250f, 0.065f, sz, 0f, 0f, 0f, 0.42f, 0.02f, 4.4f, 0.96f, 0.82f, 0.16f, 1f, 0.45f, 0f, emissive = 0.25f, useTex = 0f)
            }
        }

        // 3. Animated Rippled Water Bodies
        mapConfig.waterBodies.forEach { wb ->
            val cx = (wb.x + wb.width * 0.5f) * TerrainHeightfield.WORLD_SCALE
            val cz = (wb.y + wb.height * 0.5f) * TerrainHeightfield.WORLD_SCALE
            val w = wb.width * TerrainHeightfield.WORLD_SCALE
            val d = wb.height * TerrainHeightfield.WORLD_SCALE
            drawMesh(
                meshes.waterGrid, MaterialId.WATER_RIPPLE,
                cx, 0.08f, cz, 0f, 0f, 0f, w, 1f, d,
                0.10f, 0.52f, 0.78f, 0.84f,
                roughness = 0.08f, metallic = 0.38f,
                uvScaleX = w * 0.14f, uvScaleY = d * 0.14f,
                uvOffX = frameTimeSec * 0.06f, uvOffY = sin(frameTimeSec * 0.4f) * 0.03f
            )
        }

        // 4. Steel Truss Bridges Spanning Rivers
        TerrainHeightfield.bridgesForMap(mapConfig).forEach { br ->
            val bx = br.centerX * TerrainHeightfield.WORLD_SCALE
            val bz = ((br.startY + br.endY) * 0.5f) * TerrainHeightfield.WORLD_SCALE
            val bw = br.halfWidthEngine * 2f * TerrainHeightfield.WORLD_SCALE
            val bLen = (br.endY - br.startY) * TerrainHeightfield.WORLD_SCALE
            val dx = bx - focusX; val dz = bz - focusZ
            if (dx * dx + dz * dz < (drawDist + 40f) * (drawDist + 40f)) {
                // Concrete roadbed deck
                drawMesh(meshes.unitBox, MaterialId.ASPHALT_ROAD, bx, br.deckHeightGl + 0.16f, bz, 0f, 0f, 0f, bw, 0.42f, bLen, 0.34f, 0.36f, 0.39f, 1f, 0.7f, 0.1f, uvScaleX = 2f, uvScaleY = 6f)
                // Steel truss side girders & overhead arch beams
                for (side in listOf(-bw * 0.48f, bw * 0.48f)) {
                    drawMesh(meshes.unitBox, MaterialId.CORRUGATED_METAL, bx + side, br.deckHeightGl + 1.45f, bz, 0f, 0f, 0f, 0.45f, 2.3f, bLen, 0.58f, 0.24f, 0.18f, 1f, 0.38f, 0.82f)
                    drawMesh(meshes.unitBox, MaterialId.CORRUGATED_METAL, bx + side, br.deckHeightGl + 2.65f, bz, 0f, 0f, 0f, 0.55f, 0.36f, bLen * 0.92f, 0.48f, 0.20f, 0.16f, 1f, 0.35f, 0.85f)
                }
                // Cross overhead steel rafters
                val spanCount = 4
                for (k in 0..spanCount) {
                    val rzPos = bz - bLen * 0.38f + (k.toFloat() / spanCount) * bLen * 0.76f
                    drawMesh(meshes.unitBox, MaterialId.CORRUGATED_METAL, bx, br.deckHeightGl + 2.65f, rzPos, 0f, 0f, 0f, bw, 0.28f, 0.35f, 0.45f, 0.22f, 0.18f, 1f, 0.35f, 0.85f)
                }
            }
        }

        // 5. Buildings (Houses, Factories, Warehouses with pitched roofs, recessed framed windows, and doors)
        mapConfig.buildings.forEachIndexed { idx, b ->
            val bx = (b.x + b.width * 0.5f) * TerrainHeightfield.WORLD_SCALE
            val bz = (b.y + b.height * 0.5f) * TerrainHeightfield.WORLD_SCALE
            val dx = bx - focusX; val dz = bz - focusZ
            if (dx * dx + dz * dz < drawDist * drawDist) {
                val bw = b.width * TerrainHeightfield.WORLD_SCALE
                val bd = b.height * TerrainHeightfield.WORLD_SCALE
                val isWarehouse = bw > 10.5f || idx % 3 == 0
                val bh = if (isWarehouse) 6.4f else 5.0f

                // Concrete foundation slab
                drawMesh(meshes.unitBox, MaterialId.CONCRETE_WALL, bx, 0.22f, bz, 0f, 0f, 0f, bw + 1.0f, 0.44f, bd + 1.0f, 0.32f, 0.34f, 0.36f, 1f, 0.84f, 0.05f, uvScaleX = 3f, uvScaleY = 1f)

                if (isWarehouse) {
                    // Corrugated steel warehouse / factory walls + pitched metal roof + HVAC
                    drawMesh(meshes.unitBox, MaterialId.CORRUGATED_METAL, bx, bh * 0.5f + 0.44f, bz, 0f, 0f, 0f, bw, bh, bd, 0.46f, 0.52f, 0.58f, 1f, 0.42f, 0.72f, uvScaleX = 3f, uvScaleY = 2f)
                    drawMesh(meshes.pitchedRoof, MaterialId.CORRUGATED_METAL, bx, bh + 1.35f, bz, 0f, 0f, 0f, bw + 0.8f, 1.9f, bd + 0.8f, 0.22f, 0.32f, 0.44f, 1f, 0.35f, 0.78f, uvScaleX = 3f, uvScaleY = 2f)
                    // Sliding steel hangar door + reflective upper clerestory glass windows
                    drawMesh(meshes.unitBox, MaterialId.CORRUGATED_METAL, bx, 1.85f, bz + bd * 0.5f + 0.06f, 0f, 0f, 0f, 3.1f, 3.2f, 0.16f, 0.16f, 0.48f, 0.76f, 1f, 0.32f, 0.82f)
                } else {
                    // Two-story battleground house with concrete walls + red-terracotta pitched roof
                    drawMesh(meshes.unitBox, MaterialId.CONCRETE_WALL, bx, bh * 0.5f + 0.44f, bz, 0f, 0f, 0f, bw, bh, bd, 0.76f, 0.72f, 0.66f, 1f, 0.82f, 0.04f, uvScaleX = 2.5f, uvScaleY = 2f)
                    drawMesh(meshes.pitchedRoof, MaterialId.CORRUGATED_METAL, bx, bh + 1.28f, bz, 0f, 0f, 0f, bw + 0.9f, 1.75f, bd + 0.9f, 0.62f, 0.28f, 0.20f, 1f, 0.65f, 0.18f, uvScaleX = 3f, uvScaleY = 2f)
                    drawMesh(meshes.unitBox, MaterialId.WOOD_CRATE, bx, 1.45f, bz + bd * 0.5f + 0.06f, 0f, 0f, 0f, 1.45f, 2.35f, 0.14f, 0.42f, 0.26f, 0.14f, 1f, 0.7f, 0.05f)
                }
                // Reflective Framed Windows on front & side walls
                val winY = bh * 0.66f
                drawMesh(meshes.unitBox, MaterialId.CORRUGATED_METAL, bx - bw * 0.26f, winY, bz + bd * 0.5f + 0.05f, 0f, 0f, 0f, 1.75f, 1.35f, 0.14f, 0.10f, 0.48f, 0.82f, 1f, 0.4f, 0.6f)
                drawMesh(meshes.unitBox, MaterialId.VEHICLE_COAT, bx - bw * 0.26f, winY, bz + bd * 0.5f + 0.10f, 0f, 0f, 0f, 1.42f, 1.02f, 0.10f, 0.22f, 0.38f, 0.52f, 0.94f, 0.06f, 0.92f)
                drawMesh(meshes.unitBox, MaterialId.CORRUGATED_METAL, bx + bw * 0.26f, winY, bz + bd * 0.5f + 0.05f, 0f, 0f, 0f, 1.75f, 1.35f, 0.14f, 0.10f, 0.48f, 0.82f, 1f, 0.4f, 0.6f)
                drawMesh(meshes.unitBox, MaterialId.VEHICLE_COAT, bx + bw * 0.26f, winY, bz + bd * 0.5f + 0.10f, 0f, 0f, 0f, 1.42f, 1.02f, 0.10f, 0.22f, 0.38f, 0.52f, 0.94f, 0.06f, 0.92f)
                drawMesh(meshes.unitBox, MaterialId.VEHICLE_COAT, bx + bw * 0.5f + 0.08f, winY, bz, 0f, 0f, 0f, 0.12f, 1.08f, 1.45f, 0.22f, 0.38f, 0.52f, 0.94f, 0.06f, 0.92f)
            }
        }

        // 6. Conifer Pine Trees, Sculpted Boulders, Bushes & Grass Tufts
        val isDesert = mapConfig.id == "kalahari_dunes"
        val isSnow = mapConfig.id == "snow_mountain"
        mapConfig.obstacles.forEachIndexed { i, obs ->
            val ox = obs.x * TerrainHeightfield.WORLD_SCALE
            val oz = obs.y * TerrainHeightfield.WORLD_SCALE
            val dx = ox - focusX; val dz = oz - focusZ
            if (dx * dx + dz * dz < drawDist * drawDist) {
                val gy = TerrainHeightfield.groundHeightGl(mapConfig, obs.x, obs.y)
                val r = obs.radius * TerrainHeightfield.WORLD_SCALE
                val sway = sin(frameTimeSec * 1.6f + i * 0.7f) * 2.2f
                when (obs.type) {
                    ObstacleType.TREE -> {
                        if (graphicsConfig.shadowQuality > 0) {
                            drawMesh(meshes.softShadowDisc, MaterialId.GRASS_TERRAIN, ox, gy + 0.04f, oz, 0f, 0f, 0f, r * 1.9f, 1f, r * 1.9f, 0.03f, 0.04f, 0.03f, 0.42f, 1f, 0f, 0.75f, 0f)
                        }
                        // Fluted Bark Trunk
                        drawMesh(meshes.treeTrunk, MaterialId.TREE_BARK, ox, gy + 2.0f, oz, 0f, 0f, 0f, 0.56f, 4.1f, 0.56f, 0.42f, 0.28f, 0.16f, 1f, 0.88f, 0.02f, uvScaleX = 1.5f, uvScaleY = 3f)
                        // 3 Layered Organic Evergreen Cones with snow tint on alpine map
                        val folR = if (isSnow) 0.55f else 0.18f
                        val folG = if (isSnow) 0.68f else 0.46f
                        val folB = if (isSnow) 0.72f else 0.16f
                        drawMesh(meshes.foliageCone, MaterialId.PINE_FOLIAGE, ox, gy + 3.5f, oz, 0f, 12f, sway, r * 1.75f, 2.35f, r * 1.75f, folR, folG, folB, 1f, 0.78f, 0.02f, uvScaleX = 2f, uvScaleY = 2f)
                        drawMesh(meshes.foliageCone, MaterialId.PINE_FOLIAGE, ox, gy + 4.9f, oz, 0f, 48f, sway * 1.2f, r * 1.38f, 2.15f, r * 1.38f, folR + 0.04f, folG + 0.08f, folB + 0.03f, 1f, 0.76f, 0.02f, uvScaleX = 2f, uvScaleY = 2f)
                        drawMesh(meshes.foliageCone, MaterialId.PINE_FOLIAGE, ox, gy + 6.2f, oz, 0f, 82f, sway * 1.5f, r * 0.92f, 1.85f, r * 0.92f, folR + 0.08f, folG + 0.14f, folB + 0.05f, 1f, 0.74f, 0.02f)
                        // Accompanying low bush around selected trunks
                        if (i % 2 == 0) {
                            drawMesh(meshes.bushCluster, MaterialId.PINE_FOLIAGE, ox + r * 0.85f, gy + 0.42f, oz + 0.4f, 0f, (i * 40f) % 360f, sway, 1.35f, 0.95f, 1.35f, 0.22f, 0.48f, 0.18f, 1f, 0.8f, 0.02f)
                        }
                    }
                    ObstacleType.ROCK -> {
                        val rockTint = if (isDesert) Triple(0.68f, 0.56f, 0.42f) else if (isSnow) Triple(0.64f, 0.68f, 0.74f) else Triple(0.48f, 0.50f, 0.52f)
                        drawMesh(meshes.boulderRock, MaterialId.CONCRETE_WALL, ox, gy + r * 0.35f, oz, 12f, (i * 37f) % 360f, 0f, r * 1.75f, r * 1.15f, r * 1.48f, rockTint.first, rockTint.second, rockTint.third, 1f, 0.82f, 0.06f)
                        drawMesh(meshes.boulderRock, MaterialId.CONCRETE_WALL, ox + r * 0.42f, gy + r * 0.20f, oz - r * 0.25f, 0f, (i * 83f) % 360f, 0f, r * 0.95f, r * 0.68f, r * 0.90f, rockTint.first * 0.88f, rockTint.second * 0.88f, rockTint.third * 0.88f, 1f, 0.85f, 0.05f)
                    }
                    ObstacleType.CRATE, ObstacleType.SANDBAGS -> {
                        drawMesh(meshes.unitBox, MaterialId.WOOD_CRATE, ox, gy + 0.75f, oz, 0f, (i * 19f) % 360f, 0f, 1.6f, 1.5f, 1.6f, 0.66f, 0.48f, 0.28f, 1f, 0.72f, 0.05f)
                    }
                }
            }
        }

        // 7. Ambient Grass Blades Near Player Camera (LOD culled)
        val grassStep = graphicsConfig.grassDensityStep
        if (grassStep > 0 && !isDesert) {
            val baseX = ((focusX / 5f).toInt() * 5)
            val baseZ = ((focusZ / 5f).toInt() * 5)
            for (gz in -20..20 step (4 * grassStep)) {
                for (gx in -20..20 step (4 * grassStep)) {
                    val px = (baseX + gx).toFloat()
                    val pz = (baseZ + gz).toFloat()
                    if (abs(px - 250f) < 6.5f || abs(pz - 250f) < 6.5f) continue // skip asphalt highways
                    val ex = px / TerrainHeightfield.WORLD_SCALE
                    val ez = pz / TerrainHeightfield.WORLD_SCALE
                    if (TerrainHeightfield.isInWaterNotOnBridge(mapConfig, ex, ez)) continue
                    val gy = TerrainHeightfield.groundHeightGl(mapConfig, ex, ez)
                    val jitX = sin(px * 0.7f + pz * 0.3f) * 1.2f
                    val jitZ = cos(pz * 0.9f - px * 0.4f) * 1.2f
                    val windLean = sin(frameTimeSec * 2.4f + px * 0.4f) * 9f
                    val bladeCol = if (isSnow) Triple(0.75f, 0.82f, 0.88f) else Triple(0.28f, 0.58f, 0.20f)
                    drawMesh(meshes.grassTuft, MaterialId.PINE_FOLIAGE, px + jitX, gy, pz + jitZ, windLean, (px * 13f + pz * 7f) % 360f, 0f, 0.95f, 0.78f, 0.95f, bladeCol.first, bladeCol.second, bladeCol.third, 1f, 0.65f, 0f)
                }
            }
        }

        // 8. Iconic Arena Landmarks (Shipping Containers, Crates, Fence, Billboard, Cooling Tower, Radio Mast)
        if ((focusX - 250f) * (focusX - 250f) + (focusZ - 250f) * (focusZ - 250f) < drawDist * drawDist) {
            drawMesh(meshes.unitBox, MaterialId.CORRUGATED_METAL, 264f, 1.7f, 240f, 0f, -15f, 0f, 7.6f, 3.4f, 3.2f, 0.12f, 0.48f, 0.82f, 1f, 0.36f, 0.82f, uvScaleX = 4f, uvScaleY = 1.5f)
            drawMesh(meshes.unitBox, MaterialId.CORRUGATED_METAL, 234f, 1.7f, 263f, 0f, 72f, 0f, 7.2f, 3.3f, 3.1f, 0.22f, 0.40f, 0.28f, 1f, 0.38f, 0.78f, uvScaleX = 4f, uvScaleY = 1.5f)

            // Stacked Wooden Crates
            drawMesh(meshes.unitBox, MaterialId.WOOD_CRATE, 260f, 0.85f, 244f, 0f, 5f, 0f, 1.8f, 1.7f, 1.8f, 0.72f, 0.52f, 0.30f, 1f, 0.72f, 0.04f)
            drawMesh(meshes.unitBox, MaterialId.WOOD_CRATE, 262f, 0.85f, 244.5f, 0f, 12f, 0f, 1.8f, 1.7f, 1.8f, 0.66f, 0.46f, 0.25f, 1f, 0.72f, 0.04f)
            drawMesh(meshes.unitBox, MaterialId.WOOD_CRATE, 261f, 2.55f, 244.2f, 0f, -8f, 0f, 1.7f, 1.7f, 1.7f, 0.75f, 0.56f, 0.32f, 1f, 0.70f, 0.04f)
            drawMesh(meshes.unitBox, MaterialId.WOOD_CRATE, 242f, 0.65f, 260f, 0f, 12f, 0f, 6.5f, 1.25f, 0.24f, 0.56f, 0.40f, 0.22f, 1f, 0.75f, 0.02f, uvScaleX = 3f, uvScaleY = 1f)

            // Highway Billboard
            drawMesh(meshes.treeTrunk, MaterialId.CORRUGATED_METAL, 272f, 3.2f, 232f, 0f, 0f, 0f, 0.62f, 6.4f, 0.62f, 0.26f, 0.28f, 0.32f, 1f, 0.3f, 0.85f)
            drawMesh(meshes.unitBox, MaterialId.CORRUGATED_METAL, 272f, 6.8f, 232f, 0f, -20f, 0f, 7.6f, 3.6f, 0.45f, 0.10f, 0.12f, 0.20f, 1f, 0.28f, 0.82f)
            drawMesh(meshes.unitBox, MaterialId.VEHICLE_COAT, 272f, 6.8f, 232.18f, 0f, -20f, 0f, 7.0f, 3.0f, 0.25f, 1.0f, 0.72f, 0.10f, 1f, 0.2f, 0.4f, emissive = 0.75f)

            // Cylindrical Industrial Cooling Tower + Radio Lattice Mast
            drawMesh(meshes.treeTrunk, MaterialId.CONCRETE_WALL, 295f, 10.5f, 195f, 0f, 0f, 0f, 11.5f, 21f, 11.5f, 0.60f, 0.64f, 0.68f, 1f, 0.82f, 0.05f, uvScaleX = 4f, uvScaleY = 3f)
            drawMesh(meshes.wheelCylinder, MaterialId.CONCRETE_WALL, 295f, 21.4f, 195f, 90f, 0f, 0f, 8.6f, 8.6f, 1.2f, 0.72f, 0.75f, 0.78f, 1f, 0.78f, 0.08f)
            drawMesh(meshes.treeTrunk, MaterialId.CORRUGATED_METAL, 318f, 10f, 212f, 0f, 0f, 0f, 1.8f, 20f, 1.8f, 0.46f, 0.50f, 0.55f, 1f, 0.30f, 0.88f)
            val beaconPulse = 0.55f + 0.45f * sin(frameTimeSec * 4.5f)
            drawMesh(meshes.boulderRock, MaterialId.SKIN_DETAIL, 318f, 20.6f, 212f, 0f, 0f, 0f, 0.75f, 0.75f, 0.75f, 1.0f, 0.16f * beaconPulse, 0.12f, 1f, 0.2f, 0f, emissive = 1.0f, useTex = 0f)
        }
    }

    private fun drawMesh(
        mesh: GlMesh,
        mat: MaterialId,
        x: Float, y: Float, z: Float,
        rx: Float, ry: Float, rz: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float, a: Float,
        roughness: Float, metallic: Float,
        emissive: Float = 0f,
        useTex: Float = 1f,
        uvScaleX: Float = 1f, uvScaleY: Float = 1f,
        uvOffX: Float = 0f, uvOffY: Float = 0f
    ) {
        val texPair = textures.get(mat)
        if (texPair != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texPair.albedoTexId)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texPair.normalTexId)
        }
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        if (ry != 0f) Matrix.rotateM(modelMatrix, 0, ry, 0f, 1f, 0f)
        if (rx != 0f) Matrix.rotateM(modelMatrix, 0, rx, 1f, 0f, 0f)
        if (rz != 0f) Matrix.rotateM(modelMatrix, 0, rz, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        val buf = mesh.buffer
        buf.position(0)
        GLES20.glVertexAttribPointer(shader.aPos, 3, GLES20.GL_FLOAT, false, 32, buf)
        buf.position(3)
        GLES20.glVertexAttribPointer(shader.aNorm, 3, GLES20.GL_FLOAT, false, 32, buf)
        buf.position(6)
        GLES20.glVertexAttribPointer(shader.aUv, 2, GLES20.GL_FLOAT, false, 32, buf)

        GLES20.glUniformMatrix4fv(shader.uMvp, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uModel, 1, false, modelMatrix, 0)
        GLES20.glUniform4f(shader.uTintColor, r, g, b, a)
        GLES20.glUniform1f(shader.uRoughness, roughness)
        GLES20.glUniform1f(shader.uMetallic, metallic)
        GLES20.glUniform1f(shader.uEmissive, emissive)
        GLES20.glUniform1f(shader.uUseTex, useTex)
        GLES20.glUniform2f(shader.uUvScale, uvScaleX, uvScaleY)
        GLES20.glUniform2f(shader.uUvOffset, uvOffX, uvOffY)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.vertexCount)
    }

    private data class MaterialTuple(val mat: MaterialId, val r: Float, val g: Float, val b: Float)
}
