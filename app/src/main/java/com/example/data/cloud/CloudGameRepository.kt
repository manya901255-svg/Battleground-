package com.example.data.cloud

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CloudGameRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun computeRankTier(points: Long): String = when {
        points >= 6500L -> "Grandmaster"
        points >= 5200L -> "Heroic"
        points >= 4000L -> "Diamond I"
        points >= 3100L -> "Platinum II"
        points >= 2300L -> "Gold I"
        points >= 1600L -> "Silver I"
        else -> "Bronze I"
    }

    suspend fun ensureUserInitialized(displayName: String?): Result<Unit> {
        return runCatching {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            val snap = userRef.get().await()
            val safeName = (displayName?.takeIf { it.isNotBlank() } ?: "Operative_${uid.take(5)}")
                .replace(Regex("[^A-Za-z0-9_ ]"), "")
                .trim()
                .ifBlank { "Operative" }
                .take(20)

            if (!snap.exists()) {
                val profilePayload = mapOf(
                    "userId" to uid,
                    "username" to safeName,
                    "bio" to "Battle in Style!",
                    "avatarId" to "avatar_tactical_1",
                    "level" to 1L,
                    "xp" to 0L,
                    "maxXp" to 1000L,
                    "bpCoins" to 12000L,
                    "vortexCredits" to 650L,
                    "rankTier" to "Bronze I",
                    "rankPoints" to 1000L,
                    "matchesPlayed" to 0L,
                    "wins" to 0L,
                    "kills" to 0L,
                    "deaths" to 0L,
                    "headshots" to 0L,
                    "assists" to 0L,
                    "title" to "Rookie Striker",
                    "selectedCharacterId" to "vortex_ghost",
                    "selectedPetId" to "draco_flame",
                    "petLevel" to 1L,
                    "petVisible" to true,
                    "likesCount" to 0L,
                    "seasonId" to "S54",
                    "guildId" to "",
                    "isBanned" to false,
                    "lastDailyClaimMs" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(profilePayload).await()

                // Seed leaderboard row
                db.collection("leaderboards").document(uid).set(
                    mapOf(
                        "userId" to uid,
                        "username" to safeName,
                        "rankTier" to "Bronze I",
                        "rankPoints" to 1000L,
                        "wins" to 0L,
                        "kills" to 0L,
                        "level" to 1L,
                        "region" to "GLOBAL",
                        "seasonId" to "S54",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                // Seed default missions
                val starterMissions = listOf(
                    Triple("m_daily_1", "DAILY", "First Strike" to ("Eliminate 3 opponents in any match" to ("KILLS" to 3L))),
                    Triple("m_daily_2", "DAILY", "Heavy Firepower" to ("Deal 400 combat damage" to ("DAMAGE" to 400L))),
                    Triple("m_weekly_1", "WEEKLY", "Apex Survivor" to ("Win 1 Battle Royale match" to ("WINS" to 1L))),
                    Triple("m_season_1", "SEASON", "Season 54 Veteran" to ("Complete 5 matches" to ("MATCHES" to 5L)))
                )
                starterMissions.forEach { (docId, category, pair) ->
                    val (title, inner) = pair
                    val (desc, target) = inner
                    val (targetType, targetCount) = target
                    userRef.collection("missions").document(docId).set(
                        mapOf(
                            "category" to category,
                            "title" to title,
                            "description" to desc,
                            "targetType" to targetType,
                            "targetCount" to targetCount,
                            "currentProgress" to 0L,
                            "rewardBp" to if (category == "DAILY") 500L else 1800L,
                            "rewardCredits" to if (category == "DAILY") 25L else 120L,
                            "rewardXp" to if (category == "DAILY") 250L else 800L,
                            "isClaimed" to false,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }

                // Seed starter welcome mail
                userRef.collection("mail").document("mail_welcome").set(
                    mapOf(
                        "title" to "Welcome to Season 54!",
                        "body" to "Claim your deployment starter allowance of 1,000 BP Coins and hit the battleground.",
                        "sender" to "Vortex Command",
                        "isRead" to false,
                        "rewardBp" to 1000L,
                        "rewardClaimed" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}")
        }
    }

    fun observeProfile(userId: String): Flow<CloudUserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap -> snap.toObject(CloudUserProfile::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLog(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    fun observeInventory(userId: String): Flow<List<CloudInventoryItem>> = flow {
        val path = "users/$userId/inventory"
        emitAll(
            db.collection("users").document(userId).collection("inventory")
                .snapshots()
                .map { it.toObjects(CloudInventoryItem::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeMissions(userId: String): Flow<List<CloudMission>> = flow {
        val path = "users/$userId/missions"
        emitAll(
            db.collection("users").document(userId).collection("missions")
                .snapshots()
                .map { it.toObjects(CloudMission::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeTransactions(userId: String): Flow<List<CloudTransaction>> = flow {
        val path = "users/$userId/transactions"
        emitAll(
            db.collection("users").document(userId).collection("transactions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(40)
                .snapshots()
                .map { it.toObjects(CloudTransaction::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeMatches(userId: String): Flow<List<CloudMatchRecord>> = flow {
        val path = "users/$userId/matches"
        emitAll(
            db.collection("users").document(userId).collection("matches")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .snapshots()
                .map { it.toObjects(CloudMatchRecord::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeMail(userId: String): Flow<List<CloudMailMessage>> = flow {
        val path = "users/$userId/mail"
        emitAll(
            db.collection("users").document(userId).collection("mail")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .snapshots()
                .map { it.toObjects(CloudMailMessage::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observePublicMatchRooms(): Flow<List<CloudMatchRoom>> =
        db.collection("matchRooms")
            .whereEqualTo("isPrivate", false)
            .snapshots()
            .map { it.toObjects(CloudMatchRoom::class.java, ServerTimestampBehavior.ESTIMATE) }

    fun observeLeaderboard(): Flow<List<CloudLeaderboardEntry>> =
        db.collection("leaderboards")
            .orderBy("rankPoints", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { it.toObjects(CloudLeaderboardEntry::class.java, ServerTimestampBehavior.ESTIMATE) }

    fun observeGuilds(): Flow<List<CloudGuild>> =
        db.collection("guilds")
            .orderBy("level", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { it.toObjects(CloudGuild::class.java, ServerTimestampBehavior.ESTIMATE) }

    fun observeGuildMessages(guildId: String): Flow<List<CloudGuildMessage>> {
        val uid = requireUserId()
        return db.collection("guilds").document(guildId).collection("messages")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(CloudGuildMessage::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt?.toDate()?.time ?: 0L }
                    .takeLast(60)
            }
    }

    fun observeAnnouncements(): Flow<List<CloudAnnouncement>> =
        db.collection("announcements")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .snapshots()
            .map { it.toObjects(CloudAnnouncement::class.java, ServerTimestampBehavior.ESTIMATE) }

    fun observeMySupportTickets(userId: String): Flow<List<CloudSupportTicket>> =
        db.collection("supportTickets")
            .whereEqualTo("reporterUid", userId)
            .snapshots()
            .map { it.toObjects(CloudSupportTicket::class.java, ServerTimestampBehavior.ESTIMATE) }

    fun observeMyReports(userId: String): Flow<List<CloudPlayerReport>> =
        db.collection("reports")
            .whereEqualTo("reporterUid", userId)
            .snapshots()
            .map { it.toObjects(CloudPlayerReport::class.java, ServerTimestampBehavior.ESTIMATE) }

    suspend fun purchaseDirectBundle(
        itemId: String,
        itemName: String,
        type: String,
        targetItemId: String,
        rarity: String,
        priceBp: Long,
        priceCredits: Long
    ): Result<String> {
        return runCatching {
            val uid = requireUserId()
            require(itemId.isNotBlank()) { "Invalid item ID." }
            require(priceBp >= 0L && priceCredits >= 0L) { "Price cannot be negative." }
            val userRef = db.collection("users").document(uid)
            val invRef = userRef.collection("inventory").document(itemId)
            val txRef = userRef.collection("transactions").document("tx_${UUID.randomUUID().toString().replace("-", "").take(16)}")

            db.runTransaction { tx ->
                val profileSnap = tx.get(userRef)
                check(profileSnap.exists()) { "Player profile not initialized." }
                val existingInv = tx.get(invRef)
                if (existingInv.exists()) {
                    throw IllegalStateException("You already own $itemName.")
                }

                val currentBp = profileSnap.getLong("bpCoins") ?: 0L
                val currentCredits = profileSnap.getLong("vortexCredits") ?: 0L
                if (currentBp < priceBp || currentCredits < priceCredits) {
                    throw IllegalStateException("Insufficient currency balance.")
                }

                tx.update(
                    userRef,
                    mapOf(
                        "bpCoins" to (currentBp - priceBp),
                        "vortexCredits" to (currentCredits - priceCredits),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    invRef,
                    mapOf(
                        "name" to itemName.take(80),
                        "type" to type,
                        "targetItemId" to targetItemId,
                        "rarity" to rarity,
                        "isEquipped" to false,
                        "level" to 1L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    txRef,
                    mapOf(
                        "txType" to "PURCHASE",
                        "itemId" to itemId,
                        "itemName" to itemName.take(80),
                        "bpDelta" to -priceBp,
                        "creditsDelta" to -priceCredits,
                        "status" to "COMPLETED",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            "Purchased $itemName!"
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/inventory/$itemId")
        }
    }

    suspend fun claimVerifiedMission(mission: CloudMission): Result<Unit> {
        return runCatching {
            val uid = requireUserId()
            require(mission.id.isNotBlank()) { "Mission ID missing." }
            require(!mission.isClaimed && mission.currentProgress >= mission.targetCount) {
                "Mission must reach target before claiming reward."
            }
            val userRef = db.collection("users").document(uid)
            val missionRef = userRef.collection("missions").document(mission.id)
            val txRef = userRef.collection("transactions").document("tx_mis_${UUID.randomUUID().toString().replace("-", "").take(14)}")

            db.runTransaction { tx ->
                val mSnap = tx.get(missionRef)
                val pSnap = tx.get(userRef)
                check(mSnap.exists() && pSnap.exists()) { "Mission or profile missing." }
                val claimed = mSnap.getBoolean("isClaimed") ?: false
                val progress = mSnap.getLong("currentProgress") ?: 0L
                val target = mSnap.getLong("targetCount") ?: 1L
                if (claimed || progress < target) {
                    throw IllegalStateException("Mission reward unavailable.")
                }

                val curBp = pSnap.getLong("bpCoins") ?: 0L
                val curCr = pSnap.getLong("vortexCredits") ?: 0L
                val curXp = pSnap.getLong("xp") ?: 0L
                val maxXp = (pSnap.getLong("maxXp") ?: 1000L).coerceAtLeast(100L)
                var curLvl = pSnap.getLong("level") ?: 1L
                var nextXp = curXp + mission.rewardXp
                var nextMax = maxXp
                while (nextXp >= nextMax) {
                    nextXp -= nextMax
                    curLvl += 1L
                    nextMax += 250L
                }

                tx.update(
                    missionRef,
                    mapOf(
                        "isClaimed" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    userRef,
                    mapOf(
                        "bpCoins" to (curBp + mission.rewardBp),
                        "vortexCredits" to (curCr + mission.rewardCredits),
                        "xp" to nextXp,
                        "maxXp" to nextMax,
                        "level" to curLvl,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    txRef,
                    mapOf(
                        "txType" to "MISSION_REWARD",
                        "itemId" to mission.id,
                        "itemName" to mission.title.take(80),
                        "bpDelta" to mission.rewardBp,
                        "creditsDelta" to mission.rewardCredits,
                        "status" to "COMPLETED",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Unit
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/missions/${mission.id}")
        }
    }

    suspend fun claimMailReward(mail: CloudMailMessage): Result<Unit> {
        return runCatching {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            val mailRef = userRef.collection("mail").document(mail.id)
            db.runTransaction { tx ->
                val mSnap = tx.get(mailRef)
                val pSnap = tx.get(userRef)
                check(mSnap.exists() && pSnap.exists()) { "Mail not found." }
                val alreadyClaimed = mSnap.getBoolean("rewardClaimed") ?: false
                val rewardBp = mSnap.getLong("rewardBp") ?: 0L
                tx.update(
                    mailRef,
                    mapOf(
                        "isRead" to true,
                        "rewardClaimed" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (!alreadyClaimed && rewardBp > 0L) {
                    val curBp = pSnap.getLong("bpCoins") ?: 0L
                    tx.update(
                        userRef,
                        mapOf(
                            "bpCoins" to (curBp + rewardBp),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Unit
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/mail/${mail.id}")
        }
    }

    suspend fun recordCloudMatch(
        gameMode: String,
        mapName: String,
        placement: Int,
        kills: Int,
        assists: Int,
        damage: Float,
        durationSec: Int
    ): Result<Unit> {
        return runCatching {
            val uid = requireUserId()
            val isVictory = placement == 1
            val rpDelta = when {
                isVictory -> 40 + kills * 3 + assists
                placement <= 10 -> 20 + kills * 2
                placement <= 30 -> 6
                else -> -10
            }.toLong()
            val bpEarned = ((101 - placement).coerceAtLeast(1) * 12 + kills * 55 + (damage * 0.15f).toInt()).coerceAtLeast(120).toLong()
            val xpEarned = (durationSec / 2 + kills * 45 + if (isVictory) 500 else 120).coerceAtLeast(150).toLong()

            val userRef = db.collection("users").document(uid)
            val matchId = "match_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val matchRef = userRef.collection("matches").document(matchId)

            db.runTransaction { tx ->
                val pSnap = tx.get(userRef)
                check(pSnap.exists()) { "User profile missing." }
                val oldBp = pSnap.getLong("bpCoins") ?: 0L
                val oldRp = pSnap.getLong("rankPoints") ?: 1000L
                val newRp = (oldRp + rpDelta).coerceAtLeast(1000L)
                val newTier = computeRankTier(newRp)
                val oldMatches = pSnap.getLong("matchesPlayed") ?: 0L
                val oldWins = pSnap.getLong("wins") ?: 0L
                val oldKills = pSnap.getLong("kills") ?: 0L
                val oldDeaths = pSnap.getLong("deaths") ?: 0L
                val oldAssists = pSnap.getLong("assists") ?: 0L
                var newXp = (pSnap.getLong("xp") ?: 0L) + xpEarned
                var newLvl = pSnap.getLong("level") ?: 1L
                var maxXp = (pSnap.getLong("maxXp") ?: 1000L).coerceAtLeast(100L)
                while (newXp >= maxXp) {
                    newXp -= maxXp
                    newLvl += 1L
                    maxXp += 250L
                }
                val uname = pSnap.getString("username") ?: "Operative"

                tx.set(
                    matchRef,
                    mapOf(
                        "gameMode" to gameMode.take(48),
                        "mapName" to mapName.take(48),
                        "placement" to placement.toLong(),
                        "totalPlayers" to 100L,
                        "kills" to kills.toLong(),
                        "assists" to assists.toLong(),
                        "damage" to damage.toDouble(),
                        "survivalTimeSec" to durationSec.toLong(),
                        "rankPointsDelta" to rpDelta,
                        "bpEarned" to bpEarned,
                        "xpEarned" to xpEarned,
                        "isVictory" to isVictory,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.update(
                    userRef,
                    mapOf(
                        "bpCoins" to (oldBp + bpEarned),
                        "rankPoints" to newRp,
                        "rankTier" to newTier,
                        "matchesPlayed" to (oldMatches + 1L),
                        "wins" to (oldWins + if (isVictory) 1L else 0L),
                        "kills" to (oldKills + kills.toLong()),
                        "deaths" to (oldDeaths + if (isVictory) 0L else 1L),
                        "assists" to (oldAssists + assists.toLong()),
                        "xp" to newXp,
                        "level" to newLvl,
                        "maxXp" to maxXp,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    db.collection("leaderboards").document(uid),
                    mapOf(
                        "userId" to uid,
                        "username" to uname,
                        "rankTier" to newTier,
                        "rankPoints" to newRp,
                        "wins" to (oldWins + if (isVictory) 1L else 0L),
                        "kills" to (oldKills + kills.toLong()),
                        "level" to newLvl,
                        "region" to "GLOBAL",
                        "seasonId" to "S54",
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
            }.await()

            // Advance active missions outside transaction so queries work cleanly
            val missionsSnap = userRef.collection("missions").get().await()
            missionsSnap.documents.forEach { doc ->
                val isClaimed = doc.getBoolean("isClaimed") ?: false
                if (!isClaimed) {
                    val targetType = doc.getString("targetType") ?: ""
                    val targetCount = doc.getLong("targetCount") ?: 1L
                    val curProg = doc.getLong("currentProgress") ?: 0L
                    val delta = when (targetType) {
                        "KILLS" -> kills.toLong()
                        "DAMAGE" -> damage.toLong()
                        "MATCHES" -> 1L
                        "WINS" -> if (isVictory) 1L else 0L
                        else -> 0L
                    }
                    if (delta > 0L) {
                        doc.reference.update(
                            mapOf(
                                "currentProgress" to (curProg + delta).coerceAtMost(targetCount),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }
            Unit
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/matches")
        }
    }

    suspend fun createMatchRoom(
        title: String,
        gameMode: String,
        mapId: String,
        teamSize: Int,
        maxPlayers: Int,
        isPrivate: Boolean,
        weather: String
    ): Result<String> {
        return runCatching {
            val uid = requireUserId()
            val roomId = "room_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val roomCode = roomId.takeLast(6).uppercase()
            val payload = mapOf(
                "roomCode" to roomCode,
                "hostUid" to uid,
                "title" to title.trim().ifBlank { "Tactical Room" }.take(48),
                "gameMode" to gameMode.take(40),
                "mapId" to mapId.take(40),
                "teamSize" to teamSize.toLong(),
                "maxPlayers" to maxPlayers.toLong(),
                "memberUids" to listOf(uid),
                "status" to "waiting",
                "isPrivate" to isPrivate,
                "region" to "AUTO",
                "weather" to weather.take(24),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("matchRooms").document(roomId).set(payload).await()
            roomId
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.CREATE, "matchRooms")
        }
    }

    suspend fun joinMatchRoom(room: CloudMatchRoom): Result<Unit> {
        return runCatching {
            val uid = requireUserId()
            val updatedMembers = (room.memberUids + uid).distinct().take(16)
            db.collection("matchRooms").document(room.id).update(
                mapOf(
                    "memberUids" to updatedMembers,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Unit
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.UPDATE, "matchRooms/${room.id}")
        }
    }

    suspend fun createGuild(name: String, tag: String, description: String): Result<String> {
        return runCatching {
            val uid = requireUserId()
            val guildId = "guild_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            db.collection("guilds").document(guildId).set(
                mapOf(
                    "name" to name.trim().take(32),
                    "tag" to tag.trim().uppercase().take(6),
                    "leaderUid" to uid,
                    "level" to 1L,
                    "xp" to 0L,
                    "memberUids" to listOf(uid),
                    "description" to description.trim().take(160),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("users").document(uid).update(
                mapOf("guildId" to guildId, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            guildId
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.CREATE, "guilds")
        }
    }

    suspend fun sendGuildMessage(guild: CloudGuild, senderName: String, text: String): Result<Unit> {
        return runCatching {
            val uid = requireUserId()
            val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            db.collection("guilds").document(guild.id).collection("messages").document(msgId).set(
                mapOf(
                    "senderUid" to uid,
                    "senderName" to senderName.take(24),
                    "text" to text.trim().take(240),
                    "memberUids" to guild.memberUids.take(50),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Unit
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.CREATE, "guilds/${guild.id}/messages")
        }
    }

    suspend fun submitSupportTicket(category: String, subject: String, description: String): Result<String> {
        return runCatching {
            val uid = requireUserId()
            val ticketId = "tkt_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            db.collection("supportTickets").document(ticketId).set(
                mapOf(
                    "reporterUid" to uid,
                    "category" to category.take(32),
                    "subject" to subject.trim().take(80),
                    "description" to description.trim().take(500),
                    "status" to "OPEN",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            ticketId
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.CREATE, "supportTickets")
        }
    }

    suspend fun submitPlayerReport(targetName: String, reason: String): Result<String> {
        return runCatching {
            val uid = requireUserId()
            val reportId = "rpt_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            db.collection("reports").document(reportId).set(
                mapOf(
                    "reporterUid" to uid,
                    "targetName" to targetName.trim().take(32),
                    "reason" to reason.trim().take(120),
                    "status" to "PENDING",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            reportId
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.CREATE, "reports")
        }
    }

    suspend fun publishAnnouncement(title: String, message: String, severity: String): Result<Unit> {
        return runCatching {
            val uid = requireUserId()
            val annId = "ann_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            db.collection("announcements").document(annId).set(
                mapOf(
                    "title" to title.trim().take(80),
                    "message" to message.trim().take(300),
                    "severity" to severity,
                    "createdByUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Unit
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.CREATE, "announcements")
        }
    }

    suspend fun claimAllEligibleMissions(missions: List<CloudMission>): Result<Int> {
        return runCatching {
            var claimedCount = 0
            missions.filter { !it.isClaimed && it.currentProgress >= it.targetCount }.forEach { m ->
                claimVerifiedMission(m).onSuccess { claimedCount++ }.getOrThrow()
            }
            claimedCount
        }
    }

    suspend fun toggleEquipInventoryItem(item: CloudInventoryItem): Result<Unit> {
        return runCatching {
            val uid = requireUserId()
            db.collection("users").document(uid)
                .collection("inventory").document(item.id)
                .update(
                    mapOf(
                        "isEquipped" to !item.isEquipped,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Unit
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/inventory/${item.id}")
        }
    }

    suspend fun updateLoadoutSelection(
        characterId: String,
        petId: String,
        petVisible: Boolean
    ): Result<Unit> {
        return runCatching {
            val uid = requireUserId()
            db.collection("users").document(uid).update(
                mapOf(
                    "selectedCharacterId" to characterId.take(64),
                    "selectedPetId" to petId.take(64),
                    "petVisible" to petVisible,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Unit
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
        }
    }

    suspend fun upgradePetLevel(): Result<Long> {
        return runCatching {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            val txRef = userRef.collection("transactions").document("tx_pet_${UUID.randomUUID().toString().replace("-", "").take(14)}")
            db.runTransaction { tx ->
                val snap = tx.get(userRef)
                check(snap.exists()) { "Profile missing." }
                val curLevel = snap.getLong("petLevel") ?: 1L
                if (curLevel >= 10L) throw IllegalStateException("Pet is already max level (Lv. 10).")
                val costBp = curLevel * 600L
                val curBp = snap.getLong("bpCoins") ?: 0L
                if (curBp < costBp) throw IllegalStateException("Need $costBp BP Coins to upgrade pet.")
                val nextLevel = curLevel + 1L
                tx.update(
                    userRef,
                    mapOf(
                        "bpCoins" to (curBp - costBp),
                        "petLevel" to nextLevel,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    txRef,
                    mapOf(
                        "txType" to "PET_UPGRADE",
                        "itemId" to "pet_lvl_$nextLevel",
                        "itemName" to "Companion Upgrade Lv.$nextLevel",
                        "bpDelta" to -costBp,
                        "creditsDelta" to 0L,
                        "status" to "COMPLETED",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                nextLevel
            }.await()
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
        }
    }

    suspend fun updateProfileBasics(username: String, bio: String): Result<Unit> {
        return runCatching {
            val uid = requireUserId()
            val safeUser = username.trim().ifBlank { "Operative" }.take(24)
            val safeBio = bio.trim().take(80)
            db.collection("users").document(uid).update(
                mapOf(
                    "username" to safeUser,
                    "bio" to safeBio,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("leaderboards").document(uid).set(
                mapOf(
                    "userId" to uid,
                    "username" to safeUser,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            Unit
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
        }
    }

    suspend fun claimDailyLoginBonus(): Result<String> {
        return runCatching {
            val uid = requireUserId()
            val nowMs = System.currentTimeMillis()
            val userRef = db.collection("users").document(uid)
            val txRef = userRef.collection("transactions").document("tx_daily_${UUID.randomUUID().toString().replace("-", "").take(12)}")
            db.runTransaction { tx ->
                val snap = tx.get(userRef)
                check(snap.exists()) { "Profile missing." }
                val lastClaim = snap.getLong("lastDailyClaimMs") ?: 0L
                if (nowMs - lastClaim < 20L * 60L * 60L * 1000L) {
                    throw IllegalStateException("Daily check-in already claimed today.")
                }
                val curBp = snap.getLong("bpCoins") ?: 0L
                val curCredits = snap.getLong("vortexCredits") ?: 0L
                tx.update(
                    userRef,
                    mapOf(
                        "bpCoins" to (curBp + 1200L),
                        "vortexCredits" to (curCredits + 40L),
                        "lastDailyClaimMs" to nowMs,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    txRef,
                    mapOf(
                        "txType" to "DAILY_CHECKIN",
                        "itemId" to "daily_checkin",
                        "itemName" to "Daily Check-In Supply",
                        "bpDelta" to 1200L,
                        "creditsDelta" to 40L,
                        "status" to "COMPLETED",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            "Claimed +1,200 BP & +40 Credits!"
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
        }
    }

    suspend fun joinGuild(guild: CloudGuild): Result<Unit> {
        return runCatching {
            val uid = requireUserId()
            val updated = (guild.memberUids + uid).distinct().take(50)
            db.collection("guilds").document(guild.id).update(
                mapOf("memberUids" to updated, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            db.collection("users").document(uid).update(
                mapOf("guildId" to guild.id, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Unit
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.UPDATE, "guilds/${guild.id}")
        }
    }

    suspend fun leaveGuild(guild: CloudGuild): Result<Unit> {
        return runCatching {
            val uid = requireUserId()
            val remaining = guild.memberUids.filter { it != uid }
            if (remaining.isNotEmpty()) {
                db.collection("guilds").document(guild.id).update(
                    mapOf("memberUids" to remaining, "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            }
            db.collection("users").document(uid).update(
                mapOf("guildId" to "", "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Unit
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.UPDATE, "guilds/${guild.id}")
        }
    }

    fun observeAllUsersAdmin(): Flow<List<CloudUserProfile>> =
        db.collection("users")
            .orderBy("rankPoints", Query.Direction.DESCENDING)
            .limit(60)
            .snapshots()
            .map { it.toObjects(CloudUserProfile::class.java, ServerTimestampBehavior.ESTIMATE) }

    fun observeAllSupportTicketsAdmin(): Flow<List<CloudSupportTicket>> =
        db.collection("supportTickets")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(40)
            .snapshots()
            .map { it.toObjects(CloudSupportTicket::class.java, ServerTimestampBehavior.ESTIMATE) }

    fun observeAllReportsAdmin(): Flow<List<CloudPlayerReport>> =
        db.collection("reports")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(40)
            .snapshots()
            .map { it.toObjects(CloudPlayerReport::class.java, ServerTimestampBehavior.ESTIMATE) }

    suspend fun resolveSupportTicket(ticketId: String): Result<Unit> {
        return runCatching {
            db.collection("supportTickets").document(ticketId).update(
                mapOf("status" to "RESOLVED", "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Unit
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.UPDATE, "supportTickets/$ticketId")
        }
    }

    suspend fun reviewPlayerReport(reportId: String, status: String): Result<Unit> {
        return runCatching {
            db.collection("reports").document(reportId).update(
                mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Unit
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.UPDATE, "reports/$reportId")
        }
    }

    suspend fun setUserBannedStatus(targetUserId: String, banned: Boolean): Result<Unit> {
        return runCatching {
            db.collection("users").document(targetUserId).update(
                mapOf("isBanned" to banned, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Unit
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.UPDATE, "users/$targetUserId")
        }
    }

    private fun unwrapAndLog(error: Throwable, operationType: OperationType, path: String, uid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
