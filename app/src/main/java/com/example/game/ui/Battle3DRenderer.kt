package com.example.game.ui

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.engine.BattleRoyaleEngine
import com.example.game.map.ObstacleType
import com.example.game.model.CharacterPreset
import com.example.game.model.MatchPhase
import com.example.game.model.Player
import com.example.game.model.PlayerStance
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class ScreenProjectedPoint(
    val x: Float,
    val y: Float,
    val isVisible: Boolean
)

class Battle3DSurfaceView(
    context: Context,
    val renderer: Battle3DRenderer
) : GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}

class Battle3DRenderer(
    private val engine: BattleRoyaleEngine,
    @Volatile var graphicsQuality: String = "High"
) : GLSurfaceView.Renderer {

    companion object {
        private const val WORLD_SCALE = 0.1f // 5000 engine units -> 500m OpenGL world

        private const val VERTEX_SHADER = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            uniform vec3 uLightDir;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            varying float vLightIntensity;
            varying float vViewDist;
            void main() {
                vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
                vec3 worldNormal = normalize(mat3(uModelMatrix) * aNormal);
                float diff = max(dot(worldNormal, normalize(uLightDir)), 0.0);
                float hemi = 0.42 + 0.14 * worldNormal.y;
                vLightIntensity = clamp(hemi + diff * 0.68, 0.28, 1.25);
                vec4 clipPos = uMVPMatrix * vec4(aPosition, 1.0);
                vViewDist = clipPos.w;
                gl_Position = clipPos;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 uColor;
            uniform vec3 uFogColor;
            uniform float uFogMaxDist;
            uniform float uUnlit;
            varying float vLightIntensity;
            varying float vViewDist;
            void main() {
                vec3 lit = mix(uColor.rgb * vLightIntensity, uColor.rgb, uUnlit);
                float fogFactor = clamp((vViewDist - 40.0) / max(uFogMaxDist - 40.0, 20.0), 0.0, 0.88);
                vec3 finalRgb = mix(lit, uFogColor, fogFactor * (1.0 - uUnlit * 0.5));
                gl_FragColor = vec4(finalRgb, uColor.a);
            }
        """
    }

    private var program = 0
    private var aPositionHandle = 0
    private var aNormalHandle = 0
    private var uMVPMatrixHandle = 0
    private var uModelMatrixHandle = 0
    private var uLightDirHandle = 0
    private var uColorHandle = 0
    private var uFogColorHandle = 0
    private var uFogMaxDistHandle = 0
    private var uUnlitHandle = 0

    private lateinit var cubeBuffer: FloatBuffer

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Thread-safe copy of VP matrix for projecting 3D damage popups & enemy bars to Compose HUD
    private val lock = Any()
    private val sharedVpMatrix = FloatArray(16)
    @Volatile private var viewportWidth = 1280
    @Volatile private var viewportHeight = 720

    // Smoothed camera tracking state
    private var smoothCamX = 250f
    private var smoothCamY = 4f
    private var smoothCamZ = 250f
    private var smoothFov = 60f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val (skyR, skyG, skyB) = getSkyColor()
        GLES20.glClearColor(skyR, skyG, skyB, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        cubeBuffer = buildUnitCubeBuffer()

        val vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vs)
            GLES20.glAttachShader(it, fs)
            GLES20.glLinkProgram(it)
        }

        aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        aNormalHandle = GLES20.glGetAttribLocation(program, "aNormal")
        uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        uModelMatrixHandle = GLES20.glGetUniformLocation(program, "uModelMatrix")
        uLightDirHandle = GLES20.glGetUniformLocation(program, "uLightDir")
        uColorHandle = GLES20.glGetUniformLocation(program, "uColor")
        uFogColorHandle = GLES20.glGetUniformLocation(program, "uFogColor")
        uFogMaxDistHandle = GLES20.glGetUniformLocation(program, "uFogMaxDist")
        uUnlitHandle = GLES20.glGetUniformLocation(program, "uUnlit")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width.coerceAtLeast(1)
        viewportHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        val (skyR, skyG, skyB) = getSkyColor()
        GLES20.glClearColor(skyR, skyG, skyB, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        val state = engine.engineState.value
        val user = engine.userPlayer
        val activeTarget = if (state.isSpectating && state.spectatedPlayer != null) state.spectatedPlayer!! else user

        val qualityDrawDist = when (graphicsQuality.lowercase()) {
            "low", "smooth" -> 125f
            "medium", "balanced" -> 185f
            "high", "hd" -> 250f
            else -> 320f // Ultra / Ultra HD
        }
        val showShadows = !graphicsQuality.equals("Low", true) && !graphicsQuality.equals("Smooth", true)

        // Compute Camera & Projection
        val aspect = viewportWidth.toFloat() / viewportHeight.toFloat()
        val desiredFov = when {
            engine.isAimingDownSights -> {
                val mag = user.activeWeapon?.equippedScope?.magnification ?: 1.5f
                (58f / mag.coerceIn(1.2f, 4.2f)).coerceIn(14f, 48f)
            }
            user.isSprinting -> 66f
            state.phase == MatchPhase.SKYDIVING -> 68f
            else -> 58f
        }
        smoothFov += (desiredFov - smoothFov) * 0.18f
        Matrix.perspectiveM(projectionMatrix, 0, smoothFov, aspect, 0.4f, qualityDrawDist + 80f)

        val yawRad = Math.toRadians(engine.cameraYawDegrees.toDouble())
        val pitchRad = Math.toRadians(engine.cameraPitchDegrees.toDouble())
        val fx = cos(yawRad).toFloat()
        val fz = sin(yawRad).toFloat()
        val rx = -fz
        val rz = fx

        val targetGlX: Float
        val targetGlY: Float
        val targetGlZ: Float
        val camDist: Float
        val shoulderOffset: Float
        val camHeightOffset: Float

        when (state.phase) {
            MatchPhase.PLANE_FLIGHT, MatchPhase.WAITING_IN_LOBBY -> {
                targetGlX = state.planeX * WORLD_SCALE
                targetGlY = 60f
                targetGlZ = state.planeY * WORLD_SCALE
                camDist = 26f
                shoulderOffset = 0f
                camHeightOffset = 9f
            }
            MatchPhase.SKYDIVING -> {
                targetGlX = activeTarget.x * WORLD_SCALE
                targetGlY = (activeTarget.z * WORLD_SCALE).coerceAtLeast(1.5f)
                targetGlZ = activeTarget.y * WORLD_SCALE
                camDist = 11f
                shoulderOffset = 1.0f
                camHeightOffset = 4.5f
            }
            else -> {
                val crouchDip = if (activeTarget.stance == PlayerStance.CROUCH) -0.45f else if (activeTarget.stance == PlayerStance.PRONE) -0.95f else 0f
                targetGlX = activeTarget.x * WORLD_SCALE
                targetGlY = activeTarget.z * WORLD_SCALE + 1.65f + crouchDip
                targetGlZ = activeTarget.y * WORLD_SCALE
                camDist = if (activeTarget.vehicleId != null) 9.5f else if (engine.isAimingDownSights) 2.5f else 6.0f
                shoulderOffset = if (activeTarget.vehicleId != null) 0.4f else if (engine.isAimingDownSights) 0.72f else 1.25f
                camHeightOffset = if (activeTarget.vehicleId != null) 3.4f else 0.75f
            }
        }

        val desiredCamX = targetGlX - fx * camDist * cos(pitchRad).toFloat() + rx * shoulderOffset
        val desiredCamY = (targetGlY + camHeightOffset + camDist * sin(pitchRad).toFloat()).coerceAtLeast(1.1f)
        val desiredCamZ = targetGlZ - fz * camDist * cos(pitchRad).toFloat() + rz * shoulderOffset

        smoothCamX += (desiredCamX - smoothCamX) * 0.35f
        smoothCamY += (desiredCamY - smoothCamY) * 0.35f
        smoothCamZ += (desiredCamZ - smoothCamZ) * 0.35f

        val lookX = targetGlX + fx * 22f + rx * shoulderOffset * 0.55f
        val lookY = (targetGlY - sin(pitchRad).toFloat() * 14f).coerceAtLeast(-12f)
        val lookZ = targetGlZ + fz * 22f + rz * shoulderOffset * 0.55f

        Matrix.setLookAtM(
            viewMatrix, 0,
            smoothCamX, smoothCamY, smoothCamZ,
            lookX, lookY, lookZ,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        synchronized(lock) {
            System.arraycopy(vpMatrix, 0, sharedVpMatrix, 0, 16)
        }

        // Bind shared cube vertex attributes once
        cubeBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 24, cubeBuffer)

        cubeBuffer.position(3)
        GLES20.glEnableVertexAttribArray(aNormalHandle)
        GLES20.glVertexAttribPointer(aNormalHandle, 3, GLES20.GL_FLOAT, false, 24, cubeBuffer)

        GLES20.glUniform3f(uLightDirHandle, 0.55f, 0.85f, 0.35f)
        GLES20.glUniform3f(uFogColorHandle, skyR, skyG, skyB)
        GLES20.glUniform1f(uFogMaxDistHandle, qualityDrawDist)

        // 1. Ground Terrain Plane + Roads + Hills
        renderTerrain(targetGlX, targetGlZ, qualityDrawDist)

        // 2. Water Rivers
        engine.mapConfig.waterBodies.forEach { wb ->
            val cx = (wb.x + wb.width * 0.5f) * WORLD_SCALE
            val cz = (wb.y + wb.height * 0.5f) * WORLD_SCALE
            drawBox(cx, 0.04f, cz, wb.width * WORLD_SCALE, 0.08f, wb.height * WORLD_SCALE, 0f, 0.08f, 0.36f, 0.72f, 0.88f)
        }

        // 3. 3D Buildings (with roofs, window trims & interior contrast)
        engine.mapConfig.buildings.forEach { b ->
            val bx = (b.x + b.width * 0.5f) * WORLD_SCALE
            val bz = (b.y + b.height * 0.5f) * WORLD_SCALE
            val dx = bx - targetGlX
            val dz = bz - targetGlZ
            if (dx * dx + dz * dz < qualityDrawDist * qualityDrawDist) {
                val bw = b.width * WORLD_SCALE
                val bd = b.height * WORLD_SCALE
                val bh = if (bw > 9f) 7.2f else 5.2f
                // Foundation slab
                drawBox(bx, 0.15f, bz, bw + 0.8f, 0.3f, bd + 0.8f, 0f, 0.22f, 0.25f, 0.28f, 1f)
                // Main structure
                drawBox(bx, bh * 0.5f + 0.3f, bz, bw, bh, bd, 0f, 0.42f, 0.46f, 0.50f, 1f)
                // Roof overhang
                drawBox(bx, bh + 0.55f, bz, bw + 0.7f, 0.5f, bd + 0.7f, 0f, 0.24f, 0.20f, 0.18f, 1f)
                // Door / window dark accents
                drawBox(bx, 1.4f, bz + bd * 0.5f + 0.05f, 1.5f, 2.5f, 0.12f, 0f, 0.10f, 0.12f, 0.15f, 1f)
                drawBox(bx - bw * 0.25f, bh * 0.65f, bz + bd * 0.5f + 0.05f, 1.4f, 1.1f, 0.12f, 0f, 0.55f, 0.78f, 0.92f, 1f, unlit = 0.3f)
                drawBox(bx + bw * 0.25f, bh * 0.65f, bz + bd * 0.5f + 0.05f, 1.4f, 1.1f, 0.12f, 0f, 0.55f, 0.78f, 0.92f, 1f, unlit = 0.3f)
            }
        }

        // 4. 3D Trees, Rocks, Crates
        engine.mapConfig.obstacles.forEach { obs ->
            val ox = obs.x * WORLD_SCALE
            val oz = obs.y * WORLD_SCALE
            val dx = ox - targetGlX
            val dz = oz - targetGlZ
            if (dx * dx + dz * dz < qualityDrawDist * qualityDrawDist) {
                val r = obs.radius * WORLD_SCALE
                when (obs.type) {
                    ObstacleType.TREE -> {
                        // Trunk
                        drawBox(ox, 1.8f, oz, 0.65f, 3.6f, 0.65f, 0f, 0.36f, 0.24f, 0.14f, 1f)
                        // Lower & upper canopy
                        drawBox(ox, 4.1f, oz, r * 1.5f, 2.2f, r * 1.5f, 15f, 0.14f, 0.42f, 0.16f, 1f)
                        drawBox(ox, 5.6f, oz, r * 1.0f, 1.6f, r * 1.0f, 45f, 0.18f, 0.50f, 0.20f, 1f)
                    }
                    ObstacleType.ROCK -> {
                        drawBox(ox, r * 0.45f, oz, r * 1.3f, r * 0.9f, r * 1.1f, 25f, 0.44f, 0.46f, 0.48f, 1f)
                    }
                    ObstacleType.CRATE, ObstacleType.SANDBAGS -> {
                        drawBox(ox, 0.75f, oz, 1.6f, 1.5f, 1.6f, 0f, 0.52f, 0.40f, 0.22f, 1f)
                    }
                }
            }
        }

        // 4b. Central Tactical Plaza Props (Shipping Container Cover, Crates, Barrels, Cooling Tower)
        if ((targetGlX - 250f) * (targetGlX - 250f) + (targetGlZ - 250f) * (targetGlZ - 250f) < qualityDrawDist * qualityDrawDist) {
            // Blue steel shipping container
            drawBox(264f, 1.7f, 240f, 7.6f, 3.4f, 3.2f, -15f, 0.08f, 0.42f, 0.72f, 1f)
            // Stacked wooden supply crates
            drawBox(260f, 0.85f, 244f, 1.8f, 1.7f, 1.8f, 5f, 0.60f, 0.44f, 0.25f, 1f)
            drawBox(262f, 0.85f, 244.5f, 1.8f, 1.7f, 1.8f, 12f, 0.56f, 0.40f, 0.22f, 1f)
            drawBox(261f, 2.55f, 244.2f, 1.7f, 1.7f, 1.7f, -8f, 0.62f, 0.46f, 0.26f, 1f)
            // Red hazard fuel drums
            drawBox(268.5f, 0.85f, 243f, 1.0f, 1.7f, 1.0f, 0f, 0.84f, 0.14f, 0.14f, 1f)
            drawBox(242f, 0.85f, 236f, 1.0f, 1.7f, 1.0f, 0f, 0.84f, 0.14f, 0.14f, 1f)
            // Distant Industrial Cooling Tower Landmark
            drawBox(295f, 11f, 195f, 11f, 22f, 11f, 22f, 0.52f, 0.56f, 0.60f, 1f)
            drawBox(295f, 22.5f, 195f, 9.5f, 2.2f, 9.5f, 22f, 0.60f, 0.64f, 0.68f, 1f)
        }

        // 5. Loot Beacons
        state.groundLootSnapshot.forEach { loot ->
            val lx = loot.x * WORLD_SCALE
            val lz = loot.y * WORLD_SCALE
            val dx = lx - targetGlX
            val dz = lz - targetGlZ
            if (dx * dx + dz * dz < 110f * 110f) {
                val (r, g, b) = when {
                    loot.weapon != null -> Triple(1.0f, 0.48f, 0.0f)
                    loot.armorLevel > 0 -> Triple(0.0f, 0.72f, 1.0f)
                    loot.consumable != null -> Triple(0.0f, 0.90f, 0.45f)
                    else -> Triple(1.0f, 0.84f, 0.0f)
                }
                drawBox(lx, 0.35f, lz, 0.85f, 0.45f, 0.85f, 20f, r, g, b, 0.95f, unlit = 0.6f)
                drawBox(lx, 1.5f, lz, 0.12f, 2.4f, 0.12f, 0f, r, g, b, 0.35f, unlit = 1.0f)
            }
        }

        // 6. Vehicles
        state.vehiclesSnapshot.forEach { v ->
            val vx = v.x * WORLD_SCALE
            val vz = v.y * WORLD_SCALE
            val dx = vx - targetGlX
            val dz = vz - targetGlZ
            if (dx * dx + dz * dz < qualityDrawDist * qualityDrawDist) {
                val rot = -v.rotationAngle
                val chassisR = if (v.isDestroyed) 0.25f else 0.12f
                val chassisG = if (v.isDestroyed) 0.25f else 0.42f
                val chassisB = if (v.isDestroyed) 0.25f else 0.78f
                drawBox(vx, 0.75f, vz, 4.4f, 0.95f, 2.2f, rot, chassisR, chassisG, chassisB, 1f)
                drawBox(vx, 1.55f, vz, 2.2f, 0.75f, 1.95f, rot, 0.55f, 0.80f, 0.96f, 0.92f)
            }
        }

        // 7. Deployable Energy Shields
        state.deployableShields.forEach { shield ->
            val sx = shield.x * WORLD_SCALE
            val sz = shield.y * WORLD_SCALE
            drawBox(sx, 1.35f, sz, 0.35f, 2.6f, 4.2f, -shield.angleDegrees, 0.05f, 0.78f, 1.0f, 0.62f, unlit = 0.85f)
            drawBox(sx, 2.7f, sz, 0.4f, 0.15f, 4.3f, -shield.angleDegrees, 0.7f, 0.95f, 1.0f, 0.9f, unlit = 1f)
        }

        // 8. Airdrops with Red Beacon Beam
        state.airdrops.forEach { drop ->
            val ax = drop.x * WORLD_SCALE
            val az = drop.y * WORLD_SCALE
            val ay = (drop.altitude * WORLD_SCALE).coerceAtLeast(1.1f)
            drawBox(ax, ay, az, 2.2f, 2.2f, 2.2f, 0f, 0.82f, 0.15f, 0.15f, 1f)
            drawBox(ax, ay + 1.2f, az, 2.3f, 0.45f, 2.3f, 0f, 0.1f, 0.42f, 0.85f, 1f)
            drawBox(ax, ay + 25f, az, 0.35f, 50f, 0.35f, 0f, 1.0f, 0.15f, 0.15f, 0.32f, unlit = 1f)
        }

        // 9. Plane during Flight Phase
        if (state.phase == MatchPhase.PLANE_FLIGHT || state.phase == MatchPhase.WAITING_IN_LOBBY) {
            val px = state.planeX * WORLD_SCALE
            val pz = state.planeY * WORLD_SCALE
            drawBox(px, 60f, pz, 14f, 3.5f, 3.6f, -45f, 0.42f, 0.48f, 0.44f, 1f)
            drawBox(px, 60.5f, pz, 3.5f, 0.6f, 18f, -45f, 0.35f, 0.40f, 0.36f, 1f)
        }

        // 10. Articulated 3D Players & AI Bots
        val radarActive = state.radarScanActiveSec > 0f
        state.playersSnapshot.filter { it.isAlive }.forEach { player ->
            val px = player.x * WORLD_SCALE
            val pz = player.y * WORLD_SCALE
            val dx = px - targetGlX
            val dz = pz - targetGlZ
            if (dx * dx + dz * dz < qualityDrawDist * qualityDrawDist) {
                renderSoldier3D(player, user.teamId, showShadows, radarActive)
            }
        }

        // 11. 3D Bullet Tracers
        state.bulletsSnapshot.forEach { b ->
            val bx = b.x * WORLD_SCALE
            val bz = b.y * WORLD_SCALE
            val angleDeg = -Math.toDegrees(kotlin.math.atan2(b.vy.toDouble(), b.vx.toDouble())).toFloat()
            val len = if (b.isSniper) 2.2f else 1.35f
            drawBox(bx, 1.45f, bz, len, 0.11f, 0.11f, angleDeg, 1.0f, 0.85f, 0.2f, 1f, unlit = 1f)
        }

        // 12. Smoke Clouds & Explosion Shockwaves
        state.smokeClouds.forEach { smoke ->
            val sx = smoke.x * WORLD_SCALE
            val sz = smoke.y * WORLD_SCALE
            val r = smoke.radius * WORLD_SCALE
            val alpha = (smoke.remainingDurationSec / 25f).coerceIn(0.2f, 0.65f)
            drawBox(sx, r * 0.45f, sz, r * 1.5f, r * 0.9f, r * 1.5f, 20f, 0.78f, 0.82f, 0.86f, alpha, unlit = 0.5f)
        }
        state.explosions.forEach { exp ->
            val ex = exp.x * WORLD_SCALE
            val ez = exp.y * WORLD_SCALE
            val p = (exp.elapsedSec / exp.durationSec).coerceIn(0f, 1f)
            val rad = exp.maxRadius * WORLD_SCALE * p
            drawBox(ex, rad * 0.4f, ez, rad * 1.4f, rad * 1.1f, rad * 1.4f, p * 90f, 1.0f, 0.45f, 0.05f, (1f - p) * 0.75f, unlit = 1f)
        }

        // 13. Safe Zone Electric Blue Wall Indicator Ring Segments
        renderZoneRing(
            cx = state.safeZone.currentBlueCenterX * WORLD_SCALE,
            cz = state.safeZone.currentBlueCenterY * WORLD_SCALE,
            radius = state.safeZone.currentBlueRadius * WORLD_SCALE
        )
    }

    private fun renderTerrain(focusX: Float, focusZ: Float, drawDist: Float) {
        val (gr, gg, gb) = when (engine.mapConfig.id) {
            "kalahari_dunes" -> Triple(0.68f, 0.54f, 0.33f)
            "snow_mountain" -> Triple(0.80f, 0.86f, 0.92f)
            "urban_city" -> Triple(0.24f, 0.27f, 0.30f)
            "jungle_battleground" -> Triple(0.14f, 0.32f, 0.16f)
            else -> Triple(0.23f, 0.35f, 0.19f)
        }
        // Main Ground Slab
        drawBox(250f, -0.25f, 250f, 540f, 0.5f, 540f, 0f, gr, gg, gb, 1f)

        // Major Paved Highway Roads (North-South & East-West intersecting central town)
        drawBox(250f, 0.02f, 250f, 500f, 0.04f, 11f, 0f, 0.18f, 0.19f, 0.21f, 1f)
        drawBox(250f, 0.02f, 250f, 11f, 0.04f, 500f, 0f, 0.18f, 0.19f, 0.21f, 1f)
        // Diagonal Perimeter Road
        drawBox(250f, 0.025f, 250f, 440f, 0.04f, 7f, 45f, 0.20f, 0.21f, 0.23f, 1f)

        // Dashed Yellow Center Stripes near player
        val snapX = ((focusX / 12f).toInt() * 12f).coerceIn(20f, 480f)
        val snapZ = ((focusZ / 12f).toInt() * 12f).coerceIn(20f, 480f)
        for (i in -6..6) {
            val sx = snapX + i * 12f
            val sz = snapZ + i * 12f
            if (abs(250f - focusZ) < drawDist) {
                drawBox(sx, 0.045f, 250f, 4.5f, 0.02f, 0.45f, 0f, 0.92f, 0.78f, 0.12f, 1f, unlit = 0.5f)
            }
            if (abs(250f - focusX) < drawDist) {
                drawBox(250f, 0.045f, sz, 0.45f, 0.02f, 4.5f, 0f, 0.92f, 0.78f, 0.12f, 1f, unlit = 0.5f)
            }
        }

        // Rolling Hills / Elevated Tactical Ridges
        val hillCoords = listOf(
            Triple(180f, 190f, 22f),
            Triple(310f, 210f, 26f),
            Triple(210f, 315f, 24f),
            Triple(330f, 330f, 28f),
            Triple(150f, 260f, 20f)
        )
        hillCoords.forEach { (hx, hz, hw) ->
            val dx = hx - focusX
            val dz = hz - focusZ
            if (dx * dx + dz * dz < drawDist * drawDist) {
                drawBox(hx, 0.8f, hz, hw, 1.6f, hw * 0.8f, 18f, gr * 0.92f, gg * 0.95f, gb * 0.92f, 1f)
                drawBox(hx + 2f, 1.9f, hz - 1f, hw * 0.55f, 1.4f, hw * 0.45f, 32f, gr * 0.85f, gg * 0.9f, gb * 0.85f, 1f)
            }
        }
    }

    private fun renderSoldier3D(player: Player, userTeamId: Int, showShadow: Boolean, radarActive: Boolean) {
        val px = player.x * WORLD_SCALE
        val py = player.z * WORLD_SCALE
        val pz = player.y * WORLD_SCALE
        val yaw = -player.angle

        val preset = CharacterPreset.fromId(player.characterPresetId)
        val isTeammate = player.teamId == userTeamId
        val (armorR, armorG, armorB) = if (player.isUser || isTeammate) preset.armorRgb else Triple(0.55f, 0.12f, 0.12f)
        val (accR, accG, accB) = if (player.isUser || isTeammate) preset.accentRgb else Triple(0.95f, 0.25f, 0.15f)
        val (visR, visG, visB) = if (player.isUser || isTeammate) preset.visorRgb else Triple(1.0f, 0.15f, 0.15f)

        val crouchScale = when (player.stance) {
            PlayerStance.CROUCH -> 0.72f
            PlayerStance.PRONE -> 0.40f
            else -> 1.0f
        }
        val stride = if (player.currentSpeed > 15f || !player.isUser) sin(player.animWalkPhase) * 0.35f else 0f

        // Ground Drop Shadow
        if (showShadow) {
            drawBox(px, 0.05f, pz, 1.2f, 0.04f, 1.2f, 0f, 0.05f, 0.06f, 0.08f, 0.45f, unlit = 1f)
        }

        val rad = Math.toRadians(player.angle.toDouble())
        val fx = cos(rad).toFloat()
        val fz = sin(rad).toFloat()
        val rx = -fz
        val rz = fx

        if (player.stance == PlayerStance.PRONE) {
            // Prone horizontal soldier pose
            drawBox(px - fx * 0.55f + rx * 0.16f, py + 0.18f, pz - fz * 0.55f + rz * 0.16f, 0.85f, 0.24f, 0.26f, yaw, 0.14f, 0.15f, 0.18f, 1f)
            drawBox(px - fx * 0.55f - rx * 0.16f, py + 0.18f, pz - fz * 0.55f - rz * 0.16f, 0.85f, 0.24f, 0.26f, yaw, 0.14f, 0.15f, 0.18f, 1f)
            drawBox(px + fx * 0.10f, py + 0.28f, pz + fz * 0.10f, 0.90f, 0.36f, 0.66f, yaw, armorR, armorG, armorB, 1f)
            drawBox(px + fx * 0.12f, py + 0.36f, pz + fz * 0.12f, 0.60f, 0.22f, 0.68f, yaw, accR, accG, accB, 1f, unlit = 0.35f)
            drawBox(px + fx * 0.72f, py + 0.36f, pz + fz * 0.72f, 0.42f, 0.38f, 0.42f, yaw, armorR * 0.8f, armorG * 0.8f, armorB * 0.8f, 1f)
            drawBox(px + fx * 0.92f, py + 0.38f, pz + fz * 0.92f, 0.14f, 0.14f, 0.36f, yaw, visR, visG, visB, 1f, unlit = 0.9f)
            if (player.activeWeapon != null) {
                drawBox(px + fx * 1.15f + rx * 0.24f, py + 0.28f, pz + fz * 1.15f + rz * 0.24f, 1.05f, 0.15f, 0.14f, yaw, 0.18f, 0.20f, 0.22f, 1f)
                if (System.currentTimeMillis() - player.lastFireTimestampMs < 95L) {
                    drawBox(px + fx * 1.75f + rx * 0.24f, py + 0.28f, pz + fz * 1.75f + rz * 0.24f, 0.45f, 0.35f, 0.35f, yaw + 45f, 1.0f, 0.75f, 0.1f, 0.92f, unlit = 1f)
                }
            }
            return
        }

        // Legs (animated stride forward/back)
        val legH = 0.85f * crouchScale
        drawBox(
            px + rx * 0.20f + fx * stride, py + legH * 0.5f, pz + rz * 0.20f + fz * stride,
            0.28f, legH, 0.30f, yaw, 0.14f, 0.15f, 0.18f, 1f
        )
        drawBox(
            px - rx * 0.20f - fx * stride, py + legH * 0.5f, pz - rz * 0.20f - fz * stride,
            0.28f, legH, 0.30f, yaw, 0.14f, 0.15f, 0.18f, 1f
        )

        // Torso & Glowing Jacket/Armor Accent
        val torsoY = py + legH + 0.42f * crouchScale
        drawBox(px, torsoY, pz, 0.50f, 0.84f * crouchScale, 0.74f, yaw, armorR, armorG, armorB, 1f)
        drawBox(px, torsoY + 0.06f, pz, 0.54f, 0.34f * crouchScale, 0.78f, yaw, accR, accG, accB, 1f, unlit = 0.35f)

        // Head & Visor Helmet
        val headY = py + legH + 0.84f * crouchScale + 0.26f
        drawBox(px, headY, pz, 0.42f, 0.44f, 0.42f, yaw, armorR * 0.8f, armorG * 0.8f, armorB * 0.8f, 1f)
        drawBox(px + fx * 0.16f, headY + 0.03f, pz + fz * 0.16f, 0.16f, 0.14f, 0.36f, yaw, visR, visG, visB, 1f, unlit = 0.9f)

        // Equipped Weapon & Muzzle Flash
        if (player.activeWeapon != null) {
            val gunX = px + fx * 0.55f + rx * 0.32f
            val gunZ = pz + fz * 0.55f + rz * 0.32f
            drawBox(gunX, torsoY + 0.10f, gunZ, 1.15f, 0.16f, 0.14f, yaw, 0.18f, 0.20f, 0.22f, 1f)

            val justFired = System.currentTimeMillis() - player.lastFireTimestampMs < 95L
            if (justFired) {
                val flashX = px + fx * 1.25f + rx * 0.32f
                val flashZ = pz + fz * 1.25f + rz * 0.32f
                drawBox(flashX, torsoY + 0.10f, flashZ, 0.45f, 0.35f, 0.35f, yaw + 45f, 1.0f, 0.75f, 0.1f, 0.92f, unlit = 1f)
            }
        }

        // Radar Pulse Highlight Ring on Enemies
        if (!isTeammate && radarActive) {
            drawBox(px, py + 0.12f, pz, 1.6f, 0.15f, 1.6f, yaw, 1.0f, 0.15f, 0.15f, 0.85f, unlit = 1f)
        }

        // Skydiving Parachute Canopy
        if (player.z > 15f) {
            drawBox(px, py + 4.4f, pz, 3.6f, 0.45f, 2.4f, yaw, 1.0f, 0.48f, 0.0f, 0.9f, unlit = 0.25f)
        }
    }

    private fun renderZoneRing(cx: Float, cz: Float, radius: Float) {
        val segments = 24
        val step = (2.0 * Math.PI) / segments
        for (i in 0 until segments) {
            val angle = i * step
            val sx = cx + (cos(angle) * radius).toFloat()
            val sz = cz + (sin(angle) * radius).toFloat()
            val rotDeg = -Math.toDegrees(angle).toFloat() + 90f
            drawBox(sx, 7f, sz, (2f * Math.PI.toFloat() * radius) / segments, 14f, 0.45f, rotDeg, 0.0f, 0.85f, 1.0f, 0.22f, unlit = 1f)
        }
    }

    private fun drawBox(
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        rotYDeg: Float,
        r: Float, g: Float, b: Float, a: Float,
        unlit: Float = 0f
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        if (rotYDeg != 0f) {
            Matrix.rotateM(modelMatrix, 0, rotYDeg, 0f, 1f, 0f)
        }
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelMatrixHandle, 1, false, modelMatrix, 0)
        GLES20.glUniform4f(uColorHandle, r, g, b, a)
        GLES20.glUniform1f(uUnlitHandle, unlit)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36)
    }

    fun projectWorldToScreen(worldX: Float, worldY: Float, worldZ: Float): ScreenProjectedPoint {
        val vpCopy = FloatArray(16)
        synchronized(lock) {
            System.arraycopy(sharedVpMatrix, 0, vpCopy, 0, 16)
        }
        val vec = floatArrayOf(worldX * WORLD_SCALE, worldZ * WORLD_SCALE, worldY * WORLD_SCALE, 1f)
        val res = FloatArray(4)
        Matrix.multiplyMV(res, 0, vpCopy, 0, vec, 0)
        val w = res[3]
        if (w <= 0.1f) return ScreenProjectedPoint(0f, 0f, false)
        val ndcX = res[0] / w
        val ndcY = res[1] / w
        if (ndcX < -1.1f || ndcX > 1.1f || ndcY < -1.1f || ndcY > 1.1f) {
            return ScreenProjectedPoint(0f, 0f, false)
        }
        val sx = (ndcX * 0.5f + 0.5f) * viewportWidth
        val sy = (1f - (ndcY * 0.5f + 0.5f)) * viewportHeight
        return ScreenProjectedPoint(sx, sy, true)
    }

    private fun getSkyColor(): Triple<Float, Float, Float> = when (engine.mapConfig.id) {
        "kalahari_dunes" -> Triple(0.84f, 0.62f, 0.36f)
        "snow_mountain" -> Triple(0.56f, 0.72f, 0.88f)
        "urban_city" -> Triple(0.14f, 0.18f, 0.30f)
        "jungle_battleground" -> Triple(0.28f, 0.45f, 0.40f)
        else -> Triple(0.42f, 0.64f, 0.85f)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun buildUnitCubeBuffer(): FloatBuffer {
        // 36 vertices * (3 pos + 3 normal) = 216 floats
        val v = floatArrayOf(
            // Front (+Z)
            -0.5f, -0.5f,  0.5f,  0f, 0f, 1f,
             0.5f, -0.5f,  0.5f,  0f, 0f, 1f,
             0.5f,  0.5f,  0.5f,  0f, 0f, 1f,
            -0.5f, -0.5f,  0.5f,  0f, 0f, 1f,
             0.5f,  0.5f,  0.5f,  0f, 0f, 1f,
            -0.5f,  0.5f,  0.5f,  0f, 0f, 1f,
            // Back (-Z)
             0.5f, -0.5f, -0.5f,  0f, 0f, -1f,
            -0.5f, -0.5f, -0.5f,  0f, 0f, -1f,
            -0.5f,  0.5f, -0.5f,  0f, 0f, -1f,
             0.5f, -0.5f, -0.5f,  0f, 0f, -1f,
            -0.5f,  0.5f, -0.5f,  0f, 0f, -1f,
             0.5f,  0.5f, -0.5f,  0f, 0f, -1f,
            // Left (-X)
            -0.5f, -0.5f, -0.5f, -1f, 0f, 0f,
            -0.5f, -0.5f,  0.5f, -1f, 0f, 0f,
            -0.5f,  0.5f,  0.5f, -1f, 0f, 0f,
            -0.5f, -0.5f, -0.5f, -1f, 0f, 0f,
            -0.5f,  0.5f,  0.5f, -1f, 0f, 0f,
            -0.5f,  0.5f, -0.5f, -1f, 0f, 0f,
            // Right (+X)
             0.5f, -0.5f,  0.5f,  1f, 0f, 0f,
             0.5f, -0.5f, -0.5f,  1f, 0f, 0f,
             0.5f,  0.5f, -0.5f,  1f, 0f, 0f,
             0.5f, -0.5f,  0.5f,  1f, 0f, 0f,
             0.5f,  0.5f, -0.5f,  1f, 0f, 0f,
             0.5f,  0.5f,  0.5f,  1f, 0f, 0f,
            // Top (+Y)
            -0.5f,  0.5f,  0.5f,  0f, 1f, 0f,
             0.5f,  0.5f,  0.5f,  0f, 1f, 0f,
             0.5f,  0.5f, -0.5f,  0f, 1f, 0f,
            -0.5f,  0.5f,  0.5f,  0f, 1f, 0f,
             0.5f,  0.5f, -0.5f,  0f, 1f, 0f,
            -0.5f,  0.5f, -0.5f,  0f, 1f, 0f,
            // Bottom (-Y)
            -0.5f, -0.5f, -0.5f,  0f, -1f, 0f,
             0.5f, -0.5f, -0.5f,  0f, -1f, 0f,
             0.5f, -0.5f,  0.5f,  0f, -1f, 0f,
            -0.5f, -0.5f, -0.5f,  0f, -1f, 0f,
             0.5f, -0.5f,  0.5f,  0f, -1f, 0f,
            -0.5f, -0.5f,  0.5f,  0f, -1f, 0f
        )
        return ByteBuffer.allocateDirect(v.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(v)
                position(0)
            }
    }
}
