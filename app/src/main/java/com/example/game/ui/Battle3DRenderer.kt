package com.example.game.ui

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.engine.BattleRoyaleEngine
import com.example.game.graphics.CharacterAndVehicleRenderer
import com.example.game.graphics.EnvironmentRenderer
import com.example.game.graphics.GlMesh
import com.example.game.graphics.GraphicsConfig
import com.example.game.graphics.MaterialId
import com.example.game.graphics.MeshLibrary
import com.example.game.graphics.ParticleKind
import com.example.game.graphics.PbrShaderProgram
import com.example.game.graphics.PbrTextureAtlas
import com.example.game.graphics.TerrainHeightfield
import com.example.game.graphics.VfxParticleSystem
import com.example.game.model.GameModeType
import com.example.game.model.MatchPhase
import com.example.game.model.PetPreset
import com.example.game.model.PlayerStance
import com.example.game.model.WeatherType
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.atan2
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
    private var baseWidth = 0
    private var baseHeight = 0
    private var lastAppliedScale = -1f

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0 && baseWidth == 0) {
            baseWidth = w
            baseHeight = h
        }
        applyResolutionScale(renderer.graphicsConfig.resolutionScale)
    }

    fun applyResolutionScale(scale: Float) {
        val clamped = scale.coerceIn(0.60f, 1.00f)
        if (baseWidth > 0 && baseHeight > 0 && kotlin.math.abs(clamped - lastAppliedScale) > 0.03f) {
            lastAppliedScale = clamped
            val targetW = (baseWidth * clamped).toInt().coerceAtLeast(320)
            val targetH = (baseHeight * clamped).toInt().coerceAtLeast(240)
            post {
                holder.setFixedSize(targetW, targetH)
            }
        }
    }
}

class Battle3DRenderer(
    private val engine: BattleRoyaleEngine,
    graphicsQuality: String = "High",
    initialConfig: GraphicsConfig = GraphicsConfig.fromPreset(graphicsQuality)
) : GLSurfaceView.Renderer {

    @Volatile
    var graphicsConfig: GraphicsConfig = initialConfig

    @Volatile
    var graphicsQuality: String = graphicsQuality
        set(value) {
            field = value
            graphicsConfig = GraphicsConfig.fromPreset(value)
        }

    private val shader = PbrShaderProgram()
    private val textures = PbrTextureAtlas()
    private lateinit var meshes: MeshLibrary
    private lateinit var envRenderer: EnvironmentRenderer
    private lateinit var actorRenderer: CharacterAndVehicleRenderer
    private val vfxPool = VfxParticleSystem(320)

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
    private var smoothCamY = 5f
    private var smoothCamZ = 250f
    private var smoothFov = 58f
    private var frameTimeSec = 0f
    private var lastFrameNano = System.nanoTime()
    private var dustCooldown = 0f
    private var lastShotFxMs = 0L
    private var lastSeenExplosions = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        shader.init()
        meshes = MeshLibrary()
        textures.ensureInitialized(
            resolutionPx = graphicsConfig.textureResolutionPx,
            trilinearFiltering = graphicsConfig.antiAliasing
        )
        envRenderer = EnvironmentRenderer(meshes, textures, shader)
        actorRenderer = CharacterAndVehicleRenderer(meshes, textures, shader)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width.coerceAtLeast(1)
        viewportHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        val frameStartNs = System.nanoTime()
        val dt = ((frameStartNs - lastFrameNano) / 1_000_000_000f).coerceIn(0.004f, 0.05f)
        lastFrameNano = frameStartNs
        frameTimeSec += dt

        val cfg = graphicsConfig
        vfxPool.activeLimit = cfg.maxParticles
        textures.ensureInitialized(cfg.textureResolutionPx, cfg.antiAliasing)

        val state = engine.engineState.value
        val user = engine.userPlayer
        val activeTarget = if (state.isSpectating && state.spectatedPlayer != null) state.spectatedPlayer else user
        val drawDist = cfg.viewDistance.toFloat()

        // 1. Third-Person Camera Framing + Smooth Zoom + Raycast Obstacle Collision
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
        smoothFov += (desiredFov - smoothFov) * 0.22f
        Matrix.perspectiveM(projectionMatrix, 0, smoothFov, aspect, 0.4f, drawDist + 160f)

        val yawRad = Math.toRadians(engine.cameraYawDegrees.toDouble())
        val pitchRad = Math.toRadians(engine.cameraPitchDegrees.toDouble())
        val fx = cos(yawRad).toFloat()
        val fz = sin(yawRad).toFloat()
        val rx = -fz
        val rz = fx

        val targetGroundY = TerrainHeightfield.groundHeightGl(engine.mapConfig, activeTarget.x, activeTarget.y)
        val pivotX: Float
        val pivotY: Float
        val pivotZ: Float
        val camDist: Float
        val shoulderOffset: Float
        val camHeightOffset: Float

        when (state.phase) {
            MatchPhase.PLANE_FLIGHT, MatchPhase.WAITING_IN_LOBBY -> {
                pivotX = state.planeX * TerrainHeightfield.WORLD_SCALE
                pivotY = 62f
                pivotZ = state.planeY * TerrainHeightfield.WORLD_SCALE
                camDist = 28f
                shoulderOffset = 0f
                camHeightOffset = 9f
            }
            MatchPhase.SKYDIVING -> {
                pivotX = activeTarget.x * TerrainHeightfield.WORLD_SCALE
                pivotY = (targetGroundY + activeTarget.z * TerrainHeightfield.WORLD_SCALE).coerceAtLeast(1.8f)
                pivotZ = activeTarget.y * TerrainHeightfield.WORLD_SCALE
                camDist = 11.5f
                shoulderOffset = 1.0f
                camHeightOffset = 4.2f
            }
            else -> {
                val crouchDip = when (activeTarget.stance) {
                    PlayerStance.CROUCH -> -0.45f
                    PlayerStance.PRONE, PlayerStance.SWIMMING -> -0.92f
                    else -> 0f
                }
                pivotX = activeTarget.x * TerrainHeightfield.WORLD_SCALE
                pivotY = targetGroundY + activeTarget.z * TerrainHeightfield.WORLD_SCALE + 1.68f + crouchDip
                pivotZ = activeTarget.y * TerrainHeightfield.WORLD_SCALE
                camDist = if (activeTarget.vehicleId != null) 9.8f else if (engine.isAimingDownSights) 2.2f else 5.8f
                shoulderOffset = if (activeTarget.vehicleId != null) 0.4f else if (engine.isAimingDownSights) 0.68f else 1.24f
                camHeightOffset = if (activeTarget.vehicleId != null) 3.4f else 0.76f
            }
        }

        val desiredCamX = pivotX - fx * camDist * cos(pitchRad).toFloat() + rx * shoulderOffset
        val desiredCamY = pivotY + camHeightOffset + camDist * sin(pitchRad).toFloat()
        val desiredCamZ = pivotZ - fz * camDist * cos(pitchRad).toFloat() + rz * shoulderOffset

        val safeCam = if (state.phase == MatchPhase.ACTIVE_COMBAT || state.phase == MatchPhase.SPECTATING) {
            TerrainHeightfield.resolveCameraCollision(
                engine.mapConfig, pivotX, pivotY, pivotZ, desiredCamX, desiredCamY, desiredCamZ
            )
        } else {
            com.example.game.graphics.Vec3(desiredCamX, desiredCamY.coerceAtLeast(2.0f), desiredCamZ)
        }

        smoothCamX += (safeCam.x - smoothCamX) * 0.36f
        smoothCamY += (safeCam.y - smoothCamY) * 0.36f
        smoothCamZ += (safeCam.z - smoothCamZ) * 0.36f

        val lookX = pivotX + fx * 24f + rx * shoulderOffset * 0.52f
        val lookY = (pivotY - sin(pitchRad).toFloat() * 14f).coerceAtLeast(-10f)
        val lookZ = pivotZ + fz * 24f + rz * shoulderOffset * 0.52f

        Matrix.setLookAtM(viewMatrix, 0, smoothCamX, smoothCamY, smoothCamZ, lookX, lookY, lookZ, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        synchronized(lock) { System.arraycopy(vpMatrix, 0, sharedVpMatrix, 0, 16) }

        val atmos = computeAtmosphere(engine.mapConfig.id, engine.weatherType)
        GLES20.glClearColor(atmos.horizonR, atmos.horizonG, atmos.horizonB, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // 2. Atmospheric Gradient Sky Dome + Procedural Clouds + Sun Corona
        renderSkyDome(atmos)

        // 3. PBR World Pass Setup
        GLES20.glUseProgram(shader.worldProgram)
        GLES20.glEnableVertexAttribArray(shader.aPos)
        GLES20.glEnableVertexAttribArray(shader.aNorm)
        GLES20.glEnableVertexAttribArray(shader.aUv)

        GLES20.glUniform1i(shader.uDiffuseTex, 0)
        GLES20.glUniform1i(shader.uNormalTex, 1)
        GLES20.glUniform3f(shader.uCameraPos, smoothCamX, smoothCamY, smoothCamZ)
        GLES20.glUniform3f(shader.uLightDir, atmos.sunDirX, atmos.sunDirY, atmos.sunDirZ)
        GLES20.glUniform3f(shader.uSunColor, atmos.sunR, atmos.sunG, atmos.sunB)
        GLES20.glUniform3f(shader.uSkyZenith, atmos.zenithR, atmos.zenithG, atmos.zenithB)
        GLES20.glUniform3f(shader.uSkyHorizon, atmos.horizonR, atmos.horizonG, atmos.horizonB)
        GLES20.glUniform3f(shader.uGroundBounce, atmos.groundR, atmos.groundG, atmos.groundB)
        GLES20.glUniform3f(shader.uFogColor, atmos.fogR, atmos.fogG, atmos.fogB)
        GLES20.glUniform1f(shader.uFogMaxDist, atmos.fogDist(drawDist))

        envRenderer.setViewProjection(vpMatrix)
        actorRenderer.setViewProjection(vpMatrix)

        // Orbiting C-130 Supply Aircraft in sky
        val orbitAng = frameTimeSec * 0.14f
        renderSupplyPlane(pivotX + cos(orbitAng) * 95f, 48f, pivotZ + sin(orbitAng) * 95f, -Math.toDegrees(orbitAng.toDouble()).toFloat() + 90f)
        if (state.phase == MatchPhase.PLANE_FLIGHT || state.phase == MatchPhase.WAITING_IN_LOBBY) {
            renderSupplyPlane(state.planeX * TerrainHeightfield.WORLD_SCALE, 62f, state.planeY * TerrainHeightfield.WORLD_SCALE, -45f)
        }

        // 4. Environment (Heightfield Terrain, Mountains, Bridges, Warehouses, Houses, Conifer Trees, Boulders, Grass)
        envRenderer.renderWorldEnvironment(engine.mapConfig, pivotX, pivotZ, drawDist, cfg, frameTimeSec)

        // 5. Explosive Hazard Barrels, Loot Beacons, Vehicles, Shields, Airdrops, Silhouette Targets
        renderDynamicWorldProps(state, pivotX, pivotZ, drawDist, cfg.shadowQuality)

        // 6. Articulated 3D Players & Companion Pet
        val radarActive = state.radarScanActiveSec > 0f
        val petPreset = PetPreset.fromId(state.activePetId)
        state.playersSnapshot.filter { it.isAlive }.forEach { p ->
            val px = p.x * TerrainHeightfield.WORLD_SCALE
            val pz = p.y * TerrainHeightfield.WORLD_SCALE
            val dx = px - pivotX; val dz = pz - pivotZ
            if (dx * dx + dz * dz < drawDist * drawDist) {
                val pGround = TerrainHeightfield.groundHeightGl(engine.mapConfig, p.x, p.y)
                actorRenderer.renderSoldier3D(p, pGround, user.teamId, cfg.shadowQuality, radarActive, frameTimeSec)
                if (state.petVisible && p.isUser && p.vehicleId == null && p.z < 5f) {
                    actorRenderer.renderCompanionPet(p, pGround, petPreset, frameTimeSec)
                }
            }
        }

        // 7. Bullet Tracers, Explosions, Safe Zone Ring & Pooled 3D VFX Particles
        renderTracersAndZones(state)
        emitAndRenderVfx(dt, state, user, targetGroundY, pivotX, pivotY, pivotZ)

        // Frame pacing for battery-saving 30 FPS mode if selected
        if (cfg.targetFps <= 32) {
            val elapsedMs = (System.nanoTime() - frameStartNs) / 1_000_000L
            val remain = 32L - elapsedMs
            if (remain in 2..28) {
                try { Thread.sleep(remain) } catch (_: InterruptedException) {}
            }
        }
    }

    private fun renderSkyDome(atmos: AtmosphereColors) {
        GLES20.glDepthMask(false)
        GLES20.glUseProgram(shader.skyProgram)
        GLES20.glEnableVertexAttribArray(shader.skyAPos)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, smoothCamX, smoothCamY, smoothCamZ)
        Matrix.scaleM(modelMatrix, 0, 420f, 420f, 420f)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        val buf = meshes.skyDome.buffer
        buf.position(0)
        GLES20.glVertexAttribPointer(shader.skyAPos, 3, GLES20.GL_FLOAT, false, 32, buf)
        GLES20.glUniformMatrix4fv(shader.skyUMvp, 1, false, mvpMatrix, 0)
        GLES20.glUniform3f(shader.skyULightDir, atmos.sunDirX, atmos.sunDirY, atmos.sunDirZ)
        GLES20.glUniform3f(shader.skyUZenith, atmos.zenithR, atmos.zenithG, atmos.zenithB)
        GLES20.glUniform3f(shader.skyUHorizon, atmos.horizonR, atmos.horizonG, atmos.horizonB)
        GLES20.glUniform3f(shader.skyUSunColor, atmos.sunR, atmos.sunG, atmos.sunB)
        GLES20.glUniform1f(shader.skyUTime, frameTimeSec)
        GLES20.glUniform1f(shader.skyUCloudDensity, atmos.cloudDensity)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, meshes.skyDome.vertexCount)
        GLES20.glDepthMask(true)
    }

    private fun renderDynamicWorldProps(
        state: com.example.game.engine.MatchEngineState,
        focusX: Float, focusZ: Float, drawDist: Float, shadowQuality: Int
    ) {
        state.explosiveBarrelsSnapshot.filter { !it.isExploded }.forEach { barrel ->
            val bx = barrel.x * TerrainHeightfield.WORLD_SCALE
            val bz = barrel.y * TerrainHeightfield.WORLD_SCALE
            val dx = bx - focusX; val dz = bz - focusZ
            if (dx * dx + dz * dz < drawDist * drawDist) {
                val gy = TerrainHeightfield.groundHeightGl(engine.mapConfig, barrel.x, barrel.y)
                drawMesh(meshes.treeTrunk, MaterialId.CORRUGATED_METAL, bx, gy + 0.85f, bz, 0f, 0f, 0f, 1.05f, 1.70f, 1.05f, 0.86f, 0.14f, 0.14f, 1f, 0.35f, 0.7f, 0f)
                drawMesh(meshes.treeTrunk, MaterialId.VEHICLE_COAT, bx, gy + 0.90f, bz, 0f, 0f, 0f, 1.08f, 0.32f, 1.08f, 1.0f, 0.82f, 0.08f, 1f, 0.25f, 0.4f, 0.25f)
            }
        }

        state.groundLootSnapshot.forEach { loot ->
            val lx = loot.x * TerrainHeightfield.WORLD_SCALE
            val lz = loot.y * TerrainHeightfield.WORLD_SCALE
            val dx = lx - focusX; val dz = lz - focusZ
            if (dx * dx + dz * dz < 115f * 115f) {
                val gy = TerrainHeightfield.groundHeightGl(engine.mapConfig, loot.x, loot.y)
                val (r, g, b) = when {
                    loot.weapon != null -> Triple(1.0f, 0.52f, 0.05f)
                    loot.armorLevel > 0 -> Triple(0.0f, 0.78f, 1.0f)
                    loot.consumable != null -> Triple(0.0f, 0.92f, 0.45f)
                    else -> Triple(1.0f, 0.84f, 0.0f)
                }
                drawMesh(meshes.bevelPack, MaterialId.VEHICLE_COAT, lx, gy + 0.35f, lz, 0f, frameTimeSec * 45f, 0f, 0.82f, 0.44f, 0.82f, r, g, b, 0.96f, 0.25f, 0.4f, 0.65f)
                drawMesh(meshes.unitBox, MaterialId.SKIN_DETAIL, lx, gy + 1.55f, lz, 0f, 0f, 0f, 0.10f, 2.4f, 0.10f, r, g, b, 0.34f, 0.5f, 0f, 1.0f, 0f)
            }
        }

        state.vehiclesSnapshot.forEach { v ->
            val vx = v.x * TerrainHeightfield.WORLD_SCALE
            val vz = v.y * TerrainHeightfield.WORLD_SCALE
            val dx = vx - focusX; val dz = vz - focusZ
            if (dx * dx + dz * dz < drawDist * drawDist) {
                val gy = TerrainHeightfield.groundHeightGl(engine.mapConfig, v.x, v.y)
                actorRenderer.renderVehicle3D(v, gy, frameTimeSec, shadowQuality)
            }
        }

        state.deployableShields.forEach { shield ->
            val sx = shield.x * TerrainHeightfield.WORLD_SCALE
            val sz = shield.y * TerrainHeightfield.WORLD_SCALE
            val gy = TerrainHeightfield.groundHeightGl(engine.mapConfig, shield.x, shield.y)
            val rot = -shield.angleDegrees
            drawMesh(meshes.bevelPack, MaterialId.CORRUGATED_METAL, sx, gy + 1.35f, sz, 0f, rot, 0f, 0.36f, 2.65f, 4.4f, 0.06f, 0.78f, 1.0f, 0.68f, 0.15f, 0.7f, 0.82f)
            drawMesh(meshes.unitBox, MaterialId.VEHICLE_COAT, sx, gy + 2.72f, sz, 0f, rot, 0f, 0.42f, 0.18f, 4.5f, 0.75f, 0.96f, 1.0f, 0.92f, 0.1f, 0.5f, 1f)
        }

        state.airdrops.forEach { drop ->
            val ax = drop.x * TerrainHeightfield.WORLD_SCALE
            val az = drop.y * TerrainHeightfield.WORLD_SCALE
            val gy = TerrainHeightfield.groundHeightGl(engine.mapConfig, drop.x, drop.y)
            val ay = (gy + drop.altitude * TerrainHeightfield.WORLD_SCALE).coerceAtLeast(gy + 1.1f)
            drawMesh(meshes.bevelPack, MaterialId.CORRUGATED_METAL, ax, ay, az, 0f, 0f, 0f, 2.2f, 2.2f, 2.2f, 0.84f, 0.15f, 0.15f, 1f, 0.4f, 0.6f, 0f)
            drawMesh(meshes.unitBox, MaterialId.CORRUGATED_METAL, ax, ay + 1.2f, az, 0f, 0f, 0f, 2.3f, 0.45f, 2.3f, 0.10f, 0.45f, 0.88f, 1f, 0.4f, 0.6f, 0f)
            drawMesh(meshes.unitBox, MaterialId.SKIN_DETAIL, ax, ay + 25f, az, 0f, 0f, 0f, 0.35f, 50f, 0.35f, 1.0f, 0.18f, 0.15f, 0.32f, 0.5f, 0f, 1f, 0f)
        }

        if (engine.gameMode == GameModeType.TRAINING_GROUND) {
            listOf(2440f, 2500f, 2560f, 2620f).forEach { tx ->
                val gx = tx * TerrainHeightfield.WORLD_SCALE
                val gz = 2360f * TerrainHeightfield.WORLD_SCALE
                val gy = TerrainHeightfield.groundHeightGl(engine.mapConfig, tx, 2360f)
                drawMesh(meshes.treeTrunk, MaterialId.CORRUGATED_METAL, gx, gy + 0.9f, gz, 0f, 0f, 0f, 0.25f, 1.8f, 0.25f, 0.22f, 0.24f, 0.28f, 1f, 0.4f, 0.7f, 0f)
                drawMesh(meshes.chestTorso, MaterialId.VEHICLE_COAT, gx, gy + 1.55f, gz, 0f, 90f, 0f, 0.22f, 1.35f, 1.15f, 0.95f, 0.42f, 0.10f, 1f, 0.35f, 0.4f, 0f)
                drawMesh(meshes.humanHead, MaterialId.VEHICLE_COAT, gx, gy + 2.45f, gz, 0f, 90f, 0f, 0.48f, 0.52f, 0.52f, 1.0f, 0.85f, 0.15f, 1f, 0.25f, 0.3f, 0.4f)
            }
        }
    }

    private fun renderTracersAndZones(state: com.example.game.engine.MatchEngineState) {
        state.bulletsSnapshot.forEach { b ->
            val bx = b.x * TerrainHeightfield.WORLD_SCALE
            val bz = b.y * TerrainHeightfield.WORLD_SCALE
            val gy = TerrainHeightfield.groundHeightGl(engine.mapConfig, b.x, b.y)
            val ang = -Math.toDegrees(atan2(b.vy.toDouble(), b.vx.toDouble())).toFloat()
            val len = if (b.isSniper) 2.3f else 1.45f
            drawMesh(meshes.rifleBarrel, MaterialId.SKIN_DETAIL, bx, gy + 1.45f, bz, 0f, ang, 0f, len, 0.12f, 0.12f, 1.0f, 0.88f, 0.25f, 1f, 0.2f, 0f, 1f, 0f)
        }

        state.smokeClouds.forEach { smoke ->
            val sx = smoke.x * TerrainHeightfield.WORLD_SCALE
            val sz = smoke.y * TerrainHeightfield.WORLD_SCALE
            val gy = TerrainHeightfield.groundHeightGl(engine.mapConfig, smoke.x, smoke.y)
            val r = smoke.radius * TerrainHeightfield.WORLD_SCALE
            val alpha = (smoke.remainingDurationSec / 25f).coerceIn(0.20f, 0.62f)
            drawMesh(meshes.boulderRock, MaterialId.SKIN_DETAIL, sx, gy + r * 0.42f, sz, 0f, frameTimeSec * 12f, 0f, r * 1.55f, r * 0.95f, r * 1.55f, 0.80f, 0.84f, 0.88f, alpha, 0.95f, 0f, 0.45f, 0f)
        }

        val cx = state.safeZone.currentBlueCenterX * TerrainHeightfield.WORLD_SCALE
        val cz = state.safeZone.currentBlueCenterY * TerrainHeightfield.WORLD_SCALE
        val radius = state.safeZone.currentBlueRadius * TerrainHeightfield.WORLD_SCALE
        val segments = 24
        val step = (2.0 * Math.PI) / segments
        for (i in 0 until segments) {
            val angle = i * step
            val sx = cx + (cos(angle) * radius).toFloat()
            val sz = cz + (sin(angle) * radius).toFloat()
            val rotDeg = -Math.toDegrees(angle).toFloat() + 90f
            drawMesh(meshes.unitBox, MaterialId.SKIN_DETAIL, sx, 7f, sz, 0f, rotDeg, 0f, (2f * Math.PI.toFloat() * radius) / segments, 14f, 0.45f, 0.0f, 0.85f, 1.0f, 0.22f, 0.3f, 0f, 1f, 0f)
        }
    }

    private fun emitAndRenderVfx(
        dt: Float,
        state: com.example.game.engine.MatchEngineState,
        user: com.example.game.model.Player,
        groundY: Float,
        pivotX: Float, pivotY: Float, pivotZ: Float
    ) {
        val isDesert = engine.mapConfig.id == "kalahari_dunes"
        dustCooldown -= dt
        if (dustCooldown <= 0f) {
            if (user.isAlive && user.z <= 0.5f && (user.currentSpeed > 150f || user.isInWater)) {
                dustCooldown = if (user.isInWater) 0.14f else 0.11f
                if (user.isInWater) {
                    vfxPool.spawnWaterSplash(pivotX, groundY, pivotZ, 3)
                } else {
                    vfxPool.spawnFootstepDust(pivotX, groundY, pivotZ, isDesert)
                }
            }
            if (user.vehicleId != null) {
                val veh = state.vehiclesSnapshot.find { it.id == user.vehicleId }
                if (veh != null && veh.currentSpeed > 35f) {
                    dustCooldown = 0.07f
                    val vx = veh.x * TerrainHeightfield.WORLD_SCALE
                    val vz = veh.y * TerrainHeightfield.WORLD_SCALE
                    val gy = TerrainHeightfield.groundHeightGl(engine.mapConfig, veh.x, veh.y)
                    vfxPool.spawnFootstepDust(vx, gy, vz, isDesert)
                }
            }
        }

        if (user.lastFireTimestampMs > lastShotFxMs) {
            lastShotFxMs = user.lastFireTimestampMs
            val rad = Math.toRadians(user.angle.toDouble())
            val dirX = cos(rad).toFloat(); val dirZ = sin(rad).toFloat()
            vfxPool.spawnMuzzleSmokeAndSparks(pivotX + dirX * 1.1f, groundY + 1.38f, pivotZ + dirZ * 1.1f, dirX, dirZ)
        }

        if (state.explosions.size > lastSeenExplosions) {
            state.explosions.forEach { exp ->
                if (exp.elapsedSec < 0.05f) {
                    val ex = exp.x * TerrainHeightfield.WORLD_SCALE
                    val ez = exp.y * TerrainHeightfield.WORLD_SCALE
                    val gy = TerrainHeightfield.groundHeightGl(engine.mapConfig, exp.x, exp.y)
                    vfxPool.spawnExplosionBurst(ex, gy, ez, exp.maxRadius * TerrainHeightfield.WORLD_SCALE)
                }
            }
        }
        lastSeenExplosions = state.explosions.size

        if (state.dashActiveSec > 0f) {
            vfxPool.spawnSkillAura(pivotX, groundY, pivotZ, 1.0f, 0.58f, 0.12f)
        }
        if (state.radarScanActiveSec > 0f) {
            vfxPool.spawnSkillAura(pivotX, groundY, pivotZ, 0.0f, 0.90f, 1.0f)
        }

        vfxPool.update(dt, engine.weatherType, engine.mapConfig.id, pivotX, pivotY, pivotZ)

        // Render active pooled particles facing camera
        val lookYaw = -Math.toDegrees(atan2((smoothCamZ - pivotZ).toDouble(), (smoothCamX - pivotX).toDouble())).toFloat() + 90f
        for (p in vfxPool.pool) {
            if (!p.active) continue
            val t = (p.age / p.life).coerceIn(0f, 1f)
            val fadeAlpha = p.a * (1f - t)
            val mesh = if (p.kind == ParticleKind.RAIN) meshes.quadBillboard else meshes.quadBillboard
            val sy = if (p.kind == ParticleKind.RAIN) p.size * 7f else p.size
            val emissive = if (p.kind == ParticleKind.FIRE || p.kind == ParticleKind.SPARK || p.kind == ParticleKind.AURA) 1.0f else 0.4f
            drawMesh(mesh, MaterialId.SKIN_DETAIL, p.x, p.y, p.z, 0f, lookYaw, 0f, p.size, sy, p.size, p.r, p.g, p.b, fadeAlpha, 0.8f, 0f, emissive, 0f)
        }
    }

    private fun renderSupplyPlane(px: Float, py: Float, pz: Float, rotDeg: Float) {
        drawMesh(meshes.limbCapsule, MaterialId.CORRUGATED_METAL, px, py, pz, 0f, rotDeg, 90f, 2.8f, 13.5f, 2.8f, 0.92f, 0.94f, 0.96f, 1f, 0.32f, 0.72f, 0.2f)
        drawMesh(meshes.pitchedRoof, MaterialId.VEHICLE_COAT, px, py + 0.35f, pz, 0f, rotDeg, 0f, 3.4f, 0.55f, 18.0f, 0.86f, 0.18f, 0.18f, 1f, 0.35f, 0.62f, 0.25f)
        drawMesh(meshes.bevelPack, MaterialId.VEHICLE_COAT, px - 5.2f, py + 2.2f, pz, 0f, rotDeg, 0f, 2.2f, 2.8f, 0.52f, 0.86f, 0.18f, 0.18f, 1f, 0.35f, 0.62f, 0.25f)
    }

    private fun drawMesh(
        mesh: GlMesh, mat: MaterialId,
        x: Float, y: Float, z: Float, rx: Float, ry: Float, rz: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float, a: Float,
        roughness: Float, metallic: Float, emissive: Float, useTex: Float = 1f
    ) {
        textures.get(mat)?.let { pair ->
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, pair.albedoTexId)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, pair.normalTexId)
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
        GLES20.glUniform2f(shader.uUvScale, 1f, 1f)
        GLES20.glUniform2f(shader.uUvOffset, 0f, 0f)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.vertexCount)
    }

    fun projectWorldToScreen(worldX: Float, worldY: Float, worldZ: Float): ScreenProjectedPoint {
        val vpCopy = FloatArray(16)
        synchronized(lock) { System.arraycopy(sharedVpMatrix, 0, vpCopy, 0, 16) }
        val groundGl = TerrainHeightfield.groundHeightGl(engine.mapConfig, worldX, worldY)
        val vec = floatArrayOf(
            worldX * TerrainHeightfield.WORLD_SCALE,
            groundGl + worldZ * TerrainHeightfield.WORLD_SCALE,
            worldY * TerrainHeightfield.WORLD_SCALE,
            1f
        )
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

    private data class AtmosphereColors(
        val zenithR: Float, val zenithG: Float, val zenithB: Float,
        val horizonR: Float, val horizonG: Float, val horizonB: Float,
        val groundR: Float, val groundG: Float, val groundB: Float,
        val sunR: Float, val sunG: Float, val sunB: Float,
        val fogR: Float, val fogG: Float, val fogB: Float,
        val sunDirX: Float, val sunDirY: Float, val sunDirZ: Float,
        val cloudDensity: Float,
        val fogScale: Float
    ) {
        fun fogDist(drawDist: Float) = drawDist * fogScale
    }

    private fun computeAtmosphere(mapId: String, weather: WeatherType): AtmosphereColors {
        val sunLen = sqrt(0.55f * 0.55f + 0.72f * 0.72f + 0.42f * 0.42f)
        val sx = 0.55f / sunLen; val sy = 0.72f / sunLen; val sz = -0.42f / sunLen
        return when (weather) {
            WeatherType.SUNSET_STORM -> AtmosphereColors(
                0.24f, 0.22f, 0.36f, 0.84f, 0.45f, 0.28f,
                0.22f, 0.16f, 0.12f, 1.0f, 0.64f, 0.34f,
                0.68f, 0.40f, 0.28f, sx, 0.42f, sz, 0.85f, 0.72f
            )
            WeatherType.DENSE_FOG -> AtmosphereColors(
                0.48f, 0.56f, 0.62f, 0.68f, 0.74f, 0.78f,
                0.26f, 0.30f, 0.28f, 0.75f, 0.78f, 0.82f,
                0.66f, 0.72f, 0.76f, sx, sy, sz, 0.95f, 0.44f
            )
            WeatherType.NIGHT_OPS -> AtmosphereColors(
                0.03f, 0.05f, 0.12f, 0.09f, 0.14f, 0.24f,
                0.04f, 0.06f, 0.08f, 0.42f, 0.55f, 0.85f,
                0.07f, 0.10f, 0.18f, sx, sy, sz, 0.45f, 0.65f
            )
            WeatherType.CLEAR -> when (mapId) {
                "kalahari_dunes" -> AtmosphereColors(
                    0.22f, 0.52f, 0.85f, 0.88f, 0.72f, 0.52f,
                    0.34f, 0.26f, 0.16f, 1.0f, 0.94f, 0.80f,
                    0.84f, 0.68f, 0.48f, sx, sy, sz, 0.42f, 1.0f
                )
                "snow_mountain" -> AtmosphereColors(
                    0.18f, 0.44f, 0.80f, 0.72f, 0.84f, 0.95f,
                    0.32f, 0.36f, 0.42f, 0.96f, 0.98f, 1.0f,
                    0.70f, 0.82f, 0.94f, sx, sy, sz, 0.62f, 0.96f
                )
                else -> AtmosphereColors(
                    0.12f, 0.46f, 0.88f, 0.62f, 0.80f, 0.96f,
                    0.22f, 0.26f, 0.18f, 1.0f, 0.96f, 0.86f,
                    0.58f, 0.76f, 0.94f, sx, sy, sz, 0.60f, 1.0f
                )
            }
        }
    }
}
