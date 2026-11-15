package com.example.game.graphics

import com.example.game.map.MapConfig
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
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
 * Procedural 3D mesh library providing sculpted organic, architectural, vehicle,
 * character-anatomy, and heightfield geometry to eliminate blocky cube placeholders.
 */
class MeshLibrary {
    val unitBox: GlMesh = buildBox()
    val skyDome: GlMesh = buildSphere(stacks = 12, slices = 24, inward = true)
    val softShadowDisc: GlMesh = buildDisc(segments = 18)
    val quadBillboard: GlMesh = buildQuad()

    // Articulated human anatomical meshes
    val humanHead: GlMesh = buildSculptedHead()
    val humanHair: GlMesh = buildHairCrown()
    val helmetShell: GlMesh = buildHemisphereShell(stacks = 8, slices = 14)
    val curvedVisor: GlMesh = buildCurvedVisor()
    val chestTorso: GlMesh = buildAnatomicalTorso()
    val limbCapsule: GlMesh = buildTaperedCapsule(slices = 10, topScale = 1.0f, botScale = 0.76f)
    val taperedBoot: GlMesh = buildTaperedCapsule(slices = 8, topScale = 0.85f, botScale = 1.15f)
    val bevelPack: GlMesh = buildTaperedPrism(topScaleX = 0.86f, topScaleZ = 0.82f)

    // Vehicles
    val wheelCylinder: GlMesh = buildCylinder(slices = 18, axis = 2, radiusTop = 1f, radiusBot = 1f)
    val chassisSlopeBody: GlMesh = buildTaperedPrism(topScaleX = 0.88f, topScaleZ = 0.90f)
    val cabSlopedRoof: GlMesh = buildTaperedPrism(topScaleX = 0.72f, topScaleZ = 0.82f)

    // Weapons
    val rifleBarrel: GlMesh = buildCylinder(slices = 8, axis = 0, radiusTop = 0.85f, radiusBot = 1.0f)

    // Environment organic meshes
    val treeTrunk: GlMesh = buildCylinder(slices = 10, axis = 1, radiusTop = 0.58f, radiusBot = 1.28f)
    val foliageCone: GlMesh = buildCone(slices = 12, droopAmount = 0.14f)
    val boulderRock: GlMesh = buildDisplacedBoulder(stacks = 6, slices = 10)
    val bushCluster: GlMesh = buildDisplacedBoulder(stacks = 5, slices = 8)
    val grassTuft: GlMesh = buildGrassCrossTuft()
    val pitchedRoof: GlMesh = buildGableRoof()
    val waterGrid: GlMesh = buildWaterPlaneGrid(12)

    fun buildTerrainMeshForMap(mapConfig: MapConfig, gridRes: Int = 56): GlMesh {
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
        return out.toMesh()
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
        val stacks = 9
        val slices = 14
        val out = FloatArrayList(stacks * slices * 6 * 8)
        fun headPt(phi: Double, th: Double): Triple<Float, Float, Float> {
            val x = (cos(phi) * cos(th) * 0.48).toFloat()
            val y = (sin(phi) * 0.52).toFloat()
            val z = (cos(phi) * sin(th) * 0.44).toFloat()
            // Narrow lower jaw toward chin (front is +x in character local frame)
            val jawTaper = if (y < 0f) 0.78f + (y + 0.52f) * 0.42f else 1.0f
            val browRidge = if (x > 0.15f && y in -0.05f..0.18f) 1.06f else 1.0f
            return Triple(x * jawTaper * browRidge, y, z * jawTaper)
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

    private fun buildHairCrown(): GlMesh {
        val stacks = 6
        val slices = 12
        val out = FloatArrayList(stacks * slices * 6 * 8)
        fun hairPt(phi: Double, th: Double): Triple<Float, Float, Float> {
            val spike = 1f + 0.14f * sin((th * 6.0 + phi * 4.0)).toFloat().coerceAtLeast(0f)
            val x = (cos(phi) * cos(th) * 0.51 * spike).toFloat()
            val y = (sin(phi) * 0.48 * spike + 0.08).toFloat()
            val z = (cos(phi) * sin(th) * 0.47 * spike).toFloat()
            return Triple(x, y, z)
        }
        for (i in 0 until stacks) {
            val phi0 = (PI * 0.55) * i / stacks
            val phi1 = (PI * 0.55) * (i + 1) / stacks
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
        val slices = 10
        val rings = 4 // waist -> ribs -> chest -> shoulders
        val out = FloatArrayList(slices * rings * 6 * 8)
        val yLevels = floatArrayOf(-0.5f, -0.15f, 0.25f, 0.5f)
        val scaleX = floatArrayOf(0.42f, 0.48f, 0.54f, 0.46f)
        val scaleZ = floatArrayOf(0.38f, 0.43f, 0.52f, 0.54f)
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
            // Tapered grass blade triangle strip as 2 triangles (wide base -> pointed tip)
            out.addVertex(-cx, 0f, -cz, nx, 0.35f, nz, 0f, 0f)
            out.addVertex(cx, 0f, cz, nx, 0.35f, nz, 1f, 0f)
            out.addVertex(cx * 0.25f, 1f, cz * 0.25f, nx, 0.35f, nz, 0.7f, 1f)
            out.addVertex(-cx, 0f, -cz, nx, 0.35f, nz, 0f, 0f)
            out.addVertex(cx * 0.25f, 1f, cz * 0.25f, nx, 0.35f, nz, 0.7f, 1f)
            out.addVertex(-cx * 0.25f, 1f, -cz * 0.25f, nx, 0.35f, nz, 0.3f, 1f)
        }
        return out.toMesh()
    }

    private fun buildTaperedPrism(topScaleX: Float, topScaleZ: Float): GlMesh {
        val bx = 0.5f; val bz = 0.5f
        val tx = 0.5f * topScaleX; val tz = 0.5f * topScaleZ
        val out = FloatArrayList(36 * 8)
        val b00 = Triple(-bx, -0.5f, -bz); val b10 = Triple(bx, -0.5f, -bz)
        val b11 = Triple(bx, -0.5f, bz); val b01 = Triple(-bx, -0.5f, bz)
        val t00 = Triple(-tx, 0.5f, -tz); val t10 = Triple(tx, 0.5f, -tz)
        val t11 = Triple(tx, 0.5f, tz); val t01 = Triple(-tx, 0.5f, tz)
        // Front (+z), Back (-z), Left (-x), Right (+x), Top (+y), Bot (-y)
        out.addFacetQuad(b01, b11, t11, t01)
        out.addFacetQuad(b10, b00, t00, t10)
        out.addFacetQuad(b00, b01, t01, t00)
        out.addFacetQuad(b11, b10, t10, t11)
        out.addFacetQuad(t01, t11, t10, t00)
        out.addFacetQuad(b00, b10, b11, b01)
        return out.toMesh()
    }

    private fun buildGableRoof(): GlMesh {
        val out = FloatArrayList(18 * 8)
        val p0 = Triple(-0.5f, -0.5f, 0.5f); val p1 = Triple(0.5f, -0.5f, 0.5f)
        val p2 = Triple(0.5f, -0.5f, -0.5f); val p3 = Triple(-0.5f, -0.5f, -0.5f)
        val rF = Triple(0f, 0.5f, 0.5f); val rB = Triple(0f, 0.5f, -0.5f)
        out.addFacetQuad(p1, p2, rB, rF) // right slope
        out.addFacetQuad(p3, p0, rF, rB) // left slope
        out.addFacetTri(p0, p1, rF)      // front gable
        out.addFacetTri(p2, p3, rB)      // back gable
        return out.toMesh()
    }

    private fun buildBox(): GlMesh = buildTaperedPrism(1f, 1f)

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
}
