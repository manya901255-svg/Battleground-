package com.example.game.graphics

import com.example.game.model.WeatherType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class ParticleKind {
    DUST,
    SPARK,
    SMOKE,
    SPLASH,
    FIRE,
    RAIN,
    SNOW,
    AURA
}

data class VfxParticle(
    var active: Boolean = false,
    var kind: ParticleKind = ParticleKind.DUST,
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var vz: Float = 0f,
    var size: Float = 0.25f,
    var growRate: Float = 0f,
    var r: Float = 1f,
    var g: Float = 1f,
    var b: Float = 1f,
    var a: Float = 0.8f,
    var age: Float = 0f,
    var life: Float = 0.5f,
    var gravity: Float = 0f
)

/**
 * Zero-allocation object-pooled 3D VFX particle system for sprint dust, tire trails,
 * gunshot sparks/smoke, water splashes, explosions, weather precipitation, and skill auras.
 */
class VfxParticleSystem(maxPoolCapacity: Int = 320) {
    val pool = Array(maxPoolCapacity) { VfxParticle() }
    private var cursor = 0
    @Volatile var activeLimit: Int = maxPoolCapacity

    fun activeCount(): Int = pool.count { it.active }

    fun update(dt: Float, weatherType: WeatherType, mapId: String, camFocusX: Float, camFocusY: Float, camFocusZ: Float) {
        for (p in pool) {
            if (!p.active) continue
            p.age += dt
            if (p.age >= p.life) {
                p.active = false
                continue
            }
            p.vy -= p.gravity * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.z += p.vz * dt
            p.size = (p.size + p.growRate * dt).coerceAtLeast(0.03f)
        }

        // Ambient precipitation / dust motes depending on weather & biome
        val envSpawnCount = when {
            weatherType == WeatherType.SUNSET_STORM -> 4
            mapId == "snow_mountain" -> 2
            weatherType == WeatherType.DENSE_FOG -> 1
            else -> 0
        }
        for (i in 0 until envSpawnCount) {
            val ox = camFocusX + (Random.nextFloat() - 0.5f) * 34f
            val oz = camFocusZ + (Random.nextFloat() - 0.5f) * 34f
            val oy = camFocusY + 12f + Random.nextFloat() * 8f
            if (weatherType == WeatherType.SUNSET_STORM) {
                spawn(
                    ParticleKind.RAIN, ox, oy, oz,
                    vx = -3.2f, vy = -26f, vz = 1.5f,
                    size = 0.08f, grow = 0f,
                    r = 0.72f, g = 0.84f, b = 0.96f, a = 0.45f,
                    life = 0.55f, gravity = 0f
                )
            } else if (mapId == "snow_mountain") {
                spawn(
                    ParticleKind.SNOW, ox, oy, oz,
                    vx = (Random.nextFloat() - 0.5f) * 1.4f, vy = -3.4f, vz = (Random.nextFloat() - 0.5f) * 1.4f,
                    size = 0.14f, grow = 0f,
                    r = 0.95f, g = 0.98f, b = 1.0f, a = 0.65f,
                    life = 2.4f, gravity = 0f
                )
            } else {
                spawn(
                    ParticleKind.SMOKE, ox, camFocusY + 0.6f, oz,
                    vx = 0.7f, vy = 0.25f, vz = 0.4f,
                    size = 1.4f, grow = 0.6f,
                    r = 0.72f, g = 0.78f, b = 0.82f, a = 0.16f,
                    life = 2.0f, gravity = 0f
                )
            }
        }
    }

    fun spawnFootstepDust(x: Float, y: Float, z: Float, isDesert: Boolean) {
        val (dr, dg, db) = if (isDesert) Triple(0.82f, 0.70f, 0.48f) else Triple(0.58f, 0.55f, 0.46f)
        repeat(2) {
            spawn(
                ParticleKind.DUST,
                x + (Random.nextFloat() - 0.5f) * 0.35f,
                y + 0.12f,
                z + (Random.nextFloat() - 0.5f) * 0.35f,
                vx = (Random.nextFloat() - 0.5f) * 0.9f,
                vy = 0.55f + Random.nextFloat() * 0.45f,
                vz = (Random.nextFloat() - 0.5f) * 0.9f,
                size = 0.22f,
                grow = 0.45f,
                r = dr, g = dg, b = db, a = 0.42f,
                life = 0.45f,
                gravity = 0.3f
            )
        }
    }

    fun spawnWaterSplash(x: Float, y: Float, z: Float, intensity: Int = 3) {
        repeat(intensity) {
            val ang = Random.nextFloat() * 6.283f
            val spd = 1.2f + Random.nextFloat() * 1.8f
            spawn(
                ParticleKind.SPLASH,
                x + cos(ang) * 0.2f,
                y + 0.15f,
                z + sin(ang) * 0.2f,
                vx = cos(ang) * spd,
                vy = 2.2f + Random.nextFloat() * 1.8f,
                vz = sin(ang) * spd,
                size = 0.16f,
                grow = 0.22f,
                r = 0.82f, g = 0.94f, b = 1.0f, a = 0.75f,
                life = 0.48f,
                gravity = 8.5f
            )
        }
    }

    fun spawnMuzzleSmokeAndSparks(x: Float, y: Float, z: Float, dirX: Float, dirZ: Float) {
        spawn(
            ParticleKind.SMOKE, x, y, z,
            vx = dirX * 2.2f, vy = 0.7f, vz = dirZ * 2.2f,
            size = 0.22f, grow = 0.75f,
            r = 0.76f, g = 0.78f, b = 0.80f, a = 0.48f,
            life = 0.52f, gravity = -0.3f
        )
        repeat(3) {
            spawn(
                ParticleKind.SPARK, x, y, z,
                vx = dirX * (4f + Random.nextFloat() * 4f) + (Random.nextFloat() - 0.5f) * 2.2f,
                vy = (Random.nextFloat() - 0.2f) * 2.5f,
                vz = dirZ * (4f + Random.nextFloat() * 4f) + (Random.nextFloat() - 0.5f) * 2.2f,
                size = 0.11f, grow = -0.1f,
                r = 1.0f, g = 0.82f, b = 0.22f, a = 0.95f,
                life = 0.22f, gravity = 6.5f
            )
        }
    }

    fun spawnExplosionBurst(x: Float, y: Float, z: Float, radius: Float) {
        val count = (radius * 1.8f).toInt().coerceIn(8, 28)
        repeat(count) { i ->
            val ang = (i.toFloat() / count) * 6.283f + Random.nextFloat() * 0.4f
            val spd = (2.5f + Random.nextFloat() * 6.5f)
            val isFire = i % 2 == 0
            spawn(
                kind = if (isFire) ParticleKind.FIRE else ParticleKind.SMOKE,
                x = x + cos(ang) * 0.4f,
                y = y + 0.5f + Random.nextFloat() * 0.8f,
                z = z + sin(ang) * 0.4f,
                vx = cos(ang) * spd,
                vy = 2.8f + Random.nextFloat() * 5.2f,
                vz = sin(ang) * spd,
                size = if (isFire) 0.9f else 1.25f,
                grow = if (isFire) 1.4f else 2.1f,
                r = if (isFire) 1.0f else 0.22f,
                g = if (isFire) 0.48f else 0.22f,
                b = if (isFire) 0.08f else 0.24f,
                a = if (isFire) 0.90f else 0.72f,
                life = if (isFire) 0.55f else 1.15f,
                gravity = if (isFire) 1.5f else -0.6f
            )
        }
    }

    fun spawnSkillAura(x: Float, y: Float, z: Float, r: Float, g: Float, b: Float) {
        val ang = Random.nextFloat() * 6.283f
        spawn(
            ParticleKind.AURA,
            x + cos(ang) * 0.65f,
            y + 0.2f,
            z + sin(ang) * 0.65f,
            vx = cos(ang) * 0.25f,
            vy = 1.6f + Random.nextFloat() * 1.1f,
            vz = sin(ang) * 0.25f,
            size = 0.16f,
            grow = -0.10f,
            r = r, g = g, b = b, a = 0.88f,
            life = 0.60f,
            gravity = 0f
        )
    }

    private fun spawn(
        kind: ParticleKind,
        x: Float, y: Float, z: Float,
        vx: Float, vy: Float, vz: Float,
        size: Float, grow: Float,
        r: Float, g: Float, b: Float, a: Float,
        life: Float, gravity: Float
    ) {
        val limit = activeLimit.coerceIn(1, pool.size)
        val slot = pool[cursor % limit]
        cursor = (cursor + 1) % limit
        slot.active = true
        slot.kind = kind
        slot.x = x; slot.y = y; slot.z = z
        slot.vx = vx; slot.vy = vy; slot.vz = vz
        slot.size = size; slot.growRate = grow
        slot.r = r; slot.g = g; slot.b = b; slot.a = a
        slot.age = 0f; slot.life = life
        slot.gravity = gravity
    }
}
