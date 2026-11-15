package com.example.data.cloud

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CloudUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "VortexOperative",
    val bio: String = "Battle in Style!",
    val avatarId: String = "avatar_tactical_1",
    val level: Long = 1L,
    val xp: Long = 0L,
    val maxXp: Long = 1000L,
    val bpCoins: Long = 5000L,
    val vortexCredits: Long = 250L,
    val rankTier: String = "Bronze I",
    val rankPoints: Long = 1000L,
    val matchesPlayed: Long = 0L,
    val wins: Long = 0L,
    val kills: Long = 0L,
    val deaths: Long = 0L,
    val headshots: Long = 0L,
    val assists: Long = 0L,
    val title: String = "Rookie Striker",
    val selectedCharacterId: String = "vortex_ghost",
    val selectedPetId: String = "draco_flame",
    val petLevel: Long = 1L,
    val petVisible: Boolean = true,
    val likesCount: Long = 0L,
    val seasonId: String = "S54",
    val guildId: String = "",
    @get:PropertyName("isBanned") @set:PropertyName("isBanned")
    var isBanned: Boolean = false,
    val lastDailyClaimMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudInventoryItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val type: String = "WEAPON_SKIN",
    val targetItemId: String = "",
    val rarity: String = "COMMON",
    @get:PropertyName("isEquipped") @set:PropertyName("isEquipped")
    var isEquipped: Boolean = false,
    val level: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudMission(
    @DocumentId val id: String = "",
    val category: String = "DAILY",
    val title: String = "",
    val description: String = "",
    val targetType: String = "KILLS",
    val targetCount: Long = 1L,
    val currentProgress: Long = 0L,
    val rewardBp: Long = 0L,
    val rewardCredits: Long = 0L,
    val rewardXp: Long = 0L,
    @get:PropertyName("isClaimed") @set:PropertyName("isClaimed")
    var isClaimed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudTransaction(
    @DocumentId val id: String = "",
    val txType: String = "PURCHASE",
    val itemId: String = "",
    val itemName: String = "",
    val bpDelta: Long = 0L,
    val creditsDelta: Long = 0L,
    val status: String = "COMPLETED",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudMatchRecord(
    @DocumentId val id: String = "",
    val gameMode: String = "",
    val mapName: String = "",
    val placement: Long = 100L,
    val totalPlayers: Long = 100L,
    val kills: Long = 0L,
    val assists: Long = 0L,
    val damage: Double = 0.0,
    val survivalTimeSec: Long = 0L,
    val rankPointsDelta: Long = 0L,
    val bpEarned: Long = 0L,
    val xpEarned: Long = 0L,
    @get:PropertyName("isVictory") @set:PropertyName("isVictory")
    var isVictory: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudMailMessage(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val sender: String = "Vortex Command",
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false,
    val rewardBp: Long = 0L,
    val rewardClaimed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudLeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val rankTier: String = "Bronze I",
    val rankPoints: Long = 1000L,
    val wins: Long = 0L,
    val kills: Long = 0L,
    val level: Long = 1L,
    val region: String = "GLOBAL",
    val seasonId: String = "S54",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudMatchRoom(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val hostUid: String = "",
    val title: String = "",
    val gameMode: String = "CLASSIC_BR",
    val mapId: String = "kavala_island",
    val teamSize: Long = 4L,
    val maxPlayers: Long = 100L,
    val memberUids: List<String> = emptyList(),
    val status: String = "waiting",
    @get:PropertyName("isPrivate") @set:PropertyName("isPrivate")
    var isPrivate: Boolean = false,
    val region: String = "AUTO",
    val weather: String = "CLEAR",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudGuild(
    @DocumentId val id: String = "",
    val name: String = "",
    val tag: String = "",
    val leaderUid: String = "",
    val level: Long = 1L,
    val xp: Long = 0L,
    val memberUids: List<String> = emptyList(),
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudGuildMessage(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val text: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudSupportTicket(
    @DocumentId val id: String = "",
    val reporterUid: String = "",
    val category: String = "GAMEPLAY",
    val subject: String = "",
    val description: String = "",
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudPlayerReport(
    @DocumentId val id: String = "",
    val reporterUid: String = "",
    val targetName: String = "",
    val reason: String = "",
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudAnnouncement(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val severity: String = "INFO",
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
