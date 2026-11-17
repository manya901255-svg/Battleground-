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

    // Pre-allocated hierarchy stack (Zero GC allocation per frame)
    private val stack = Array(14) { FloatArray(16) }
    private var stackTop = 0

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
        hasPonytailHair: Boolean = false
    ) {
        val px = player.x * TerrainHeightfield.WORLD_SCALE
        val py = groundY + player.z * TerrainHeightfield.WORLD_SCALE
        val pz = player.y * TerrainHeightfield.WORLD_SCALE
        val yaw = -player.angle

        val preset = CharacterPreset.fromId(player.characterPresetId)
        val isTeammate = player.teamId == userTeamId
        val isFemale = preset.gender.equals("Female", ignoreCase = true)
        val (armorR, armorG, armorB) = armorRgbOverride
            ?: if (player.isUser || isTeammate) preset.armorRgb else Triple(0.46f, 0.14f, 0.14f)
        val (accR, accG, accB) = accentRgbOverride
            ?: if (player.isUser || isTeammate) preset.accentRgb else Triple(0.95f, 0.25f, 0.15f)
        val (visR, visG, visB) = if (player.isUser || isTeammate) preset.visorRgb else Triple(1.0f, 0.15f, 0.15f)

        val skinTone = when (preset) {
            CharacterPreset.VORTEX_GHOST, CharacterPreset.FROST_RECON -> Triple(0.84f, 0.68f, 0.56f)
            CharacterPreset.NOVA_VALKYRIE, CharacterPreset.SERAPH_ECHO -> Triple(0.88f, 0.72f, 0.62f)
            CharacterPreset.BLAZE_STRIKER -> Triple(0.56f, 0.38f, 0.28f)
            else -> Triple(0.76f, 0.60f, 0.48f)
        }
        val hairRgb = hairRgbOverride ?: when (preset) {
            CharacterPreset.NOVA_VALKYRIE -> Triple(0.76f, 0.16f, 0.42f)
            CharacterPreset.CYBER_VIPER -> Triple(0.10f, 0.62f, 0.76f)
            CharacterPreset.FROST_RECON -> Triple(0.88f, 0.90f, 0.94f)
            CharacterPreset.SERAPH_ECHO -> Triple(0.88f, 0.72f, 0.28f)
            else -> Triple(0.12f, 0.10f, 0.10f)
        }

        // Contact Drop Shadow
        if (shadowQuality > 0 && player.vehicleId == null) {
            val sScale = (1.35f - (player.z * TerrainHeightfield.WORLD_SCALE * 0.08f)).coerceIn(0.4f, 1.35f)
            drawPart(
                mesh = meshes.softShadowDisc,
                mat = MaterialId.GRASS_TERRAIN,
                x = px, y = groundY + 0.04f, z = pz,
                rx = 0f, ry = 0f, rz = 0f,
                sx = sScale * (if (player.stance == PlayerStance.PRONE) 1.6f else 1.15f),
                sy = 1f,
                sz = sScale * (if (player.stance == PlayerStance.PRONE) 1.0f else 1.15f),
                r = 0.04f, g = 0.05f, b = 0.07f,
                a = if (shadowQuality >= 2) 0.48f else 0.32f,
                roughness = 0.98f, metallic = 0f, emissive = 0.75f, useTex = 0f
            )
        }

        // Smooth natural kinematics: breathing, stride, vault/landing & reload dynamics
        val speedNorm = (player.currentSpeed / 260f).coerceIn(0f, 1.12f)
        val strideSin = sin(player.animWalkPhase) * speedNorm
        val strideCos = cos(player.animWalkPhase) * speedNorm
        val breathe = sin(frameTimeSec * 2.4f) * 0.020f
        val idleSway = cos(frameTimeSec * 1.15f) * (1f - speedNorm).coerceIn(0f, 1f) * 2.2f
        val landDip = if (player.landingRecoverySec > 0f) sin((player.landingRecoverySec / 0.35f) * Math.PI).toFloat() * 0.22f else 0f
        val vaultLift = if (player.climbVaultTimerSec > 0f) sin((player.climbVaultTimerSec / 0.45f) * Math.PI).toFloat() * 0.45f else 0f

        val isProne = player.stance == PlayerStance.PRONE
        val isCrouch = player.stance == PlayerStance.CROUCH
        val isSwimming = player.stance == PlayerStance.SWIMMING
        val isDriving = player.stance == PlayerStance.DRIVING || player.vehicleId != null
        val isSkydiving = player.z > 15f
        val isJumping = player.stance == PlayerStance.JUMPING || (player.z > 1.5f && !isSkydiving)
        val isEmoting = player.emoteAnimSec > 0f
        val isReloading = player.isReloading
        val reloadOsc = if (isReloading) sin(player.reloadTimerSec * 7.5f) else 0f
        val fireElapsedMs = System.currentTimeMillis() - player.lastFireTimestampMs
        val recoilKick = if (fireElapsedMs in 0..110L) (1f - fireElapsedMs / 110f) else 0f

        // Compute Root Transform in world space (+X forward in character local frame)
        Matrix.setIdentityM(modelMatrix, 0)
        val pelvisY = when {
            isProne -> py + 0.22f
            isSwimming -> py - 0.10f + sin(frameTimeSec * 4.5f) * 0.05f
            isDriving -> py + 0.65f
            isCrouch -> py + 0.62f - landDip
            else -> py + 0.95f - landDip + vaultLift + abs(strideSin) * 0.042f
        }
        Matrix.translateM(modelMatrix, 0, px, pelvisY, pz)
        Matrix.rotateM(modelMatrix, 0, yaw + strideSin * 4.5f + idleSway, 0f, 1f, 0f)

        if (isProne || isSwimming) {
            Matrix.rotateM(modelMatrix, 0, 82f, 0f, 0f, -1f)
        } else if (isSkydiving) {
            Matrix.rotateM(modelMatrix, 0, 38f, 0f, 0f, -1f)
        } else if (speedNorm > 0.08f && !isDriving) {
            Matrix.rotateM(modelMatrix, 0, if (player.isSprinting) 13f else 5.5f, 0f, 0f, -1f)
        }

        val pantsColor = Triple(armorR * 0.44f + 0.06f, armorG * 0.44f + 0.07f, armorB * 0.44f + 0.09f)

        // Waist Tactical Belt + Utility Buckle + Thigh Holster
        drawLocalMesh(
            meshes.chamferedStructure, MaterialId.TACTICAL_FABRIC,
            tx = 0f, ty = 0.04f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = if (isFemale) 0.30f else 0.34f, sy = 0.09f, sz = if (isFemale) 0.44f else 0.50f,
            r = 0.10f, g = 0.11f, b = 0.13f,
            roughness = 0.48f, metallic = 0.28f
        )
        drawLocalMesh(
            meshes.chamferedStructure, MaterialId.CORRUGATED_METAL,
            tx = if (isFemale) 0.155f else 0.175f, ty = 0.04f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.03f, sy = 0.07f, sz = 0.12f,
            r = accR * 0.85f, g = accG * 0.85f, b = accB * 0.85f,
            roughness = 0.24f, metallic = 0.82f
        )

        // --- HIPS, ARTICULATED LEGS, KNEE PADS & COMBAT BOOTS ---
        val leftHipPitch = when {
            isDriving -> -72f
            isSwimming -> sin(frameTimeSec * 9f) * 28f
            isJumping -> 32f
            isCrouch -> -48f + strideSin * 26f
            isProne -> strideSin * 14f
            else -> strideSin * (if (player.isSprinting) 44f else 30f)
        }
        val rightHipPitch = when {
            isDriving -> -72f
            isSwimming -> -sin(frameTimeSec * 9f) * 28f
            isJumping -> -18f
            isCrouch -> -48f - strideSin * 26f
            isProne -> -strideSin * 14f
            else -> -strideSin * (if (player.isSprinting) 44f else 30f)
        }
        val leftKneeBend = when {
            isDriving -> 78f
            isCrouch -> 78f
            isJumping -> 48f
            else -> (-strideCos * 30f).coerceAtLeast(6f)
        }
        val rightKneeBend = when {
            isDriving -> 78f
            isCrouch -> 78f
            isJumping -> 24f
            else -> (strideCos * 30f).coerceAtLeast(6f)
        }

        renderLeg(+0.135f, leftHipPitch, leftKneeBend, pantsColor, accR, accG, accB, hasHolster = false)
        renderLeg(-0.135f, rightHipPitch, rightKneeBend, pantsColor, accR, accG, accB, hasHolster = true)

        // --- TORSO, JACKET COLLAR, TACTICAL VEST, MOLLE POUCHES & BACKPACK ---
        pushMatrix(modelMatrix)
        val torsoLean = when {
            isCrouch -> 17f
            isEmoting -> sin(frameTimeSec * 8f) * 10f
            else -> breathe * 65f + recoilKick * 3.5f
        }
        Matrix.rotateM(modelMatrix, 0, torsoLean, 0f, 0f, -1f)

        val torsoWidthScale = if (isFemale) 0.47f else 0.56f
        val torsoDepthScale = if (isFemale) 0.30f else 0.34f
        drawLocalMesh(
            meshes.chestTorso, MaterialId.TACTICAL_FABRIC,
            tx = 0f, ty = 0.36f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = torsoDepthScale, sy = 0.62f, sz = torsoWidthScale,
            r = armorR, g = armorG, b = armorB,
            roughness = 0.66f, metallic = 0.14f
        )
        // Raised Tactical Jacket Collar
        drawLocalMesh(
            meshes.jacketCollar, MaterialId.TACTICAL_FABRIC,
            tx = -0.01f, ty = 0.67f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = torsoDepthScale * 0.72f, sy = 0.11f, sz = torsoWidthScale * 0.54f,
            r = armorR * 0.85f, g = armorG * 0.85f, b = armorB * 0.85f,
            roughness = 0.60f, metallic = 0.16f
        )

        // Tactical Ballistic Vest + MOLLE Chest Pouches
        if (player.armorLevel > 0) {
            drawLocalMesh(
                meshes.bevelPack, MaterialId.TACTICAL_FABRIC,
                tx = 0f, ty = 0.39f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = torsoDepthScale + 0.05f, sy = 0.42f, sz = torsoWidthScale + 0.03f,
                r = armorR * 0.72f, g = armorG * 0.72f, b = armorB * 0.72f,
                roughness = 0.46f, metallic = 0.34f
            )
            // Front MOLLE Magazine Pouches & Emblem Trim
            for (pouchZ in listOf(-0.11f, 0f, 0.11f)) {
                drawLocalMesh(
                    meshes.chamferedStructure, MaterialId.TACTICAL_FABRIC,
                    tx = torsoDepthScale * 0.5f + 0.04f, ty = 0.28f, tz = pouchZ,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.055f, sy = 0.12f, sz = 0.085f,
                    r = 0.11f, g = 0.12f, b = 0.14f,
                    roughness = 0.55f, metallic = 0.22f
                )
            }
            drawLocalMesh(
                meshes.chamferedStructure, MaterialId.VEHICLE_COAT,
                tx = torsoDepthScale * 0.5f + 0.035f, ty = 0.46f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.04f, sy = 0.09f, sz = torsoWidthScale * 0.74f,
                r = accR, g = accG, b = accB,
                roughness = 0.22f, metallic = 0.65f, emissive = 0.60f
            )
        }

        // Backpack & Slung Secondary Rifle
        if (player.backpackLevel > 0 && !isDriving) {
            drawLocalMesh(
                meshes.bevelPack, MaterialId.TACTICAL_FABRIC,
                tx = -0.25f, ty = 0.38f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.24f, sy = 0.48f, sz = 0.38f,
                r = 0.14f, g = 0.16f, b = 0.19f,
                roughness = 0.65f, metallic = 0.15f
            )
            drawLocalMesh(
                meshes.chamferedStructure, MaterialId.VEHICLE_COAT,
                tx = -0.36f, ty = 0.44f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.03f, sy = 0.12f, sz = 0.24f,
                r = accR, g = accG, b = accB,
                roughness = 0.28f, metallic = 0.55f, emissive = 0.45f
            )
            if (player.isUser || player.primaryWeapon2 != null) {
                drawLocalMesh(
                    meshes.rifleBarrel, MaterialId.CORRUGATED_METAL,
                    tx = -0.33f, ty = 0.52f, tz = -0.10f,
                    rx = 0f, ry = 0f, rz = 65f,
                    sx = 0.90f, sy = 0.065f, sz = 0.065f,
                    r = 0.18f, g = 0.22f, b = 0.26f,
                    roughness = 0.28f, metallic = 0.85f
                )
                drawLocalMesh(
                    meshes.weaponReceiver, MaterialId.VEHICLE_COAT,
                    tx = -0.33f, ty = 0.54f, tz = -0.10f,
                    rx = 0f, ry = 0f, rz = 65f,
                    sx = 0.54f, sy = 0.055f, sz = 0.045f,
                    r = accR, g = accG, b = accB,
                    roughness = 0.22f, metallic = 0.68f, emissive = 0.52f
                )
            }
        }

        // --- NECK & REALISTIC SCULPTED HUMAN HEAD + FACIAL FEATURES + HAIR ---
        pushMatrix(modelMatrix)
        Matrix.translateM(modelMatrix, 0, 0f, 0.72f, 0f)
        if (isSwimming || isProne) {
            Matrix.rotateM(modelMatrix, 0, -68f, 0f, 0f, -1f)
        }
        // Subtle idle head scan look
        val headMicroYaw = sin(frameTimeSec * 0.9f) * 4.5f * (1f - speedNorm).coerceIn(0f, 1f)
        Matrix.rotateM(modelMatrix, 0, headMicroYaw, 0f, 1f, 0f)

        // Neck
        drawLocalMesh(
            meshes.limbCapsule, MaterialId.SKIN_DETAIL,
            tx = 0f, ty = 0.0f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.135f, sy = 0.17f, sz = 0.135f,
            r = skinTone.first * 0.92f, g = skinTone.second * 0.92f, b = skinTone.third * 0.92f,
            roughness = 0.62f, metallic = 0f
        )
        // Sculpted Head Cranium, Cheekbones & Jawline
        drawLocalMesh(
            meshes.humanHead, MaterialId.SKIN_DETAIL,
            tx = 0.01f, ty = 0.19f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = if (isFemale) 0.32f else 0.34f,
            sy = if (isFemale) 0.345f else 0.365f,
            sz = if (isFemale) 0.295f else 0.315f,
            r = skinTone.first, g = skinTone.second, b = skinTone.third,
            roughness = 0.56f, metallic = 0.02f
        )
        // Sculpted Ears
        for (earSide in listOf(-1f, 1f)) {
            drawLocalMesh(
                meshes.humanEar, MaterialId.SKIN_DETAIL,
                tx = -0.01f, ty = 0.19f, tz = earSide * 0.142f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.045f, sy = 0.072f, sz = 0.024f,
                r = skinTone.first * 0.90f, g = skinTone.second * 0.88f, b = skinTone.third * 0.88f,
                roughness = 0.60f, metallic = 0f
            )
        }

        // Realistic Eyes, Iris, Eyebrows, Nose Bridge & Lips (whenever not covered by full helmet visor)
        val fullFaceVisor = showHelmetAndVisor && (player.helmetLevel >= 3)
        if (!fullFaceVisor) {
            // Sculpted Nose Bridge
            drawLocalMesh(
                meshes.humanNose, MaterialId.SKIN_DETAIL,
                tx = 0.165f, ty = 0.175f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.042f, sy = 0.068f, sz = 0.040f,
                r = skinTone.first * 0.95f, g = skinTone.second * 0.92f, b = skinTone.third * 0.90f,
                roughness = 0.54f, metallic = 0f
            )
            // Left & Right Eyes (Sclera + Iris + Pupil catchlight) + Eyebrows
            for (eyeSide in listOf(-1f, 1f)) {
                val ez = eyeSide * 0.056f
                drawLocalMesh(
                    meshes.humanEyeOrb, MaterialId.SKIN_DETAIL,
                    tx = 0.146f, ty = 0.215f, tz = ez,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.028f, sy = 0.026f, sz = 0.036f,
                    r = 0.94f, g = 0.94f, b = 0.96f,
                    roughness = 0.18f, metallic = 0.05f, useTex = 0f
                )
                drawLocalMesh(
                    meshes.humanEyeOrb, MaterialId.SKIN_DETAIL,
                    tx = 0.158f, ty = 0.215f, tz = ez,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.016f, sy = 0.018f, sz = 0.020f,
                    r = 0.14f, g = 0.18f, b = 0.24f,
                    roughness = 0.10f, metallic = 0.15f, useTex = 0f
                )
                // Eyebrow arch
                drawLocalMesh(
                    meshes.facialMouth, MaterialId.TACTICAL_FABRIC,
                    tx = 0.152f, ty = 0.248f, tz = ez,
                    rx = eyeSide * -6f, ry = 0f, rz = 0f,
                    sx = 0.024f, sy = 0.014f, sz = 0.048f,
                    r = hairRgb.first * 0.7f, g = hairRgb.second * 0.7f, b = hairRgb.third * 0.7f,
                    roughness = 0.78f, metallic = 0f, useTex = 0f
                )
            }
            // Subtle Mouth / Lips (when not wearing half-mask)
            if (halfMaskRgb == null) {
                drawLocalMesh(
                    meshes.facialMouth, MaterialId.SKIN_DETAIL,
                    tx = 0.150f, ty = 0.105f, tz = 0f,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.026f, sy = 0.016f, sz = 0.056f,
                    r = skinTone.first * 0.78f, g = skinTone.second * 0.56f, b = skinTone.third * 0.54f,
                    roughness = 0.45f, metallic = 0.05f, useTex = 0f
                )
            }
        }

        // Layered Natural Hair Crown + Optional Swaying Ponytail
        drawLocalMesh(
            meshes.humanHair, MaterialId.TACTICAL_FABRIC,
            tx = -0.01f, ty = 0.245f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.355f, sy = 0.32f, sz = 0.325f,
            r = hairRgb.first, g = hairRgb.second, b = hairRgb.third,
            roughness = 0.70f, metallic = 0.08f
        )
        if (hasPonytailHair) {
            val sway = sin(frameTimeSec * 3.1f + speedNorm * 2f) * (5f + speedNorm * 8f)
            drawLocalMesh(
                meshes.hairPonytail, MaterialId.TACTICAL_FABRIC,
                tx = -0.21f, ty = 0.24f, tz = 0f,
                rx = 0f, ry = 0f, rz = -42f + sway,
                sx = 0.20f, sy = 0.38f, sz = 0.20f,
                r = hairRgb.first, g = hairRgb.second, b = hairRgb.third,
                roughness = 0.66f, metallic = 0.10f
            )
            drawLocalMesh(
                meshes.hairPonytail, MaterialId.VEHICLE_COAT,
                tx = -0.32f, ty = 0.11f, tz = 0f,
                rx = 0f, ry = 0f, rz = -58f + sway * 1.25f,
                sx = 0.16f, sy = 0.24f, sz = 0.16f,
                r = 0.88f, g = 0.14f, b = 0.18f,
                roughness = 0.56f, metallic = 0.12f, emissive = 0.18f
            )
        }

        // Sleek Tactical Half-Mask over lower jaw
        if (halfMaskRgb != null) {
            drawLocalMesh(
                meshes.bevelPack, MaterialId.TACTICAL_FABRIC,
                tx = 0.075f, ty = 0.115f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.23f, sy = 0.155f, sz = 0.265f,
                r = halfMaskRgb.first, g = halfMaskRgb.second, b = halfMaskRgb.third,
                roughness = 0.48f, metallic = 0.25f
            )
            val mAcc = halfMaskAccentRgb ?: Triple(0.95f, 0.12f, 0.16f)
            drawLocalMesh(
                meshes.chamferedStructure, MaterialId.VEHICLE_COAT,
                tx = 0.188f, ty = 0.120f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.032f, sy = 0.038f, sz = 0.16f,
                r = mAcc.first, g = mAcc.second, b = mAcc.third,
                roughness = 0.25f, metallic = 0.45f, emissive = 0.55f, useTex = 0f
            )
        }

        // Tactical Helmet + Reflective Curved Visor
        if (showHelmetAndVisor) {
            if (player.helmetLevel > 0) {
                drawLocalMesh(
                    meshes.helmetShell, MaterialId.CORRUGATED_METAL,
                    tx = -0.01f, ty = 0.23f, tz = 0f,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.39f, sy = 0.32f, sz = 0.36f,
                    r = armorR * 0.85f, g = armorG * 0.85f, b = armorB * 0.85f,
                    roughness = 0.32f, metallic = 0.55f
                )
            }
            if (player.helmetLevel >= 2 || player.isUser) {
                drawLocalMesh(
                    meshes.curvedVisor, MaterialId.VEHICLE_COAT,
                    tx = 0.04f, ty = 0.21f, tz = 0f,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.32f, sy = 0.24f, sz = 0.30f,
                    r = visR, g = visG, b = visB,
                    roughness = 0.12f, metallic = 0.88f, emissive = 0.55f
                )
            }
        }
        popMatrix(modelMatrix)

        // --- SHOULDERS, ARMS, GLOVED HANDS & DETAILED MULTI-PART HELD WEAPON ---
        val rightArmRaise = when {
            isEmoting -> 125f + sin(frameTimeSec * 10f) * 28f
            isDriving -> 55f
            isSwimming -> 75f + sin(frameTimeSec * 7f) * 65f
            isReloading -> 52f + reloadOsc * 8f
            player.activeWeapon != null -> 66f + recoilKick * 12f
            else -> -strideSin * 26f
        }
        val leftArmRaise = when {
            isEmoting -> 45f + cos(frameTimeSec * 8f) * 22f
            isDriving -> 55f
            isSwimming -> 75f - sin(frameTimeSec * 7f) * 65f
            isReloading -> 40f + reloadOsc * 24f
            player.activeWeapon != null -> 58f + recoilKick * 9f
            else -> strideSin * 26f
        }
        val shoulderOffsetZ = if (isFemale) 0.24f else 0.27f
        renderArm(+shoulderOffsetZ, leftArmRaise, 28f, armorR, armorG, armorB, skinTone)
        renderArm(-shoulderOffsetZ, rightArmRaise, 22f, armorR, armorG, armorB, skinTone)

        // Detailed Multi-Part Held Weapon + Reload Tilt + Recoil Kick + Muzzle Flash
        val heldWeapon = player.activeWeapon
        if (heldWeapon != null && !isDriving && !isSwimming && !isSkydiving && !isEmoting) {
            pushMatrix(modelMatrix)
            Matrix.translateM(modelMatrix, 0, 0.32f - recoilKick * 0.08f, 0.44f + recoilKick * 0.03f, 0.18f)
            if (isReloading) {
                Matrix.rotateM(modelMatrix, 0, reloadOsc * 16f, 1f, 0f, 0f)
                Matrix.rotateM(modelMatrix, 0, -24f, 0f, 1f, 0f)
            } else {
                Matrix.rotateM(modelMatrix, 0, recoilKick * 7f, 0f, 0f, 1f)
            }

            val weaponLenScale = when (heldWeapon.category) {
                WeaponCategory.SNIPER_RIFLE -> 1.48f
                WeaponCategory.SHOTGUN -> 1.12f
                WeaponCategory.SMG -> 0.78f
                WeaponCategory.PISTOL -> 0.42f
                else -> 1.0f
            }
            val (skinR, skinG, skinB) = resolveWeaponSkinColor(heldWeapon.skinName, accR, accG, accB)

            // 1. Chamfered Upper/Lower Receiver
            drawLocalMesh(
                meshes.weaponReceiver, MaterialId.CORRUGATED_METAL,
                tx = 0.12f * weaponLenScale, ty = 0f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.48f * weaponLenScale.coerceAtLeast(0.55f), sy = 0.135f, sz = 0.095f,
                r = 0.16f, g = 0.18f, b = 0.22f,
                roughness = 0.28f, metallic = 0.85f
            )
            // 2. Anodized Skin Accent Shroud Rail
            drawLocalMesh(
                meshes.chamferedStructure, MaterialId.VEHICLE_COAT,
                tx = 0.18f * weaponLenScale, ty = 0.072f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.38f * weaponLenScale.coerceAtLeast(0.55f), sy = 0.036f, sz = 0.078f,
                r = skinR, g = skinG, b = skinB,
                roughness = 0.18f, metallic = 0.76f, emissive = 0.62f
            )
            // 3. Fluted Cylindrical Barrel + Muzzle Brake / Suppressor
            drawLocalMesh(
                meshes.rifleBarrel, MaterialId.CORRUGATED_METAL,
                tx = 0.48f * weaponLenScale, ty = 0.02f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.46f * weaponLenScale, sy = 0.052f, sz = 0.052f,
                r = 0.22f, g = 0.24f, b = 0.28f,
                roughness = 0.20f, metallic = 0.92f
            )
            if (heldWeapon.equippedMuzzle == MuzzleType.SUPPRESSOR) {
                drawLocalMesh(
                    meshes.rifleBarrel, MaterialId.CORRUGATED_METAL,
                    tx = 0.74f * weaponLenScale, ty = 0.02f, tz = 0f,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.22f, sy = 0.078f, sz = 0.078f,
                    r = 0.10f, g = 0.11f, b = 0.13f,
                    roughness = 0.30f, metallic = 0.82f
                )
            }
            // 4. Buttstock & Pistol Grip
            if (heldWeapon.category != WeaponCategory.PISTOL) {
                drawLocalMesh(
                    meshes.tacticalStock, MaterialId.TACTICAL_FABRIC,
                    tx = -0.22f * weaponLenScale.coerceAtMost(1.1f), ty = -0.02f, tz = 0f,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.30f, sy = 0.14f, sz = 0.08f,
                    r = 0.14f, g = 0.15f, b = 0.18f,
                    roughness = 0.55f, metallic = 0.25f
                )
            }
            drawLocalMesh(
                meshes.ergoGrip, MaterialId.TACTICAL_FABRIC,
                tx = -0.02f, ty = -0.10f, tz = 0f,
                rx = 0f, ry = 0f, rz = 18f,
                sx = 0.07f, sy = 0.15f, sz = 0.06f,
                r = 0.12f, g = 0.13f, b = 0.15f,
                roughness = 0.68f, metallic = 0.15f
            )
            // 5. Curved Detachable Magazine (drops slightly during reload)
            val magDropY = if (isReloading) -abs(reloadOsc) * 0.10f else 0f
            drawLocalMesh(
                meshes.curvedMagazine, MaterialId.CORRUGATED_METAL,
                tx = 0.15f, ty = -0.11f + magDropY, tz = 0f,
                rx = 0f, ry = 0f, rz = -8f,
                sx = if (heldWeapon.category == WeaponCategory.SHOTGUN) 0.14f else 0.09f,
                sy = if (heldWeapon.category == WeaponCategory.PISTOL) 0.14f else 0.21f,
                sz = 0.06f,
                r = skinR * 0.68f, g = skinG * 0.68f, b = skinB * 0.68f,
                roughness = 0.36f, metallic = 0.74f
            )
            // 6. Optic Scope + Objective Lens
            if (heldWeapon.category == WeaponCategory.SNIPER_RIFLE ||
                heldWeapon.equippedScope != ScopeType.IRON_SIGHT
            ) {
                drawLocalMesh(
                    meshes.rifleBarrel, MaterialId.CORRUGATED_METAL,
                    tx = 0.14f, ty = 0.125f, tz = 0f,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = if (heldWeapon.category == WeaponCategory.SNIPER_RIFLE) 0.34f else 0.18f,
                    sy = 0.06f, sz = 0.06f,
                    r = 0.10f, g = 0.12f, b = 0.16f,
                    roughness = 0.15f, metallic = 0.90f
                )
                drawLocalMesh(
                    meshes.humanEyeOrb, MaterialId.VEHICLE_COAT,
                    tx = 0.24f, ty = 0.125f, tz = 0f,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.028f, sy = 0.052f, sz = 0.052f,
                    r = 0.12f, g = 0.88f, b = 1.0f,
                    roughness = 0.08f, metallic = 0.9f, emissive = 0.82f, useTex = 0f
                )
            }

            // 7. Crisp Multi-Fin Muzzle Flash Star Burst
            if (fireElapsedMs in 0..90L) {
                drawLocalMesh(
                    meshes.grassTuft, MaterialId.SKIN_DETAIL,
                    tx = 0.78f * weaponLenScale + 0.08f, ty = 0.02f, tz = 0f,
                    rx = frameTimeSec * 480f, ry = 0f, rz = -90f,
                    sx = 0.34f, sy = 0.44f, sz = 0.34f,
                    r = 1.0f, g = 0.82f, b = 0.22f,
                    roughness = 0.5f, metallic = 0f, emissive = 1.0f, useTex = 0f, alpha = 0.94f
                )
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

        // Inflated Ram-Air Parachute Canopy & Converging Harness Risers during Skydiving
        if (isSkydiving) {
            drawPart(
                meshes.curvedParachuteCanopy, MaterialId.TACTICAL_FABRIC,
                px, py + 4.35f, pz, 0f, yaw, 0f, 4.8f, 1.65f, 2.75f,
                1.0f, 0.44f, 0.06f, 0.95f, roughness = 0.62f, metallic = 0.06f, emissive = 0.22f
            )
            val rad = Math.toRadians(player.angle.toDouble())
            val rxV = -sin(rad).toFloat()
            val rzV = cos(rad).toFloat()
            drawPart(meshes.columnBeam, MaterialId.CORRUGATED_METAL, px + rxV * 0.95f, py + 2.4f, pz + rzV * 0.95f, 0f, yaw, -16f, 0.045f, 3.8f, 0.045f, 0.22f, 0.24f, 0.26f, 0.92f, 0.5f, 0.4f, 0f, 0f)
            drawPart(meshes.columnBeam, MaterialId.CORRUGATED_METAL, px - rxV * 0.95f, py + 2.4f, pz - rzV * 0.95f, 0f, yaw, 16f, 0.045f, 3.8f, 0.045f, 0.22f, 0.24f, 0.26f, 0.92f, 0.5f, 0.4f, 0f, 0f)
        }
    }

    private fun renderLeg(
        hipOffsetZ: Float,
        hipPitchDeg: Float,
        kneeBendDeg: Float,
        pantsRgb: Triple<Float, Float, Float>,
        accR: Float, accG: Float, accB: Float,
        hasHolster: Boolean
    ) {
        pushMatrix(modelMatrix)
        Matrix.translateM(modelMatrix, 0, 0f, -0.04f, hipOffsetZ)
        Matrix.rotateM(modelMatrix, 0, hipPitchDeg, 0f, 0f, 1f)
        // Thigh
        drawLocalMesh(
            meshes.limbCapsule, MaterialId.TACTICAL_FABRIC,
            tx = 0f, ty = -0.22f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.20f, sy = 0.44f, sz = 0.20f,
            r = pantsRgb.first, g = pantsRgb.second, b = pantsRgb.third,
            roughness = 0.72f, metallic = 0.05f
        )
        if (hasHolster) {
            drawLocalMesh(
                meshes.kneePadShell, MaterialId.TACTICAL_FABRIC,
                tx = 0.01f, ty = -0.20f, tz = -0.10f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.11f, sy = 0.16f, sz = 0.05f,
                r = 0.10f, g = 0.11f, b = 0.13f,
                roughness = 0.5f, metallic = 0.2f
            )
        }
        // Articulated Knee Guard + Calf
        Matrix.translateM(modelMatrix, 0, 0f, -0.44f, 0f)
        drawLocalMesh(
            meshes.kneePadShell, MaterialId.CORRUGATED_METAL,
            tx = 0.085f, ty = 0f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.06f, sy = 0.10f, sz = 0.13f,
            r = 0.14f, g = 0.16f, b = 0.19f,
            roughness = 0.40f, metallic = 0.45f
        )
        Matrix.rotateM(modelMatrix, 0, -kneeBendDeg, 0f, 0f, 1f)
        drawLocalMesh(
            meshes.limbCapsule, MaterialId.TACTICAL_FABRIC,
            tx = 0f, ty = -0.21f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.16f, sy = 0.42f, sz = 0.16f,
            r = pantsRgb.first * 0.88f, g = pantsRgb.second * 0.88f, b = pantsRgb.third * 0.88f,
            roughness = 0.72f, metallic = 0.05f
        )
        // Tactical Combat Boot + Lugged Sole Trim
        drawLocalMesh(
            meshes.taperedBoot, MaterialId.CORRUGATED_METAL,
            tx = 0.05f, ty = -0.42f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.24f, sy = 0.14f, sz = 0.15f,
            r = 0.12f, g = 0.12f, b = 0.14f,
            roughness = 0.45f, metallic = 0.25f
        )
        drawLocalMesh(
            meshes.chamferedStructure, MaterialId.VEHICLE_COAT,
            tx = 0.05f, ty = -0.475f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.245f, sy = 0.042f, sz = 0.145f,
            r = accR * 0.7f, g = accG * 0.7f, b = accB * 0.7f,
            roughness = 0.35f, metallic = 0.4f
        )
        popMatrix(modelMatrix)
    }

    private fun renderArm(
        shoulderOffsetZ: Float,
        armRaiseDeg: Float,
        elbowBendDeg: Float,
        armorR: Float, armorG: Float, armorB: Float,
        skinTone: Triple<Float, Float, Float>
    ) {
        pushMatrix(modelMatrix)
        Matrix.translateM(modelMatrix, 0, 0.02f, 0.60f, shoulderOffsetZ)
        // Shoulder Pauldron
        drawLocalMesh(
            meshes.kneePadShell, MaterialId.TACTICAL_FABRIC,
            tx = 0f, ty = 0f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.17f, sy = 0.15f, sz = 0.17f,
            r = armorR * 0.9f, g = armorG * 0.9f, b = armorB * 0.9f,
            roughness = 0.45f, metallic = 0.3f
        )
        Matrix.rotateM(modelMatrix, 0, armRaiseDeg, 0f, 0f, 1f)
        // Upper Arm
        drawLocalMesh(
            meshes.limbCapsule, MaterialId.TACTICAL_FABRIC,
            tx = 0f, ty = -0.15f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.14f, sy = 0.30f, sz = 0.14f,
            r = armorR, g = armorG, b = armorB,
            roughness = 0.65f, metallic = 0.1f
        )
        // Forearm & Tactical Gloved Hand
        Matrix.translateM(modelMatrix, 0, 0f, -0.30f, 0f)
        drawLocalMesh(
            meshes.kneePadShell, MaterialId.TACTICAL_FABRIC,
            tx = -0.04f, ty = 0f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.08f, sy = 0.08f, sz = 0.09f,
            r = 0.12f, g = 0.13f, b = 0.15f,
            roughness = 0.45f, metallic = 0.35f
        )
        Matrix.rotateM(modelMatrix, 0, elbowBendDeg, 0f, 0f, 1f)
        drawLocalMesh(
            meshes.limbCapsule, MaterialId.SKIN_DETAIL,
            tx = 0f, ty = -0.13f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.115f, sy = 0.26f, sz = 0.115f,
            r = skinTone.first, g = skinTone.second, b = skinTone.third,
            roughness = 0.56f, metallic = 0.02f
        )
        drawLocalMesh(
            meshes.humanHandGlove, MaterialId.TACTICAL_FABRIC,
            tx = 0f, ty = -0.27f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.105f, sy = 0.095f, sz = 0.095f,
            r = 0.12f, g = 0.12f, b = 0.14f,
            roughness = 0.48f, metallic = 0.22f
        )
        popMatrix(modelMatrix)
    }

    fun renderCompanionPet(owner: Player, groundY: Float, pet: PetPreset, frameTimeSec: Float) {
        val rad = Math.toRadians(owner.angle.toDouble())
        val fx = cos(rad).toFloat()
        val fz = sin(rad).toFloat()
        val rx = -fz
        val rz = fx
        val hover = sin(frameTimeSec * 4.5f) * 0.18f
        val petX = owner.x * TerrainHeightfield.WORLD_SCALE - fx * 0.72f - rx * 1.15f
        val petY = groundY + owner.z * TerrainHeightfield.WORLD_SCALE + 1.40f + hover
        val petZ = owner.y * TerrainHeightfield.WORLD_SCALE - fz * 0.72f - rz * 1.15f
        val yaw = -owner.angle
        val (br, bg, bb) = pet.bodyRgb
        val (gr, gg, gb) = pet.glowRgb
        val flap = sin(frameTimeSec * 12f) * 24f

        drawPart(meshes.humanHead, MaterialId.VEHICLE_COAT, petX, petY, petZ, 0f, yaw, 0f, 0.46f, 0.42f, 0.42f, br, bg, bb, 1f, 0.35f, 0.45f, 0.15f)
        drawPart(meshes.sweptAircraftWing, MaterialId.VEHICLE_COAT, petX, petY + 0.12f, petZ, 0f, yaw, flap, 0.36f, 0.14f, 1.15f, gr, gg, gb, 0.94f, 0.25f, 0.6f, 0.7f)
        drawPart(meshes.curvedVisor, MaterialId.SKIN_DETAIL, petX + fx * 0.12f, petY + 0.06f, petZ + fz * 0.12f, 0f, yaw, 0f, 0.32f, 0.18f, 0.28f, 1.0f, 0.92f, 0.25f, 1f, 0.1f, 0.2f, 1f, 0f)
    }

    fun renderVehicle3D(v: VehicleInstance, groundY: Float, frameTimeSec: Float, shadowQuality: Int) {
        val vx = v.x * TerrainHeightfield.WORLD_SCALE
        val vz = v.y * TerrainHeightfield.WORLD_SCALE
        val rot = -v.rotationAngle
        val isDestroyed = v.isDestroyed

        val (cr, cg, cb) = when {
            isDestroyed -> Triple(0.18f, 0.18f, 0.20f)
            v.type == VehicleType.UAZ_JEEP -> Triple(0.84f, 0.86f, 0.88f)
            v.type == VehicleType.BUGGY -> Triple(0.95f, 0.56f, 0.10f)
            v.type == VehicleType.MOTORBIKE -> Triple(0.88f, 0.16f, 0.18f)
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
            drawPart(meshes.chassisSlopeBody, MaterialId.VEHICLE_COAT, vx, groundY + 0.74f + bounce, vz, 0f, rot, 0f, 2.3f, 0.52f, 0.54f, cr, cg, cb, 1f, 0.22f, 0.78f, 0.05f)
            drawPart(meshes.curvedVisor, MaterialId.VEHICLE_COAT, vx + fx * 0.62f, groundY + 1.05f + bounce, vz + fz * 0.62f, 0f, rot, 0f, 0.45f, 0.45f, 0.42f, 0.45f, 0.82f, 0.96f, 0.88f, 0.08f, 0.92f, 0.3f)
            drawWheel(vx + fx * 0.95f, groundY + 0.42f, vz + fz * 0.95f, rot, wheelSpin, 0.82f, 0.24f)
            drawWheel(vx - fx * 0.95f, groundY + 0.42f, vz - fz * 0.95f, rot, wheelSpin, 0.86f, 0.28f)
            return
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
            lower.contains("glacier") || lower.contains("arctic") || lower.contains("frost") -> Triple(0.15f, 0.86f, 1.0f)
            lower.contains("dragon") || lower.contains("crimson") || lower.contains("inferno") -> Triple(1.0f, 0.22f, 0.14f)
            lower.contains("gold") || lower.contains("royal") || lower.contains("solar") -> Triple(1.0f, 0.82f, 0.18f)
            lower.contains("neon") || lower.contains("cyber") -> Triple(0.05f, 0.92f, 0.85f)
            lower.contains("cobalt") -> Triple(0.24f, 0.54f, 1.0f)
            else -> Triple(fallbackR, fallbackG, fallbackB)
        }
    }
}
