package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlin.random.Random

class GameRepository(private val dao: VortexDao) {

    val userProfile: Flow<UserProfileEntity?> = dao.getUserProfile()
    val matchHistory: Flow<List<MatchHistoryEntity>> = dao.getMatchHistory()
    val inventory: Flow<List<InventoryItemEntity>> = dao.getOwnedInventory()
    val battlePassTiers: Flow<List<BattlePassTierEntity>> = dao.getBattlePassTiers()
    val settings: Flow<GameSettingsEntity?> = dao.getSettings()
    val missions: Flow<List<MissionEntity>> = dao.getMissions()

    suspend fun initializeDefaultDataIfEmpty() {
        if (dao.getUserProfileOnce() == null) {
            dao.insertOrUpdateProfile(UserProfileEntity())
        }

        val items = dao.getAllInventoryItems()
        if (items.isEmpty()) {
            val starterItems = listOf(
                InventoryItemEntity("skin_m416_slate", "M416 - Tactical Slate", "WEAPON_SKIN", "m416", "COMMON", isEquipped = true, isOwned = true),
                InventoryItemEntity("skin_m416_glacier", "M416 - Glacier Frost", "WEAPON_SKIN", "m416", "MYTHIC", isEquipped = false, isOwned = false, priceCredits = 1200),
                InventoryItemEntity("skin_akm_dragon", "AKM - Dragonfire Inferno", "WEAPON_SKIN", "akm", "LEGENDARY", isEquipped = true, isOwned = true),
                InventoryItemEntity("skin_awm_cyber", "AWM - Cyber Gold", "WEAPON_SKIN", "awm", "MYTHIC", isEquipped = true, isOwned = true),
                InventoryItemEntity("skin_pan_gold", "Crowbar Pan - Golden Winner", "WEAPON_SKIN", "pan", "EPIC", isEquipped = true, isOwned = true),
                InventoryItemEntity("outfit_operative", "Stealth Operative Suit", "OUTFIT", "player", "RARE", isEquipped = true, isOwned = true),
                InventoryItemEntity("outfit_cyber_assassin", "Cyber Assassin Cyberwear", "OUTFIT", "player", "LEGENDARY", isEquipped = false, isOwned = false, priceCredits = 850),
                InventoryItemEntity("outfit_ghillie", "Full Arctic Ghillie Suit", "OUTFIT", "player", "EPIC", isEquipped = false, isOwned = true),
                InventoryItemEntity("helmet_cyber_vis", "Cybernetic Visor Helmet L3", "HELMET", "helmet", "LEGENDARY", isEquipped = true, isOwned = true),
                InventoryItemEntity("backpack_tactical", "Spectre Military Pack L3", "BACKPACK", "backpack", "RARE", isEquipped = true, isOwned = true),
                InventoryItemEntity("para_flame", "Inferno Streak Parachute", "PARACHUTE", "parachute", "EPIC", isEquipped = true, isOwned = true),
                InventoryItemEntity("vehicle_uaz_camo", "UAZ - Desert Camo Armor", "VEHICLE", "uaz", "RARE", isEquipped = true, isOwned = true)
            )
            dao.insertInventoryItems(starterItems)
        }

        val tiers = dao.getBattlePassTiers().firstOrNull()
        if (tiers.isNullOrEmpty()) {
            val bpTiers = (1..50).map { t ->
                val freeReward = when {
                    t % 5 == 0 -> "Vortex Crate"
                    t % 2 == 0 -> "300 BP Coins"
                    else -> "50 XP"
                }
                val eliteReward = when (t) {
                    1 -> "AKM - Dragonfire Skin"
                    10 -> "Cyber Tactical Helmet L3"
                    25 -> "Combat Buggy Neon Drift"
                    40 -> "AWM - Cyber Gold"
                    50 -> "Apex Overlord Mythic Outfit"
                    else -> if (t % 3 == 0) "100 Vortex Credits" else "800 BP Coins"
                }
                BattlePassTierEntity(
                    tier = t,
                    freeReward = freeReward,
                    freeRewardAmount = if (t % 5 == 0) 1 else if (t % 2 == 0) 300 else 50,
                    eliteReward = eliteReward,
                    eliteRewardAmount = 1
                )
            }
            dao.insertBattlePassTiers(bpTiers)
        }

        val curMissions = dao.getMissions().firstOrNull()
        if (curMissions.isNullOrEmpty()) {
            val defaultMissions = listOf(
                // DAILY MISSIONS
                MissionEntity("d_1", "DAILY", "First Strike", "Eliminate 3 enemies in any Battle Royale mode", "KILLS", 3, currentProgress = 0, rewardBp = 450, rewardXp = 200),
                MissionEntity("d_2", "DAILY", "Heavy Artillery", "Deal 500 total combat damage across matches", "DAMAGE", 500, currentProgress = 0, rewardBp = 600, rewardXp = 250),
                MissionEntity("d_3", "DAILY", "Iron Will", "Survive for at least 6 cumulative minutes in combat", "SURVIVAL_MINUTES", 6, currentProgress = 0, rewardBp = 500, rewardXp = 300),
                MissionEntity("d_4", "DAILY", "Deployment Specialist", "Participate in 2 complete matches", "MATCHES", 2, currentProgress = 0, rewardBp = 400, rewardXp = 180),
                // WEEKLY MISSIONS
                MissionEntity("w_1", "WEEKLY", "Apex Victor", "Achieve 1 Victory Royale (#1 Winner)", "WINS", 1, currentProgress = 0, rewardBp = 2500, rewardCredits = 150, rewardXp = 1200),
                MissionEntity("w_2", "WEEKLY", "Reaper's Harvest", "Eliminate 25 total enemies in Classic BR", "KILLS", 25, currentProgress = 0, rewardBp = 3000, rewardCredits = 100, rewardXp = 1500),
                MissionEntity("w_3", "WEEKLY", "Combat Veteran", "Play 10 Battle Royale matches", "MATCHES", 10, currentProgress = 0, rewardBp = 2000, rewardCredits = 80, rewardXp = 1000),
                // ACHIEVEMENTS
                MissionEntity("a_1", "ACHIEVEMENT", "Sharpshooter Elite", "Score 50 total eliminations", "KILLS", 50, currentProgress = 0, rewardBp = 5000, rewardCredits = 300, rewardXp = 2500),
                MissionEntity("a_2", "ACHIEVEMENT", "Legendary Destroyer", "Deal 10,000 total damage to enemies", "DAMAGE", 10000, currentProgress = 0, rewardBp = 7500, rewardCredits = 500, rewardXp = 4000),
                MissionEntity("a_3", "ACHIEVEMENT", "Undisputed Champion", "Win 5 Victory Royales", "WINS", 5, currentProgress = 0, rewardBp = 10000, rewardCredits = 800, rewardXp = 5000)
            )
            dao.insertMissions(defaultMissions)
        }

        val curSettings = dao.getSettings().firstOrNull()
        if (curSettings == null) {
            dao.insertSettings(GameSettingsEntity())
        }
    }

    suspend fun recordMatchFinished(
        mode: String,
        map: String,
        placement: Int,
        kills: Int,
        damage: Float,
        durationSec: Int
    ): MatchHistoryEntity {
        val isVictory = placement == 1
        val rpDelta = if (isVictory) 35 + kills * 3 else if (placement <= 10) 18 + kills * 2 else if (placement <= 30) 5 else -12
        val bpEarned = (placement * 15 + kills * 50 + (damage * 0.2f).toInt()).coerceAtLeast(100)
        val xpEarned = (durationSec / 2 + kills * 40 + if (isVictory) 500 else 100).coerceAtLeast(150)

        val match = MatchHistoryEntity(
            gameMode = mode,
            mapName = map,
            placement = placement,
            kills = kills,
            damage = damage,
            survivalTimeSec = durationSec,
            rankPointsDelta = rpDelta,
            bpEarned = bpEarned,
            xpEarned = xpEarned,
            isVictory = isVictory
        )
        dao.insertMatchHistory(match)

        // Advance verified mission objectives
        val activeMissions = dao.getMissions().firstOrNull() ?: emptyList()
        activeMissions.forEach { m ->
            if (!m.isClaimed) {
                var delta = 0
                when (m.targetType) {
                    "KILLS" -> delta = kills
                    "DAMAGE" -> delta = damage.toInt()
                    "MATCHES" -> delta = 1
                    "WINS" -> if (isVictory) delta = 1
                    "SURVIVAL_MINUTES" -> delta = durationSec / 60
                }
                if (delta > 0) {
                    val updated = m.copy(currentProgress = (m.currentProgress + delta).coerceAtMost(m.targetCount))
                    dao.updateMission(updated)
                }
            }
        }

        // Update player profile
        val profile = dao.getUserProfileOnce() ?: UserProfileEntity()
        var newXp = profile.xp + xpEarned
        var newLvl = profile.level
        var maxXp = profile.maxXp
        while (newXp >= maxXp) {
            newXp -= maxXp
            newLvl++
            maxXp += 500
        }

        val updatedProfile = profile.copy(
            level = newLvl,
            xp = newXp,
            maxXp = maxXp,
            bpCoins = profile.bpCoins + bpEarned,
            rankPoints = (profile.rankPoints + rpDelta).coerceAtLeast(1000),
            matchesPlayed = profile.matchesPlayed + 1,
            wins = profile.wins + if (isVictory) 1 else 0,
            kills = profile.kills + kills,
            deaths = profile.deaths + if (isVictory) 0 else 1,
            headshots = profile.headshots + (kills / 3)
        )
        dao.insertOrUpdateProfile(updatedProfile)

        return match
    }

    suspend fun claimMissionReward(missionId: String): Boolean {
        val mission = dao.getMissionById(missionId) ?: return false
        // Server/local authoritative verification: must NOT be already claimed and must have reached target
        if (mission.isClaimed || mission.currentProgress < mission.targetCount) {
            return false
        }

        // Mark claimed
        dao.updateMission(mission.copy(isClaimed = true))

        // Grant rewards
        val profile = dao.getUserProfileOnce() ?: UserProfileEntity()
        var newXp = profile.xp + mission.rewardXp
        var newLvl = profile.level
        var maxXp = profile.maxXp
        while (newXp >= maxXp) {
            newXp -= maxXp
            newLvl++
            maxXp += 500
        }

        dao.insertOrUpdateProfile(
            profile.copy(
                level = newLvl,
                xp = newXp,
                maxXp = maxXp,
                bpCoins = profile.bpCoins + mission.rewardBp,
                vortexCredits = profile.vortexCredits + mission.rewardCredits
            )
        )
        return true
    }

    suspend fun updateHudSettings(scale: Float, opacity: Float) {
        val cur = dao.getSettings().firstOrNull() ?: GameSettingsEntity()
        dao.insertSettings(cur.copy(hudScale = scale, hudOpacity = opacity))
    }

    suspend fun equipItem(item: InventoryItemEntity) {
        dao.unequipAllOfType(item.type)
        dao.updateInventoryItem(item.copy(isEquipped = true))
    }

    suspend fun purchaseShopItem(item: InventoryItemEntity, useCredits: Boolean): Boolean {
        val profile = dao.getUserProfileOnce() ?: return false
        if (useCredits) {
            if (profile.vortexCredits >= item.priceCredits && item.priceCredits > 0) {
                dao.insertOrUpdateProfile(profile.copy(vortexCredits = profile.vortexCredits - item.priceCredits))
                dao.insertInventoryItems(listOf(item.copy(isOwned = true)))
                return true
            }
        } else {
            if (profile.bpCoins >= item.priceBp && item.priceBp > 0) {
                dao.insertOrUpdateProfile(profile.copy(bpCoins = profile.bpCoins - item.priceBp))
                dao.insertInventoryItems(listOf(item.copy(isOwned = true)))
                return true
            }
        }
        return false
    }

    suspend fun openLootCrate(useCredits: Boolean = false): InventoryItemEntity? {
        val profile = dao.getUserProfileOnce() ?: UserProfileEntity()
        if (useCredits) {
            if (profile.vortexCredits < 120) return null
            dao.insertOrUpdateProfile(profile.copy(vortexCredits = profile.vortexCredits - 120))
        } else {
            if (profile.bpCoins < 1000) return null
            dao.insertOrUpdateProfile(profile.copy(bpCoins = profile.bpCoins - 1000))
        }

        val allItems = dao.getAllInventoryItems()
        val roll = Random.nextFloat() * 100f
        val targetRarity = when {
            roll < 0.5f -> "MYTHIC"
            roll < 4.0f -> "LEGENDARY"
            roll < 20.0f -> "EPIC"
            else -> "RARE"
        }
        val pool = allItems.filter { it.rarity == targetRarity }
        val chosen = (if (pool.isNotEmpty()) pool.random() else allItems.randomOrNull())
            ?: InventoryItemEntity("skin_awm_${Random.nextInt()}", "AWM - Tactical Viper", "WEAPON_SKIN", "awm", targetRarity, isOwned = true)
        val owned = chosen.copy(isOwned = true)
        dao.insertInventoryItems(listOf(owned))
        return owned
    }

    suspend fun claimDailyReward(): Boolean {
        val profile = dao.getUserProfileOnce() ?: return false
        val now = System.currentTimeMillis()
        val oneDayMs = 24L * 60L * 60L * 1000L
        if (now - profile.lastDailyClaimTimestamp < oneDayMs && profile.lastDailyClaimTimestamp > 0L) {
            return false
        }
        dao.insertOrUpdateProfile(
            profile.copy(
                bpCoins = profile.bpCoins + 1500,
                lastDailyClaimTimestamp = now
            )
        )
        return true
    }

    suspend fun updateUsername(newName: String) {
        val trimmed = newName.trim().take(16)
        if (trimmed.isEmpty()) return
        val profile = dao.getUserProfileOnce() ?: return
        dao.insertOrUpdateProfile(profile.copy(username = trimmed))
    }

    suspend fun claimBattlePassTier(tierEntity: BattlePassTierEntity, isElite: Boolean) {
        val updated = if (isElite) tierEntity.copy(isEliteClaimed = true) else tierEntity.copy(isFreeClaimed = true)
        dao.updateBattlePassTier(updated)
        val profile = dao.getUserProfileOnce() ?: return
        if (!isElite && tierEntity.freeReward.contains("BP")) {
            dao.insertOrUpdateProfile(profile.copy(bpCoins = profile.bpCoins + tierEntity.freeRewardAmount))
        } else if (isElite && tierEntity.eliteReward.contains("Credits")) {
            dao.insertOrUpdateProfile(profile.copy(vortexCredits = profile.vortexCredits + 100))
        } else if (isElite && tierEntity.eliteReward.contains("BP")) {
            dao.insertOrUpdateProfile(profile.copy(bpCoins = profile.bpCoins + 800))
        }
    }

    suspend fun updateSettings(settings: GameSettingsEntity) {
        dao.insertSettings(settings)
    }

    suspend fun updateWallpaperTheme(themeId: String) {
        val current = dao.getSettings().firstOrNull() ?: GameSettingsEntity()
        dao.insertSettings(current.copy(selectedWallpaperTheme = themeId))
    }

    suspend fun updateSelectedCharacter(presetId: String) {
        val profile = dao.getUserProfileOnce() ?: UserProfileEntity()
        dao.insertOrUpdateProfile(profile.copy(selectedCharacterId = presetId))
    }

    suspend fun updateGraphicsQuality(quality: String) {
        val current = dao.getSettings().firstOrNull() ?: GameSettingsEntity()
        dao.insertSettings(current.copy(graphicsQuality = quality))
    }

    suspend fun grantTestCurrency(bp: Int, credits: Int) {
        val profile = dao.getUserProfileOnce() ?: UserProfileEntity()
        dao.insertOrUpdateProfile(profile.copy(
            bpCoins = profile.bpCoins + bp,
            vortexCredits = profile.vortexCredits + credits
        ))
    }

    companion object {
        fun create(context: Context): GameRepository {
            val db = VortexDatabase.getInstance(context)
            return GameRepository(db.dao())
        }
    }
}
