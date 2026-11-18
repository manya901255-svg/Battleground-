package com.example.game.graphics

import android.opengl.GLES20
import android.opengl.Matrix
import com.example.game.model.CharacterPreset
import com.example.game.model.MagType
import com.example.game.model.MuzzleType
import com.example.game.model.PetPreset
import com.example.game.model.Player
import com.example.game.model.PlayerStance
import com.example.game.model.ScopeType
import com.example.game.model.VehicleInstance
import com.example.game.model.VehicleType
import com.example.game.model.WeaponCategory
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders realistic articulated hierarchical 3D human operatives (sculpted jawline, eyes,
 * eyebrows, nose bridge, ears, lips, natural hair/ponytail, tactical jacket collar, MOLLE vest,
 * belt/holster, knee/elbow pads, combat boots, detailed multi-part firearms with reload & recoil
 * kinematics) and multi-part 3D vehicles & aerodynamic pets. Zero blocky/voxel geometry.
 */
class CharacterAndVehicleRenderer(
    private val meshes: MeshLibrary,
    private val textures: PbrTextureAtlas,
    private val shader: PbrShaderProgram
) {
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val scratchMatrix = FloatArray(16)

    // Pre-allocated hierarchy stack & per-player pose blend states (Zero GC churn)
    private val stack = Array(14) { FloatArray(16) }
    private var stackTop = 0

    private data class PlayerAnimState(
        var crouchBlend: Float = 0f,
        var proneBlend: Float = 0f,
        var swimBlend: Float = 0f,
        var jumpBlend: Float = 0f,
        var sprintBlend: Float = 0f,
        var reloadBlend: Float = 0f,
        var turnBankDeg: Float = 0f,
        var lastYawDeg: Float = 0f,
        var initialized: Boolean = false
    )

    private val animStates = HashMap<String, PlayerAnimState>(64)

    fun setViewProjection(vp: FloatArray) {
        System.arraycopy(vp, 0, vpMatrix, 0, 16)
    }

    private fun pushMatrix(base: FloatArray) {
        if (stackTop < stack.size) {
            System.arraycopy(base, 0, stack[stackTop], 0, 16)
            stackTop++
        }
    }

    private fun popMatrix(dest: FloatArray) {
        if (stackTop > 0) {
            stackTop--
            System.arraycopy(stack[stackTop], 0, dest, 0, 16)
        }
    }

    fun renderSoldier3D(
        player: Player,
        groundY: Float,
        userTeamId: Int,
        shadowQuality: Int,
        radarActive: Boolean,
        frameTimeSec: Float,
        armorRgbOverride: Triple<Float, Float, Float>? = null,
        accentRgbOverride: Triple<Float, Float, Float>? = null,
        hairRgbOverride: Triple<Float, Float, Float>? = null,
        showHelmetAndVisor: Boolean = true,
        halfMaskRgb: Triple<Float, Float, Float>? = null,
        halfMaskAccentRgb: Triple<Float, Float, Float>? = null,
        hasPonytailHair: Boolean = false,
        parachuteSkinId: String = "para_flame",
        frameDtSec: Float = 0.016f,
        aimPitchDeg: Float = 0f,
        sunDirX: Float = 0.55f,
        sunDirZ: Float = -0.42f
    ) {
        val px = player.x * TerrainHeightfield.WORLD_SCALE
        val py = groundY + player.z * TerrainHeightfield.WORLD_SCALE
        val pz = player.y * TerrainHeightfield.WORLD_SCALE
        val yaw = -player.angle

        if (animStates.size > 160) animStates.clear()
        val anim = animStates.getOrPut(player.id) { PlayerAnimState(lastYawDeg = player.angle) }

        val preset = CharacterPreset.fromId(player.characterPresetId)
        val isTeammate = player.teamId == userTeamId
        val isFemale = preset.gender.equals("Female", ignoreCase = true)
        val (armorR, armorG, armorB) = armorRgbOverride
            ?: if (player.isUser || isTeammate) preset.armorRgb else Triple(0.46f, 0.14f, 0.14f)
        val (accR, accG, accB) = accentRgbOverride
            ?: if (player.isUser || isTeammate) preset.accentRgb else Triple(0.95f, 0.25f, 0.15f)
        val (visR, visG, visB) = if (player.isUser || isTeammate) preset.visorRgb else Triple(1.0f, 0.15f, 0.15f)

        val skinTone = when (preset) {
            CharacterPreset.VORTEX_GHOST, CharacterPreset.FROST_RECON, CharacterPreset.STREET_MAFIA -> Triple(0.86f, 0.70f, 0.58f)
            CharacterPreset.NOVA_VALKYRIE, CharacterPreset.SERAPH_ECHO, CharacterPreset.KITSUNE_SHRINE, CharacterPreset.VIOLET_BLADE -> Triple(0.90f, 0.75f, 0.65f)
            CharacterPreset.BLAZE_STRIKER, CharacterPreset.CYBER_AFRO -> Triple(0.56f, 0.38f, 0.28f)
            else -> Triple(0.76f, 0.60f, 0.48f)
        }
        val hairRgb = hairRgbOverride ?: when (preset) {
            CharacterPreset.VORTEX_GHOST -> Triple(0.92f, 0.94f, 0.98f)
            CharacterPreset.NOVA_VALKYRIE -> Triple(0.09f, 0.08f, 0.11f)
            CharacterPreset.CYBER_VIPER -> Triple(0.10f, 0.62f, 0.76f)
            CharacterPreset.FROST_RECON -> Triple(0.88f, 0.90f, 0.94f)
            CharacterPreset.SERAPH_ECHO, CharacterPreset.ROYAL_COMMANDER -> Triple(0.88f, 0.72f, 0.28f)
            CharacterPreset.KITSUNE_SHRINE -> Triple(0.92f, 0.88f, 0.92f)
            CharacterPreset.VIOLET_BLADE -> Triple(0.68f, 0.26f, 0.92f)
            CharacterPreset.CYBER_AFRO -> Triple(0.10f, 0.09f, 0.12f)
            CharacterPreset.NEON_SAMURAI -> Triple(0.12f, 0.82f, 0.96f)
            else -> Triple(0.12f, 0.10f, 0.10f)
        }
        val effectiveHalfMaskRgb = halfMaskRgb ?: when (preset.styleTag) {
            "HALF_MASK" -> Triple(0.08f, 0.08f, 0.10f)
            "FLAME_HOOD" -> Triple(0.14f, 0.08f, 0.08f)
            "SAMURAI_CLOAK" -> Triple(0.12f, 0.06f, 0.20f)
            else -> null
        }
        val effectiveHalfMaskAccent = halfMaskAccentRgb ?: when (preset) {
            CharacterPreset.VIOLET_BLADE -> Triple(0.80f, 0.28f, 1.0f)
            CharacterPreset.FLAME_SKULL -> Triple(1.0f, 0.45f, 0.08f)
            CharacterPreset.NEON_SAMURAI -> Triple(0.0f, 0.92f, 1.0f)
            else -> if (effectiveHalfMaskRgb != null) Triple(0.95f, 0.12f, 0.18f) else null
        }
        val effectiveHasPonytail = hasPonytailHair || preset.styleTag == "PONYTAIL" || preset == CharacterPreset.VIOLET_BLADE

        // Soft Contact Shadow + Directional Cast Shadow Ellipse
        if (shadowQuality > 0 && player.vehicleId == null) {
            val altAtten = (1.35f - (player.z * TerrainHeightfield.WORLD_SCALE * 0.08f)).coerceIn(0.4f, 1.35f)
            GLES20.glUniform1f(shader.uSurfaceMode, 3f)
            drawPart(
                mesh = meshes.softShadowDisc,
                mat = MaterialId.GRASS_TERRAIN,
                x = px, y = groundY + 0.045f, z = pz,
                rx = 0f, ry = 0f, rz = 0f,
                sx = altAtten * (if (player.stance == PlayerStance.PRONE) 1.65f else 1.15f),
                sy = 1f,
                sz = altAtten * (if (player.stance == PlayerStance.PRONE) 1.05f else 1.15f),
                r = 0.03f, g = 0.04f, b = 0.06f,
                a = if (shadowQuality >= 2) 0.44f else 0.30f,
                roughness = 0.98f, metallic = 0f, emissive = 0.0f, useTex = 0f
            )
            if (shadowQuality >= 2) {
                val sunYawDeg = -Math.toDegrees(kotlin.math.atan2(sunDirZ.toDouble(), sunDirX.toDouble())).toFloat()
                val shLen = (1.95f - (player.z * TerrainHeightfield.WORLD_SCALE * 0.12f)).coerceIn(0.6f, 2.2f)
                drawPart(
                    mesh = meshes.softShadowDisc,
                    mat = MaterialId.GRASS_TERRAIN,
                    x = px - sunDirX * 0.55f, y = groundY + 0.040f, z = pz - sunDirZ * 0.55f,
                    rx = 0f, ry = sunYawDeg, rz = 0f,
                    sx = shLen * altAtten, sy = 1f, sz = 0.85f * altAtten,
                    r = 0.04f, g = 0.05f, b = 0.08f,
                    a = 0.30f,
                    roughness = 0.98f, metallic = 0f, emissive = 0f, useTex = 0f
                )
            }
            GLES20.glUniform1f(shader.uSurfaceMode, 0f)
        }

        // Smooth Exponential Pose Blending (zero robotic snapping across stance/reload/turn transitions)
        val dt = frameDtSec.coerceIn(0.004f, 0.06f)
        val blendAlpha = (dt * 11f).coerceIn(0.06f, 1.0f)

        val targetProne = if (player.stance == PlayerStance.PRONE) 1f else 0f
        val targetCrouch = if (player.stance == PlayerStance.CROUCH) 1f else 0f
        val targetSwim = if (player.stance == PlayerStance.SWIMMING) 1f else 0f
        val isSkydiving = player.z > 15f
        val targetJump = if (player.stance == PlayerStance.JUMPING || (player.z > 1.5f && !isSkydiving)) 1f else 0f
        val targetSprint = if (player.isSprinting && player.currentSpeed > 40f) 1f else 0f
        val targetReload = if (player.isReloading) 1f else 0f

        var dYaw = player.angle - anim.lastYawDeg
        while (dYaw > 180f) dYaw -= 360f
        while (dYaw < -180f) dYaw += 360f
        anim.lastYawDeg = player.angle
        val targetTurnBank = (dYaw * 0.45f).coerceIn(-12f, 12f)

        if (!anim.initialized) {
            anim.crouchBlend = targetCrouch
            anim.proneBlend = targetProne
            anim.swimBlend = targetSwim
            anim.jumpBlend = targetJump
            anim.sprintBlend = targetSprint
            anim.reloadBlend = targetReload
            anim.turnBankDeg = 0f
            anim.initialized = true
        } else {
            anim.crouchBlend += (targetCrouch - anim.crouchBlend) * blendAlpha
            anim.proneBlend += (targetProne - anim.proneBlend) * blendAlpha
            anim.swimBlend += (targetSwim - anim.swimBlend) * blendAlpha
            anim.jumpBlend += (targetJump - anim.jumpBlend) * blendAlpha
            anim.sprintBlend += (targetSprint - anim.sprintBlend) * blendAlpha
            anim.reloadBlend += (targetReload - anim.reloadBlend) * blendAlpha
            anim.turnBankDeg += (targetTurnBank - anim.turnBankDeg) * (dt * 8f).coerceIn(0.05f, 1f)
        }

        val speedNorm = (player.currentSpeed / 260f).coerceIn(0f, 1.15f)
        val strideSin = sin(player.animWalkPhase) * speedNorm
        val strideCos = cos(player.animWalkPhase) * speedNorm
        val breathe = sin(frameTimeSec * 2.4f) * 0.020f
        val idleSway = cos(frameTimeSec * 1.15f) * (1f - speedNorm).coerceIn(0f, 1f) * 2.2f
        val landDip = if (player.landingRecoverySec > 0f) sin((player.landingRecoverySec / 0.35f) * Math.PI).toFloat() * 0.22f else 0f
        val vaultLift = if (player.climbVaultTimerSec > 0f) sin((player.climbVaultTimerSec / 0.45f) * Math.PI).toFloat() * 0.45f else 0f

        val isProne = anim.proneBlend > 0.5f
        val isCrouch = anim.crouchBlend > 0.5f
        val isSwimming = anim.swimBlend > 0.5f
        val isDriving = player.stance == PlayerStance.DRIVING || player.vehicleId != null
        val isJumping = anim.jumpBlend > 0.5f
        val isEmoting = player.emoteAnimSec > 0f
        val isReloading = player.isReloading || anim.reloadBlend > 0.08f
        val reloadPhase = (player.reloadTimerSec * 1.45f).coerceIn(0f, 3f)
        val reloadOsc = sin(reloadPhase * Math.PI.toFloat()) * anim.reloadBlend
        val fireElapsedMs = System.currentTimeMillis() - player.lastFireTimestampMs
        val recoilKick = if (fireElapsedMs in 0..110L) (1f - fireElapsedMs / 110f) else 0f
        val pitchTrack = aimPitchDeg.coerceIn(-28f, 34f)

        // Compute Blended Root Transform in world space (+X forward, +Z left, -Z right, height ~1.79m-1.84m)
        Matrix.setIdentityM(modelMatrix, 0)
        val standPelvis = 0.92f - landDip + vaultLift + abs(strideSin) * 0.036f
        val pelvisRelY = when {
            isDriving -> 0.62f
            else -> {
                var yBlend = standPelvis
                yBlend = yBlend * (1f - anim.crouchBlend) + (0.58f - landDip) * anim.crouchBlend
                yBlend = yBlend * (1f - anim.proneBlend) + 0.19f * anim.proneBlend
                yBlend = yBlend * (1f - anim.swimBlend) + (-0.08f + sin(frameTimeSec * 4.5f) * 0.04f) * anim.swimBlend
                yBlend
            }
        }
        val pelvisY = py + pelvisRelY
        Matrix.translateM(modelMatrix, 0, px, pelvisY, pz)
        Matrix.rotateM(modelMatrix, 0, yaw + strideSin * 4.5f + idleSway, 0f, 1f, 0f)
        if (abs(anim.turnBankDeg) > 0.1f && !isDriving) {
            Matrix.rotateM(modelMatrix, 0, anim.turnBankDeg, 1f, 0f, 0f)
        }

        val pronePitch = (anim.proneBlend.coerceAtLeast(anim.swimBlend)) * 78f
        val skyPitch = if (isSkydiving) 36f else 0f
        val runLeanPitch = if (!isDriving && speedNorm > 0.06f) (5.0f + anim.sprintBlend * 7.0f) * speedNorm else 0f
        val bodyFwdPitch = pronePitch.coerceAtLeast(skyPitch).coerceAtLeast(runLeanPitch)
        if (bodyFwdPitch > 0.2f) {
            Matrix.rotateM(modelMatrix, 0, bodyFwdPitch, 0f, 0f, -1f)
        }

        val pantsColor = Triple(armorR * 0.44f + 0.06f, armorG * 0.44f + 0.07f, armorB * 0.44f + 0.09f)

        // Sculpted Pelvis / Hip Bridge (seamless waist-to-hip anatomy) + Contoured Tactical Belt & Buckle
        drawLocalMesh(
            meshes.pelvisHip, MaterialId.TACTICAL_FABRIC,
            tx = 0f, ty = 0.01f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = if (isFemale) 0.255f else 0.295f,
            sy = 0.21f,
            sz = if (isFemale) 0.39f else 0.46f,
            r = pantsColor.first, g = pantsColor.second, b = pantsColor.third,
            roughness = 0.72f, metallic = 0.05f
        )
        drawLocalMesh(
            meshes.curvedBand, MaterialId.TACTICAL_FABRIC,
            tx = 0f, ty = 0.055f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = if (isFemale) 0.235f else 0.275f,
            sy = 0.065f,
            sz = if (isFemale) 0.36f else 0.43f,
            r = 0.10f, g = 0.11f, b = 0.13f,
            roughness = 0.46f, metallic = 0.28f
        )
        drawLocalMesh(
            meshes.humanHandGlove, MaterialId.CORRUGATED_METAL,
            tx = if (isFemale) 0.118f else 0.136f, ty = 0.055f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.032f, sy = 0.055f, sz = 0.095f,
            r = accR * 0.85f, g = accG * 0.85f, b = accB * 0.85f,
            roughness = 0.24f, metallic = 0.82f
        )

        // --- HIPS, ARTICULATED LEGS, KNEE PADS & COMBAT BOOTS (Smoothly Blended, Flush Grounding at -0.92m) ---
        val runHipSpan = 30f + anim.sprintBlend * 14f
        val standHipL = strideSin * runHipSpan
        val standHipR = -strideSin * runHipSpan
        val crouchHipL = -46f + strideSin * 24f
        val crouchHipR = -46f - strideSin * 24f
        val leftHipPitch = when {
            isDriving -> -72f
            else -> {
                var h = standHipL * (1f - anim.crouchBlend) + crouchHipL * anim.crouchBlend
                h = h * (1f - anim.proneBlend) + (8f + strideSin * 10f) * anim.proneBlend
                h = h * (1f - anim.swimBlend) + (sin(frameTimeSec * 9f) * 26f) * anim.swimBlend
                h * (1f - anim.jumpBlend) + 30f * anim.jumpBlend
            }
        }
        val rightHipPitch = when {
            isDriving -> -72f
            else -> {
                var h = standHipR * (1f - anim.crouchBlend) + crouchHipR * anim.crouchBlend
                h = h * (1f - anim.proneBlend) + (8f - strideSin * 10f) * anim.proneBlend
                h = h * (1f - anim.swimBlend) + (-sin(frameTimeSec * 9f) * 26f) * anim.swimBlend
                h * (1f - anim.jumpBlend) + (-16f) * anim.jumpBlend
            }
        }
        val standKneeL = (-strideCos * 28f).coerceAtLeast(6f)
        val standKneeR = (strideCos * 28f).coerceAtLeast(6f)
        val leftKneeBend = when {
            isDriving -> 78f
            else -> {
                val baseK = standKneeL * (1f - anim.crouchBlend) + 74f * anim.crouchBlend
                (baseK * (1f - anim.proneBlend) + (-12f) * anim.proneBlend) * (1f - anim.jumpBlend) + 46f * anim.jumpBlend
            }
        }
        val rightKneeBend = when {
            isDriving -> 78f
            else -> {
                val baseK = standKneeR * (1f - anim.crouchBlend) + 74f * anim.crouchBlend
                (baseK * (1f - anim.proneBlend) + (-12f) * anim.proneBlend) * (1f - anim.jumpBlend) + 22f * anim.jumpBlend
            }
        }

        val hipZ = if (isFemale) 0.105f else 0.120f
        renderLeg(+hipZ, leftHipPitch, leftKneeBend, isFemale, pantsColor, accR, accG, accB, hasHolster = false)
        renderLeg(-hipZ, rightHipPitch, rightKneeBend, isFemale, pantsColor, accR, accG, accB, hasHolster = true)

        // --- TORSO, JACKET COLLAR, CONTOURED TACTICAL VEST & RUCKSACK ---
        pushMatrix(modelMatrix)
        val standLean = breathe * 58f + recoilKick * 3.0f + pitchTrack * 0.22f
        val torsoLean = when {
            isEmoting -> sin(frameTimeSec * 8f) * 9f
            else -> {
                val base = standLean * (1f - anim.crouchBlend) + (15f + pitchTrack * 0.22f) * anim.crouchBlend
                base * (1f - anim.proneBlend) + (-14f) * anim.proneBlend
            }
        }
        Matrix.rotateM(modelMatrix, 0, torsoLean, 0f, 0f, -1f)

        val torsoWidthScale = if (isFemale) 0.41f else 0.49f
        val torsoDepthScale = if (isFemale) 0.255f else 0.295f
        drawLocalMesh(
            meshes.chestTorso, MaterialId.TACTICAL_FABRIC,
            tx = 0f, ty = 0.32f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = torsoDepthScale, sy = 0.56f, sz = torsoWidthScale,
            r = armorR, g = armorG, b = armorB,
            roughness = 0.66f, metallic = 0.14f
        )
        // Raised Tactical Jacket Collar
        drawLocalMesh(
            meshes.jacketCollar, MaterialId.TACTICAL_FABRIC,
            tx = -0.01f, ty = 0.595f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = torsoDepthScale * 0.72f, sy = 0.095f, sz = torsoWidthScale * 0.54f,
            r = armorR * 0.85f, g = armorG * 0.85f, b = armorB * 0.85f,
            roughness = 0.60f, metallic = 0.16f
        )

        // Contoured Ballistic Plate Carrier Vest + Rounded Magazine Pouches (Lv.1..Lv.4)
        if (player.armorLevel > 0) {
            val (vR, vG, vB) = when (player.armorLevel) {
                1 -> Triple(0.12f, 0.13f, 0.16f)
                2 -> Triple(0.76f, 0.42f, 0.12f)
                3 -> Triple(0.12f, 0.56f, 0.84f)
                else -> Triple(0.18f, 0.36f, 0.76f)
            }
            drawLocalMesh(
                meshes.contouredVest, MaterialId.TACTICAL_FABRIC,
                tx = 0f, ty = 0.34f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = torsoDepthScale + (if (player.armorLevel >= 4) 0.068f else 0.048f),
                sy = 0.38f,
                sz = torsoWidthScale + (if (player.armorLevel >= 4) 0.048f else 0.028f),
                r = vR, g = vG, b = vB,
                roughness = 0.44f, metallic = if (player.armorLevel >= 3) 0.48f else 0.32f
            )
            for (pouchZ in listOf(-0.088f, 0f, 0.088f)) {
                drawLocalMesh(
                    meshes.tacticalBackpack, MaterialId.TACTICAL_FABRIC,
                    tx = torsoDepthScale * 0.5f + 0.032f, ty = 0.24f, tz = pouchZ,
                    rx = 0f, ry = 180f, rz = 0f,
                    sx = 0.052f, sy = 0.105f, sz = 0.072f,
                    r = if (player.armorLevel >= 4) 0.98f else 0.11f,
                    g = if (player.armorLevel >= 4) 0.84f else 0.12f,
                    b = if (player.armorLevel >= 4) 0.12f else 0.14f,
                    roughness = 0.35f, metallic = if (player.armorLevel >= 4) 0.6f else 0.22f
                )
            }
            drawLocalMesh(
                meshes.curvedVisor, MaterialId.VEHICLE_COAT,
                tx = torsoDepthScale * 0.5f - 0.085f, ty = 0.41f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.26f, sy = 0.22f, sz = torsoWidthScale * 0.68f,
                r = if (player.armorLevel >= 4) 1.0f else accR,
                g = if (player.armorLevel >= 4) 0.85f else accG,
                b = if (player.armorLevel >= 4) 0.15f else accB,
                roughness = 0.20f, metallic = 0.68f, emissive = if (player.armorLevel >= 4) 0.78f else 0.48f
            )
        }

        // Character Style Signature Overcoat/Cape/Ember Silhouettes
        when (preset.styleTag) {
            "MAFIA_CAPE", "SAMURAI_CLOAK" -> {
                val capeRgb = if (preset == CharacterPreset.NEON_SAMURAI) Triple(0.75f, 0.12f, 0.78f) else Triple(0.86f, 0.86f, 0.88f)
                drawLocalMesh(
                    meshes.bannerClothStrip, MaterialId.TACTICAL_FABRIC,
                    tx = -0.19f, ty = 0.15f, tz = 0f,
                    rx = 0f, ry = 90f, rz = -8f + sin(frameTimeSec * 3.8f) * 6f,
                    sx = 0.48f, sy = 0.72f, sz = 0.24f,
                    r = capeRgb.first, g = capeRgb.second, b = capeRgb.third,
                    roughness = 0.55f, metallic = 0.18f, emissive = if (preset == CharacterPreset.NEON_SAMURAI) 0.35f else 0.05f
                )
            }
            "FLAME_HOOD" -> {
                for (sz in listOf(-0.22f, 0.22f)) {
                    drawLocalMesh(
                        meshes.foliageCone, MaterialId.VEHICLE_COAT,
                        tx = 0f, ty = 0.60f, tz = sz,
                        rx = 0f, ry = 0f, rz = 12f,
                        sx = 0.14f, sy = 0.22f, sz = 0.14f,
                        r = 1.0f, g = 0.42f, b = 0.06f,
                        roughness = 0.2f, metallic = 0.1f, emissive = 0.92f
                    )
                }
            }
            "CROWN_COAT" -> {
                drawLocalMesh(
                    meshes.jacketCollar, MaterialId.VEHICLE_COAT,
                    tx = -0.02f, ty = 0.63f, tz = 0f,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = torsoDepthScale * 0.82f, sy = 0.13f, sz = torsoWidthScale * 0.62f,
                    r = 1.0f, g = 0.80f, b = 0.18f,
                    roughness = 0.22f, metallic = 0.85f, emissive = 0.35f
                )
            }
        }

        // Contoured Backpack (Lv1..Lv3) & Slung Secondary Rifle
        if (player.backpackLevel > 0 && !isDriving) {
            val (bagR, bagG, bagB) = when (player.backpackLevel) {
                1 -> Triple(0.78f, 0.58f, 0.24f)
                2 -> Triple(0.92f, 0.42f, 0.08f)
                else -> Triple(0.24f, 0.44f, 0.20f)
            }
            val bagScale = 0.16f + (player.backpackLevel.coerceIn(1, 3) - 1) * 0.038f
            drawLocalMesh(
                meshes.tacticalBackpack, MaterialId.TACTICAL_FABRIC,
                tx = -0.20f, ty = 0.34f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = bagScale, sy = 0.40f + (player.backpackLevel - 1) * 0.04f, sz = 0.29f + (player.backpackLevel - 1) * 0.035f,
                r = bagR, g = bagG, b = bagB,
                roughness = 0.62f, metallic = 0.16f
            )
            drawLocalMesh(
                meshes.curvedBand, MaterialId.VEHICLE_COAT,
                tx = -0.22f - bagScale * 0.42f, ty = 0.38f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.045f, sy = 0.095f, sz = 0.19f,
                r = accR, g = accG, b = accB,
                roughness = 0.28f, metallic = 0.55f, emissive = 0.42f
            )
            if (player.isUser || player.primaryWeapon2 != null) {
                drawLocalMesh(
                    meshes.rifleBarrel, MaterialId.CORRUGATED_METAL,
                    tx = -0.28f, ty = 0.46f, tz = -0.09f,
                    rx = 0f, ry = 0f, rz = 65f,
                    sx = 0.80f, sy = 0.056f, sz = 0.056f,
                    r = 0.18f, g = 0.22f, b = 0.26f,
                    roughness = 0.28f, metallic = 0.85f
                )
                drawLocalMesh(
                    meshes.weaponReceiver, MaterialId.VEHICLE_COAT,
                    tx = -0.28f, ty = 0.48f, tz = -0.09f,
                    rx = 0f, ry = 0f, rz = 65f,
                    sx = 0.48f, sy = 0.048f, sz = 0.040f,
                    r = accR, g = accG, b = accB,
                    roughness = 0.22f, metallic = 0.68f, emissive = 0.50f
                )
            }
        }

        // --- NECK & REALISTIC ADULT-PROPORTION HUMAN HEAD (~24cm, 1:7.3 scale) + FACE + HAIR ---
        pushMatrix(modelMatrix)
        Matrix.translateM(modelMatrix, 0, 0f, 0.61f, 0f)
        if (isSwimming || isProne) {
            Matrix.rotateM(modelMatrix, 0, -64f, 0f, 0f, -1f)
        } else if (abs(pitchTrack) > 0.5f) {
            Matrix.rotateM(modelMatrix, 0, pitchTrack * 0.45f, 0f, 0f, 1f)
        }
        val headMicroYaw = sin(frameTimeSec * 0.9f) * 4.5f * (1f - speedNorm).coerceIn(0f, 1f)
        Matrix.rotateM(modelMatrix, 0, headMicroYaw, 0f, 1f, 0f)

        // Neck
        drawLocalMesh(
            meshes.limbCapsule, MaterialId.SKIN_DETAIL,
            tx = 0f, ty = 0.02f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.110f, sy = 0.145f, sz = 0.110f,
            r = skinTone.first * 0.92f, g = skinTone.second * 0.92f, b = skinTone.third * 0.92f,
            roughness = 0.62f, metallic = 0f
        )
        // Sculpted Head Cranium, Cheekbones & Jawline
        drawLocalMesh(
            meshes.humanHead, MaterialId.SKIN_DETAIL,
            tx = 0.008f, ty = 0.165f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = if (isFemale) 0.232f else 0.248f,
            sy = if (isFemale) 0.250f else 0.266f,
            sz = if (isFemale) 0.212f else 0.226f,
            r = skinTone.first, g = skinTone.second, b = skinTone.third,
            roughness = 0.54f, metallic = 0.02f
        )
        // Sculpted Ears
        for (earSide in listOf(-1f, 1f)) {
            drawLocalMesh(
                meshes.humanEar, MaterialId.SKIN_DETAIL,
                tx = -0.006f, ty = 0.165f, tz = earSide * 0.102f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.032f, sy = 0.052f, sz = 0.018f,
                r = skinTone.first * 0.90f, g = skinTone.second * 0.88f, b = skinTone.third * 0.88f,
                roughness = 0.58f, metallic = 0f
            )
        }

        // Realistic 4-Layer Eyes, Eyebrows, Nose Bridge & Lips
        val fullFaceVisor = showHelmetAndVisor && (player.helmetLevel >= 3)
        if (!fullFaceVisor) {
            drawLocalMesh(
                meshes.humanNose, MaterialId.SKIN_DETAIL,
                tx = 0.122f, ty = 0.155f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.032f, sy = 0.048f, sz = 0.030f,
                r = skinTone.first * 0.95f, g = skinTone.second * 0.92f, b = skinTone.third * 0.90f,
                roughness = 0.52f, metallic = 0f
            )
            for (eyeSide in listOf(-1f, 1f)) {
                val ez = eyeSide * 0.041f
                drawLocalMesh(
                    meshes.humanEyeOrb, MaterialId.SKIN_DETAIL,
                    tx = 0.105f, ty = 0.184f, tz = ez,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.022f, sy = 0.019f, sz = 0.028f,
                    r = 0.95f, g = 0.95f, b = 0.97f,
                    roughness = 0.14f, metallic = 0.05f, useTex = 0f
                )
                drawLocalMesh(
                    meshes.humanEyeOrb, MaterialId.SKIN_DETAIL,
                    tx = 0.114f, ty = 0.184f, tz = ez,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.014f, sy = 0.015f, sz = 0.018f,
                    r = 0.22f + accR * 0.18f, g = 0.34f + accG * 0.12f, b = 0.44f + accB * 0.12f,
                    roughness = 0.08f, metallic = 0.15f, useTex = 0f
                )
                drawLocalMesh(
                    meshes.humanEyeOrb, MaterialId.SKIN_DETAIL,
                    tx = 0.118f, ty = 0.184f, tz = ez,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.009f, sy = 0.009f, sz = 0.010f,
                    r = 0.04f, g = 0.04f, b = 0.06f,
                    roughness = 0.06f, metallic = 0.10f, useTex = 0f
                )
                drawLocalMesh(
                    meshes.humanEyeOrb, MaterialId.SKIN_DETAIL,
                    tx = 0.121f, ty = 0.187f, tz = ez + 0.003f,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.004f, sy = 0.004f, sz = 0.004f,
                    r = 1.0f, g = 1.0f, b = 1.0f,
                    roughness = 0.06f, metallic = 0f, emissive = 0.6f, useTex = 0f
                )
                drawLocalMesh(
                    meshes.facialMouth, MaterialId.TACTICAL_FABRIC,
                    tx = 0.113f, ty = 0.209f, tz = ez,
                    rx = eyeSide * -6f, ry = 0f, rz = 0f,
                    sx = 0.018f, sy = 0.010f, sz = 0.038f,
                    r = hairRgb.first * 0.65f, g = hairRgb.second * 0.65f, b = hairRgb.third * 0.65f,
                    roughness = 0.78f, metallic = 0f, useTex = 0f
                )
            }
            if (effectiveHalfMaskRgb == null) {
                drawLocalMesh(
                    meshes.facialMouth, MaterialId.SKIN_DETAIL,
                    tx = 0.112f, ty = 0.102f, tz = 0f,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.021f, sy = 0.013f, sz = 0.042f,
                    r = (skinTone.first * 0.80f).coerceAtMost(0.86f),
                    g = skinTone.second * 0.54f,
                    b = skinTone.third * 0.52f,
                    roughness = 0.38f, metallic = 0.05f, useTex = 0f
                )
            }
        }

        // Layered Hair Crown + Bangs + Ponytail / Kitsune Ears / Royal Crown Diadem
        val hairScaleX = if (preset.styleTag == "CYBER_AFRO") 0.38f else 0.262f
        val hairScaleY = if (preset.styleTag == "CYBER_AFRO") 0.34f else 0.238f
        val hairScaleZ = if (preset.styleTag == "CYBER_AFRO") 0.36f else 0.240f
        drawLocalMesh(
            meshes.humanHair, MaterialId.TACTICAL_FABRIC,
            tx = -0.008f, ty = if (preset.styleTag == "CYBER_AFRO") 0.230f else 0.205f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = hairScaleX, sy = hairScaleY, sz = hairScaleZ,
            r = hairRgb.first, g = hairRgb.second, b = hairRgb.third,
            roughness = 0.66f, metallic = 0.08f
        )
        if (preset.styleTag != "CYBER_AFRO") {
            drawLocalMesh(
                meshes.hairBangs, MaterialId.TACTICAL_FABRIC,
                tx = 0.008f, ty = 0.200f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.256f, sy = 0.192f, sz = 0.234f,
                r = hairRgb.first, g = hairRgb.second, b = hairRgb.third,
                roughness = 0.64f, metallic = 0.08f
            )
        }
        if (preset.styleTag == "KITSUNE") {
            for (earSide in listOf(-0.080f, 0.080f)) {
                drawLocalMesh(
                    meshes.kitsuneEar, MaterialId.TACTICAL_FABRIC,
                    tx = 0.015f, ty = 0.345f, tz = earSide,
                    rx = earSide * 190f, ry = 0f, rz = 0f,
                    sx = 0.080f, sy = 0.160f, sz = 0.080f,
                    r = 0.96f, g = 0.94f, b = 0.96f,
                    roughness = 0.55f, metallic = 0.10f
                )
                drawLocalMesh(
                    meshes.kitsuneEar, MaterialId.VEHICLE_COAT,
                    tx = 0.030f, ty = 0.330f, tz = earSide,
                    rx = earSide * 190f, ry = 0f, rz = 0f,
                    sx = 0.044f, sy = 0.102f, sz = 0.044f,
                    r = 0.94f, g = 0.12f, b = 0.22f,
                    roughness = 0.35f, metallic = 0.2f, emissive = 0.35f
                )
            }
        }
        if (preset.styleTag == "CROWN_COAT") {
            drawLocalMesh(
                meshes.crownDiadem, MaterialId.VEHICLE_COAT,
                tx = 0f, ty = 0.325f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.205f, sy = 0.075f, sz = 0.190f,
                r = 1.0f, g = 0.82f, b = 0.18f,
                roughness = 0.18f, metallic = 0.90f, emissive = 0.48f
            )
        }
        if (effectiveHasPonytail) {
            val sway = sin(frameTimeSec * 3.1f + speedNorm * 2f) * (5f + speedNorm * 8f)
            drawLocalMesh(
                meshes.hairPonytail, MaterialId.TACTICAL_FABRIC,
                tx = -0.155f, ty = 0.200f, tz = 0f,
                rx = 0f, ry = 0f, rz = -42f + sway,
                sx = 0.145f, sy = 0.28f, sz = 0.145f,
                r = hairRgb.first, g = hairRgb.second, b = hairRgb.third,
                roughness = 0.66f, metallic = 0.10f
            )
            drawLocalMesh(
                meshes.hairPonytail, MaterialId.VEHICLE_COAT,
                tx = -0.235f, ty = 0.105f, tz = 0f,
                rx = 0f, ry = 0f, rz = -58f + sway * 1.25f,
                sx = 0.118f, sy = 0.175f, sz = 0.118f,
                r = accR, g = accG, b = accB,
                roughness = 0.52f, metallic = 0.14f, emissive = 0.24f
            )
        }

        // Contoured Tactical Half-Mask over lower jaw & nose bridge
        if (effectiveHalfMaskRgb != null) {
            drawLocalMesh(
                meshes.contouredHalfMask, MaterialId.TACTICAL_FABRIC,
                tx = 0.012f, ty = 0.112f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.238f, sy = 0.115f, sz = 0.228f,
                r = effectiveHalfMaskRgb.first, g = effectiveHalfMaskRgb.second, b = effectiveHalfMaskRgb.third,
                roughness = 0.48f, metallic = 0.25f
            )
            val mAcc = effectiveHalfMaskAccent ?: Triple(0.95f, 0.12f, 0.18f)
            drawLocalMesh(
                meshes.curvedVisor, MaterialId.VEHICLE_COAT,
                tx = 0.022f, ty = 0.118f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.225f, sy = 0.045f, sz = 0.215f,
                r = mAcc.first, g = mAcc.second, b = mAcc.third,
                roughness = 0.25f, metallic = 0.45f, emissive = 0.55f, useTex = 0f
            )
        }

        // Tactical Helmets Lv1-4 (All Smooth Curved Shells)
        val hideSignatureHairHelmet =
            (preset == CharacterPreset.VORTEX_GHOST || preset == CharacterPreset.NOVA_VALKYRIE || preset == CharacterPreset.KITSUNE_SHRINE) && player.helmetLevel < 3
        if (showHelmetAndVisor && !hideSignatureHairHelmet && player.helmetLevel > 0) {
            val (hR, hG, hB) = when (player.helmetLevel) {
                1 -> Triple(0.18f, 0.36f, 0.22f)
                2 -> Triple(0.42f, 0.44f, 0.26f)
                3 -> Triple(0.24f, 0.26f, 0.30f)
                else -> Triple(0.12f, 0.14f, 0.22f)
            }
            drawLocalMesh(
                meshes.helmetShell, MaterialId.CORRUGATED_METAL,
                tx = -0.008f, ty = 0.195f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.288f, sy = 0.240f, sz = 0.266f,
                r = hR, g = hG, b = hB,
                roughness = 0.28f, metallic = 0.62f
            )
            if (player.helmetLevel == 1) {
                drawLocalMesh(
                    meshes.tacticalCapBrim, MaterialId.TACTICAL_FABRIC,
                    tx = 0.075f, ty = 0.205f, tz = 0f,
                    rx = 0f, ry = 0f, rz = -4f,
                    sx = 0.28f, sy = 0.09f, sz = 0.24f,
                    r = 0.12f, g = 0.24f, b = 0.14f,
                    roughness = 0.5f, metallic = 0.1f
                )
            } else if (player.helmetLevel == 2) {
                drawLocalMesh(
                    meshes.curvedVisor, MaterialId.VEHICLE_COAT,
                    tx = 0.044f, ty = 0.230f, tz = 0f,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.252f, sy = 0.102f, sz = 0.230f,
                    r = 0.94f, g = 0.56f, b = 0.12f,
                    roughness = 0.15f, metallic = 0.72f, emissive = 0.25f
                )
            } else if (player.helmetLevel == 3) {
                drawLocalMesh(
                    meshes.curvedVisor, MaterialId.CORRUGATED_METAL,
                    tx = 0.036f, ty = 0.175f, tz = 0f,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.252f, sy = 0.190f, sz = 0.236f,
                    r = 0.28f, g = 0.30f, b = 0.34f,
                    roughness = 0.24f, metallic = 0.85f, emissive = 0.12f
                )
            } else {
                drawLocalMesh(
                    meshes.contouredHalfMask, MaterialId.CORRUGATED_METAL,
                    tx = 0.022f, ty = 0.140f, tz = 0f,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.260f, sy = 0.190f, sz = 0.244f,
                    r = 0.14f, g = 0.16f, b = 0.22f,
                    roughness = 0.22f, metallic = 0.82f
                )
                for (ez in listOf(-0.046f, 0.046f)) {
                    drawLocalMesh(
                        meshes.humanEyeOrb, MaterialId.SKIN_DETAIL,
                        tx = 0.145f, ty = 0.188f, tz = ez,
                        rx = 0f, ry = 0f, rz = 0f,
                        sx = 0.032f, sy = 0.026f, sz = 0.038f,
                        r = 1.0f, g = 0.10f, b = 0.14f,
                        roughness = 0.1f, metallic = 0.1f, emissive = 1.0f, useTex = 0f
                    )
                }
            }
        }
        popMatrix(modelMatrix)

        // --- SHOULDERS, ARMS, ARTICULATED FINGERS & TWO-HANDED HELD WEAPON ALIGNMENT ---
        val heldWeapon = player.activeWeapon
        val hasHeldWeapon = heldWeapon != null && !isDriving && !isSwimming && !isSkydiving && !isEmoting
        val isMeleeHeld = heldWeapon?.category == WeaponCategory.MELEE

        val rightArmRaise = when {
            isEmoting -> 125f + sin(frameTimeSec * 10f) * 26f
            isDriving -> 55f
            isSwimming -> 75f + sin(frameTimeSec * 7f) * 65f
            isProne && hasHeldWeapon -> 54f
            hasHeldWeapon && isMeleeHeld -> 54f + recoilKick * 48f
            hasHeldWeapon -> (48f + pitchTrack * 0.50f + recoilKick * 10f) * (1f - anim.reloadBlend) + (40f + reloadOsc * 6f) * anim.reloadBlend
            else -> -strideSin * (26f + anim.sprintBlend * 12f)
        }
        val leftArmRaise = when {
            isEmoting -> 45f + cos(frameTimeSec * 8f) * 20f
            isDriving -> 55f
            isSwimming -> 75f - sin(frameTimeSec * 7f) * 65f
            isProne && hasHeldWeapon -> 58f
            hasHeldWeapon && isMeleeHeld -> 18f
            hasHeldWeapon -> (56f + pitchTrack * 0.45f + recoilKick * 8f) * (1f - anim.reloadBlend) + (34f + reloadOsc * 22f) * anim.reloadBlend
            else -> strideSin * (26f + anim.sprintBlend * 12f)
        }
        val rightArmYaw = when {
            hasHeldWeapon && !isMeleeHeld -> -18f
            hasHeldWeapon && isMeleeHeld -> -10f
            else -> -4f
        }
        val leftArmYaw = when {
            hasHeldWeapon && !isMeleeHeld -> 36f
            else -> 4f
        }
        val leftForearmYaw = if (hasHeldWeapon && !isMeleeHeld) 16f else 0f
        val rightForearmYaw = if (hasHeldWeapon && !isMeleeHeld) -14f else 0f
        val rightElbowBend = if (hasHeldWeapon && !isMeleeHeld) 44f else 22f
        val leftElbowBend = if (hasHeldWeapon && !isMeleeHeld) 32f else 26f

        val shoulderOffsetZ = if (isFemale) 0.205f else 0.238f
        renderArm(+shoulderOffsetZ, leftArmRaise, leftArmYaw, leftElbowBend, leftForearmYaw, armorR, armorG, armorB, skinTone)
        renderArm(-shoulderOffsetZ, rightArmRaise, rightArmYaw, rightElbowBend, rightForearmYaw, armorR, armorG, armorB, skinTone)

        if (heldWeapon != null && !isDriving && !isSwimming && !isSkydiving && !isEmoting) {
            pushMatrix(modelMatrix)
            if (isMeleeHeld) {
                Matrix.translateM(modelMatrix, 0, 0.28f, 0.34f, -0.18f)
                Matrix.rotateM(modelMatrix, 0, -25f + recoilKick * 58f, 0f, 0f, 1f)
                val (mR, mG, mB) = resolveWeaponSkinColor(heldWeapon.skinName, accR, accG, accB)
                when (heldWeapon.id.lowercase()) {
                    "katana" -> {
                        drawLocalMesh(meshes.ergoGrip, MaterialId.TACTICAL_FABRIC, 0f, 0f, 0f, 0f, 0f, 0f, 0.22f, 0.050f, 0.050f, 0.12f, 0.12f, 0.15f, 0.6f, 0.2f)
                        drawLocalMesh(meshes.katanaBlade, MaterialId.VEHICLE_COAT, 0.48f, 0.02f, 0f, 0f, 0f, 8f, 0.90f, 0.40f, 0.95f, 0.35f, 0.85f, 1.0f, 0.14f, 0.92f, 0.65f)
                    }
                    "scythe" -> {
                        drawLocalMesh(meshes.rifleBarrel, MaterialId.CORRUGATED_METAL, 0.30f, 0f, 0f, 0f, 0f, 0f, 1.05f, 0.042f, 0.042f, 0.18f, 0.18f, 0.22f, 0.3f, 0.85f)
                        drawLocalMesh(meshes.crescentScythe, MaterialId.VEHICLE_COAT, 0.80f, 0.05f, 0f, 0f, 0f, -20f, 0.60f, 0.60f, 0.75f, 1.0f, 0.18f, 0.25f, 0.16f, 0.88f, 0.68f)
                    }
                    "bat" -> {
                        drawLocalMesh(meshes.rifleBarrel, MaterialId.TACTICAL_FABRIC, 0.34f, 0f, 0f, 0f, 0f, 6f, 0.82f, 0.088f, 0.088f, 0.88f, 0.58f, 0.14f, 0.35f, 0.4f, 0.15f)
                    }
                    "machete" -> {
                        drawLocalMesh(meshes.ergoGrip, MaterialId.TACTICAL_FABRIC, 0f, 0f, 0f, 0f, 0f, 0f, 0.17f, 0.048f, 0.048f, 0.14f, 0.14f, 0.16f, 0.6f, 0.2f)
                        drawLocalMesh(meshes.katanaBlade, MaterialId.CORRUGATED_METAL, 0.35f, 0.02f, 0f, 0f, 0f, 3f, 0.56f, 0.52f, 0.78f, 0.78f, 0.82f, 0.88f, 0.18f, 0.92f)
                    }
                    else -> {
                        drawLocalMesh(meshes.ergoGrip, MaterialId.TACTICAL_FABRIC, 0.06f, 0f, 0f, 0f, 0f, 0f, 0.22f, 0.044f, 0.044f, 0.12f, 0.12f, 0.14f, 0.6f, 0.2f)
                        drawLocalMesh(meshes.wheelCylinder, MaterialId.VEHICLE_COAT, 0.34f, 0f, 0f, 90f, 0f, 0f, 0.38f, 0.34f, 0.075f, mR, mG, mB, 0.22f, 0.88f, 0.25f)
                    }
                }
            } else {
                Matrix.translateM(modelMatrix, 0, 0.26f - recoilKick * 0.06f, 0.37f + recoilKick * 0.02f, -0.13f)
                Matrix.rotateM(modelMatrix, 0, -15f, 0f, 1f, 0f)
                if (abs(pitchTrack) > 0.5f) {
                    Matrix.rotateM(modelMatrix, 0, pitchTrack * 0.58f, 0f, 0f, 1f)
                }
                if (isReloading) {
                    Matrix.rotateM(modelMatrix, 0, reloadOsc * 16f, 1f, 0f, 0f)
                    Matrix.rotateM(modelMatrix, 0, -20f * anim.reloadBlend, 0f, 1f, 0f)
                } else {
                    Matrix.rotateM(modelMatrix, 0, recoilKick * 6.5f, 0f, 0f, 1f)
                }

                val weaponLenScale = when (heldWeapon.category) {
                    WeaponCategory.SNIPER_RIFLE -> 1.40f
                    WeaponCategory.SHOTGUN -> 1.08f
                    WeaponCategory.SMG -> 0.76f
                    WeaponCategory.PISTOL -> 0.42f
                    else -> 0.96f
                }
                val (skinR, skinG, skinB) = resolveWeaponSkinColor(heldWeapon.skinName, accR, accG, accB)

                drawLocalMesh(
                    meshes.weaponReceiver, MaterialId.CORRUGATED_METAL,
                    tx = 0.11f * weaponLenScale, ty = 0f, tz = 0f,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.46f * weaponLenScale.coerceAtLeast(0.55f), sy = 0.125f, sz = 0.088f,
                    r = 0.16f, g = 0.18f, b = 0.22f,
                    roughness = 0.28f, metallic = 0.85f
                )
                drawLocalMesh(
                    meshes.opticBody, MaterialId.VEHICLE_COAT,
                    tx = 0.17f * weaponLenScale, ty = 0.068f, tz = 0f,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.36f * weaponLenScale.coerceAtLeast(0.55f), sy = 0.034f, sz = 0.072f,
                    r = skinR, g = skinG, b = skinB,
                    roughness = 0.18f, metallic = 0.76f, emissive = 0.62f
                )
                if (heldWeapon.id.equals("m1887", ignoreCase = true)) {
                    for (bz in listOf(-0.024f, 0.024f)) {
                        drawLocalMesh(meshes.rifleBarrel, MaterialId.CORRUGATED_METAL, 0.46f * weaponLenScale, 0.02f, bz, 0f, 0f, 0f, 0.44f * weaponLenScale, 0.040f, 0.040f, 0.25f, 0.26f, 0.30f, 0.2f, 0.92f)
                    }
                } else {
                    drawLocalMesh(meshes.rifleBarrel, MaterialId.CORRUGATED_METAL, 0.46f * weaponLenScale, 0.02f, 0f, 0f, 0f, 0f, 0.44f * weaponLenScale, 0.048f, 0.048f, 0.22f, 0.24f, 0.28f, 0.20f, 0.92f)
                }
                if (heldWeapon.equippedMuzzle == MuzzleType.SUPPRESSOR) {
                    drawLocalMesh(meshes.rifleBarrel, MaterialId.CORRUGATED_METAL, 0.72f * weaponLenScale, 0.02f, 0f, 0f, 0f, 0f, 0.20f, 0.072f, 0.072f, 0.10f, 0.11f, 0.13f, 0.30f, 0.82f)
                }
                if (heldWeapon.category != WeaponCategory.PISTOL) {
                    drawLocalMesh(meshes.tacticalStock, MaterialId.TACTICAL_FABRIC, -0.20f * weaponLenScale.coerceAtMost(1.1f), -0.02f, 0f, 0f, 0f, 0f, 0.28f, 0.13f, 0.074f, 0.14f, 0.15f, 0.18f, 0.55f, 0.25f)
                }
                drawLocalMesh(meshes.ergoGrip, MaterialId.TACTICAL_FABRIC, -0.02f, -0.092f, 0f, 0f, 0f, 18f, 0.065f, 0.14f, 0.056f, 0.12f, 0.13f, 0.15f, 0.68f, 0.15f)
                val magDropY = if (isReloading) -abs(reloadOsc) * 0.10f else 0f
                drawLocalMesh(
                    meshes.curvedMagazine, MaterialId.CORRUGATED_METAL,
                    tx = 0.14f, ty = -0.105f + magDropY, tz = 0f,
                    rx = 0f, ry = 0f, rz = -8f,
                    sx = if (heldWeapon.category == WeaponCategory.SHOTGUN) 0.13f else 0.085f,
                    sy = if (heldWeapon.category == WeaponCategory.PISTOL) 0.13f else 0.195f,
                    sz = 0.056f,
                    r = skinR * 0.68f, g = skinG * 0.68f, b = skinB * 0.68f,
                    roughness = 0.36f, metallic = 0.74f
                )
                if (heldWeapon.category == WeaponCategory.SNIPER_RIFLE || heldWeapon.equippedScope != ScopeType.IRON_SIGHT) {
                    drawLocalMesh(meshes.rifleBarrel, MaterialId.CORRUGATED_METAL, 0.13f, 0.118f, 0f, 0f, 0f, 0f, if (heldWeapon.category == WeaponCategory.SNIPER_RIFLE) 0.32f else 0.17f, 0.056f, 0.056f, 0.10f, 0.12f, 0.16f, 0.15f, 0.90f)
                    drawLocalMesh(meshes.humanEyeOrb, MaterialId.VEHICLE_COAT, 0.22f, 0.118f, 0f, 0f, 0f, 0f, 0.026f, 0.048f, 0.048f, 0.12f, 0.88f, 1.0f, 0.08f, 0.9f, emissive = 0.82f, useTex = 0f)
                    if (heldWeapon.skinName.contains("Butterfly", ignoreCase = true)) {
                        val flapW = sin(frameTimeSec * 4.5f) * 8f
                        drawLocalMesh(meshes.butterflyWing, MaterialId.VEHICLE_COAT, 0.24f, 0.16f, 0.028f, flapW, 0f, 0f, 0.34f, 0.32f, 0.34f, 1.0f, 0.12f, 0.26f, 0.18f, 0.3f, emissive = 0.85f, alpha = 0.86f)
                        drawLocalMesh(meshes.butterflyWing, MaterialId.VEHICLE_COAT, 0.24f, 0.16f, -0.028f, -flapW, 0f, 0f, 0.34f, 0.32f, -0.34f, 1.0f, 0.12f, 0.26f, 0.18f, 0.3f, emissive = 0.85f, alpha = 0.86f)
                    }
                }
                if (fireElapsedMs in 0..90L) {
                    drawLocalMesh(
                        meshes.grassTuft, MaterialId.SKIN_DETAIL,
                        tx = 0.74f * weaponLenScale + 0.08f, ty = 0.02f, tz = 0f,
                        rx = frameTimeSec * 480f, ry = 0f, rz = -90f,
                        sx = 0.32f, sy = 0.42f, sz = 0.32f,
                        r = 1.0f, g = 0.82f, b = 0.22f,
                        roughness = 0.5f, metallic = 0f, emissive = 1.0f, useTex = 0f, alpha = 0.94f
                    )
                }
            }
            popMatrix(modelMatrix)
        }

        popMatrix(modelMatrix)

        // Radar Pulse Ring
        if (!isTeammate && radarActive) {
            drawPart(
                meshes.softShadowDisc, MaterialId.VEHICLE_COAT,
                px, py + 0.10f, pz, 0f, frameTimeSec * 90f, 0f, 1.75f, 1f, 1.75f,
                1.0f, 0.18f, 0.15f, 0.70f, roughness = 0.4f, metallic = 0f, emissive = 1f, useTex = 0f
            )
        }

        // Inflated Ram-Air Parachute Canopy OR Aero Glider Wings during Skydiving
        if (isSkydiving) {
            val isGlider = parachuteSkinId.contains("glider", ignoreCase = true)
            val (pR, pG, pB) = when {
                parachuteSkinId.contains("matrix") -> Triple(0.0f, 0.90f, 0.52f)
                parachuteSkinId.contains("royal") -> Triple(1.0f, 0.76f, 0.12f)
                isGlider -> Triple(0.95f, 0.14f, 0.26f)
                else -> Triple(1.0f, 0.44f, 0.06f)
            }
            if (isGlider) {
                drawPart(meshes.sweptAircraftWing, MaterialId.VEHICLE_COAT, px, py + 1.65f, pz, 0f, yaw, 0f, 2.8f, 0.35f, 5.4f, pR, pG, pB, 0.96f, 0.25f, 0.4f, 0.42f)
            } else {
                drawPart(meshes.curvedParachuteCanopy, MaterialId.TACTICAL_FABRIC, px, py + 4.10f, pz, 0f, yaw, 0f, 4.8f, 1.65f, 2.75f, pR, pG, pB, 0.95f, 0.62f, 0.06f, 0.22f)
                val rad = Math.toRadians(player.angle.toDouble())
                val rxV = -sin(rad).toFloat()
                val rzV = cos(rad).toFloat()
                drawPart(meshes.columnBeam, MaterialId.CORRUGATED_METAL, px + rxV * 0.95f, py + 2.25f, pz + rzV * 0.95f, 0f, yaw, -16f, 0.045f, 3.6f, 0.045f, 0.22f, 0.24f, 0.26f, 0.92f, 0.5f, 0.4f, 0f, 0f)
                drawPart(meshes.columnBeam, MaterialId.CORRUGATED_METAL, px - rxV * 0.95f, py + 2.25f, pz - rzV * 0.95f, 0f, yaw, 16f, 0.045f, 3.6f, 0.045f, 0.22f, 0.24f, 0.26f, 0.92f, 0.5f, 0.4f, 0f, 0f)
            }
        }
    }

    /**
     * Articulated human skydiving figure under a ram-air parachute canopy (used for distant/background skydivers).
     */
    fun renderArticulatedParachutist(
        x: Float,
        y: Float,
        z: Float,
        yawDeg: Float,
        swayDeg: Float = 0f,
        canopyR: Float = 0.16f,
        canopyG: Float = 0.62f,
        canopyB: Float = 0.95f,
        scale: Float = 1.0f
    ) {
        drawPart(meshes.curvedParachuteCanopy, MaterialId.TACTICAL_FABRIC, x, y + 2.15f * scale, z, 0f, yawDeg, swayDeg, 3.1f * scale, 1.05f * scale, 1.75f * scale, canopyR, canopyG, canopyB, 0.96f, 0.52f, 0.08f, 0.22f)
        // Risers
        drawPart(meshes.columnBeam, MaterialId.CORRUGATED_METAL, x + 0.55f * scale, y + 1.15f * scale, z, 0f, yawDeg, -18f + swayDeg, 0.035f * scale, 2.0f * scale, 0.035f * scale, 0.20f, 0.22f, 0.25f, 0.92f, 0.5f, 0.3f, 0f, 0f)
        drawPart(meshes.columnBeam, MaterialId.CORRUGATED_METAL, x - 0.55f * scale, y + 1.15f * scale, z, 0f, yawDeg, 18f + swayDeg, 0.035f * scale, 2.0f * scale, 0.035f * scale, 0.20f, 0.22f, 0.25f, 0.92f, 0.5f, 0.3f, 0f, 0f)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, swayDeg, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale)

        // Articulated Torso + Pelvis + Head + Raised Arms + Bent Legs
        drawLocalMesh(meshes.pelvisHip, MaterialId.TACTICAL_FABRIC, 0f, 0f, 0f, 0f, 0f, 0f, 0.27f, 0.20f, 0.42f, 0.16f, 0.17f, 0.20f, 0.72f, 0.05f)
        drawLocalMesh(meshes.chestTorso, MaterialId.TACTICAL_FABRIC, 0f, 0.31f, 0f, 0f, 0f, 0f, 0.28f, 0.54f, 0.46f, 0.14f, 0.16f, 0.20f, 0.65f, 0.14f)
        drawLocalMesh(meshes.humanHead, MaterialId.SKIN_DETAIL, 0.01f, 0.74f, 0f, 0f, 0f, 0f, 0.24f, 0.26f, 0.22f, 0.78f, 0.62f, 0.50f, 0.54f, 0.02f)
        drawLocalMesh(meshes.helmetShell, MaterialId.CORRUGATED_METAL, 0f, 0.77f, 0f, 0f, 0f, 0f, 0.28f, 0.24f, 0.26f, canopyR * 0.75f, canopyG * 0.75f, canopyB * 0.75f, 0.30f, 0.60f)
        renderArm(+0.22f, 152f, 16f, 18f, 0f, 0.14f, 0.16f, 0.20f, Triple(0.78f, 0.62f, 0.50f))
        renderArm(-0.22f, 152f, -16f, 18f, 0f, 0.14f, 0.16f, 0.20f, Triple(0.78f, 0.62f, 0.50f))
        renderLeg(+0.115f, -20f, 34f, false, Triple(0.14f, 0.15f, 0.18f), canopyR, canopyG, canopyB, false)
        renderLeg(-0.115f, -20f, 34f, false, Triple(0.14f, 0.15f, 0.18f), canopyR, canopyG, canopyB, false)
    }

    private fun renderLeg(
        hipOffsetZ: Float,
        hipPitchDeg: Float,
        kneeBendDeg: Float,
        isFemale: Boolean,
        pantsRgb: Triple<Float, Float, Float>,
        accR: Float, accG: Float, accB: Float,
        hasHolster: Boolean
    ) {
        pushMatrix(modelMatrix)
        Matrix.translateM(modelMatrix, 0, 0f, -0.02f, hipOffsetZ)
        Matrix.rotateM(modelMatrix, 0, hipPitchDeg, 0f, 0f, 1f)
        val thighRadius = if (isFemale) 0.165f else 0.185f
        val calfRadius = if (isFemale) 0.135f else 0.148f
        // Thigh
        drawLocalMesh(
            meshes.limbCapsule, MaterialId.TACTICAL_FABRIC,
            tx = 0f, ty = -0.21f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = thighRadius, sy = 0.42f, sz = thighRadius,
            r = pantsRgb.first, g = pantsRgb.second, b = pantsRgb.third,
            roughness = 0.72f, metallic = 0.05f
        )
        // Contoured Thigh Strap
        drawLocalMesh(
            meshes.curvedBand, MaterialId.VEHICLE_COAT,
            tx = 0f, ty = -0.17f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = thighRadius + 0.014f, sy = 0.042f, sz = thighRadius + 0.014f,
            r = accR, g = accG, b = accB,
            roughness = 0.35f, metallic = 0.28f, emissive = 0.12f
        )
        if (hasHolster) {
            drawLocalMesh(
                meshes.kneePadShell, MaterialId.TACTICAL_FABRIC,
                tx = 0.01f, ty = -0.19f, tz = -thighRadius * 0.52f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.095f, sy = 0.145f, sz = 0.046f,
                r = 0.10f, g = 0.11f, b = 0.13f,
                roughness = 0.5f, metallic = 0.2f
            )
        }
        // Knee Joint + Armor Knee Pad + Calf
        Matrix.translateM(modelMatrix, 0, 0f, -0.42f, 0f)
        drawLocalMesh(
            meshes.kneePadShell, MaterialId.CORRUGATED_METAL,
            tx = 0.072f, ty = 0f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.055f, sy = 0.090f, sz = 0.115f,
            r = 0.14f, g = 0.16f, b = 0.19f,
            roughness = 0.40f, metallic = 0.45f
        )
        Matrix.rotateM(modelMatrix, 0, -kneeBendDeg, 0f, 0f, 1f)
        drawLocalMesh(
            meshes.limbCapsule, MaterialId.TACTICAL_FABRIC,
            tx = 0f, ty = -0.195f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = calfRadius, sy = 0.39f, sz = calfRadius,
            r = pantsRgb.first * 0.88f, g = pantsRgb.second * 0.88f, b = pantsRgb.third * 0.88f,
            roughness = 0.72f, metallic = 0.05f
        )
        // High-Top Sculpted Sneaker + Ankle Collar + Contoured Outsole (Bottom flush at -0.480f -> total leg -0.920m)
        drawLocalMesh(
            meshes.taperedBoot, MaterialId.VEHICLE_COAT,
            tx = 0.04f, ty = -0.40f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.22f, sy = 0.12f, sz = 0.135f,
            r = 0.94f, g = 0.95f, b = 0.97f,
            roughness = 0.40f, metallic = 0.12f
        )
        drawLocalMesh(
            meshes.curvedBand, MaterialId.VEHICLE_COAT,
            tx = 0f, ty = -0.34f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.145f, sy = 0.058f, sz = 0.140f,
            r = accR, g = accG, b = accB,
            roughness = 0.34f, metallic = 0.22f, emissive = 0.08f
        )
        drawLocalMesh(
            meshes.contouredSole, MaterialId.VEHICLE_COAT,
            tx = 0.04f, ty = -0.464f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.228f, sy = 0.032f, sz = 0.140f,
            r = accR * 0.85f, g = accG * 0.85f, b = accB * 0.85f,
            roughness = 0.35f, metallic = 0.25f
        )
        popMatrix(modelMatrix)
    }

    private fun renderArm(
        shoulderOffsetZ: Float,
        armRaiseDeg: Float,
        armYawDeg: Float,
        elbowBendDeg: Float,
        forearmYawDeg: Float,
        armorR: Float, armorG: Float, armorB: Float,
        skinTone: Triple<Float, Float, Float>
    ) {
        pushMatrix(modelMatrix)
        Matrix.translateM(modelMatrix, 0, 0.01f, 0.51f, shoulderOffsetZ)
        // Shoulder Pauldron
        drawLocalMesh(
            meshes.kneePadShell, MaterialId.TACTICAL_FABRIC,
            tx = 0f, ty = 0f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.145f, sy = 0.13f, sz = 0.145f,
            r = armorR * 0.9f, g = armorG * 0.9f, b = armorB * 0.9f,
            roughness = 0.45f, metallic = 0.3f
        )
        if (armYawDeg != 0f) {
            Matrix.rotateM(modelMatrix, 0, armYawDeg, 0f, 1f, 0f)
        }
        Matrix.rotateM(modelMatrix, 0, armRaiseDeg, 0f, 0f, 1f)
        // Upper Arm
        drawLocalMesh(
            meshes.limbCapsule, MaterialId.TACTICAL_FABRIC,
            tx = 0f, ty = -0.132f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.122f, sy = 0.265f, sz = 0.122f,
            r = armorR, g = armorG, b = armorB,
            roughness = 0.65f, metallic = 0.1f
        )
        // Elbow + Forearm + Wrist Glove Palm + Articulated Fingers & Thumb
        Matrix.translateM(modelMatrix, 0, 0f, -0.265f, 0f)
        drawLocalMesh(
            meshes.kneePadShell, MaterialId.TACTICAL_FABRIC,
            tx = -0.032f, ty = 0f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.072f, sy = 0.072f, sz = 0.080f,
            r = 0.12f, g = 0.13f, b = 0.15f,
            roughness = 0.45f, metallic = 0.35f
        )
        if (forearmYawDeg != 0f) {
            Matrix.rotateM(modelMatrix, 0, forearmYawDeg, 0f, 1f, 0f)
        }
        Matrix.rotateM(modelMatrix, 0, elbowBendDeg, 0f, 0f, 1f)
        drawLocalMesh(
            meshes.limbCapsule, MaterialId.SKIN_DETAIL,
            tx = 0f, ty = -0.118f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.102f, sy = 0.236f, sz = 0.102f,
            r = skinTone.first, g = skinTone.second, b = skinTone.third,
            roughness = 0.56f, metallic = 0.02f
        )
        drawLocalMesh(
            meshes.humanHandGlove, MaterialId.TACTICAL_FABRIC,
            tx = 0f, ty = -0.242f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.092f, sy = 0.080f, sz = 0.084f,
            r = 0.11f, g = 0.11f, b = 0.14f,
            roughness = 0.46f, metallic = 0.24f
        )
        val handSideSign = if (shoulderOffsetZ > 0f) 1f else -1f
        drawLocalMesh(
            meshes.sculptedFingers, MaterialId.SKIN_DETAIL,
            tx = 0.008f, ty = -0.295f, tz = 0f,
            rx = 0f, ry = if (handSideSign > 0f) 90f else -90f, rz = 0f,
            sx = 0.076f, sy = 0.064f, sz = 0.072f,
            r = skinTone.first * 0.94f, g = skinTone.second * 0.92f, b = skinTone.third * 0.90f,
            roughness = 0.54f, metallic = 0.02f
        )
        drawLocalMesh(
            meshes.sculptedThumb, MaterialId.SKIN_DETAIL,
            tx = 0.032f, ty = -0.260f, tz = -handSideSign * 0.032f,
            rx = handSideSign * -22f, ry = 0f, rz = 26f,
            sx = 0.025f, sy = 0.050f, sz = 0.025f,
            r = skinTone.first * 0.94f, g = skinTone.second * 0.92f, b = skinTone.third * 0.90f,
            roughness = 0.54f, metallic = 0.02f
        )
        popMatrix(modelMatrix)
    }

    fun renderCompanionPet(owner: Player, groundY: Float, pet: PetPreset, frameTimeSec: Float) {
        val rad = Math.toRadians(owner.angle.toDouble())
        val fx = cos(rad).toFloat()
        val fz = sin(rad).toFloat()
        val rx = -fz
        val rz = fx
        val yaw = -owner.angle
        val (br, bg, bb) = pet.bodyRgb
        val (gr, gg, gb) = pet.glowRgb

        when (pet.modelShape) {
            "CANINE", "DRAGON" -> {
                val wolfX = owner.x * TerrainHeightfield.WORLD_SCALE - fx * 0.22f + rx * 0.92f
                val bob = abs(sin(frameTimeSec * 4.5f)) * (if (owner.currentSpeed > 25f) 0.08f else 0.015f)
                val wolfY = groundY + owner.z * TerrainHeightfield.WORLD_SCALE + 0.50f + bob
                val wolfZ = owner.y * TerrainHeightfield.WORLD_SCALE - fz * 0.22f + rz * 0.92f
                val tailWag = sin(frameTimeSec * 8f) * 14f

                drawPart(meshes.chestTorso, MaterialId.TACTICAL_FABRIC, wolfX, wolfY, wolfZ, 0f, yaw, 84f, 0.42f, 0.78f, 0.36f, br, bg, bb, 1f, 0.55f, 0.25f, 0.12f)
                drawPart(meshes.jacketCollar, MaterialId.VEHICLE_COAT, wolfX + fx * 0.24f, wolfY + 0.14f, wolfZ + fz * 0.24f, 0f, yaw, 0f, 0.32f, 0.18f, 0.34f, gr, gg, gb, 1f, 0.22f, 0.65f, 0.78f, 0f)
                drawPart(meshes.humanHead, MaterialId.TACTICAL_FABRIC, wolfX + fx * 0.42f, wolfY + 0.26f, wolfZ + fz * 0.42f, 0f, yaw, 0f, 0.34f, 0.30f, 0.28f, br * 1.15f, bg * 1.15f, bb * 1.15f, 1f, 0.5f, 0.2f, 0.1f)
                drawPart(meshes.taperedBoot, MaterialId.TACTICAL_FABRIC, wolfX + fx * 0.62f, wolfY + 0.21f, wolfZ + fz * 0.62f, 0f, yaw, 0f, 0.22f, 0.13f, 0.15f, 0.92f, 0.92f, 0.95f, 1f, 0.5f, 0.1f)
                for (earSide in listOf(-0.10f, 0.10f)) {
                    drawPart(meshes.foliageCone, MaterialId.VEHICLE_COAT, wolfX + fx * 0.36f + rx * earSide, wolfY + 0.44f, wolfZ + fz * 0.36f + rz * earSide, 0f, yaw, 0f, 0.08f, 0.18f, 0.08f, gr, gg, gb, 1f, 0.3f, 0.4f, 0.55f, 0f)
                    drawPart(meshes.humanEyeOrb, MaterialId.SKIN_DETAIL, wolfX + fx * 0.56f + rx * earSide * 0.6f, wolfY + 0.29f, wolfZ + fz * 0.56f + rz * earSide * 0.6f, 0f, yaw, 0f, 0.035f, 0.035f, 0.035f, gr, gg, gb, 1f, 0.1f, 0f, 1.0f, 0f)
                }
                for ((lx, lz) in listOf(Pair(0.24f, 0.13f), Pair(0.24f, -0.13f), Pair(-0.24f, 0.13f), Pair(-0.24f, -0.13f))) {
                    drawPart(meshes.limbCapsule, MaterialId.TACTICAL_FABRIC, wolfX + fx * lx + rx * lz, wolfY - 0.26f, wolfZ + fz * lx + rz * lz, 0f, yaw, 0f, 0.11f, 0.42f, 0.11f, br, bg, bb, 1f, 0.6f, 0.2f)
                    drawPart(meshes.contouredSole, MaterialId.VEHICLE_COAT, wolfX + fx * lx + rx * lz, wolfY - 0.44f, wolfZ + fz * lx + rz * lz, 0f, yaw, 0f, 0.14f, 0.08f, 0.14f, gr, gg, gb, 1f, 0.3f, 0.4f, 0.65f, 0f)
                }
                drawPart(meshes.sweptAircraftWing, MaterialId.VEHICLE_COAT, wolfX - fx * 0.46f, wolfY + 0.18f, wolfZ - fz * 0.46f, 0f, yaw + tailWag, -32f, 0.34f, 0.16f, 0.28f, gr, gg, gb, 0.96f, 0.22f, 0.3f, 0.92f, 0f)
            }
            "PANDA" -> {
                val px = owner.x * TerrainHeightfield.WORLD_SCALE - fx * 0.24f + rx * 0.88f
                val bob = abs(sin(frameTimeSec * 4.2f)) * 0.05f
                val py = groundY + owner.z * TerrainHeightfield.WORLD_SCALE + 0.36f + bob
                val pz = owner.y * TerrainHeightfield.WORLD_SCALE - fz * 0.24f + rz * 0.88f
                // Plump white body + black chest band + bamboo vest + round black ears
                drawPart(meshes.humanHead, MaterialId.TACTICAL_FABRIC, px, py, pz, 0f, yaw, 0f, 0.46f, 0.46f, 0.42f, 0.96f, 0.96f, 0.98f, 1f, 0.6f, 0.05f)
                drawPart(meshes.chestTorso, MaterialId.TACTICAL_FABRIC, px, py + 0.04f, pz, 0f, yaw, 0f, 0.48f, 0.18f, 0.44f, 0.10f, 0.10f, 0.12f, 1f, 0.7f, 0.05f)
                drawPart(meshes.humanHead, MaterialId.TACTICAL_FABRIC, px, py + 0.34f, pz, 0f, yaw, 0f, 0.42f, 0.36f, 0.38f, 0.96f, 0.96f, 0.98f, 1f, 0.6f, 0.05f)
                for (s in listOf(-0.14f, 0.14f)) {
                    drawPart(meshes.humanEyeOrb, MaterialId.TACTICAL_FABRIC, px + rx * s, py + 0.54f, pz + rz * s, 0f, yaw, 0f, 0.13f, 0.13f, 0.08f, 0.08f, 0.08f, 0.10f, 1f, 0.7f, 0.05f)
                    drawPart(meshes.humanEyeOrb, MaterialId.SKIN_DETAIL, px + fx * 0.18f + rx * s * 0.55f, py + 0.36f, pz + fz * 0.18f + rz * s * 0.55f, 0f, yaw, 0f, 0.06f, 0.07f, 0.04f, 0.08f, 0.08f, 0.10f, 1f, 0.4f, 0.1f, 0f, 0f)
                }
            }
            "PENGUIN" -> {
                val px = owner.x * TerrainHeightfield.WORLD_SCALE - fx * 0.24f + rx * 0.88f
                val bob = abs(sin(frameTimeSec * 4.8f)) * 0.05f
                val py = groundY + owner.z * TerrainHeightfield.WORLD_SCALE + 0.36f + bob
                val pz = owner.y * TerrainHeightfield.WORLD_SCALE - fz * 0.24f + rz * 0.88f
                // Navy back + white belly + orange beak + tactical cyan goggles + beanie cap
                drawPart(meshes.chestTorso, MaterialId.TACTICAL_FABRIC, px, py + 0.10f, pz, 0f, yaw, 0f, 0.38f, 0.54f, 0.36f, 0.10f, 0.14f, 0.24f, 1f, 0.48f, 0.15f)
                drawPart(meshes.chamferedStructure, MaterialId.SKIN_DETAIL, px + fx * 0.12f, py + 0.08f, pz + fz * 0.12f, 0f, yaw, 0f, 0.14f, 0.42f, 0.26f, 0.96f, 0.96f, 0.98f, 1f, 0.5f, 0.05f, 0f, 0f)
                drawPart(meshes.humanNose, MaterialId.VEHICLE_COAT, px + fx * 0.24f, py + 0.26f, pz + fz * 0.24f, 0f, yaw, 0f, 0.12f, 0.08f, 0.10f, 1.0f, 0.62f, 0.08f, 1f, 0.3f, 0.1f, 0.15f, 0f)
                drawPart(meshes.curvedVisor, MaterialId.VEHICLE_COAT, px + fx * 0.08f, py + 0.32f, pz + fz * 0.08f, 0f, yaw, 0f, 0.28f, 0.14f, 0.28f, gr, gg, gb, 1f, 0.15f, 0.8f, 0.72f, 0f)
            }
            "CYBER_ORB" -> {
                // Spherical Hovering Cyber Cat-Ear Robo Orb with cyan LED eyes + 4 orbiting satellites!
                val hover = sin(frameTimeSec * 4.2f) * 0.16f
                val px = owner.x * TerrainHeightfield.WORLD_SCALE - fx * 0.52f - rx * 1.05f
                val py = groundY + owner.z * TerrainHeightfield.WORLD_SCALE + 1.38f + hover
                val pz = owner.y * TerrainHeightfield.WORLD_SCALE - fz * 0.52f - rz * 1.05f
                drawPart(meshes.radomeSphere, MaterialId.CORRUGATED_METAL, px, py, pz, 0f, yaw, 0f, 0.52f, 0.50f, 0.52f, br, bg, bb, 1f, 0.22f, 0.85f, 0.18f)
                for (ear in listOf(-0.16f, 0.16f)) {
                    drawPart(meshes.kitsuneEar, MaterialId.CORRUGATED_METAL, px + rx * ear, py + 0.26f, pz + rz * ear, ear * 80f, yaw, 0f, 0.16f, 0.22f, 0.16f, br, bg, bb, 1f, 0.25f, 0.82f, 0.22f)
                    drawPart(meshes.humanEyeOrb, MaterialId.VEHICLE_COAT, px + fx * 0.24f + rx * ear * 0.58f, py + 0.04f, pz + fz * 0.24f + rz * ear * 0.58f, 0f, yaw, 0f, 0.055f, 0.075f, 0.055f, gr, gg, gb, 1f, 0.1f, 0.2f, 1.0f, 0f)
                }
                for (k in 0..3) {
                    val oAng = frameTimeSec * 3.4f + k * (Math.PI.toFloat() * 0.5f)
                    val ox = px + cos(oAng) * 0.48f
                    val oz = pz + sin(oAng) * 0.48f
                    drawPart(meshes.radomeSphere, MaterialId.VEHICLE_COAT, ox, py - 0.08f, oz, 0f, 0f, 0f, 0.11f, 0.11f, 0.11f, gr, gg, gb, 1f, 0.15f, 0.7f, 0.95f, 0f)
                }
            }
            else -> {
                val hover = sin(frameTimeSec * 4.5f) * 0.18f
                val petX = owner.x * TerrainHeightfield.WORLD_SCALE - fx * 0.72f - rx * 1.15f
                val petY = groundY + owner.z * TerrainHeightfield.WORLD_SCALE + 1.40f + hover
                val petZ = owner.y * TerrainHeightfield.WORLD_SCALE - fz * 0.72f - rz * 1.15f
                val flap = sin(frameTimeSec * 12f) * 24f

                drawPart(meshes.humanHead, MaterialId.VEHICLE_COAT, petX, petY, petZ, 0f, yaw, 0f, 0.46f, 0.42f, 0.42f, br, bg, bb, 1f, 0.35f, 0.45f, 0.15f)
                drawPart(meshes.sweptAircraftWing, MaterialId.VEHICLE_COAT, petX, petY + 0.12f, petZ, 0f, yaw, flap, 0.36f, 0.14f, 1.15f, gr, gg, gb, 0.94f, 0.25f, 0.6f, 0.7f)
                drawPart(meshes.curvedVisor, MaterialId.SKIN_DETAIL, petX + fx * 0.12f, petY + 0.06f, petZ + fz * 0.12f, 0f, yaw, 0f, 0.32f, 0.18f, 0.28f, 1.0f, 0.92f, 0.25f, 1f, 0.1f, 0.2f, 1f, 0f)
            }
        }
    }

    fun renderVehicle3D(
        v: VehicleInstance,
        groundY: Float,
        frameTimeSec: Float,
        shadowQuality: Int,
        vehicleSkinId: String = "vehicle_uaz_camo"
    ) {
        val vx = v.x * TerrainHeightfield.WORLD_SCALE
        val vz = v.y * TerrainHeightfield.WORLD_SCALE
        val rot = -v.rotationAngle
        val isDestroyed = v.isDestroyed

        val (cr, cg, cb) = when {
            isDestroyed -> Triple(0.18f, 0.18f, 0.20f)
            v.type == VehicleType.DACIA_SEDAN && vehicleSkinId.contains("gold") -> Triple(0.98f, 0.78f, 0.16f)
            v.type == VehicleType.DACIA_SEDAN && vehicleSkinId.contains("crimson") -> Triple(0.88f, 0.12f, 0.18f)
            v.type == VehicleType.MOTORBIKE -> Triple(0.08f, 0.85f, 0.96f)
            v.type == VehicleType.UAZ_JEEP -> Triple(0.84f, 0.86f, 0.88f)
            v.type == VehicleType.BUGGY -> Triple(0.95f, 0.56f, 0.10f)
            v.type == VehicleType.SPEEDBOAT -> Triple(0.16f, 0.54f, 0.85f)
            else -> Triple(0.14f, 0.44f, 0.78f)
        }

        val rad = Math.toRadians(v.rotationAngle.toDouble())
        val fx = cos(rad).toFloat()
        val fz = sin(rad).toFloat()
        val rx = -fz
        val rz = fx
        val bounce = if (v.currentSpeed > 12f) sin(frameTimeSec * 18f) * 0.04f else 0f
        val wheelSpin = -(frameTimeSec * v.currentSpeed * 11f) % 360f

        if (shadowQuality > 0) {
            drawPart(
                meshes.softShadowDisc, MaterialId.GRASS_TERRAIN,
                vx, groundY + 0.04f, vz, 0f, rot, 0f, 5.2f, 1f, 2.8f,
                0.04f, 0.05f, 0.06f, 0.50f, 0.98f, 0f, 0.75f, 0f
            )
        }

        if (v.type == VehicleType.MOTORBIKE) {
            drawPart(meshes.chassisSlopeBody, MaterialId.VEHICLE_COAT, vx, groundY + 0.74f + bounce, vz, 0f, rot, 0f, 2.3f, 0.52f, 0.54f, 0.14f, 0.12f, 0.22f, 1f, 0.18f, 0.82f, 0.18f)
            drawPart(meshes.chamferedStructure, MaterialId.VEHICLE_COAT, vx, groundY + 0.84f + bounce, vz, 0f, rot, 0f, 1.9f, 0.14f, 0.56f, cr, cg, cb, 1f, 0.15f, 0.6f, 0.82f, 0f)
            drawPart(meshes.curvedVisor, MaterialId.VEHICLE_COAT, vx + fx * 0.62f, groundY + 1.05f + bounce, vz + fz * 0.62f, 0f, rot, 0f, 0.45f, 0.45f, 0.42f, cr, cg, cb, 0.88f, 0.08f, 0.92f, 0.45f)
            drawWheel(vx + fx * 0.95f, groundY + 0.42f, vz + fz * 0.95f, rot, wheelSpin, 0.82f, 0.24f)
            drawWheel(vx - fx * 0.95f, groundY + 0.42f, vz - fz * 0.95f, rot, wheelSpin, 0.86f, 0.28f)
            return
        }

        // Supercar rear aerodynamic carbon wing for Dacia/Hypercar skins
        if (v.type == VehicleType.DACIA_SEDAN) {
            drawPart(meshes.chamferedStructure, MaterialId.CORRUGATED_METAL, vx - fx * 2.05f, groundY + 1.38f + bounce, vz - fz * 2.05f, 0f, rot, -10f, 0.42f, 0.08f, 2.05f, 0.12f, 0.12f, 0.15f, 1f, 0.22f, 0.85f)
        }

        // Main Sculpted Lower Chassis & Sloped Hood
        drawPart(
            meshes.chassisSlopeBody, MaterialId.VEHICLE_COAT,
            vx, groundY + 0.76f + bounce, vz, 0f, rot, 0f,
            4.55f, 0.88f, 2.15f,
            cr, cg, cb, 1f, roughness = 0.22f, metallic = 0.72f
        )
        // Cab Roof / Rollcage
        val cabScaleX = if (v.type == VehicleType.UAZ_JEEP) 2.75f else 2.25f
        drawPart(
            meshes.cabSlopedRoof, MaterialId.VEHICLE_COAT,
            vx - fx * 0.28f, groundY + 1.54f + bounce, vz - fz * 0.28f, 0f, rot, 0f,
            cabScaleX, 0.76f, 1.92f,
            cr * 0.90f, cg * 0.90f, cb * 0.90f, 1f, roughness = 0.26f, metallic = 0.68f
        )
        // Chamfered Bullbar Bumper & Radiator Grille
        drawPart(
            meshes.chamferedStructure, MaterialId.CORRUGATED_METAL,
            vx + fx * 2.24f, groundY + 0.56f + bounce, vz + fz * 2.24f, 0f, rot, 0f,
            0.22f, 0.34f, 1.96f,
            0.14f, 0.15f, 0.17f, 1f, roughness = 0.35f, metallic = 0.85f
        )
        // Sloped Glass Windshield & Side Windows
        drawPart(
            meshes.chassisSlopeBody, MaterialId.VEHICLE_COAT,
            vx + fx * 0.78f, groundY + 1.48f + bounce, vz + fz * 0.78f, 0f, rot, 24f,
            0.14f, 0.68f, 1.72f,
            0.35f, 0.68f, 0.88f, 0.86f, roughness = 0.08f, metallic = 0.92f, emissive = 0.35f
        )
        // Headlights (warm emissive glow) & Chamfered Ruby Taillights
        drawPart(meshes.radomeSphere, MaterialId.SKIN_DETAIL, vx + fx * 2.26f + rx * 0.68f, groundY + 0.78f + bounce, vz + fz * 2.26f + rz * 0.68f, 0f, rot, 90f, 0.24f, 0.22f, 0.28f, 1.0f, 0.95f, 0.65f, 1f, 0.1f, 0.2f, 1f, 0f)
        drawPart(meshes.radomeSphere, MaterialId.SKIN_DETAIL, vx + fx * 2.26f - rx * 0.68f, groundY + 0.78f + bounce, vz + fz * 2.26f - rz * 0.68f, 0f, rot, 90f, 0.24f, 0.22f, 0.28f, 1.0f, 0.95f, 0.65f, 1f, 0.1f, 0.2f, 1f, 0f)
        drawPart(meshes.chamferedStructure, MaterialId.SKIN_DETAIL, vx - fx * 2.25f + rx * 0.70f, groundY + 0.80f + bounce, vz - fz * 2.25f + rz * 0.70f, 0f, rot, 0f, 0.12f, 0.18f, 0.30f, 0.95f, 0.12f, 0.12f, 1f, 0.2f, 0.2f, 0.85f, 0f)
        drawPart(meshes.chamferedStructure, MaterialId.SKIN_DETAIL, vx - fx * 2.25f - rx * 0.70f, groundY + 0.80f + bounce, vz - fz * 2.25f - rz * 0.70f, 0f, rot, 0f, 0.12f, 0.18f, 0.30f, 0.95f, 0.12f, 0.12f, 1f, 0.2f, 0.2f, 0.85f, 0f)

        // 4 Rotating Cylindrical Rubber Wheels + Alloy Rims
        if (v.type != VehicleType.SPEEDBOAT) {
            val wheelOffsets = listOf(Pair(1.42f, 1.06f), Pair(1.42f, -1.06f), Pair(-1.42f, 1.06f), Pair(-1.42f, -1.06f))
            wheelOffsets.forEach { (wF, wS) ->
                val wx = vx + fx * wF + rx * wS
                val wz = vz + fz * wF + rz * wS
                drawWheel(wx, groundY + 0.44f, wz, rot, wheelSpin, 0.88f, 0.36f)
            }
        }
    }

    private fun drawWheel(wx: Float, wy: Float, wz: Float, rotY: Float, spinDeg: Float, diameter: Float, width: Float) {
        drawPart(meshes.wheelCylinder, MaterialId.TACTICAL_FABRIC, wx, wy, wz, 0f, rotY, spinDeg, diameter, diameter, width, 0.10f, 0.11f, 0.12f, 1f, 0.82f, 0.08f)
        drawPart(meshes.wheelCylinder, MaterialId.CORRUGATED_METAL, wx, wy, wz, 0f, rotY, spinDeg, diameter * 0.54f, diameter * 0.54f, width * 1.04f, 0.72f, 0.75f, 0.80f, 1f, 0.20f, 0.90f)
    }

    private fun drawLocalMesh(
        mesh: GlMesh,
        mat: MaterialId,
        tx: Float, ty: Float, tz: Float,
        rx: Float, ry: Float, rz: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float,
        roughness: Float,
        metallic: Float,
        emissive: Float = 0f,
        useTex: Float = 1f,
        alpha: Float = 1f
    ) {
        System.arraycopy(modelMatrix, 0, scratchMatrix, 0, 16)
        Matrix.translateM(scratchMatrix, 0, tx, ty, tz)
        if (ry != 0f) Matrix.rotateM(scratchMatrix, 0, ry, 0f, 1f, 0f)
        if (rx != 0f) Matrix.rotateM(scratchMatrix, 0, rx, 1f, 0f, 0f)
        if (rz != 0f) Matrix.rotateM(scratchMatrix, 0, rz, 0f, 0f, 1f)
        Matrix.scaleM(scratchMatrix, 0, sx, sy, sz)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, scratchMatrix, 0)
        bindAndDrawMesh(mesh, mat, scratchMatrix, mvpMatrix, r, g, b, alpha, roughness, metallic, emissive, useTex)
    }

    private fun drawPart(
        mesh: GlMesh,
        mat: MaterialId,
        x: Float, y: Float, z: Float,
        rx: Float, ry: Float, rz: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float, a: Float,
        roughness: Float, metallic: Float,
        emissive: Float = 0f,
        useTex: Float = 1f
    ) {
        Matrix.setIdentityM(scratchMatrix, 0)
        Matrix.translateM(scratchMatrix, 0, x, y, z)
        if (ry != 0f) Matrix.rotateM(scratchMatrix, 0, ry, 0f, 1f, 0f)
        if (rx != 0f) Matrix.rotateM(scratchMatrix, 0, rx, 1f, 0f, 0f)
        if (rz != 0f) Matrix.rotateM(scratchMatrix, 0, rz, 0f, 0f, 1f)
        Matrix.scaleM(scratchMatrix, 0, sx, sy, sz)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, scratchMatrix, 0)
        bindAndDrawMesh(mesh, mat, scratchMatrix, mvpMatrix, r, g, b, a, roughness, metallic, emissive, useTex)
    }

    private fun bindAndDrawMesh(
        mesh: GlMesh,
        mat: MaterialId,
        mMat: FloatArray,
        mvp: FloatArray,
        r: Float, g: Float, b: Float, a: Float,
        roughness: Float, metallic: Float, emissive: Float, useTex: Float
    ) {
        val texPair = textures.get(mat)
        if (texPair != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texPair.albedoTexId)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texPair.normalTexId)
        }
        val buf = mesh.buffer
        buf.position(0)
        GLES20.glVertexAttribPointer(shader.aPos, 3, GLES20.GL_FLOAT, false, 32, buf)
        buf.position(3)
        GLES20.glVertexAttribPointer(shader.aNorm, 3, GLES20.GL_FLOAT, false, 32, buf)
        buf.position(6)
        GLES20.glVertexAttribPointer(shader.aUv, 2, GLES20.GL_FLOAT, false, 32, buf)

        GLES20.glUniformMatrix4fv(shader.uMvp, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(shader.uModel, 1, false, mMat, 0)
        GLES20.glUniform4f(shader.uTintColor, r, g, b, a)
        GLES20.glUniform1f(shader.uRoughness, roughness)
        GLES20.glUniform1f(shader.uMetallic, metallic)
        GLES20.glUniform1f(shader.uEmissive, emissive)
        GLES20.glUniform1f(shader.uUseTex, useTex)
        GLES20.glUniform2f(shader.uUvScale, 1f, 1f)
        GLES20.glUniform2f(shader.uUvOffset, 0f, 0f)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.vertexCount)
    }

    private fun resolveWeaponSkinColor(
        skinName: String,
        fallbackR: Float,
        fallbackG: Float,
        fallbackB: Float
    ): Triple<Float, Float, Float> {
        val lower = skinName.lowercase()
        return when {
            lower.contains("bunny") -> Triple(0.68f, 0.92f, 0.08f)
            lower.contains("rapper") || lower.contains("underworld") -> Triple(0.72f, 0.20f, 1.0f)
            lower.contains("butterfly") || lower.contains("dragon") || lower.contains("crimson") || lower.contains("inferno") || lower.contains("koi") -> Triple(1.0f, 0.22f, 0.24f)
            lower.contains("glacier") || lower.contains("arctic") || lower.contains("frost") -> Triple(0.15f, 0.86f, 1.0f)
            lower.contains("gold") || lower.contains("royal") || lower.contains("solar") -> Triple(1.0f, 0.82f, 0.18f)
            lower.contains("neon") || lower.contains("cyber") -> Triple(0.05f, 0.92f, 0.85f)
            lower.contains("cobalt") -> Triple(0.24f, 0.54f, 1.0f)
            else -> Triple(fallbackR, fallbackG, fallbackB)
        }
    }
}
