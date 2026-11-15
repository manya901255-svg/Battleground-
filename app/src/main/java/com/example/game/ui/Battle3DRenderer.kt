package com.example.game.ui

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.engine.BattleRoyaleEngine
import com.example.game.map.ObstacleType
import com.example.game.model.CharacterPreset
import com.example.game.model.GameModeType
import com.example.game.model.MatchPhase
import com.example.game.model.PetPreset
import com.example.game.model.Player
import com.example.game.model.PlayerStance
import com.example.game.model.VehicleType
import com.example.game.model.WeatherType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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

        private const val SURFACE_SOLID = 0
        private const val SURFACE_TERRAIN = 1
        private const val SURFACE_RIBBED = 2
        private const val SURFACE_CONCRETE = 3

        private const val VERTEX_SHADER = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            uniform vec3 uLightDir;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            varying float vLightIntensity;
            varying float vViewDist;
            varying vec3 vWorldPos;
            varying vec3 vWorldNormal;
            void main() {
                vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
                vec3 worldNormal = normalize(mat3(uModelMatrix) * aNormal);
                float diff = max(dot(worldNormal, normalize(uLightDir)), 0.0);
                float hemi = 0.45 + 0.16 * worldNormal.y;
                vLightIntensity = clamp(hemi + diff * 0.72, 0.30, 1.30);
                vec4 clipPos = uMVPMatrix * vec4(aPosition, 1.0);
                vViewDist = clipPos.w;
                vWorldPos = worldPos.xyz;
                vWorldNormal = worldNormal;
                gl_Position = clipPos;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 uColor;
            uniform vec3 uFogColor;
            uniform float uFogMaxDist;
            uniform float uUnlit;
            uniform int uSurfaceType;
            varying float vLightIntensity;
            varying float vViewDist;
            varying vec3 vWorldPos;
            varying vec3 vWorldNormal;
            void main() {
                vec3 baseRgb = uColor.rgb;
                if (uSurfaceType == 1) {
                    // Rich procedural battleground grass / soil pattern
                    float macro = sin(vWorldPos.x * 0.09) * cos(vWorldPos.z * 0.09);
                    float tufts = sin(vWorldPos.x * 0.85 + sin(vWorldPos.z * 0.7)) * cos(vWorldPos.z * 0.92);
                    float fine = fract(sin(dot(floor(vWorldPos.xz * 2.2), vec2(12.9898, 78.233))) * 43758.5453);
                    vec3 dirtPatch = baseRgb * vec3(1.18, 0.94, 0.72);
                    baseRgb = mix(baseRgb, dirtPatch, clamp(macro * 0.35 + 0.20, 0.0, 0.45));
                    baseRgb *= (0.88 + tufts * 0.10 + fine * 0.12);
                } else if (uSurfaceType == 2) {
                    // Corrugated steel container / plank crate ribs
                    float stripe = sin((vWorldPos.x + vWorldPos.z) * 7.5 + vWorldPos.y * 1.5);
                    baseRgb *= (0.90 + 0.12 * stripe);
                } else if (uSurfaceType == 3) {
                    // Weathered concrete plaster blocks
                    float block = sin(vWorldPos.y * 2.6) * 0.04 + sin((vWorldPos.x + vWorldPos.z) * 1.4) * 0.04;
                    baseRgb *= (0.96 + block);
                }
                vec3 lit = mix(baseRgb * vLightIntensity, baseRgb, uUnlit);
                float fogFactor = clamp((vViewDist - 48.0) / max(uFogMaxDist - 48.0, 24.0), 0.0, 0.86);
                vec3 finalRgb = mix(lit, uFogColor, fogFactor * (1.0 - uUnlit * 0.55));
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
    private var uSurfaceTypeHandle = 0

    private lateinit var cubeBuffer: FloatBuffer

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private val lock = Any()
    private val sharedVpMatrix = FloatArray(16)
    @Volatile private var viewportWidth = 1280
    @Volatile private var viewportHeight = 720

    private var smoothCamX = 250f
    private var smoothCamY = 4f
    private var smoothCamZ = 250f
    private var smoothFov = 58f
    private var frameTimeSec = 0f

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
        uSurfaceTypeHandle = GLES20.glGetUniformLocation(program, "uSurfaceType")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width.coerceAtLeast(1)
        viewportHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        frameTimeSec += 0.016f
        val (skyR, skyG, skyB) = getSkyColor()
        GLES20.glClearColor(skyR, skyG, skyB, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        val state = engine.engineState.value
        val user = engine.userPlayer
        val activeTarget = if (state.isSpectating && state.spectatedPlayer != null) state.spectatedPlayer else user

        val qualityDrawDist = when (graphicsQuality.lowercase()) {
            "low", "smooth" -> 135f
            "medium", "balanced" -> 195f
            "high", "hd" -> 265f
            else -> 340f
        }
        val showShadows = !graphicsQuality.equals("Low", true) && !graphicsQuality.equals("Smooth", true)

        val aspect = viewportWidth.toFloat() / viewportHeight.toFloat()
        val desiredFov = when {
            engine.isAimingDownSights -> {
                val mag = user.activeWeapon?.equippedScope?.magnification ?: 1.5f
                (56f / mag.coerceIn(1.25f, 4.5f)).coerceIn(14f, 44f)
            }
            user.isSprinting -> 65f
            state.phase == MatchPhase.SKYDIVING -> 68f
            else -> 57f
        }
        smoothFov += (desiredFov - smoothFov) * 0.20f
        Matrix.perspectiveM(projectionMatrix, 0, smoothFov, aspect, 0.4f, qualityDrawDist + 110f)

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
                camDist = if (activeTarget.vehicleId != null) 9.6f else if (engine.isAimingDownSights) 2.2f else 5.8f
                shoulderOffset = if (activeTarget.vehicleId != null) 0.4f else if (engine.isAimingDownSights) 0.68f else 1.22f
                camHeightOffset = if (activeTarget.vehicleId != null) 3.4f else 0.72f
            }
        }

        val desiredCamX = targetGlX - fx * camDist * cos(pitchRad).toFloat() + rx * shoulderOffset
        val desiredCamY = (targetGlY + camHeightOffset + camDist * sin(pitchRad).toFloat()).coerceAtLeast(1.1f)
        val desiredCamZ = targetGlZ - fz * camDist * cos(pitchRad).toFloat() + rz * shoulderOffset

        smoothCamX += (desiredCamX - smoothCamX) * 0.35f
        smoothCamY += (desiredCamY - smoothCamY) * 0.35f
        smoothCamZ += (desiredCamZ - smoothCamZ) * 0.35f

        val lookX = targetGlX + fx * 24f + rx * shoulderOffset * 0.52f
        val lookY = (targetGlY - sin(pitchRad).toFloat() * 14f).coerceAtLeast(-12f)
        val lookZ = targetGlZ + fz * 24f + rz * shoulderOffset * 0.52f

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

        cubeBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 24, cubeBuffer)

        cubeBuffer.position(3)
        GLES20.glEnableVertexAttribArray(aNormalHandle)
        GLES20.glVertexAttribPointer(aNormalHandle, 3, GLES20.GL_FLOAT, false, 24, cubeBuffer)

        val (fogR, fogG, fogB) = when (engine.weatherType) {
            WeatherType.CLEAR -> Triple(skyR, skyG, skyB)
            WeatherType.SUNSET_STORM -> Triple(0.68f, 0.36f, 0.24f)
            WeatherType.DENSE_FOG -> Triple(0.62f, 0.68f, 0.72f)
            WeatherType.NIGHT_OPS -> Triple(0.06f, 0.09f, 0.16f)
        }
        val fogDist = when (engine.weatherType) {
            WeatherType.DENSE_FOG -> qualityDrawDist * 0.42f
            WeatherType.SUNSET_STORM -> qualityDrawDist * 0.72f
            WeatherType.NIGHT_OPS -> qualityDrawDist * 0.65f
            WeatherType.CLEAR -> qualityDrawDist
        }

        GLES20.glUniform3f(uLightDirHandle, 0.55f, 0.85f, 0.35f)
        GLES20.glUniform3f(uFogColorHandle, fogR, fogG, fogB)
        GLES20.glUniform1f(uFogMaxDistHandle, fogDist)

        // 0. Sun, Ambient Sky Clouds & Ambient Supply Plane + Parachutists
        renderSkyDetails(targetGlX, targetGlZ)

        // 1. Textured Procedural Ground Terrain + Roads + Hills
        renderTerrain(targetGlX, targetGlZ, qualityDrawDist)

        // 2. Water Rivers
        engine.mapConfig.waterBodies.forEach { wb ->
            val cx = (wb.x + wb.width * 0.5f) * WORLD_SCALE
            val cz = (wb.y + wb.height * 0.5f) * WORLD_SCALE
            drawBox(cx, 0.04f, cz, wb.width * WORLD_SCALE, 0.08f, wb.height * WORLD_SCALE, 0f, 0.08f, 0.42f, 0.78f, 0.88f, unlit = 0.25f)
        }

        // 3. Detailed Battleground Buildings (Concrete, Blue Window Frames, Roof Overhang & Parapets)
        engine.mapConfig.buildings.forEach { b ->
            val bx = (b.x + b.width * 0.5f) * WORLD_SCALE
            val bz = (b.y + b.height * 0.5f) * WORLD_SCALE
            val dx = bx - targetGlX
            val dz = bz - targetGlZ
            if (dx * dx + dz * dz < qualityDrawDist * qualityDrawDist) {
                val bw = b.width * WORLD_SCALE
                val bd = b.height * WORLD_SCALE
                val bh = if (bw > 9f) 7.0f else 5.2f
                // Foundation slab
                drawBox(bx, 0.16f, bz, bw + 0.9f, 0.32f, bd + 0.9f, 0f, 0.22f, 0.25f, 0.28f, 1f, surfaceType = SURFACE_CONCRETE)
                // Weathered concrete main structure
                drawBox(bx, bh * 0.5f + 0.32f, bz, bw, bh, bd, 0f, 0.48f, 0.52f, 0.55f, 1f, surfaceType = SURFACE_CONCRETE)
                // Roof slab + parapet
                drawBox(bx, bh + 0.55f, bz, bw + 0.7f, 0.48f, bd + 0.7f, 0f, 0.26f, 0.22f, 0.20f, 1f)
                drawBox(bx - bw * 0.22f, bh + 1.1f, bz + bd * 0.15f, 1.8f, 0.9f, 1.8f, 0f, 0.34f, 0.38f, 0.42f, 1f)
                // Blue window trim frames + dark interior windows (matching reference battleground building)
                drawBox(bx - bw * 0.26f, bh * 0.65f, bz + bd * 0.5f + 0.04f, 1.65f, 1.35f, 0.12f, 0f, 0.08f, 0.45f, 0.85f, 1f)
                drawBox(bx - bw * 0.26f, bh * 0.65f, bz + bd * 0.5f + 0.08f, 1.30f, 1.00f, 0.12f, 0f, 0.09f, 0.11f, 0.14f, 1f)
                drawBox(bx + bw * 0.26f, bh * 0.65f, bz + bd * 0.5f + 0.04f, 1.65f, 1.35f, 0.12f, 0f, 0.08f, 0.45f, 0.85f, 1f)
                drawBox(bx + bw * 0.26f, bh * 0.65f, bz + bd * 0.5f + 0.08f, 1.30f, 1.00f, 0.12f, 0f, 0.09f, 0.11f, 0.14f, 1f)
                // Side windows & doorway
                drawBox(bx, 1.35f, bz + bd * 0.5f + 0.05f, 1.55f, 2.45f, 0.14f, 0f, 0.10f, 0.12f, 0.15f, 1f)
                drawBox(bx + bw * 0.5f + 0.05f, bh * 0.62f, bz, 0.12f, 1.3f, 1.6f, 0f, 0.08f, 0.45f, 0.85f, 1f)
                drawBox(bx + bw * 0.5f + 0.08f, bh * 0.62f, bz, 0.12f, 0.95f, 1.25f, 0f, 0.09f, 0.11f, 0.14f, 1f)
            }
        }

        // 4. Conifer Pine Trees, Rocks, Crates
        engine.mapConfig.obstacles.forEach { obs ->
            val ox = obs.x * WORLD_SCALE
            val oz = obs.y * WORLD_SCALE
            val dx = ox - targetGlX
            val dz = oz - targetGlZ
            if (dx * dx + dz * dz < qualityDrawDist * qualityDrawDist) {
                val r = obs.radius * WORLD_SCALE
                when (obs.type) {
                    ObstacleType.TREE -> {
                        if (showShadows) {
                            drawBox(ox, 0.04f, oz, r * 1.4f, 0.04f, r * 1.4f, 0f, 0.04f, 0.06f, 0.04f, 0.35f, unlit = 1f)
                        }
                        drawBox(ox, 1.9f, oz, 0.55f, 3.8f, 0.55f, 0f, 0.35f, 0.22f, 0.12f, 1f, surfaceType = SURFACE_RIBBED)
                        // 3-tier conifer pine foliage
                        drawBox(ox, 3.6f, oz, r * 1.65f, 1.55f, r * 1.65f, 12f, 0.13f, 0.38f, 0.14f, 1f)
                        drawBox(ox, 4.9f, oz, r * 1.22f, 1.45f, r * 1.22f, 38f, 0.16f, 0.46f, 0.18f, 1f)
                        drawBox(ox, 6.1f, oz, r * 0.72f, 1.30f, r * 0.72f, 65f, 0.20f, 0.54f, 0.22f, 1f)
                    }
                    ObstacleType.ROCK -> {
                        drawBox(ox, r * 0.45f, oz, r * 1.35f, r * 0.90f, r * 1.15f, 25f, 0.44f, 0.46f, 0.48f, 1f, surfaceType = SURFACE_CONCRETE)
                        drawBox(ox + r * 0.25f, r * 0.25f, oz - r * 0.2f, r * 0.7f, r * 0.5f, r * 0.7f, 52f, 0.38f, 0.40f, 0.42f, 1f)
                    }
                    ObstacleType.CRATE, ObstacleType.SANDBAGS -> {
                        drawBox(ox, 0.75f, oz, 1.6f, 1.5f, 1.6f, 0f, 0.56f, 0.42f, 0.24f, 1f, surfaceType = SURFACE_RIBBED)
                    }
                }
            }
        }

        // 4b. Iconic Battleground Landmarks & Cover Props (Shipping Container, Wooden Crates, Cooling Tower, Radio Mast, Billboard)
        if ((targetGlX - 250f) * (targetGlX - 250f) + (targetGlZ - 250f) * (targetGlZ - 250f) < qualityDrawDist * qualityDrawDist) {
            // Blue corrugated shipping container
            drawBox(264f, 1.7f, 240f, 7.6f, 3.4f, 3.2f, -15f, 0.08f, 0.46f, 0.80f, 1f, surfaceType = SURFACE_RIBBED)
            drawBox(264f, 3.42f, 240f, 7.7f, 0.18f, 3.3f, -15f, 0.06f, 0.36f, 0.65f, 1f)
            // Second olive cargo container nearby
            drawBox(234f, 1.7f, 263f, 7.2f, 3.3f, 3.1f, 72f, 0.18f, 0.36f, 0.24f, 1f, surfaceType = SURFACE_RIBBED)
            // Stacked wooden supply crates
            drawBox(260f, 0.85f, 244f, 1.8f, 1.7f, 1.8f, 5f, 0.62f, 0.45f, 0.25f, 1f, surfaceType = SURFACE_RIBBED)
            drawBox(262f, 0.85f, 244.5f, 1.8f, 1.7f, 1.8f, 12f, 0.58f, 0.41f, 0.22f, 1f, surfaceType = SURFACE_RIBBED)
            drawBox(261f, 2.55f, 244.2f, 1.7f, 1.7f, 1.7f, -8f, 0.64f, 0.48f, 0.27f, 1f, surfaceType = SURFACE_RIBBED)
            // Wooden perimeter cover fence
            drawBox(242f, 0.65f, 260f, 6.5f, 1.25f, 0.24f, 12f, 0.48f, 0.34f, 0.18f, 1f, surfaceType = SURFACE_RIBBED)

            // Highway "VORTEX" Billboard (like BATTLE ROYALE highway sign)
            drawBox(272f, 3.2f, 232f, 0.55f, 6.4f, 0.55f, 0f, 0.25f, 0.27f, 0.30f, 1f)
            drawBox(272f, 6.8f, 232f, 7.5f, 3.6f, 0.45f, -20f, 0.08f, 0.10f, 0.18f, 1f)
            drawBox(272f, 6.8f, 232.15f, 7.0f, 3.1f, 0.25f, -20f, 1.0f, 0.72f, 0.08f, 1f, unlit = 0.7f)

            // Distant Industrial Cooling Tower + Radio Lattice Tower Landmark
            drawBox(295f, 8.5f, 195f, 12.5f, 17f, 12.5f, 22f, 0.54f, 0.58f, 0.62f, 1f, surfaceType = SURFACE_CONCRETE)
            drawBox(295f, 19.5f, 195f, 10.2f, 7f, 10.2f, 22f, 0.58f, 0.62f, 0.66f, 1f, surfaceType = SURFACE_CONCRETE)
            drawBox(295f, 23.4f, 195f, 10.6f, 1.2f, 10.6f, 22f, 0.72f, 0.75f, 0.78f, 1f)
            // Radio communications tower + red warning light
            drawBox(318f, 10f, 212f, 1.6f, 20f, 1.6f, 0f, 0.42f, 0.46f, 0.50f, 1f)
            drawBox(318f, 20.6f, 212f, 0.7f, 0.7f, 0.7f, 0f, 1.0f, 0.15f, 0.12f, 1f, unlit = 1f)
        }

        // 4c. Interactive Red Explosive Fuel Barrels (shoot to detonate!)
        state.explosiveBarrelsSnapshot.filter { !it.isExploded }.forEach { barrel ->
            val bx = barrel.x * WORLD_SCALE
            val bz = barrel.y * WORLD_SCALE
            val dx = bx - targetGlX
            val dz = bz - targetGlZ
            if (dx * dx + dz * dz < qualityDrawDist * qualityDrawDist) {
                drawBox(bx, 0.85f, bz, 1.05f, 1.70f, 1.05f, 0f, 0.86f, 0.12f, 0.12f, 1f)
                // Yellow hazard band around middle
                drawBox(bx, 0.90f, bz, 1.09f, 0.34f, 1.09f, 0f, 1.0f, 0.82f, 0.08f, 1f, unlit = 0.2f)
                drawBox(bx, 1.72f, bz, 0.98f, 0.08f, 0.98f, 0f, 0.25f, 0.25f, 0.28f, 1f)
            }
        }

        // 5. Loot Beacons
        state.groundLootSnapshot.forEach { loot ->
            val lx = loot.x * WORLD_SCALE
            val lz = loot.y * WORLD_SCALE
            val dx = lx - targetGlX
            val dz = lz - targetGlZ
            if (dx * dx + dz * dz < 115f * 115f) {
                val (r, g, b) = when {
                    loot.weapon != null -> Triple(1.0f, 0.52f, 0.05f)
                    loot.armorLevel > 0 -> Triple(0.0f, 0.75f, 1.0f)
                    loot.consumable != null -> Triple(0.0f, 0.92f, 0.45f)
                    else -> Triple(1.0f, 0.84f, 0.0f)
                }
                drawBox(lx, 0.35f, lz, 0.85f, 0.45f, 0.85f, frameTimeSec * 40f, r, g, b, 0.95f, unlit = 0.6f)
                drawBox(lx, 1.6f, lz, 0.12f, 2.5f, 0.12f, 0f, r, g, b, 0.32f, unlit = 1.0f)
            }
        }

        // 6. Detailed 4x4 Vehicles (Wheels, Grille, Windshield, Headlights)
        state.vehiclesSnapshot.forEach { v ->
            val vx = v.x * WORLD_SCALE
            val vz = v.y * WORLD_SCALE
            val dx = vx - targetGlX
            val dz = vz - targetGlZ
            if (dx * dx + dz * dz < qualityDrawDist * qualityDrawDist) {
                renderVehicle3D(vx, vz, -v.rotationAngle, v.type, v.isDestroyed)
            }
        }

        // 7. Gloo Wall / Deployable Energy Shields
        state.deployableShields.forEach { shield ->
            val sx = shield.x * WORLD_SCALE
            val sz = shield.y * WORLD_SCALE
            val rot = -shield.angleDegrees
            drawBox(sx, 1.35f, sz, 0.36f, 2.65f, 4.4f, rot, 0.06f, 0.78f, 1.0f, 0.68f, unlit = 0.82f, surfaceType = SURFACE_RIBBED)
            drawBox(sx, 2.72f, sz, 0.42f, 0.18f, 4.5f, rot, 0.75f, 0.96f, 1.0f, 0.92f, unlit = 1f)
            drawBox(sx, 0.18f, sz, 0.44f, 0.25f, 4.5f, rot, 0.12f, 0.22f, 0.35f, 0.95f)
        }

        // 8. Airdrops with Red/Blue Beacon
        state.airdrops.forEach { drop ->
            val ax = drop.x * WORLD_SCALE
            val az = drop.y * WORLD_SCALE
            val ay = (drop.altitude * WORLD_SCALE).coerceAtLeast(1.1f)
            drawBox(ax, ay, az, 2.2f, 2.2f, 2.2f, 0f, 0.84f, 0.15f, 0.15f, 1f)
            drawBox(ax, ay + 1.2f, az, 2.3f, 0.45f, 2.3f, 0f, 0.10f, 0.45f, 0.88f, 1f)
            drawBox(ax, ay + 25f, az, 0.35f, 50f, 0.35f, 0f, 1.0f, 0.18f, 0.15f, 0.32f, unlit = 1f)
        }

        // 9. Plane during Flight Phase
        if (state.phase == MatchPhase.PLANE_FLIGHT || state.phase == MatchPhase.WAITING_IN_LOBBY) {
            renderSupplyPlane(state.planeX * WORLD_SCALE, 60f, state.planeY * WORLD_SCALE, -45f)
        }

        // 9b. Training Ground Silhouette Shooting Targets
        if (engine.gameMode == GameModeType.TRAINING_GROUND) {
            val rangeTargets = listOf(2440f, 2500f, 2560f, 2620f)
            rangeTargets.forEach { tx ->
                val gx = tx * WORLD_SCALE
                val gz = 2360f * WORLD_SCALE
                drawBox(gx, 0.9f, gz, 0.25f, 1.8f, 0.25f, 0f, 0.22f, 0.24f, 0.28f, 1f)
                drawBox(gx, 1.55f, gz, 1.15f, 1.35f, 0.22f, 0f, 0.95f, 0.42f, 0.10f, 1f)
                drawBox(gx, 2.45f, gz, 0.55f, 0.55f, 0.22f, 0f, 1.0f, 0.85f, 0.15f, 1f, unlit = 0.4f)
            }
        }

        // 10. Articulated 3D Players + Glowing Weapons + Companion Pet
        val radarActive = state.radarScanActiveSec > 0f
        val petPreset = PetPreset.fromId(state.activePetId)
        state.playersSnapshot.filter { it.isAlive }.forEach { player ->
            val px = player.x * WORLD_SCALE
            val pz = player.y * WORLD_SCALE
            val dx = px - targetGlX
            val dz = pz - targetGlZ
            if (dx * dx + dz * dz < qualityDrawDist * qualityDrawDist) {
                renderSoldier3D(player, user.teamId, showShadows, radarActive)
                if (state.petVisible && player.isUser && player.vehicleId == null && player.z < 5f) {
                    renderCompanionPet(player, petPreset)
                }
            }
        }

        // 11. 3D Bullet Tracers
        state.bulletsSnapshot.forEach { b ->
            val bx = b.x * WORLD_SCALE
            val bz = b.y * WORLD_SCALE
            val angleDeg = -Math.toDegrees(kotlin.math.atan2(b.vy.toDouble(), b.vx.toDouble())).toFloat()
            val len = if (b.isSniper) 2.3f else 1.45f
            drawBox(bx, 1.45f, bz, len, 0.12f, 0.12f, angleDeg, 1.0f, 0.88f, 0.25f, 1f, unlit = 1f)
        }

        // 12. Smoke Clouds & Explosion Shockwaves
        state.smokeClouds.forEach { smoke ->
            val sx = smoke.x * WORLD_SCALE
            val sz = smoke.y * WORLD_SCALE
            val r = smoke.radius * WORLD_SCALE
            val alpha = (smoke.remainingDurationSec / 25f).coerceIn(0.22f, 0.65f)
            drawBox(sx, r * 0.45f, sz, r * 1.5f, r * 0.9f, r * 1.5f, 20f, 0.80f, 0.84f, 0.88f, alpha, unlit = 0.5f)
        }
        state.explosions.forEach { exp ->
            val ex = exp.x * WORLD_SCALE
            val ez = exp.y * WORLD_SCALE
            val p = (exp.elapsedSec / exp.durationSec).coerceIn(0f, 1f)
            val rad = exp.maxRadius * WORLD_SCALE * p
            drawBox(ex, rad * 0.42f, ez, rad * 1.4f, rad * 1.1f, rad * 1.4f, p * 95f, 1.0f, 0.42f, 0.05f, (1f - p) * 0.80f, unlit = 1f)
            drawBox(ex, rad * 0.25f, ez, rad * 0.85f, rad * 0.7f, rad * 0.85f, -p * 60f, 1.0f, 0.88f, 0.22f, (1f - p) * 0.9f, unlit = 1f)
        }

        // 13. Safe Zone Electric Blue Wall Indicator Ring Segments
        renderZoneRing(
            cx = state.safeZone.currentBlueCenterX * WORLD_SCALE,
            cz = state.safeZone.currentBlueCenterY * WORLD_SCALE,
            radius = state.safeZone.currentBlueRadius * WORLD_SCALE
        )
    }

    private fun renderSkyDetails(focusX: Float, focusZ: Float) {
        // Bright sunlit disc + soft clouds + distant Battle Royale supply plane
        drawBox(focusX + 120f, 88f, focusZ - 140f, 14f, 14f, 14f, 0f, 1.0f, 0.95f, 0.75f, 0.92f, unlit = 1f)
        drawBox(focusX - 80f, 75f, focusZ - 120f, 28f, 5f, 16f, 15f, 0.96f, 0.98f, 1.0f, 0.75f, unlit = 0.9f)
        drawBox(focusX + 65f, 80f, focusZ + 115f, 34f, 5.5f, 18f, -20f, 0.96f, 0.98f, 1.0f, 0.72f, unlit = 0.9f)

        // Ambient orbiting C-130 Supply Plane + 2 skydiving silhouettes
        val orbitAngle = frameTimeSec * 0.14f
        val px = focusX + cos(orbitAngle) * 95f
        val pz = focusZ + sin(orbitAngle) * 95f
        renderSupplyPlane(px, 46f, pz, -Math.toDegrees(orbitAngle.toDouble()).toFloat() + 90f)
    }

    private fun renderSupplyPlane(px: Float, py: Float, pz: Float, rotDeg: Float) {
        drawBox(px, py, pz, 13f, 2.8f, 3.0f, rotDeg, 0.92f, 0.94f, 0.96f, 1f, unlit = 0.3f)
        drawBox(px, py + 0.3f, pz, 3.2f, 0.55f, 17f, rotDeg, 0.85f, 0.16f, 0.16f, 1f, unlit = 0.3f)
        drawBox(px - 5.2f, py + 2.2f, pz, 2.2f, 2.6f, 0.5f, rotDeg, 0.85f, 0.16f, 0.16f, 1f, unlit = 0.3f)
    }

    private fun renderVehicle3D(vx: Float, vz: Float, rot: Float, type: VehicleType, isDestroyed: Boolean) {
        val chassisR = when {
            isDestroyed -> 0.22f
            type == VehicleType.UAZ_JEEP -> 0.82f
            type == VehicleType.BUGGY -> 0.92f
            type == VehicleType.MOTORBIKE -> 0.85f
            else -> 0.12f
        }
        val chassisG = when {
            isDestroyed -> 0.22f
            type == VehicleType.UAZ_JEEP -> 0.84f
            type == VehicleType.BUGGY -> 0.55f
            type == VehicleType.MOTORBIKE -> 0.20f
            else -> 0.45f
        }
        val chassisB = when {
            isDestroyed -> 0.22f
            type == VehicleType.UAZ_JEEP -> 0.86f
            type == VehicleType.BUGGY -> 0.12f
            type == VehicleType.MOTORBIKE -> 0.20f
            else -> 0.82f
        }
        val rad = Math.toRadians((-rot).toDouble())
        val fx = cos(rad).toFloat()
        val fz = sin(rad).toFloat()
        val rx = -fz
        val rz = fx

        // Main chassis & cab
        drawBox(vx, 0.78f, vz, 4.4f, 0.92f, 2.15f, rot, chassisR, chassisG, chassisB, 1f)
        drawBox(vx - fx * 0.25f, 1.55f, vz - fz * 0.25f, 2.25f, 0.75f, 1.95f, rot, chassisR * 0.85f, chassisG * 0.85f, chassisB * 0.85f, 1f)
        // Windshield & headlights
        drawBox(vx + fx * 0.90f, 1.52f, vz + fz * 0.90f, 0.15f, 0.62f, 1.78f, rot, 0.45f, 0.75f, 0.92f, 0.88f, unlit = 0.4f)
        drawBox(vx + fx * 2.22f + rx * 0.65f, 0.78f, vz + fz * 2.22f + rz * 0.65f, 0.14f, 0.26f, 0.35f, rot, 1.0f, 0.92f, 0.55f, 1f, unlit = 1f)
        drawBox(vx + fx * 2.22f - rx * 0.65f, 0.78f, vz + fz * 2.22f - rz * 0.65f, 0.14f, 0.26f, 0.35f, rot, 1.0f, 0.92f, 0.55f, 1f, unlit = 1f)
        // 4 Black rubber wheels
        val wheelPositions = listOf(
            Pair(1.35f, 1.05f), Pair(1.35f, -1.05f),
            Pair(-1.35f, 1.05f), Pair(-1.35f, -1.05f)
        )
        wheelPositions.forEach { (wForward, wSide) ->
            val wx = vx + fx * wForward + rx * wSide
            val wz = vz + fz * wForward + rz * wSide
            drawBox(wx, 0.42f, wz, 0.82f, 0.82f, 0.35f, rot, 0.10f, 0.10f, 0.12f, 1f)
        }
    }

    private fun renderTerrain(focusX: Float, focusZ: Float, drawDist: Float) {
        val (gr, gg, gb) = when (engine.mapConfig.id) {
            "kalahari_dunes" -> Triple(0.68f, 0.54f, 0.33f)
            "snow_mountain" -> Triple(0.80f, 0.86f, 0.92f)
            "urban_city" -> Triple(0.24f, 0.27f, 0.30f)
            "jungle_battleground" -> Triple(0.16f, 0.38f, 0.17f)
            else -> Triple(0.28f, 0.44f, 0.20f)
        }
        // Main Textured Ground Slab
        drawBox(250f, -0.25f, 250f, 540f, 0.5f, 540f, 0f, gr, gg, gb, 1f, surfaceType = SURFACE_TERRAIN)

        // Major Paved Highway Roads
        drawBox(250f, 0.02f, 250f, 500f, 0.04f, 11f, 0f, 0.20f, 0.21f, 0.23f, 1f)
        drawBox(250f, 0.02f, 250f, 11f, 0.04f, 500f, 0f, 0.20f, 0.21f, 0.23f, 1f)
        drawBox(250f, 0.025f, 250f, 440f, 0.04f, 7f, 45f, 0.22f, 0.23f, 0.25f, 1f)

        // Dashed Yellow Center Stripes
        val snapX = ((focusX / 12f).toInt() * 12f).coerceIn(20f, 480f)
        val snapZ = ((focusZ / 12f).toInt() * 12f).coerceIn(20f, 480f)
        for (i in -6..6) {
            val sx = snapX + i * 12f
            val sz = snapZ + i * 12f
            if (abs(250f - focusZ) < drawDist) {
                drawBox(sx, 0.045f, 250f, 4.5f, 0.02f, 0.45f, 0f, 0.94f, 0.80f, 0.14f, 1f, unlit = 0.5f)
            }
            if (abs(250f - focusX) < drawDist) {
                drawBox(250f, 0.045f, sz, 0.45f, 0.02f, 4.5f, 0f, 0.94f, 0.80f, 0.14f, 1f, unlit = 0.5f)
            }
        }

        // Rolling Hills / Elevated Tactical Ridges
        val hillCoords = listOf(
            Triple(180f, 190f, 24f),
            Triple(310f, 210f, 28f),
            Triple(210f, 315f, 24f),
            Triple(330f, 330f, 28f),
            Triple(150f, 260f, 22f)
        )
        hillCoords.forEach { (hx, hz, hw) ->
            val dx = hx - focusX
            val dz = hz - focusZ
            if (dx * dx + dz * dz < drawDist * drawDist) {
                drawBox(hx, 0.9f, hz, hw, 1.8f, hw * 0.82f, 18f, gr * 0.92f, gg * 0.96f, gb * 0.92f, 1f, surfaceType = SURFACE_TERRAIN)
                drawBox(hx + 2f, 2.1f, hz - 1f, hw * 0.55f, 1.5f, hw * 0.45f, 32f, gr * 0.86f, gg * 0.92f, gb * 0.86f, 1f, surfaceType = SURFACE_TERRAIN)
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
        val (armorR, armorG, armorB) = if (player.isUser || isTeammate) preset.armorRgb else Triple(0.58f, 0.14f, 0.14f)
        val (accR, accG, accB) = if (player.isUser || isTeammate) preset.accentRgb else Triple(0.95f, 0.25f, 0.15f)
        val (visR, visG, visB) = if (player.isUser || isTeammate) preset.visorRgb else Triple(1.0f, 0.15f, 0.15f)

        val crouchScale = when (player.stance) {
            PlayerStance.CROUCH -> 0.72f
            PlayerStance.PRONE -> 0.40f
            else -> 1.0f
        }
        val stride = if (player.currentSpeed > 15f || !player.isUser) sin(player.animWalkPhase) * 0.36f else 0f

        if (showShadow) {
            drawBox(px, 0.05f, pz, 1.25f, 0.04f, 1.25f, 0f, 0.05f, 0.06f, 0.08f, 0.48f, unlit = 1f)
        }

        val rad = Math.toRadians(player.angle.toDouble())
        val fx = cos(rad).toFloat()
        val fz = sin(rad).toFloat()
        val rx = -fz
        val rz = fx

        if (player.stance == PlayerStance.PRONE) {
            drawBox(px - fx * 0.55f + rx * 0.16f, py + 0.18f, pz - fz * 0.55f + rz * 0.16f, 0.85f, 0.24f, 0.26f, yaw, 0.14f, 0.15f, 0.18f, 1f)
            drawBox(px - fx * 0.55f - rx * 0.16f, py + 0.18f, pz - fz * 0.55f - rz * 0.16f, 0.85f, 0.24f, 0.26f, yaw, 0.14f, 0.15f, 0.18f, 1f)
            drawBox(px + fx * 0.10f, py + 0.28f, pz + fz * 0.10f, 0.90f, 0.36f, 0.66f, yaw, armorR, armorG, armorB, 1f)
            drawBox(px + fx * 0.12f, py + 0.36f, pz + fz * 0.12f, 0.60f, 0.22f, 0.68f, yaw, accR, accG, accB, 1f, unlit = 0.45f)
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

        // Legs + Boots
        val legH = 0.85f * crouchScale
        drawBox(px + rx * 0.20f + fx * stride, py + legH * 0.5f, pz + rz * 0.20f + fz * stride, 0.28f, legH, 0.30f, yaw, 0.14f, 0.15f, 0.18f, 1f)
        drawBox(px - rx * 0.20f - fx * stride, py + legH * 0.5f, pz - rz * 0.20f - fz * stride, 0.28f, legH, 0.30f, yaw, 0.14f, 0.15f, 0.18f, 1f)

        // Torso, Vest & Shoulder Trim
        val torsoY = py + legH + 0.42f * crouchScale
        drawBox(px, torsoY, pz, 0.50f, 0.84f * crouchScale, 0.74f, yaw, armorR, armorG, armorB, 1f)
        drawBox(px, torsoY + 0.08f, pz, 0.54f, 0.32f * crouchScale, 0.80f, yaw, accR, accG, accB, 1f, unlit = 0.45f)

        // Tactical Backpack + Glowing Back Rifle (matching reference over-the-shoulder look)
        if (player.backpackLevel > 0) {
            drawBox(px - fx * 0.36f, torsoY + 0.04f, pz - fz * 0.36f, 0.30f, 0.62f * crouchScale, 0.52f, yaw, 0.15f, 0.17f, 0.20f, 1f)
            if (player.isUser) {
                drawBox(px - fx * 0.50f - rx * 0.16f, torsoY + 0.16f, pz - fz * 0.50f - rz * 0.16f, 0.14f, 1.25f * crouchScale, 0.14f, yaw + 15f, 0.0f, 0.85f, 1.0f, 0.95f, unlit = 0.9f)
            }
        }

        // Head, Helmet & Neon Visor
        val headY = py + legH + 0.84f * crouchScale + 0.26f
        drawBox(px, headY, pz, 0.42f, 0.44f, 0.42f, yaw, armorR * 0.85f, armorG * 0.85f, armorB * 0.85f, 1f)
        drawBox(px + fx * 0.16f, headY + 0.03f, pz + fz * 0.16f, 0.16f, 0.14f, 0.36f, yaw, visR, visG, visB, 1f, unlit = 0.9f)

        // Equipped Weapon with Glowing Accent Rail + Muzzle Flash
        if (player.activeWeapon != null) {
            val gunX = px + fx * 0.58f + rx * 0.32f
            val gunZ = pz + fz * 0.58f + rz * 0.32f
            drawBox(gunX, torsoY + 0.10f, gunZ, 1.18f, 0.17f, 0.15f, yaw, 0.16f, 0.18f, 0.22f, 1f)
            drawBox(gunX + fx * 0.12f, torsoY + 0.17f, gunZ + fz * 0.12f, 0.82f, 0.06f, 0.16f, yaw, accR, accG, accB, 1f, unlit = 0.9f)

            val justFired = System.currentTimeMillis() - player.lastFireTimestampMs < 95L
            if (justFired) {
                val flashX = px + fx * 1.28f + rx * 0.32f
                val flashZ = pz + fz * 1.28f + rz * 0.32f
                drawBox(flashX, torsoY + 0.10f, flashZ, 0.52f, 0.38f, 0.38f, yaw + 45f, 1.0f, 0.78f, 0.12f, 0.95f, unlit = 1f)
            }
        }

        // Radar Pulse Highlight Ring
        if (!isTeammate && radarActive) {
            drawBox(px, py + 0.12f, pz, 1.65f, 0.16f, 1.65f, yaw, 1.0f, 0.15f, 0.15f, 0.85f, unlit = 1f)
        }

        // Skydiving Parachute Canopy + Risers
        if (player.z > 15f) {
            drawBox(px, py + 4.5f, pz, 3.8f, 0.48f, 2.5f, yaw, 1.0f, 0.48f, 0.0f, 0.92f, unlit = 0.25f)
            drawBox(px + rx * 0.8f, py + 2.7f, pz + rz * 0.8f, 0.06f, 3.2f, 0.06f, yaw, 0.2f, 0.2f, 0.2f, 0.8f)
            drawBox(px - rx * 0.8f, py + 2.7f, pz - rz * 0.8f, 0.06f, 3.2f, 0.06f, yaw, 0.2f, 0.2f, 0.2f, 0.8f)
        }
    }

    private fun renderCompanionPet(owner: Player, pet: PetPreset) {
        val rad = Math.toRadians(owner.angle.toDouble())
        val fx = cos(rad).toFloat()
        val fz = sin(rad).toFloat()
        val rx = -fz
        val rz = fx
        val hover = sin(frameTimeSec * 4.5f) * 0.18f
        val petX = owner.x * WORLD_SCALE - fx * 0.75f - rx * 1.15f
        val petY = owner.z * WORLD_SCALE + 1.35f + hover
        val petZ = owner.y * WORLD_SCALE - fz * 0.75f - rz * 1.15f
        val yaw = -owner.angle
        val (br, bg, bb) = pet.bodyRgb
        val (gr, gg, gb) = pet.glowRgb
        // Body, wings/fins, and glowing eyes
        drawBox(petX, petY, petZ, 0.52f, 0.48f, 0.52f, yaw, br, bg, bb, 1f)
        drawBox(petX, petY + 0.12f, petZ, 0.25f, 0.18f, 1.15f, yaw, gr, gg, gb, 0.92f, unlit = 0.75f)
        drawBox(petX + fx * 0.22f, petY + 0.10f, petZ + fz * 0.22f, 0.14f, 0.14f, 0.34f, yaw, 1.0f, 0.92f, 0.2f, 1f, unlit = 1f)
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
        unlit: Float = 0f,
        surfaceType: Int = SURFACE_SOLID
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
        GLES20.glUniform1i(uSurfaceTypeHandle, surfaceType)
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

    private fun getSkyColor(): Triple<Float, Float, Float> {
        val base = when (engine.mapConfig.id) {
            "kalahari_dunes" -> Triple(0.84f, 0.62f, 0.36f)
            "snow_mountain" -> Triple(0.56f, 0.72f, 0.88f)
            "urban_city" -> Triple(0.14f, 0.18f, 0.30f)
            "jungle_battleground" -> Triple(0.28f, 0.45f, 0.40f)
            else -> Triple(0.36f, 0.65f, 0.92f)
        }
        return when (engine.weatherType) {
            WeatherType.CLEAR -> base
            WeatherType.SUNSET_STORM -> Triple(0.72f, 0.38f, 0.24f)
            WeatherType.DENSE_FOG -> Triple(0.62f, 0.68f, 0.72f)
            WeatherType.NIGHT_OPS -> Triple(0.06f, 0.09f, 0.16f)
        }
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun buildUnitCubeBuffer(): FloatBuffer {
        val v = floatArrayOf(
            -0.5f, -0.5f,  0.5f,  0f, 0f, 1f,
             0.5f, -0.5f,  0.5f,  0f, 0f, 1f,
             0.5f,  0.5f,  0.5f,  0f, 0f, 1f,
            -0.5f, -0.5f,  0.5f,  0f, 0f, 1f,
             0.5f,  0.5f,  0.5f,  0f, 0f, 1f,
            -0.5f,  0.5f,  0.5f,  0f, 0f, 1f,
             0.5f, -0.5f, -0.5f,  0f, 0f, -1f,
            -0.5f, -0.5f, -0.5f,  0f, 0f, -1f,
            -0.5f,  0.5f, -0.5f,  0f, 0f, -1f,
             0.5f, -0.5f, -0.5f,  0f, 0f, -1f,
            -0.5f,  0.5f, -0.5f,  0f, 0f, -1f,
             0.5f,  0.5f, -0.5f,  0f, 0f, -1f,
            -0.5f, -0.5f, -0.5f, -1f, 0f, 0f,
            -0.5f, -0.5f,  0.5f, -1f, 0f, 0f,
            -0.5f,  0.5f,  0.5f, -1f, 0f, 0f,
            -0.5f, -0.5f, -0.5f, -1f, 0f, 0f,
            -0.5f,  0.5f,  0.5f, -1f, 0f, 0f,
            -0.5f,  0.5f, -0.5f, -1f, 0f, 0f,
             0.5f, -0.5f,  0.5f,  1f, 0f, 0f,
             0.5f, -0.5f, -0.5f,  1f, 0f, 0f,
             0.5f,  0.5f, -0.5f,  1f, 0f, 0f,
             0.5f, -0.5f,  0.5f,  1f, 0f, 0f,
             0.5f,  0.5f, -0.5f,  1f, 0f, 0f,
             0.5f,  0.5f,  0.5f,  1f, 0f, 0f,
            -0.5f,  0.5f,  0.5f,  0f, 1f, 0f,
             0.5f,  0.5f,  0.5f,  0f, 1f, 0f,
             0.5f,  0.5f, -0.5f,  0f, 1f, 0f,
            -0.5f,  0.5f,  0.5f,  0f, 1f, 0f,
             0.5f,  0.5f, -0.5f,  0f, 1f, 0f,
            -0.5f,  0.5f, -0.5f,  0f, 1f, 0f,
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
