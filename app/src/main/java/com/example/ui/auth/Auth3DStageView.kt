package com.example.ui.auth

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.example.game.graphics.CharacterAndVehicleRenderer
import com.example.game.graphics.EnvironmentRenderer
import com.example.game.graphics.GlMesh
import com.example.game.graphics.GraphicsConfig
import com.example.game.graphics.MaterialId
import com.example.game.graphics.MeshLibrary
import com.example.game.graphics.PbrShaderProgram
import com.example.game.graphics.PbrTextureAtlas
import com.example.game.graphics.TerrainHeightfield
import com.example.game.graphics.VfxParticleSystem
import com.example.game.map.MapCatalog
import com.example.game.model.Player
import com.example.game.model.PlayerStance
import com.example.game.model.WeaponCatalog
import com.example.game.model.WeatherType
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun Auth3DStageView(
    cameraDragOffset: Float,
    graphicsConfig: GraphicsConfig,
    modifier: Modifier = Modifier
) {
    val isInPreview = LocalInspectionMode.current
    val context = LocalContext.current

    val fallbackBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0A1E4E),
            Color(0xFF15479B),
            Color(0xFF2874C8),
            Color(0xFF140B14)
        )
    )

    if (isInPreview) {
        Box(modifier = modifier.fillMaxSize().background(fallbackBrush))
        return
    }

    val renderer = remember { Auth3DRenderer(graphicsConfig) }
    LaunchedEffect(cameraDragOffset, graphicsConfig) {
        renderer.cameraDragOffset = cameraDragOffset
        renderer.graphicsConfig = graphicsConfig
    }

    val surfaceView = remember(context) {
        runCatching {
            Auth3DSurfaceView(context, renderer)
        }.getOrNull()
    }

    if (surfaceView == null) {
        Box(modifier = modifier.fillMaxSize().background(fallbackBrush))
        return
    }

    DisposableEffect(surfaceView) {
        surfaceView.onResume()
        onDispose {
            surfaceView.onPause()
        }
    }

    AndroidView(
        factory = { surfaceView },
        update = { view ->
            view.applyResolutionScale(graphicsConfig.resolutionScale)
        },
        modifier = modifier.fillMaxSize()
    )
}

private class Auth3DSurfaceView(
    context: Context,
    val renderer: Auth3DRenderer
) : GLSurfaceView(context) {
    private var baseWidth = 0
    private var baseHeight = 0
    private var lastScale = -1f

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
        if (baseWidth > 0 && baseHeight > 0 && kotlin.math.abs(clamped - lastScale) > 0.03f) {
            lastScale = clamped
            val targetW = (baseWidth * clamped).toInt().coerceAtLeast(320)
            val targetH = (baseHeight * clamped).toInt().coerceAtLeast(180)
            post {
                holder.setFixedSize(targetW, targetH)
            }
        }
    }
}

private class Auth3DRenderer(
    initialConfig: GraphicsConfig
) : GLSurfaceView.Renderer {

    @Volatile var graphicsConfig: GraphicsConfig = initialConfig
    @Volatile var cameraDragOffset: Float = 0f

    private val shader = PbrShaderProgram()
    private val textures = PbrTextureAtlas()
    private lateinit var meshes: MeshLibrary
    private lateinit var envRenderer: EnvironmentRenderer
    private lateinit var actorRenderer: CharacterAndVehicleRenderer
    private val vfxPool = VfxParticleSystem(240)

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private val mapConfig = MapCatalog.getKavalaIsland()
    private var viewportWidth = 1280
    private var viewportHeight = 720
    private var frameTimeSec = 0f
    private var lastFrameNs = System.nanoTime()
    private var emberCooldown = 0f
    private var flareCooldown = 0f

    // Front left hero: silver-white hair, black/red half-mask, crouching ready stance
    private val heroMale = Player(
        id = "auth_hero_male",
        name = "WarX_Ghost",
        isUser = false,
        teamId = 1,
        x = 2478f,
        y = 2504f,
        z = 0f,
        angle = 44f,
        stance = PlayerStance.CROUCH,
        primaryWeapon1 = WeaponCatalog.createM416("Crimson Inferno"),
        activeSlot = 1,
        armorLevel = 2,
        helmetLevel = 0,
        backpackLevel = 1
    )

    // Rear center-left heroine: dark high ponytail with red tips, crimson/black tactical crop vest
    private val heroFemale = Player(
        id = "auth_hero_female",
        name = "WarX_Valkyrie",
        isUser = false,
        teamId = 1,
        x = 2494f,
        y = 2486f,
        z = 0f,
        angle = 62f,
        stance = PlayerStance.STAND,
        characterPresetId = "nova_valkyrie",
        primaryWeapon1 = WeaponCatalog.createById("akm", "Dragonfire Inferno"),
        activeSlot = 1,
        armorLevel = 1,
        helmetLevel = 0,
        backpackLevel = 0
    )

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
        val nowNs = System.nanoTime()
        val dt = ((nowNs - lastFrameNs) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
        lastFrameNs = nowNs
        frameTimeSec += dt

        val cfg = graphicsConfig
        textures.ensureInitialized(cfg.textureResolutionPx, cfg.antiAliasing)
        vfxPool.activeLimit = (cfg.maxParticles / 2).coerceIn(50, 220)

        val focusX = 250f
        val focusZ = 250f
        val baseGroundY = TerrainHeightfield.groundHeightGl(mapConfig, 2500f, 2500f)

        val aspect = viewportWidth.toFloat() / viewportHeight.toFloat()
        val drawDist = cfg.viewDistance.toFloat().coerceAtLeast(220f)
        Matrix.perspectiveM(projectionMatrix, 0, 44f, aspect, 0.35f, drawDist + 160f)

        val camSwayX = sin(frameTimeSec * 0.48f) * 0.14f + cameraDragOffset * 0.35f
        val camSwayY = cos(frameTimeSec * 0.38f) * 0.06f
        // Camera looks slightly toward the right so characters stay anchored in the left 45% of the screen
        val camX = focusX + 0.90f + camSwayX
        val camY = baseGroundY + 2.08f + camSwayY
        val camZ = focusZ + 4.85f
        val lookX = focusX + 0.65f
        val lookY = baseGroundY + 1.50f
        val lookZ = focusZ - 4.0f

        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, lookX, lookY, lookZ, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Bright daytime royal-blue sky palette matching reference art
        val zenithR = 0.07f; val zenithG = 0.34f; val zenithB = 0.78f
        val horizonR = 0.42f; val horizonG = 0.75f; val horizonB = 0.98f
        val sunR = 1.0f; val sunG = 0.95f; val sunB = 0.85f
        val sunDirX = -0.35f; val sunDirY = 0.78f; val sunDirZ = -0.52f

        GLES20.glClearColor(horizonR, horizonG, horizonB, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // 1. Royal-blue Sky Dome with moving clouds
        renderSkyDome(camX, camY, camZ, sunDirX, sunDirY, sunDirZ, zenithR, zenithG, zenithB, horizonR, horizonG, horizonB, sunR, sunG, sunB)

        // 2. PBR 3D World Pass
        GLES20.glUseProgram(shader.worldProgram)
        GLES20.glEnableVertexAttribArray(shader.aPos)
        GLES20.glEnableVertexAttribArray(shader.aNorm)
        GLES20.glEnableVertexAttribArray(shader.aUv)

        GLES20.glUniform1i(shader.uDiffuseTex, 0)
        GLES20.glUniform1i(shader.uNormalTex, 1)
        GLES20.glUniform3f(shader.uCameraPos, camX, camY, camZ)
        GLES20.glUniform3f(shader.uLightDir, sunDirX, sunDirY, sunDirZ)
        GLES20.glUniform3f(shader.uSunColor, sunR, sunG, sunB)
        GLES20.glUniform3f(shader.uSkyZenith, zenithR, zenithG, zenithB)
        GLES20.glUniform3f(shader.uSkyHorizon, horizonR, horizonG, horizonB)
        GLES20.glUniform3f(shader.uGroundBounce, 0.28f, 0.24f, 0.26f)
        GLES20.glUniform3f(shader.uFogColor, horizonR, horizonG, horizonB)
        GLES20.glUniform1f(shader.uFogMaxDist, drawDist * 0.95f)

        envRenderer.setViewProjection(vpMatrix)
        actorRenderer.setViewProjection(vpMatrix)

        envRenderer.renderWorldEnvironment(mapConfig, focusX, focusZ, drawDist, cfg, frameTimeSec)

        // Foreground Left Volcanic Rocky Cliff Outcrop
        drawMesh(meshes.boulderRock, MaterialId.CONCRETE_WALL, focusX - 2.35f, baseGroundY + 0.15f, focusZ + 0.45f, 0f, 18f, -12f, 2.75f, 1.35f, 2.25f, 0.12f, 0.12f, 0.15f, 1f, 0.90f, 0.08f, 0f)
        drawMesh(meshes.boulderRock, MaterialId.CONCRETE_WALL, focusX - 0.70f, baseGroundY + 0.05f, focusZ - 1.35f, 0f, -24f, 6f, 2.30f, 1.38f, 1.95f, 0.15f, 0.14f, 0.17f, 1f, 0.88f, 0.06f, 0f)
        drawMesh(meshes.boulderRock, MaterialId.CONCRETE_WALL, focusX - 3.60f, baseGroundY + 0.65f, focusZ - 0.80f, 0f, 35f, 14f, 1.75f, 1.85f, 1.65f, 0.10f, 0.10f, 0.13f, 1f, 0.92f, 0.08f, 0f)

        // Fluttering Black & Crimson WarX Crown Flag on Left Cliff Peak
        renderWarXCliffFlag(focusX - 3.35f, baseGroundY + 1.55f, focusZ - 0.95f)

        // Coastal Shoreline Harbor & Radio Mast Silhouette on Far Right Distance
        renderCoastalRadioOutpost(focusX + 12.5f, baseGroundY + 0.2f, focusZ - 26f)

        // Cruising C-130 Dropship & 3 Descending Parachutes in Sky
        val planeDrift = sin(frameTimeSec * 0.22f) * 3.8f
        renderDropship(focusX - 1.2f + planeDrift, baseGroundY + 13.8f, focusZ - 32f, 24f)
        renderParachutist(focusX - 0.8f + sin(frameTimeSec * 0.7f) * 0.4f, baseGroundY + 8.8f - (frameTimeSec * 0.18f % 1.4f), focusZ - 24f, frameTimeSec)
        renderParachutist(focusX - 0.2f + cos(frameTimeSec * 0.6f) * 0.35f, baseGroundY + 6.4f - ((frameTimeSec * 0.15f + 0.6f) % 1.2f), focusZ - 21f, frameTimeSec + 1.2f)
        renderParachutist(focusX + 10.6f + sin(frameTimeSec * 0.5f) * 0.3f, baseGroundY + 7.2f, focusZ - 25f, frameTimeSec + 2.1f)

        // Male Frontline Operative (Silver-White Hair, Black/Red Half Mask, Obsidian + Crimson Armor)
        heroMale.angle = 32f + sin(frameTimeSec * 0.85f) * 2.5f
        actorRenderer.renderSoldier3D(
            player = heroMale,
            groundY = baseGroundY + 0.68f,
            userTeamId = 1,
            shadowQuality = cfg.shadowQuality,
            radarActive = false,
            frameTimeSec = frameTimeSec,
            armorRgbOverride = Triple(0.11f, 0.11f, 0.14f),
            accentRgbOverride = Triple(0.94f, 0.11f, 0.17f),
            hairRgbOverride = Triple(0.92f, 0.94f, 0.97f),
            showHelmetAndVisor = false,
            halfMaskRgb = Triple(0.08f, 0.08f, 0.10f),
            halfMaskAccentRgb = Triple(0.96f, 0.12f, 0.18f),
            hasPonytailHair = false
        )

        // Female Overwatch Operative (Dark Hair + High Red-Tipped Ponytail, Crimson/Black Combat Vest)
        heroFemale.angle = 48f + cos(frameTimeSec * 0.75f) * 2.2f
        actorRenderer.renderSoldier3D(
            player = heroFemale,
            groundY = baseGroundY + 0.62f,
            userTeamId = 1,
            shadowQuality = cfg.shadowQuality,
            radarActive = false,
            frameTimeSec = frameTimeSec,
            armorRgbOverride = Triple(0.14f, 0.10f, 0.12f),
            accentRgbOverride = Triple(0.92f, 0.13f, 0.20f),
            hairRgbOverride = Triple(0.10f, 0.08f, 0.09f),
            showHelmetAndVisor = false,
            halfMaskRgb = null,
            halfMaskAccentRgb = null,
            hasPonytailHair = true
        )

        // Embers & Crimson Smoke Flare VFX
        updateAndDrawAuthVfx(dt, focusX, baseGroundY, focusZ)

        if (cfg.targetFps <= 32) {
            val elapsedMs = (System.nanoTime() - nowNs) / 1_000_000L
            val remain = 32L - elapsedMs
            if (remain in 2..28) {
                try { Thread.sleep(remain) } catch (_: InterruptedException) {}
            }
        }
    }

    private fun renderWarXCliffFlag(poleBaseX: Float, poleBaseY: Float, poleBaseZ: Float) {
        // Slanted Steel Flagpole
        drawMesh(meshes.unitBox, MaterialId.CORRUGATED_METAL, poleBaseX, poleBaseY + 1.4f, poleBaseZ, 0f, 0f, 14f, 0.08f, 3.6f, 0.08f, 0.16f, 0.16f, 0.20f, 1f, 0.35f, 0.85f, 0f)
        // Animated Cloth Segments (3 undulating strips + crimson crown crest)
        for (i in 0..3) {
            val waveZ = sin(frameTimeSec * 4.8f - i * 0.8f) * 0.10f
            val waveY = cos(frameTimeSec * 3.6f - i * 0.7f) * 0.04f
            val segX = poleBaseX - 0.15f - i * 0.36f
            val segY = poleBaseY + 2.45f + i * 0.08f + waveY
            drawMesh(
                meshes.unitBox, MaterialId.TACTICAL_FABRIC,
                segX, segY, poleBaseZ + waveZ,
                0f, waveZ * 45f, 12f,
                0.38f, 1.35f, 0.04f,
                0.08f, 0.08f, 0.10f, 0.96f, 0.75f, 0.05f, 0f
            )
            if (i == 1 || i == 2) {
                drawMesh(
                    meshes.unitBox, MaterialId.VEHICLE_COAT,
                    segX, segY + 0.06f, poleBaseZ + waveZ + 0.028f,
                    0f, waveZ * 45f, 12f,
                    0.30f, 0.54f, 0.02f,
                    0.94f, 0.10f, 0.16f, 0.98f, 0.3f, 0.3f, 0.75f, useTex = 0f
                )
            }
        }
    }

    private fun renderCoastalRadioOutpost(x: Float, y: Float, z: Float) {
        // Outpost Building
        drawMesh(meshes.unitBox, MaterialId.CONCRETE_WALL, x, y + 1.2f, z, 0f, 12f, 0f, 4.2f, 2.4f, 3.6f, 0.26f, 0.28f, 0.36f, 1f, 0.75f, 0.15f, 0f)
        // Radio Mast Tower + Crimson Beacons
        drawMesh(meshes.unitBox, MaterialId.CORRUGATED_METAL, x + 0.8f, y + 5.2f, z - 0.5f, 0f, 0f, 0f, 0.55f, 7.6f, 0.55f, 0.22f, 0.25f, 0.32f, 1f, 0.38f, 0.82f, 0f)
        drawMesh(meshes.unitBox, MaterialId.VEHICLE_COAT, x + 0.8f, y + 8.8f, z - 0.5f, 0f, 0f, 0f, 0.32f, 0.45f, 0.32f, 1.0f, 0.16f, 0.22f, 1f, 0.2f, 0.2f, 0.95f, useTex = 0f)
    }

    private fun renderDropship(x: Float, y: Float, z: Float, yawDeg: Float) {
        drawMesh(meshes.chassisSlopeBody, MaterialId.CORRUGATED_METAL, x, y, z, 6f, yawDeg, -8f, 9.6f, 2.1f, 2.5f, 0.42f, 0.46f, 0.54f, 1f, 0.30f, 0.85f, 0.05f)
        drawMesh(meshes.unitBox, MaterialId.CORRUGATED_METAL, x, y + 0.22f, z, 6f, yawDeg, -8f, 2.4f, 0.28f, 13.5f, 0.48f, 0.52f, 0.58f, 1f, 0.32f, 0.82f, 0.05f)
        // Wing Red/Orange Engine Highlights
        drawMesh(meshes.unitBox, MaterialId.VEHICLE_COAT, x, y + 0.10f, z + 3.6f, 6f, yawDeg, -8f, 1.4f, 0.45f, 0.45f, 0.96f, 0.32f, 0.18f, 1f, 0.2f, 0.4f, 0.8f, useTex = 0f)
        drawMesh(meshes.unitBox, MaterialId.VEHICLE_COAT, x, y + 0.10f, z - 3.6f, 6f, yawDeg, -8f, 1.4f, 0.45f, 0.45f, 0.96f, 0.32f, 0.18f, 1f, 0.2f, 0.4f, 0.8f, useTex = 0f)
    }

    private fun renderParachutist(x: Float, y: Float, z: Float, phase: Float) {
        val sway = sin(phase * 1.9f) * 8f
        drawMesh(meshes.helmetShell, MaterialId.TACTICAL_FABRIC, x, y + 1.25f, z, 0f, 0f, sway, 2.2f, 0.85f, 1.45f, 0.14f, 0.15f, 0.18f, 0.96f, 0.65f, 0.05f, 0f)
        drawMesh(meshes.unitBox, MaterialId.VEHICLE_COAT, x, y + 1.42f, z, 0f, 0f, sway, 1.6f, 0.22f, 1.15f, 0.95f, 0.16f, 0.20f, 0.95f, 0.35f, 0.2f, 0.4f, useTex = 0f)
        drawMesh(meshes.limbCapsule, MaterialId.TACTICAL_FABRIC, x, y + 0.18f, z, 0f, 0f, sway, 0.22f, 0.65f, 0.22f, 0.10f, 0.12f, 0.16f, 1f, 0.6f, 0.1f, 0f)
    }

    private fun updateAndDrawAuthVfx(dt: Float, focusX: Float, baseGroundY: Float, focusZ: Float) {
        vfxPool.update(dt, WeatherType.CLEAR, mapConfig.id, focusX, baseGroundY, focusZ)

        // Left Cliff Rising Fiery Embers (Orange & Crimson Glow)
        emberCooldown -= dt
        if (emberCooldown <= 0f) {
            emberCooldown = 0.09f
            vfxPool.spawnSkillAura(
                x = focusX - 2.8f + (Random.nextFloat() - 0.5f) * 2.2f,
                y = baseGroundY + 0.35f,
                z = focusZ - 0.6f + (Random.nextFloat() - 0.5f) * 1.6f,
                r = 1.0f,
                g = 0.28f + Random.nextFloat() * 0.25f,
                b = 0.06f
            )
        }

        // Right Shoreline Rising Crimson Smoke Flare
        flareCooldown -= dt
        if (flareCooldown <= 0f) {
            flareCooldown = 0.16f
            vfxPool.spawnSkillAura(
                x = focusX + 8.5f + (Random.nextFloat() - 0.5f) * 0.9f,
                y = baseGroundY + 0.4f,
                z = focusZ - 14.5f + (Random.nextFloat() - 0.5f) * 0.9f,
                r = 0.96f,
                g = 0.10f,
                b = 0.22f
            )
        }

        GLES20.glDepthMask(false)
        for (part in vfxPool.pool) {
            if (!part.active) continue
            val lifeAlpha = (1f - (part.age / part.life).coerceIn(0f, 1f)) * part.a
            drawMesh(
                meshes.quadBillboard, MaterialId.SKIN_DETAIL,
                part.x, part.y, part.z, 0f, frameTimeSec * 36f, 0f,
                part.size * 1.4f, part.size * 1.4f, part.size * 1.4f,
                part.r, part.g, part.b, lifeAlpha,
                0.5f, 0f, 0.95f, useTex = 0f
            )
        }
        GLES20.glDepthMask(true)
    }

    private fun renderSkyDome(
        camX: Float, camY: Float, camZ: Float,
        sunDirX: Float, sunDirY: Float, sunDirZ: Float,
        zenithR: Float, zenithG: Float, zenithB: Float,
        horizonR: Float, horizonG: Float, horizonB: Float,
        sunR: Float, sunG: Float, sunB: Float
    ) {
        GLES20.glDepthMask(false)
        GLES20.glUseProgram(shader.skyProgram)
        GLES20.glEnableVertexAttribArray(shader.skyAPos)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, camX, camY, camZ)
        Matrix.scaleM(modelMatrix, 0, 340f, 340f, 340f)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        val buf = meshes.skyDome.buffer
        buf.position(0)
        GLES20.glVertexAttribPointer(shader.skyAPos, 3, GLES20.GL_FLOAT, false, 32, buf)
        GLES20.glUniformMatrix4fv(shader.skyUMvp, 1, false, mvpMatrix, 0)
        GLES20.glUniform3f(shader.skyULightDir, sunDirX, sunDirY, sunDirZ)
        GLES20.glUniform3f(shader.skyUZenith, zenithR, zenithG, zenithB)
        GLES20.glUniform3f(shader.skyUHorizon, horizonR, horizonG, horizonB)
        GLES20.glUniform3f(shader.skyUSunColor, sunR, sunG, sunB)
        GLES20.glUniform1f(shader.skyUTime, frameTimeSec)
        GLES20.glUniform1f(shader.skyUCloudDensity, 0.68f)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, meshes.skyDome.vertexCount)
        GLES20.glDepthMask(true)
    }

    private fun drawMesh(
        mesh: GlMesh, mat: MaterialId,
        x: Float, y: Float, z: Float, rx: Float, ry: Float, rz: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float, a: Float,
        roughness: Float, metallic: Float, emissive: Float,
        useTex: Float = 1f
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        if (ry != 0f) Matrix.rotateM(modelMatrix, 0, ry, 0f, 1f, 0f)
        if (rx != 0f) Matrix.rotateM(modelMatrix, 0, rx, 1f, 0f, 0f)
        if (rz != 0f) Matrix.rotateM(modelMatrix, 0, rz, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        textures.get(mat)?.let { pair ->
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, pair.albedoTexId)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, pair.normalTexId)
        }
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
        GLES20.glUniform2f(shader.uUvScale, 1f, 1f)
        GLES20.glUniform2f(shader.uUvOffset, 0f, 0f)
        GLES20.glUniform1f(shader.uUseTex, useTex)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.vertexCount)
    }
}
