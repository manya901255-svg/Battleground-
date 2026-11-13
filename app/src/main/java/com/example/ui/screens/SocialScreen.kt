package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.GameRepository
import com.example.data.MatchHistoryEntity
import com.example.data.MissionEntity
import com.example.data.UserProfileEntity
import com.example.ui.theme.AirdropGold
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.ElectricBlueSafe
import com.example.ui.theme.HazardYellow
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.RedZoneBombardment
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
    onBack: () -> Unit
) {
    val history by repository.matchHistory.collectAsState(initial = emptyList())
    val profile by repository.userProfile.collectAsState(initial = null)
    val missions by repository.missions.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    // Squad / Friend interactive states
    val allFriends = remember {
        listOf(
            FriendEntry("f1", "Viper_42", "In Lobby • Squad Ready", true, "Ace"),
            FriendEntry("f2", "Shadow_X", "In Classic Match • Alive: 14", true, "Crown I"),
            FriendEntry("f3", "CyberGhost", "Online • Team Deathmatch", true, "Diamond II"),
            FriendEntry("f4", "AlphaStrike", "Last online 2 hours ago", false, "Crown IV"),
            FriendEntry("f5", "ApexQueen", "Offline", false, "Ace Master")
        )
    }
    val invitedFriends = remember { mutableStateMapOf<String, Boolean>() }
    val blockedFriendIds = remember { mutableStateListOf<String>() }
    val reportedFriendIds = remember { mutableStateListOf<String>() }

    // Private Room state
    var showPrivateRoomDialog by remember { mutableStateOf(false) }
    var privateRoomCode by remember { mutableStateOf(generateRoomCode()) }
    var privateRoomMode by remember { mutableStateOf("SQUAD (100P)") }
    var privateRoomMap by remember { mutableStateOf("Kavala Island") }

    // Squad Chat state
    val chatMessages = remember {
        mutableStateListOf(
            SquadChatMessage("Viper_42", "Ready for Classic BR hot drop at Military Base?", false),
            SquadChatMessage("CyberGhost", "I'll run sniper support with AWM.", false)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(TacticalSurface, TacticalDarkBg)
                )
            )
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
                    Text("OPERATIVE PROFILE & SOCIAL", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("CAREER STATS, SQUAD INVITES & PRIVATE ROOMS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                        Text(statusBanner!!, color = ElectricBlueSafe, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { showPrivateRoomDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberOrange),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("create_private_room_button")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Private Room", tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PRIVATE ROOM", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tabs
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
            val tabTitles = listOf("PROFILE", "FRIENDS & SQUAD", "CLAN", "LEADERBOARD", "HISTORY")
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.testTag("social_tab_${title.lowercase().replace(" ", "_")}"),
                    text = {
                        Text(
                            title,
                            fontSize = 10.sp,
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
                achievements = missions.filter { it.category == "ACHIEVEMENT" },
                onSaveUsername = { newName ->
                    scope.launch {
                        repository.updateUsername(newName)
                        statusBanner = "Operative callsign updated to $newName"
                    }
                },
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
                    statusBanner = if (nowInvited) {
                        "Squad invite dispatched to ${friend.name}"
                    } else {
                        "Cancelled invite to ${friend.name}"
                    }
                },
                onReportFriend = { friend ->
                    if (friend.id !in reportedFriendIds) {
                        reportedFriendIds.add(friend.id)
                    }
                    statusBanner = "Reported ${friend.name} to Fair Play Enforcement"
                },
                onBlockFriend = { friend ->
                    if (friend.id !in blockedFriendIds) {
                        blockedFriendIds.add(friend.id)
                    }
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

            2 -> ClanTabContent(
                profile = profile,
                onCheckIn = {
                    statusBanner = "Clan daily check-in logged! +50 Clan Activity"
                }
            )

            3 -> LeaderboardTabContent(profile = profile)

            4 -> HistoryTabContent(history = history)
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
                statusBanner = "Private Room #$privateRoomCode active ($privateRoomMode • $privateRoomMap)"
            }
        )
    }
}

@Composable
private fun ProfileTabContent(
    profile: UserProfileEntity?,
    achievements: List<MissionEntity>,
    onSaveUsername: (String) -> Unit,
    onClaimAchievement: (String) -> Unit
) {
    val p = profile ?: UserProfileEntity()
    var isEditingName by remember { mutableStateOf(false) }
    var nameDraft by remember(p.username) { mutableStateOf(p.username) }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left Column: Identity Card & Level Progress
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(CyberOrange.copy(alpha = 0.55f), TacticalSurfaceVariant)
                                    )
                                )
                                .border(2.dp, CyberOrange, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Avatar", tint = Color.White, modifier = Modifier.size(32.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            if (isEditingName) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = nameDraft,
                                        onValueChange = { if (it.length <= 16) nameDraft = it },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("username_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedBorderColor = CyberOrange,
                                            unfocusedBorderColor = TextSecondary
                                        )
                                    )
                                    IconButton(
                                        onClick = {
                                            if (nameDraft.isNotBlank()) {
                                                onSaveUsername(nameDraft.trim())
                                            }
                                            isEditingName = false
                                        },
                                        modifier = Modifier.testTag("save_username_button")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Save Username", tint = HealthGreen)
                                    }
                                    IconButton(onClick = {
                                        nameDraft = p.username
                                        isEditingName = false
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Cancel Edit", tint = RedZoneBombardment)
                                    }
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        p.username,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            nameDraft = p.username
                                            isEditingName = true
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("edit_username_button")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Username", tint = CyberOrange, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Text("ID: VTX-1049-882 • ${p.title}", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Level & XP Progress
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("LEVEL ${p.level}", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        Text("${p.xp} / ${p.maxXp} XP", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    LinearProgressIndicator(
                        progress = { (p.xp.toFloat() / p.maxXp.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = CyberOrange,
                        trackColor = TacticalSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Current Rank Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(TacticalSurfaceVariant)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = "Rank", tint = AirdropGold, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("SEASON RANK", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(p.rankTier.uppercase(), color = AirdropGold, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }
                        Text("${p.rankPoints} RP", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }

                // Economy summary
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("BATTLE POINTS", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("${p.bpCoins} BP", color = HazardYellow, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("VORTEX CREDITS", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("${p.vortexCredits} VC", color = ElectricBlueSafe, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Right Column: Career Metrics + Achievements
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val stats = listOf(
                Triple("MATCHES", "${p.matchesPlayed}", Icons.Default.History),
                Triple("VICTORIES", "${p.wins}", Icons.Default.EmojiEvents),
                Triple("K/D RATIO", String.format(Locale.US, "%.2f", p.kdRatio), Icons.Default.Whatshot),
                Triple("WIN RATE", String.format(Locale.US, "%.1f%%", p.winRatePercent), Icons.Default.Star),
                Triple("HEADSHOTS", "${p.headshots}", Icons.Default.GpsFixed),
                Triple("FAV WEAPON", "M416", Icons.Default.Shield)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(148.dp)
            ) {
                items(stats) { (label, value, icon) ->
                    ProfileStatTile(label = label, value = value, icon = icon)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("COMBAT ACHIEVEMENTS", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (achievements.isEmpty()) {
                        Text("Play matches to track career milestones.", color = TextSecondary, fontSize = 11.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(achievements) { ach ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(TacticalSurfaceVariant)
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ach.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
                                            Text("CLAIM", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                        }
                                        else -> Text("IN PROGRESS", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatTile(label: String, value: String, icon: ImageVector) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = TacticalSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, contentDescription = label, tint = CyberOrange, modifier = Modifier.size(13.dp))
                Text(label, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Text(value, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 1)
        }
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

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left: Friends & Invites
        LazyColumn(
            modifier = Modifier
                .weight(1.15f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(friends, key = { it.id }) { friend ->
                val isInvited = invitedFriends[friend.id] == true
                val isReported = friend.id in reportedIds
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (friend.isOnline) HealthGreen else Color.Gray)
                            )
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
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isInvited) HealthGreen else CyberOrange
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("invite_friend_${friend.id}")
                                ) {
                                    Text(
                                        if (isInvited) "INVITED ✓" else "INVITE",
                                        color = Color.Black,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onReportFriend(friend) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("report_friend_${friend.id}")
                            ) {
                                Icon(
                                    Icons.Default.Flag,
                                    contentDescription = "Report ${friend.name}",
                                    tint = if (isReported) HazardYellow else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onBlockFriend(friend) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("block_friend_${friend.id}")
                            ) {
                                Icon(
                                    Icons.Default.Block,
                                    contentDescription = "Block ${friend.name}",
                                    tint = RedZoneBombardment,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right: Squad Chat
        Card(
            modifier = Modifier
                .weight(0.95f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Group, contentDescription = "Squad Chat", tint = CyberOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SQUAD TACTICAL CHAT ($playerName)", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(chatMessages) { msg ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (msg.isSelf) CyberOrange.copy(alpha = 0.16f)
                                    else TacticalSurfaceVariant
                                )
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                msg.sender,
                                color = if (msg.isSelf) CyberOrange else ElectricBlueSafe,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                            Text(msg.text, color = TextPrimary, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Tactical Callouts
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
                        modifier = Modifier
                            .weight(1f)
                            .testTag("squad_chat_input"),
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
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("squad_chat_send_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Chat", tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ClanTabContent(
    profile: UserProfileEntity?,
    onCheckIn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TacticalSurface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("[VTX] VORTEX ELITE", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text("Clan Level 7 • 48/50 Members • Leader: VortexKing", color = TextSecondary, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("GLOBAL RANK #12", color = HazardYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Button(
                    onClick = onCheckIn,
                    colors = ButtonDefaults.buttonColors(containerColor = HealthGreen),
                    modifier = Modifier.testTag("clan_checkin_button")
                ) {
                    Text("DAILY CHECK-IN", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text("CLAN MOTTO: Swift, lethal, and tactical dominance in every zone.", color = TextPrimary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("YOUR CLAN ROLE: Elite Vanguard (${profile?.username ?: "VortexGhost"} • ${profile?.rankTier ?: "Crown III"})", color = ElectricBlueSafe, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text("CLAN PERKS: +10% BP Match Bonus • Exclusive [VTX] Tag • Weekly Supply Crate", color = HealthGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LeaderboardTabContent(profile: UserProfileEntity?) {
    val topPlayers = listOf(
        Triple(1, "ShroudGhost", "Conqueror • 8,420 RP"),
        Triple(2, "ApexViper", "Conqueror • 8,110 RP"),
        Triple(3, "VortexKing", "Ace Dominator • 7,850 RP"),
        Triple(4, "NinjaBlade", "Ace Master • 7,420 RP"),
        Triple(5, "SniperGod_99", "Ace • 7,100 RP")
    )
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(topPlayers) { (rank, name, tier) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
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
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(tier, color = CyberOrange, fontSize = 10.sp)
                            }
                        }
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Rank", tint = HazardYellow, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        // Highlight current operative rank
        val p = profile ?: UserProfileEntity()
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
            Text("YOUR STANDING • ${p.username}", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text("${p.rankTier} • ${p.rankPoints} RP", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun HistoryTabContent(history: List<MatchHistoryEntity>) {
    if (history.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No matches played yet. Start a match to record career stats!", color = TextSecondary, fontSize = 12.sp)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history) { match ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CREATE PRIVATE CUSTOM ROOM", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Room Code Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ROOM JOIN CODE", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(roomCode, color = HazardYellow, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 3.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = {
                            clipboard.setText(AnnotatedString(roomCode))
                            copied = true
                        }) {
                            Text(if (copied) "COPIED" else "COPY", color = ElectricBlueSafe, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = {
                            copied = false
                            onRegenerateCode()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate Room Code", tint = CyberOrange)
                        }
                    }
                }

                // Mode selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("SOLO (100P)", "DUO (100P)", "SQUAD (100P)").forEach { mode ->
                        val active = mode == selectedMode
                        Button(
                            onClick = { onSelectMode(mode) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) CyberOrange else TacticalSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text(mode, color = if (active) Color.Black else TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Map selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Kavala Island", "Sahara Dunes", "Frostbite Ridge").forEach { mapName ->
                        val active = mapName == selectedMap
                        Button(
                            onClick = { onSelectMap(mapName) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) ElectricBlueSafe else TacticalSurfaceVariant
                            ),
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

private fun generateRoomCode(): String {
    return (100000 + Random.nextInt(900000)).toString()
}
