package com.example.ui.components

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.example.game.graphics.CharacterAndVehicleRenderer
import com.example.game.graphics.MaterialId
import com.example.game.graphics.MeshLibrary
import com.example.game.graphics.PbrShaderProgram
import com.example.game.graphics.PbrTextureAtlas
import com.example.game.model.CharacterPreset
import com.example.game.model.MagType
import com.example.game.model.MuzzleType
import com.example.game.model.Player
import com.example.game.model.PlayerStance
import com.example.game.model.ScopeType
import com.example.game.model.Weapon
import com.example.game.model.WeaponCatalog
import com.example.game.model.WeaponCategory
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Character3DShowcaseView(
    characterPresetId: String,
    weaponId: String = "m416",
    weaponSkin: String = "Tactical Slate",
    animMode: String = "IDLE",
    autoRotate: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    var yawDeg by remember { mutableFloatStateOf(20f) }

    if (isPreview) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF162238), Color(0xFF0A0F1A))))
        )
        return
    }

    val renderer = remember { CharacterShowcaseRenderer() }

    LaunchedEffect(characterPresetId, weaponId, weaponSkin, animMode, autoRotate, yawDeg) {
        renderer.characterPresetId = characterPresetId
        renderer.weaponId = weaponId
        renderer.weaponSkin = weaponSkin
        renderer.animMode = animMode
        renderer.autoRotate = autoRotate
        renderer.userYawDeg = yawDeg
    }

    val surfaceView = remember(context) {
        runCatching {
            GLSurfaceView(context).apply {
                setEGLContextClientVersion(2)
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
        }.getOrNull()
    }

    if (surfaceView == null) {
        Box(modifier = modifier.fillMaxSize().background(Color(0xFF0A0F1A)))
        return
    }

    DisposableEffect(surfaceView) {
        surfaceView.onResume()
        onDispose { surfaceView.onPause() }
    }

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                yawDeg = (yawDeg + dragAmount.x * 0.55f) % 360f
            }
        }
    ) {
        AndroidView(factory = { surfaceView }, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun Weapon3DShowcaseView(
    weapon: Weapon,
    skinName: String,
    scopeType: ScopeType,
    muzzleType: MuzzleType,
    magType: MagType,
    autoInspect: Boolean,
    upgradeLevel: Int = 1,
    modifier: Modifier = Modifier
) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    var yawDeg by remember { mutableFloatStateOf(-25f) }
    var pitchDeg by remember { mutableFloatStateOf(8f) }

    if (isPreview) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(Color(0xFF1F2B44), Color(0xFF090D16))))
        )
        return
    }

    val renderer = remember { WeaponShowcaseRenderer() }

    LaunchedEffect(weapon.id, weapon.category, skinName, scopeType, muzzleType, magType, autoInspect, upgradeLevel, yawDeg, pitchDeg) {
        renderer.weaponCategory = weapon.category
        renderer.weaponId = weapon.id
        renderer.skinName = skinName
        renderer.scopeType = scopeType
        renderer.muzzleType = muzzleType
        renderer.magType = magType
        renderer.autoInspect = autoInspect
        renderer.upgradeLevel = upgradeLevel
        renderer.yawDeg = yawDeg
        renderer.pitchDeg = pitchDeg
    }

    val surfaceView = remember(context) {
        runCatching {
            GLSurfaceView(context).apply {
                setEGLContextClientVersion(2)
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
        }.getOrNull()
    }

    if (surfaceView == null) {
        Box(modifier = modifier.fillMaxSize().background(Color(0xFF0A0F1A)))
        return
    }

    DisposableEffect(surfaceView) {
        surfaceView.onResume()
        onDispose { surfaceView.onPause() }
    }

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                yawDeg = (yawDeg + dragAmount.x * 0.55f) % 360f
                pitchDeg = (pitchDeg - dragAmount.y * 0.35f).coerceIn(-40f, 40f)
            }
        }
    ) {
        AndroidView(factory = { surfaceView }, modifier = Modifier.fillMaxSize())
    }
}

private class CharacterShowcaseRenderer : GLSurfaceView.Renderer {
    @Volatile var characterPresetId: String = "vortex_ghost"
    @Volatile var weaponId: String = "m416"
    @Volatile var weaponSkin: String = "Tactical Slate"
    @Volatile var animMode: String = "IDLE"
    @Volatile var autoRotate: Boolean = false
    @Volatile var userYawDeg: Float = 20f

    private val shader = PbrShaderProgram()
    private val textures = PbrTextureAtlas()
    private lateinit var meshes: MeshLibrary
    private lateinit var actorRenderer: CharacterAndVehicleRenderer

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private var viewportW = 640
    private var viewportH = 480
    private var frameSec = 0f
    private var lastNs = System.nanoTime()
    private var lastWKey = ""

    private val modelPlayer = Player(
        id = "preview_char",
        name = "Operative",
        isUser = true,
        teamId = 1,
        x = 0f,
        y = 0f,
        z = 0f,
        angle = 90f,
        armorLevel = 2,
        helmetLevel = 2,
        backpackLevel = 2,
        primaryWeapon1 = WeaponCatalog.createM416(),
        activeSlot = 1
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        shader.init()
        meshes = MeshLibrary()
        textures.ensureInitialized(256, true)
        actorRenderer = CharacterAndVehicleRenderer(meshes, textures, shader)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportW = width.coerceAtLeast(1)
        viewportH = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, viewportW, viewportH)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastNs) / 1_000_000_000f).coerceIn(0.004f, 0.05f)
        lastNs = now
        frameSec += dt

        val preset = CharacterPreset.fromId(characterPresetId)
        val (ar, ag, ab) = preset.accentRgb

        GLES20.glClearColor(0.05f, 0.08f, 0.14f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val aspect = viewportW.toFloat() / viewportH.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 38f, aspect, 0.25f, 50f)
        val camY = 1.38f
        val camZ = 3.55f
        Matrix.setLookAtM(viewMatrix, 0, 0f, camY, camZ, 0f, 1.05f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        GLES20.glUseProgram(shader.worldProgram)
        GLES20.glEnableVertexAttribArray(shader.aPos)
        GLES20.glEnableVertexAttribArray(shader.aNorm)
        GLES20.glEnableVertexAttribArray(shader.aUv)

        GLES20.glUniform1i(shader.uDiffuseTex, 0)
        GLES20.glUniform1i(shader.uNormalTex, 1)
        GLES20.glUniform3f(shader.uCameraPos, 0f, camY, camZ)
        GLES20.glUniform3f(shader.uLightDir, 0.48f, 0.72f, 0.50f)
        GLES20.glUniform3f(shader.uSunColor, 1.0f, 0.94f, 0.86f)
        GLES20.glUniform3f(shader.uSkyZenith, 0.16f, 0.28f, 0.48f)
        GLES20.glUniform3f(shader.uSkyHorizon, 0.24f, 0.34f, 0.48f)
        GLES20.glUniform3f(shader.uGroundBounce, ar * 0.4f, ag * 0.4f, ab * 0.4f)
        GLES20.glUniform3f(shader.uFogColor, 0.05f, 0.08f, 0.14f)
        GLES20.glUniform1f(shader.uFogMaxDist, 45f)

        actorRenderer.setViewProjection(vpMatrix)

        // Glowing Holographic Turntable Pedestal
        drawStudioMesh(meshes.wheelCylinder, MaterialId.CONCRETE_WALL, 0f, 0.06f, 0f, 90f, 0f, 0f, 1.55f, 1.55f, 0.12f, 0.14f, 0.18f, 0.26f, 1f, 0.3f, 0.7f, 0.05f)
        drawStudioMesh(meshes.wheelCylinder, MaterialId.VEHICLE_COAT, 0f, 0.13f, 0f, 90f, frameSec * 18f, 0f, 1.42f, 1.42f, 0.03f, ar, ag, ab, 0.9f, 0.2f, 0.3f, 0.85f)

        val wKey = "${weaponId}_${weaponSkin}"
        if (wKey != lastWKey) {
            lastWKey = wKey
            modelPlayer.primaryWeapon1 = WeaponCatalog.createById(weaponId, weaponSkin)
        }
        modelPlayer.characterPresetId = characterPresetId
        val rotSpin = if (autoRotate) frameSec * 36f else 0f
        modelPlayer.angle = 95f + userYawDeg + rotSpin
        when (animMode) {
            "VICTORY" -> {
                modelPlayer.stance = PlayerStance.STAND
                modelPlayer.emoteAnimSec = 1.0f
                modelPlayer.currentSpeed = 0f
            }
            "COMBAT" -> {
                modelPlayer.stance = PlayerStance.CROUCH
                modelPlayer.emoteAnimSec = 0f
                modelPlayer.currentSpeed = 95f
                modelPlayer.animWalkPhase = frameSec * 4.5f
            }
            else -> {
                modelPlayer.stance = PlayerStance.STAND
                modelPlayer.emoteAnimSec = 0f
                modelPlayer.currentSpeed = 0f
            }
        }

        actorRenderer.renderSoldier3D(
            player = modelPlayer,
            groundY = 0.14f,
            userTeamId = 1,
            shadowQuality = 2,
            radarActive = false,
            frameTimeSec = frameSec
        )
    }

    private fun drawStudioMesh(
        mesh: com.example.game.graphics.GlMesh,
        mat: MaterialId,
        x: Float, y: Float, z: Float, rx: Float, ry: Float, rz: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float, a: Float,
        roughness: Float, metallic: Float, emissive: Float
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
        GLES20.glUniform1f(shader.uUseTex, 0f)
        GLES20.glUniform2f(shader.uUvScale, 1f, 1f)
        GLES20.glUniform2f(shader.uUvOffset, 0f, 0f)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.vertexCount)
    }
}

private class WeaponShowcaseRenderer : GLSurfaceView.Renderer {
    @Volatile var weaponCategory: WeaponCategory = WeaponCategory.ASSAULT_RIFLE
    @Volatile var weaponId: String = "m416"
    @Volatile var skinName: String = "Tactical Slate"
    @Volatile var scopeType: ScopeType = ScopeType.IRON_SIGHT
    @Volatile var muzzleType: MuzzleType = MuzzleType.NONE
    @Volatile var magType: MagType = MagType.STANDARD
    @Volatile var autoInspect: Boolean = true
    @Volatile var upgradeLevel: Int = 1
    @Volatile var yawDeg: Float = -25f
    @Volatile var pitchDeg: Float = 8f

    private val shader = PbrShaderProgram()
    private val textures = PbrTextureAtlas()
    private lateinit var meshes: MeshLibrary

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val rootMatrix = FloatArray(16)
    private val partMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private var viewportW = 640
    private var viewportH = 420
    private var frameSec = 0f
    private var lastNs = System.nanoTime()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        shader.init()
        meshes = MeshLibrary()
        textures.ensureInitialized(256, true)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportW = width.coerceAtLeast(1)
        viewportH = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, viewportW, viewportH)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastNs) / 1_000_000_000f).coerceIn(0.004f, 0.05f)
        lastNs = now
        frameSec += dt

        val (skinR, skinG, skinB) = resolveSkinTint(skinName)

        GLES20.glClearColor(0.045f, 0.068f, 0.12f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val aspect = viewportW.toFloat() / viewportH.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 36f, aspect, 0.2f, 30f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0.22f, 2.35f, 0f, 0.02f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        GLES20.glUseProgram(shader.worldProgram)
        GLES20.glEnableVertexAttribArray(shader.aPos)
        GLES20.glEnableVertexAttribArray(shader.aNorm)
        GLES20.glEnableVertexAttribArray(shader.aUv)

        GLES20.glUniform1i(shader.uDiffuseTex, 0)
        GLES20.glUniform1i(shader.uNormalTex, 1)
        GLES20.glUniform3f(shader.uCameraPos, 0f, 0.22f, 2.35f)
        GLES20.glUniform3f(shader.uLightDir, 0.55f, 0.70f, 0.45f)
        GLES20.glUniform3f(shader.uSunColor, 1.0f, 0.96f, 0.90f)
        GLES20.glUniform3f(shader.uSkyZenith, 0.16f, 0.30f, 0.52f)
        GLES20.glUniform3f(shader.uSkyHorizon, 0.25f, 0.36f, 0.52f)
        GLES20.glUniform3f(shader.uGroundBounce, skinR * 0.45f, skinG * 0.45f, skinB * 0.45f)
        GLES20.glUniform3f(shader.uFogColor, 0.045f, 0.068f, 0.12f)
        GLES20.glUniform1f(shader.uFogMaxDist, 30f)

        val inspectYaw = if (autoInspect) frameSec * 38f else yawDeg
        val inspectRoll = if (autoInspect) sin(frameSec * 1.9f) * 11f else pitchDeg
        val floatY = sin(frameSec * 2.1f) * 0.03f

        // Base holographic stand ring below firearm
        Matrix.setIdentityM(rootMatrix, 0)
        drawStandPart(0f, -0.34f, 0f, 90f, frameSec * 22f, 0f, 1.35f, 1.35f, 0.025f, skinR, skinG, skinB, 0.80f)

        // Assemble multi-part 3D Weapon Transform
        Matrix.setIdentityM(rootMatrix, 0)
        Matrix.translateM(rootMatrix, 0, 0f, floatY, 0f)
        Matrix.rotateM(rootMatrix, 0, inspectYaw, 0f, 1f, 0f)
        Matrix.rotateM(rootMatrix, 0, inspectRoll, 1f, 0f, 0f)

        val lenFactor = when (weaponCategory) {
            WeaponCategory.SNIPER_RIFLE -> 1.34f
            WeaponCategory.SHOTGUN -> 1.10f
            WeaponCategory.SMG -> 0.80f
            WeaponCategory.PISTOL -> 0.50f
            else -> 1.0f
        }
        val glowStrength = 0.22f + (upgradeLevel - 1).coerceIn(0, 4) * 0.16f

        // 1. Upper & Lower Receiver
        drawWeaponPart(meshes.bevelPack, MaterialId.CORRUGATED_METAL, -0.05f, 0.02f, 0f, 0f, 0f, 0f, 0.72f * lenFactor.coerceAtLeast(0.75f), 0.16f, 0.11f, 0.18f, 0.20f, 0.24f, 0.28f, 0.86f, 0f)
        // Accent anodized receiver shroud (skin finish)
        drawWeaponPart(meshes.unitBox, MaterialId.VEHICLE_COAT, -0.02f, 0.065f, 0f, 0f, 0f, 0f, 0.62f * lenFactor.coerceAtLeast(0.7f), 0.07f, 0.115f, skinR, skinG, skinB, 0.18f, 0.78f, glowStrength)

        // 2. Barrel Assembly
        drawWeaponPart(meshes.rifleBarrel, MaterialId.CORRUGATED_METAL, 0.44f * lenFactor, 0.05f, 0f, 0f, 0f, 0f, 0.62f * lenFactor, 0.055f, 0.055f, 0.24f, 0.26f, 0.30f, 0.20f, 0.92f, 0f)
        if (weaponCategory != WeaponCategory.PISTOL) {
            // Perforated Handguard / Gas Block
            drawWeaponPart(meshes.bevelPack, MaterialId.TACTICAL_FABRIC, 0.30f * lenFactor, 0.045f, 0f, 0f, 0f, 0f, 0.38f * lenFactor, 0.105f, 0.095f, skinR * 0.72f, skinG * 0.72f, skinB * 0.72f, 0.36f, 0.65f, glowStrength * 0.5f)
        }

        // 3. Muzzle Attachment
        val muzzleX = 0.76f * lenFactor
        when (muzzleType) {
            MuzzleType.SUPPRESSOR -> drawWeaponPart(meshes.rifleBarrel, MaterialId.CORRUGATED_METAL, muzzleX + 0.10f, 0.05f, 0f, 0f, 0f, 0f, 0.26f, 0.085f, 0.085f, 0.10f, 0.11f, 0.13f, 0.32f, 0.82f, 0f)
            MuzzleType.COMPENSATOR -> drawWeaponPart(meshes.unitBox, MaterialId.CORRUGATED_METAL, muzzleX + 0.05f, 0.05f, 0f, 0f, 0f, 0f, 0.12f, 0.075f, 0.08f, skinR, skinG, skinB, 0.22f, 0.88f, 0.25f)
            MuzzleType.FLASH_HIDER -> drawWeaponPart(meshes.rifleBarrel, MaterialId.CORRUGATED_METAL, muzzleX + 0.04f, 0.05f, 0f, 0f, 0f, 0f, 0.10f, 0.07f, 0.07f, 0.35f, 0.36f, 0.40f, 0.24f, 0.90f, 0f)
            MuzzleType.NONE -> {}
        }

        // 4. Buttstock (omitted on pistols)
        if (weaponCategory != WeaponCategory.PISTOL) {
            drawWeaponPart(meshes.taperedBoot, MaterialId.TACTICAL_FABRIC, -0.52f * lenFactor.coerceAtMost(1.1f), -0.01f, 0f, 0f, 0f, 0f, 0.44f, 0.16f, 0.09f, 0.15f, 0.16f, 0.19f, 0.55f, 0.25f, 0f)
        }

        // 5. Pistol Grip & Detachable Magazine
        drawWeaponPart(meshes.unitBox, MaterialId.TACTICAL_FABRIC, -0.22f * lenFactor.coerceAtLeast(0.75f), -0.11f, 0f, 0f, 0f, 18f, 0.09f, 0.20f, 0.07f, 0.12f, 0.13f, 0.15f, 0.72f, 0.15f, 0f)
        val magHeight = when (magType) {
            MagType.EXTENDED, MagType.EXTENDED_QUICKDRAW -> 0.30f
            else -> 0.22f
        }
        val magTilt = if (weaponId == "akm") -22f else -10f
        drawWeaponPart(meshes.unitBox, MaterialId.CORRUGATED_METAL, 0.06f, -0.14f, 0f, 0f, 0f, magTilt, 0.11f, magHeight, 0.065f, skinR * 0.85f, skinG * 0.85f, skinB * 0.85f, 0.28f, 0.75f, glowStrength * 0.6f)

        // 6. Optic Scope on Top Rail
        when (scopeType) {
            ScopeType.SCOPE_8X, ScopeType.SCOPE_4X -> {
                val len = if (scopeType == ScopeType.SCOPE_8X) 0.42f else 0.28f
                drawWeaponPart(meshes.rifleBarrel, MaterialId.CORRUGATED_METAL, 0.0f, 0.16f, 0f, 0f, 0f, 0f, len, 0.085f, 0.085f, 0.10f, 0.12f, 0.15f, 0.18f, 0.90f, 0f)
                drawWeaponPart(meshes.curvedVisor, MaterialId.VEHICLE_COAT, len * 0.48f, 0.16f, 0f, 0f, 0f, 0f, 0.04f, 0.075f, 0.075f, 0.1f, 0.88f, 1.0f, 0.08f, 0.9f, 0.8f)
            }
            ScopeType.RED_DOT, ScopeType.HOLO -> {
                drawWeaponPart(meshes.unitBox, MaterialId.CORRUGATED_METAL, 0.02f, 0.145f, 0f, 0f, 0f, 0f, 0.12f, 0.08f, 0.085f, 0.12f, 0.14f, 0.18f, 0.22f, 0.85f, 0f)
                drawWeaponPart(meshes.unitBox, MaterialId.SKIN_DETAIL, 0.02f, 0.15f, 0f, 0f, 0f, 0f, 0.02f, 0.04f, 0.04f, 1.0f, 0.25f, 0.25f, 0.1f, 0f, 0.95f)
            }
            else -> {}
        }

        // 7. Upgrade Level Holographic Crest Stars along Barrel Side
        for (lvlIdx in 0 until upgradeLevel.coerceIn(1, 5)) {
            val sxOff = -0.18f + lvlIdx * 0.075f
            drawWeaponPart(meshes.unitBox, MaterialId.VEHICLE_COAT, sxOff, 0.04f, 0.062f, 0f, 0f, 45f, 0.028f, 0.028f, 0.015f, 1.0f, 0.82f, 0.15f, 0.15f, 0.8f, 0.95f)
        }
    }

    private fun drawWeaponPart(
        mesh: com.example.game.graphics.GlMesh,
        mat: MaterialId,
        tx: Float, ty: Float, tz: Float,
        rx: Float, ry: Float, rz: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float,
        roughness: Float, metallic: Float, emissive: Float
    ) {
        System.arraycopy(rootMatrix, 0, partMatrix, 0, 16)
        Matrix.translateM(partMatrix, 0, tx, ty, tz)
        if (ry != 0f) Matrix.rotateM(partMatrix, 0, ry, 0f, 1f, 0f)
        if (rx != 0f) Matrix.rotateM(partMatrix, 0, rx, 1f, 0f, 0f)
        if (rz != 0f) Matrix.rotateM(partMatrix, 0, rz, 0f, 0f, 1f)
        Matrix.scaleM(partMatrix, 0, sx, sy, sz)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, partMatrix, 0)
        bindAndDraw(mesh, mat, r, g, b, 1f, roughness, metallic, emissive)
    }

    private fun drawStandPart(
        tx: Float, ty: Float, tz: Float, rx: Float, ry: Float, rz: Float,
        sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float, a: Float
    ) {
        Matrix.setIdentityM(partMatrix, 0)
        Matrix.translateM(partMatrix, 0, tx, ty, tz)
        if (ry != 0f) Matrix.rotateM(partMatrix, 0, ry, 0f, 1f, 0f)
        if (rx != 0f) Matrix.rotateM(partMatrix, 0, rx, 1f, 0f, 0f)
        if (rz != 0f) Matrix.rotateM(partMatrix, 0, rz, 0f, 0f, 1f)
        Matrix.scaleM(partMatrix, 0, sx, sy, sz)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, partMatrix, 0)
        bindAndDraw(meshes.wheelCylinder, MaterialId.VEHICLE_COAT, r, g, b, a, 0.25f, 0.5f, 0.65f)
    }

    private fun bindAndDraw(
        mesh: com.example.game.graphics.GlMesh,
        mat: MaterialId,
        r: Float, g: Float, b: Float, a: Float,
        roughness: Float, metallic: Float, emissive: Float
    ) {
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
        GLES20.glUniformMatrix4fv(shader.uModel, 1, false, partMatrix, 0)
        GLES20.glUniform4f(shader.uTintColor, r, g, b, a)
        GLES20.glUniform1f(shader.uRoughness, roughness)
        GLES20.glUniform1f(shader.uMetallic, metallic)
        GLES20.glUniform1f(shader.uEmissive, emissive)
        GLES20.glUniform1f(shader.uUseTex, 0f)
        GLES20.glUniform2f(shader.uUvScale, 1f, 1f)
        GLES20.glUniform2f(shader.uUvOffset, 0f, 0f)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.vertexCount)
    }

    private fun resolveSkinTint(skinName: String): Triple<Float, Float, Float> {
        val lower = skinName.lowercase()
        return when {
            lower.contains("glacier") || lower.contains("frost") || lower.contains("arctic") -> Triple(0.18f, 0.88f, 1.0f)
            lower.contains("dragon") || lower.contains("crimson") || lower.contains("inferno") -> Triple(1.0f, 0.24f, 0.16f)
            lower.contains("gold") || lower.contains("royal") || lower.contains("solar") -> Triple(1.0f, 0.82f, 0.16f)
            lower.contains("neon") || lower.contains("cyber") -> Triple(0.05f, 0.95f, 0.85f)
            lower.contains("cobalt") -> Triple(0.24f, 0.56f, 1.0f)
            else -> Triple(1.0f, 0.45f, 0.08f)
        }
    }
}
