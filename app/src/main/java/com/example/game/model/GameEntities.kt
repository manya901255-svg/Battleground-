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
    val skinName: String = "Standard Military",
    val upgradeLevel: Int = 1
) {
    val clampedLevel: Int
        get() = upgradeLevel.coerceIn(1, 5)

    val totalCapacity: Int
        get() = maxCapacity + equippedMag.extraCapacity

    val effectiveDamage: Float
        get() = baseDamage * (1f + (clampedLevel - 1) * 0.06f)

    val effectiveFireRateMs: Long
        get() = (fireRateMs * (1f - (clampedLevel - 1) * 0.025f)).toLong().coerceAtLeast(50L)

    val effectiveRecoil: Float
        get() = (baseRecoil * (1f - equippedMuzzle.recoilReduction) * (1f - (clampedLevel - 1) * 0.04f))
            .coerceAtLeast(0.08f)
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

    fun createDesertEagle(skin: String = "Chrome Plated", upgradeLevel: Int = 1): Weapon = Weapon(
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
        activeFireMode = FireMode.SINGLE,
        skinName = skin,
        upgradeLevel = upgradeLevel
    )

    fun createMP40(skin: String = "Tactical Noir"): Weapon = Weapon(
        id = "mp40",
        name = "MP40",
        category = WeaponCategory.SMG,
        ammoType = AmmoType.AMMO_9MM,
        baseDamage = 33f,
        fireRateMs = 74L,
        bulletSpeed = 430f,
        baseRecoil = 0.36f,
        maxCapacity = 28,
        currentAmmo = 28,
        supportedFireModes = listOf(FireMode.FULL_AUTO),
        activeFireMode = FireMode.FULL_AUTO,
        skinName = skin
    )

    fun createM1887(skin: String = "Rapper Underworld"): Weapon = Weapon(
        id = "m1887",
        name = "M1887",
        category = WeaponCategory.SHOTGUN,
        ammoType = AmmoType.AMMO_12GAUGE,
        baseDamage = 28f * 7f,
        fireRateMs = 460L,
        bulletSpeed = 380f,
        baseRecoil = 1.55f,
        maxCapacity = 2,
        currentAmmo = 2,
        supportedFireModes = listOf(FireMode.SINGLE),
        activeFireMode = FireMode.SINGLE,
        skinName = skin
    )

    fun createGatling(skin: String = "Obsidian Vulcan"): Weapon = Weapon(
        id = "gatling",
        name = "Gatling",
        category = WeaponCategory.ASSAULT_RIFLE,
        ammoType = AmmoType.AMMO_556,
        baseDamage = 28f,
        fireRateMs = 55L,
        bulletSpeed = 780f,
        baseRecoil = 0.65f,
        maxCapacity = 100,
        currentAmmo = 100,
        supportedFireModes = listOf(FireMode.FULL_AUTO),
        activeFireMode = FireMode.FULL_AUTO,
        skinName = skin
    )

    fun createPan(skin: String = "Golden Winner"): Weapon = Weapon(
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
        activeFireMode = FireMode.SINGLE,
        skinName = skin
    )

    fun createKatana(skin: String = "Cyber Neon Edge"): Weapon = Weapon(
        id = "katana",
        name = "Katana",
        category = WeaponCategory.MELEE,
        ammoType = AmmoType.NONE,
        baseDamage = 92f,
        fireRateMs = 520L,
        bulletSpeed = 0f,
        baseRecoil = 0f,
        maxCapacity = 0,
        currentAmmo = 0,
        supportedFireModes = listOf(FireMode.SINGLE),
        activeFireMode = FireMode.SINGLE,
        skinName = skin
    )

    fun createBat(skin: String = "Spiked Slugger"): Weapon = Weapon(
        id = "bat",
        name = "Bat",
        category = WeaponCategory.MELEE,
        ammoType = AmmoType.NONE,
        baseDamage = 84f,
        fireRateMs = 620L,
        bulletSpeed = 0f,
        baseRecoil = 0f,
        maxCapacity = 0,
        currentAmmo = 0,
        supportedFireModes = listOf(FireMode.SINGLE),
        activeFireMode = FireMode.SINGLE,
        skinName = skin
    )

    fun createScythe(skin: String = "Crimson Crescent"): Weapon = Weapon(
        id = "scythe",
        name = "Scythe",
        category = WeaponCategory.MELEE,
        ammoType = AmmoType.NONE,
        baseDamage = 96f,
        fireRateMs = 590L,
        bulletSpeed = 0f,
        baseRecoil = 0f,
        maxCapacity = 0,
        currentAmmo = 0,
        supportedFireModes = listOf(FireMode.SINGLE),
        activeFireMode = FireMode.SINGLE,
        skinName = skin
    )

    fun createMachete(skin: String = "Shadow Cleaver"): Weapon = Weapon(
        id = "machete",
        name = "Machete",
        category = WeaponCategory.MELEE,
        ammoType = AmmoType.NONE,
        baseDamage = 88f,
        fireRateMs = 560L,
        bulletSpeed = 0f,
        baseRecoil = 0f,
        maxCapacity = 0,
        currentAmmo = 0,
        supportedFireModes = listOf(FireMode.SINGLE),
        activeFireMode = FireMode.SINGLE,
        skinName = skin
    )

    fun createById(id: String, skin: String? = null, upgradeLevel: Int = 1): Weapon {
        val base = when (id.lowercase()) {
            "akm" -> createAKM(skin ?: "Dragonfire")
            "awm" -> createAWM(skin ?: "Cyber Gold")
            "kar98k" -> createKar98k(skin ?: "Vintage Walnut")
            "ump45" -> createUMP45(skin ?: "Arctic Camo")
            "mp40" -> createMP40(skin ?: "Tactical Noir")
            "s12k" -> createS12K(skin ?: "Breacher Steel")
            "m1887" -> createM1887(skin ?: "Rapper Underworld")
            "gatling" -> createGatling(skin ?: "Obsidian Vulcan")
            "deagle" -> createDesertEagle(skin ?: "Chrome Plated", upgradeLevel)
            "pan" -> createPan(skin ?: "Golden Winner")
            "katana" -> createKatana(skin ?: "Cyber Neon Edge")
            "bat" -> createBat(skin ?: "Spiked Slugger")
            "scythe" -> createScythe(skin ?: "Crimson Crescent")
            "machete" -> createMachete(skin ?: "Shadow Cleaver")
            else -> createM416(skin ?: "Tactical Slate")
        }
        return base.copy(upgradeLevel = upgradeLevel.coerceIn(1, 5))
    }

    fun primaryFirearms(): List<Weapon> = listOf(
        createM416(),
        createAKM(),
        createMP40(),
        createUMP45(),
        createM1887(),
        createS12K(),
        createAWM(),
        createKar98k(),
        createGatling(),
        createDesertEagle()
    )

    fun meleeWeapons(): List<Weapon> = listOf(
        createKatana(),
        createScythe(),
        createBat(),
        createMachete(),
        createPan()
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

enum class WeatherType(val id: String, val displayName: String) {
    CLEAR("CLEAR", "Clear Daylight"),
    SUNSET_STORM("SUNSET_STORM", "Sunset Storm"),
    DENSE_FOG("DENSE_FOG", "Tactical Fog"),
    NIGHT_OPS("NIGHT_OPS", "Night Ops")
}

enum class EmotePreset(val id: String, val label: String, val badge: String, val rarity: String = "RARE") {
    SALUTE("salute", "Tactical Salute", "🫡", "COMMON"),
    VICTORY_DANCE("victory", "Booyah Groove", "🏆", "EPIC"),
    RESPECT("respect", "Operative Honor", "🤝", "RARE"),
    SHIELD_FLEX("flex", "Titan Flex", "🛡️", "EPIC"),
    SCAN_PING("scan", "Radar Lock", "📡", "RARE"),
    CHAMPION_ROAR("roar", "Apex Battle Cry", "🔥", "LEGENDARY"),
    BACKFLIP("backflip", "Acro Backflip", "🤸", "EPIC"),
    THRONE_SIT("throne", "King's Throne", "👑", "MYTHIC"),
    HIGH_KICK("high_kick", "Dragon High Kick", "🥋", "LEGENDARY"),
    DJ_MIX("dj_mix", "Neon DJ Drop", "🎧", "EPIC"),
    DAB("dab", "Winner Dab", "⚡", "RARE"),
    KUNGFU_STANCE("kungfu", "Master Stance", "🐉", "LEGENDARY")
}

enum class RankTierBadge(
    val title: String,
    val minRp: Int,
    val primaryHex: Long,
    val secondaryHex: Long,
    val accentHex: Long,
    val divisionCount: Int = 3
) {
    BRONZE("Bronze", 1000, 0xFFCD7F32, 0xFF7B4318, 0xFFFFB074),
    SILVER("Silver", 1500, 0xFFB8C7D6, 0xFF5D6B7A, 0xFFEAF2FA),
    GOLD("Gold", 2000, 0xFFFFC107, 0xFF8E5C00, 0xFFFFEB84, 4),
    PLATINUM("Platinum", 2600, 0xFF26D6DA, 0xFF0E6873, 0xFFA2F7FA, 4),
    DIAMOND("Diamond", 3200, 0xFF6B7CFF, 0xFF2B328C, 0xFFBCC5FF, 4),
    HEROIC("Heroic", 3900, 0xFFFF2A3C, 0xFF7D0A18, 0xFFFFC107, 1),
    GRANDMASTER("Grandmaster", 4600, 0xFFFF1744, 0xFFFFC400, 0xFFFFFFFF, 1);

    companion object {
        fun fromRp(rp: Int): RankTierBadge = entries.lastOrNull { rp >= it.minRp } ?: BRONZE

        fun formattedRankLabel(rp: Int): String {
            val tier = fromRp(rp)
            if (tier.divisionCount == 1) return tier.title
            val nextMin = entries.getOrNull(entries.indexOf(tier) + 1)?.minRp ?: (tier.minRp + 800)
            val span = (nextMin - tier.minRp).coerceAtLeast(100)
            val rel = ((rp - tier.minRp).coerceIn(0, span - 1) * tier.divisionCount) / span
            val roman = when (tier.divisionCount - rel) {
                4 -> "IV"
                3 -> "III"
                2 -> "II"
                else -> "I"
            }
            return "${tier.title} $roman"
        }
    }
}

enum class CharacterPreset(
    val id: String,
    val displayName: String,
    val gender: String,
    val roleTitle: String,
    val activeSkillName: String,
    val activeSkillDesc: String,
    val passiveSkillName: String,
    val passiveSkillDesc: String,
    val armorRgb: Triple<Float, Float, Float>,
    val accentRgb: Triple<Float, Float, Float>,
    val visorRgb: Triple<Float, Float, Float>,
    val starterUnlocked: Boolean = false,
    val unlockBp: Int = 0,
    val unlockCredits: Int = 0,
    val mobilityStat: Int = 80,
    val combatStat: Int = 80,
    val survivalStat: Int = 80,
    val styleTag: String = "TACTICAL"
) {
    VORTEX_GHOST(
        id = "vortex_ghost",
        displayName = "WarX Ghost",
        gender = "Male",
        roleTitle = "WarX Crimson Striker",
        activeSkillName = "Overdrive Dash",
        activeSkillDesc = "+32% sprint burst for 5s",
        passiveSkillName = "Tactical Steady",
        passiveSkillDesc = "-10% weapon recoil dispersion",
        armorRgb = Triple(0.10f, 0.10f, 0.13f),
        accentRgb = Triple(0.94f, 0.11f, 0.18f),
        visorRgb = Triple(1.0f, 0.82f, 0.12f),
        starterUnlocked = true,
        mobilityStat = 88,
        combatStat = 90,
        survivalStat = 78,
        styleTag = "HALF_MASK"
    ),
    NOVA_VALKYRIE(
        id = "nova_valkyrie",
        displayName = "WarX Valkyrie",
        gender = "Female",
        roleTitle = "WarX Crimson Blade",
        activeSkillName = "Bio-Nano Pulse",
        activeSkillDesc = "Restores +20 HP & +45 EP immediately",
        passiveSkillName = "Buzzer Beater",
        passiveSkillDesc = "Recovers +15 HP after eliminating an enemy",
        armorRgb = Triple(0.12f, 0.09f, 0.12f),
        accentRgb = Triple(0.92f, 0.12f, 0.20f),
        visorRgb = Triple(1.0f, 0.82f, 0.14f),
        starterUnlocked = true,
        mobilityStat = 86,
        combatStat = 76,
        survivalStat = 94,
        styleTag = "PONYTAIL"
    ),
    BLAZE_STRIKER(
        id = "blaze_striker",
        displayName = "Blaze Striker",
        gender = "Male",
        roleTitle = "Frontline Breacher",
        activeSkillName = "Thermal Surge",
        activeSkillDesc = "+15% SMG & Shotgun close-range punch",
        passiveSkillName = "Armor Forged",
        passiveSkillDesc = "+10 Max Tactical Shield",
        armorRgb = Triple(0.22f, 0.14f, 0.16f),
        accentRgb = Triple(0.95f, 0.16f, 0.22f),
        visorRgb = Triple(1.0f, 0.65f, 0.1f),
        starterUnlocked = false,
        unlockBp = 6000,
        mobilityStat = 78,
        combatStat = 94,
        survivalStat = 86,
        styleTag = "HEAVY_JACKET"
    ),
    CYBER_VIPER(
        id = "cyber_viper",
        displayName = "Cyber Viper",
        gender = "Female",
        roleTitle = "Neon Recon",
        activeSkillName = "Spectre Scan",
        activeSkillDesc = "Reveals nearby hostiles on minimap",
        passiveSkillName = "Silent Step",
        passiveSkillDesc = "+8% crouch & flank agility",
        armorRgb = Triple(0.12f, 0.18f, 0.28f),
        accentRgb = Triple(0.0f, 0.85f, 1.0f),
        visorRgb = Triple(0.0f, 1.0f, 0.65f),
        starterUnlocked = false,
        unlockBp = 7500,
        mobilityStat = 95,
        combatStat = 84,
        survivalStat = 80,
        styleTag = "VISOR"
    ),
    FROST_RECON(
        id = "frost_recon",
        displayName = "Frost Recon",
        gender = "Male",
        roleTitle = "Arctic Marksman",
        activeSkillName = "Ballistic Lock",
        activeSkillDesc = "Stabilizes 4x/8x sniper sway",
        passiveSkillName = "Cold Blooded",
        passiveSkillDesc = "+12% headshot critical multiplier",
        armorRgb = Triple(0.75f, 0.82f, 0.88f),
        accentRgb = Triple(0.20f, 0.60f, 0.95f),
        visorRgb = Triple(0.45f, 0.92f, 1.0f),
        starterUnlocked = false,
        unlockCredits = 480,
        mobilityStat = 82,
        combatStat = 96,
        survivalStat = 76,
        styleTag = "VISOR"
    ),
    SERAPH_ECHO(
        id = "seraph_echo",
        displayName = "Seraph Echo",
        gender = "Female",
        roleTitle = "Aegis Defender",
        activeSkillName = "Aegis Barrier",
        activeSkillDesc = "Deploys reinforced directional cover",
        passiveSkillName = "Fast Hands",
        passiveSkillDesc = "+12% faster reload speed",
        armorRgb = Triple(0.84f, 0.86f, 0.90f),
        accentRgb = Triple(1.0f, 0.72f, 0.18f),
        visorRgb = Triple(0.25f, 0.85f, 1.0f),
        starterUnlocked = false,
        unlockCredits = 550,
        mobilityStat = 84,
        combatStat = 86,
        survivalStat = 92,
        styleTag = "PONYTAIL"
    ),
    APEX_TITAN(
        id = "apex_titan",
        displayName = "Apex Titan",
        gender = "Male",
        roleTitle = "Heavy Enforcer",
        activeSkillName = "Gloo Bastion",
        activeSkillDesc = "Hardens shield durability by 20%",
        passiveSkillName = "Deep Pockets",
        passiveSkillDesc = "+25% backpack supply capacity",
        armorRgb = Triple(0.16f, 0.16f, 0.18f),
        accentRgb = Triple(1.0f, 0.78f, 0.12f),
        visorRgb = Triple(1.0f, 0.35f, 0.1f),
        starterUnlocked = false,
        unlockCredits = 680,
        mobilityStat = 72,
        combatStat = 90,
        survivalStat = 98,
        styleTag = "HEAVY_JACKET"
    ),
    KITSUNE_SHRINE(
        id = "kitsune_shrine",
        displayName = "Kitsune Miko",
        gender = "Female",
        roleTitle = "Crimson Shrine Fox",
        activeSkillName = "Foxfire Surge",
        activeSkillDesc = "+24% move speed & +15 EP burst",
        passiveSkillName = "Spirit Evasion",
        passiveSkillDesc = "+10% agile crouch sprint",
        armorRgb = Triple(0.92f, 0.90f, 0.92f),
        accentRgb = Triple(0.94f, 0.12f, 0.22f),
        visorRgb = Triple(1.0f, 0.76f, 0.16f),
        starterUnlocked = false,
        unlockBp = 6800,
        mobilityStat = 94,
        combatStat = 85,
        survivalStat = 84,
        styleTag = "KITSUNE"
    ),
    FLAME_SKULL(
        id = "flame_skull",
        displayName = "Inferno Skull",
        gender = "Male",
        roleTitle = "Ember Hood Revenant",
        activeSkillName = "Hellfire Overload",
        activeSkillDesc = "+18% AR & Shotgun close punch",
        passiveSkillName = "Cinder Ward",
        passiveSkillDesc = "+10 Max Tactical Shield",
        armorRgb = Triple(0.14f, 0.08f, 0.08f),
        accentRgb = Triple(1.0f, 0.38f, 0.06f),
        visorRgb = Triple(1.0f, 0.78f, 0.15f),
        starterUnlocked = false,
        unlockCredits = 720,
        mobilityStat = 84,
        combatStat = 96,
        survivalStat = 86,
        styleTag = "FLAME_HOOD"
    ),
    NEON_SAMURAI(
        id = "neon_samurai",
        displayName = "Neon Ronin",
        gender = "Male",
        roleTitle = "Cyber Shogun",
        activeSkillName = "Blade Blink",
        activeSkillDesc = "+30% dash & katana strike speed",
        passiveSkillName = "Bushido Edge",
        passiveSkillDesc = "+25% melee damage multiplier",
        armorRgb = Triple(0.16f, 0.08f, 0.26f),
        accentRgb = Triple(0.0f, 0.90f, 1.0f),
        visorRgb = Triple(1.0f, 0.16f, 0.65f),
        starterUnlocked = false,
        unlockCredits = 780,
        mobilityStat = 92,
        combatStat = 93,
        survivalStat = 82,
        styleTag = "SAMURAI_CLOAK"
    ),
    CYBER_AFRO(
        id = "cyber_afro",
        displayName = "Volt Groove",
        gender = "Male",
        roleTitle = "Neon Street Punk",
        activeSkillName = "Arc Pulse",
        activeSkillDesc = "Restores +35 EP & speed aura",
        passiveSkillName = "Rhythm Reload",
        passiveSkillDesc = "+14% SMG fire stability",
        armorRgb = Triple(0.86f, 0.88f, 0.12f),
        accentRgb = Triple(0.12f, 0.85f, 1.0f),
        visorRgb = Triple(1.0f, 0.25f, 0.68f),
        starterUnlocked = false,
        unlockBp = 5800,
        mobilityStat = 93,
        combatStat = 86,
        survivalStat = 80,
        styleTag = "CYBER_AFRO"
    ),
    ROYAL_COMMANDER(
        id = "royal_commander",
        displayName = "Sovereign Gold",
        gender = "Male",
        roleTitle = "Royal Vanguard Lord",
        activeSkillName = "Royal Decree",
        activeSkillDesc = "Deploys reinforced gold Gloo wall",
        passiveSkillName = "Sovereign Plate",
        passiveSkillDesc = "+10 Max Shield & +10% reload",
        armorRgb = Triple(0.90f, 0.90f, 0.92f),
        accentRgb = Triple(0.98f, 0.76f, 0.16f),
        visorRgb = Triple(1.0f, 0.92f, 0.45f),
        starterUnlocked = false,
        unlockCredits = 890,
        mobilityStat = 80,
        combatStat = 90,
        survivalStat = 95,
        styleTag = "CROWN_COAT"
    ),
    STREET_MAFIA(
        id = "street_mafia",
        displayName = "Don Crimson",
        gender = "Male",
        roleTitle = "Ivory Syndicate",
        activeSkillName = "Syndicate Focus",
        activeSkillDesc = "+15% precision sniper & rifle accuracy",
        passiveSkillName = "Cold Resolve",
        passiveSkillDesc = "+12 HP siphon on knockout",
        armorRgb = Triple(0.86f, 0.86f, 0.88f),
        accentRgb = Triple(0.75f, 0.10f, 0.18f),
        visorRgb = Triple(0.18f, 0.18f, 0.20f),
        starterUnlocked = false,
        unlockBp = 8200,
        mobilityStat = 85,
        combatStat = 91,
        survivalStat = 85,
        styleTag = "MAFIA_CAPE"
    ),
    VIOLET_BLADE(
        id = "violet_blade",
        displayName = "Violet Phantom",
        gender = "Female",
        roleTitle = "Amethyst Assassin",
        activeSkillName = "Phase Shroud",
        activeSkillDesc = "Radar cloaking & +28% sprint burst",
        passiveSkillName = "Shadow Reflex",
        passiveSkillDesc = "+15% headshot critical focus",
        armorRgb = Triple(0.24f, 0.12f, 0.36f),
        accentRgb = Triple(0.78f, 0.32f, 1.0f),
        visorRgb = Triple(0.0f, 0.95f, 1.0f),
        starterUnlocked = false,
        unlockCredits = 690,
        mobilityStat = 96,
        combatStat = 90,
        survivalStat = 82,
        styleTag = "HALF_MASK"
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
    var remainingSec: Float = 16f,
    val skinId: String = "gloo_cyber"
)

data class WarriorLootTrap(
    val id: String = UUID.randomUUID().toString(),
    val x: Float,
    val y: Float,
    val victimName: String,
    var fuseRemainingSec: Float = 9.0f,
    val totalFuseSec: Float = 9.0f,
    var isLooted: Boolean = false,
    var isDetonated: Boolean = false,
    val rewardWeapon: Weapon? = null,
    val rewardArmorLevel: Int = 4,
    val rewardHelmetLevel: Int = 4
)

data class ExplosiveBarrel(
    val id: String = UUID.randomUUID().toString(),
    val x: Float,
    val y: Float,
    var health: Float = 60f,
    val maxHealth: Float = 60f,
    var isExploded: Boolean = false
)

enum class PetPreset(
    val id: String,
    val displayName: String,
    val skillTitle: String,
    val skillDesc: String,
    val bodyRgb: Triple<Float, Float, Float>,
    val glowRgb: Triple<Float, Float, Float>,
    val modelShape: String = "HOVER_DRONE"
) {
    DRACO_FLAME(
        id = "draco_flame",
        displayName = "Draco Flame",
        skillTitle = "Dragon Ember",
        skillDesc = "+15 EP gained per elimination",
        bodyRgb = Triple(0.58f, 0.16f, 0.42f),
        glowRgb = Triple(1.0f, 0.42f, 0.08f),
        modelShape = "DRAGON"
    ),
    CYBER_PUP(
        id = "cyber_pup",
        displayName = "Cyber Pup",
        skillTitle = "Tactical Sense",
        skillDesc = "Extended Pulse radar range",
        bodyRgb = Triple(0.18f, 0.42f, 0.72f),
        glowRgb = Triple(0.0f, 0.90f, 1.0f),
        modelShape = "CANINE"
    ),
    FROST_OWL(
        id = "frost_owl",
        displayName = "Frost Owl",
        skillTitle = "Glacial Ward",
        skillDesc = "+15 Max Tactical Shield capacity",
        bodyRgb = Triple(0.75f, 0.88f, 0.96f),
        glowRgb = Triple(0.35f, 0.85f, 1.0f),
        modelShape = "OWL"
    ),
    VORTEX_ROBO(
        id = "vortex_robo",
        displayName = "Vortex Robo",
        skillTitle = "Overclocked Mag",
        skillDesc = "+15% faster weapon reload speed",
        bodyRgb = Triple(0.24f, 0.26f, 0.30f),
        glowRgb = Triple(1.0f, 0.84f, 0.0f),
        modelShape = "HOVER_DRONE"
    ),
    PANDA_DETECTIVE(
        id = "panda_detective",
        displayName = "Detective Panda",
        skillTitle = "Panda's Blessings",
        skillDesc = "Restores +12 HP on every elimination",
        bodyRgb = Triple(0.95f, 0.95f, 0.97f),
        glowRgb = Triple(0.18f, 0.82f, 0.48f),
        modelShape = "PANDA"
    ),
    CYBER_ORBIT(
        id = "cyber_orbit",
        displayName = "Neko Cyber Orb",
        skillTitle = "Orbital Resonance",
        skillDesc = "Reduces Tactical Shield cooldown by 20%",
        bodyRgb = Triple(0.18f, 0.19f, 0.24f),
        glowRgb = Triple(0.0f, 0.95f, 1.0f),
        modelShape = "CYBER_ORB"
    ),
    WAGGOR_PENGUIN(
        id = "waggor_penguin",
        displayName = "Mr. Waggor",
        skillTitle = "Smooth Gloo",
        skillDesc = "Generates passive Gloo Shield recharge",
        bodyRgb = Triple(0.12f, 0.15f, 0.22f),
        glowRgb = Triple(0.20f, 0.88f, 1.0f),
        modelShape = "PENGUIN"
    );

    companion object {
        fun fromId(id: String): PetPreset = entries.firstOrNull { it.id == id } ?: DRACO_FLAME
    }
}

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
    var landingRecoverySec: Float = 0f,
    var climbVaultTimerSec: Float = 0f,
    var emoteAnimSec: Float = 0f,
    var isInWater: Boolean = false,
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
    var assists: Int = 0,
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
    CLASSIC_BR("Classic Battle Royale", "100 Players • Solo / Duo / Squad survival", 100),
    RANKED_BR("Ranked Battle Royale", "Season 54 • Competitive RP survival", 100),
    CLASH_SQUAD("Clash Squad 4v4", "Fast round-based tactical 4v4 arena", 8),
    RANKED_CLASH_SQUAD("Ranked Clash Squad", "Competitive 4v4 squad elimination", 8),
    OFFLINE_PRACTICE("Offline Practice", "Bot Skirmish • Easy/Tactical/Veteran AI", 24),
    TEAM_DEATHMATCH("Team Deathmatch", "Warehouse 4v4 • First to 40 Kills", 8),
    SNIPER_TRAINING("Sniper Training", "Bolt-Action Only • Long-range precision", 20),
    LIMITED_TIME_BLITZ("Blitz Hot-Drop (LTM)", "Ultra-fast shrinking zones & high-tier drops", 32),
    ZOMBIE_SURVIVAL("Zombie Outbreak", "Survive infected hordes & mutant bosses", 16),
    TRAINING_GROUND("Training Simulator", "Target ranges, recoil lab & vehicle tracks", 1),
    CUSTOM_ROOM("Custom Match Room", "Host tournament matches with custom configs", 100)
}
