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
        hasPonytailHair: Boolean = false,
        parachuteSkinId: String = "para_flame"
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

        // Tactical Ballistic Vest + MOLLE Chest Pouches (Lv.1 Police Black, Lv.2 Orange/Tan, Lv.3 Blue Camo, Lv.4 Heavy Gold-Blue Plates)
        if (player.armorLevel > 0) {
            val (vR, vG, vB) = when (player.armorLevel) {
                1 -> Triple(0.12f, 0.13f, 0.16f) // Police dark vest
                2 -> Triple(0.76f, 0.42f, 0.12f) // Tan/Orange tactical vest
                3 -> Triple(0.12f, 0.56f, 0.84f) // Blue camo vest
                else -> Triple(0.18f, 0.36f, 0.76f) // Lv4 reinforced tactical blue+gold plates
            }
            drawLocalMesh(
                meshes.bevelPack, MaterialId.TACTICAL_FABRIC,
                tx = 0f, ty = 0.39f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = torsoDepthScale + (if (player.armorLevel >= 4) 0.075f else 0.05f),
                sy = 0.42f,
                sz = torsoWidthScale + (if (player.armorLevel >= 4) 0.055f else 0.03f),
                r = vR, g = vG, b = vB,
                roughness = 0.44f, metallic = if (player.armorLevel >= 3) 0.48f else 0.32f
            )
            // Front MOLLE Magazine Pouches & Emblem Trim (Gold trim on Lv4!)
            for (pouchZ in listOf(-0.11f, 0f, 0.11f)) {
                drawLocalMesh(
                    meshes.chamferedStructure, MaterialId.TACTICAL_FABRIC,
                    tx = torsoDepthScale * 0.5f + 0.045f, ty = 0.28f, tz = pouchZ,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.058f, sy = 0.12f, sz = 0.088f,
                    r = if (player.armorLevel >= 4) 0.98f else 0.11f,
                    g = if (player.armorLevel >= 4) 0.84f else 0.12f,
                    b = if (player.armorLevel >= 4) 0.12f else 0.14f,
                    roughness = 0.35f, metallic = if (player.armorLevel >= 4) 0.6f else 0.22f
                )
            }
            drawLocalMesh(
                meshes.chamferedStructure, MaterialId.VEHICLE_COAT,
                tx = torsoDepthScale * 0.5f + 0.042f, ty = 0.46f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.045f, sy = 0.095f, sz = torsoWidthScale * 0.74f,
                r = if (player.armorLevel >= 4) 1.0f else accR,
                g = if (player.armorLevel >= 4) 0.85f else accG,
                b = if (player.armorLevel >= 4) 0.15f else accB,
                roughness = 0.20f, metallic = 0.68f, emissive = if (player.armorLevel >= 4) 0.78f else 0.50f
            )
        }

        // Character Style Signature Overcoat/Cape/Ember Silhouettes
        when (preset.styleTag) {
            "MAFIA_CAPE", "SAMURAI_CLOAK" -> {
                val capeRgb = if (preset == CharacterPreset.NEON_SAMURAI) Triple(0.75f, 0.12f, 0.78f) else Triple(0.86f, 0.86f, 0.88f)
                drawLocalMesh(
                    meshes.bannerClothStrip, MaterialId.TACTICAL_FABRIC,
                    tx = -0.22f, ty = 0.18f, tz = 0f,
                    rx = 0f, ry = 90f, rz = -8f + sin(frameTimeSec * 3.8f) * 6f,
                    sx = 0.56f, sy = 0.82f, sz = 0.28f,
                    r = capeRgb.first, g = capeRgb.second, b = capeRgb.third,
                    roughness = 0.55f, metallic = 0.18f, emissive = if (preset == CharacterPreset.NEON_SAMURAI) 0.35f else 0.05f
                )
            }
            "FLAME_HOOD" -> {
                // Blazing ember shoulder plumes
                for (sz in listOf(-0.26f, 0.26f)) {
                    drawLocalMesh(
                        meshes.foliageCone, MaterialId.VEHICLE_COAT,
                        tx = 0f, ty = 0.70f, tz = sz,
                        rx = 0f, ry = 0f, rz = 12f,
                        sx = 0.16f, sy = 0.26f, sz = 0.16f,
                        r = 1.0f, g = 0.42f, b = 0.06f,
                        roughness = 0.2f, metallic = 0.1f, emissive = 0.92f
                    )
                }
            }
            "CROWN_COAT" -> {
                // High sovereign golden collar & pauldrons
                drawLocalMesh(
                    meshes.jacketCollar, MaterialId.VEHICLE_COAT,
                    tx = -0.02f, ty = 0.72f, tz = 0f,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = torsoDepthScale * 0.84f, sy = 0.16f, sz = torsoWidthScale * 0.64f,
                    r = 1.0f, g = 0.80f, b = 0.18f,
                    roughness = 0.22f, metallic = 0.85f, emissive = 0.35f
                )
            }
        }

        // Backpack (Lv1 tan sling -> Lv2 vibrant orange pack -> Lv3 camo military pack) & Slung Secondary Rifle
        if (player.backpackLevel > 0 && !isDriving) {
            val (bagR, bagG, bagB) = when (player.backpackLevel) {
                1 -> Triple(0.78f, 0.58f, 0.24f)
                2 -> Triple(0.92f, 0.42f, 0.08f)
                else -> Triple(0.24f, 0.44f, 0.20f)
            }
            val bagScale = 0.18f + (player.backpackLevel.coerceIn(1, 3) - 1) * 0.045f
            drawLocalMesh(
                meshes.bevelPack, MaterialId.TACTICAL_FABRIC,
                tx = -0.25f, ty = 0.38f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = bagScale, sy = 0.46f + (player.backpackLevel - 1) * 0.05f, sz = 0.34f + (player.backpackLevel - 1) * 0.04f,
                r = bagR, g = bagG, b = bagB,
                roughness = 0.62f, metallic = 0.16f
            )
            drawLocalMesh(
                meshes.chamferedStructure, MaterialId.VEHICLE_COAT,
                tx = -0.35f, ty = 0.44f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.03f, sy = 0.12f, sz = 0.22f,
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
            if (effectiveHalfMaskRgb == null) {
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

        // Layered Natural Hair Crown + Optional Afro / Kitsune Ears / Crown / Swaying Ponytail
        val hairScaleX = if (preset.styleTag == "CYBER_AFRO") 0.52f else 0.355f
        val hairScaleY = if (preset.styleTag == "CYBER_AFRO") 0.46f else 0.32f
        val hairScaleZ = if (preset.styleTag == "CYBER_AFRO") 0.50f else 0.325f
        drawLocalMesh(
            meshes.humanHair, MaterialId.TACTICAL_FABRIC,
            tx = -0.01f, ty = if (preset.styleTag == "CYBER_AFRO") 0.28f else 0.245f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = hairScaleX, sy = hairScaleY, sz = hairScaleZ,
            r = hairRgb.first, g = hairRgb.second, b = hairRgb.third,
            roughness = 0.70f, metallic = 0.08f
        )
        if (preset.styleTag == "KITSUNE") {
            for (earSide in listOf(-0.11f, 0.11f)) {
                drawLocalMesh(
                    meshes.kitsuneEar, MaterialId.TACTICAL_FABRIC,
                    tx = 0.02f, ty = 0.44f, tz = earSide,
                    rx = earSide * 140f, ry = 0f, rz = 0f,
                    sx = 0.11f, sy = 0.22f, sz = 0.11f,
                    r = 0.96f, g = 0.94f, b = 0.96f,
                    roughness = 0.55f, metallic = 0.10f
                )
                drawLocalMesh(
                    meshes.kitsuneEar, MaterialId.VEHICLE_COAT,
                    tx = 0.04f, ty = 0.42f, tz = earSide,
                    rx = earSide * 140f, ry = 0f, rz = 0f,
                    sx = 0.06f, sy = 0.14f, sz = 0.06f,
                    r = 0.94f, g = 0.12f, b = 0.22f,
                    roughness = 0.35f, metallic = 0.2f, emissive = 0.35f
                )
            }
        }
        if (preset.styleTag == "CROWN_COAT") {
            drawLocalMesh(
                meshes.chamferedStructure, MaterialId.VEHICLE_COAT,
                tx = 0f, ty = 0.43f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.24f, sy = 0.09f, sz = 0.24f,
                r = 1.0f, g = 0.82f, b = 0.18f,
                roughness = 0.18f, metallic = 0.90f, emissive = 0.48f
            )
        }
        if (effectiveHasPonytail) {
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
                r = accR, g = accG, b = accB,
                roughness = 0.52f, metallic = 0.14f, emissive = 0.24f
            )
        }

        // Sleek Tactical Half-Mask over lower jaw
        if (effectiveHalfMaskRgb != null) {
            drawLocalMesh(
                meshes.bevelPack, MaterialId.TACTICAL_FABRIC,
                tx = 0.075f, ty = 0.115f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.23f, sy = 0.155f, sz = 0.265f,
                r = effectiveHalfMaskRgb.first, g = effectiveHalfMaskRgb.second, b = effectiveHalfMaskRgb.third,
                roughness = 0.48f, metallic = 0.25f
            )
            val mAcc = effectiveHalfMaskAccent ?: Triple(0.95f, 0.12f, 0.18f)
            drawLocalMesh(
                meshes.chamferedStructure, MaterialId.VEHICLE_COAT,
                tx = 0.188f, ty = 0.120f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.032f, sy = 0.038f, sz = 0.16f,
                r = mAcc.first, g = mAcc.second, b = mAcc.third,
                roughness = 0.25f, metallic = 0.45f, emissive = 0.55f, useTex = 0f
            )
        }

        // Tactical Helmets Lv1-4 (Lv1 Olive Cap, Lv2 Camo Goggle Helm, Lv3 Welded Spetsnaz Visor, Lv4 Red-Eye Cyber Respirator)
        val hideSignatureHairHelmet =
            (preset == CharacterPreset.VORTEX_GHOST || preset == CharacterPreset.NOVA_VALKYRIE || preset == CharacterPreset.KITSUNE_SHRINE) && player.helmetLevel < 3
        if (showHelmetAndVisor && !hideSignatureHairHelmet && player.helmetLevel > 0) {
            val (hR, hG, hB) = when (player.helmetLevel) {
                1 -> Triple(0.18f, 0.36f, 0.22f) // Olive Police Cap Helm
                2 -> Triple(0.42f, 0.44f, 0.26f) // Tan Camo Helm
                3 -> Triple(0.24f, 0.26f, 0.30f) // Dark Titanium Spetsnaz
                else -> Triple(0.12f, 0.14f, 0.22f) // Deep Midnight Lv4 Cyber Respirator
            }
            drawLocalMesh(
                meshes.helmetShell, MaterialId.CORRUGATED_METAL,
                tx = -0.01f, ty = 0.23f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.395f, sy = 0.33f, sz = 0.365f,
                r = hR, g = hG, b = hB,
                roughness = 0.28f, metallic = 0.62f
            )
            if (player.helmetLevel == 1) {
                // Motorcycle cap brim
                drawLocalMesh(
                    meshes.chamferedStructure, MaterialId.TACTICAL_FABRIC,
                    tx = 0.21f, ty = 0.24f, tz = 0f,
                    rx = 0f, ry = 0f, rz = -8f,
                    sx = 0.14f, sy = 0.03f, sz = 0.24f,
                    r = 0.12f, g = 0.24f, b = 0.14f,
                    roughness = 0.5f, metallic = 0.1f
                )
            } else if (player.helmetLevel == 2) {
                // Orange-tinted tactical goggles band on forehead
                drawLocalMesh(
                    meshes.curvedVisor, MaterialId.VEHICLE_COAT,
                    tx = 0.06f, ty = 0.28f, tz = 0f,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.34f, sy = 0.14f, sz = 0.31f,
                    r = 0.94f, g = 0.56f, b = 0.12f,
                    roughness = 0.15f, metallic = 0.72f, emissive = 0.25f
                )
            } else if (player.helmetLevel == 3) {
                // Welded Spetsnaz armored visor
                drawLocalMesh(
                    meshes.curvedVisor, MaterialId.CORRUGATED_METAL,
                    tx = 0.05f, ty = 0.20f, tz = 0f,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.34f, sy = 0.26f, sz = 0.32f,
                    r = 0.28f, g = 0.30f, b = 0.34f,
                    roughness = 0.24f, metallic = 0.85f, emissive = 0.12f
                )
            } else {
                // Level 4 Cyber Respirator Full Mask + Glowing Red LED Eyes!
                drawLocalMesh(
                    meshes.bevelPack, MaterialId.CORRUGATED_METAL,
                    tx = 0.08f, ty = 0.15f, tz = 0f,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.28f, sy = 0.28f, sz = 0.31f,
                    r = 0.14f, g = 0.16f, b = 0.22f,
                    roughness = 0.22f, metallic = 0.82f
                )
                for (ez in listOf(-0.065f, 0.065f)) {
                    drawLocalMesh(
                        meshes.humanEyeOrb, MaterialId.SKIN_DETAIL,
                        tx = 0.20f, ty = 0.22f, tz = ez,
                        rx = 0f, ry = 0f, rz = 0f,
                        sx = 0.045f, sy = 0.036f, sz = 0.052f,
                        r = 1.0f, g = 0.10f, b = 0.14f,
                        roughness = 0.1f, metallic = 0.1f, emissive = 1.0f, useTex = 0f
                    )
                }
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

        // Detailed Multi-Part Held Firearm OR 5 Distinct Melee Weapons + AWM Crimson Butterfly Wings!
        val heldWeapon = player.activeWeapon
        if (heldWeapon != null && !isDriving && !isSwimming && !isSkydiving && !isEmoting) {
            pushMatrix(modelMatrix)
            Matrix.translateM(modelMatrix, 0, 0.32f - recoilKick * 0.08f, 0.44f + recoilKick * 0.03f, 0.18f)
            if (heldWeapon.category == WeaponCategory.MELEE) {
                Matrix.rotateM(modelMatrix, 0, -25f + recoilKick * 58f, 0f, 0f, 1f)
                val (mR, mG, mB) = resolveWeaponSkinColor(heldWeapon.skinName, accR, accG, accB)
                when (heldWeapon.id.lowercase()) {
                    "katana" -> {
                        drawLocalMesh(meshes.ergoGrip, MaterialId.TACTICAL_FABRIC, 0f, 0f, 0f, 0f, 0f, 0f, 0.24f, 0.055f, 0.055f, 0.12f, 0.12f, 0.15f, 0.6f, 0.2f)
                        drawLocalMesh(meshes.katanaBlade, MaterialId.VEHICLE_COAT, 0.52f, 0.02f, 0f, 0f, 0f, 8f, 0.96f, 0.42f, 1.0f, 0.35f, 0.85f, 1.0f, 0.14f, 0.92f, 0.65f)
                    }
                    "scythe" -> {
                        drawLocalMesh(meshes.rifleBarrel, MaterialId.CORRUGATED_METAL, 0.32f, 0f, 0f, 0f, 0f, 0f, 1.15f, 0.045f, 0.045f, 0.18f, 0.18f, 0.22f, 0.3f, 0.85f)
                        drawLocalMesh(meshes.crescentScythe, MaterialId.VEHICLE_COAT, 0.86f, 0.05f, 0f, 0f, 0f, -20f, 0.65f, 0.65f, 0.8f, 1.0f, 0.18f, 0.25f, 0.16f, 0.88f, 0.68f)
                    }
                    "bat" -> {
                        drawLocalMesh(meshes.rifleBarrel, MaterialId.TACTICAL_FABRIC, 0.36f, 0f, 0f, 0f, 0f, 6f, 0.88f, 0.095f, 0.095f, 0.88f, 0.58f, 0.14f, 0.35f, 0.4f, 0.15f)
                    }
                    "machete" -> {
                        drawLocalMesh(meshes.ergoGrip, MaterialId.TACTICAL_FABRIC, 0f, 0f, 0f, 0f, 0f, 0f, 0.18f, 0.05f, 0.05f, 0.14f, 0.14f, 0.16f, 0.6f, 0.2f)
                        drawLocalMesh(meshes.chamferedStructure, MaterialId.CORRUGATED_METAL, 0.38f, 0.02f, 0f, 0f, 0f, 0f, 0.62f, 0.11f, 0.024f, 0.78f, 0.82f, 0.88f, 0.18f, 0.92f)
                    }
                    else -> { // Cast Iron Pan
                        drawLocalMesh(meshes.ergoGrip, MaterialId.TACTICAL_FABRIC, 0.06f, 0f, 0f, 0f, 0f, 0f, 0.24f, 0.045f, 0.045f, 0.12f, 0.12f, 0.14f, 0.6f, 0.2f)
                        drawLocalMesh(meshes.wheelCylinder, MaterialId.VEHICLE_COAT, 0.36f, 0f, 0f, 90f, 0f, 0f, 0.40f, 0.36f, 0.08f, mR, mG, mB, 0.22f, 0.88f, 0.25f)
                    }
                }
            } else {
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

                drawLocalMesh(
                    meshes.weaponReceiver, MaterialId.CORRUGATED_METAL,
                    tx = 0.12f * weaponLenScale, ty = 0f, tz = 0f,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.48f * weaponLenScale.coerceAtLeast(0.55f), sy = 0.135f, sz = 0.095f,
                    r = 0.16f, g = 0.18f, b = 0.22f,
                    roughness = 0.28f, metallic = 0.85f
                )
                drawLocalMesh(
                    meshes.chamferedStructure, MaterialId.VEHICLE_COAT,
                    tx = 0.18f * weaponLenScale, ty = 0.072f, tz = 0f,
                    rx = 0f, ry = 0f, rz = 0f,
                    sx = 0.38f * weaponLenScale.coerceAtLeast(0.55f), sy = 0.036f, sz = 0.078f,
                    r = skinR, g = skinG, b = skinB,
                    roughness = 0.18f, metallic = 0.76f, emissive = 0.62f
                )
                // Twin shotgun barrels for M1887 or triple rotary for Gatling
                if (heldWeapon.id.equals("m1887", ignoreCase = true)) {
                    for (bz in listOf(-0.025f, 0.025f)) {
                        drawLocalMesh(meshes.rifleBarrel, MaterialId.CORRUGATED_METAL, 0.48f * weaponLenScale, 0.02f, bz, 0f, 0f, 0f, 0.46f * weaponLenScale, 0.042f, 0.042f, 0.25f, 0.26f, 0.30f, 0.2f, 0.92f)
                    }
                } else {
                    drawLocalMesh(meshes.rifleBarrel, MaterialId.CORRUGATED_METAL, 0.48f * weaponLenScale, 0.02f, 0f, 0f, 0f, 0f, 0.46f * weaponLenScale, 0.052f, 0.052f, 0.22f, 0.24f, 0.28f, 0.20f, 0.92f)
                }
                if (heldWeapon.equippedMuzzle == MuzzleType.SUPPRESSOR) {
                    drawLocalMesh(meshes.rifleBarrel, MaterialId.CORRUGATED_METAL, 0.74f * weaponLenScale, 0.02f, 0f, 0f, 0f, 0f, 0.22f, 0.078f, 0.078f, 0.10f, 0.11f, 0.13f, 0.30f, 0.82f)
                }
                if (heldWeapon.category != WeaponCategory.PISTOL) {
                    drawLocalMesh(meshes.tacticalStock, MaterialId.TACTICAL_FABRIC, -0.22f * weaponLenScale.coerceAtMost(1.1f), -0.02f, 0f, 0f, 0f, 0f, 0.30f, 0.14f, 0.08f, 0.14f, 0.15f, 0.18f, 0.55f, 0.25f)
                }
                drawLocalMesh(meshes.ergoGrip, MaterialId.TACTICAL_FABRIC, -0.02f, -0.10f, 0f, 0f, 0f, 18f, 0.07f, 0.15f, 0.06f, 0.12f, 0.13f, 0.15f, 0.68f, 0.15f)
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
                if (heldWeapon.category == WeaponCategory.SNIPER_RIFLE || heldWeapon.equippedScope != ScopeType.IRON_SIGHT) {
                    drawLocalMesh(meshes.rifleBarrel, MaterialId.CORRUGATED_METAL, 0.14f, 0.125f, 0f, 0f, 0f, 0f, if (heldWeapon.category == WeaponCategory.SNIPER_RIFLE) 0.34f else 0.18f, 0.06f, 0.06f, 0.10f, 0.12f, 0.16f, 0.15f, 0.90f)
                    drawLocalMesh(meshes.humanEyeOrb, MaterialId.VEHICLE_COAT, 0.24f, 0.125f, 0f, 0f, 0f, 0f, 0.028f, 0.052f, 0.052f, 0.12f, 0.88f, 1.0f, 0.08f, 0.9f, emissive = 0.82f, useTex = 0f)
                    // AWM - Crimson Butterfly Translucent Wings above Sniper Scope!
                    if (heldWeapon.skinName.contains("Butterfly", ignoreCase = true)) {
                        val flapW = sin(frameTimeSec * 4.5f) * 8f
                        drawLocalMesh(meshes.butterflyWing, MaterialId.VEHICLE_COAT, 0.26f, 0.17f, 0.03f, flapW, 0f, 0f, 0.36f, 0.34f, 0.36f, 1.0f, 0.12f, 0.26f, 0.18f, 0.3f, emissive = 0.85f, alpha = 0.86f)
                        drawLocalMesh(meshes.butterflyWing, MaterialId.VEHICLE_COAT, 0.26f, 0.17f, -0.03f, -flapW, 0f, 0f, 0.36f, 0.34f, -0.36f, 1.0f, 0.12f, 0.26f, 0.18f, 0.3f, emissive = 0.85f, alpha = 0.86f)
                    }
                }
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
                drawPart(meshes.sweptAircraftWing, MaterialId.VEHICLE_COAT, px, py + 1.85f, pz, 0f, yaw, 0f, 2.8f, 0.35f, 5.4f, pR, pG, pB, 0.96f, 0.25f, 0.4f, 0.42f)
            } else {
                drawPart(meshes.curvedParachuteCanopy, MaterialId.TACTICAL_FABRIC, px, py + 4.35f, pz, 0f, yaw, 0f, 4.8f, 1.65f, 2.75f, pR, pG, pB, 0.95f, 0.62f, 0.06f, 0.22f)
                val rad = Math.toRadians(player.angle.toDouble())
                val rxV = -sin(rad).toFloat()
                val rzV = cos(rad).toFloat()
                drawPart(meshes.columnBeam, MaterialId.CORRUGATED_METAL, px + rxV * 0.95f, py + 2.4f, pz + rzV * 0.95f, 0f, yaw, -16f, 0.045f, 3.8f, 0.045f, 0.22f, 0.24f, 0.26f, 0.92f, 0.5f, 0.4f, 0f, 0f)
                drawPart(meshes.columnBeam, MaterialId.CORRUGATED_METAL, px - rxV * 0.95f, py + 2.4f, pz - rzV * 0.95f, 0f, yaw, 16f, 0.045f, 3.8f, 0.045f, 0.22f, 0.24f, 0.26f, 0.92f, 0.5f, 0.4f, 0f, 0f)
            }
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
        // Crimson WarX Tactical Thigh Strap & Utility Buckles
        drawLocalMesh(
            meshes.chamferedStructure, MaterialId.VEHICLE_COAT,
            tx = 0f, ty = -0.18f, tz = if (hipOffsetZ > 0f) 0.095f else -0.095f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.14f, sy = 0.048f, sz = 0.032f,
            r = accR, g = accG, b = accB,
            roughness = 0.35f, metallic = 0.28f, emissive = 0.12f
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
        // High-Top WarX Sneaker Collar + Crisp White Upper & Crimson Trim + Lugged Outsole
        drawLocalMesh(
            meshes.taperedBoot, MaterialId.VEHICLE_COAT,
            tx = 0.05f, ty = -0.42f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.24f, sy = 0.14f, sz = 0.15f,
            r = 0.94f, g = 0.95f, b = 0.97f,
            roughness = 0.40f, metallic = 0.12f
        )
        drawLocalMesh(
            meshes.chamferedStructure, MaterialId.VEHICLE_COAT,
            tx = -0.01f, ty = -0.35f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.155f, sy = 0.075f, sz = 0.148f,
            r = accR, g = accG, b = accB,
            roughness = 0.34f, metallic = 0.22f, emissive = 0.08f
        )
        drawLocalMesh(
            meshes.chamferedStructure, MaterialId.VEHICLE_COAT,
            tx = 0.05f, ty = -0.475f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.25f, sy = 0.045f, sz = 0.152f,
            r = accR * 0.85f, g = accG * 0.85f, b = accB * 0.85f,
            roughness = 0.35f, metallic = 0.25f
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
                    drawPart(meshes.chamferedStructure, MaterialId.VEHICLE_COAT, wolfX + fx * lx + rx * lz, wolfY - 0.44f, wolfZ + fz * lx + rz * lz, 0f, yaw, 0f, 0.13f, 0.08f, 0.13f, gr, gg, gb, 1f, 0.3f, 0.4f, 0.65f, 0f)
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
