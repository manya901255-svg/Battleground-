package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.GameRepository
import com.example.data.MatchHistoryEntity
import com.example.data.MissionEntity
import com.example.data.UserProfileEntity
import com.example.data.cloud.CloudGameRepository
import com.example.data.cloud.CloudGuild
import com.example.data.cloud.CloudGuildMessage
import com.example.data.cloud.CloudLeaderboardEntry
import com.example.data.cloud.CloudMatchRecord
import com.example.data.cloud.CloudMatchRoom
import com.example.data.cloud.CloudUserProfile
import com.example.game.model.CharacterPreset
import com.example.game.model.PetPreset
import com.example.game.model.RankTierBadge
import com.example.ui.components.Character3DShowcaseView
import com.example.ui.components.RankGuideDialog
import com.example.ui.components.RankTierCrest
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.ElectricBlueSafe
import com.example.ui.theme.HazardYellow
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.RedZoneBombardment
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

private data class FriendEntry(
    val id: String,
    val name: String,
    val status: String,
    val isOnline: Boolean,
    val rank: String
)

private data class SquadChatMessage(
    val sender: String,
    val text: String,
    val isSelf: Boolean
)

@Composable
fun SocialScreen(
    repository: GameRepository,
    cloudRepository: CloudGameRepository? = null,
    cloudProfile: CloudUserProfile? = null,
    initialTab: Int = 0,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val history by repository.matchHistory.collectAsState(initial = emptyList())
    val profile by repository.userProfile.collectAsState(initial = null)
    val missions by repository.missions.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val currentUid = remember { runCatching { Firebase.auth.currentUser?.uid }.getOrNull() }

    val leaderboardFlow = remember(cloudRepository) {
        cloudRepository?.observeLeaderboard()?.catch { emit(emptyList()) } ?: flowOf(emptyList())
    }
    val guildsFlow = remember(cloudRepository) {
        cloudRepository?.observeGuilds()?.catch { emit(emptyList()) } ?: flowOf(emptyList())
    }
    val publicRoomsFlow = remember(cloudRepository) {
        cloudRepository?.observePublicMatchRooms()?.catch { emit(emptyList()) } ?: flowOf(emptyList())
    }
    val cloudMatchesFlow = remember(cloudRepository, currentUid) {
        if (cloudRepository != null && !currentUid.isNullOrBlank()) {
            cloudRepository.observeMatches(currentUid).catch { emit(emptyList()) }
        } else {
            flowOf(emptyList())
        }
    }

    val cloudLeaderboard by leaderboardFlow.collectAsState(initial = emptyList())
    val cloudGuilds by guildsFlow.collectAsState(initial = emptyList())
    val publicRooms by publicRoomsFlow.collectAsState(initial = emptyList())
    val cloudMatches by cloudMatchesFlow.collectAsState(initial = emptyList())

    val activeGuild = remember(cloudGuilds, cloudProfile?.guildId, currentUid) {
        cloudGuilds.firstOrNull { g ->
            g.id == cloudProfile?.guildId || (currentUid != null && currentUid in g.memberUids)
        }
    }

    val guildMessagesFlow = remember(cloudRepository, activeGuild?.id) {
        if (cloudRepository != null && activeGuild != null) {
            cloudRepository.observeGuildMessages(activeGuild.id).catch { emit(emptyList()) }
        } else {
            flowOf(emptyList())
        }
    }
    val guildMessages by guildMessagesFlow.collectAsState(initial = emptyList())

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 5)) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    val allFriends = remember {
        listOf(
            FriendEntry("f1", "Viper_42", "In Lobby • Squad Ready", true, "Heroic"),
            FriendEntry("f2", "Shadow_X", "In Classic Match • Alive: 14", true, "Diamond I"),
            FriendEntry("f3", "CyberGhost", "Online • Clash Squad", true, "Platinum II"),
            FriendEntry("f4", "AlphaStrike", "Last online 2 hours ago", false, "Diamond II"),
            FriendEntry("f5", "ApexQueen", "Offline", false, "Grandmaster")
        )
    }
    val invitedFriends = remember { mutableStateMapOf<String, Boolean>() }
    val blockedFriendIds = remember { mutableStateListOf<String>() }
    val reportedFriendIds = remember { mutableStateListOf<String>() }

    var showPrivateRoomDialog by remember { mutableStateOf(false) }
    var privateRoomCode by remember { mutableStateOf(generateRoomCode()) }
    var privateRoomMode by remember { mutableStateOf("SQUAD (100P)") }
    var privateRoomMap by remember { mutableStateOf("Kavala Island") }
    var isTournamentRegistered by remember { mutableStateOf(false) }

    val chatMessages = remember {
        mutableStateListOf(
            SquadChatMessage("Viper_42", "Ready for Classic BR hot drop at Military Base?", false),
            SquadChatMessage("CyberGhost", "I'll run sniper support with AWM.", false)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(TacticalSurface, TacticalDarkBg)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("social_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("social_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text("OPERATIVE SOCIAL & ESPORTS", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("CAREER RANK, GUILDS, MATCH ROOMS & TOURNAMENTS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (statusBanner != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElectricBlueSafe.copy(alpha = 0.2f))
                            .border(1.dp, ElectricBlueSafe, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(statusBanner!!, color = ElectricBlueSafe, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                Button(
                    onClick = { showPrivateRoomDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberOrange),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("create_private_room_button")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Private Room", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PRIVATE ROOM", color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = TacticalSurface,
            contentColor = CyberOrange,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CyberOrange
                )
            }
        ) {
            val tabTitles = listOf("PROFILE", "SQUAD", "GUILD", "ROOMS & TOURNEY", "RANKING", "HISTORY")
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.testTag("social_tab_${title.lowercase().replace(" ", "_")}"),
                    text = {
                        Text(
                            title,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (selectedTab) {
            0 -> ProfileTabContent(
                profile = profile,
                cloudProfile = cloudProfile,
                achievements = missions.filter { it.category == "ACHIEVEMENT" },
                onSaveUsername = { newName ->
                    scope.launch {
                        repository.updateUsername(newName)
                        cloudRepository?.updateProfileBasics(newName, profile?.signatureBio ?: "Battle in Style!")
                        statusBanner = "Operative callsign updated to $newName"
                    }
                },
                onSaveBio = { newBio ->
                    scope.launch {
                        repository.updateSignatureBio(newBio)
                        cloudRepository?.updateProfileBasics(profile?.username ?: "Operative", newBio)
                        statusBanner = "Signature bio updated!"
                    }
                },
                onIncrementLikes = {
                    scope.launch {
                        repository.incrementProfileLikes()
                        statusBanner = "Profile liked!"
                    }
                },
                onCopyUid = { uid -> statusBanner = "Copied UID: $uid" },
                onClaimAchievement = { missionId ->
                    scope.launch {
                        val ok = repository.claimMissionReward(missionId)
                        statusBanner = if (ok) "Achievement reward claimed!" else "Objective not yet completed."
                    }
                }
            )

            1 -> FriendsAndSquadTabContent(
                friends = allFriends.filter { it.id !in blockedFriendIds },
                invitedFriends = invitedFriends,
                reportedIds = reportedFriendIds,
                chatMessages = chatMessages,
                playerName = profile?.username ?: "VortexGhost",
                onToggleInvite = { friend ->
                    val nowInvited = !(invitedFriends[friend.id] ?: false)
                    invitedFriends[friend.id] = nowInvited
                    statusBanner = if (nowInvited) "Squad invite sent to ${friend.name}" else "Cancelled invite to ${friend.name}"
                },
                onReportFriend = { friend ->
                    if (friend.id !in reportedFriendIds) {
                        reportedFriendIds.add(friend.id)
                    }
                    scope.launch {
                        cloudRepository?.submitPlayerReport(friend.name, "Unsportsmanlike / Fair Play review")
                    }
                    statusBanner = "Reported ${friend.name} to Fair Play Enforcement"
                },
                onBlockFriend = { friend ->
                    if (friend.id !in blockedFriendIds) blockedFriendIds.add(friend.id)
                    invitedFriends.remove(friend.id)
                    statusBanner = "Blocked ${friend.name}"
                },
                onSendChat = { msgText ->
                    val trimmed = msgText.trim()
                    if (trimmed.isNotEmpty()) {
                        chatMessages.add(
                            SquadChatMessage(
                                sender = profile?.username ?: "VortexGhost",
                                text = trimmed,
                                isSelf = true
                            )
                        )
                    }
                }
            )

            2 -> GuildTabContent(
                profile = profile,
                guilds = cloudGuilds,
                activeGuild = activeGuild,
                guildMessages = guildMessages,
                currentUid = currentUid,
                onCreateGuild = { name, tag, desc ->
                    scope.launch {
                        if (cloudRepository != null) {
                            cloudRepository.createGuild(name, tag, desc)
                                .onSuccess { statusBanner = "Created Guild [$tag] $name!" }
                                .onFailure { statusBanner = it.message ?: "Could not create guild" }
                        } else {
                            statusBanner = "Sign in with Google to create persistent Guilds."
                        }
                    }
                },
                onJoinGuild = { guild ->
                    scope.launch {
                        cloudRepository?.joinGuild(guild)
                            ?.onSuccess { statusBanner = "Joined [${guild.tag}] ${guild.name}!" }
                            ?.onFailure { statusBanner = it.message }
                    }
                },
                onLeaveGuild = { guild ->
                    scope.launch {
                        cloudRepository?.leaveGuild(guild)
                            ?.onSuccess { statusBanner = "Left ${guild.name}" }
                    }
                },
                onSendGuildMessage = { guild, text ->
                    scope.launch {
                        val sender = cloudProfile?.username ?: profile?.username ?: "Operative"
                        cloudRepository?.sendGuildMessage(guild, sender, text)
                    }
                }
            )

            3 -> RoomsAndTournamentTabContent(
                publicRooms = publicRooms,
                currentUid = currentUid,
                isTournamentRegistered = isTournamentRegistered,
                onCreatePublicRoom = { title, mode, map ->
                    scope.launch {
                        if (cloudRepository != null) {
                            cloudRepository.createMatchRoom(
                                title = title,
                                gameMode = mode,
                                mapId = map,
                                teamSize = 4,
                                maxPlayers = 100,
                                isPrivate = false,
                                weather = "CLEAR"
                            ).onSuccess { statusBanner = "Public Room created & listed live!" }
                                .onFailure { statusBanner = it.message }
                        } else {
                            statusBanner = "Sign in to publish cloud rooms."
                        }
                    }
                },
                onJoinRoom = { room ->
                    scope.launch {
                        cloudRepository?.joinMatchRoom(room)
                            ?.onSuccess { statusBanner = "Joined Room #${room.roomCode} (${room.title})" }
                            ?.onFailure { statusBanner = it.message }
                    }
                },
                onRegisterTournament = {
                    isTournamentRegistered = true
                    statusBanner = "Registered for Season 54 Vortex Cup Qualifier!"
                }
            )

            4 -> LeaderboardTabContent(
                profile = profile,
                cloudProfile = cloudProfile,
                cloudLeaderboard = cloudLeaderboard
            )

            5 -> HistoryTabContent(
                localHistory = history,
                cloudMatches = cloudMatches
            )
        }
    }

    if (showPrivateRoomDialog) {
        PrivateRoomDialog(
            roomCode = privateRoomCode,
            selectedMode = privateRoomMode,
            selectedMap = privateRoomMap,
            onSelectMode = { privateRoomMode = it },
            onSelectMap = { privateRoomMap = it },
            onRegenerateCode = { privateRoomCode = generateRoomCode() },
            onDismiss = { showPrivateRoomDialog = false },
            onConfirmCreate = {
                showPrivateRoomDialog = false
                scope.launch {
                    cloudRepository?.createMatchRoom(
                        title = "Private Lobby #$privateRoomCode",
                        gameMode = privateRoomMode,
                        mapId = privateRoomMap,
                        teamSize = if (privateRoomMode.startsWith("SOLO")) 1 else if (privateRoomMode.startsWith("DUO")) 2 else 4,
                        maxPlayers = 100,
                        isPrivate = true,
                        weather = "CLEAR"
                    )
                }
                statusBanner = "Private Room #$privateRoomCode active ($privateRoomMode • $privateRoomMap)"
            }
        )
    }
}

@Composable
private fun ProfileTabContent(
    profile: UserProfileEntity?,
    cloudProfile: CloudUserProfile?,
    achievements: List<MissionEntity>,
    onSaveUsername: (String) -> Unit,
    onSaveBio: (String) -> Unit,
    onIncrementLikes: () -> Unit,
    onCopyUid: (String) -> Unit,
    onClaimAchievement: (String) -> Unit
) {
    val p = profile ?: UserProfileEntity()
    val clipboard = LocalClipboardManager.current
    var isEditingName by remember { mutableStateOf(false) }
    var nameDraft by remember(p.username) { mutableStateOf(p.username) }
    var isEditingBio by remember { mutableStateOf(false) }
    var bioDraft by remember(p.signatureBio) { mutableStateOf(p.signatureBio) }
    var showAchievementsPanel by remember { mutableStateOf(false) }
    var showRankGuideDialog by remember { mutableStateOf(false) }

    val effectiveRp = cloudProfile?.rankPoints?.toInt() ?: p.rankPoints
    val activeRankBadge = remember(effectiveRp) { RankTierBadge.fromRp(effectiveRp) }
    val effectiveTier = RankTierBadge.formattedRankLabel(effectiveRp)
    val pet = remember(p.selectedPetId) { PetPreset.fromId(p.selectedPetId) }
    val preset = remember(p.selectedCharacterId) { CharacterPreset.fromId(p.selectedCharacterId) }
    val petGlow = Color(pet.glowRgb.first, pet.glowRgb.second, pet.glowRgb.third)
    val uidString = cloudProfile?.userId?.take(10)?.uppercase() ?: "WARX${p.id}7094"
    val rankFrac = ((effectiveRp - 1000).coerceAtLeast(0).toFloat() / 5500f).coerceIn(0f, 1f)
    val xpFrac = (p.xp.toFloat() / p.maxXp.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val topTenEst = (p.wins + (p.matchesPlayed - p.wins).coerceAtLeast(0) / 3).coerceAtMost(p.matchesPlayed)
    val avgPlacement = if (p.matchesPlayed > 0) String.format(Locale.US, "%.1f", (24f - (p.wins.toFloat() / p.matchesPlayed.coerceAtLeast(1)) * 18f).coerceIn(1.2f, 22.0f)) else "--"
    val avgDmg = if (p.matchesPlayed > 0) String.format(Locale.US, "%.1f", p.kdRatio * 138.4f + 85f) else "0.0"

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07090F)),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // LEFT: Obsidian 2x3 Career Dashboard + Optional Expandable Achievements Drawer
        Column(
            modifier = Modifier
                .weight(1.34f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showAchievementsPanel) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10141F))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("COMBAT ACHIEVEMENTS & BADGES", color = HazardYellow, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Button(
                                onClick = { showAchievementsPanel = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21293B)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("BACK TO OVERVIEW", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(achievements) { ach ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF171D2C))
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ach.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("${ach.currentProgress} / ${ach.targetCount} • +${ach.rewardCredits} VC", color = TextSecondary, fontSize = 9.sp)
                                    }
                                    when {
                                        ach.isClaimed -> Text("CLAIMED", color = HealthGreen, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                        ach.isCompleted -> Button(
                                            onClick = { onClaimAchievement(ach.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberOrange),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("CLAIM", color = Color.White, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                        }
                                        else -> Text("IN PROGRESS", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // TOP ROW: SEASON RANKED (wider) + COMBAT RECORD / NOTABLE SEASON
                Row(
                    modifier = Modifier.weight(1.15f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Season Ranked Slate Card
                    Column(
                        modifier = Modifier
                            .weight(1.45f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF1E263D), Color(0xFF101521))))
                            .border(1.dp, Color(0xFF2D3959), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SEASON 1  |  RANKED", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.8.sp)
                            Text("${effectiveRp} RP", color = HazardYellow, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showRankGuideDialog = true }
                                    .padding(vertical = 2.dp)
                                    .testTag("profile_rank_crest_button")
                            ) {
                                RankTierCrest(tier = activeRankBadge, size = 48.dp)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(effectiveTier.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    ObsidianMetricCell("WINS", "${p.wins}", Modifier.weight(1f))
                                    ObsidianMetricCell("TOP 10", "$topTenEst", Modifier.weight(1f))
                                    ObsidianMetricCell("MATCHES", "${p.matchesPlayed}", Modifier.weight(1f))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    ObsidianMetricCell("AVG PLACE", avgPlacement, Modifier.weight(1f))
                                    ObsidianMetricCell("K/D RATIO", String.format(Locale.US, "%.2f", p.kdRatio), Modifier.weight(1f))
                                    ObsidianMetricCell("AVG DMG", avgDmg, Modifier.weight(1f))
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            LinearProgressIndicator(
                                progress = { rankFrac },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = HazardYellow,
                                trackColor = Color(0xFF222A3D)
                            )
                            Text("Displays season career statistics across Classic & Ranked battlegrounds.", color = TextSecondary, fontSize = 7.sp)
                        }
                    }

                    // Notable Season / Bio & Quick Achievements Button
                    Column(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF28224A), Color(0xFF131523))))
                            .border(1.dp, Color(0xFF423872), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("NOTABLE SEASON", color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Black, fontSize = 10.sp)
                            Icon(Icons.Default.EmojiEvents, contentDescription = "Peak Season", tint = HazardYellow, modifier = Modifier.size(14.dp))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RankTierCrest(tier = activeRankBadge, size = 38.dp)
                            Column {
                                Text(effectiveTier.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                Text("WARX SQUAD • ${preset.displayName}", color = ElectricBlueSafe, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (isEditingBio) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = bioDraft,
                                    onValueChange = { if (it.length <= 48) bioDraft = it },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("signature_bio_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = CyberOrange,
                                        unfocusedBorderColor = TextSecondary
                                    )
                                )
                                IconButton(
                                    onClick = {
                                        if (bioDraft.isNotBlank()) onSaveBio(bioDraft)
                                        isEditingBio = false
                                    },
                                    modifier = Modifier.testTag("save_bio_button")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Save Bio", tint = HealthGreen)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .clickable { bioDraft = p.signatureBio; isEditingBio = true }
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(p.signatureBio, color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Edit, contentDescription = "Edit Signature", tint = HazardYellow, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }

                // BOTTOM ROW: 3 OBSIDIAN METRIC PANELS (SURVIVAL LEVEL | LAST WEAPON KILL | LAST MEDAL EARNED)
                Row(
                    modifier = Modifier.weight(0.95f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Survival Level
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF10141F))
                            .border(1.dp, Color(0xFF232C40), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SURVIVAL LEVEL", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 0.6.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1A2233))
                                    .border(1.2.dp, ElectricBlueSafe, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${p.level}", color = ElectricBlueSafe, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Lv.${p.level}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                LinearProgressIndicator(
                                    progress = { xpFrac },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = HazardYellow,
                                    trackColor = Color(0xFF252E42)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${p.xp}/${p.maxXp} XP", color = TextSecondary, fontSize = 8.sp)
                            }
                        }
                        Text("TIER ${(p.level / 10).coerceAtLeast(1)}  •  ${p.bpCoins} BP", color = HazardYellow, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }

                    // 2. Last Weapon Kill
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF10141F))
                            .border(1.dp, Color(0xFF232C40), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("FAVORITE WEAPON", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Black, fontSize = 9.sp)
                            Text(p.equippedWeaponId.uppercase(), color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 9.sp)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF171E2E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Whatshot, contentDescription = p.equippedWeaponId, tint = HazardYellow, modifier = Modifier.size(20.dp))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL KILLS", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("X ${p.kills}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }

                    // 3. Last Medal Earned + Tap to View All Achievements
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF10141F))
                            .border(1.dp, HazardYellow.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .clickable { showAchievementsPanel = true }
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("LAST MEDAL EARNED", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Black, fontSize = 9.sp)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(HazardYellow.copy(alpha = 0.35f), Color(0xFF1A1610))))
                                .border(2.dp, HazardYellow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("#1", color = HazardYellow, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                        Text("VICTORY ROYALE • ACHIEVE (${achievements.count { it.isCompleted }})", color = ElectricBlueSafe, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // RIGHT: Live 3D Operative + Tactical Crate Showcase + UID & Edit Profile Plaque
        Box(
            modifier = Modifier
                .weight(0.92f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF131722), Color(0xFF090B10))))
                .border(1.dp, Color(0xFF283147), RoundedCornerShape(10.dp))
        ) {
            Character3DShowcaseView(
                characterPresetId = p.selectedCharacterId,
                weaponId = p.equippedWeaponId,
                weaponSkin = "Crimson Bloodline",
                animMode = "IDLE",
                autoRotate = false,
                showSupplyCrate = true,
                modifier = Modifier.fillMaxSize().padding(bottom = 52.dp)
            )

            // Companion Pet Chip Top-Left
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, petGlow.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Icons.Default.Pets, contentDescription = pet.displayName, tint = petGlow, modifier = Modifier.size(13.dp))
                Text("${preset.displayName.uppercase()} • ${pet.displayName.uppercase()} Lv.${p.petLevel}", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }

            // Bottom Action & Player Identity Overlay matching obsidian profile layout
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE080A0F), Color(0xFF06080C))))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1B2130))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .clickable {
                                clipboard.setText(AnnotatedString(uidString))
                                onCopyUid(uidString)
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy UID", tint = ElectricBlueSafe, modifier = Modifier.size(12.dp))
                        Text("UID: $uidString", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1B2130))
                            .border(1.dp, HazardYellow.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .clickable { onIncrementLikes() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("like_profile_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.ThumbUp, contentDescription = "Like Profile", tint = HazardYellow, modifier = Modifier.size(12.dp))
                        Text("${p.likesCount}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }

                if (isEditingName) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = nameDraft,
                            onValueChange = { if (it.length <= 16) nameDraft = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("username_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = HazardYellow,
                                unfocusedBorderColor = TextSecondary
                            )
                        )
                        IconButton(
                            onClick = {
                                if (nameDraft.isNotBlank()) onSaveUsername(nameDraft.trim())
                                isEditingName = false
                            },
                            modifier = Modifier.testTag("save_username_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save Username", tint = HealthGreen)
                        }
                        IconButton(onClick = { nameDraft = p.username; isEditingName = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = RedZoneBombardment)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberOrange)
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text("WARX", color = Color.White, fontWeight = FontWeight.Black, fontSize = 9.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(p.username, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Lv.${p.level} • ${p.title}", color = HazardYellow, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1F2738))
                                .border(1.dp, HazardYellow, RoundedCornerShape(6.dp))
                                .clickable { nameDraft = p.username; isEditingName = true }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                .testTag("edit_username_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Username", tint = HazardYellow, modifier = Modifier.size(12.dp))
                            Text("EDIT PROFILE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }

    if (showRankGuideDialog) {
        RankGuideDialog(
            currentRp = effectiveRp,
            onDismiss = { showRankGuideDialog = false }
        )
    }
}

@Composable
private fun ObsidianMetricCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = TextSecondary, fontSize = 7.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun FriendsAndSquadTabContent(
    friends: List<FriendEntry>,
    invitedFriends: Map<String, Boolean>,
    reportedIds: List<String>,
    chatMessages: List<SquadChatMessage>,
    playerName: String,
    onToggleInvite: (FriendEntry) -> Unit,
    onReportFriend: (FriendEntry) -> Unit,
    onBlockFriend: (FriendEntry) -> Unit,
    onSendChat: (String) -> Unit
) {
    var chatDraft by remember { mutableStateOf("") }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyColumn(modifier = Modifier.weight(1.15f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(friends, key = { it.id }) { friend ->
                val isInvited = invitedFriends[friend.id] == true
                val isReported = friend.id in reportedIds
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (friend.isOnline) HealthGreen else Color.Gray))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("${friend.name} • ${friend.rank}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(friend.status, color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (friend.isOnline) {
                                Button(
                                    onClick = { onToggleInvite(friend) },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isInvited) HealthGreen else CyberOrange),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp).testTag("invite_friend_${friend.id}")
                                ) {
                                    Text(if (isInvited) "INVITED ✓" else "INVITE", color = if (isInvited) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            IconButton(
                                onClick = { onReportFriend(friend) },
                                modifier = Modifier.size(32.dp).testTag("report_friend_${friend.id}")
                            ) {
                                Icon(Icons.Default.Flag, contentDescription = "Report ${friend.name}", tint = if (isReported) HazardYellow else TextSecondary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { onBlockFriend(friend) },
                                modifier = Modifier.size(32.dp).testTag("block_friend_${friend.id}")
                            ) {
                                Icon(Icons.Default.Block, contentDescription = "Block ${friend.name}", tint = RedZoneBombardment, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.weight(0.95f).fillMaxHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Group, contentDescription = "Squad Chat", tint = CyberOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SQUAD TACTICAL CHAT ($playerName)", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.3f)).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(chatMessages) { msg ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (msg.isSelf) CyberOrange.copy(alpha = 0.16f) else TacticalSurfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(msg.sender, color = if (msg.isSelf) CyberOrange else ElectricBlueSafe, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            Text(msg.text, color = TextPrimary, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Ready up!", "Drop Pochinki", "Need sniper").forEach { quick ->
                        OutlinedButton(
                            onClick = { onSendChat(quick) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(quick, color = TextSecondary, fontSize = 9.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = chatDraft,
                        onValueChange = { chatDraft = it },
                        placeholder = { Text("Type squad message...", fontSize = 11.sp, color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("squad_chat_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberOrange,
                            unfocusedBorderColor = TextSecondary
                        )
                    )
                    Button(
                        onClick = {
                            if (chatDraft.isNotBlank()) {
                                onSendChat(chatDraft)
                                chatDraft = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberOrange),
                        modifier = Modifier.height(48.dp).testTag("squad_chat_send_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Chat", tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GuildTabContent(
    profile: UserProfileEntity?,
    guilds: List<CloudGuild>,
    activeGuild: CloudGuild?,
    guildMessages: List<CloudGuildMessage>,
    currentUid: String?,
    onCreateGuild: (name: String, tag: String, description: String) -> Unit,
    onJoinGuild: (CloudGuild) -> Unit,
    onLeaveGuild: (CloudGuild) -> Unit,
    onSendGuildMessage: (CloudGuild, String) -> Unit
) {
    var showCreateForm by remember { mutableStateOf(false) }
    var guildName by remember { mutableStateOf("") }
    var guildTag by remember { mutableStateOf("") }
    var guildDesc by remember { mutableStateOf("") }
    var msgDraft by remember { mutableStateOf("") }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Left: Active Guild or Cloud Guild Directory
        Column(modifier = Modifier.weight(1.1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (activeGuild != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberOrange)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("[${activeGuild.tag}] ${activeGuild.name}", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            OutlinedButton(onClick = { onLeaveGuild(activeGuild) }, modifier = Modifier.height(30.dp), contentPadding = PaddingValues(horizontal = 10.dp)) {
                                Text("LEAVE", color = RedZoneBombardment, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Text("Level ${activeGuild.level} • ${activeGuild.memberUids.size} Members", color = HazardYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(activeGuild.description.ifBlank { "Tactical Guild Division" }, color = TextPrimary, fontSize = 11.sp)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("CLOUD GUILD DIRECTORY", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Button(
                        onClick = { showCreateForm = !showCreateForm },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberOrange),
                        modifier = Modifier.height(32.dp).testTag("toggle_create_guild_button")
                    ) {
                        Text(if (showCreateForm) "CANCEL" else "+ CREATE GUILD", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                }
                if (showCreateForm) {
                    Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = TacticalSurface)) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(value = guildName, onValueChange = { if (it.length <= 24) guildName = it }, label = { Text("Guild Name") }, singleLine = true, modifier = Modifier.weight(2f))
                                OutlinedTextField(value = guildTag, onValueChange = { if (it.length <= 5) guildTag = it.uppercase() }, label = { Text("Tag") }, singleLine = true, modifier = Modifier.weight(1f))
                            }
                            OutlinedTextField(value = guildDesc, onValueChange = { if (it.length <= 120) guildDesc = it }, label = { Text("Motto") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Button(
                                enabled = guildName.isNotBlank() && guildTag.isNotBlank(),
                                onClick = {
                                    onCreateGuild(guildName.trim(), guildTag.trim(), guildDesc.ifBlank { "Tactical Dominance" })
                                    guildName = ""; guildTag = ""; guildDesc = ""; showCreateForm = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HealthGreen),
                                modifier = Modifier.fillMaxWidth().testTag("confirm_create_guild_button")
                            ) {
                                Text("FOUND GUILD IN CLOUD", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (guilds.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(TacticalSurface).padding(12.dp)) {
                            Text("No cloud guilds found yet. Found your own guild above!", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                } else {
                    items(guilds, key = { it.id }) { guild ->
                        val isMember = currentUid != null && currentUid in guild.memberUids
                        Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = TacticalSurface)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("[${guild.tag}] ${guild.name}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Lv.${guild.level} • ${guild.memberUids.size} Members • ${guild.description}", color = TextSecondary, fontSize = 9.sp, maxLines = 1)
                                }
                                Button(
                                    enabled = !isMember,
                                    onClick = { onJoinGuild(guild) },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isMember) TacticalSurfaceVariant else ElectricBlueSafe),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(if (isMember) "JOINED" else "JOIN", color = if (isMember) HealthGreen else Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Right: Live Cloud Guild Chat
        Card(
            modifier = Modifier.weight(0.9f).fillMaxHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Text(
                    if (activeGuild != null) "LIVE GUILD CHANNEL • [${activeGuild.tag}]" else "JOIN A GUILD FOR LIVE CLOUD CHAT",
                    color = ElectricBlueSafe,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.35f)).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(guildMessages, key = { it.id }) { m ->
                        val isSelf = m.senderUid == currentUid
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelf) CyberOrange.copy(alpha = 0.16f) else TacticalSurfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(m.senderName, color = if (isSelf) CyberOrange else ElectricBlueSafe, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            Text(m.text, color = TextPrimary, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = msgDraft,
                        onValueChange = { msgDraft = it },
                        enabled = activeGuild != null,
                        placeholder = { Text(if (activeGuild != null) "Message guild..." else "Join guild first", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        enabled = activeGuild != null && msgDraft.isNotBlank(),
                        onClick = {
                            if (activeGuild != null && msgDraft.isNotBlank()) {
                                onSendGuildMessage(activeGuild, msgDraft)
                                msgDraft = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberOrange),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Guild Msg", tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomsAndTournamentTabContent(
    publicRooms: List<CloudMatchRoom>,
    currentUid: String?,
    isTournamentRegistered: Boolean,
    onCreatePublicRoom: (title: String, mode: String, map: String) -> Unit,
    onJoinRoom: (CloudMatchRoom) -> Unit,
    onRegisterTournament: () -> Unit
) {
    var newRoomTitle by remember { mutableStateOf("") }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Public Cloud Match Rooms Browser
        Column(modifier = Modifier.weight(1.1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = newRoomTitle,
                    onValueChange = { if (it.length <= 36) newRoomTitle = it },
                    placeholder = { Text("Host Public Match Room Title...", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        onCreatePublicRoom(newRoomTitle.ifBlank { "Season 54 Custom Squads" }, "CLASSIC_BR", "kavala_island")
                        newRoomTitle = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberOrange),
                    modifier = Modifier.height(48.dp).testTag("host_public_room_button")
                ) {
                    Icon(Icons.Default.Public, contentDescription = "Host Room", tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("HOST", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (publicRooms.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(TacticalSurface).padding(14.dp)) {
                            Text("No public rooms active right now. Tap HOST to create one!", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                } else {
                    items(publicRooms, key = { it.id }) { room ->
                        val joined = currentUid != null && currentUid in room.memberUids
                        Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = TacticalSurface)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${room.title} [#${room.roomCode}]", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("${room.gameMode} • ${room.mapId} • ${room.memberUids.size}/${room.maxPlayers} Joined", color = ElectricBlueSafe, fontSize = 9.sp)
                                }
                                Button(
                                    enabled = !joined,
                                    onClick = { onJoinRoom(room) },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (joined) TacticalSurfaceVariant else HealthGreen),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(if (joined) "JOINED ✓" else "JOIN", color = if (joined) HealthGreen else Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Season 54 Tournament Bracket Panel
        Card(
            modifier = Modifier.weight(0.9f).fillMaxHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, HazardYellow.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Tournament", tint = HazardYellow, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SEASON 54 VORTEX CUP", color = HazardYellow, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                    Text("PRIZE POOL: 5,000 VC + MYTHIC CHAMPION TITLE", color = CyberOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(TacticalDarkBg).padding(10.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("QUARTERFINALS: Team Apex vs Shadow Squad", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("SEMIFINALS: Vortex Elite vs Nova Strikers", color = ElectricBlueSafe, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("GRAND FINALS: Best-of-3 Classic BR (Kavala)", color = HealthGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Button(
                    enabled = !isTournamentRegistered,
                    onClick = onRegisterTournament,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isTournamentRegistered) TacticalSurfaceVariant else HazardYellow),
                    modifier = Modifier.fillMaxWidth().testTag("register_tournament_button")
                ) {
                    Text(
                        if (isTournamentRegistered) "REGISTERED FOR QUALIFIERS ✓" else "ENTER SEASON 54 QUALIFIERS",
                        color = if (isTournamentRegistered) HealthGreen else Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardTabContent(
    profile: UserProfileEntity?,
    cloudProfile: CloudUserProfile?,
    cloudLeaderboard: List<CloudLeaderboardEntry>
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("SEASON 54 GLOBAL RANKINGS (SERVER VERIFIED)", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 11.sp)
        if (cloudLeaderboard.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Play ranked matches while signed in to populate the live Season 54 leaderboard.", color = TextSecondary, fontSize = 12.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(cloudLeaderboard.size) { index ->
                    val entry = cloudLeaderboard[index]
                    val rank = index + 1
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = TacticalSurface)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "#$rank",
                                    color = when (rank) {
                                        1 -> HazardYellow
                                        2 -> Color(0xFFECEFF1)
                                        3 -> Color(0xFFFFB74D)
                                        else -> TextSecondary
                                    },
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(entry.username, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${entry.rankTier} • ${entry.rankPoints} RP • ${entry.wins} Wins", color = CyberOrange, fontSize = 10.sp)
                                }
                            }
                            Text("Lv.${entry.level} • ${entry.kills} Kills", color = ElectricBlueSafe, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        val uname = cloudProfile?.username ?: profile?.username ?: "Operative"
        val rTier = cloudProfile?.rankTier ?: profile?.rankTier ?: "Bronze I"
        val rPts = cloudProfile?.rankPoints?.toInt() ?: profile?.rankPoints ?: 1000
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CyberOrange.copy(alpha = 0.18f))
                .border(1.dp, CyberOrange, RoundedCornerShape(10.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("YOUR STANDING • $uname", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text("$rTier • $rPts RP", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun HistoryTabContent(
    localHistory: List<MatchHistoryEntity>,
    cloudMatches: List<CloudMatchRecord>
) {
    if (cloudMatches.isEmpty() && localHistory.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No matches played yet. Start a match to record career stats!", color = TextSecondary, fontSize = 12.sp)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (cloudMatches.isNotEmpty()) {
                items(cloudMatches, key = { "cloud_${it.id}" }) { match ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = TacticalSurface)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(
                                    if (match.isVictory) "VICTORY ROYALE (#1) [CLOUD]" else "#${match.placement} / ${match.totalPlayers} [CLOUD]",
                                    color = if (match.isVictory) HazardYellow else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text("${match.gameMode} • ${match.mapName}", color = TextSecondary, fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${match.kills}K • ${match.assists}A • ${match.damage.toInt()} DMG", color = CyberOrange, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("${match.rankPointsDelta} RP • +${match.bpEarned} BP", color = HealthGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(localHistory) { match ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = TacticalSurface)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(
                                    if (match.isVictory) "VICTORY ROYALE (#1)" else "#${match.placement} / ${match.totalPlayers}",
                                    color = if (match.isVictory) HazardYellow else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text("${match.gameMode} • ${match.mapName}", color = TextSecondary, fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${match.kills} KILLS • ${match.damage.toInt()} DMG", color = CyberOrange, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("${match.rankPointsDelta} RP • +${match.bpEarned} BP", color = HealthGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivateRoomDialog(
    roomCode: String,
    selectedMode: String,
    selectedMap: String,
    onSelectMode: (String) -> Unit,
    onSelectMap: (String) -> Unit,
    onRegenerateCode: () -> Unit,
    onDismiss: () -> Unit,
    onConfirmCreate: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberOrange.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                .testTag("private_room_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("CREATE PRIVATE CUSTOM ROOM", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.45f)).padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ROOM JOIN CODE", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(roomCode, color = HazardYellow, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 3.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { clipboard.setText(AnnotatedString(roomCode)); copied = true }) {
                            Text(if (copied) "COPIED" else "COPY", color = ElectricBlueSafe, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { copied = false; onRegenerateCode() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate Room Code", tint = CyberOrange)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("SOLO (100P)", "DUO (100P)", "SQUAD (100P)").forEach { mode ->
                        val active = mode == selectedMode
                        Button(
                            onClick = { onSelectMode(mode) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (active) CyberOrange else TacticalSurfaceVariant),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text(mode, color = if (active) Color.Black else TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Kavala Island", "Sahara Dunes", "Frostbite Ridge").forEach { mapName ->
                        val active = mapName == selectedMap
                        Button(
                            onClick = { onSelectMap(mapName) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (active) ElectricBlueSafe else TacticalSurfaceVariant),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text(mapName, color = if (active) Color.Black else TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onConfirmCreate,
                        colors = ButtonDefaults.buttonColors(containerColor = HealthGreen),
                        modifier = Modifier.testTag("confirm_private_room_button")
                    ) {
                        Text("OPEN PRIVATE LOBBY", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private fun generateRoomCode(): String = (100000 + Random.nextInt(900000)).toString()
