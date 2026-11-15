package com.example.game.graphics

import android.opengl.GLES20
import android.opengl.Matrix
import com.example.game.model.CharacterPreset
import com.example.game.model.PetPreset
import com.example.game.model.Player
import com.example.game.model.PlayerStance
import com.example.game.model.VehicleInstance
import com.example.game.model.VehicleType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders articulated hierarchical 3D human characters (with sculpted head, hair, face,
 * tactical vest, limb joints, boots, backpack, helmet, visor, and multi-part weapons)
 * and sculpted multi-part 3D vehicles with rotating cylindrical wheels and glass reflections.
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
    private val stack = Array(12) { FloatArray(16) }
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
        frameTimeSec: Float
    ) {
        val px = player.x * TerrainHeightfield.WORLD_SCALE
        val py = groundY + player.z * TerrainHeightfield.WORLD_SCALE
        val pz = player.y * TerrainHeightfield.WORLD_SCALE
        val yaw = -player.angle

        val preset = CharacterPreset.fromId(player.characterPresetId)
        val isTeammate = player.teamId == userTeamId
        val (armorR, armorG, armorB) = if (player.isUser || isTeammate) preset.armorRgb else Triple(0.46f, 0.14f, 0.14f)
        val (accR, accG, accB) = if (player.isUser || isTeammate) preset.accentRgb else Triple(0.95f, 0.25f, 0.15f)
        val (visR, visG, visB) = if (player.isUser || isTeammate) preset.visorRgb else Triple(1.0f, 0.15f, 0.15f)

        val skinTone = when (preset) {
            CharacterPreset.VORTEX_GHOST, CharacterPreset.FROST_RECON -> Triple(0.84f, 0.68f, 0.56f)
            CharacterPreset.NOVA_VALKYRIE, CharacterPreset.SERAPH_ECHO -> Triple(0.88f, 0.72f, 0.62f)
            CharacterPreset.BLAZE_STRIKER -> Triple(0.56f, 0.38f, 0.28f)
            else -> Triple(0.76f, 0.60f, 0.48f)
        }
        val hairRgb = when (preset) {
            CharacterPreset.NOVA_VALKYRIE -> Triple(0.72f, 0.16f, 0.38f)
            CharacterPreset.CYBER_VIPER -> Triple(0.12f, 0.58f, 0.72f)
            CharacterPreset.FROST_RECON -> Triple(0.86f, 0.88f, 0.92f)
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

        // Animation phase & posture blending
        val speedNorm = (player.currentSpeed / 260f).coerceIn(0f, 1.1f)
        val strideSin = sin(player.animWalkPhase) * speedNorm
        val strideCos = cos(player.animWalkPhase) * speedNorm
        val breathe = sin(frameTimeSec * 2.8f) * 0.025f
        val landDip = if (player.landingRecoverySec > 0f) sin((player.landingRecoverySec / 0.35f) * Math.PI).toFloat() * 0.22f else 0f
        val vaultLift = if (player.climbVaultTimerSec > 0f) sin((player.climbVaultTimerSec / 0.45f) * Math.PI).toFloat() * 0.45f else 0f

        val isProne = player.stance == PlayerStance.PRONE
        val isCrouch = player.stance == PlayerStance.CROUCH
        val isSwimming = player.stance == PlayerStance.SWIMMING
        val isDriving = player.stance == PlayerStance.DRIVING || player.vehicleId != null
        val isSkydiving = player.z > 15f
        val isJumping = player.stance == PlayerStance.JUMPING || (player.z > 1.5f && !isSkydiving)
        val isEmoting = player.emoteAnimSec > 0f

        // Compute Root Transform in world space (+X forward in character local frame)
        Matrix.setIdentityM(modelMatrix, 0)
        val pelvisY = when {
            isProne -> py + 0.22f
            isSwimming -> py - 0.10f + sin(frameTimeSec * 4.5f) * 0.05f
            isDriving -> py + 0.65f
            isCrouch -> py + 0.62f - landDip
            else -> py + 0.94f - landDip + vaultLift + abs(strideSin) * 0.04f
        }
        Matrix.translateM(modelMatrix, 0, px, pelvisY, pz)
        Matrix.rotateM(modelMatrix, 0, yaw, 0f, 1f, 0f)

        if (isProne || isSwimming) {
            Matrix.rotateM(modelMatrix, 0, 82f, 0f, 0f, -1f)
        } else if (isSkydiving) {
            Matrix.rotateM(modelMatrix, 0, 38f, 0f, 0f, -1f)
        } else if (speedNorm > 0.1f && !isDriving) {
            // Dynamic forward sprint/run lean
            Matrix.rotateM(modelMatrix, 0, if (player.isSprinting) 14f else 6f, 0f, 0f, -1f)
        }

        val pantsColor = Triple(armorR * 0.45f + 0.06f, armorG * 0.45f + 0.07f, armorB * 0.45f + 0.08f)

        // --- HIPS & LEGS ---
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
            else -> (-strideCos * 28f).coerceAtLeast(6f)
        }
        val rightKneeBend = when {
            isDriving -> 78f
            isCrouch -> 78f
            isJumping -> 24f
            else -> (strideCos * 28f).coerceAtLeast(6f)
        }

        renderLeg(+0.14f, leftHipPitch, leftKneeBend, pantsColor, accR, accG, accB)
        renderLeg(-0.14f, rightHipPitch, rightKneeBend, pantsColor, accR, accG, accB)

        // --- TORSO & VEST & BACKPACK ---
        pushMatrix(modelMatrix)
        val torsoLean = when {
            isCrouch -> 18f
            isEmoting -> sin(frameTimeSec * 8f) * 10f
            else -> breathe * 60f
        }
        Matrix.rotateM(modelMatrix, 0, torsoLean, 0f, 0f, -1f)

        // Anatomical Waist/Chest Torso (jacket/combat shirt)
        drawLocalMesh(
            meshes.chestTorso, MaterialId.TACTICAL_FABRIC,
            tx = 0f, ty = 0.36f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.34f, sy = 0.62f, sz = 0.56f,
            r = armorR, g = armorG, b = armorB,
            roughness = 0.68f, metallic = 0.12f
        )
        // Armor Tactical Vest & Glowing Accent Stripe
        if (player.armorLevel > 0) {
            drawLocalMesh(
                meshes.bevelPack, MaterialId.TACTICAL_FABRIC,
                tx = 0f, ty = 0.38f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.38f, sy = 0.42f, sz = 0.54f,
                r = armorR * 0.72f, g = armorG * 0.72f, b = armorB * 0.72f,
                roughness = 0.48f, metallic = 0.35f
            )
            drawLocalMesh(
                meshes.unitBox, MaterialId.VEHICLE_COAT,
                tx = 0.18f, ty = 0.44f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.05f, sy = 0.14f, sz = 0.44f,
                r = accR, g = accG, b = accB,
                roughness = 0.25f, metallic = 0.6f, emissive = 0.65f
            )
        }

        // Backpack & Slung Secondary Rifle (signature FF MAX silhouette)
        if (player.backpackLevel > 0 && !isDriving) {
            drawLocalMesh(
                meshes.bevelPack, MaterialId.TACTICAL_FABRIC,
                tx = -0.26f, ty = 0.38f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.24f, sy = 0.50f, sz = 0.40f,
                r = 0.14f, g = 0.16f, b = 0.19f,
                roughness = 0.65f, metallic = 0.15f
            )
            if (player.isUser || player.primaryWeapon2 != null) {
                drawLocalMesh(
                    meshes.rifleBarrel, MaterialId.CORRUGATED_METAL,
                    tx = -0.34f, ty = 0.52f, tz = -0.10f,
                    rx = 0f, ry = 0f, rz = 65f,
                    sx = 0.92f, sy = 0.07f, sz = 0.07f,
                    r = 0.18f, g = 0.22f, b = 0.26f,
                    roughness = 0.28f, metallic = 0.85f
                )
                drawLocalMesh(
                    meshes.unitBox, MaterialId.VEHICLE_COAT,
                    tx = -0.34f, ty = 0.54f, tz = -0.10f,
                    rx = 0f, ry = 0f, rz = 65f,
                    sx = 0.62f, sy = 0.04f, sz = 0.04f,
                    r = accR, g = accG, b = accB,
                    roughness = 0.2f, metallic = 0.6f, emissive = 0.78f
                )
            }
        }

        // --- NECK & SCULPTED HEAD + FACE + HAIR + HELMET ---
        pushMatrix(modelMatrix)
        Matrix.translateM(modelMatrix, 0, 0f, 0.74f, 0f)
        if (isSwimming || isProne) {
            Matrix.rotateM(modelMatrix, 0, -68f, 0f, 0f, -1f)
        }
        // Neck
        drawLocalMesh(
            meshes.limbCapsule, MaterialId.SKIN_DETAIL,
            tx = 0f, ty = 0.0f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.13f, sy = 0.16f, sz = 0.13f,
            r = skinTone.first * 0.92f, g = skinTone.second * 0.92f, b = skinTone.third * 0.92f,
            roughness = 0.62f, metallic = 0f
        )
        // Sculpted Head Cranium & Jaw
        drawLocalMesh(
            meshes.humanHead, MaterialId.SKIN_DETAIL,
            tx = 0.01f, ty = 0.18f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.34f, sy = 0.36f, sz = 0.31f,
            r = skinTone.first, g = skinTone.second, b = skinTone.third,
            roughness = 0.55f, metallic = 0.02f
        )
        // Hair Crown
        drawLocalMesh(
            meshes.humanHair, MaterialId.TACTICAL_FABRIC,
            tx = -0.01f, ty = 0.24f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.35f, sy = 0.32f, sz = 0.32f,
            r = hairRgb.first, g = hairRgb.second, b = hairRgb.third,
            roughness = 0.72f, metallic = 0.08f
        )
        // Eyes / Facial Brow Detail (when no full-face visor hides it)
        if (player.helmetLevel < 3) {
            drawLocalMesh(
                meshes.unitBox, MaterialId.SKIN_DETAIL,
                tx = 0.15f, ty = 0.21f, tz = 0.06f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.04f, sy = 0.03f, sz = 0.05f,
                r = 0.08f, g = 0.08f, b = 0.10f,
                roughness = 0.2f, metallic = 0.2f, useTex = 0f
            )
            drawLocalMesh(
                meshes.unitBox, MaterialId.SKIN_DETAIL,
                tx = 0.15f, ty = 0.21f, tz = -0.06f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.04f, sy = 0.03f, sz = 0.05f,
                r = 0.08f, g = 0.08f, b = 0.10f,
                roughness = 0.2f, metallic = 0.2f, useTex = 0f
            )
        }
        // Tactical Helmet + Reflective Curved Visor
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
        popMatrix(modelMatrix)

        // --- SHOULDERS, ARMS & HELD DETAILED WEAPON ---
        val rightArmRaise = when {
            isEmoting -> 125f + sin(frameTimeSec * 10f) * 28f
            isDriving -> 55f
            isSwimming -> 75f + sin(frameTimeSec * 7f) * 65f
            player.activeWeapon != null -> 68f
            else -> -strideSin * 26f
        }
        val leftArmRaise = when {
            isEmoting -> 45f + cos(frameTimeSec * 8f) * 22f
            isDriving -> 55f
            isSwimming -> 75f - sin(frameTimeSec * 7f) * 65f
            player.activeWeapon != null -> 58f
            else -> strideSin * 26f
        }
        renderArm(+0.27f, leftArmRaise, 28f, armorR, armorG, armorB, skinTone)
        renderArm(-0.27f, rightArmRaise, 22f, armorR, armorG, armorB, skinTone)

        // Equipped Held Weapon + Muzzle Flash
        if (player.activeWeapon != null && !isDriving && !isSwimming && !isSkydiving && !isEmoting) {
            pushMatrix(modelMatrix)
            Matrix.translateM(modelMatrix, 0, 0.32f, 0.44f, 0.18f)
            // Receiver
            drawLocalMesh(
                meshes.bevelPack, MaterialId.CORRUGATED_METAL,
                tx = 0.12f, ty = 0f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.52f, sy = 0.14f, sz = 0.10f,
                r = 0.16f, g = 0.18f, b = 0.22f,
                roughness = 0.32f, metallic = 0.82f
            )
            // Cylindrical Fluted Barrel
            drawLocalMesh(
                meshes.rifleBarrel, MaterialId.CORRUGATED_METAL,
                tx = 0.52f, ty = 0.02f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.48f, sy = 0.055f, sz = 0.055f,
                r = 0.22f, g = 0.24f, b = 0.28f,
                roughness = 0.22f, metallic = 0.90f
            )
            // Accent Skin Rail + Magazine + Optic Scope
            drawLocalMesh(
                meshes.unitBox, MaterialId.VEHICLE_COAT,
                tx = 0.18f, ty = 0.075f, tz = 0f,
                rx = 0f, ry = 0f, rz = 0f,
                sx = 0.38f, sy = 0.035f, sz = 0.075f,
                r = accR, g = accG, b = accB,
                roughness = 0.20f, metallic = 0.7f, emissive = 0.65f
            )
            drawLocalMesh(
                meshes.unitBox, MaterialId.CORRUGATED_METAL,
                tx = 0.16f, ty = -0.11f, tz = 0f,
                rx = 0f, ry = 0f, rz = -12f,
                sx = 0.09f, sy = 0.20f, sz = 0.06f,
                r = 0.14f, g = 0.15f, b = 0.18f,
                roughness = 0.4f, metallic = 0.7f
            )

            // Muzzle Flash Star Burst
            if (System.currentTimeMillis() - player.lastFireTimestampMs < 90L) {
                drawLocalMesh(
                    meshes.boulderRock, MaterialId.SKIN_DETAIL,
                    tx = 0.84f, ty = 0.02f, tz = 0f,
                    rx = frameTimeSec * 400f, ry = 0f, rz = 0f,
                    sx = 0.42f, sy = 0.28f, sz = 0.28f,
                    r = 1.0f, g = 0.78f, b = 0.15f,
                    roughness = 0.5f, metallic = 0f, emissive = 1.0f, useTex = 0f, alpha = 0.92f
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

        // Parachute Canopy & Harness Risers during Skydiving
        if (isSkydiving) {
            drawPart(
                meshes.helmetShell, MaterialId.TACTICAL_FABRIC,
                px, py + 4.3f, pz, 0f, yaw, 0f, 4.4f, 1.6f, 2.8f,
                1.0f, 0.45f, 0.05f, 0.94f, roughness = 0.65f, metallic = 0.05f, emissive = 0.22f
            )
            val rad = Math.toRadians(player.angle.toDouble())
            val rxV = -sin(rad).toFloat()
            val rzV = cos(rad).toFloat()
            drawPart(meshes.unitBox, MaterialId.CORRUGATED_METAL, px + rxV * 0.9f, py + 2.4f, pz + rzV * 0.9f, 0f, yaw, 0f, 0.05f, 3.6f, 0.05f, 0.2f, 0.2f, 0.2f, 0.9f, 0.6f, 0.4f, 0f, 0f)
            drawPart(meshes.unitBox, MaterialId.CORRUGATED_METAL, px - rxV * 0.9f, py + 2.4f, pz - rzV * 0.9f, 0f, yaw, 0f, 0.05f, 3.6f, 0.05f, 0.2f, 0.2f, 0.2f, 0.9f, 0.6f, 0.4f, 0f, 0f)
        }
    }

    private fun renderLeg(
        hipOffsetZ: Float,
        hipPitchDeg: Float,
        kneeBendDeg: Float,
        pantsRgb: Triple<Float, Float, Float>,
        accR: Float, accG: Float, accB: Float
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
        // Knee + Calf
        Matrix.translateM(modelMatrix, 0, 0f, -0.44f, 0f)
        Matrix.rotateM(modelMatrix, 0, -kneeBendDeg, 0f, 0f, 1f)
        drawLocalMesh(
            meshes.limbCapsule, MaterialId.TACTICAL_FABRIC,
            tx = 0f, ty = -0.21f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.16f, sy = 0.42f, sz = 0.16f,
            r = pantsRgb.first * 0.88f, g = pantsRgb.second * 0.88f, b = pantsRgb.third * 0.88f,
            roughness = 0.72f, metallic = 0.05f
        )
        // Tactical Combat Boot
        drawLocalMesh(
            meshes.taperedBoot, MaterialId.CORRUGATED_METAL,
            tx = 0.05f, ty = -0.42f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.24f, sy = 0.14f, sz = 0.15f,
            r = 0.12f, g = 0.12f, b = 0.14f,
            roughness = 0.45f, metallic = 0.25f
        )
        // Boot Accent Sole/Lace highlight
        drawLocalMesh(
            meshes.unitBox, MaterialId.VEHICLE_COAT,
            tx = 0.05f, ty = -0.47f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.25f, sy = 0.04f, sz = 0.15f,
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
            meshes.boulderRock, MaterialId.TACTICAL_FABRIC,
            tx = 0f, ty = 0f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.18f, sy = 0.16f, sz = 0.18f,
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
        // Forearm & Gloved Hand
        Matrix.translateM(modelMatrix, 0, 0f, -0.30f, 0f)
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
            meshes.boulderRock, MaterialId.TACTICAL_FABRIC,
            tx = 0f, ty = -0.27f, tz = 0f,
            rx = 0f, ry = 0f, rz = 0f,
            sx = 0.11f, sy = 0.10f, sz = 0.10f,
            r = 0.12f, g = 0.12f, b = 0.14f,
            roughness = 0.5f, metallic = 0.2f
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
        drawPart(meshes.pitchedRoof, MaterialId.VEHICLE_COAT, petX, petY + 0.14f, petZ, 0f, yaw, flap, 0.28f, 0.12f, 1.18f, gr, gg, gb, 0.94f, 0.25f, 0.6f, 0.7f)
        drawPart(meshes.unitBox, MaterialId.SKIN_DETAIL, petX + fx * 0.20f, petY + 0.08f, petZ + fz * 0.20f, 0f, yaw, 0f, 0.08f, 0.09f, 0.24f, 1.0f, 0.92f, 0.25f, 1f, 0.1f, 0.2f, 1f, 0f)
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

        // Main Sculpted Lower Chassis & Sloped Hood/Bumper
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
        // Front Bumper & Grille
        drawPart(
            meshes.unitBox, MaterialId.CORRUGATED_METAL,
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
        // Headlights (warm emissive glow) & Taillights (crimson)
        drawPart(meshes.limbCapsule, MaterialId.SKIN_DETAIL, vx + fx * 2.26f + rx * 0.68f, groundY + 0.78f + bounce, vz + fz * 2.26f + rz * 0.68f, 0f, rot, 90f, 0.26f, 0.12f, 0.32f, 1.0f, 0.95f, 0.65f, 1f, 0.1f, 0.2f, 1f, 0f)
        drawPart(meshes.limbCapsule, MaterialId.SKIN_DETAIL, vx + fx * 2.26f - rx * 0.68f, groundY + 0.78f + bounce, vz + fz * 2.26f - rz * 0.68f, 0f, rot, 90f, 0.26f, 0.12f, 0.32f, 1.0f, 0.95f, 0.65f, 1f, 0.1f, 0.2f, 1f, 0f)
        drawPart(meshes.unitBox, MaterialId.SKIN_DETAIL, vx - fx * 2.25f + rx * 0.70f, groundY + 0.80f + bounce, vz - fz * 2.25f + rz * 0.70f, 0f, rot, 0f, 0.12f, 0.20f, 0.32f, 0.95f, 0.12f, 0.12f, 1f, 0.2f, 0.2f, 0.85f, 0f)
        drawPart(meshes.unitBox, MaterialId.SKIN_DETAIL, vx - fx * 2.25f - rx * 0.70f, groundY + 0.80f + bounce, vz - fz * 2.25f - rz * 0.70f, 0f, rot, 0f, 0.12f, 0.20f, 0.32f, 0.95f, 0.12f, 0.12f, 1f, 0.2f, 0.2f, 0.85f, 0f)

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
}
