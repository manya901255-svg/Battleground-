package com.example.game.graphics

import com.example.game.map.MapConfig
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Interleaved 3D mesh buffer: [px, py, pz, nx, ny, nz, u, v] (8 floats = 32 bytes per vertex).
 */
class GlMesh(
    val buffer: FloatBuffer,
    val vertexCount: Int
)

/**
 * High-resolution procedural 3D mesh library providing smooth-normal anatomical human figures
 * (detailed cranium, facial jaw/cheekbones, lips, nose, ears, eyeballs, eyebrows, layered hair &
 * bangs, muscular torso, spherical-capped limbs, articulated palms, thumbs, and fingers, combat
 * sneakers/boots), realistic vehicles, firearms, botanical foliage, and detailed architecture.
 */
class MeshLibrary {
    val skyDome: GlMesh = cachedStatic("skyDome_v2") { buildSphere(stacks = 14, slices = 28, inward = true) }
    val softShadowDisc: GlMesh = cachedStatic("softShadowDisc_v2") { buildDisc(segments = 24) }
    val quadBillboard: GlMesh = cachedStatic("quadBillboard") { buildQuad() }

    // Articulated human anatomical & facial meshes (all smooth vertex normals)
    val humanHead: GlMesh = cachedStatic("humanHead_v3") { buildSculptedHead() }
    val humanEyeOrb: GlMesh = cachedStatic("humanEyeOrb_v2") { buildSphere(stacks = 8, slices = 14, inward = false) }
    val humanNose: GlMesh = cachedStatic("humanNose_v2") { buildSculptedNose() }
    val humanEar: GlMesh = cachedStatic("humanEar_v2") { buildChamferedPrism(bevel = 0.28f, topScaleX = 0.85f, topScaleZ = 0.72f) }
    val browRidge: GlMesh = cachedStatic("browRidge_v2") { buildCurvedArc(slices = 8, arcAngle = PI * 0.45, thick = 0.22f) }
    val facialMouth: GlMesh = cachedStatic("facialMouth_v2") { buildSculptedLips() }
    val humanHair: GlMesh = cachedStatic("humanHair_v3") { buildHairCrown() }
    val hairBangs: GlMesh = cachedStatic("hairBangs_v1") { buildHairBangs() }
    val hairPonytail: GlMesh = cachedStatic("hairPonytail_v2") { buildCurvedPlume() }
    val jacketCollar: GlMesh = cachedStatic("jacketCollar_v2") { buildCollarWrap() }
    val helmetShell: GlMesh = cachedStatic("helmetShell_v2") { buildHemisphereShell(stacks = 10, slices = 20) }
    val curvedVisor: GlMesh = cachedStatic("curvedVisor_v2") { buildCurvedVisor() }
    val chestTorso: GlMesh = cachedStatic("chestTorso_v4") { buildAnatomicalTorso() }
    val pelvisHip: GlMesh = cachedStatic("pelvisHip_v1") { buildPelvisHip() }
    val contouredVest: GlMesh = cachedStatic("contouredVest_v1") { buildContouredVest() }
    val tacticalBackpack: GlMesh = cachedStatic("tacticalBackpack_v1") { buildTacticalBackpack() }
    val contouredHalfMask: GlMesh = cachedStatic("contouredHalfMask_v1") { buildContouredHalfMask() }
    val curvedBand: GlMesh = cachedStatic("curvedBand_v1") { buildCurvedBand() }
    val crownDiadem: GlMesh = cachedStatic("crownDiadem_v1") { buildCrownDiadem() }
    val tacticalCapBrim: GlMesh = cachedStatic("tacticalCapBrim_v1") { buildCurvedCapBrim() }
    val limbCapsule: GlMesh = cachedStatic("limbCapsule_v3") { buildSmoothCapsule(slices = 16, stacks = 6, topScale = 1.0f, botScale = 0.78f) }
    val taperedBoot: GlMesh = cachedStatic("taperedBoot_v3") { buildSculptedBoot() }
    val contouredSole: GlMesh = cachedStatic("contouredSole_v1") { buildSculptedSole() }
    val bevelPack: GlMesh = cachedStatic("bevelPack_v3") { buildChamferedPrism(bevel = 0.16f, topScaleX = 0.86f, topScaleZ = 0.84f) }
    val kneePadShell: GlMesh = cachedStatic("kneePadShell_v2") { buildSmoothCapsule(slices = 12, stacks = 4, topScale = 0.92f, botScale = 0.88f) }
    val humanHandGlove: GlMesh = cachedStatic("humanHandGlove_v2") { buildSculptedHandPalm() }
    val sculptedFingers: GlMesh = cachedStatic("sculptedFingers_v1") { buildSculptedFingers() }
    val sculptedThumb: GlMesh = cachedStatic("sculptedThumb_v1") { buildSmoothCapsule(slices = 10, stacks = 4, topScale = 0.9f, botScale = 0.72f) }

    // Vehicles
    val wheelCylinder: GlMesh = cachedStatic("wheelCylinder_v2") { buildSmoothCylinder(slices = 22, axis = 2, radiusTop = 1f, radiusBot = 1f) }
    val chassisSlopeBody: GlMesh = cachedStatic("chassisSlopeBody_v3") { buildChamferedPrism(bevel = 0.14f, topScaleX = 0.86f, topScaleZ = 0.88f) }
    val cabSlopedRoof: GlMesh = cachedStatic("cabSlopedRoof_v3") { buildChamferedPrism(bevel = 0.14f, topScaleX = 0.70f, topScaleZ = 0.80f) }

    // Detailed multi-part Weapons
    val rifleBarrel: GlMesh = cachedStatic("rifleBarrel_v3") { buildSmoothCylinder(slices = 14, axis = 0, radiusTop = 0.88f, radiusBot = 1.0f) }
    val weaponReceiver: GlMesh = cachedStatic("weaponReceiver_v2") { buildChamferedPrism(bevel = 0.20f, topScaleX = 0.94f, topScaleZ = 0.92f) }
    val ergoGrip: GlMesh = cachedStatic("ergoGrip_v2") { buildChamferedPrism(bevel = 0.22f, topScaleX = 0.86f, topScaleZ = 0.88f) }
    val curvedMagazine: GlMesh = cachedStatic("curvedMagazine") { buildCurvedMagazine() }
    val tacticalStock: GlMesh = cachedStatic("tacticalStock") { buildTaperedStock() }
    val opticBody: GlMesh = cachedStatic("opticBody") { buildChamferedPrism(bevel = 0.20f, topScaleX = 0.88f, topScaleZ = 0.88f) }
    val katanaBlade: GlMesh = cachedStatic("katanaBlade") { buildKatanaBlade() }
    val crescentScythe: GlMesh = cachedStatic("crescentScythe") { buildCrescentScythe() }
    val butterflyWing: GlMesh = cachedStatic("butterflyWing") { buildButterflyWing() }
    val kitsuneEar: GlMesh = cachedStatic("kitsuneEar") { buildCone(slices = 8, droopAmount = 0.05f) }

    // Environment organic, architectural, military & aircraft meshes
    val treeTrunk: GlMesh = cachedStatic("treeTrunk_v2") { buildSmoothCylinder(slices = 14, axis = 1, radiusTop = 0.54f, radiusBot = 1.32f) }
    val columnBeam: GlMesh = cachedStatic("columnBeam_v2") { buildSmoothCylinder(slices = 12, axis = 1, radiusTop = 0.96f, radiusBot = 1.0f) }
    val foliageCone: GlMesh = cachedStatic("foliageCone_v2") { buildSmoothCone(slices = 14, droopAmount = 0.18f) }
    val foliageCrown: GlMesh = cachedStatic("foliageCrown_v1") { buildOrganicCanopy(stacks = 8, slices = 14) }
    val boulderRock: GlMesh = cachedStatic("boulderRock_v2") { buildDisplacedBoulder(stacks = 9, slices = 14) }
    val bushCluster: GlMesh = cachedStatic("bushCluster_v2") { buildOrganicCanopy(stacks = 6, slices = 11) }
    val grassTuft: GlMesh = cachedStatic("grassTuft") { buildGrassCrossTuft() }
    val pitchedRoof: GlMesh = cachedStatic("pitchedRoof") { buildGableRoof() }
    val slopedAwningRoof: GlMesh = cachedStatic("slopedAwningRoof") { buildGableRoofWithEaves() }
    val hippedShingleRoof: GlMesh = cachedStatic("hippedShingleRoof_v1") { buildHippedRoofWithEaves() }
    val staircaseFlight: GlMesh = cachedStatic("staircaseFlight_v1") { buildStaircaseFlight(steps = 3) }
    val chamferedStructure: GlMesh = cachedStatic("chamferedStructure") { buildChamferedPrism(bevel = 0.06f, topScaleX = 0.98f, topScaleZ = 0.98f) }
    val corrugatedContainer: GlMesh = cachedStatic("corrugatedContainer") { buildCorrugatedContainer(ribs = 6) }
    val bridgeArchTruss: GlMesh = cachedStatic("bridgeArchTruss") { buildArchTruss(segments = 8) }
    val sandbagRow: GlMesh = cachedStatic("sandbagRow") { buildSandbagWall(bags = 4) }
    val militaryTent: GlMesh = cachedStatic("militaryTent") { buildMilitaryTent() }
    val radomeSphere: GlMesh = cachedStatic("radomeSphere_v2") { buildSphere(stacks = 10, slices = 16, inward = false) }
    val sweptAircraftWing: GlMesh = cachedStatic("sweptAircraftWing") { buildSweptWing() }
    val aircraftTailFin: GlMesh = cachedStatic("aircraftTailFin") { buildTailFin() }
    val curvedParachuteCanopy: GlMesh = cachedStatic("curvedParachuteCanopy") { buildRamAirCanopy(cells = 7) }
    val shieldHexBarrier: GlMesh = cachedStatic("shieldHexBarrier") { buildCurvedShield(segments = 6) }
    val bannerClothStrip: GlMesh = cachedStatic("bannerClothStrip") { buildWavedCloth() }
    val waterGrid: GlMesh = cachedStatic("waterGrid") { buildWaterPlaneGrid(12) }

    fun buildTerrainMeshForMap(mapConfig: MapConfig, gridRes: Int = 48): GlMesh {
        val key = "${mapConfig.id}_$gridRes"
        sharedTerrainCache[key]?.let { tpl ->
            return GlMesh(tpl.buffer.duplicate().apply { position(0) }, tpl.vertexCount)
        }
        val out = FloatArrayList(gridRes * gridRes * 6 * 8)
        val stepGl = 500f / gridRes
        for (gz in 0 until gridRes) {
            for (gx in 0 until gridRes) {
                val x0 = gx * stepGl
                val x1 = (gx + 1) * stepGl
                val z0 = gz * stepGl
                val z1 = (gz + 1) * stepGl

                val eX0 = x0 / TerrainHeightfield.WORLD_SCALE
                val eX1 = x1 / TerrainHeightfield.WORLD_SCALE
                val eZ0 = z0 / TerrainHeightfield.WORLD_SCALE
                val eZ1 = z1 / TerrainHeightfield.WORLD_SCALE

                val h00 = TerrainHeightfield.groundHeightGl(mapConfig, eX0, eZ0)
                val h10 = TerrainHeightfield.groundHeightGl(mapConfig, eX1, eZ0)
                val h01 = TerrainHeightfield.groundHeightGl(mapConfig, eX0, eZ1)
                val h11 = TerrainHeightfield.groundHeightGl(mapConfig, eX1, eZ1)

                val n00 = TerrainHeightfield.terrainNormalGl(mapConfig, eX0, eZ0)
                val n10 = TerrainHeightfield.terrainNormalGl(mapConfig, eX1, eZ0)
                val n01 = TerrainHeightfield.terrainNormalGl(mapConfig, eX0, eZ1)
                val n11 = TerrainHeightfield.terrainNormalGl(mapConfig, eX1, eZ1)

                val uvScale = 0.18f
                out.addVertex(x0, h00, z0, n00.x, n00.y, n00.z, x0 * uvScale, z0 * uvScale)
                out.addVertex(x0, h01, z1, n01.x, n01.y, n01.z, x0 * uvScale, z1 * uvScale)
                out.addVertex(x1, h10, z0, n10.x, n10.y, n10.z, x1 * uvScale, z0 * uvScale)

                out.addVertex(x1, h10, z0, n10.x, n10.y, n10.z, x1 * uvScale, z0 * uvScale)
                out.addVertex(x0, h01, z1, n01.x, n01.y, n01.z, x0 * uvScale, z1 * uvScale)
                out.addVertex(x1, h11, z1, n11.x, n11.y, n11.z, x1 * uvScale, z1 * uvScale)
            }
        }
        val built = out.toMesh()
        sharedTerrainCache.putIfAbsent(key, built)
        return GlMesh(built.buffer.duplicate().apply { position(0) }, built.vertexCount)
    }

    private fun buildQuad(): GlMesh {
        val d = floatArrayOf(
            -0.5f, -0.5f, 0f, 0f, 0f, 1f, 0f, 0f,
             0.5f, -0.5f, 0f, 0f, 0f, 1f, 1f, 0f,
             0.5f,  0.5f, 0f, 0f, 0f, 1f, 1f, 1f,
            -0.5f, -0.5f, 0f, 0f, 0f, 1f, 0f, 0f,
             0.5f,  0.5f, 0f, 0f, 0f, 1f, 1f, 1f,
            -0.5f,  0.5f, 0f, 0f, 0f, 1f, 0f, 1f
        )
        return arrayToMesh(d)
    }

    private fun buildWaterPlaneGrid(div: Int): GlMesh {
        val out = FloatArrayList(div * div * 6 * 8)
        val step = 1f / div
        for (jz in 0 until div) {
            for (ix in 0 until div) {
                val x0 = -0.5f + ix * step
                val x1 = x0 + step
                val z0 = -0.5f + jz * step
                val z1 = z0 + step
                val u0 = ix.toFloat() * 2f
                val u1 = (ix + 1).toFloat() * 2f
                val v0 = jz.toFloat() * 2f
                val v1 = (jz + 1).toFloat() * 2f
                out.addVertex(x0, 0f, z0, 0f, 1f, 0f, u0, v0)
                out.addVertex(x0, 0f, z1, 0f, 1f, 0f, u0, v1)
                out.addVertex(x1, 0f, z0, 0f, 1f, 0f, u1, v0)
                out.addVertex(x1, 0f, z0, 0f, 1f, 0f, u1, v0)
                out.addVertex(x0, 0f, z1, 0f, 1f, 0f, u0, v1)
                out.addVertex(x1, 0f, z1, 0f, 1f, 0f, u1, v1)
            }
        }
        return out.toMesh()
    }

    private fun buildDisc(segments: Int): GlMesh {
        val out = FloatArrayList(segments * 3 * 8)
        val step = (2.0 * PI) / segments
        for (i in 0 until segments) {
            val a0 = i * step
            val a1 = (i + 1) * step
            out.addVertex(0f, 0f, 0f, 0f, 1f, 0f, 0.5f, 0.5f)
            out.addVertex((cos(a0) * 0.5).toFloat(), 0f, (sin(a0) * 0.5).toFloat(), 0f, 1f, 0f, (0.5 + 0.5 * cos(a0)).toFloat(), (0.5 + 0.5 * sin(a0)).toFloat())
            out.addVertex((cos(a1) * 0.5).toFloat(), 0f, (sin(a1) * 0.5).toFloat(), 0f, 1f, 0f, (0.5 + 0.5 * cos(a1)).toFloat(), (0.5 + 0.5 * sin(a1)).toFloat())
        }
        return out.toMesh()
    }

    private fun buildSphere(stacks: Int, slices: Int, inward: Boolean): GlMesh {
        val out = FloatArrayList(stacks * slices * 6 * 8)
        for (i in 0 until stacks) {
            val phi0 = PI * i / stacks - PI * 0.5
            val phi1 = PI * (i + 1) / stacks - PI * 0.5
            for (j in 0 until slices) {
                val th0 = 2.0 * PI * j / slices
                val th1 = 2.0 * PI * (j + 1) / slices
                val p00 = spherePoint(phi0, th0)
                val p10 = spherePoint(phi1, th0)
                val p01 = spherePoint(phi0, th1)
                val p11 = spherePoint(phi1, th1)
                val s = if (inward) -1f else 1f
                if (inward) {
                    out.addSphereTri(p00, p01, p10, s)
                    out.addSphereTri(p10, p01, p11, s)
                } else {
                    out.addSphereTri(p00, p10, p01, s)
                    out.addSphereTri(p10, p11, p01, s)
                }
            }
        }
        return out.toMesh()
    }

    private fun buildHemisphereShell(stacks: Int, slices: Int): GlMesh {
        val out = FloatArrayList(stacks * slices * 6 * 8)
        for (i in 0 until stacks) {
            val phi0 = (PI * 0.55) * i / stacks - PI * 0.05
            val phi1 = (PI * 0.55) * (i + 1) / stacks - PI * 0.05
            for (j in 0 until slices) {
                val th0 = 2.0 * PI * j / slices
                val th1 = 2.0 * PI * (j + 1) / slices
                val p00 = spherePoint(phi0, th0)
                val p10 = spherePoint(phi1, th0)
                val p01 = spherePoint(phi0, th1)
                val p11 = spherePoint(phi1, th1)
                out.addSphereTri(p00, p10, p01, 1f)
                out.addSphereTri(p10, p11, p01, 1f)
            }
        }
        return out.toMesh()
    }

    /**
     * Smooth-normal sculpted human cranium + cheekbones + orbital sockets + contoured jaw/chin.
     */
    private fun buildSculptedHead(): GlMesh {
        val stacks = 18
        val slices = 28
        val grid = SmoothGridBuilder(stacks + 1, slices + 1)
        for (i in 0..stacks) {
            val phi = PI * i / stacks - PI * 0.5
            val cosP = cos(phi).toFloat()
            val sinP = sin(phi).toFloat()
            val v = i.toFloat() / stacks
            for (j in 0..slices) {
                val th = 2.0 * PI * (j % slices) / slices
                val cosT = cos(th).toFloat()
                val sinT = sin(th).toFloat()
                val u = j.toFloat() / slices

                var x = cosP * cosT * 0.46f
                val y = sinP * 0.54f
                var z = cosP * sinT * 0.42f

                // Subtle cranial occiput back extension
                if (x < -0.05f && y > -0.1f) {
                    x -= 0.035f * ((y + 0.1f).coerceIn(0f, 0.5f))
                }
                // Tapered jawline + defined chin
                if (y < 0.04f) {
                    val jawT = ((y + 0.54f) / 0.58f).coerceIn(0f, 1f)
                    val smoothJaw = jawT * jawT * (3f - 2f * jawT)
                    z *= (0.64f + smoothJaw * 0.36f)
                    x *= if (x > 0f) (0.82f + smoothJaw * 0.18f) else (0.72f + smoothJaw * 0.28f)
                    if (x > 0.14f && abs(sinT) < 0.35f && y in -0.46f..-0.24f) {
                        x += 0.026f
                    }
                }
                // Cheekbone & orbital eye socket contouring
                if (x > 0.12f) {
                    val orbDistY = abs(y - 0.07f) / 0.11f
                    val orbDistZ = (abs(abs(sinT) - 0.36f)) / 0.20f
                    val orbMask = (1f - orbDistY * orbDistY).coerceAtLeast(0f) * (1f - orbDistZ * orbDistZ).coerceAtLeast(0f)
                    x -= orbMask * 0.040f

                    val cheekMask = (1f - abs(y + 0.08f) / 0.13f).coerceAtLeast(0f) * (1f - abs(abs(sinT) - 0.58f) / 0.28f).coerceAtLeast(0f)
                    x += cheekMask * 0.022f

                    val browMask = (1f - abs(y - 0.20f) / 0.08f).coerceAtLeast(0f) * (1f - abs(sinT) / 0.62f).coerceAtLeast(0f)
                    x += browMask * 0.028f
                }
                grid.setVertex(i, j, x, y, z, u, v)
            }
        }
        return grid.buildMesh(wrapU = true)
    }

    private fun buildSculptedNose(): GlMesh {
        val out = FloatArrayList(24 * 8)
        val top = Triple(-0.08f, 0.48f, 0f)
        val bridge = Triple(0.36f, 0.14f, 0f)
        val tip = Triple(0.56f, -0.30f, 0f)
        val sub = Triple(0.18f, -0.48f, 0f)
        val lBase = Triple(-0.20f, -0.42f, 0.44f)
        val rBase = Triple(-0.20f, -0.42f, -0.44f)
        out.addFacetTri(top, bridge, lBase)
        out.addFacetTri(top, rBase, bridge)
        out.addFacetTri(bridge, tip, lBase)
        out.addFacetTri(bridge, rBase, tip)
        out.addFacetTri(lBase, tip, sub)
        out.addFacetTri(sub, tip, rBase)
        return out.toMesh()
    }

    private fun buildSculptedLips(): GlMesh {
        val slices = 10
        val stacks = 5
        val grid = SmoothGridBuilder(stacks + 1, slices + 1)
        for (i in 0..stacks) {
            val v = i.toFloat() / stacks
            val phi = -0.75 + v * 1.5
            for (j in 0..slices) {
                val u = j.toFloat() / slices
                val th = -0.55 + u * 1.10
                val x = (cos(phi) * cos(th) * 0.36).toFloat()
                val cupidsBow = if (phi > 0.0) (1f - abs(sin(th * 2.5).toFloat()) * 0.15f) else 1f
                val y = (sin(phi) * 0.42 * cupidsBow).toFloat()
                val z = (sin(th) * 0.50).toFloat()
                grid.setVertex(i, j, x, y, z, u, v)
            }
        }
        return grid.buildMesh(wrapU = false)
    }

    private fun buildHairCrown(): GlMesh {
        val stacks = 10
        val slices = 22
        val grid = SmoothGridBuilder(stacks + 1, slices + 1)
        for (i in 0..stacks) {
            val v = i.toFloat() / stacks
            val phi = (PI * 0.58) * v - PI * 0.06
            for (j in 0..slices) {
                val u = j.toFloat() / slices
                val th = 2.0 * PI * (j % slices) / slices
                val lock = 1f + 0.12f * sin(th * 7.0 + phi * 3.5).toFloat().coerceAtLeast(0f) +
                    0.05f * cos(th * 13.0 - phi * 2.0).toFloat()
                val sweepBack = if (cos(th) < 0.12) 1.09f else 1.0f
                val x = (cos(phi) * cos(th) * 0.51 * lock * sweepBack).toFloat()
                val y = (sin(phi) * 0.50 * lock + 0.06).toFloat()
                val z = (cos(phi) * sin(th) * 0.48 * lock).toFloat()
                grid.setVertex(i, j, x, y, z, u, v)
            }
        }
        return grid.buildMesh(wrapU = true)
    }

    private fun buildHairBangs(): GlMesh {
        val stacks = 5
        val slices = 12
        val grid = SmoothGridBuilder(stacks + 1, slices + 1)
        for (i in 0..stacks) {
            val v = i.toFloat() / stacks
            for (j in 0..slices) {
                val u = j.toFloat() / slices
                val th = -PI * 0.36 + u * PI * 0.72
                val strand = abs(sin(th * 6.5)).toFloat()
                val flare = 0.46f + v * 0.06f + strand * 0.04f
                val droop = -v * (0.45f + strand * 0.25f)
                val x = (cos(th) * flare).toFloat()
                val y = 0.34f + droop
                val z = (sin(th) * flare * 0.92f).toFloat()
                grid.setVertex(i, j, x, y, z, u, v)
            }
        }
        return grid.buildMesh(wrapU = false)
    }

    private fun buildCurvedPlume(): GlMesh {
        val rings = 8
        val slices = 12
        val grid = SmoothGridBuilder(rings, slices + 1)
        for (r in 0 until rings) {
            val t = r.toFloat() / (rings - 1)
            val rad = (0.20f + sin(t * PI).toFloat() * 0.18f) * (1f - t * 0.68f).coerceAtLeast(0.14f)
            val y = 0.5f - t
            val xOff = 0.10f - t * t * 0.34f
            for (j in 0..slices) {
                val a = 2.0 * PI * (j % slices) / slices
                val wave = 1f + 0.12f * sin(a * 4.0 + t * 5.0).toFloat()
                val x = xOff + (cos(a) * rad * wave).toFloat()
                val z = (sin(a) * rad * wave).toFloat()
                grid.setVertex(r, j, x, y, z, j.toFloat() / slices, t)
            }
        }
        return grid.buildMesh(wrapU = true)
    }

    private fun buildCurvedArc(slices: Int, arcAngle: Double, thick: Float): GlMesh {
        val out = FloatArrayList(slices * 12 * 8)
        val aStart = -arcAngle * 0.5
        for (j in 0 until slices) {
            val t0 = aStart + arcAngle * j / slices
            val t1 = aStart + arcAngle * (j + 1) / slices
            val a = Triple((cos(t0) * 0.45).toFloat(), -thick, (sin(t0) * 0.45).toFloat())
            val b = Triple((cos(t0) * 0.45).toFloat(), thick, (sin(t0) * 0.45).toFloat())
            val c = Triple((cos(t1) * 0.45).toFloat(), -thick, (sin(t1) * 0.45).toFloat())
            val d = Triple((cos(t1) * 0.45).toFloat(), thick, (sin(t1) * 0.45).toFloat())
            out.addFacetQuad(a, b, d, c)
        }
        return out.toMesh()
    }

    private fun buildCollarWrap(): GlMesh {
        val slices = 14
        val out = FloatArrayList(slices * 12 * 8)
        val aStart = -PI * 0.72
        val aEnd = PI * 0.72
        for (j in 0 until slices) {
            val t0 = aStart + (aEnd - aStart) * j / slices
            val t1 = aStart + (aEnd - aStart) * (j + 1) / slices
            val ib0 = Triple(-(cos(t0) * 0.36).toFloat(), -0.35f, (sin(t0) * 0.36).toFloat())
            val ib1 = Triple(-(cos(t1) * 0.36).toFloat(), -0.35f, (sin(t1) * 0.36).toFloat())
            val ob0 = Triple(-(cos(t0) * 0.52).toFloat(), -0.45f, (sin(t0) * 0.52).toFloat())
            val ob1 = Triple(-(cos(t1) * 0.52).toFloat(), -0.45f, (sin(t1) * 0.52).toFloat())
            val it0 = Triple(-(cos(t0) * 0.44).toFloat(), 0.42f, (sin(t0) * 0.44).toFloat())
            val it1 = Triple(-(cos(t1) * 0.44).toFloat(), 0.42f, (sin(t1) * 0.44).toFloat())
            out.addFacetQuad(ob0, ob1, it1, it0)
            out.addFacetQuad(it0, it1, ib1, ib0)
        }
        return out.toMesh()
    }

    private fun buildCurvedVisor(): GlMesh {
        val slices = 12
        val stacks = 3
        val grid = SmoothGridBuilder(stacks + 1, slices + 1)
        val aStart = -PI * 0.34
        val aEnd = PI * 0.34
        for (i in 0..stacks) {
            val v = i.toFloat() / stacks
            val y = -0.15f + v * 0.30f
            val tilt = 0.50f - v * 0.03f
            for (j in 0..slices) {
                val u = j.toFloat() / slices
                val t = aStart + (aEnd - aStart) * u
                grid.setVertex(i, j, (cos(t) * tilt).toFloat(), y, (sin(t) * (tilt - 0.02f)).toFloat(), u, v)
            }
        }
        return grid.buildMesh(wrapU = false)
    }

    /**
     * Smooth-normal anatomical torso with realistic chest expansion, shoulder breadth, and waist taper.
     */
    private fun buildAnatomicalTorso(): GlMesh {
        val slices = 18
        val rings = 7
        val grid = SmoothGridBuilder(rings, slices + 1)
        val yLevels = floatArrayOf(-0.50f, -0.32f, -0.12f, 0.12f, 0.32f, 0.44f, 0.50f)
        val scaleX = floatArrayOf(0.36f, 0.39f, 0.42f, 0.48f, 0.52f, 0.44f, 0.28f)
        val scaleZ = floatArrayOf(0.35f, 0.36f, 0.40f, 0.49f, 0.55f, 0.48f, 0.30f)
        for (r in 0 until rings) {
            val v = r.toFloat() / (rings - 1)
            for (i in 0..slices) {
                val u = i.toFloat() / slices
                val a = 2.0 * PI * (i % slices) / slices
                val cosA = cos(a).toFloat()
                val sinA = sin(a).toFloat()
                // Subtle pectoral/back anatomical sculpt
                val pecBoost = if (cosA > 0f && r in 3..4) 1.07f else 1f
                val x = cosA * scaleX[r] * pecBoost
                val z = sinA * scaleZ[r]
                grid.setVertex(r, i, x, yLevels[r], z, u, v)
            }
        }
        return grid.buildMesh(wrapU = true)
    }

    /**
     * Sculpted human pelvis/hip saddle joining torso waist smoothly to hip joints with zero gaps.
     */
    private fun buildPelvisHip(): GlMesh {
        val slices = 18
        val rings = 6
        val grid = SmoothGridBuilder(rings, slices + 1)
        val yLevels = floatArrayOf(-0.50f, -0.30f, -0.08f, 0.16f, 0.36f, 0.50f)
        val scaleX = floatArrayOf(0.24f, 0.38f, 0.44f, 0.43f, 0.39f, 0.36f)
        val scaleZ = floatArrayOf(0.34f, 0.46f, 0.48f, 0.45f, 0.40f, 0.36f)
        for (r in 0 until rings) {
            val v = r.toFloat() / (rings - 1)
            for (i in 0..slices) {
                val u = i.toFloat() / slices
                val a = 2.0 * PI * (i % slices) / slices
                val cosA = cos(a).toFloat()
                val sinA = sin(a).toFloat()
                val gluteBack = if (cosA < -0.1f && r in 1..2) 1.09f else 1f
                grid.setVertex(r, i, cosA * scaleX[r] * gluteBack, yLevels[r], sinA * scaleZ[r], u, v)
            }
        }
        return grid.buildMesh(wrapU = true)
    }

    /**
     * Ergonomic contoured ballistic plate carrier shell curving cleanly around chest & back.
     */
    private fun buildContouredVest(): GlMesh {
        val slices = 18
        val rings = 6
        val grid = SmoothGridBuilder(rings, slices + 1)
        val yLevels = floatArrayOf(-0.50f, -0.34f, -0.06f, 0.22f, 0.40f, 0.50f)
        val scaleX = floatArrayOf(0.41f, 0.48f, 0.50f, 0.49f, 0.43f, 0.32f)
        val scaleZ = floatArrayOf(0.42f, 0.49f, 0.50f, 0.48f, 0.42f, 0.32f)
        for (r in 0 until rings) {
            val v = r.toFloat() / (rings - 1)
            for (i in 0..slices) {
                val u = i.toFloat() / slices
                val a = 2.0 * PI * (i % slices) / slices
                val cosA = cos(a).toFloat()
                val sinA = sin(a).toFloat()
                val plateBevel = if (abs(cosA) > 0.65f && r in 1..4) 1.05f else 0.96f
                grid.setVertex(r, i, cosA * scaleX[r] * plateBevel, yLevels[r], sinA * scaleZ[r], u, v)
            }
        }
        return grid.buildMesh(wrapU = true)
    }

    /**
     * Rounded anatomical tactical rucksack with domed top lid and tapered side gussets.
     */
    private fun buildTacticalBackpack(): GlMesh {
        val slices = 16
        val rings = 7
        val grid = SmoothGridBuilder(rings, slices + 1)
        val yLevels = floatArrayOf(-0.50f, -0.38f, -0.12f, 0.14f, 0.34f, 0.45f, 0.50f)
        val scaleX = floatArrayOf(0.28f, 0.46f, 0.50f, 0.48f, 0.42f, 0.32f, 0.14f)
        val scaleZ = floatArrayOf(0.32f, 0.47f, 0.50f, 0.48f, 0.44f, 0.35f, 0.16f)
        for (r in 0 until rings) {
            val v = r.toFloat() / (rings - 1)
            for (i in 0..slices) {
                val u = i.toFloat() / slices
                val a = 2.0 * PI * (i % slices) / slices
                val cosA = cos(a).toFloat()
                val sinA = sin(a).toFloat()
                // Flat ergonomic spine contact on +X (front against soldier back), rounded outer pack on -X
                val x = if (cosA > 0f) cosA * scaleX[r] * 0.68f else cosA * scaleX[r] * 1.12f
                grid.setVertex(r, i, x, yLevels[r], sinA * scaleZ[r], u, v)
            }
        }
        return grid.buildMesh(wrapU = true)
    }

    /**
     * Curved lower-face tactical respirator / bandana half-mask wrapping cleanly over nose bridge & jaw.
     */
    private fun buildContouredHalfMask(): GlMesh {
        val slices = 14
        val rings = 5
        val grid = SmoothGridBuilder(rings, slices + 1)
        val aStart = -PI * 0.56
        val aEnd = PI * 0.56
        for (r in 0 until rings) {
            val v = r.toFloat() / (rings - 1)
            val y = -0.50f + v
            val taper = if (r == 0) 0.78f else if (r == rings - 1) 0.92f else 1.0f
            for (j in 0..slices) {
                val u = j.toFloat() / slices
                val t = aStart + (aEnd - aStart) * u
                val noseBridge = if (r >= 3 && abs(t) < 0.35) 0.08f else 0f
                val chinDrop = if (r <= 1 && abs(t) < 0.45) -0.05f else 0f
                val x = (cos(t) * 0.50f * taper).toFloat() + noseBridge
                val z = (sin(t) * 0.48f * taper).toFloat()
                grid.setVertex(r, j, x, y + chinDrop, z, u, v)
            }
        }
        return grid.buildMesh(wrapU = false)
    }

    /**
     * Smooth contoured wrap band for tactical belt, thigh straps, sneaker cuffs, and face visor trim.
     */
    private fun buildCurvedBand(): GlMesh {
        val slices = 18
        val rings = 4
        val grid = SmoothGridBuilder(rings, slices + 1)
        val yLevels = floatArrayOf(-0.50f, -0.22f, 0.22f, 0.50f)
        val radScale = floatArrayOf(0.46f, 0.50f, 0.50f, 0.46f)
        for (r in 0 until rings) {
            val v = r.toFloat() / (rings - 1)
            for (j in 0..slices) {
                val u = j.toFloat() / slices
                val a = 2.0 * PI * (j % slices) / slices
                grid.setVertex(r, j, (cos(a) * radScale[r]).toFloat(), yLevels[r], (sin(a) * radScale[r]).toFloat(), u, v)
            }
        }
        return grid.buildMesh(wrapU = true)
    }

    /**
     * Royal multi-peak circlet diadem for sovereign character heads.
     */
    private fun buildCrownDiadem(): GlMesh {
        val slices = 20
        val rings = 3
        val grid = SmoothGridBuilder(rings, slices + 1)
        for (r in 0 until rings) {
            val v = r.toFloat() / (rings - 1)
            for (j in 0..slices) {
                val u = j.toFloat() / slices
                val a = 2.0 * PI * (j % slices) / slices
                val peak = if (r == 2) 0.28f * abs(cos(a * 2.5)).toFloat() else 0f
                val flare = 0.46f + v * 0.06f
                grid.setVertex(r, j, (cos(a) * flare).toFloat(), -0.50f + v * 0.72f + peak, (sin(a) * flare).toFloat(), u, v)
            }
        }
        return grid.buildMesh(wrapU = true)
    }

    /**
     * Smooth arched tactical cap brim curving cleanly forward from helmet/cap brow.
     */
    private fun buildCurvedCapBrim(): GlMesh {
        val slices = 12
        val rings = 4
        val grid = SmoothGridBuilder(rings, slices + 1)
        for (r in 0 until rings) {
            val v = r.toFloat() / (rings - 1)
            for (j in 0..slices) {
                val u = j.toFloat() / slices
                val a = -PI * 0.36 + u * PI * 0.72
                val rad = 0.24f + v * 0.28f
                val droop = -v * 0.22f - (1f - cos(a).toFloat()) * 0.20f
                grid.setVertex(r, j, (cos(a) * rad).toFloat(), droop, (sin(a) * rad * 0.94f).toFloat(), u, v)
            }
        }
        return grid.buildMesh(wrapU = false)
    }

    /**
     * True rounded-hemisphere smooth capsule with natural muscle contour along the shaft.
     * Eliminates flat-capped sharp cylinder joints at elbows, shoulders, knees, and wrists.
     */
    private fun buildSmoothCapsule(slices: Int, stacks: Int, topScale: Float, botScale: Float): GlMesh {
        val totalRings = stacks * 2 + 4
        val grid = SmoothGridBuilder(totalRings, slices + 1)
        var row = 0
        // Bottom hemisphere cap
        for (k in 0..stacks) {
            val t = k.toFloat() / stacks
            val ang = -PI * 0.5 + t * (PI * 0.5)
            val y = -0.38f + sin(ang).toFloat() * 0.12f
            val r = cos(ang).toFloat() * 0.5f * botScale
            val v = row.toFloat() / (totalRings - 1)
            writeCapsuleRing(grid, row++, slices, y, r, v)
        }
        // Muscular mid-shaft rings
        val midY = floatArrayOf(-0.14f, 0.14f)
        val midR = floatArrayOf(
            0.5f * (botScale * 0.62f + topScale * 0.38f) * 1.04f,
            0.5f * (botScale * 0.30f + topScale * 0.70f) * 1.06f
        )
        for (m in 0..1) {
            val v = row.toFloat() / (totalRings - 1)
            writeCapsuleRing(grid, row++, slices, midY[m], midR[m], v)
        }
        // Top hemisphere cap
        for (k in 0..stacks) {
            val t = k.toFloat() / stacks
            val ang = t * (PI * 0.5)
            val y = 0.38f + sin(ang).toFloat() * 0.12f
            val r = cos(ang).toFloat() * 0.5f * topScale
            val v = row.toFloat() / (totalRings - 1)
            writeCapsuleRing(grid, row++, slices, y, r, v)
        }
        return grid.buildMesh(wrapU = true)
    }

    private fun writeCapsuleRing(grid: SmoothGridBuilder, row: Int, slices: Int, y: Float, r: Float, v: Float) {
        for (j in 0..slices) {
            val u = j.toFloat() / slices
            val a = 2.0 * PI * (j % slices) / slices
            grid.setVertex(row, j, (cos(a) * r).toFloat(), y, (sin(a) * r).toFloat(), u, v)
        }
    }

    /**
     * Sculpted athletic tactical sneaker/boot with curved toe box, mid arch, ankle collar, and heel.
     */
    private fun buildSculptedBoot(): GlMesh {
        val rings = 6
        val slices = 12
        val grid = SmoothGridBuilder(rings, slices + 1)
        val ySteps = floatArrayOf(-0.50f, -0.34f, -0.06f, 0.22f, 0.40f, 0.50f)
        val xRadii = floatArrayOf(0.52f, 0.56f, 0.50f, 0.36f, 0.30f, 0.26f)
        val xShifts = floatArrayOf(0.14f, 0.16f, 0.10f, -0.04f, -0.08f, -0.09f)
        val zRadii = floatArrayOf(0.42f, 0.45f, 0.42f, 0.34f, 0.30f, 0.26f)
        for (r in 0 until rings) {
            val v = r.toFloat() / (rings - 1)
            for (j in 0..slices) {
                val u = j.toFloat() / slices
                val a = 2.0 * PI * (j % slices) / slices
                val x = xShifts[r] + (cos(a) * xRadii[r]).toFloat()
                val z = (sin(a) * zRadii[r]).toFloat()
                grid.setVertex(r, j, x, ySteps[r], z, u, v)
            }
        }
        return grid.buildMesh(wrapU = true)
    }

    /**
     * Rounded lugged sneaker/boot outsole matching buildSculptedBoot footprint with zero boxy edges.
     */
    private fun buildSculptedSole(): GlMesh {
        val rings = 3
        val slices = 14
        val grid = SmoothGridBuilder(rings, slices + 1)
        val ySteps = floatArrayOf(-0.50f, 0.0f, 0.50f)
        val radScale = floatArrayOf(0.92f, 1.0f, 0.94f)
        for (r in 0 until rings) {
            val v = r.toFloat() / (rings - 1)
            for (j in 0..slices) {
                val u = j.toFloat() / slices
                val a = 2.0 * PI * (j % slices) / slices
                val x = 0.14f + (cos(a) * 0.55f * radScale[r]).toFloat()
                val z = (sin(a) * 0.44f * radScale[r]).toFloat()
                grid.setVertex(r, j, x, ySteps[r], z, u, v)
            }
        }
        return grid.buildMesh(wrapU = true)
    }

    /**
     * Sculpted human palm/metacarpal hand geometry with smooth wrist transition.
     */
    private fun buildSculptedHandPalm(): GlMesh {
        val rings = 5
        val slices = 12
        val grid = SmoothGridBuilder(rings, slices + 1)
        val ySteps = floatArrayOf(-0.48f, -0.22f, 0.08f, 0.34f, 0.48f)
        val xScale = floatArrayOf(0.42f, 0.48f, 0.46f, 0.38f, 0.26f)
        val zScale = floatArrayOf(0.32f, 0.34f, 0.30f, 0.26f, 0.20f)
        for (r in 0 until rings) {
            val v = r.toFloat() / (rings - 1)
            for (j in 0..slices) {
                val u = j.toFloat() / slices
                val a = 2.0 * PI * (j % slices) / slices
                grid.setVertex(r, j, (cos(a) * xScale[r]).toFloat(), ySteps[r], (sin(a) * zScale[r]).toFloat(), u, v)
            }
        }
        return grid.buildMesh(wrapU = true)
    }

    /**
     * Four sculpted slightly curled anatomical fingers (index, middle, ring, pinky) in a single crisp draw call.
     */
    private fun buildSculptedFingers(): GlMesh {
        val out = FloatArrayList(4 * 36 * 8)
        val fingerXOffsets = floatArrayOf(-0.33f, -0.11f, 0.11f, 0.33f)
        val fingerLengths = floatArrayOf(0.84f, 0.96f, 0.88f, 0.72f)
        for (f in 0 until 4) {
            val fx = fingerXOffsets[f]
            val len = fingerLengths[f]
            val rBase = 0.088f
            val rMid = 0.078f
            val rTip = 0.058f
            val segs = 3
            val slices = 6
            for (s in 0 until segs) {
                val t0 = s.toFloat() / segs
                val t1 = (s + 1).toFloat() / segs
                val y0 = 0.5f - t0 * len
                val y1 = 0.5f - t1 * len
                val z0 = t0 * t0 * 0.24f
                val z1 = t1 * t1 * 0.24f
                val r0 = rBase * (1f - t0) + (if (s == 0) rMid else rTip) * t0
                val r1 = rMid * (1f - t1) + rTip * t1
                for (k in 0 until slices) {
                    val a0 = 2.0 * PI * k / slices
                    val a1 = 2.0 * PI * (k + 1) / slices
                    val p00 = Triple(fx + (cos(a0) * r0).toFloat(), y0, z0 + (sin(a0) * r0).toFloat())
                    val p01 = Triple(fx + (cos(a1) * r0).toFloat(), y0, z0 + (sin(a1) * r0).toFloat())
                    val p10 = Triple(fx + (cos(a0) * r1).toFloat(), y1, z1 + (sin(a0) * r1).toFloat())
                    val p11 = Triple(fx + (cos(a1) * r1).toFloat(), y1, z1 + (sin(a1) * r1).toFloat())
                    out.addFacetQuad(p00, p01, p11, p10)
                }
            }
        }
        return out.toMesh()
    }

    private fun buildSmoothCylinder(slices: Int, axis: Int, radiusTop: Float, radiusBot: Float): GlMesh {
        val out = FloatArrayList(slices * 12 * 8)
        for (i in 0 until slices) {
            val a0 = 2.0 * PI * i / slices
            val a1 = 2.0 * PI * (i + 1) / slices
            val c0 = cos(a0).toFloat(); val s0 = sin(a0).toFloat()
            val c1 = cos(a1).toFloat(); val s1 = sin(a1).toFloat()

            fun pt(r: Float, c: Float, s: Float, h: Float): Triple<Float, Float, Float> = when (axis) {
                0 -> Triple(h, r * c, r * s)
                2 -> Triple(r * c, r * s, h)
                else -> Triple(r * c, h, r * s)
            }
            fun norm(c: Float, s: Float): Triple<Float, Float, Float> = when (axis) {
                0 -> Triple(0f, c, s)
                2 -> Triple(c, s, 0f)
                else -> Triple(c, 0f, s)
            }

            val b0 = pt(radiusBot * 0.5f, c0, s0, -0.5f)
            val b1 = pt(radiusBot * 0.5f, c1, s1, -0.5f)
            val t0 = pt(radiusTop * 0.5f, c0, s0, 0.5f)
            val t1 = pt(radiusTop * 0.5f, c1, s1, 0.5f)
            val n0 = norm(c0, s0)
            val n1 = norm(c1, s1)
            val u0 = i.toFloat() / slices
            val u1 = (i + 1).toFloat() / slices

            out.addVertex(b0.first, b0.second, b0.third, n0.first, n0.second, n0.third, u0, 0f)
            out.addVertex(t0.first, t0.second, t0.third, n0.first, n0.second, n0.third, u0, 1f)
            out.addVertex(b1.first, b1.second, b1.third, n1.first, n1.second, n1.third, u1, 0f)

            out.addVertex(t0.first, t0.second, t0.third, n0.first, n0.second, n0.third, u0, 1f)
            out.addVertex(t1.first, t1.second, t1.third, n1.first, n1.second, n1.third, u1, 1f)
            out.addVertex(b1.first, b1.second, b1.third, n1.first, n1.second, n1.third, u1, 0f)

            val centerTop = pt(0f, 0f, 0f, 0.5f)
            val centerBot = pt(0f, 0f, 0f, -0.5f)
            out.addFacetTri(centerTop, t1, t0)
            out.addFacetTri(centerBot, b0, b1)
        }
        return out.toMesh()
    }

    private fun buildSmoothCone(slices: Int, droopAmount: Float): GlMesh {
        val rings = 5
        val grid = SmoothGridBuilder(rings, slices + 1)
        for (r in 0 until rings) {
            val t = r.toFloat() / (rings - 1)
            val y = -0.5f + t
            val radScale = (1f - t).coerceAtLeast(0.02f)
            for (i in 0..slices) {
                val u = i.toFloat() / slices
                val a = 2.0 * PI * (i % slices) / slices
                val w = 0.5f * radScale * (1f + droopAmount * sin(a * 4.0 + t * 2.0).toFloat())
                val droopY = y - (1f - t) * droopAmount * 0.25f * abs(sin(a * 4.0)).toFloat()
                grid.setVertex(r, i, (cos(a) * w).toFloat(), droopY, (sin(a) * w).toFloat(), u, t)
            }
        }
        return grid.buildMesh(wrapU = true)
    }

    private fun buildOrganicCanopy(stacks: Int, slices: Int): GlMesh {
        val grid = SmoothGridBuilder(stacks + 1, slices + 1)
        for (i in 0..stacks) {
            val v = i.toFloat() / stacks
            val phi = PI * v - PI * 0.5
            for (j in 0..slices) {
                val u = j.toFloat() / slices
                val th = 2.0 * PI * (j % slices) / slices
                val lobe = 0.44f + 0.09f * sin(phi * 3.5 + th * 3.0).toFloat() + 0.05f * cos(th * 5.0).toFloat()
                val x = (cos(phi) * cos(th) * lobe).toFloat()
                val y = (sin(phi) * lobe * 0.78f).toFloat()
                val z = (cos(phi) * sin(th) * lobe).toFloat()
                grid.setVertex(i, j, x, y, z, u, v)
            }
        }
        return grid.buildMesh(wrapU = true)
    }

    private fun buildCone(slices: Int, droopAmount: Float): GlMesh {
        val out = FloatArrayList(slices * 6 * 8)
        for (i in 0 until slices) {
            val a0 = 2.0 * PI * i / slices
            val a1 = 2.0 * PI * (i + 1) / slices
            val w0 = 0.5f * (1f + droopAmount * sin(a0 * 3.0).toFloat())
            val w1 = 0.5f * (1f + droopAmount * sin(a1 * 3.0).toFloat())
            val b0 = Triple((cos(a0) * w0).toFloat(), -0.5f, (sin(a0) * w0).toFloat())
            val b1 = Triple((cos(a1) * w1).toFloat(), -0.5f, (sin(a1) * w1).toFloat())
            val tip = Triple(0f, 0.5f, 0f)
            val botC = Triple(0f, -0.5f, 0f)
            out.addFacetTri(b0, tip, b1)
            out.addFacetTri(botC, b1, b0)
        }
        return out.toMesh()
    }

    private fun buildDisplacedBoulder(stacks: Int, slices: Int): GlMesh {
        val grid = SmoothGridBuilder(stacks + 1, slices + 1)
        for (i in 0..stacks) {
            val v = i.toFloat() / stacks
            val phi = PI * v - PI * 0.5
            for (j in 0..slices) {
                val u = j.toFloat() / slices
                val th = 2.0 * PI * (j % slices) / slices
                val disp = 0.42f + 0.12f * sin(phi * 3.2 + th * 2.1).toFloat() * cos(th * 3.7).toFloat()
                grid.setVertex(
                    i, j,
                    (cos(phi) * cos(th) * disp).toFloat(),
                    (sin(phi) * disp * 0.80f).toFloat(),
                    (cos(phi) * sin(th) * disp).toFloat(),
                    u, v
                )
            }
        }
        return grid.buildMesh(wrapU = true)
    }

    private fun buildGrassCrossTuft(): GlMesh {
        val out = FloatArrayList(24 * 8)
        val angles = floatArrayOf(0f, 60f, 120f)
        for (deg in angles) {
            val rad = Math.toRadians(deg.toDouble())
            val cx = (cos(rad) * 0.45).toFloat()
            val cz = (sin(rad) * 0.45).toFloat()
            val nx = -sin(rad).toFloat()
            val nz = cos(rad).toFloat()
            out.addVertex(-cx, 0f, -cz, nx, 0.35f, nz, 0f, 0f)
            out.addVertex(cx, 0f, cz, nx, 0.35f, nz, 1f, 0f)
            out.addVertex(cx * 0.25f, 1f, cz * 0.25f, nx, 0.35f, nz, 0.7f, 1f)
            out.addVertex(-cx, 0f, -cz, nx, 0.35f, nz, 0f, 0f)
            out.addVertex(cx * 0.25f, 1f, cz * 0.25f, nx, 0.35f, nz, 0.7f, 1f)
            out.addVertex(-cx * 0.25f, 1f, -cz * 0.25f, nx, 0.35f, nz, 0.3f, 1f)
        }
        return out.toMesh()
    }

    private fun buildChamferedPrism(bevel: Float, topScaleX: Float, topScaleZ: Float): GlMesh {
        val out = FloatArrayList(96 * 8)
        val b = bevel.coerceIn(0.04f, 0.30f)
        val yLevels = floatArrayOf(-0.5f, -0.5f + b, 0.5f - b, 0.5f)
        val rFactors = floatArrayOf(1f - b * 0.85f, 1f, 1f, 1f - b * 0.85f)
        val ringPts = Array(4) { level ->
            val y = yLevels[level]
            val tY = (y + 0.5f)
            val sX = (1f + (topScaleX - 1f) * tY) * rFactors[level] * 0.5f
            val sZ = (1f + (topScaleZ - 1f) * tY) * rFactors[level] * 0.5f
            val chX = sX * (1f - b * 0.9f)
            val chZ = sZ * (1f - b * 0.9f)
            listOf(
                Triple(chX, y, sZ),
                Triple(sX, y, chZ),
                Triple(sX, y, -chZ),
                Triple(chX, y, -sZ),
                Triple(-chX, y, -sZ),
                Triple(-sX, y, -chZ),
                Triple(-sX, y, chZ),
                Triple(-chX, y, sZ)
            )
        }
        for (l in 0 until 3) {
            for (k in 0 until 8) {
                val nk = (k + 1) % 8
                out.addFacetQuad(ringPts[l][k], ringPts[l][nk], ringPts[l + 1][nk], ringPts[l + 1][k])
            }
        }
        val topCenter = Triple(0f, 0.5f, 0f)
        val botCenter = Triple(0f, -0.5f, 0f)
        for (k in 0 until 8) {
            val nk = (k + 1) % 8
            out.addFacetTri(topCenter, ringPts[3][k], ringPts[3][nk])
            out.addFacetTri(botCenter, ringPts[0][nk], ringPts[0][k])
        }
        return out.toMesh()
    }

    private fun buildCorrugatedContainer(ribs: Int): GlMesh {
        val out = FloatArrayList((ribs * 24 + 48) * 8)
        val slices = ribs * 2
        val halfX = 0.5f
        val halfY = 0.5f
        val halfZ = 0.5f
        for (i in 0 until slices) {
            val x0 = -halfX + i.toFloat() / slices
            val x1 = -halfX + (i + 1).toFloat() / slices
            val r0 = if (i % 2 == 0) 0f else 0.032f
            val r1 = if ((i + 1) % 2 == 0) 0f else 0.032f
            out.addFacetQuad(
                Triple(x0, -halfY, halfZ + r0), Triple(x1, -halfY, halfZ + r1),
                Triple(x1, halfY, halfZ + r1), Triple(x0, halfY, halfZ + r0)
            )
            out.addFacetQuad(
                Triple(x1, -halfY, -halfZ - r1), Triple(x0, -halfY, -halfZ - r0),
                Triple(x0, halfY, -halfZ - r0), Triple(x1, halfY, -halfZ - r1)
            )
            out.addFacetQuad(
                Triple(x0, halfY + r0, halfZ), Triple(x1, halfY + r1, halfZ),
                Triple(x1, halfY + r1, -halfZ), Triple(x0, halfY + r0, -halfZ)
            )
        }
        val e00 = Triple(-halfX, -halfY, halfZ); val e10 = Triple(halfX, -halfY, halfZ)
        val e11 = Triple(halfX, halfY, halfZ); val e01 = Triple(-halfX, halfY, halfZ)
        val f00 = Triple(-halfX, -halfY, -halfZ); val f10 = Triple(halfX, -halfY, -halfZ)
        val f11 = Triple(halfX, halfY, -halfZ); val f01 = Triple(-halfX, halfY, -halfZ)
        out.addFacetQuad(e10, f10, f11, e11)
        out.addFacetQuad(f00, e00, e01, f01)
        out.addFacetQuad(f00, f10, e10, e00)
        return out.toMesh()
    }

    private fun buildCurvedMagazine(): GlMesh {
        val segs = 4
        val out = FloatArrayList(segs * 24 * 8)
        for (i in 0 until segs) {
            val t0 = i.toFloat() / segs
            val t1 = (i + 1).toFloat() / segs
            val y0 = 0.5f - t0
            val y1 = 0.5f - t1
            val x0 = t0 * t0 * 0.28f - 0.14f
            val x1 = t1 * t1 * 0.28f - 0.14f
            val w = 0.42f; val d = 0.42f
            val a0 = Triple(x0 - w, y0, d); val a1 = Triple(x0 + w, y0, d)
            val a2 = Triple(x0 + w, y0, -d); val a3 = Triple(x0 - w, y0, -d)
            val b0 = Triple(x1 - w, y1, d); val b1 = Triple(x1 + w, y1, d)
            val b2 = Triple(x1 + w, y1, -d); val b3 = Triple(x1 - w, y1, -d)
            out.addFacetQuad(b0, b1, a1, a0)
            out.addFacetQuad(b1, b2, a2, a1)
            out.addFacetQuad(b2, b3, a3, a2)
            out.addFacetQuad(b3, b0, a0, a3)
        }
        return out.toMesh()
    }

    private fun buildTaperedStock(): GlMesh {
        val out = FloatArrayList(48 * 8)
        val wF = 0.28f; val wB = 0.46f
        val zF = 0.42f; val zB = 0.46f
        val f0 = Triple(0.5f, wF, zF); val f1 = Triple(0.5f, wF, -zF)
        val f2 = Triple(0.5f, -wF * 0.4f, -zF); val f3 = Triple(0.5f, -wF * 0.4f, zF)
        val b0 = Triple(-0.5f, wB * 0.72f, zB); val b1 = Triple(-0.5f, wB * 0.72f, -zB)
        val b2 = Triple(-0.5f, -wB, -zB); val b3 = Triple(-0.5f, -wB, zB)
        out.addFacetQuad(b3, f3, f0, b0)
        out.addFacetQuad(f2, b2, b1, f1)
        out.addFacetQuad(b0, f0, f1, b1)
        out.addFacetQuad(b2, f2, f3, b3)
        out.addFacetQuad(b3, b0, b1, b2)
        out.addFacetQuad(f0, f3, f2, f1)
        return out.toMesh()
    }

    private fun buildGableRoof(): GlMesh {
        val out = FloatArrayList(24 * 8)
        val p0 = Triple(-0.5f, -0.5f, 0.5f); val p1 = Triple(0.5f, -0.5f, 0.5f)
        val p2 = Triple(0.5f, -0.5f, -0.5f); val p3 = Triple(-0.5f, -0.5f, -0.5f)
        val rF = Triple(0f, 0.5f, 0.5f); val rB = Triple(0f, 0.5f, -0.5f)
        out.addFacetQuad(p1, p2, rB, rF)
        out.addFacetQuad(p3, p0, rF, rB)
        out.addFacetTri(p0, p1, rF)
        out.addFacetTri(p2, p3, rB)
        out.addFacetQuad(p3, p2, p1, p0)
        return out.toMesh()
    }

    private fun buildGableRoofWithEaves(): GlMesh {
        val out = FloatArrayList(36 * 8)
        val ov = 0.56f
        val p0 = Triple(-ov, -0.5f, ov); val p1 = Triple(ov, -0.5f, ov)
        val p2 = Triple(ov, -0.5f, -ov); val p3 = Triple(-ov, -0.5f, -ov)
        val rF = Triple(0f, 0.52f, ov); val rB = Triple(0f, 0.52f, -ov)
        out.addFacetQuad(p1, p2, rB, rF)
        out.addFacetQuad(p3, p0, rF, rB)
        out.addFacetTri(p0, p1, rF)
        out.addFacetTri(p2, p3, rB)
        out.addFacetQuad(p3, p2, p1, p0)
        return out.toMesh()
    }

    private fun buildHippedRoofWithEaves(): GlMesh {
        val out = FloatArrayList(30 * 8)
        val ov = 0.57f
        val p0 = Triple(-ov, -0.5f, ov); val p1 = Triple(ov, -0.5f, ov)
        val p2 = Triple(ov, -0.5f, -ov); val p3 = Triple(-ov, -0.5f, -ov)
        val rF = Triple(0f, 0.52f, 0.24f); val rB = Triple(0f, 0.52f, -0.24f)
        out.addFacetQuad(p1, p2, rB, rF)
        out.addFacetQuad(p3, p0, rF, rB)
        out.addFacetTri(p0, p1, rF)
        out.addFacetTri(p2, p3, rB)
        out.addFacetQuad(p3, p2, p1, p0)
        return out.toMesh()
    }

    private fun buildStaircaseFlight(steps: Int): GlMesh {
        val out = FloatArrayList(steps * 18 * 8)
        for (i in 0 until steps) {
            val t0 = i.toFloat() / steps
            val t1 = (i + 1).toFloat() / steps
            val yTop = -0.5f + t1
            val zFront = 0.5f - t0
            val zBack = 0.5f - t1
            // Step riser
            out.addFacetQuad(
                Triple(-0.5f, -0.5f + t0, zFront),
                Triple(0.5f, -0.5f + t0, zFront),
                Triple(0.5f, yTop, zFront),
                Triple(-0.5f, yTop, zFront)
            )
            // Step tread
            out.addFacetQuad(
                Triple(-0.5f, yTop, zFront),
                Triple(0.5f, yTop, zFront),
                Triple(0.5f, yTop, zBack),
                Triple(-0.5f, yTop, zBack)
            )
        }
        return out.toMesh()
    }

    private fun buildArchTruss(segments: Int): GlMesh {
        val out = FloatArrayList(segments * 24 * 8)
        for (i in 0 until segments) {
            val t0 = i.toFloat() / segments
            val t1 = (i + 1).toFloat() / segments
            val z0 = -0.5f + t0
            val z1 = -0.5f + t1
            val y0 = sin(t0 * PI).toFloat() * 0.9f - 0.5f
            val y1 = sin(t1 * PI).toFloat() * 0.9f - 0.5f
            val tThick = 0.14f
            val p00 = Triple(-0.5f, y0, z0); val p10 = Triple(0.5f, y0, z0)
            val p01 = Triple(-0.5f, y1, z1); val p11 = Triple(0.5f, y1, z1)
            val u00 = Triple(-0.5f, y0 + tThick, z0); val u10 = Triple(0.5f, y0 + tThick, z0)
            val u01 = Triple(-0.5f, y1 + tThick, z1); val u11 = Triple(0.5f, y1 + tThick, z1)
            out.addFacetQuad(u00, u10, u11, u01)
            out.addFacetQuad(p00, p01, u01, u00)
            out.addFacetQuad(p10, u10, u11, p11)
        }
        return out.toMesh()
    }

    private fun buildSandbagWall(bags: Int): GlMesh {
        val out = FloatArrayList(bags * 24 * 8)
        val step = 1f / bags
        for (row in 0..1) {
            val yBase = -0.5f + row * 0.48f
            val xOff = if (row == 1) step * 0.25f else 0f
            for (b in 0 until bags) {
                val x0 = -0.5f + b * step + xOff
                val x1 = (x0 + step * 0.94f).coerceAtMost(0.5f)
                val xm = (x0 + x1) * 0.5f
                val yt = yBase + 0.44f
                val zF = 0.42f - row * 0.06f
                val p00 = Triple(x0, yBase, zF); val p10 = Triple(x1, yBase, zF)
                val p01 = Triple(x0, yBase, -zF); val p11 = Triple(x1, yBase, -zF)
                val top = Triple(xm, yt + 0.08f, 0f)
                out.addFacetTri(p00, p10, top)
                out.addFacetTri(p10, p11, top)
                out.addFacetTri(p11, p01, top)
                out.addFacetTri(p01, p00, top)
            }
        }
        return out.toMesh()
    }

    private fun buildMilitaryTent(): GlMesh {
        val out = FloatArrayList(36 * 8)
        val wBase = 0.52f; val wMid = 0.44f; val dBase = 0.50f
        val b0 = Triple(-wBase, -0.5f, dBase); val b1 = Triple(wBase, -0.5f, dBase)
        val b2 = Triple(wBase, -0.5f, -dBase); val b3 = Triple(-wBase, -0.5f, -dBase)
        val m0 = Triple(-wMid, 0.12f, dBase); val m1 = Triple(wMid, 0.12f, dBase)
        val m2 = Triple(wMid, 0.12f, -dBase); val m3 = Triple(-wMid, 0.12f, -dBase)
        val rF = Triple(0f, 0.52f, dBase * 0.96f); val rB = Triple(0f, 0.52f, -dBase * 0.96f)
        out.addFacetQuad(b0, b1, m1, m0)
        out.addFacetQuad(b2, b3, m3, m2)
        out.addFacetQuad(b1, b2, m2, m1)
        out.addFacetQuad(b3, b0, m0, m3)
        out.addFacetQuad(m1, m2, rB, rF)
        out.addFacetQuad(m3, m0, rF, rB)
        out.addFacetTri(m0, m1, rF)
        out.addFacetTri(m2, m3, rB)
        return out.toMesh()
    }

    private fun buildSweptWing(): GlMesh {
        val out = FloatArrayList(36 * 8)
        val rL = Triple(-0.2f, 0.08f, 0f); val rT = Triple(-0.2f, -0.06f, 0f)
        val lTipL = Triple(0.18f, 0.12f, 0.5f); val lTipT = Triple(0.18f, -0.04f, 0.5f)
        val rTipL = Triple(0.18f, 0.12f, -0.5f); val rTipT = Triple(0.18f, -0.04f, -0.5f)
        val leadR = Triple(-0.46f, 0.02f, 0f)
        val leadL = Triple(-0.08f, 0.04f, 0.5f)
        val leadRight = Triple(-0.08f, 0.04f, -0.5f)
        out.addFacetQuad(leadR, leadL, lTipL, rL)
        out.addFacetQuad(leadRight, leadR, rL, rTipL)
        out.addFacetQuad(leadL, leadR, rT, lTipT)
        out.addFacetQuad(leadR, leadRight, rTipT, rT)
        return out.toMesh()
    }

    private fun buildTailFin(): GlMesh {
        val out = FloatArrayList(24 * 8)
        val fB = Triple(0.35f, -0.5f, 0f)
        val rB = Triple(-0.45f, -0.5f, 0f)
        val fT = Triple(0.05f, 0.5f, 0f)
        val rT = Triple(-0.50f, 0.5f, 0f)
        val off = 0.06f
        out.addFacetQuad(Triple(rB.first, rB.second, off), Triple(fB.first, fB.second, off), Triple(fT.first, fT.second, off), Triple(rT.first, rT.second, off))
        out.addFacetQuad(Triple(fB.first, fB.second, -off), Triple(rB.first, rB.second, -off), Triple(rT.first, rT.second, -off), Triple(fT.first, fT.second, -off))
        out.addFacetQuad(Triple(fB.first, fB.second, off), Triple(fB.first, fB.second, -off), Triple(fT.first, fT.second, -off), Triple(fT.first, fT.second, off))
        return out.toMesh()
    }

    private fun buildRamAirCanopy(cells: Int): GlMesh {
        val out = FloatArrayList(cells * 18 * 8)
        for (i in 0 until cells) {
            val t0 = i.toFloat() / cells
            val t1 = (i + 1).toFloat() / cells
            val x0 = -0.5f + t0
            val x1 = -0.5f + t1
            val arch0 = sin(t0 * PI).toFloat() * 0.42f
            val arch1 = sin(t1 * PI).toFloat() * 0.42f
            val puff0 = arch0 + (if (i % 2 == 0) 0.04f else 0.08f)
            val puff1 = arch1 + (if ((i + 1) % 2 == 0) 0.04f else 0.08f)
            val p0F = Triple(x0, puff0, 0.48f); val p1F = Triple(x1, puff1, 0.48f)
            val p0B = Triple(x0, puff0 * 0.88f, -0.48f); val p1B = Triple(x1, puff1 * 0.88f, -0.48f)
            val b0F = Triple(x0, arch0 - 0.18f, 0.48f); val b1F = Triple(x1, arch1 - 0.18f, 0.48f)
            out.addFacetQuad(p0B, p1B, p1F, p0F)
            out.addFacetQuad(b0F, b1F, p1F, p0F)
        }
        return out.toMesh()
    }

    private fun buildCurvedShield(segments: Int): GlMesh {
        val out = FloatArrayList(segments * 12 * 8)
        val aStart = -PI * 0.28
        val aEnd = PI * 0.28
        for (i in 0 until segments) {
            val a0 = aStart + (aEnd - aStart) * i / segments
            val a1 = aStart + (aEnd - aStart) * (i + 1) / segments
            val x0 = (cos(a0) * 0.35 - 0.28).toFloat(); val z0 = (sin(a0) * 0.58).toFloat()
            val x1 = (cos(a1) * 0.35 - 0.28).toFloat(); val z1 = (sin(a1) * 0.58).toFloat()
            val f0 = Triple(x0, -0.5f, z0); val f1 = Triple(x1, -0.5f, z1)
            val f2 = Triple(x1 * 0.92f, 0.5f, z1 * 0.94f); val f3 = Triple(x0 * 0.92f, 0.5f, z0 * 0.94f)
            out.addFacetQuad(f0, f1, f2, f3)
        }
        return out.toMesh()
    }

    private fun buildWavedCloth(): GlMesh {
        val segs = 6
        val out = FloatArrayList(segs * 12 * 8)
        for (i in 0 until segs) {
            val x0 = -0.5f + i.toFloat() / segs
            val x1 = -0.5f + (i + 1).toFloat() / segs
            val z0 = sin(i * 1.1f) * 0.14f
            val z1 = sin((i + 1) * 1.1f) * 0.14f
            val p0 = Triple(x0, -0.5f, z0); val p1 = Triple(x1, -0.5f, z1)
            val p2 = Triple(x1, 0.5f, z1); val p3 = Triple(x0, 0.5f, z0)
            out.addFacetQuad(p0, p1, p2, p3)
            out.addFacetQuad(p1, p0, p3, p2)
        }
        return out.toMesh()
    }

    private fun buildKatanaBlade(): GlMesh {
        val segs = 6
        val out = FloatArrayList(segs * 24 * 8)
        for (i in 0 until segs) {
            val t0 = i.toFloat() / segs
            val t1 = (i + 1).toFloat() / segs
            val x0 = -0.5f + t0
            val x1 = -0.5f + t1
            val c0 = t0 * t0 * 0.08f
            val c1 = t1 * t1 * 0.08f
            val h0 = 0.08f * (1f - t0 * 0.35f)
            val h1 = 0.08f * (1f - t1 * 0.35f)
            val a = Triple(x0, c0 + h0, 0.025f); val b = Triple(x1, c1 + h1, 0.025f)
            val c = Triple(x1, c1 - h1 * 0.5f, 0f); val d = Triple(x0, c0 - h0 * 0.5f, 0f)
            val e = Triple(x0, c0 + h0, -0.025f); val f = Triple(x1, c1 + h1, -0.025f)
            out.addFacetQuad(a, b, c, d)
            out.addFacetQuad(f, e, d, c)
            out.addFacetQuad(a, e, f, b)
        }
        return out.toMesh()
    }

    private fun buildCrescentScythe(): GlMesh {
        val segs = 7
        val out = FloatArrayList(segs * 24 * 8)
        for (i in 0 until segs) {
            val t0 = i.toFloat() / segs
            val t1 = (i + 1).toFloat() / segs
            val ang0 = -PI * 0.15 + t0 * PI * 0.72
            val ang1 = -PI * 0.15 + t1 * PI * 0.72
            val w0 = 0.18f * (1f - t0 * 0.72f)
            val w1 = 0.18f * (1f - t1 * 0.72f)
            val o0 = Triple((cos(ang0) * 0.52).toFloat(), (sin(ang0) * 0.45).toFloat(), 0.03f)
            val o1 = Triple((cos(ang1) * 0.52).toFloat(), (sin(ang1) * 0.45).toFloat(), 0.03f)
            val i0 = Triple((cos(ang0) * (0.52 - w0)).toFloat(), (sin(ang0) * (0.45 - w0)).toFloat(), 0f)
            val i1 = Triple((cos(ang1) * (0.52 - w1)).toFloat(), (sin(ang1) * (0.45 - w1)).toFloat(), 0f)
            val b0 = Triple(o0.first, o0.second, -0.03f); val b1 = Triple(o1.first, o1.second, -0.03f)
            out.addFacetQuad(o0, o1, i1, i0)
            out.addFacetQuad(b1, b0, i0, i1)
            out.addFacetQuad(o0, b0, b1, o1)
        }
        return out.toMesh()
    }

    private fun buildButterflyWing(): GlMesh {
        val out = FloatArrayList(24 * 8)
        val root = Triple(0f, 0f, 0f)
        val tipTop = Triple(-0.22f, 0.55f, 0.48f)
        val outerMid = Triple(-0.46f, 0.34f, 0.52f)
        val lowerTip = Triple(-0.32f, -0.18f, 0.34f)
        val innerLow = Triple(0.08f, -0.12f, 0.14f)
        out.addFacetTri(root, tipTop, outerMid)
        out.addFacetTri(root, outerMid, lowerTip)
        out.addFacetTri(root, lowerTip, innerLow)
        out.addFacetTri(tipTop, root, outerMid)
        out.addFacetTri(outerMid, root, lowerTip)
        return out.toMesh()
    }

    private fun spherePoint(phi: Double, th: Double): Triple<Float, Float, Float> = Triple(
        (cos(phi) * cos(th) * 0.5).toFloat(),
        (sin(phi) * 0.5).toFloat(),
        (cos(phi) * sin(th) * 0.5).toFloat()
    )

    private fun arrayToMesh(data: FloatArray): GlMesh {
        val fb = ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(data)
                position(0)
            }
        return GlMesh(fb, data.size / 8)
    }

    /**
     * Indexed grid builder that accumulates area-weighted vertex normals across shared triangles
     * so organic anatomy, hair, clothing, foliage, and terrain curves shade smoothly with zero facets.
     */
    private class SmoothGridBuilder(private val rows: Int, private val cols: Int) {
        private val count = rows * cols
        private val px = FloatArray(count)
        private val py = FloatArray(count)
        private val pz = FloatArray(count)
        private val pu = FloatArray(count)
        private val pv = FloatArray(count)
        private val nx = FloatArray(count)
        private val ny = FloatArray(count)
        private val nz = FloatArray(count)

        fun setVertex(r: Int, c: Int, x: Float, y: Float, z: Float, u: Float, v: Float) {
            val idx = r * cols + c
            px[idx] = x; py[idx] = y; pz[idx] = z
            pu[idx] = u; pv[idx] = v
        }

        private fun accumTri(i0: Int, i1: Int, i2: Int) {
            val ux = px[i1] - px[i0]; val uy = py[i1] - py[i0]; val uz = pz[i1] - pz[i0]
            val vx = px[i2] - px[i0]; val vy = py[i2] - py[i0]; val vz = pz[i2] - pz[i0]
            val fnx = uy * vz - uz * vy
            val fny = uz * vx - ux * vz
            val fnz = ux * vy - uy * vx
            nx[i0] += fnx; ny[i0] += fny; nz[i0] += fnz
            nx[i1] += fnx; ny[i1] += fny; nz[i1] += fnz
            nx[i2] += fnx; ny[i2] += fny; nz[i2] += fnz
        }

        fun buildMesh(wrapU: Boolean): GlMesh {
            for (r in 0 until rows - 1) {
                for (c in 0 until cols - 1) {
                    val i00 = r * cols + c
                    val i10 = (r + 1) * cols + c
                    val i01 = r * cols + (c + 1)
                    val i11 = (r + 1) * cols + (c + 1)
                    accumTri(i00, i10, i01)
                    accumTri(i10, i11, i01)
                }
            }
            if (wrapU) {
                for (r in 0 until rows) {
                    val iStart = r * cols
                    val iEnd = r * cols + (cols - 1)
                    val sx = nx[iStart] + nx[iEnd]
                    val sy = ny[iStart] + ny[iEnd]
                    val sz = nz[iStart] + nz[iEnd]
                    nx[iStart] = sx; ny[iStart] = sy; nz[iStart] = sz
                    nx[iEnd] = sx; ny[iEnd] = sy; nz[iEnd] = sz
                }
            }
            for (k in 0 until count) {
                val len = sqrt(nx[k] * nx[k] + ny[k] * ny[k] + nz[k] * nz[k]).coerceAtLeast(0.0001f)
                nx[k] /= len; ny[k] /= len; nz[k] /= len
            }

            val out = FloatArrayList((rows - 1) * (cols - 1) * 6 * 8)
            for (r in 0 until rows - 1) {
                for (c in 0 until cols - 1) {
                    val i00 = r * cols + c
                    val i10 = (r + 1) * cols + c
                    val i01 = r * cols + (c + 1)
                    val i11 = (r + 1) * cols + (c + 1)
                    emit(out, i00); emit(out, i10); emit(out, i01)
                    emit(out, i10); emit(out, i11); emit(out, i01)
                }
            }
            return out.toMesh()
        }

        private fun emit(out: FloatArrayList, i: Int) {
            out.addVertex(px[i], py[i], pz[i], nx[i], ny[i], nz[i], pu[i], pv[i])
        }
    }

    private class FloatArrayList(initialCap: Int) {
        private var arr = FloatArray(initialCap.coerceAtLeast(128))
        private var size = 0

        fun addVertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, u: Float, v: Float) {
            if (size + 8 > arr.size) {
                arr = arr.copyOf((arr.size * 2).coerceAtLeast(size + 64))
            }
            arr[size++] = x; arr[size++] = y; arr[size++] = z
            arr[size++] = nx; arr[size++] = ny; arr[size++] = nz
            arr[size++] = u; arr[size++] = v
        }

        fun addFacetTri(a: Triple<Float, Float, Float>, b: Triple<Float, Float, Float>, c: Triple<Float, Float, Float>) {
            val ux = b.first - a.first; val uy = b.second - a.second; val uz = b.third - a.third
            val vx = c.first - a.first; val vy = c.second - a.second; val vz = c.third - a.third
            var nx = uy * vz - uz * vy
            var ny = uz * vx - ux * vz
            var nz = ux * vy - uy * vx
            val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
            nx /= len; ny /= len; nz /= len
            addVertex(a.first, a.second, a.third, nx, ny, nz, 0f, 0f)
            addVertex(b.first, b.second, b.third, nx, ny, nz, 1f, 0f)
            addVertex(c.first, c.second, c.third, nx, ny, nz, 0.5f, 1f)
        }

        fun addSphereTri(a: Triple<Float, Float, Float>, b: Triple<Float, Float, Float>, c: Triple<Float, Float, Float>, nSign: Float) {
            fun norm(p: Triple<Float, Float, Float>): Triple<Float, Float, Float> {
                val len = sqrt(p.first * p.first + p.second * p.second + p.third * p.third).coerceAtLeast(0.0001f)
                return Triple((p.first / len) * nSign, (p.second / len) * nSign, (p.third / len) * nSign)
            }
            val na = norm(a); val nb = norm(b); val nc = norm(c)
            addVertex(a.first, a.second, a.third, na.first, na.second, na.third, a.first + 0.5f, a.second + 0.5f)
            addVertex(b.first, b.second, b.third, nb.first, nb.second, nb.third, b.first + 0.5f, b.second + 0.5f)
            addVertex(c.first, c.second, c.third, nc.first, nc.second, nc.third, c.first + 0.5f, c.second + 0.5f)
        }

        fun addFacetQuad(a: Triple<Float, Float, Float>, b: Triple<Float, Float, Float>, c: Triple<Float, Float, Float>, d: Triple<Float, Float, Float>) {
            addFacetTri(a, b, c)
            addFacetTri(a, c, d)
        }

        fun toMesh(): GlMesh {
            val trimmed = arr.copyOf(size)
            val fb = ByteBuffer.allocateDirect(trimmed.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(trimmed)
                    position(0)
                }
            return GlMesh(fb, trimmed.size / 8)
        }
    }

    private companion object {
        val sharedStaticCache = ConcurrentHashMap<String, GlMesh>()
        val sharedTerrainCache = ConcurrentHashMap<String, GlMesh>()

        inline fun cachedStatic(name: String, builder: () -> GlMesh): GlMesh {
            val tpl = sharedStaticCache[name] ?: builder().also { sharedStaticCache.putIfAbsent(name, it) }
            return GlMesh(tpl.buffer.duplicate().apply { position(0) }, tpl.vertexCount)
        }
    }
}
