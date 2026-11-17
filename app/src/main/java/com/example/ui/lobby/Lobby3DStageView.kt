package com.example.ui.lobby

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.example.game.graphics.CharacterAndVehicleRenderer
import com.example.game.graphics.EnvironmentRenderer
import com.example.game.graphics.GlResolutionScaler
import com.example.game.graphics.GraphicsConfig
import com.example.game.graphics.MaterialId
import com.example.game.graphics.MeshLibrary
import com.example.game.graphics.PbrShaderProgram
import com.example.game.graphics.PbrTextureAtlas
import com.example.game.graphics.TerrainHeightfield
import com.example.game.graphics.VfxParticleSystem
import com.example.game.map.MapCatalog
import com.example.game.map.MapConfig
import com.example.game.model.PetPreset
import com.example.game.model.Player
import com.example.game.model.PlayerStance
import com.example.game.model.VehicleInstance
import com.example.game.model.VehicleType
import com.example.game.model.WeaponCatalog
import com.example.game.model.WeatherType
import com.example.ui.theme.LobbyWallpaperTheme
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun Lobby3DStageView(
    characterPresetId: String,
    petPresetId: String,
    petVisible: Boolean,
    equippedWeaponId: String = "m416",
    equippedWeaponSkin: String = "Tactical Slate",
    soldierYawOffset: Float,
    emoteTriggerTick: Int,
    wallpaperTheme: LobbyWallpaperTheme,
    weatherType: WeatherType,
    selectedMap: MapConfig,
    graphicsConfig: GraphicsConfig,
    modifier: Modifier = Modifier
) {
    val isInPreview = LocalInspectionMode.current
    val context = LocalContext.current

    if (isInPreview) {
        Box(modifier = modifier.fillMaxSize().background(wallpaperTheme.fallbackGradient))
        return
    }

    val renderer = remember { Lobby3DRenderer(graphicsConfig) }

    LaunchedEffect(
        characterPresetId,
        petPresetId,
        petVisible,
        equippedWeaponId,
        equippedWeaponSkin,
        soldierYawOffset,
        wallpaperTheme,
        weatherType,
        selectedMap.id,
        graphicsConfig
    ) {
        renderer.characterPresetId = characterPresetId
        renderer.petPresetId = petPresetId
        renderer.petVisible = petVisible
        renderer.equippedWeaponId = equippedWeaponId
        renderer.equippedWeaponSkin = equippedWeaponSkin
        renderer.soldierYawOffset = soldierYawOffset
        renderer.wallpaperTheme = wallpaperTheme
        renderer.weatherType = weatherType
        renderer.mapConfig = selectedMap
        renderer.graphicsConfig = graphicsConfig
    }

    LaunchedEffect(emoteTriggerTick) {
        if (emoteTriggerTick > 0) {
            renderer.emoteRemainingSec = 2.4f
        }
    }

    val surfaceView = remember(context) {
        runCatching {
            Lobby3DSurfaceView(context, renderer)
        }.getOrNull()
    }

    if (surfaceView == null) {
        Box(modifier = modifier.fillMaxSize().background(wallpaperTheme.fallbackGradient))
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
        modifier = modifier.fillMaxSize()
    )
}

private class Lobby3DSurfaceView(
    context: Context,
    renderer: Lobby3DRenderer
) : GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}

private class Lobby3DRenderer(
    initialConfig: GraphicsConfig
) : GLSurfaceView.Renderer {

    @Volatile var graphicsConfig: GraphicsConfig = initialConfig
    @Volatile var characterPresetId: String = "vortex_ghost"
    @Volatile var petPresetId: String = "draco_flame"
    @Volatile var petVisible: Boolean = true
    @Volatile var equippedWeaponId: String = "m416"
    @Volatile var equippedWeaponSkin: String = "Tactical Slate"
    @Volatile var soldierYawOffset: Float = 0f
    @Volatile var emoteRemainingSec: Float = 0f
    @Volatile var wallpaperTheme: LobbyWallpaperTheme = LobbyWallpaperTheme.DARK_TACTICAL
    @Volatile var weatherType: WeatherType = WeatherType.CLEAR
    @Volatile var mapConfig: MapConfig = MapCatalog.getKavalaIsland()

    private var cachedWeaponKey: String = ""

    private val shader = PbrShaderProgram()
    private val textures = PbrTextureAtlas()
    private val resolutionScaler = GlResolutionScaler()
    private lateinit var meshes: MeshLibrary
    private lateinit var envRenderer: EnvironmentRenderer
    private lateinit var actorRenderer: CharacterAndVehicleRenderer
    private val vfxPool = VfxParticleSystem(180)

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var viewportWidth = 1280
    private var viewportHeight = 720
    private var frameTimeSec = 0f
    private var lastFrameNs = System.nanoTime()
    private var emberCooldown = 0f

    private val showcasePlayer = Player(
        id = "lobby_operative",
        name = "Operative",
        isUser = true,
        teamId = 1,
        x = 2500f,
        y = 2500f,
        z = 0f,
        angle = 112f,
        stance = PlayerStance.STAND,
        primaryWeapon1 = WeaponCatalog.createM416(),
        activeSlot = 1
    )

    private val parkedVehicle = VehicleInstance(
        id = "lobby_uaz",
        type = VehicleType.UAZ_JEEP,
        x = 2625f,
        y = 2435f,
        rotationAngle = -38f
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        resolutionScaler.reset()
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
        val activeMap = mapConfig
        val activeTheme = wallpaperTheme
        val activeWeather = weatherType

        textures.ensureInitialized(cfg.textureResolutionPx, cfg.antiAliasing)
        vfxPool.activeLimit = (cfg.maxParticles / 2).coerceIn(40, 180)

        if (emoteRemainingSec > 0f) {
            emoteRemainingSec = (emoteRemainingSec - dt).coerceAtLeast(0f)
        }

        val groundY = TerrainHeightfield.groundHeightGl(activeMap, showcasePlayer.x, showcasePlayer.y)
        val px = showcasePlayer.x * TerrainHeightfield.WORLD_SCALE
        val pz = showcasePlayer.y * TerrainHeightfield.WORLD_SCALE

        // Subtle camera breathing + slight look-left offset so character sits prominently center-right
        val aspect = viewportWidth.toFloat() / viewportHeight.toFloat()
        val drawDist = cfg.viewDistance.toFloat().coerceAtLeast(180f)
        Matrix.perspectiveM(projectionMatrix, 0, 44f, aspect, 0.35f, drawDist + 140f)

        val camSway = sin(frameTimeSec * 0.55f) * 0.14f
        val camX = px - 0.55f + camSway
        val camY = groundY + 1.92f
        val camZ = pz + 4.35f
        val lookX = px - 0.38f
        val lookY = groundY + 1.38f
        val lookZ = pz - 1.2f

        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, lookX, lookY, lookZ, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        val palette = computeLobbyPalette(activeTheme, activeWeather)
        resolutionScaler.beginScaledPass(viewportWidth, viewportHeight, cfg.resolutionScale)
        GLES20.glClearColor(palette.horizonR, palette.horizonG, palette.horizonB, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // 1. Animated Sky Dome & Clouds
        renderSkyDome(palette, camX, camY, camZ)

        // 2. PBR World Pass
        GLES20.glUseProgram(shader.worldProgram)
        GLES20.glEnableVertexAttribArray(shader.aPos)
        GLES20.glEnableVertexAttribArray(shader.aNorm)
        GLES20.glEnableVertexAttribArray(shader.aUv)

        GLES20.glUniform1i(shader.uDiffuseTex, 0)
        GLES20.glUniform1i(shader.uNormalTex, 1)
        GLES20.glUniform3f(shader.uCameraPos, camX, camY, camZ)
        GLES20.glUniform3f(shader.uLightDir, palette.sunDirX, palette.sunDirY, palette.sunDirZ)
        GLES20.glUniform3f(shader.uSunColor, palette.sunR, palette.sunG, palette.sunB)
        GLES20.glUniform3f(shader.uSkyZenith, palette.zenithR, palette.zenithG, palette.zenithB)
        GLES20.glUniform3f(shader.uSkyHorizon, palette.horizonR, palette.horizonG, palette.horizonB)
        GLES20.glUniform3f(shader.uGroundBounce, palette.groundR, palette.groundG, palette.groundB)
        GLES20.glUniform3f(shader.uFogColor, palette.horizonR, palette.horizonG, palette.horizonB)
        GLES20.glUniform1f(shader.uFogMaxDist, drawDist * 0.92f)

        envRenderer.setViewProjection(vpMatrix)
        actorRenderer.setViewProjection(vpMatrix)

        // Terrain, mountains, conifers & environment backdrop
        envRenderer.renderWorldEnvironment(
            mapConfig = activeMap,
            focusX = px,
            focusZ = pz,
            drawDist = drawDist,
            graphicsConfig = cfg,
            frameTimeSec = frameTimeSec,
            isNightMode = activeWeather == WeatherType.NIGHT_OPS
        )

        // Tactical Lobby Helipad / Command Pedestal
        drawMesh(
            meshes.wheelCylinder, MaterialId.CONCRETE_WALL,
            px, groundY + 0.09f, pz, 90f, 0f, 0f,
            3.8f, 3.8f, 0.22f,
            0.16f, 0.20f, 0.28f, 1f, 0.35f, 0.65f, 0.08f
        )
        drawMesh(
            meshes.wheelCylinder, MaterialId.VEHICLE_COAT,
            px, groundY + 0.20f, pz, 90f, frameTimeSec * 12f, 0f,
            3.35f, 3.35f, 0.04f,
            palette.accentR, palette.accentG, palette.accentB, 0.85f,
            0.22f, 0.30f, 0.72f, useTex = 0f
        )

        // Background cruising C-130 Dropship in the sky
        val planeT = frameTimeSec * 0.16f
        val planeX = px - 18f + cos(planeT) * 52f
        val planeZ = pz - 58f + sin(planeT) * 28f
        val planeYaw = -Math.toDegrees(planeT.toDouble()).toFloat() + 90f
        renderBackdropDropship(planeX, groundY + 26f, planeZ, planeYaw)

        // Background parked tactical UAZ
        val uazGround = TerrainHeightfield.groundHeightGl(activeMap, parkedVehicle.x, parkedVehicle.y)
        actorRenderer.renderVehicle3D(parkedVehicle, uazGround, frameTimeSec, cfg.shadowQuality)

        // Prominent Animated 3D Playable Operative
        val wKey = "${equippedWeaponId}_${equippedWeaponSkin}"
        if (wKey != cachedWeaponKey) {
            cachedWeaponKey = wKey
            showcasePlayer.primaryWeapon1 = WeaponCatalog.createById(equippedWeaponId, equippedWeaponSkin)
        }
        showcasePlayer.characterPresetId = characterPresetId
        showcasePlayer.angle = 98f + soldierYawOffset + sin(frameTimeSec * 0.7f) * 3.5f
        showcasePlayer.emoteAnimSec = emoteRemainingSec
        actorRenderer.renderSoldier3D(
            player = showcasePlayer,
            groundY = groundY + 0.22f,
            userTeamId = 1,
            shadowQuality = cfg.shadowQuality,
            radarActive = false,
            frameTimeSec = frameTimeSec
        )

        // Hovering / Standing 3D Companion Pet beside the operative
        if (petVisible) {
            actorRenderer.renderCompanionPet(
                owner = showcasePlayer,
                groundY = groundY + 0.22f,
                pet = PetPreset.fromId(petPresetId),
                frameTimeSec = frameTimeSec
            )
        }

        // Dynamic Atmospheric Embers & Weather Particles
        updateAndDrawLobbyParticles(dt, px, groundY, pz, activeWeather, activeMap.id, palette)

        resolutionScaler.endScaledPassAndPresent(viewportWidth, viewportHeight)

        if (cfg.targetFps <= 32) {
            val elapsedMs = (System.nanoTime() - nowNs) / 1_000_000L
            val remain = 32L - elapsedMs
            if (remain in 2..28) {
                try { Thread.sleep(remain) } catch (_: InterruptedException) {}
            }
        }
    }

    private fun renderSkyDome(p: LobbyPalette, camX: Float, camY: Float, camZ: Float) {
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
        GLES20.glUniform3f(shader.skyULightDir, p.sunDirX, p.sunDirY, p.sunDirZ)
        GLES20.glUniform3f(shader.skyUZenith, p.zenithR, p.zenithG, p.zenithB)
        GLES20.glUniform3f(shader.skyUHorizon, p.horizonR, p.horizonG, p.horizonB)
        GLES20.glUniform3f(shader.skyUSunColor, p.sunR, p.sunG, p.sunB)
        GLES20.glUniform1f(shader.skyUTime, frameTimeSec)
        GLES20.glUniform1f(shader.skyUCloudDensity, 0.72f)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, meshes.skyDome.vertexCount)
        GLES20.glDepthMask(true)
    }

    private fun renderBackdropDropship(x: Float, y: Float, z: Float, yawDeg: Float) {
        drawMesh(meshes.chassisSlopeBody, MaterialId.CORRUGATED_METAL, x, y, z, 0f, yawDeg, 0f, 12.5f, 2.8f, 3.2f, 0.34f, 0.38f, 0.44f, 1f, 0.32f, 0.85f, 0f)
        drawMesh(meshes.sweptAircraftWing, MaterialId.CORRUGATED_METAL, x, y + 0.3f, z, 0f, yawDeg, 0f, 3.4f, 0.42f, 18.2f, 0.38f, 0.42f, 0.48f, 1f, 0.34f, 0.82f, 0f)
        drawMesh(meshes.aircraftTailFin, MaterialId.VEHICLE_COAT, x, y + 2.2f, z, 0f, yawDeg, 0f, 2.4f, 2.8f, 0.48f, 0.72f, 0.22f, 0.16f, 1f, 0.32f, 0.75f, 0.25f)
        drawMesh(meshes.rifleBarrel, MaterialId.CORRUGATED_METAL, x + 0.8f, y - 0.12f, z + 4.2f, 0f, yawDeg, 0f, 2.4f, 0.65f, 0.65f, 0.24f, 0.26f, 0.32f, 1f, 0.28f, 0.88f, 0.1f)
        drawMesh(meshes.rifleBarrel, MaterialId.CORRUGATED_METAL, x + 0.8f, y - 0.12f, z - 4.2f, 0f, yawDeg, 0f, 2.4f, 0.65f, 0.65f, 0.24f, 0.26f, 0.32f, 1f, 0.28f, 0.88f, 0.1f)
    }

    private fun updateAndDrawLobbyParticles(
        dt: Float,
        px: Float, groundY: Float, pz: Float,
        weather: WeatherType,
        mapId: String,
        palette: LobbyPalette
    ) {
        vfxPool.update(dt, weather, mapId, px, groundY, pz)
        emberCooldown -= dt
        if (emberCooldown <= 0f) {
            emberCooldown = 0.14f
            vfxPool.spawnSkillAura(
                x = px + (Random.nextFloat() - 0.5f) * 6.5f,
                y = groundY + 0.1f,
                z = pz + (Random.nextFloat() - 0.5f) * 5.0f,
                r = palette.accentR,
                g = palette.accentG,
                b = palette.accentB
            )
        }
        GLES20.glDepthMask(false)
        for (part in vfxPool.pool) {
            if (!part.active) continue
            val lifeAlpha = (1f - (part.age / part.life).coerceIn(0f, 1f)) * part.a
            drawMesh(
                meshes.quadBillboard, MaterialId.SKIN_DETAIL,
                part.x, part.y, part.z, 0f, frameTimeSec * 40f, 0f,
                part.size, part.size, part.size,
                part.r, part.g, part.b, lifeAlpha,
                0.5f, 0f, 0.95f, useTex = 0f
            )
        }
        GLES20.glDepthMask(true)
    }

    private fun drawMesh(
        mesh: com.example.game.graphics.GlMesh, mat: MaterialId,
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

    private data class LobbyPalette(
        val zenithR: Float, val zenithG: Float, val zenithB: Float,
        val horizonR: Float, val horizonG: Float, val horizonB: Float,
        val sunR: Float, val sunG: Float, val sunB: Float,
        val groundR: Float, val groundG: Float, val groundB: Float,
        val accentR: Float, val accentG: Float, val accentB: Float,
        val sunDirX: Float, val sunDirY: Float, val sunDirZ: Float
    )

    private fun computeLobbyPalette(theme: LobbyWallpaperTheme, weather: WeatherType): LobbyPalette {
        val base = when (theme) {
            LobbyWallpaperTheme.DARK_TACTICAL -> LobbyPalette(0.10f, 0.22f, 0.45f, 0.58f, 0.42f, 0.28f, 1.0f, 0.88f, 0.72f, 0.28f, 0.24f, 0.20f, 1.0f, 0.54f, 0.08f, 0.52f, 0.68f, -0.51f)
            LobbyWallpaperTheme.DESERT_WARFARE -> LobbyPalette(0.24f, 0.36f, 0.58f, 0.88f, 0.62f, 0.34f, 1.0f, 0.84f, 0.58f, 0.42f, 0.32f, 0.20f, 1.0f, 0.62f, 0.10f, 0.60f, 0.58f, -0.55f)
            LobbyWallpaperTheme.NEON_CYBERPUNK -> LobbyPalette(0.06f, 0.08f, 0.24f, 0.16f, 0.38f, 0.56f, 0.65f, 0.90f, 1.0f, 0.14f, 0.18f, 0.28f, 0.0f, 0.90f, 1.0f, 0.40f, 0.72f, -0.56f)
            LobbyWallpaperTheme.SNOW_BASE -> LobbyPalette(0.18f, 0.34f, 0.62f, 0.70f, 0.82f, 0.94f, 0.94f, 0.97f, 1.0f, 0.42f, 0.48f, 0.56f, 0.45f, 0.85f, 1.0f, 0.48f, 0.74f, -0.46f)
            LobbyWallpaperTheme.TROPICAL_ISLAND -> LobbyPalette(0.14f, 0.48f, 0.82f, 0.64f, 0.86f, 0.95f, 1.0f, 0.96f, 0.86f, 0.22f, 0.38f, 0.28f, 0.0f, 0.90f, 0.46f, 0.54f, 0.70f, -0.46f)
            LobbyWallpaperTheme.JUNGLE_SURVIVAL -> LobbyPalette(0.10f, 0.36f, 0.44f, 0.50f, 0.74f, 0.62f, 0.95f, 0.96f, 0.82f, 0.20f, 0.32f, 0.18f, 0.46f, 1.0f, 0.02f, 0.52f, 0.72f, -0.45f)
            LobbyWallpaperTheme.SCIFI_ARENA -> LobbyPalette(0.14f, 0.06f, 0.32f, 0.46f, 0.22f, 0.68f, 0.92f, 0.78f, 1.0f, 0.22f, 0.14f, 0.34f, 0.88f, 0.25f, 0.98f, 0.45f, 0.72f, -0.52f)
            LobbyWallpaperTheme.APOCALYPSE -> LobbyPalette(0.18f, 0.05f, 0.08f, 0.78f, 0.28f, 0.14f, 1.0f, 0.58f, 0.35f, 0.32f, 0.16f, 0.12f, 1.0f, 0.18f, 0.22f, 0.62f, 0.46f, -0.62f)
        }
        return if (weather == WeatherType.NIGHT_OPS) {
            base.copy(
                zenithR = base.zenithR * 0.35f, zenithG = base.zenithG * 0.38f, zenithB = base.zenithB * 0.55f,
                horizonR = base.horizonR * 0.40f, horizonG = base.horizonG * 0.44f, horizonB = base.horizonB * 0.60f,
                sunR = base.sunR * 0.62f, sunG = base.sunG * 0.68f, sunB = base.sunB * 0.88f
            )
        } else base
    }
}
