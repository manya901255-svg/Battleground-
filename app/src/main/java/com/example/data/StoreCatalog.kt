package com.example.data

import com.example.game.model.CharacterPreset
import com.example.game.model.EmotePreset
import com.example.game.model.PetPreset

enum class StoreCategory(val title: String, val shortLabel: String, val itemType: String) {
    CHARACTER_STORE("Character Store", "Characters", "CHARACTER"),
    WEAPON_SKINS("Weapon Skins", "Gun Skins", "WEAPON_SKIN"),
    GUN_COLLECTION("Gun Collection", "Guns", "GUN"),
    OUTFIT_COLLECTION("Outfit Collection", "Outfits", "OUTFIT"),
    BACKPACK_SKINS("Backpack Skins", "Backpacks", "BACKPACK"),
    HELMET_SKINS("Helmet Skins", "Helmets", "HELMET"),
    PARACHUTE_SKINS("Parachute Skins", "Parachutes", "PARACHUTE"),
    VEHICLE_SKINS("Vehicle Skins", "Vehicles", "VEHICLE"),
    EMOTES("Emotes", "Emotes", "EMOTE"),
    PET_COLLECTION("Pet Collection", "Pets", "PET"),
    PREMIUM_BUNDLES("Premium Bundles", "Bundles", "BUNDLE")
}

data class CatalogItemSpec(
    val id: String,
    val name: String,
    val category: StoreCategory,
    val type: String,
    val targetItemId: String,
    val rarity: String,
    val priceBp: Int = 0,
    val priceCredits: Int = 0,
    val description: String,
    val starterOwned: Boolean = false,
    val starterEquipped: Boolean = false,
    val accentHex: Long = 0xFFFF6E1A
) {
    fun toEntity(): InventoryItemEntity = InventoryItemEntity(
        id = id,
        name = name,
        type = type,
        targetItemId = targetItemId,
        rarity = rarity,
        isEquipped = starterEquipped,
        isOwned = starterOwned,
        priceBp = priceBp,
        priceCredits = priceCredits,
        upgradeLevel = 1
    )
}

object StoreCatalog {

    val allCatalogItems: List<CatalogItemSpec> = listOf(
        // 1. CHARACTER STORE
        CatalogItemSpec(
            id = "char_vortex_ghost",
            name = CharacterPreset.VORTEX_GHOST.displayName,
            category = StoreCategory.CHARACTER_STORE,
            type = "CHARACTER",
            targetItemId = CharacterPreset.VORTEX_GHOST.id,
            rarity = "EPIC",
            priceBp = 0,
            description = "${CharacterPreset.VORTEX_GHOST.roleTitle} • ${CharacterPreset.VORTEX_GHOST.activeSkillName}: ${CharacterPreset.VORTEX_GHOST.activeSkillDesc}",
            starterOwned = true,
            starterEquipped = true,
            accentHex = 0xFFFF7A00
        ),
        CatalogItemSpec(
            id = "char_nova_valkyrie",
            name = CharacterPreset.NOVA_VALKYRIE.displayName,
            category = StoreCategory.CHARACTER_STORE,
            type = "CHARACTER",
            targetItemId = CharacterPreset.NOVA_VALKYRIE.id,
            rarity = "EPIC",
            priceBp = 0,
            description = "${CharacterPreset.NOVA_VALKYRIE.roleTitle} • ${CharacterPreset.NOVA_VALKYRIE.activeSkillName}: ${CharacterPreset.NOVA_VALKYRIE.activeSkillDesc}",
            starterOwned = true,
            accentHex = 0xFFF552B8
        ),
        CatalogItemSpec(
            id = "char_blaze_striker",
            name = CharacterPreset.BLAZE_STRIKER.displayName,
            category = StoreCategory.CHARACTER_STORE,
            type = "CHARACTER",
            targetItemId = CharacterPreset.BLAZE_STRIKER.id,
            rarity = "LEGENDARY",
            priceBp = CharacterPreset.BLAZE_STRIKER.unlockBp,
            description = "${CharacterPreset.BLAZE_STRIKER.roleTitle} • ${CharacterPreset.BLAZE_STRIKER.activeSkillName}: ${CharacterPreset.BLAZE_STRIKER.activeSkillDesc}",
            accentHex = 0xFFF22938
        ),
        CatalogItemSpec(
            id = "char_cyber_viper",
            name = CharacterPreset.CYBER_VIPER.displayName,
            category = StoreCategory.CHARACTER_STORE,
            type = "CHARACTER",
            targetItemId = CharacterPreset.CYBER_VIPER.id,
            rarity = "LEGENDARY",
            priceBp = CharacterPreset.CYBER_VIPER.unlockBp,
            description = "${CharacterPreset.CYBER_VIPER.roleTitle} • ${CharacterPreset.CYBER_VIPER.activeSkillName}: ${CharacterPreset.CYBER_VIPER.activeSkillDesc}",
            accentHex = 0xFF00D9FF
        ),
        CatalogItemSpec(
            id = "char_frost_recon",
            name = CharacterPreset.FROST_RECON.displayName,
            category = StoreCategory.CHARACTER_STORE,
            type = "CHARACTER",
            targetItemId = CharacterPreset.FROST_RECON.id,
            rarity = "LEGENDARY",
            priceCredits = CharacterPreset.FROST_RECON.unlockCredits,
            description = "${CharacterPreset.FROST_RECON.roleTitle} • ${CharacterPreset.FROST_RECON.activeSkillName}: ${CharacterPreset.FROST_RECON.activeSkillDesc}",
            accentHex = 0xFF3399F2
        ),
        CatalogItemSpec(
            id = "char_seraph_echo",
            name = CharacterPreset.SERAPH_ECHO.displayName,
            category = StoreCategory.CHARACTER_STORE,
            type = "CHARACTER",
            targetItemId = CharacterPreset.SERAPH_ECHO.id,
            rarity = "MYTHIC",
            priceCredits = CharacterPreset.SERAPH_ECHO.unlockCredits,
            description = "${CharacterPreset.SERAPH_ECHO.roleTitle} • ${CharacterPreset.SERAPH_ECHO.activeSkillName}: ${CharacterPreset.SERAPH_ECHO.activeSkillDesc}",
            accentHex = 0xFFFFB82E
        ),
        CatalogItemSpec(
            id = "char_apex_titan",
            name = CharacterPreset.APEX_TITAN.displayName,
            category = StoreCategory.CHARACTER_STORE,
            type = "CHARACTER",
            targetItemId = CharacterPreset.APEX_TITAN.id,
            rarity = "MYTHIC",
            priceCredits = CharacterPreset.APEX_TITAN.unlockCredits,
            description = "${CharacterPreset.APEX_TITAN.roleTitle} • ${CharacterPreset.APEX_TITAN.activeSkillName}: ${CharacterPreset.APEX_TITAN.activeSkillDesc}",
            accentHex = 0xFFFFC71F
        ),

        // 2. WEAPON SKINS
        CatalogItemSpec(
            id = "skin_m416_slate",
            name = "M416 - Tactical Slate",
            category = StoreCategory.WEAPON_SKINS,
            type = "WEAPON_SKIN",
            targetItemId = "m416",
            rarity = "COMMON",
            priceBp = 1500,
            description = "Matte tungsten-slate finish with reinforced Picatinny rail.",
            starterOwned = true,
            starterEquipped = true,
            accentHex = 0xFF607D8B
        ),
        CatalogItemSpec(
            id = "skin_m416_glacier",
            name = "M416 - Glacier Frost",
            category = StoreCategory.WEAPON_SKINS,
            type = "WEAPON_SKIN",
            targetItemId = "m416",
            rarity = "MYTHIC",
            priceCredits = 1200,
            description = "Crystalline cryo alloy barrel with icy kill-effect shimmer.",
            accentHex = 0xFF00E5FF
        ),
        CatalogItemSpec(
            id = "skin_akm_dragon",
            name = "AKM - Dragonfire Inferno",
            category = StoreCategory.WEAPON_SKINS,
            type = "WEAPON_SKIN",
            targetItemId = "akm",
            rarity = "LEGENDARY",
            priceBp = 5500,
            description = "Molten ember engravings along a forged steel receiver.",
            starterOwned = true,
            starterEquipped = true,
            accentHex = 0xFFFF5252
        ),
        CatalogItemSpec(
            id = "skin_awm_cyber",
            name = "AWM - Cyber Gold",
            category = StoreCategory.WEAPON_SKINS,
            type = "WEAPON_SKIN",
            targetItemId = "awm",
            rarity = "MYTHIC",
            priceCredits = 900,
            description = "Anodized 24k gold ballistic chassis with carbon-weave barrel.",
            starterOwned = true,
            starterEquipped = true,
            accentHex = 0xFFFFD54F
        ),
        CatalogItemSpec(
            id = "skin_awm_nova",
            name = "AWM - Solar Flare Direct Pack",
            category = StoreCategory.WEAPON_SKINS,
            type = "WEAPON_SKIN",
            targetItemId = "awm",
            rarity = "LEGENDARY",
            priceBp = 9200,
            description = "High-contrast solar orange shroud tuned for desert operations.",
            accentHex = 0xFFFF9100
        ),
        CatalogItemSpec(
            id = "skin_ump45_neon",
            name = "UMP45 - Neon Pulse",
            category = StoreCategory.WEAPON_SKINS,
            type = "WEAPON_SKIN",
            targetItemId = "ump45",
            rarity = "EPIC",
            priceBp = 4800,
            description = "Electric cyan street-ops SMG finish with luminous iron sights.",
            accentHex = 0xFF18FFFF
        ),
        CatalogItemSpec(
            id = "skin_s12k_crimson",
            name = "S12K - Crimson Breach",
            category = StoreCategory.WEAPON_SKINS,
            type = "WEAPON_SKIN",
            targetItemId = "s12k",
            rarity = "EPIC",
            priceBp = 5200,
            description = "Close-quarters breacher coating with hazard stripes.",
            accentHex = 0xFFE53935
        ),
        CatalogItemSpec(
            id = "skin_kar98k_royal",
            name = "Kar98k - Imperial Sovereign",
            category = StoreCategory.WEAPON_SKINS,
            type = "WEAPON_SKIN",
            targetItemId = "kar98k",
            rarity = "LEGENDARY",
            priceCredits = 620,
            description = "Polished walnut and engraved brass bolt-action receiver.",
            accentHex = 0xFFFFCA28
        ),
        CatalogItemSpec(
            id = "skin_deagle_cobalt",
            name = "Desert Eagle - Cobalt Strike",
            category = StoreCategory.WEAPON_SKINS,
            type = "WEAPON_SKIN",
            targetItemId = "deagle",
            rarity = "RARE",
            priceBp = 3400,
            description = "Deep cobalt-anodized slide and custom tactical grip panels.",
            accentHex = 0xFF448AFF
        ),
        CatalogItemSpec(
            id = "skin_pan_gold",
            name = "Golden Pan Supreme",
            category = StoreCategory.WEAPON_SKINS,
            type = "WEAPON_SKIN",
            targetItemId = "pan",
            rarity = "EPIC",
            priceBp = 8000,
            description = "Bullet-deflecting forged championship skillet in mirror gold.",
            starterOwned = true,
            starterEquipped = true,
            accentHex = 0xFFFFD700
        ),

        // 3. GUN COLLECTION (Unlockable & Upgradable Primary / Secondary Blueprints)
        CatalogItemSpec(
            id = "gun_m416",
            name = "M416 Tactical Rifle",
            category = StoreCategory.GUN_COLLECTION,
            type = "GUN",
            targetItemId = "m416",
            rarity = "RARE",
            priceBp = 0,
            description = "Versatile 5.56mm Assault Rifle with balanced recoil & rapid handling.",
            starterOwned = true,
            starterEquipped = true,
            accentHex = 0xFF4FC3F7
        ),
        CatalogItemSpec(
            id = "gun_akm",
            name = "AKM Heavy Carbine",
            category = StoreCategory.GUN_COLLECTION,
            type = "GUN",
            targetItemId = "akm",
            rarity = "EPIC",
            priceBp = 4200,
            description = "High-caliber 7.62mm Assault Rifle packing devastating stopping power.",
            starterOwned = true,
            accentHex = 0xFFFF7043
        ),
        CatalogItemSpec(
            id = "gun_ump45",
            name = "UMP45 Spec-Ops SMG",
            category = StoreCategory.GUN_COLLECTION,
            type = "GUN",
            targetItemId = "ump45",
            rarity = "RARE",
            priceBp = 3200,
            description = "Agile .45 ACP Submachine Gun built for urban rushing.",
            starterOwned = true,
            accentHex = 0xFF26C6DA
        ),
        CatalogItemSpec(
            id = "gun_s12k",
            name = "S12K Auto Shotgun",
            category = StoreCategory.GUN_COLLECTION,
            type = "GUN",
            targetItemId = "s12k",
            rarity = "EPIC",
            priceBp = 4600,
            description = "Semi-automatic 12-Gauge Shotgun capable of clearing squads indoors.",
            accentHex = 0xFFEF5350
        ),
        CatalogItemSpec(
            id = "gun_kar98k",
            name = "Kar98k Marksman Bolt",
            category = StoreCategory.GUN_COLLECTION,
            type = "GUN",
            targetItemId = "kar98k",
            rarity = "EPIC",
            priceBp = 5800,
            description = "Precision 7.62mm Sniper Rifle with factory 4x optics.",
            accentHex = 0xFFFFB74D
        ),
        CatalogItemSpec(
            id = "gun_awm",
            name = "AWM Magnum Precision",
            category = StoreCategory.GUN_COLLECTION,
            type = "GUN",
            targetItemId = "awm",
            rarity = "MYTHIC",
            priceCredits = 750,
            description = "Elite .300 Magnum Anti-Materiel Sniper Rifle with 8x optic.",
            accentHex = 0xFFFF4081
        ),
        CatalogItemSpec(
            id = "gun_deagle",
            name = "Desert Eagle .45 Handcannon",
            category = StoreCategory.GUN_COLLECTION,
            type = "GUN",
            targetItemId = "deagle",
            rarity = "RARE",
            priceBp = 2600,
            description = "High-impact semi-auto pistol sidearm with armor punch.",
            starterOwned = true,
            accentHex = 0xFFB0BEC5
        ),

        // 4. OUTFIT COLLECTION
        CatalogItemSpec(
            id = "outfit_operative",
            name = "Stealth Operative Suit",
            category = StoreCategory.OUTFIT_COLLECTION,
            type = "OUTFIT",
            targetItemId = "player",
            rarity = "RARE",
            priceBp = 2000,
            description = "Matte black Kevlar tactical jacket and cargo combat trousers.",
            starterOwned = true,
            starterEquipped = true,
            accentHex = 0xFF455A64
        ),
        CatalogItemSpec(
            id = "outfit_ghillie",
            name = "Full Arctic Ghillie Suit",
            category = StoreCategory.OUTFIT_COLLECTION,
            type = "OUTFIT",
            targetItemId = "player",
            rarity = "EPIC",
            priceBp = 4800,
            description = "Camouflaged winter sniper cloak designed for highland ridges.",
            starterOwned = true,
            accentHex = 0xFFCFD8DC
        ),
        CatalogItemSpec(
            id = "outfit_desert_spec",
            name = "Desert Spec-Ops Camo",
            category = StoreCategory.OUTFIT_COLLECTION,
            type = "OUTFIT",
            targetItemId = "player",
            rarity = "RARE",
            priceBp = 4500,
            description = "Sandstorm weave uniform engineered for Miramar crossings.",
            accentHex = 0xFFD4A373
        ),
        CatalogItemSpec(
            id = "outfit_cyber_assassin",
            name = "Cyber Assassin Cyberwear",
            category = StoreCategory.OUTFIT_COLLECTION,
            type = "OUTFIT",
            targetItemId = "player",
            rarity = "LEGENDARY",
            priceCredits = 850,
            description = "Neon-lit exosuit plating worn by Night Ops strike teams.",
            accentHex = 0xFF00E5FF
        ),
        CatalogItemSpec(
            id = "outfit_crimson_commander",
            name = "Crimson Phantom Overcoat",
            category = StoreCategory.OUTFIT_COLLECTION,
            type = "OUTFIT",
            targetItemId = "player",
            rarity = "MYTHIC",
            priceCredits = 1150,
            description = "Signature red-gold championship jacket with armored pauldrons.",
            accentHex = 0xFFFF1744
        ),

        // 5. BACKPACK SKINS
        CatalogItemSpec(
            id = "backpack_tactical",
            name = "Spectre Military Pack L3",
            category = StoreCategory.BACKPACK_SKINS,
            type = "BACKPACK",
            targetItemId = "backpack",
            rarity = "RARE",
            priceBp = 1800,
            description = "Reinforced modular field backpack with hydration pouch.",
            starterOwned = true,
            starterEquipped = true,
            accentHex = 0xFF546E7A
        ),
        CatalogItemSpec(
            id = "backpack_cyber_wing",
            name = "Mecha Jetwing Pack L3",
            category = StoreCategory.BACKPACK_SKINS,
            type = "BACKPACK",
            targetItemId = "backpack",
            rarity = "LEGENDARY",
            priceCredits = 520,
            description = "Aerodynamic twin-thruster alloy pack with glowing vents.",
            accentHex = 0xFF2979FF
        ),
        CatalogItemSpec(
            id = "backpack_dragon_shell",
            name = "Inferno Scale Pack L3",
            category = StoreCategory.BACKPACK_SKINS,
            type = "BACKPACK",
            targetItemId = "backpack",
            rarity = "EPIC",
            priceBp = 5200,
            description = "Heat-resistant crimson carapace field pack.",
            accentHex = 0xFFFF3D00
        ),

        // 6. HELMET SKINS
        CatalogItemSpec(
            id = "helmet_cyber_vis",
            name = "Cybernetic Visor Helmet L3",
            category = StoreCategory.HELMET_SKINS,
            type = "HELMET",
            targetItemId = "helmet",
            rarity = "LEGENDARY",
            priceCredits = 480,
            description = "HUD-projected ballistic visor integrated into composite shell.",
            starterOwned = true,
            starterEquipped = true,
            accentHex = 0xFFFFA000
        ),
        CatalogItemSpec(
            id = "helmet_titan_gold",
            name = "Titan Command Visor L3",
            category = StoreCategory.HELMET_SKINS,
            type = "HELMET",
            targetItemId = "helmet",
            rarity = "EPIC",
            priceBp = 5400,
            description = "Heavy gilded commander headgear with armored jaw guard.",
            accentHex = 0xFFFFD54F
        ),
        CatalogItemSpec(
            id = "helmet_oni_phantom",
            name = "Phantom Oni Mask L3",
            category = StoreCategory.HELMET_SKINS,
            type = "HELMET",
            targetItemId = "helmet",
            rarity = "MYTHIC",
            priceCredits = 780,
            description = "Intimidating neon-horned ballistic mask for elite duelists.",
            accentHex = 0xFFD500F9
        ),

        // 7. PARACHUTE SKINS
        CatalogItemSpec(
            id = "para_flame",
            name = "Inferno Streak Parachute",
            category = StoreCategory.PARACHUTE_SKINS,
            type = "PARACHUTE",
            targetItemId = "parachute",
            rarity = "EPIC",
            priceBp = 4000,
            description = "Blazing ember canopy visible across drop zones.",
            starterOwned = true,
            starterEquipped = true,
            accentHex = 0xFFFF6D00
        ),
        CatalogItemSpec(
            id = "para_matrix",
            name = "Matrix Neon Parachute",
            category = StoreCategory.PARACHUTE_SKINS,
            type = "PARACHUTE",
            targetItemId = "parachute",
            rarity = "EPIC",
            priceBp = 6500,
            description = "Digital circuit-grid chute with cyan edge trim.",
            accentHex = 0xFF00E676
        ),
        CatalogItemSpec(
            id = "para_royal_wing",
            name = "WarX Apex Wings Chute",
            category = StoreCategory.PARACHUTE_SKINS,
            type = "PARACHUTE",
            targetItemId = "parachute",
            rarity = "LEGENDARY",
            priceCredits = 440,
            description = "Signature crowned wing canopy emblazoned with the WarX crest.",
            accentHex = 0xFFFFC400
        ),

        // 8. VEHICLE SKINS
        CatalogItemSpec(
            id = "vehicle_uaz_camo",
            name = "UAZ - Desert Camo Armor",
            category = StoreCategory.VEHICLE_SKINS,
            type = "VEHICLE",
            targetItemId = "uaz",
            rarity = "RARE",
            priceBp = 3500,
            description = "Reinforced off-road jeep rollcage with sandstorm finish.",
            starterOwned = true,
            starterEquipped = true,
            accentHex = 0xFFA1887F
        ),
        CatalogItemSpec(
            id = "vehicle_buggy_neon",
            name = "Offroad Buggy - Neon Beast",
            category = StoreCategory.VEHICLE_SKINS,
            type = "VEHICLE",
            targetItemId = "buggy",
            rarity = "LEGENDARY",
            priceCredits = 950,
            description = "Custom glowing underbody frame and alloy turbo dune wheels.",
            accentHex = 0xFFFF9100
        ),
        CatalogItemSpec(
            id = "vehicle_moto_crimson",
            name = "Sport Moto - Crimson Turbo",
            category = StoreCategory.VEHICLE_SKINS,
            type = "VEHICLE",
            targetItemId = "motorbike",
            rarity = "EPIC",
            priceBp = 7200,
            description = "Racing fairing in glossy crimson red & carbon fiber.",
            accentHex = 0xFFFF1744
        ),

        // 9. EMOTES
        CatalogItemSpec(
            id = "emote_salute",
            name = EmotePreset.SALUTE.label,
            category = StoreCategory.EMOTES,
            type = "EMOTE",
            targetItemId = EmotePreset.SALUTE.id,
            rarity = "COMMON",
            priceBp = 0,
            description = "Crisp military salute performed in Lobby or after eliminations.",
            starterOwned = true,
            starterEquipped = true,
            accentHex = 0xFF69F0AE
        ),
        CatalogItemSpec(
            id = "emote_victory",
            name = EmotePreset.VICTORY_DANCE.label,
            category = StoreCategory.EMOTES,
            type = "EMOTE",
            targetItemId = EmotePreset.VICTORY_DANCE.id,
            rarity = "EPIC",
            priceBp = 3000,
            description = "Signature celebration dance when claiming #1 Booyah!",
            starterOwned = true,
            accentHex = 0xFFFFD740
        ),
        CatalogItemSpec(
            id = "emote_flex",
            name = EmotePreset.SHIELD_FLEX.label,
            category = StoreCategory.EMOTES,
            type = "EMOTE",
            targetItemId = EmotePreset.SHIELD_FLEX.id,
            rarity = "EPIC",
            priceBp = 3800,
            description = "Enforcer power stance displaying holographic armor pulse.",
            accentHex = 0xFF40C4FF
        ),
        CatalogItemSpec(
            id = "emote_roar",
            name = EmotePreset.CHAMPION_ROAR.label,
            category = StoreCategory.EMOTES,
            type = "EMOTE",
            targetItemId = EmotePreset.CHAMPION_ROAR.id,
            rarity = "LEGENDARY",
            priceCredits = 360,
            description = "Fierce WarX champion battle cry with ember burst.",
            accentHex = 0xFFFF5252
        ),

        // 10. PET COLLECTION
        CatalogItemSpec(
            id = "pet_draco_flame",
            name = PetPreset.DRACO_FLAME.displayName,
            category = StoreCategory.PET_COLLECTION,
            type = "PET",
            targetItemId = PetPreset.DRACO_FLAME.id,
            rarity = "EPIC",
            priceBp = 0,
            description = "${PetPreset.DRACO_FLAME.skillTitle}: ${PetPreset.DRACO_FLAME.skillDesc}",
            starterOwned = true,
            starterEquipped = true,
            accentHex = 0xFFFF6B14
        ),
        CatalogItemSpec(
            id = "pet_cyber_pup",
            name = PetPreset.CYBER_PUP.displayName,
            category = StoreCategory.PET_COLLECTION,
            type = "PET",
            targetItemId = PetPreset.CYBER_PUP.id,
            rarity = "EPIC",
            priceBp = 4500,
            description = "${PetPreset.CYBER_PUP.skillTitle}: ${PetPreset.CYBER_PUP.skillDesc}",
            accentHex = 0xFF00E5FF
        ),
        CatalogItemSpec(
            id = "pet_frost_owl",
            name = PetPreset.FROST_OWL.displayName,
            category = StoreCategory.PET_COLLECTION,
            type = "PET",
            targetItemId = PetPreset.FROST_OWL.id,
            rarity = "LEGENDARY",
            priceCredits = 420,
            description = "${PetPreset.FROST_OWL.skillTitle}: ${PetPreset.FROST_OWL.skillDesc}",
            accentHex = 0xFF59D9FF
        ),
        CatalogItemSpec(
            id = "pet_vortex_robo",
            name = PetPreset.VORTEX_ROBO.displayName,
            category = StoreCategory.PET_COLLECTION,
            type = "PET",
            targetItemId = PetPreset.VORTEX_ROBO.id,
            rarity = "LEGENDARY",
            priceCredits = 500,
            description = "${PetPreset.VORTEX_ROBO.skillTitle}: ${PetPreset.VORTEX_ROBO.skillDesc}",
            accentHex = 0xFFFFD600
        ),

        // 11. PREMIUM BUNDLES
        CatalogItemSpec(
            id = "bundle_warx_vanguard",
            name = "WarX Vanguard Command Bundle",
            category = StoreCategory.PREMIUM_BUNDLES,
            type = "BUNDLE",
            targetItemId = "player",
            rarity = "MYTHIC",
            priceCredits = 1480,
            description = "Elite Vanguard Armored Rig + Golden Crest Helm + Ember Flare FX.",
            accentHex = 0xFFFF6E1A
        ),
        CatalogItemSpec(
            id = "bundle_neon_phantom",
            name = "Neon Phantom Cyber Pack",
            category = StoreCategory.PREMIUM_BUNDLES,
            type = "BUNDLE",
            targetItemId = "player",
            rarity = "LEGENDARY",
            priceBp = 9800,
            description = "Cyberpunk tactical coat, holographic visor & stealth drone aura.",
            accentHex = 0xFF00E5FF
        ),
        CatalogItemSpec(
            id = "bundle_arctic_overlord",
            name = "Arctic Overlord Strike Set",
            category = StoreCategory.PREMIUM_BUNDLES,
            type = "BUNDLE",
            targetItemId = "player",
            rarity = "LEGENDARY",
            priceCredits = 1050,
            description = "Full frost-forged combat suit + winter recon tactical wrap.",
            accentHex = 0xFF80D8FF
        )
    )

    fun byCategory(category: StoreCategory): List<CatalogItemSpec> =
        allCatalogItems.filter { it.category == category }

    fun findSpec(id: String): CatalogItemSpec? =
        allCatalogItems.firstOrNull { it.id == id }

    fun skinsForWeapon(weaponId: String): List<CatalogItemSpec> =
        allCatalogItems.filter { it.type == "WEAPON_SKIN" && it.targetItemId.equals(weaponId, ignoreCase = true) }

    fun upgradeCostBp(currentLevel: Int): Int = currentLevel.coerceIn(1, 5) * 1200
}
