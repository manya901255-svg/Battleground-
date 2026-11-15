package com.example.game.map

import com.example.game.model.AmmoType
import com.example.game.model.ConsumableType
import com.example.game.model.GroundLoot
import com.example.game.model.MagType
import com.example.game.model.MuzzleType
import com.example.game.model.ScopeType
import com.example.game.model.ThrowableType
import com.example.game.model.VehicleInstance
import com.example.game.model.VehicleType
import com.example.game.model.WeaponCatalog
import kotlin.random.Random

data class MapZone(
    val name: String,
    val x: Float,
    val y: Float,
    val radius: Float,
    val lootTier: Int // 1: Standard, 2: High, 3: Military Hot-Drop
)

data class Building(
    val id: String,
    val name: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val wallColorHex: Long = 0xFF37474F,
    val roofColorHex: Long = 0xFF263238
)

data class Obstacle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val type: ObstacleType
)

enum class ObstacleType {
    TREE,
    ROCK,
    CRATE,
    SANDBAGS
}

data class MapConfig(
    val id: String,
    val name: String,
    val description: String,
    val worldSize: Float = 5000f,
    val zones: List<MapZone>,
    val buildings: List<Building>,
    val obstacles: List<Obstacle>,
    val waterBodies: List<WaterBody>
)

data class WaterBody(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

object MapCatalog {
    fun getKavalaIsland(): MapConfig {
        val zones = listOf(
            MapZone("Fort Apex Military", 2500f, 4100f, 400f, 3),
            MapZone("Pochinki City", 2500f, 2500f, 350f, 3),
            MapZone("Port Kraken Docks", 1100f, 1200f, 320f, 3),
            MapZone("Novoe Harbor", 3900f, 3700f, 300f, 2),
            MapZone("Shelter Bunkers", 3400f, 2700f, 260f, 2),
            MapZone("Mountain Observatory", 2100f, 1500f, 280f, 2),
            MapZone("Gatka Windmills", 1300f, 2800f, 240f, 1),
            MapZone("Yasnaya Towers", 3800f, 1600f, 320f, 3),
            MapZone("Quarry Excavation", 1400f, 3800f, 280f, 2),
            MapZone("Ruins of Erangel", 2000f, 2100f, 220f, 2)
        )

        val buildings = mutableListOf<Building>()
        // Generate buildings inside zones
        zones.forEachIndexed { zIndex, zone ->
            val bCount = if (zone.lootTier == 3) 8 else if (zone.lootTier == 2) 5 else 3
            for (i in 0 until bCount) {
                val offsetX = (Random.nextFloat() - 0.5f) * zone.radius * 1.3f
                val offsetY = (Random.nextFloat() - 0.5f) * zone.radius * 1.3f
                buildings.add(
                    Building(
                        id = "b_${zIndex}_$i",
                        name = "${zone.name} Compound $i",
                        x = zone.x + offsetX,
                        y = zone.y + offsetY,
                        width = 90f + Random.nextFloat() * 40f,
                        height = 70f + Random.nextFloat() * 40f
                    )
                )
            }
        }

        val obstacles = mutableListOf<Obstacle>()
        // Scatter trees and rocks across the landscape
        for (i in 0..120) {
            val ox = 400f + Random.nextFloat() * 4200f
            val oy = 400f + Random.nextFloat() * 4200f
            obstacles.add(
                Obstacle(
                    x = ox,
                    y = oy,
                    radius = 18f + Random.nextFloat() * 14f,
                    type = if (Random.nextBoolean()) ObstacleType.TREE else ObstacleType.ROCK
                )
            )
        }

        val waterBodies = listOf(
            WaterBody(x = 1000f, y = 3200f, width = 3000f, height = 350f) // River dividing island
        )

        return MapConfig(
            id = "kavala_island",
            name = "Kavala Island",
            description = "8x8 km Iconic tactical island with military base, cities and bridge chokepoints",
            worldSize = 5000f,
            zones = zones,
            buildings = buildings,
            obstacles = obstacles,
            waterBodies = waterBodies
        )
    }

    fun getKalahariDunes(): MapConfig {
        val zones = listOf(
            MapZone("Oil Refinery", 2200f, 2400f, 380f, 3),
            MapZone("Canyon Outpost", 1500f, 1500f, 300f, 2),
            MapZone("Oasis Town", 3500f, 2000f, 320f, 2),
            MapZone("Abandoned Airfield", 2800f, 3700f, 350f, 3),
            MapZone("Sunken Temple", 1400f, 3600f, 260f, 2)
        )
        val buildings = mutableListOf<Building>()
        zones.forEachIndexed { zIndex, zone ->
            for (i in 0..5) {
                val ox = (Random.nextFloat() - 0.5f) * zone.radius
                val oy = (Random.nextFloat() - 0.5f) * zone.radius
                buildings.add(Building("kd_${zIndex}_$i", "Compound", zone.x + ox, zone.y + oy, 85f, 75f))
            }
        }
        val obstacles = mutableListOf<Obstacle>()
        for (i in 0..80) {
            obstacles.add(
                Obstacle(
                    x = 500f + Random.nextFloat() * 4000f,
                    y = 500f + Random.nextFloat() * 4000f,
                    radius = 22f,
                    type = ObstacleType.ROCK
                )
            )
        }
        return MapConfig(
            id = "kalahari_dunes",
            name = "Desert City",
            description = "4x4 km Scorching desert dunes with oil refinery, abandoned towns and canyons",
            worldSize = 5000f,
            zones = zones,
            buildings = buildings,
            obstacles = obstacles,
            waterBodies = emptyList()
        )
    }

    fun getSnowMountain(): MapConfig {
        val zones = listOf(
            MapZone("Radar Peak Bunker", 2500f, 1500f, 350f, 3),
            MapZone("Cosmodrome Launchpad", 1500f, 3500f, 380f, 3),
            MapZone("Frozen Lake Station", 3500f, 2500f, 300f, 2),
            MapZone("Alpine Village", 2500f, 3800f, 280f, 2),
            MapZone("Glacier Outpost", 1200f, 1800f, 260f, 1)
        )
        val buildings = mutableListOf<Building>()
        zones.forEachIndexed { zIndex, zone ->
            for (i in 0..5) {
                buildings.add(Building("snow_${zIndex}_$i", "Arctic Bunker", zone.x + (Random.nextFloat() - 0.5f) * zone.radius, zone.y + (Random.nextFloat() - 0.5f) * zone.radius, 90f, 80f))
            }
        }
        val obstacles = (0..90).map {
            Obstacle(x = 400f + Random.nextFloat() * 4200f, y = 400f + Random.nextFloat() * 4200f, radius = 20f, type = ObstacleType.ROCK)
        }
        return MapConfig(
            id = "snow_mountain",
            name = "Snow Mountain",
            description = "6x6 km Freezing sub-zero mountain with radar bases, frozen lakes and ski bunkers",
            worldSize = 5000f,
            zones = zones,
            buildings = buildings,
            obstacles = obstacles,
            waterBodies = emptyList()
        )
    }

    fun getUrbanCity(): MapConfig {
        val zones = listOf(
            MapZone("Metropolis Central", 2500f, 2500f, 450f, 3),
            MapZone("Commercial Plaza", 1800f, 1800f, 320f, 3),
            MapZone("Cargo Seaport", 3500f, 3500f, 350f, 3),
            MapZone("Suburban Complex", 1500f, 3200f, 300f, 2),
            MapZone("Subway Interchange", 3200f, 1500f, 280f, 2)
        )
        val buildings = mutableListOf<Building>()
        zones.forEachIndexed { zIndex, zone ->
            for (i in 0..7) {
                buildings.add(Building("city_${zIndex}_$i", "Skyscraper Base", zone.x + (Random.nextFloat() - 0.5f) * zone.radius, zone.y + (Random.nextFloat() - 0.5f) * zone.radius, 110f, 95f))
            }
        }
        val obstacles = (0..70).map {
            Obstacle(x = 500f + Random.nextFloat() * 4000f, y = 500f + Random.nextFloat() * 4000f, radius = 16f, type = ObstacleType.CRATE)
        }
        return MapConfig(
            id = "urban_city",
            name = "Modern Urban City",
            description = "8x8 km Dense multi-story skyscraper city with highways, bridges and cargo docks",
            worldSize = 5000f,
            zones = zones,
            buildings = buildings,
            obstacles = obstacles,
            waterBodies = listOf(WaterBody(500f, 2400f, 4000f, 250f))
        )
    }

    fun getJungleBattleground(): MapConfig {
        val zones = listOf(
            MapZone("Ancient Temple Ruins", 2500f, 2500f, 400f, 3),
            MapZone("River Plantation", 1500f, 2000f, 320f, 2),
            MapZone("Guerrilla Camp", 3500f, 3200f, 300f, 3),
            MapZone("Canopy Ropeways", 2200f, 3700f, 280f, 2),
            MapZone("Hidden Waterfall", 3400f, 1600f, 260f, 2)
        )
        val buildings = mutableListOf<Building>()
        zones.forEachIndexed { zIndex, zone ->
            for (i in 0..5) {
                buildings.add(Building("jungle_${zIndex}_$i", "Temple Compound", zone.x + (Random.nextFloat() - 0.5f) * zone.radius, zone.y + (Random.nextFloat() - 0.5f) * zone.radius, 85f, 75f))
            }
        }
        val obstacles = (0..150).map {
            Obstacle(x = 400f + Random.nextFloat() * 4200f, y = 400f + Random.nextFloat() * 4200f, radius = 24f, type = ObstacleType.TREE)
        }
        return MapConfig(
            id = "jungle_battleground",
            name = "Jungle Battleground",
            description = "6x6 km Thick tropical jungle canopy with ancient temple ruins and mist valleys",
            worldSize = 5000f,
            zones = zones,
            buildings = buildings,
            obstacles = obstacles,
            waterBodies = listOf(WaterBody(1200f, 1000f, 2600f, 300f))
        )
    }

    fun getAllMaps(): List<MapConfig> = listOf(
        getKavalaIsland(),
        getKalahariDunes(),
        getSnowMountain(),
        getUrbanCity(),
        getJungleBattleground()
    )

    fun generateInitialLoot(mapConfig: MapConfig): List<GroundLoot> {
        val lootList = mutableListOf<GroundLoot>()

        // Spawn loot in every building
        mapConfig.buildings.forEach { b ->
            // 2 to 4 loot piles per building
            val piles = 2 + Random.nextInt(3)
            for (p in 0 until piles) {
                val lx = b.x + 20f + Random.nextFloat() * (b.width - 40f)
                val ly = b.y + 20f + Random.nextFloat() * (b.height - 40f)
                val roll = Random.nextFloat()

                when {
                    roll < 0.28f -> {
                        // AR or SMG
                        val w = if (Random.nextBoolean()) WeaponCatalog.createM416() else WeaponCatalog.createAKM()
                        lootList.add(GroundLoot(x = lx, y = ly, name = w.name, weapon = w))
                        // Also spawn ammo nearby
                        lootList.add(GroundLoot(x = lx + 15f, y = ly + 10f, name = w.ammoType.displayName, ammoType = w.ammoType, ammoCount = 60))
                    }
                    roll < 0.42f -> {
                        // Sniper or Shotgun
                        val w = if (Random.nextBoolean()) WeaponCatalog.createKar98k() else WeaponCatalog.createUMP45()
                        lootList.add(GroundLoot(x = lx, y = ly, name = w.name, weapon = w))
                        lootList.add(GroundLoot(x = lx + 12f, y = ly + 8f, name = w.ammoType.displayName, ammoType = w.ammoType, ammoCount = 30))
                    }
                    roll < 0.58f -> {
                        // Consumable
                        val cons = ConsumableType.values()[Random.nextInt(ConsumableType.values().size)]
                        lootList.add(GroundLoot(x = lx, y = ly, name = cons.displayName, consumable = cons))
                    }
                    roll < 0.70f -> {
                        // Armor / Helmet / Backpack
                        val lvl = 1 + Random.nextInt(3)
                        lootList.add(GroundLoot(x = lx, y = ly, name = "Level $lvl Armor Vest", armorLevel = lvl))
                    }
                    roll < 0.82f -> {
                        val lvl = 1 + Random.nextInt(3)
                        lootList.add(GroundLoot(x = lx, y = ly, name = "Level $lvl Helmet", helmetLevel = lvl))
                    }
                    roll < 0.92f -> {
                        // Throwable
                        val thro = if (Random.nextBoolean()) ThrowableType.FRAG_GRENADE else ThrowableType.SMOKE_GRENADE
                        lootList.add(GroundLoot(x = lx, y = ly, name = thro.displayName, throwable = thro))
                    }
                    else -> {
                        // Scopes
                        val sc = if (Random.nextBoolean()) ScopeType.SCOPE_4X else ScopeType.RED_DOT
                        lootList.add(GroundLoot(x = lx, y = ly, name = sc.displayName, scope = sc))
                    }
                }
            }
        }
        return lootList
    }

    fun generateVehicles(mapConfig: MapConfig): List<VehicleInstance> {
        val vehicles = mutableListOf<VehicleInstance>()
        // Place vehicles near roads and towns
        mapConfig.zones.forEach { zone ->
            val vType = when (Random.nextInt(4)) {
                0 -> VehicleType.DACIA_SEDAN
                1 -> VehicleType.UAZ_JEEP
                2 -> VehicleType.BUGGY
                else -> VehicleType.MOTORBIKE
            }
            vehicles.add(
                VehicleInstance(
                    type = vType,
                    x = zone.x + zone.radius * 0.7f,
                    y = zone.y + zone.radius * 0.7f,
                    rotationAngle = Random.nextFloat() * 360f
                )
            )
        }
        return vehicles
    }

    fun generateBarrels(mapConfig: MapConfig): List<com.example.game.model.ExplosiveBarrel> {
        val barrels = mutableListOf<com.example.game.model.ExplosiveBarrel>()
        // Central compound explosive hazard drums (matching reference arena layout)
        barrels.add(com.example.game.model.ExplosiveBarrel(x = 2685f, y = 2430f))
        barrels.add(com.example.game.model.ExplosiveBarrel(x = 2420f, y = 2360f))
        barrels.add(com.example.game.model.ExplosiveBarrel(x = 2595f, y = 2590f))
        mapConfig.zones.forEach { zone ->
            barrels.add(
                com.example.game.model.ExplosiveBarrel(
                    x = zone.x + (Random.nextFloat() - 0.5f) * zone.radius * 0.9f,
                    y = zone.y + (Random.nextFloat() - 0.5f) * zone.radius * 0.9f
                )
            )
        }
        return barrels
    }
}
