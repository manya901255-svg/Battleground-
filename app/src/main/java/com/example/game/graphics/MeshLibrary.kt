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
 * Procedural 3D mesh library providing sculpted organic, anatomical, architectural,
 * military compound, firearm, aircraft, and heightfield geometry.
 * Zero raw cubic/voxel placeholders are exposed or used anywhere in the renderer.
 */
class MeshLibrary {
    val skyDome: GlMesh = cachedStatic("skyDome") { buildSphere(stacks = 12, slices = 24, inward = true) }
    val softShadowDisc: GlMesh = cachedStatic("softShadowDisc") { buildDisc(segments = 20) }
    val quadBillboard: GlMesh = cachedStatic("quadBillboard") { buildQuad() }

    // Articulated human anatomical & facial meshes
    val humanHead: GlMesh = cachedStatic("humanHead_v2") { buildSculptedHead() }
    val humanEyeOrb: GlMesh = cachedStatic("humanEyeOrb") { buildSphere(stacks = 6, slices = 10, inward = false) }
    val humanNose: GlMesh = cachedStatic("humanNose") { buildSculptedNose() }
    val humanEar: GlMesh = cachedStatic("humanEar") { buildChamferedPrism(bevel = 0.24f, topScaleX = 0.85f, topScaleZ = 0.78f) }
    val browRidge: GlMesh = cachedStatic("browRidge") { buildCurvedArc(slices = 6, arcAngle = PI * 0.45, thick = 0.22f) }
    val facialMouth: GlMesh = cachedStatic("facialMouth") { buildChamferedPrism(bevel = 0.28f, topScaleX = 0.85f, topScaleZ = 0.85f) }
    val humanHair: GlMesh = cachedStatic("humanHair_v2") { buildHairCrown() }
    val hairPonytail: GlMesh = cachedStatic("hairPonytail") { buildCurvedPlume() }
    val jacketCollar: GlMesh = cachedStatic("jacketCollar") { buildCollarWrap() }
    val helmetShell: GlMesh = cachedStatic("helmetShell") { buildHemisphereShell(stacks = 8, slices = 16) }
    val curvedVisor: GlMesh = cachedStatic("curvedVisor") { buildCurvedVisor() }
    val chestTorso: GlMesh = cachedStatic("chestTorso") { buildAnatomicalTorso() }
    val limbCapsule: GlMesh = cachedStatic("limbCapsule") { buildTaperedCapsule(slices = 12, topScale = 1.0f, botScale = 0.74f) }
    val taperedBoot: GlMesh = cachedStatic("taperedBoot") { buildTaperedCapsule(slices = 10, topScale = 0.82f, botScale = 1.18f) }
    val bevelPack: GlMesh = cachedStatic("bevelPack_v2") { buildChamferedPrism(bevel = 0.14f, topScaleX = 0.86f, topScaleZ = 0.84f) }
    val kneePadShell: GlMesh = cachedStatic("kneePadShell") { buildChamferedPrism(bevel = 0.24f, topScaleX = 0.84f, topScaleZ = 0.84f) }
    val humanHandGlove: GlMesh = cachedStatic("humanHandGlove") { buildChamferedPrism(bevel = 0.22f, topScaleX = 0.88f, topScaleZ = 0.86f) }

    // Vehicles
    val wheelCylinder: GlMesh = cachedStatic("wheelCylinder") { buildCylinder(slices = 18, axis = 2, radiusTop = 1f, radiusBot = 1f) }
    val chassisSlopeBody: GlMesh = cachedStatic("chassisSlopeBody_v2") { buildChamferedPrism(bevel = 0.10f, topScaleX = 0.88f, topScaleZ = 0.90f) }
    val cabSlopedRoof: GlMesh = cachedStatic("cabSlopedRoof_v2") { buildChamferedPrism(bevel = 0.10f, topScaleX = 0.72f, topScaleZ = 0.82f) }

    // Detailed multi-part Weapons
    val rifleBarrel: GlMesh = cachedStatic("rifleBarrel_v2") { buildCylinder(slices = 10, axis = 0, radiusTop = 0.86f, radiusBot = 1.0f) }
    val weaponReceiver: GlMesh = cachedStatic("weaponReceiver") { buildChamferedPrism(bevel = 0.18f, topScaleX = 0.94f, topScaleZ = 0.92f) }
    val ergoGrip: GlMesh = cachedStatic("ergoGrip") { buildChamferedPrism(bevel = 0.16f, topScaleX = 0.86f, topScaleZ = 0.88f) }
    val curvedMagazine: GlMesh = cachedStatic("curvedMagazine") { buildCurvedMagazine() }
    val tacticalStock: GlMesh = cachedStatic("tacticalStock") { buildTaperedStock() }
    val opticBody: GlMesh = cachedStatic("opticBody") { buildChamferedPrism(bevel = 0.20f, topScaleX = 0.88f, topScaleZ = 0.88f) }

    // Environment organic, architectural, military & aircraft meshes
    val treeTrunk: GlMesh = cachedStatic("treeTrunk") { buildCylinder(slices = 10, axis = 1, radiusTop = 0.58f, radiusBot = 1.28f) }
    val columnBeam: GlMesh = cachedStatic("columnBeam") { buildCylinder(slices = 8, axis = 1, radiusTop = 0.96f, radiusBot = 1.0f) }
    val foliageCone: GlMesh = cachedStatic("foliageCone") { buildCone(slices = 12, droopAmount = 0.16f) }
    val boulderRock: GlMesh = cachedStatic("boulderRock") { buildDisplacedBoulder(stacks = 6, slices = 10) }
    val bushCluster: GlMesh = cachedStatic("bushCluster") { buildDisplacedBoulder(stacks = 5, slices = 9) }
    val grassTuft: GlMesh = cachedStatic("grassTuft") { buildGrassCrossTuft() }
    val pitchedRoof: GlMesh = cachedStatic("pitchedRoof") { buildGableRoof() }
    val slopedAwningRoof: GlMesh = cachedStatic("slopedAwningRoof") { buildGableRoofWithEaves() }
    val chamferedStructure: GlMesh = cachedStatic("chamferedStructure") { buildChamferedPrism(bevel = 0.06f, topScaleX = 0.98f, topScaleZ = 0.98f) }
    val corrugatedContainer: GlMesh = cachedStatic("corrugatedContainer") { buildCorrugatedContainer(ribs = 6) }
    val bridgeArchTruss: GlMesh = cachedStatic("bridgeArchTruss") { buildArchTruss(segments = 8) }
    val sandbagRow: GlMesh = cachedStatic("sandbagRow") { buildSandbagWall(bags = 4) }
    val militaryTent: GlMesh = cachedStatic("militaryTent") { buildMilitaryTent() }
    val radomeSphere: GlMesh = cachedStatic("radomeSphere") { buildSphere(stacks = 8, slices = 14, inward = false) }
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

    private fun buildSculptedHead(): GlMesh {
        val stacks = 12
        val slices = 18
        val out = FloatArrayList(stacks * slices * 6 * 8)
        fun headPt(phi: Double, th: Double): Triple<Float, Float, Float> {
            val cosP = cos(phi).toFloat()
            val sinP = sin(phi).toFloat()
            val cosT = cos(th).toFloat()
            val sinT = sin(th).toFloat()
            var x = cosP * cosT * 0.47f
            val y = sinP * 0.53f
            var z = cosP * sinT * 0.43f
            // Taper lower jaw & project strong chin
            if (y < 0.02f) {
                val jawT = (y + 0.53f) / 0.55f
                val narrow = 0.72f + jawT * 0.28f
                z *= narrow
                x *= if (x > 0f) (0.86f + jawT * 0.14f) else (0.76f + jawT * 0.24f)
            }
            // Cheekbone prominence + subtle orbital eye socket recess
            if (x > 0.16f) {
                val inOrbital = y in -0.02f..0.16f && abs(sinT) in 0.18f..0.52f
                val brow = y in 0.16f..0.28f && abs(sinT) < 0.55f
                if (inOrbital) x -= 0.032f
                if (brow) x += 0.028f
            }
            return Triple(x, y, z)
        }
        for (i in 0 until stacks) {
            val phi0 = PI * i / stacks - PI * 0.5
            val phi1 = PI * (i + 1) / stacks - PI * 0.5
            for (j in 0 until slices) {
                val th0 = 2.0 * PI * j / slices
                val th1 = 2.0 * PI * (j + 1) / slices
                val a = headPt(phi0, th0)
                val b = headPt(phi1, th0)
                val c = headPt(phi0, th1)
                val d = headPt(phi1, th1)
                out.addFacetTri(a, b, c)
                out.addFacetTri(b, d, c)
            }
        }
        return out.toMesh()
    }

    private fun buildSculptedNose(): GlMesh {
        val out = FloatArrayList(18 * 8)
        val top = Triple(-0.1f, 0.5f, 0f)
        val bridge = Triple(0.32f, 0.12f, 0f)
        val tip = Triple(0.55f, -0.35f, 0f)
        val lBase = Triple(-0.22f, -0.45f, 0.46f)
        val rBase = Triple(-0.22f, -0.45f, -0.46f)
        out.addFacetTri(top, bridge, lBase)
        out.addFacetTri(top, rBase, bridge)
        out.addFacetTri(bridge, tip, lBase)
        out.addFacetTri(bridge, rBase, tip)
        out.addFacetTri(lBase, tip, rBase)
        return out.toMesh()
    }

    private fun buildHairCrown(): GlMesh {
        val stacks = 7
        val slices = 16
        val out = FloatArrayList(stacks * slices * 6 * 8)
        fun hairPt(phi: Double, th: Double): Triple<Float, Float, Float> {
            val lock = 1f + 0.13f * sin(th * 7.0 + phi * 3.5).toFloat().coerceAtLeast(0f)
            val sweepBack = if (cos(th) < 0.1) 1.07f else 1.0f
            val x = (cos(phi) * cos(th) * 0.50 * lock * sweepBack).toFloat()
            val y = (sin(phi) * 0.49 * lock + 0.08).toFloat()
            val z = (cos(phi) * sin(th) * 0.47 * lock).toFloat()
            return Triple(x, y, z)
        }
        for (i in 0 until stacks) {
            val phi0 = (PI * 0.56) * i / stacks - PI * 0.04
            val phi1 = (PI * 0.56) * (i + 1) / stacks - PI * 0.04
            for (j in 0 until slices) {
                val th0 = 2.0 * PI * j / slices
                val th1 = 2.0 * PI * (j + 1) / slices
                val a = hairPt(phi0, th0)
                val b = hairPt(phi1, th0)
                val c = hairPt(phi0, th1)
                val d = hairPt(phi1, th1)
                out.addFacetTri(a, b, c)
                out.addFacetTri(b, d, c)
            }
        }
        return out.toMesh()
    }

    private fun buildCurvedPlume(): GlMesh {
        val rings = 5
        val slices = 8
        val out = FloatArrayList(rings * slices * 6 * 8)
        val radii = floatArrayOf(0.24f, 0.34f, 0.30f, 0.20f, 0.06f)
        val ySteps = floatArrayOf(0.5f, 0.25f, 0.0f, -0.25f, -0.5f)
        val xCurve = floatArrayOf(0.08f, -0.02f, -0.10f, -0.16f, -0.22f)
        for (r in 0 until rings - 1) {
            for (j in 0 until slices) {
                val a0 = 2.0 * PI * j / slices
                val a1 = 2.0 * PI * (j + 1) / slices
                val p00 = Triple(xCurve[r] + (cos(a0) * radii[r]).toFloat(), ySteps[r], (sin(a0) * radii[r]).toFloat())
                val p10 = Triple(xCurve[r + 1] + (cos(a0) * radii[r + 1]).toFloat(), ySteps[r + 1], (sin(a0) * radii[r + 1]).toFloat())
                val p01 = Triple(xCurve[r] + (cos(a1) * radii[r]).toFloat(), ySteps[r], (sin(a1) * radii[r]).toFloat())
                val p11 = Triple(xCurve[r + 1] + (cos(a1) * radii[r + 1]).toFloat(), ySteps[r + 1], (sin(a1) * radii[r + 1]).toFloat())
                out.addFacetTri(p00, p10, p01)
                out.addFacetTri(p10, p11, p01)
            }
        }
        return out.toMesh()
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
        val slices = 12
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
        val slices = 8
        val out = FloatArrayList(slices * 6 * 8)
        val aStart = -PI * 0.34
        val aEnd = PI * 0.34
        for (j in 0 until slices) {
            val t0 = aStart + (aEnd - aStart) * j / slices
            val t1 = aStart + (aEnd - aStart) * (j + 1) / slices
            val a = Triple((cos(t0) * 0.50).toFloat(), -0.14f, (sin(t0) * 0.48).toFloat())
            val b = Triple((cos(t0) * 0.48).toFloat(), 0.14f, (sin(t0) * 0.46).toFloat())
            val c = Triple((cos(t1) * 0.50).toFloat(), -0.14f, (sin(t1) * 0.48).toFloat())
            val d = Triple((cos(t1) * 0.48).toFloat(), 0.14f, (sin(t1) * 0.46).toFloat())
            out.addFacetTri(a, b, c)
            out.addFacetTri(b, d, c)
        }
        return out.toMesh()
    }

    private fun buildAnatomicalTorso(): GlMesh {
        val slices = 12
        val rings = 4
        val out = FloatArrayList(slices * rings * 6 * 8)
        val yLevels = floatArrayOf(-0.5f, -0.15f, 0.25f, 0.5f)
        val scaleX = floatArrayOf(0.40f, 0.47f, 0.54f, 0.45f)
        val scaleZ = floatArrayOf(0.36f, 0.42f, 0.52f, 0.54f)
        for (r in 0 until rings - 1) {
            for (i in 0 until slices) {
                val a0 = 2.0 * PI * i / slices
                val a1 = 2.0 * PI * (i + 1) / slices
                val p00 = Triple((cos(a0) * scaleX[r]).toFloat(), yLevels[r], (sin(a0) * scaleZ[r]).toFloat())
                val p10 = Triple((cos(a0) * scaleX[r + 1]).toFloat(), yLevels[r + 1], (sin(a0) * scaleZ[r + 1]).toFloat())
                val p01 = Triple((cos(a1) * scaleX[r]).toFloat(), yLevels[r], (sin(a1) * scaleZ[r]).toFloat())
                val p11 = Triple((cos(a1) * scaleX[r + 1]).toFloat(), yLevels[r + 1], (sin(a1) * scaleZ[r + 1]).toFloat())
                out.addFacetTri(p00, p10, p01)
                out.addFacetTri(p10, p11, p01)
            }
        }
        return out.toMesh()
    }

    private fun buildTaperedCapsule(slices: Int, topScale: Float, botScale: Float): GlMesh {
        return buildCylinder(slices, axis = 1, radiusTop = topScale * 0.5f, radiusBot = botScale * 0.5f)
    }

    private fun buildCylinder(slices: Int, axis: Int, radiusTop: Float, radiusBot: Float): GlMesh {
        val out = FloatArrayList(slices * 12 * 8)
        for (i in 0 until slices) {
            val a0 = 2.0 * PI * i / slices
            val a1 = 2.0 * PI * (i + 1) / slices
            val c0 = cos(a0).toFloat()
            val s0 = sin(a0).toFloat()
            val c1 = cos(a1).toFloat()
            val s1 = sin(a1).toFloat()

            fun pt(r: Float, c: Float, s: Float, h: Float): Triple<Float, Float, Float> = when (axis) {
                0 -> Triple(h, r * c, r * s)
                2 -> Triple(r * c, r * s, h)
                else -> Triple(r * c, h, r * s)
            }

            val b0 = pt(radiusBot * 0.5f, c0, s0, -0.5f)
            val b1 = pt(radiusBot * 0.5f, c1, s1, -0.5f)
            val t0 = pt(radiusTop * 0.5f, c0, s0, 0.5f)
            val t1 = pt(radiusTop * 0.5f, c1, s1, 0.5f)
            val centerTop = pt(0f, 0f, 0f, 0.5f)
            val centerBot = pt(0f, 0f, 0f, -0.5f)

            out.addFacetTri(b0, t0, b1)
            out.addFacetTri(t0, t1, b1)
            out.addFacetTri(centerTop, t1, t0)
            out.addFacetTri(centerBot, b0, b1)
        }
        return out.toMesh()
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
        val out = FloatArrayList(stacks * slices * 6 * 8)
        fun rockPt(phi: Double, th: Double): Triple<Float, Float, Float> {
            val disp = 0.42f + 0.14f * sin(phi * 3.2 + th * 2.1).toFloat() * cos(th * 3.7).toFloat()
            return Triple(
                (cos(phi) * cos(th) * disp).toFloat(),
                (sin(phi) * disp * 0.82f).toFloat(),
                (cos(phi) * sin(th) * disp).toFloat()
            )
        }
        for (i in 0 until stacks) {
            val phi0 = PI * i / stacks - PI * 0.5
            val phi1 = PI * (i + 1) / stacks - PI * 0.5
            for (j in 0 until slices) {
                val th0 = 2.0 * PI * j / slices
                val th1 = 2.0 * PI * (j + 1) / slices
                val a = rockPt(phi0, th0)
                val b = rockPt(phi1, th0)
                val c = rockPt(phi0, th1)
                val d = rockPt(phi1, th1)
                out.addFacetTri(a, b, c)
                out.addFacetTri(b, d, c)
            }
        }
        return out.toMesh()
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

    /**
     * Beveled chamfered prism: replaces all raw cubes with sculpted angled edge bevels
     * so specular highlights catch every corner naturally without 90-degree blocky edges.
     */
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
        // Top & bottom caps
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
            // Front wall ribs (+Z)
            out.addFacetQuad(
                Triple(x0, -halfY, halfZ + r0), Triple(x1, -halfY, halfZ + r1),
                Triple(x1, halfY, halfZ + r1), Triple(x0, halfY, halfZ + r0)
            )
            // Back wall ribs (-Z)
            out.addFacetQuad(
                Triple(x1, -halfY, -halfZ - r1), Triple(x0, -halfY, -halfZ - r0),
                Triple(x0, halfY, -halfZ - r0), Triple(x1, halfY, -halfZ - r1)
            )
            // Roof ribs (+Y)
            out.addFacetQuad(
                Triple(x0, halfY + r0, halfZ), Triple(x1, halfY + r1, halfZ),
                Triple(x1, halfY + r1, -halfZ), Triple(x0, halfY + r0, -halfZ)
            )
        }
        // End doors (+X, -X) & floor
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
        // Upper roof slopes
        out.addFacetQuad(p1, p2, rB, rF)
        out.addFacetQuad(p3, p0, rF, rB)
        out.addFacetTri(p0, p1, rF)
        out.addFacetTri(p2, p3, rB)
        // Soffit underside
        out.addFacetQuad(p3, p2, p1, p0)
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
        // Left & right wing upper/lower foils
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
        val segs = 4
        val out = FloatArrayList(segs * 12 * 8)
        for (i in 0 until segs) {
            val x0 = -0.5f + i.toFloat() / segs
            val x1 = -0.5f + (i + 1).toFloat() / segs
            val z0 = sin(i * 1.4f) * 0.16f
            val z1 = sin((i + 1) * 1.4f) * 0.16f
            val p0 = Triple(x0, -0.5f, z0); val p1 = Triple(x1, -0.5f, z1)
            val p2 = Triple(x1, 0.5f, z1); val p3 = Triple(x0, 0.5f, z0)
            out.addFacetQuad(p0, p1, p2, p3)
            out.addFacetQuad(p1, p0, p3, p2)
        }
        return out.toMesh()
    }

    private fun buildTaperedPrism(topScaleX: Float, topScaleZ: Float): GlMesh =
        buildChamferedPrism(bevel = 0.08f, topScaleX = topScaleX, topScaleZ = topScaleZ)

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
