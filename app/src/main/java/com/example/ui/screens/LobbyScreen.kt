package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameRepository
import com.example.data.MissionEntity
import com.example.data.UserProfileEntity
import com.example.data.cloud.CloudAnnouncement
import com.example.data.cloud.CloudMailMessage
import com.example.game.graphics.GraphicsConfig
import com.example.game.map.MapCatalog
import com.example.game.map.MapConfig
import com.example.game.model.CharacterPreset
import com.example.game.model.EmotePreset
import com.example.game.model.GameModeType
import com.example.game.model.PetPreset
import com.example.game.model.WeatherType
import com.example.ui.lobby.Lobby3DStageView
import com.example.ui.lobby.LobbyBottomBar
import com.example.ui.lobby.LobbyFriendStatus
import com.example.ui.lobby.LobbyLeftSideMenu
import com.example.ui.lobby.LobbyMatchmakingOverlay
import com.example.ui.lobby.LobbyMissionsDialog
import com.example.ui.lobby.LobbyModeAndMapDialog
import com.example.ui.lobby.LobbyRightSideMenu
import com.example.ui.lobby.LobbyThemeAndUtilityModals
import com.example.ui.lobby.LobbyTopBar
import com.example.ui.lobby.LobbyVipAndSeasonDialog
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.ElectricBlueSafe
import com.example.ui.theme.HazardYellow
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.LobbyWallpaperTheme
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun LobbyScreen(
    profile: UserProfileEntity,
    repository: GameRepository? = null,
    selectedMode: GameModeType,
    selectedMap: MapConfig = MapCatalog.getKavalaIsland(),
    onSelectMap: (MapConfig) -> Unit = {},
    currentTheme: LobbyWallpaperTheme = LobbyWallpaperTheme.DARK_TACTICAL,
    onSelectTheme: (LobbyWallpaperTheme) -> Unit = {},
    onSelectMode: (GameModeType) -> Unit,
    selectedTeamSize: Int = 4,
    onSelectTeamSize: (Int) -> Unit = {},
    selectedWeather: WeatherType = WeatherType.CLEAR,
    onSelectWeather: (WeatherType) -> Unit = {},
    graphicsConfig: GraphicsConfig = GraphicsConfig.fromPreset("High"),
    announcements: List<CloudAnnouncement> = emptyList(),
    mailList: List<CloudMailMessage> = emptyList(),
    onClaimMail: (CloudMailMessage) -> Unit = {},
    onSubmitSupportTicket: (subject: String, message: String) -> Unit = { _, _ -> },
    onStartMatch: () -> Unit,
    onNavigateToBattlePass: () -> Unit,
    onNavigateToShop: () -> Unit,
    onNavigateToCustomization: () -> Unit,
    onNavigateToSocial: () -> Unit,
    onNavigateToSocialTab: (Int) -> Unit = { onNavigateToSocial() },
    onNavigateToCustomizationTab: (Int) -> Unit = { onNavigateToCustomization() },
    onNavigateToShopTab: (Int) -> Unit = { onNavigateToShop() },
    onNavigateToSettings: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onPlayEmote: () -> Unit = {}
) {
    var isMatchmaking by remember { mutableStateOf(false) }
    var matchmakingPlayersFound by remember { mutableIntStateOf(1) }
    var matchmakingElapsedSec by remember { mutableIntStateOf(0) }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showModeSelectionDialog by remember { mutableStateOf(false) }
    var showDailyDialog by remember { mutableStateOf(false) }
    var showMissionsDialog by remember { mutableStateOf(false) }
    var selectedMissionTab by remember { mutableIntStateOf(0) }
    var showMailboxDialog by remember { mutableStateOf(false) }
    var showNewsDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showVipDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var supportSubject by remember { mutableStateOf("") }
    var supportMessage by remember { mutableStateOf("") }

    var selectedEmote by remember { mutableStateOf(EmotePreset.SALUTE) }
    var emoteTriggerTick by remember { mutableIntStateOf(0) }
    var soldierYaw by remember { mutableFloatStateOf(0f) }
    var invitedFriendIds by remember { mutableStateOf(setOf<String>()) }
    var lobbyToastText by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val missions: List<MissionEntity> by (repository?.missions ?: flowOf(emptyList())).collectAsState(initial = emptyList())

    val onlineFriends = remember {
        listOf(
            LobbyFriendStatus("f1", "Viper_42", "Heroic", true, "In Lobby"),
            LobbyFriendStatus("f2", "Shadow_X", "Diamond I", true, "Squad Ready"),
            LobbyFriendStatus("f3", "CyberGhost", "Platinum II", true, "Online"),
            LobbyFriendStatus("f4", "ApexQueen", "Grandmaster", false, "Offline")
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "warx_lobby_anim")
    val startButtonPulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1150, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "start_btn_pulse"
    )

    // Real Matchmaking Queue Progression (supports Cancel cleanly)
    LaunchedEffect(isMatchmaking) {
        if (isMatchmaking) {
            matchmakingElapsedSec = 0
            if (selectedMode == GameModeType.OFFLINE_PRACTICE || selectedMode == GameModeType.TRAINING_GROUND) {
                matchmakingPlayersFound = selectedMode.maxPlayers
                delay(240)
            } else {
                matchmakingPlayersFound = 1
                var msAccum = 0
                while (isMatchmaking && matchmakingPlayersFound < selectedMode.maxPlayers) {
                    delay(80)
                    msAccum += 80
                    if (msAccum >= 1000) {
                        msAccum -= 1000
                        matchmakingElapsedSec += 1
                    }
                    matchmakingPlayersFound = (matchmakingPlayersFound + (5..13).random()).coerceAtMost(selectedMode.maxPlayers)
                }
                if (isMatchmaking) delay(280)
            }
            if (isMatchmaking) {
                isMatchmaking = false
                onStartMatch()
            }
        }
    }

    LaunchedEffect(lobbyToastText) {
        if (lobbyToastText != null) {
            delay(2600)
            lobbyToastText = null
        }
    }

    val unreadMailCount = mailList.count { !it.isRead && !it.rewardClaimed }
    val unclaimedMissionsCount = missions.count { it.isCompleted && !it.isClaimed }
    val dailyRewardReady = remember(profile.lastDailyClaimTimestamp) {
        profile.lastDailyClaimTimestamp == 0L || (System.currentTimeMillis() - profile.lastDailyClaimTimestamp) >= 86_400_000L
    }
    val notificationCount = unreadMailCount + unclaimedMissionsCount + announcements.size + (if (dailyRewardReady) 1 else 0)

    val activeCharacter = remember(profile.selectedCharacterId) { CharacterPreset.fromId(profile.selectedCharacterId) }
    val activePet = remember(profile.selectedPetId) { PetPreset.fromId(profile.selectedPetId) }
    val petGlow = Color(activePet.glowRgb.first, activePet.glowRgb.second, activePet.glowRgb.third)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalDarkBg)
            .testTag("landscape_lobby_root")
    ) {
        // 1. FULL-SCREEN 16:9 REAL-TIME 3D BATTLEFIELD STAGE + OPERATIVE SHOWCASE
        Lobby3DStageView(
            characterPresetId = profile.selectedCharacterId,
            petPresetId = profile.selectedPetId,
            petVisible = profile.petVisible,
            soldierYawOffset = soldierYaw,
            emoteTriggerTick = emoteTriggerTick,
            wallpaperTheme = currentTheme,
            weatherType = selectedWeather,
            selectedMap = selectedMap,
            graphicsConfig = graphicsConfig,
            modifier = Modifier.fillMaxSize()
        )

        // Interactive Drag-to-Rotate Gesture Layer for the 3D Operative
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        soldierYaw = (soldierYaw + dragAmount * 0.42f) % 360f
                    }
                }
        )

        // Atmospheric Edge Scrim for UI Legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0x88050912), Color.Transparent, Color.Transparent, Color(0x88050912))
                    )
                )
        )

        // 2. TOP NAVIGATION BAR
        LobbyTopBar(
            profile = profile,
            unreadMailCount = unreadMailCount,
            notificationCount = notificationCount,
            accentColor = currentTheme.accentColor,
            onOpenProfile = { onNavigateToSocialTab(0) },
            onOpenVip = { showVipDialog = true },
            onOpenStore = { onNavigateToShopTab(0) },
            onOpenFriends = { onNavigateToSocialTab(1) },
            onOpenMail = { showMailboxDialog = true },
            onOpenNotifications = { showNotificationsDialog = true },
            onOpenThemes = { showThemeDialog = true },
            onOpenAdmin = onNavigateToAdmin,
            onOpenSettings = onNavigateToSettings,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 3. LEFT SIDE MENU (Profile, Friends, Clan, Missions, Events, Ranking, Achievements, Mail)
        LobbyLeftSideMenu(
            unclaimedMissionsCount = unclaimedMissionsCount,
            unreadMailCount = unreadMailCount,
            onOpenProfile = { onNavigateToSocialTab(0) },
            onOpenFriends = { onNavigateToSocialTab(1) },
            onOpenClan = { onNavigateToSocialTab(2) },
            onOpenMissions = {
                selectedMissionTab = 0
                showMissionsDialog = true
            },
            onOpenEvents = { showNewsDialog = true },
            onOpenRanking = { onNavigateToSocialTab(4) },
            onOpenAchievements = {
                selectedMissionTab = 2
                showMissionsDialog = true
            },
            onOpenMail = { showMailboxDialog = true },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 10.dp)
        )

        // 4. CENTER-RIGHT SHOWCASE INFO PILLS (Operative Loadout + Companion Pet + Support Quick Link)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 150.dp, top = 75.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xBF0B111E))
                    .border(1.dp, currentTheme.accentColor.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .clickable { onNavigateToCustomizationTab(0) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(activeCharacter.displayName.uppercase(), color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("• ${activeCharacter.activeSkillName}", color = currentTheme.accentColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }

            if (profile.petVisible) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xBF0B111E))
                        .border(1.dp, petGlow.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .clickable { onNavigateToCustomizationTab(1) }
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(petGlow)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("${activePet.displayName} Lv.${profile.petLevel}", color = petGlow, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        // Support / Squad Invite Feed Pill (Bottom-Left above nav bar)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 152.dp, bottom = 92.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xB8090E18))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                .clickable { showSupportDialog = true }
                .padding(horizontal = 9.dp, vertical = 4.dp)
                .testTag("support_button"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.SupportAgent, contentDescription = "Support Desk", tint = ElectricBlueSafe, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = lobbyToastText ?: "[Squad] ${profile.username}: Drag stage to inspect operative • Tap for Support",
                color = if (lobbyToastText != null) HealthGreen else TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 5. RIGHT SIDE MENU (Battle Pass, Daily Login, Limited Events, Special Missions, Offers, Season Rewards, Friends)
        LobbyRightSideMenu(
            profile = profile,
            dailyRewardReady = dailyRewardReady,
            onlineFriends = onlineFriends,
            invitedFriendIds = invitedFriendIds,
            onInviteFriend = { friend ->
                invitedFriendIds = invitedFriendIds + friend.id
                lobbyToastText = "Squad invite dispatched to ${friend.name}!"
            },
            onOpenBattlePass = onNavigateToBattlePass,
            onOpenDailyRewards = { showDailyDialog = true },
            onOpenLimitedEvents = { showNewsDialog = true },
            onOpenSpecialMissions = {
                selectedMissionTab = 1
                showMissionsDialog = true
            },
            onOpenFeaturedOffers = { onNavigateToShopTab(0) },
            onOpenSeasonRewards = { showVipDialog = true },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(end = 10.dp, bottom = 26.dp)
        )

        // 6. BOTTOM NAVIGATION DOCK + GAME MODE SELECTOR + LARGE START BUTTON
        LobbyBottomBar(
            selectedMode = selectedMode,
            selectedMap = selectedMap,
            selectedTeamSize = selectedTeamSize,
            selectedWeather = selectedWeather,
            selectedEmote = selectedEmote,
            isMatchmaking = isMatchmaking,
            startPulse = startButtonPulse,
            onOpenCharacter = { onNavigateToCustomizationTab(0) },
            onOpenInventory = { onNavigateToCustomizationTab(3) },
            onOpenWeapons = { onNavigateToCustomizationTab(2) },
            onOpenStore = { onNavigateToShopTab(0) },
            onOpenPets = { onNavigateToCustomizationTab(1) },
            onOpenCustomization = { onNavigateToCustomizationTab(4) },
            onCycleWeather = {
                val entries = WeatherType.entries
                onSelectWeather(entries[(selectedWeather.ordinal + 1) % entries.size])
            },
            onTriggerEmote = {
                val entries = EmotePreset.entries
                selectedEmote = entries[(selectedEmote.ordinal + 1) % entries.size]
                emoteTriggerTick += 1
                onPlayEmote()
            },
            onSelectTeamSize = onSelectTeamSize,
            onOpenModeSelector = { showModeSelectionDialog = true },
            onStartMatch = { isMatchmaking = true },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 7. LIVE MATCHMAKING QUEUE OVERLAY
        if (isMatchmaking) {
            LobbyMatchmakingOverlay(
                selectedMode = selectedMode,
                selectedMap = selectedMap,
                selectedTeamSize = selectedTeamSize,
                playersFound = matchmakingPlayersFound,
                elapsedSec = matchmakingElapsedSec,
                onCancel = {
                    isMatchmaking = false
                    lobbyToastText = "Matchmaking cancelled."
                }
            )
        }
    }

    // 8. MODAL DIALOGS
    if (showModeSelectionDialog) {
        LobbyModeAndMapDialog(
            selectedMode = selectedMode,
            selectedMap = selectedMap,
            selectedTeamSize = selectedTeamSize,
            onSelectMap = onSelectMap,
            onSelectCoreMode = { mode, team ->
                onSelectMode(mode)
                if (team != null) onSelectTeamSize(team)
            },
            onDismiss = { showModeSelectionDialog = false }
        )
    }

    if (showVipDialog) {
        LobbyVipAndSeasonDialog(
            profile = profile,
            onOpenStore = { onNavigateToShopTab(0) },
            onOpenBattlePass = onNavigateToBattlePass,
            onDismiss = { showVipDialog = false }
        )
    }

    if (showMissionsDialog) {
        LobbyMissionsDialog(
            missions = missions,
            selectedTab = selectedMissionTab,
            onSelectTab = { selectedMissionTab = it },
            onClaimMission = { missionId ->
                scope.launch {
                    val claimed = repository?.claimMissionReward(missionId) ?: false
                    lobbyToastText = if (claimed) "Mission reward credited!" else "Complete mission objectives first."
                }
            },
            onDismiss = { showMissionsDialog = false }
        )
    }

    LobbyThemeAndUtilityModals(
        profile = profile,
        currentTheme = currentTheme,
        showThemeDialog = showThemeDialog,
        showDailyDialog = showDailyDialog,
        showMailboxDialog = showMailboxDialog,
        showNewsDialog = showNewsDialog,
        showNotificationsDialog = showNotificationsDialog,
        showSupportDialog = showSupportDialog,
        mailList = mailList,
        announcements = announcements,
        supportSubject = supportSubject,
        supportMessage = supportMessage,
        onSupportSubjectChange = { if (it.length <= 80) supportSubject = it },
        onSupportMessageChange = { if (it.length <= 400) supportMessage = it },
        onSelectTheme = onSelectTheme,
        onClaimDailyReward = {
            scope.launch {
                val ok = repository?.claimDailyReward() ?: false
                showDailyDialog = false
                lobbyToastText = if (ok) "Daily Login Bonus +1,500 BP claimed!" else "Already claimed today."
            }
        },
        onClaimMail = onClaimMail,
        onActivateLtmMode = {
            onSelectMode(GameModeType.LIMITED_TIME_BLITZ)
            showNewsDialog = false
            showNotificationsDialog = false
            lobbyToastText = "Blitz Hot-Drop (LTM) selected!"
        },
        onSubmitSupport = {
            onSubmitSupportTicket(supportSubject.trim(), supportMessage.trim())
            supportSubject = ""
            supportMessage = ""
            showSupportDialog = false
            lobbyToastText = "Support ticket submitted."
        },
        onDismissAll = {
            showThemeDialog = false
            showDailyDialog = false
            showMailboxDialog = false
            showNewsDialog = false
            showNotificationsDialog = false
            showSupportDialog = false
        }
    )
}
