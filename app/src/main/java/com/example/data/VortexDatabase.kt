package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val username: String = "VortexGhost",
    val avatarId: String = "avatar_tactical_1",
    val level: Int = 42,
    val xp: Int = 3450,
    val maxXp: Int = 5000,
    val bpCoins: Int = 18500,
    val vortexCredits: Int = 960,
    val rankTier: String = "Crown III",
    val rankPoints: Int = 4120,
    val matchesPlayed: Int = 184,
    val wins: Int = 42,
    val kills: Int = 890,
    val deaths: Int = 142,
    val headshots: Int = 312,
    val title: String = "Apex Marksman",
    val lastDailyClaimTimestamp: Long = 0L,
    val selectedCharacterId: String = "vortex_ghost"
) {
    val kdRatio: Float
        get() = if (deaths > 0) kills.toFloat() / deaths.toFloat() else kills.toFloat()

    val winRatePercent: Float
        get() = if (matchesPlayed > 0) (wins.toFloat() / matchesPlayed.toFloat()) * 100f else 0f
}

@Entity(tableName = "match_history")
data class MatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameMode: String,
    val mapName: String,
    val placement: Int,
    val totalPlayers: Int = 100,
    val kills: Int,
    val damage: Float,
    val survivalTimeSec: Int,
    val rankPointsDelta: Int,
    val bpEarned: Int,
    val xpEarned: Int,
    val isVictory: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // WEAPON_SKIN, OUTFIT, HELMET, BACKPACK, PARACHUTE, VEHICLE
    val targetItemId: String, // e.g. "m416", "player_male", "uaz"
    val rarity: String, // COMMON, RARE, EPIC, LEGENDARY, MYTHIC
    val isEquipped: Boolean = false,
    val isOwned: Boolean = true,
    val priceBp: Int = 0,
    val priceCredits: Int = 0
)

@Entity(tableName = "battle_pass_tiers")
data class BattlePassTierEntity(
    @PrimaryKey val tier: Int,
    val freeReward: String,
    val freeRewardAmount: Int,
    val isFreeClaimed: Boolean = false,
    val eliteReward: String,
    val eliteRewardAmount: Int,
    val isEliteClaimed: Boolean = false
)

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey val id: String,
    val category: String, // DAILY, WEEKLY, ACHIEVEMENT
    val title: String,
    val description: String,
    val targetType: String, // KILLS, DAMAGE, MATCHES, WINS, SURVIVAL_MINUTES
    val targetCount: Int,
    val currentProgress: Int = 0,
    val rewardBp: Int = 0,
    val rewardCredits: Int = 0,
    val rewardXp: Int = 0,
    val isClaimed: Boolean = false
) {
    val isCompleted: Boolean
        get() = currentProgress >= targetCount
}

@Entity(tableName = "game_settings")
data class GameSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val graphicsQuality: String = "Ultra HD", // Smooth, Balanced, HD, Ultra HD
    val targetFps: Int = 60, // 30, 60, 90
    val aimAssistEnabled: Boolean = true,
    val autoPickupEnabled: Boolean = true,
    val gyroscopeEnabled: Boolean = false,
    val leftHandFireEnabled: Boolean = true,
    val cameraSensitivity: Float = 1.0f,
    val adsSensitivity: Float = 0.85f,
    val masterVolume: Float = 1.0f,
    val sfxVolume: Float = 1.0f,
    val voiceChatVolume: Float = 0.8f,
    val selectedWallpaperTheme: String = "DARK_TACTICAL",
    val hudScale: Float = 1.0f,
    val hudOpacity: Float = 0.90f,
    val bgmVolume: Float = 0.55f
)

@Dao
interface VortexDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfileOnce(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM match_history ORDER BY timestamp DESC LIMIT 30")
    fun getMatchHistory(): Flow<List<MatchHistoryEntity>>

    @Insert
    suspend fun insertMatchHistory(match: MatchHistoryEntity)

    @Query("SELECT * FROM inventory_items WHERE isOwned = 1")
    fun getOwnedInventory(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items")
    suspend fun getAllInventoryItems(): List<InventoryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItems(items: List<InventoryItemEntity>)

    @Update
    suspend fun updateInventoryItem(item: InventoryItemEntity)

    @Query("UPDATE inventory_items SET isEquipped = 0 WHERE type = :type")
    suspend fun unequipAllOfType(type: String)

    @Query("SELECT * FROM battle_pass_tiers ORDER BY tier ASC")
    fun getBattlePassTiers(): Flow<List<BattlePassTierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBattlePassTiers(tiers: List<BattlePassTierEntity>)

    @Update
    suspend fun updateBattlePassTier(tier: BattlePassTierEntity)

    @Query("SELECT * FROM game_settings WHERE id = 1")
    fun getSettings(): Flow<GameSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: GameSettingsEntity)

    @Query("SELECT * FROM missions ORDER BY category ASC, targetCount ASC")
    fun getMissions(): Flow<List<MissionEntity>>

    @Query("SELECT * FROM missions WHERE id = :id")
    suspend fun getMissionById(id: String): MissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMissions(missions: List<MissionEntity>)

    @Update
    suspend fun updateMission(mission: MissionEntity)
}

@Database(
    entities = [
        UserProfileEntity::class,
        MatchHistoryEntity::class,
        InventoryItemEntity::class,
        BattlePassTierEntity::class,
        GameSettingsEntity::class,
        MissionEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class VortexDatabase : RoomDatabase() {
    abstract fun dao(): VortexDao

    companion object {
        @Volatile
        private var INSTANCE: VortexDatabase? = null

        fun getInstance(context: Context): VortexDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VortexDatabase::class.java,
                    "vortex_battlegrounds.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
