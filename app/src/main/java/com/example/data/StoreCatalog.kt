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
            accentHex = 0xFFE5192B
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
            accentHex = 0xFFFF2E43
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
        CatalogItemSpec(
            id = "char_kitsune_shrine",
            name = CharacterPreset.KITSUNE_SHRINE.displayName,
            category = StoreCategory.CHARACTER_STORE,
            type = "CHARACTER",
            targetItemId = CharacterPreset.KITSUNE_SHRINE.id,
            rarity = "LEGENDARY",
            priceBp = CharacterPreset.KITSUNE_SHRINE.unlockBp,
            description = "${CharacterPreset.KITSUNE_SHRINE.roleTitle} • ${CharacterPreset.KITSUNE_SHRINE.activeSkillName}: ${CharacterPreset.KITSUNE_SHRINE.activeSkillDesc}",
            accentHex = 0xFFFF2442
        ),
        CatalogItemSpec(
            id = "char_flame_skull",
            name = CharacterPreset.FLAME_SKULL.displayName,
            category = StoreCategory.CHARACTER_STORE,
            type = "CHARACTER",
            targetItemId = CharacterPreset.FLAME_SKULL.id,
            rarity = "MYTHIC",
            priceCredits = CharacterPreset.FLAME_SKULL.unlockCredits,
            description = "${CharacterPreset.FLAME_SKULL.roleTitle} • ${CharacterPreset.FLAME_SKULL.activeSkillName}: ${CharacterPreset.FLAME_SKULL.activeSkillDesc}",
            accentHex = 0xFFFF6E1A
        ),
        CatalogItemSpec(
            id = "char_neon_samurai",
            name = CharacterPreset.NEON_SAMURAI.displayName,
            category = StoreCategory.CHARACTER_STORE,
            type = "CHARACTER",
            targetItemId = CharacterPreset.NEON_SAMURAI.id,
            rarity = "MYTHIC",
            priceCredits = CharacterPreset.NEON_SAMURAI.unlockCredits,
            description = "${CharacterPreset.NEON_SAMURAI.roleTitle} • ${CharacterPreset.NEON_SAMURAI.activeSkillName}: ${CharacterPreset.NEON_SAMURAI.activeSkillDesc}",
            accentHex = 0xFFFF22B8
        ),
        CatalogItemSpec(
            id = "char_cyber_afro",
            name = CharacterPreset.CYBER_AFRO.displayName,
            category = StoreCategory.CHARACTER_STORE,
            type = "CHARACTER",
            targetItemId = CharacterPreset.CYBER_AFRO.id,
            rarity = "EPIC",
            priceBp = CharacterPreset.CYBER_AFRO.unlockBp,
            description = "${CharacterPreset.CYBER_AFRO.roleTitle} • ${CharacterPreset.CYBER_AFRO.activeSkillName}: ${CharacterPreset.CYBER_AFRO.activeSkillDesc}",
            accentHex = 0xFFE6FF14
        ),
        CatalogItemSpec(
            id = "char_royal_commander",
            name = CharacterPreset.ROYAL_COMMANDER.displayName,
            category = StoreCategory.CHARACTER_STORE,
            type = "CHARACTER",
            targetItemId = CharacterPreset.ROYAL_COMMANDER.id,
            rarity = "MYTHIC",
            priceCredits = CharacterPreset.ROYAL_COMMANDER.unlockCredits,
            description = "${CharacterPreset.ROYAL_COMMANDER.roleTitle} • ${CharacterPreset.ROYAL_COMMANDER.activeSkillName}: ${CharacterPreset.ROYAL_COMMANDER.activeSkillDesc}",
            accentHex = 0xFFFFC828
        ),
        CatalogItemSpec(
            id = "char_street_mafia",
            name = CharacterPreset.STREET_MAFIA.displayName,
            category = StoreCategory.CHARACTER_STORE,
            type = "CHARACTER",
            targetItemId = CharacterPreset.STREET_MAFIA.id,
            rarity = "LEGENDARY",
            priceBp = CharacterPreset.STREET_MAFIA.unlockBp,
            description = "${CharacterPreset.STREET_MAFIA.roleTitle} • ${CharacterPreset.STREET_MAFIA.activeSkillName}: ${CharacterPreset.STREET_MAFIA.activeSkillDesc}",
            accentHex = 0xFFE5E5EC
        ),
        CatalogItemSpec(
            id = "char_violet_blade",
            name = CharacterPreset.VIOLET_BLADE.displayName,
            category = StoreCategory.CHARACTER_STORE,
            type = "CHARACTER",
            targetItemId = CharacterPreset.VIOLET_BLADE.id,
            rarity = "LEGENDARY",
            priceCredits = CharacterPreset.VIOLET_BLADE.unlockCredits,
            description = "${CharacterPreset.VIOLET_BLADE.roleTitle} • ${CharacterPreset.VIOLET_BLADE.activeSkillName}: ${CharacterPreset.VIOLET_BLADE.activeSkillDesc}",
            accentHex = 0xFFAE3EFF
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
            id = "skin_awm_butterfly",
            name = "AWM - Crimson Butterfly",
            category = StoreCategory.WEAPON_SKINS,
            type = "WEAPON_SKIN",
            targetItemId = "awm",
            rarity = "MYTHIC",
            priceCredits = 1350,
            description = "Translucent crimson butterfly wings above the 8x sniper scope with scarlet tracers.",
            accentHex = 0xFFFF2442
        ),
        CatalogItemSpec(
            id = "skin_m1887_rapper",
            name = "M1887 - Rapper Underworld",
            category = StoreCategory.WEAPON_SKINS,
            type = "WEAPON_SKIN",
            targetItemId = "m1887",
            rarity = "MYTHIC",
            priceCredits = 1100,
            description = "Violet flame & gilded skull double-barrel finish with blistering close-range fire rate.",
            starterOwned = true,
            starterEquipped = true,
            accentHex = 0xFFB833FF
        ),
        CatalogItemSpec(
            id = "skin_mp40_bunny",
            name = "MP40 - Crazy Bunny",
            category = StoreCategory.WEAPON_SKINS,
            type = "WEAPON_SKIN",
            targetItemId = "mp40",
            rarity = "LEGENDARY",
            priceBp = 7400,
            description = "Acid-lime, violet and flame-orange street-art custom chassis.",
            starterOwned = true,
            starterEquipped = true,
            accentHex = 0xFFAEEA00
        ),
        CatalogItemSpec(
            id = "skin_katana_neon",
            name = "Katana - Cyber Neon Edge",
            category = StoreCategory.WEAPON_SKINS,
            type = "WEAPON_SKIN",
            targetItemId = "katana",
            rarity = "LEGENDARY",
            priceBp = 6200,
            description = "Electric indigo scabbard with glowing plasma edge trail.",
            starterOwned = true,
            starterEquipped = true,
            accentHex = 0xFF40C4FF
        ),
        CatalogItemSpec(
            id = "skin_deagle_koi",
            name = "Sidearm - Koi Dragon",
            category = StoreCategory.WEAPON_SKINS,
            type = "WEAPON_SKIN",
            targetItemId = "deagle",
            rarity = "EPIC",
            priceBp = 4400,
            description = "Crimson and ivory koi-wave slide engraving with golden trigger.",
            accentHex = 0xFFFF5252
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
        CatalogItemSpec(
            id = "gun_mp40",
            name = "MP40 Cobra SMG",
            category = StoreCategory.GUN_COLLECTION,
            type = "GUN",
            targetItemId = "mp40",
            rarity = "EPIC",
            priceBp = 3600,
            description = "Ultra-high RPM folding-stock submachine gun built for aggressive rushes.",
            starterOwned = true,
            accentHex = 0xFFAEEA00
        ),
        CatalogItemSpec(
            id = "gun_m1887",
            name = "M1887 Double-Barrel Shotgun",
            category = StoreCategory.GUN_COLLECTION,
            type = "GUN",
            targetItemId = "m1887",
            rarity = "MYTHIC",
            priceCredits = 680,
            description = "Lethal 2-shot lever-action shotgun capable of one-tap knockdowns.",
            starterOwned = true,
            accentHex = 0xFFB833FF
        ),
        CatalogItemSpec(
            id = "gun_gatling",
            name = "Gatling Vulcan LMG",
            category = StoreCategory.GUN_COLLECTION,
            type = "GUN",
            targetItemId = "gatling",
            rarity = "LEGENDARY",
            priceCredits = 740,
            description = "Multi-barrel rotary machine gun with 100-round drum capacity.",
            accentHex = 0xFFFF8F00
        ),
        // Melee collection weapons (Slot 4)
        CatalogItemSpec(
            id = "melee_katana",
            name = "Katana",
            category = StoreCategory.GUN_COLLECTION,
            type = "MELEE",
            targetItemId = "katana",
            rarity = "LEGENDARY",
            priceBp = 2800,
            description = "Razor-sharp cybernetic katana with rapid dash-slash reach.",
            starterOwned = true,
            starterEquipped = true,
            accentHex = 0xFF40C4FF
        ),
        CatalogItemSpec(
            id = "melee_scythe",
            name = "Crimson Scythe",
            category = StoreCategory.GUN_COLLECTION,
            type = "MELEE",
            targetItemId = "scythe",
            rarity = "MYTHIC",
            priceBp = 4500,
            description = "Curved executioner scythe dealing sweeping close-range cleaves.",
            starterOwned = true,
            accentHex = 0xFFFF2442
        ),
        CatalogItemSpec(
            id = "melee_bat",
            name = "Spiked Combat Bat",
            category = StoreCategory.GUN_COLLECTION,
            type = "MELEE",
            targetItemId = "bat",
            rarity = "EPIC",
            priceBp = 2200,
            description = "Wrapped street slugger reinforced with tempered steel bands.",
            starterOwned = true,
            accentHex = 0xFFFFB300
        ),
        CatalogItemSpec(
            id = "melee_machete",
            name = "Shadow Machete",
            category = StoreCategory.GUN_COLLECTION,
            type = "MELEE",
            targetItemId = "machete",
            rarity = "RARE",
            priceBp = 1600,
            description = "Tactical survival blade balanced for quick melee takedowns.",
            starterOwned = true,
            accentHex = 0xFF90A4AE
        ),
        CatalogItemSpec(
            id = "melee_pan",
            name = "Cast Iron Pan",
            category = StoreCategory.GUN_COLLECTION,
            type = "MELEE",
            targetItemId = "pan",
            rarity = "EPIC",
            priceBp = 3000,
            description = "Iconic bullet-deflecting Battle Royale melee champion skillet.",
            starterOwned = true,
            accentHex = 0xFFFFD700
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

        // 6. HELMET SKINS (Level 1 to Level 4 styles)
        CatalogItemSpec(
            id = "helmet_cyber_vis",
            name = "Cybernetic Respirator Helmet L4",
            category = StoreCategory.HELMET_SKINS,
            type = "HELMET",
            targetItemId = "helmet",
            rarity = "LEGENDARY",
            priceCredits = 480,
            description = "Lv.4 glowing red-eye tactical respirator visor integrated into ballistic alloy.",
            starterOwned = true,
            starterEquipped = true,
            accentHex = 0xFFFF1744
        ),
        CatalogItemSpec(
            id = "helmet_titan_gold",
            name = "Spetsnaz Welded Shield Helm L3",
            category = StoreCategory.HELMET_SKINS,
            type = "HELMET",
            targetItemId = "helmet",
            rarity = "EPIC",
            priceBp = 5400,
            description = "Heavy reinforced titanium face shield engineered for Level 3 protection.",
            accentHex = 0xFFFFD54F
        ),
        CatalogItemSpec(
            id = "helmet_oni_phantom",
            name = "Phantom Oni Mask L4",
            category = StoreCategory.HELMET_SKINS,
            type = "HELMET",
            targetItemId = "helmet",
            rarity = "MYTHIC",
            priceCredits = 780,
            description = "Intimidating crimson-eyed armored mask for top-ranked WarX duelists.",
            accentHex = 0xFFD500F9
        ),

        // 7. PARACHUTE & GLIDER SKINS
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
        CatalogItemSpec(
            id = "para_glider_aero",
            name = "Tactical Aero Glider Wing",
            category = StoreCategory.PARACHUTE_SKINS,
            type = "PARACHUTE",
            targetItemId = "glider",
            rarity = "MYTHIC",
            priceCredits = 620,
            description = "High-glide crimson aero wingsuit frame for extended map drops.",
            starterOwned = true,
            accentHex = 0xFFFF2442
        ),

        // 8. VEHICLE & SUPERCAR SKINS
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
            name = "Sport Moto - Cyber Neon Skull",
            category = StoreCategory.VEHICLE_SKINS,
            type = "VEHICLE",
            targetItemId = "motorbike",
            rarity = "MYTHIC",
            priceBp = 7200,
            description = "Holographic neon-aqua street bike with glowing turbine rims.",
            starterOwned = true,
            accentHex = 0xFF00E5FF
        ),
        CatalogItemSpec(
            id = "vehicle_supercar_crimson",
            name = "WarX Hypercar - Crimson Fury",
            category = StoreCategory.VEHICLE_SKINS,
            type = "VEHICLE",
            targetItemId = "dacia",
            rarity = "MYTHIC",
            priceCredits = 1280,
            description = "Aerodynamic mid-engine supercar with carbon spoiler & crimson vents.",
            accentHex = 0xFFFF1744
        ),
        CatalogItemSpec(
            id = "vehicle_supercar_gold",
            name = "Apex GT - Sovereign Gold",
            category = StoreCategory.VEHICLE_SKINS,
            type = "VEHICLE",
            targetItemId = "dacia",
            rarity = "LEGENDARY",
            priceCredits = 980,
            description = "Gilded championship coupe with titanium exhaust flames.",
            accentHex = 0xFFFFD54F
        ),

        // 9. EMOTES (12 Iconic Expressions)
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
            starterOwned = true,
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
        CatalogItemSpec(
            id = "emote_backflip",
            name = EmotePreset.BACKFLIP.label,
            category = StoreCategory.EMOTES,
            type = "EMOTE",
            targetItemId = EmotePreset.BACKFLIP.id,
            rarity = "EPIC",
            priceBp = 3200,
            description = "Zero-gravity mid-air backflip taunt.",
            starterOwned = true,
            accentHex = 0xFFBA68C8
        ),
        CatalogItemSpec(
            id = "emote_throne",
            name = EmotePreset.THRONE_SIT.label,
            category = StoreCategory.EMOTES,
            type = "EMOTE",
            targetItemId = EmotePreset.THRONE_SIT.id,
            rarity = "MYTHIC",
            priceCredits = 490,
            description = "Summons a crimson championship throne for the victor.",
            accentHex = 0xFFFFB300
        ),
        CatalogItemSpec(
            id = "emote_high_kick",
            name = EmotePreset.HIGH_KICK.label,
            category = StoreCategory.EMOTES,
            type = "EMOTE",
            targetItemId = EmotePreset.HIGH_KICK.id,
            rarity = "LEGENDARY",
            priceBp = 4800,
            description = "High-impact martial flying kick combo.",
            accentHex = 0xFFFF5252
        ),
        CatalogItemSpec(
            id = "emote_dj_mix",
            name = EmotePreset.DJ_MIX.label,
            category = StoreCategory.EMOTES,
            type = "EMOTE",
            targetItemId = EmotePreset.DJ_MIX.id,
            rarity = "EPIC",
            priceBp = 3600,
            description = "Holo-turntable bass drop celebration.",
            starterOwned = true,
            accentHex = 0xFF00E5FF
        ),
        CatalogItemSpec(
            id = "emote_dab",
            name = EmotePreset.DAB.label,
            category = StoreCategory.EMOTES,
            type = "EMOTE",
            targetItemId = EmotePreset.DAB.id,
            rarity = "RARE",
            priceBp = 2200,
            description = "Classic lightning winner dab move.",
            starterOwned = true,
            accentHex = 0xFF448AFF
        ),
        CatalogItemSpec(
            id = "emote_kungfu",
            name = EmotePreset.KUNGFU_STANCE.label,
            category = StoreCategory.EMOTES,
            type = "EMOTE",
            targetItemId = EmotePreset.KUNGFU_STANCE.id,
            rarity = "LEGENDARY",
            priceCredits = 320,
            description = "Flowing martial arts dragon stance.",
            accentHex = 0xFFFF9800
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
        CatalogItemSpec(
            id = "pet_panda_detective",
            name = PetPreset.PANDA_DETECTIVE.displayName,
            category = StoreCategory.PET_COLLECTION,
            type = "PET",
            targetItemId = PetPreset.PANDA_DETECTIVE.id,
            rarity = "EPIC",
            priceBp = 4200,
            description = "${PetPreset.PANDA_DETECTIVE.skillTitle}: ${PetPreset.PANDA_DETECTIVE.skillDesc}",
            starterOwned = true,
            accentHex = 0xFF1DE9B6
        ),
        CatalogItemSpec(
            id = "pet_cyber_orbit",
            name = PetPreset.CYBER_ORBIT.displayName,
            category = StoreCategory.PET_COLLECTION,
            type = "PET",
            targetItemId = PetPreset.CYBER_ORBIT.id,
            rarity = "MYTHIC",
            priceCredits = 580,
            description = "${PetPreset.CYBER_ORBIT.skillTitle}: ${PetPreset.CYBER_ORBIT.skillDesc}",
            starterOwned = true,
            accentHex = 0xFF00E5FF
        ),
        CatalogItemSpec(
            id = "pet_waggor_penguin",
            name = PetPreset.WAGGOR_PENGUIN.displayName,
            category = StoreCategory.PET_COLLECTION,
            type = "PET",
            targetItemId = PetPreset.WAGGOR_PENGUIN.id,
            rarity = "LEGENDARY",
            priceBp = 5600,
            description = "${PetPreset.WAGGOR_PENGUIN.skillTitle}: ${PetPreset.WAGGOR_PENGUIN.skillDesc}",
            starterOwned = true,
            accentHex = 0xFF40C4FF
        ),

        // 11. PREMIUM BUNDLES, GLOO WALL SKINS & TACTICAL LOADOUT ITEMS
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
        ),
        // Gloo Wall Skins (Rendered on DeployableShield in 3D)
        CatalogItemSpec(
            id = "gloo_cyber",
            name = "Gloo Wall - Cyber Lightning",
            category = StoreCategory.PREMIUM_BUNDLES,
            type = "GLOO_WALL",
            targetItemId = "gloo_cyber",
            rarity = "EPIC",
            priceBp = 3500,
            description = "Electric aqua lightning barrier shielding teammates under fire.",
            starterOwned = true,
            starterEquipped = true,
            accentHex = 0xFF00E5FF
        ),
        CatalogItemSpec(
            id = "gloo_flame_skull",
            name = "Gloo Wall - Flame Skull Hockey",
            category = StoreCategory.PREMIUM_BUNDLES,
            type = "GLOO_WALL",
            targetItemId = "gloo_flame_skull",
            rarity = "MYTHIC",
            priceCredits = 640,
            description = "Scorching crimson demon-skull barrier with blazing vents.",
            starterOwned = true,
            accentHex = 0xFFFF3D00
        ),
        CatalogItemSpec(
            id = "gloo_samurai_ink",
            name = "Gloo Wall - Samurai Ink Lotus",
            category = StoreCategory.PREMIUM_BUNDLES,
            type = "GLOO_WALL",
            targetItemId = "gloo_samurai_ink",
            rarity = "LEGENDARY",
            priceBp = 5800,
            description = "Sumi-e bamboo ink crest etched into forged steel.",
            accentHex = 0xFF90A4AE
        ),
        CatalogItemSpec(
            id = "gloo_neon_mech",
            name = "Gloo Wall - Neon Mech Wheel",
            category = StoreCategory.PREMIUM_BUNDLES,
            type = "GLOO_WALL",
            targetItemId = "gloo_neon_mech",
            rarity = "LEGENDARY",
            priceCredits = 520,
            description = "Solar-yellow heavy mecha turbine cover wall.",
            accentHex = 0xFFFFD600
        ),
        CatalogItemSpec(
            id = "gloo_purple_crown",
            name = "Gloo Wall - Royal Crown Violet",
            category = StoreCategory.PREMIUM_BUNDLES,
            type = "GLOO_WALL",
            targetItemId = "gloo_purple_crown",
            rarity = "MYTHIC",
            priceCredits = 720,
            description = "Amethyst crystalline shield crowned with gilded WarX wings.",
            accentHex = 0xFFD500F9
        ),
        // Loadout Utility Items
        CatalogItemSpec(
            id = "item_bonfire",
            name = "Loadout - Squad Bonfire",
            category = StoreCategory.PREMIUM_BUNDLES,
            type = "BUNDLE",
            targetItemId = "bonfire",
            rarity = "RARE",
            priceBp = 1500,
            description = "Drops a healing & EP-restoring tactical camp beacon.",
            starterOwned = true,
            accentHex = 0xFFFF9100
        ),
        CatalogItemSpec(
            id = "item_bounty_map",
            name = "Loadout - Warrior's Bounty Token",
            category = StoreCategory.PREMIUM_BUNDLES,
            type = "BUNDLE",
            targetItemId = "bounty",
            rarity = "EPIC",
            priceBp = 2200,
            description = "Unlocks bonus Lv.4 gear inside Warrior's Loot crates.",
            starterOwned = true,
            accentHex = 0xFFFF2442
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
