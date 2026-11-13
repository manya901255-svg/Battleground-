package com.example.game.model

import java.util.UUID

enum class WeaponCategory(val displayName: String) {
    ASSAULT_RIFLE("Assault Rifle"),
    SNIPER_RIFLE("Sniper Rifle"),
    SMG("Submachine Gun"),
    SHOTGUN("Shotgun"),
    PISTOL("Pistol"),
    MELEE("Melee"),
    THROWABLE("Throwable")
}

enum class AmmoType(val displayName: String, val caliber: String) {
    AMMO_556("5.56mm", "Green Box"),
    AMMO_762("7.62mm", "Brown Box"),
    AMMO_300MAG(".300 Magnum", "Black Box"),
    AMMO_9MM("9mm", "Orange Box"),
    AMMO_12GAUGE("12 Gauge", "Red Shells"),
    AMMO_45ACP(".45 ACP", "Blue Box"),
    NONE("None", "")
}

enum class ScopeType(val magnification: Float, val displayName: String) {
    IRON_SIGHT(1.0f, "Iron Sights"),
    RED_DOT(1.2f, "Red Dot"),
    HOLO(1.25f, "Holographic"),
    SCOPE_2X(2.0f, "2x Scope"),
    SCOPE_4X(4.0f, "4x ACOG Scope"),
    SCOPE_8X(8.0f, "8x Ballistic Scope")
}

enum class MuzzleType(val displayName: String, val recoilReduction: Float, val isSilenced: Boolean) {
    NONE("None", 0f, false),
    COMPENSATOR("Compensator", 0.35f, false),
    SUPPRESSOR("Suppressor", 0.15f, true),
    FLASH_HIDER("Flash Hider", 0.20f, false)
}

enum class MagType(val displayName: String, val extraCapacity: Int, val reloadSpeedMultiplier: Float) {
    STANDARD("Standard Mag", 0, 1.0f),
    EXTENDED("Extended Mag", 10, 1.0f),
    QUICKDRAW("Quickdraw Mag", 0, 1.35f),
    EXTENDED_QUICKDRAW("Ext. Quickdraw Mag", 10, 1.35f)
}

enum class FireMode {
    SINGLE,
    BURST,
    FULL_AUTO
}

data class Weapon(
    val id: String,
    val name: String,
    val category: WeaponCategory,
    val ammoType: AmmoType,
    val baseDamage: Float,
    val fireRateMs: Long,
    val bulletSpeed: Float,
    val baseRecoil: Float,
    val maxCapacity: Int,
    var currentAmmo: Int,
    val supportedFireModes: List<FireMode>,
    var activeFireMode: FireMode = FireMode.FULL_AUTO,
    var equippedScope: ScopeType = ScopeType.IRON_SIGHT,
    var equippedMuzzle: MuzzleType = MuzzleType.NONE,
    var equippedMag: MagType = MagType.STANDARD,
    val skinName: String = "Standard Military"
) {
    val totalCapacity: Int
        get() = maxCapacity + equippedMag.extraCapacity

    val effectiveRecoil: Float
        get() = (baseRecoil * (1f - equippedMuzzle.recoilReduction)).coerceAtLeast(0.1f)
}

object WeaponCatalog {
    fun createM416(skin: String = "Tactical Slate"): Weapon = Weapon(
        id = "m416",
        name = "M416",
        category = WeaponCategory.ASSAULT_RIFLE,
        ammoType = AmmoType.AMMO_556,
        baseDamage = 41f,
        fireRateMs = 86L,
        bulletSpeed = 880f,
        baseRecoil = 0.55f,
        maxCapacity = 30,
        currentAmmo = 30,
        supportedFireModes = listOf(FireMode.SINGLE, FireMode.FULL_AUTO),
        activeFireMode = FireMode.FULL_AUTO,
        skinName = skin
    )

    fun createAKM(skin: String = "Dragonfire"): Weapon = Weapon(
        id = "akm",
        name = "AKM",
        category = WeaponCategory.ASSAULT_RIFLE,
        ammoType = AmmoType.AMMO_762,
        baseDamage = 49f,
        fireRateMs = 100L,
        bulletSpeed = 715f,
        baseRecoil = 0.85f,
        maxCapacity = 30,
        currentAmmo = 30,
        supportedFireModes = listOf(FireMode.SINGLE, FireMode.FULL_AUTO),
        activeFireMode = FireMode.FULL_AUTO,
        skinName = skin
    )

    fun createAWM(skin: String = "Cyber Gold"): Weapon = Weapon(
        id = "awm",
        name = "AWM",
        category = WeaponCategory.SNIPER_RIFLE,
        ammoType = AmmoType.AMMO_300MAG,
        baseDamage = 105f,
        fireRateMs = 1850L,
        bulletSpeed = 945f,
        baseRecoil = 1.2f,
        maxCapacity = 5,
        currentAmmo = 5,
        supportedFireModes = listOf(FireMode.SINGLE),
        activeFireMode = FireMode.SINGLE,
        equippedScope = ScopeType.SCOPE_8X,
        skinName = skin
    )

    fun createKar98k(skin: String = "Vintage Walnut"): Weapon = Weapon(
        id = "kar98k",
        name = "Kar98k",
        category = WeaponCategory.SNIPER_RIFLE,
        ammoType = AmmoType.AMMO_762,
        baseDamage = 79f,
        fireRateMs = 1900L,
        bulletSpeed = 760f,
        baseRecoil = 1.1f,
        maxCapacity = 5,
        currentAmmo = 5,
        supportedFireModes = listOf(FireMode.SINGLE),
        activeFireMode = FireMode.SINGLE,
        equippedScope = ScopeType.SCOPE_4X,
        skinName = skin
    )

    fun createUMP45(skin: String = "Arctic Camo"): Weapon = Weapon(
        id = "ump45",
        name = "UMP45",
        category = WeaponCategory.SMG,
        ammoType = AmmoType.AMMO_45ACP,
        baseDamage = 38f,
        fireRateMs = 92L,
        bulletSpeed = 400f,
        baseRecoil = 0.40f,
        maxCapacity = 25,
        currentAmmo = 25,
        supportedFireModes = listOf(FireMode.SINGLE, FireMode.BURST, FireMode.FULL_AUTO),
        activeFireMode = FireMode.FULL_AUTO,
        equippedScope = ScopeType.RED_DOT,
        skinName = skin
    )

    fun createS12K(skin: String = "Breacher Steel"): Weapon = Weapon(
        id = "s12k",
        name = "S12K",
        category = WeaponCategory.SHOTGUN,
        ammoType = AmmoType.AMMO_12GAUGE,
        baseDamage = 24f * 8f, // 8 pellets
        fireRateMs = 250L,
        bulletSpeed = 350f,
        baseRecoil = 1.4f,
        maxCapacity = 5,
        currentAmmo = 5,
        supportedFireModes = listOf(FireMode.SINGLE),
        activeFireMode = FireMode.SINGLE,
        skinName = skin
    )

    fun createDesertEagle(): Weapon = Weapon(
        id = "deagle",
        name = "Desert Eagle",
        category = WeaponCategory.PISTOL,
        ammoType = AmmoType.AMMO_45ACP,
        baseDamage = 62f,
        fireRateMs = 320L,
        bulletSpeed = 450f,
        baseRecoil = 0.9f,
        maxCapacity = 7,
        currentAmmo = 7,
        supportedFireModes = listOf(FireMode.SINGLE),
        activeFireMode = FireMode.SINGLE
    )

    fun createPan(): Weapon = Weapon(
        id = "pan",
        name = "Cast Iron Pan",
        category = WeaponCategory.MELEE,
        ammoType = AmmoType.NONE,
        baseDamage = 80f,
        fireRateMs = 750L,
        bulletSpeed = 0f,
        baseRecoil = 0f,
        maxCapacity = 0,
        currentAmmo = 0,
        supportedFireModes = listOf(FireMode.SINGLE),
        activeFireMode = FireMode.SINGLE
    )
}

enum class ConsumableType(val displayName: String, val healAmount: Float, val boostAmount: Float, val useTimeSec: Float) {
    BANDAGE("Bandage", 10f, 0f, 4f),
    FIRST_AID("First Aid Kit", 75f, 0f, 6f),
    MEDKIT("Med Kit", 100f, 0f, 8f),
    ENERGY_DRINK("Energy Drink", 0f, 40f, 4f),
    PAINKILLER("Painkiller", 0f, 60f, 6f),
    ADRENALINE("Adrenaline Syringe", 0f, 100f, 6f)
}

enum class ThrowableType(val displayName: String, val fuseSec: Float, val blastRadius: Float, val damage: Float) {
    FRAG_GRENADE("Frag Grenade", 5f, 120f, 100f),
    SMOKE_GRENADE("Smoke Grenade", 4f, 160f, 0f),
    MOLOTOV("Molotov Cocktail", 0f, 100f, 45f),
    STUN_GRENADE("Flashbang", 2.5f, 140f, 5f)
}

enum class VehicleType(val displayName: String, val maxSpeed: Float, val health: Float, val seats: Int) {
    DACIA_SEDAN("Dacia Sedan", 115f, 600f, 4),
    UAZ_JEEP("Armored UAZ 4x4", 95f, 950f, 4),
    BUGGY("Offroad Buggy", 105f, 500f, 2),
    MOTORBIKE("Tactical Motorbike", 130f, 350f, 2),
    SPEEDBOAT("Assault Speedboat", 90f, 750f, 4)
}

data class VehicleInstance(
    val id: String = UUID.randomUUID().toString(),
    val type: VehicleType,
    var x: Float,
    var y: Float,
    var rotationAngle: Float = 0f,
    var currentSpeed: Float = 0f,
    var currentHealth: Float = type.health,
    var currentFuel: Float = 100f,
    var driverPlayerId: String? = null,
    val passengerPlayerIds: MutableList<String> = mutableListOf(),
    var isDestroyed: Boolean = false
)

enum class PlayerStance {
    STAND,
    CROUCH,
    PRONE,
    JUMPING,
    SWIMMING,
    DRIVING
}

data class GroundLoot(
    val id: String = UUID.randomUUID().toString(),
    val x: Float,
    val y: Float,
    val name: String,
    val weapon: Weapon? = null,
    val ammoType: AmmoType = AmmoType.NONE,
    val ammoCount: Int = 0,
    val consumable: ConsumableType? = null,
    val throwable: ThrowableType? = null,
    val armorLevel: Int = 0,
    val helmetLevel: Int = 0,
    val backpackLevel: Int = 0,
    val scope: ScopeType? = null,
    val muzzle: MuzzleType? = null,
    val mag: MagType? = null
)

data class BulletProjectile(
    val id: String = UUID.randomUUID().toString(),
    val shooterId: String,
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Float,
    val maxDistance: Float,
    var distanceTraveled: Float = 0f,
    val isSniper: Boolean = false
)

data class ThrowableProjectile(
    val id: String = UUID.randomUUID().toString(),
    val throwerId: String,
    val type: ThrowableType,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var remainingFuseSec: Float,
    var hasDetonated: Boolean = false
)

data class SmokeCloud(
    val id: String = UUID.randomUUID().toString(),
    val x: Float,
    val y: Float,
    val radius: Float = 150f,
    var remainingDurationSec: Float = 25f
)

data class AirdropCrate(
    val id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    var altitude: Float = 800f,
    var isLanded: Boolean = false,
    var hasBeenLooted: Boolean = false,
    val lootItems: MutableList<GroundLoot> = mutableListOf()
)

data class KillFeedEntry(
    val killerName: String,
    val victimName: String,
    val weaponName: String,
    val isHeadshot: Boolean,
    val isKnockdown: Boolean,
    val timestampMs: Long = System.currentTimeMillis()
)

enum class BotDifficulty(val label: String, val accuracyJitterDeg: Float, val fireCooldownMs: Long, val moveSpeedMul: Float) {
    RECRUIT("Recruit", 16f, 750L, 0.85f),
    TACTICAL("Tactical", 9f, 480L, 1.0f),
    VETERAN("Veteran", 4f, 300L, 1.18f)
}

enum class CharacterPreset(
    val id: String,
    val displayName: String,
    val roleTitle: String,
    val armorRgb: Triple<Float, Float, Float>,
    val accentRgb: Triple<Float, Float, Float>,
    val visorRgb: Triple<Float, Float, Float>
) {
    VORTEX_GHOST(
        id = "vortex_ghost",
        displayName = "Vortex Ghost",
        roleTitle = "Tactical Assault",
        armorRgb = Triple(0.18f, 0.22f, 0.28f),
        accentRgb = Triple(1.0f, 0.48f, 0.0f),
        visorRgb = Triple(1.0f, 0.84f, 0.0f)
    ),
    BLAZE_STRIKER(
        id = "blaze_striker",
        displayName = "Blaze Striker",
        roleTitle = "Frontline Breacher",
        armorRgb = Triple(0.22f, 0.14f, 0.16f),
        accentRgb = Triple(0.95f, 0.16f, 0.22f),
        visorRgb = Triple(1.0f, 0.65f, 0.1f)
    ),
    CYBER_VIPER(
        id = "cyber_viper",
        displayName = "Cyber Viper",
        roleTitle = "Neon Recon",
        armorRgb = Triple(0.12f, 0.18f, 0.28f),
        accentRgb = Triple(0.0f, 0.85f, 1.0f),
        visorRgb = Triple(0.0f, 1.0f, 0.65f)
    ),
    FROST_RECON(
        id = "frost_recon",
        displayName = "Frost Recon",
        roleTitle = "Arctic Marksman",
        armorRgb = Triple(0.75f, 0.82f, 0.88f),
        accentRgb = Triple(0.20f, 0.60f, 0.95f),
        visorRgb = Triple(0.45f, 0.92f, 1.0f)
    ),
    APEX_TITAN(
        id = "apex_titan",
        displayName = "Apex Titan",
        roleTitle = "Heavy Enforcer",
        armorRgb = Triple(0.16f, 0.16f, 0.18f),
        accentRgb = Triple(1.0f, 0.78f, 0.12f),
        visorRgb = Triple(1.0f, 0.35f, 0.1f)
    );

    companion object {
        fun fromId(id: String): CharacterPreset = entries.firstOrNull { it.id == id } ?: VORTEX_GHOST
    }
}

data class DeployableShield(
    val id: String = UUID.randomUUID().toString(),
    val ownerId: String,
    val x: Float,
    val y: Float,
    val angleDegrees: Float,
    var health: Float = 260f,
    val maxHealth: Float = 260f,
    var remainingSec: Float = 16f
)

data class DamagePopup(
    val id: String = UUID.randomUUID().toString(),
    val worldX: Float,
    val worldY: Float,
    val worldZ: Float,
    val amount: Int,
    val isHeadshot: Boolean,
    val isShieldHit: Boolean,
    var ageSec: Float = 0f,
    val durationSec: Float = 0.85f
)

enum class BotBehavior {
    IDLE,
    LOOTING,
    HUNTING,
    COMBAT_ENGAGE,
    TAKING_COVER,
    FLEEING_ZONE,
    DRIVING
}

data class Player(
    val id: String,
    val name: String,
    val isUser: Boolean = false,
    val teamId: Int = 1,
    var x: Float,
    var y: Float,
    var z: Float = 0f,
    var vz: Float = 0f,
    var angle: Float = 0f,
    var targetAngle: Float = 0f,
    var currentSpeed: Float = 0f,
    var isAlive: Boolean = true,
    var isKnocked: Boolean = false,
    var bleedOutTimerSec: Float = 60f,
    var health: Float = 100f,
    var maxHealth: Float = 100f,
    var shield: Float = 100f,
    var maxShield: Float = 100f,
    var ep: Float = 100f,
    var maxEp: Float = 200f,
    var boost: Float = 0f,
    var armorLevel: Int = 0,
    var helmetLevel: Int = 0,
    var backpackLevel: Int = 1,
    var stance: PlayerStance = PlayerStance.STAND,
    var isSprinting: Boolean = false,
    var animWalkPhase: Float = 0f,
    var lastFireTimestampMs: Long = 0L,
    var characterPresetId: String = "vortex_ghost",
    var primaryWeapon1: Weapon? = null,
    var primaryWeapon2: Weapon? = null,
    var pistol: Weapon? = WeaponCatalog.createDesertEagle(),
    var melee: Weapon? = WeaponCatalog.createPan(),
    var activeSlot: Int = 1, // 1: Prim1, 2: Prim2, 3: Pistol, 4: Melee, 5: Throwable
    val ammoInventory: MutableMap<AmmoType, Int> = mutableMapOf(),
    val consumableInventory: MutableMap<ConsumableType, Int> = mutableMapOf(),
    val throwableInventory: MutableMap<ThrowableType, Int> = mutableMapOf(),
    var activeThrowable: ThrowableType = ThrowableType.FRAG_GRENADE,
    var isReloading: Boolean = false,
    var reloadTimerSec: Float = 0f,
    var kills: Int = 0,
    var damageDealt: Float = 0f,
    var vehicleId: String? = null,
    var isDriver: Boolean = false,
    // Bot AI
    var botBehavior: BotBehavior = BotBehavior.LOOTING,
    var botDifficulty: BotDifficulty = BotDifficulty.TACTICAL,
    var botTargetX: Float = 0f,
    var botTargetY: Float = 0f,
    var botTargetPlayerId: String? = null,
    var botLastFireTimeMs: Long = 0L,
    var botReactionDelayMs: Long = 200L
) {
    val activeWeapon: Weapon?
        get() = when (activeSlot) {
            1 -> primaryWeapon1
            2 -> primaryWeapon2
            3 -> pistol
            4 -> melee
            else -> null
        }
}

enum class MatchPhase {
    WAITING_IN_LOBBY,
    PLANE_FLIGHT,
    SKYDIVING,
    ACTIVE_COMBAT,
    SPECTATING,
    MATCH_ENDED
}

data class ZoneState(
    var safeCenterX: Float = 2500f,
    var safeCenterY: Float = 2500f,
    var safeRadius: Float = 2200f,
    var currentBlueCenterX: Float = 2500f,
    var currentBlueCenterY: Float = 2500f,
    var currentBlueRadius: Float = 2600f,
    var targetBlueCenterX: Float = 2500f,
    var targetBlueCenterY: Float = 2500f,
    var targetBlueRadius: Float = 2200f,
    var phase: Int = 1,
    var timeUntilShrinkSec: Float = 90f,
    var isShrinking: Boolean = false,
    var blueDps: Float = 2.5f
)

data class RedZoneState(
    val centerX: Float,
    val centerY: Float,
    val radius: Float = 350f,
    var remainingTimeSec: Float = 45f,
    val explosionPings: MutableList<Pair<Float, Float>> = mutableListOf()
)

enum class GameModeType(val title: String, val subtitle: String, val maxPlayers: Int) {
    CLASSIC_BR("Classic Battle Royale", "100 Players • Survival to the end", 100),
    OFFLINE_PRACTICE("Offline Practice", "Bot Skirmish • Easy/Tactical/Veteran AI", 24),
    TEAM_DEATHMATCH("Team Deathmatch", "Warehouse 4v4 • First to 40 Kills", 8),
    SNIPER_TRAINING("Sniper Training", "Bolt-Action Only • Long-range precision", 20),
    ZOMBIE_SURVIVAL("Zombie Outbreak", "Survive infected hordes & mutant bosses", 16),
    TRAINING_GROUND("Training Simulator", "Target ranges, recoil lab & vehicle tracks", 1),
    CUSTOM_ROOM("Custom Match Room", "Host tournament matches with custom configs", 100)
}
