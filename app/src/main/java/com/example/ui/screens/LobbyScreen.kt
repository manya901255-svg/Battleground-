package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import com.example.data.MissionEntity
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.UserProfileEntity
import com.example.data.cloud.CloudAnnouncement
import com.example.data.cloud.CloudMailMessage
import com.example.game.model.EmotePreset
import com.example.game.model.GameModeType
import com.example.game.model.WeatherType
import com.example.ui.theme.AirdropGold
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
import kotlin.random.Random

@Composable
fun LobbyScreen(
    profile: UserProfileEntity,
    repository: com.example.data.GameRepository? = null,
    selectedMode: GameModeType,
    selectedMap: com.example.game.map.MapConfig = com.example.game.map.MapCatalog.getKavalaIsland(),
    onSelectMap: (com.example.game.map.MapConfig) -> Unit = {},
    currentTheme: LobbyWallpaperTheme = LobbyWallpaperTheme.DARK_TACTICAL,
    onSelectTheme: (LobbyWallpaperTheme) -> Unit = {},
    onSelectMode: (GameModeType) -> Unit,
    selectedTeamSize: Int = 4,
    onSelectTeamSize: (Int) -> Unit = {},
    selectedWeather: WeatherType = WeatherType.CLEAR,
    onSelectWeather: (WeatherType) -> Unit = {},
    announcements: List<CloudAnnouncement> = emptyList(),
    mailList: List<CloudMailMessage> = emptyList(),
    onClaimMail: (CloudMailMessage) -> Unit = {},
    onSubmitSupportTicket: (subject: String, message: String) -> Unit = { _, _ -> },
    onStartMatch: () -> Unit,
    onNavigateToBattlePass: () -> Unit,
    onNavigateToShop: () -> Unit,
    onNavigateToCustomization: () -> Unit,
    onNavigateToSocial: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onPlayEmote: () -> Unit = {}
) {
    var isMatchmaking by remember { mutableStateOf(false) }
    var matchmakingPlayersFound by remember { mutableStateOf(1) }
    var isTpp by remember { mutableStateOf(true) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showMapSelectionDialog by remember { mutableStateOf(false) }
    var showDailyRewardToast by remember { mutableStateOf(false) }
    var showMissionsDialog by remember { mutableStateOf(false) }
    var showMailboxDialog by remember { mutableStateOf(false) }
    var showNewsDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var supportSubject by remember { mutableStateOf("") }
    var supportMessage by remember { mutableStateOf("") }
    var selectedEmote by remember { mutableStateOf(EmotePreset.SALUTE) }
    var soldierYaw by remember { mutableStateOf(0f) }
    var inviteToast by remember { mutableStateOf(false) }
    var selectedMissionTab by remember { mutableIntStateOf(0) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val missions: List<MissionEntity> by (repository?.missions ?: kotlinx.coroutines.flow.flowOf(emptyList<MissionEntity>())).collectAsState(initial = emptyList<MissionEntity>())

    // Character Idle Breathing Animation
    val infiniteTransition = rememberInfiniteTransition(label = "idle_breathing")
    val soldierScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "soldier_breath"
    )

    // Pulse Glow for START MATCH button
    val startButtonPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "start_pulse"
    )

    // Matchmaking progression (instant launch for Offline Practice & Training Ground)
    LaunchedEffect(isMatchmaking) {
        if (isMatchmaking) {
            if (selectedMode == GameModeType.OFFLINE_PRACTICE || selectedMode == GameModeType.TRAINING_GROUND) {
                matchmakingPlayersFound = selectedMode.maxPlayers
                delay(150)
            } else {
                matchmakingPlayersFound = 1
                while (matchmakingPlayersFound < selectedMode.maxPlayers) {
                    delay(35)
                    matchmakingPlayersFound += (3..8).random()
                }
                delay(300)
            }
            isMatchmaking = false
            onStartMatch()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalDarkBg)
            .testTag("landscape_lobby_root")
    ) {
        // 1. FULL-SCREEN 16:9 CINEMATIC BATTLEGROUND WALLPAPER
        if (currentTheme.drawableResId != null) {
            Image(
                painter = painterResource(id = currentTheme.drawableResId),
                contentDescription = currentTheme.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(currentTheme.fallbackGradient)
            )
        }

        // Ambient Cinematic Vignette & Lighting Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.65f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.82f)
                        )
                    )
                )
        )

        // Floating Embers / Heat Particles Effect
        Canvas(modifier = Modifier.fillMaxSize()) {
            val random = Random(42)
            for (i in 0..28) {
                val px = random.nextFloat() * size.width
                val py = random.nextFloat() * size.height
                val radius = 1.5f + random.nextFloat() * 2.5f
                drawCircle(
                    color = currentTheme.accentColor.copy(alpha = 0.25f + random.nextFloat() * 0.45f),
                    center = Offset(px, py),
                    radius = radius
                )
            }
        }

        // 2. CENTER: FREE FIRE MAX CYBER STAGE + STANDING SOLDIER + COMPANION PET
        val activePreset = remember(profile.selectedCharacterId) {
            com.example.game.model.CharacterPreset.fromId(profile.selectedCharacterId)
        }
        val activePet = remember(profile.selectedPetId) {
            com.example.game.model.PetPreset.fromId(profile.selectedPetId)
        }
        val petGlow = Color(activePet.glowRgb.first, activePet.glowRgb.second, activePet.glowRgb.third)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Diagonal Cyber Spotlight Beams & Hexagonal Stage Pedestal
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width * 0.5f
                drawLine(
                    color = Color(0xFF9C27B0).copy(alpha = 0.22f),
                    start = Offset(cx - 260f, 0f),
                    end = Offset(cx - 90f, size.height * 0.85f),
                    strokeWidth = 34f
                )
                drawLine(
                    color = HazardYellow.copy(alpha = 0.16f),
                    start = Offset(cx + 260f, 0f),
                    end = Offset(cx + 90f, size.height * 0.85f),
                    strokeWidth = 28f
                )
            }

            // Hexagonal Pedestal Disc
            Canvas(
                modifier = Modifier
                    .size(width = 330.dp, height = 62.dp)
                    .offset(y = 14.dp)
            ) {
                drawOval(
                    color = Color.Black.copy(alpha = 0.78f),
                    topLeft = Offset.Zero,
                    size = size
                )
                drawOval(
                    color = currentTheme.accentColor.copy(alpha = 0.55f),
                    topLeft = Offset(10f, 6f),
                    size = androidx.compose.ui.geometry.Size(size.width - 20f, size.height - 12f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                )
            }

            // Standing Soldier Render (Drag to Rotate 3D Stage Showcase)
            Image(
                painter = painterResource(id = R.drawable.vortex_hero_soldier_1794458613075),
                contentDescription = "Futuristic Special Forces Soldier",
                modifier = Modifier
                    .fillMaxHeight(0.92f)
                    .scale(soldierScale)
                    .graphicsLayer { rotationY = soldierYaw }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            soldierYaw = ((soldierYaw + dragAmount * 0.25f) % 360f)
                        }
                    }
                    .clickable {
                        val entries = EmotePreset.entries
                        selectedEmote = entries[(selectedEmote.ordinal + 1) % entries.size]
                        onPlayEmote()
                    },
                contentScale = ContentScale.Fit
            )

            // Companion Pet Orbiting Badge next to Soldier (tap to customize pet)
            if (profile.petVisible) {
                Row(
                    modifier = Modifier
                        .offset(x = 128.dp, y = (-72).dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(TacticalSurface.copy(alpha = 0.90f))
                        .border(1.5.dp, petGlow, RoundedCornerShape(14.dp))
                        .clickable { onNavigateToCustomization() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(activePet.bodyRgb.first, activePet.bodyRgb.second, activePet.bodyRgb.third))
                            .border(1.5.dp, petGlow, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("${activePet.displayName} • Lv.${profile.petLevel}", color = petGlow, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        Text("${activePreset.displayName} Active", color = TextSecondary, fontSize = 8.sp)
                    }
                }
            }

            // Quick Emote Trigger Pill (Cycles & Plays Emotes)
            Row(
                modifier = Modifier
                    .offset(y = (-45).dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TacticalSurface.copy(alpha = 0.85f))
                    .border(1.dp, currentTheme.accentColor, RoundedCornerShape(16.dp))
                    .clickable {
                        val entries = EmotePreset.entries
                        selectedEmote = entries[(selectedEmote.ordinal + 1) % entries.size]
                        onPlayEmote()
                    }
                    .padding(horizontal = 12.dp, vertical = 5.dp)
                    .testTag("lobby_emote_pill"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SportsKabaddi, contentDescription = "Emote", tint = currentTheme.accentColor, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("EMOTE: ${selectedEmote.label.uppercase()}", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 3. TOP BAR: PROFILE (LEFT) & CURRENCIES (RIGHT)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TOP-LEFT: Player Profile Capsule
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(TacticalSurface.copy(alpha = 0.90f))
                    .border(1.dp, CyberOrange.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                    .clickable { onNavigateToSocial() }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyberOrange)
                        .border(1.5.dp, HazardYellow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("V", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.username, color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LVL ${profile.level}", color = HazardYellow, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    Text(
                        text = "${profile.rankTier} • ${profile.rankPoints} RP",
                        color = currentTheme.accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // TOP-RIGHT: Currencies, Mail, Support, Notifications, Themes, Admin & Settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Gold (BP Coins)
                CurrencyCapsule(
                    label = "BP",
                    amount = "${profile.bpCoins}",
                    icon = Icons.Default.MonetizationOn,
                    iconColor = HazardYellow,
                    onClick = onNavigateToShop
                )

                // Diamonds (Vortex Credits)
                CurrencyCapsule(
                    label = "DIAMOND",
                    amount = "${profile.vortexCredits}",
                    icon = Icons.Default.Diamond,
                    iconColor = ElectricBlueSafe,
                    onClick = onNavigateToShop
                )

                // Mailbox Button
                val unreadMail = mailList.count { !it.isRead && !it.rewardClaimed }
                Box {
                    IconButton(
                        onClick = { showMailboxDialog = true },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(TacticalSurface.copy(alpha = 0.85f))
                            .testTag("mailbox_button")
                    ) {
                        Icon(Icons.Default.Email, contentDescription = "Mailbox", tint = if (unreadMail > 0) HazardYellow else TextPrimary, modifier = Modifier.size(18.dp))
                    }
                    if (unreadMail > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(CyberOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$unreadMail", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                // Customer Support Button
                IconButton(
                    onClick = { showSupportDialog = true },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(TacticalSurface.copy(alpha = 0.85f))
                        .testTag("support_button")
                ) {
                    Icon(Icons.Default.SupportAgent, contentDescription = "Customer Support", tint = ElectricBlueSafe, modifier = Modifier.size(18.dp))
                }

                // Notification / Daily Reward Bell
                IconButton(
                    onClick = { showDailyRewardToast = true },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(TacticalSurface.copy(alpha = 0.85f))
                        .testTag("notification_button")
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }

                // Theme Switcher Button
                IconButton(
                    onClick = { showThemeDialog = true },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(TacticalSurface.copy(alpha = 0.85f))
                        .border(1.dp, currentTheme.accentColor, CircleShape)
                        .testTag("theme_selector_button")
                ) {
                    Icon(Icons.Default.ColorLens, contentDescription = "Themes", tint = currentTheme.accentColor, modifier = Modifier.size(18.dp))
                }

                // Admin Button
                IconButton(
                    onClick = onNavigateToAdmin,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(TacticalSurface.copy(alpha = 0.85f))
                        .testTag("admin_button")
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin", tint = CyberOrange, modifier = Modifier.size(18.dp))
                }

                // Settings Button
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(TacticalSurface.copy(alpha = 0.85f))
                        .testTag("settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // 4. LEFT SIDEBAR: VERTICAL NAVIGATION RAIL
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(TacticalSurface.copy(alpha = 0.88f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SidebarNavIcon("EVENTS", Icons.Default.Whatshot, onClick = { showNewsDialog = true }, color = CyberOrange)
            SidebarNavIcon("MISSIONS", Icons.AutoMirrored.Filled.Assignment, onClick = { showMissionsDialog = true }, color = HazardYellow)
            SidebarNavIcon("ARMORY", Icons.Default.Security, onClick = onNavigateToCustomization, color = ElectricBlueSafe)
            SidebarNavIcon("STORE", Icons.Default.ShoppingBag, onClick = onNavigateToShop, color = AirdropGold)
            SidebarNavIcon("CLAN", Icons.Default.EmojiEvents, onClick = onNavigateToSocial, color = Color(0xFFE040FB))
            SidebarNavIcon("FRIENDS", Icons.Default.Group, onClick = onNavigateToSocial, color = HealthGreen)
        }

        // 5. RIGHT SIDEBAR: PASS & REWARDS CARDS
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .width(155.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Season Pass Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToBattlePass() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface.copy(alpha = 0.90f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberOrange)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Stars, contentDescription = "Pass", tint = HazardYellow, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("VORTEX PASS", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        Text("SEASON 1 • TIER 24", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                }
            }

            // Daily Rewards Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDailyRewardToast = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface.copy(alpha = 0.90f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, HealthGreen)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = "Daily", tint = HealthGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("DAILY REWARD", color = HealthGreen, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        Text("DAY 5: 1,500 BP", color = HazardYellow, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                }
            }
        }

        // 6. BOTTOM CONTROLS: TEAM SELECTOR (CENTER) & START MATCH (RIGHT)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Bottom-Left: Theme & Weather Switcher Capsules
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TacticalSurface.copy(alpha = 0.82f))
                        .border(1.dp, ElectricBlueSafe.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable {
                            val entries = WeatherType.entries
                            onSelectWeather(entries[(selectedWeather.ordinal + 1) % entries.size])
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("weather_cycle_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = "Weather", tint = ElectricBlueSafe, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WEATHER: ${selectedWeather.displayName.uppercase()}", color = ElectricBlueSafe, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TacticalSurface.copy(alpha = 0.75f))
                        .clickable { showThemeDialog = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ColorLens, contentDescription = "Theme", tint = currentTheme.accentColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(currentTheme.title.uppercase(), color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            // BOTTOM-CENTER: Team Mode (SOLO, DUO, SQUAD), Invite Squad & Map
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(TacticalSurface.copy(alpha = 0.92f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TeamModePill("SOLO", isSelected = selectedTeamSize == 1, onClick = { onSelectTeamSize(1) })
                    TeamModePill("DUO", isSelected = selectedTeamSize == 2, onClick = { onSelectTeamSize(2) })
                    TeamModePill("SQUAD", isSelected = selectedTeamSize == 4, onClick = { onSelectTeamSize(4) })
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isTpp) CyberOrange.copy(alpha = 0.25f) else TacticalSurface)
                            .clickable { isTpp = !isTpp }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(if (isTpp) "TPP" else "FPP", color = if (isTpp) CyberOrange else TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(HealthGreen.copy(alpha = 0.22f))
                            .clickable {
                                inviteToast = true
                                onNavigateToSocial()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("+ INVITE", color = HealthGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }

                // Selected Map & Mode Capsule
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(TacticalSurface.copy(alpha = 0.90f))
                        .border(1.dp, currentTheme.accentColor, RoundedCornerShape(12.dp))
                        .clickable { showMapSelectionDialog = true }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Map, contentDescription = "Map", tint = currentTheme.accentColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${selectedMap.name.uppercase()} • ${selectedMode.title.uppercase()}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // BOTTOM-RIGHT: LARGE ANIMATED START MATCH BUTTON
            Button(
                onClick = { isMatchmaking = true },
                enabled = !isMatchmaking,
                colors = ButtonDefaults.buttonColors(containerColor = CyberOrange),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .height(64.dp)
                    .width(180.dp)
                    .scale(if (!isMatchmaking) startButtonPulse else 1.0f)
                    .border(2.dp, HazardYellow, RoundedCornerShape(18.dp))
                    .testTag("start_match_button")
            ) {
                if (isMatchmaking) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("$matchmakingPlayersFound/${selectedMode.maxPlayers}", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = Color.Black, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("START MATCH", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }
    }

    // 7. INTERCHANGEABLE WALLPAPER THEME SELECTOR MODAL
    if (showThemeDialog) {
        Dialog(onDismissRequest = { showThemeDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
                border = androidx.compose.foundation.BorderStroke(2.dp, CyberOrange)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SELECT CINEMATIC 16:9 THEME WALLPAPER", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        IconButton(onClick = { showThemeDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(LobbyWallpaperTheme.values()) { theme ->
                            val isSelected = theme == currentTheme
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp)
                                    .clickable {
                                        onSelectTheme(theme)
                                        showThemeDialog = false
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, theme.accentColor) else null
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = theme.title,
                                        color = if (isSelected) theme.accentColor else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 2
                                    )
                                    Text(theme.subtitle, color = TextSecondary, fontSize = 9.sp, maxLines = 2)
                                    Text(
                                        text = if (isSelected) "ACTIVE" else "SELECT",
                                        color = if (isSelected) theme.accentColor else Color.Gray,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Daily Rewards Dialog
    if (showDailyRewardToast) {
        val now = System.currentTimeMillis()
        val oneDayMs = 24L * 60L * 60L * 1000L
        val canClaim = profile.lastDailyClaimTimestamp == 0L || (now - profile.lastDailyClaimTimestamp) >= oneDayMs

        Dialog(onDismissRequest = { showDailyRewardToast = false }) {
            Card(
                modifier = Modifier
                    .width(360.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
                border = androidx.compose.foundation.BorderStroke(2.dp, if (canClaim) HealthGreen else CyberOrange)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (canClaim) "DAILY LOGIN BONUS READY!" else "DAILY BONUS ALREADY CLAIMED",
                        color = if (canClaim) HazardYellow else TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (canClaim) "Day 5 Login Reward: +1,500 BP Coins!" else "You have already collected today's +1,500 BP Coins. Return tomorrow!",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (canClaim) {
                                scope.launch {
                                    repository?.claimDailyReward()
                                    showDailyRewardToast = false
                                }
                            } else {
                                showDailyRewardToast = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (canClaim) HealthGreen else CyberOrange),
                        modifier = Modifier.fillMaxWidth().testTag("collect_daily_reward_button")
                    ) {
                        Text(if (canClaim) "COLLECT +1,500 BP" else "CLOSE", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Authoritative Battlefield Missions Dialog (Daily, Weekly, Achievements)
    if (showMissionsDialog) {
        val categories = listOf("DAILY", "WEEKLY", "ACHIEVEMENT")
        val currentCategory = categories[selectedMissionTab]
        val filteredMissions = missions.filter { it.category == currentCategory }

        Dialog(onDismissRequest = { showMissionsDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.88f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
                border = androidx.compose.foundation.BorderStroke(2.dp, CyberOrange)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("BATTLEFIELD MISSIONS & OBJECTIVES", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            Text("COMPLETE GENUINE COMBAT OBJECTIVES TO CLAIM REWARDS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { showMissionsDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Tabs
                    TabRow(
                        selectedTabIndex = selectedMissionTab,
                        containerColor = TacticalSurface,
                        contentColor = CyberOrange
                    ) {
                        Tab(selected = selectedMissionTab == 0, onClick = { selectedMissionTab = 0 }, text = { Text("DAILY", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
                        Tab(selected = selectedMissionTab == 1, onClick = { selectedMissionTab = 1 }, text = { Text("WEEKLY", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
                        Tab(selected = selectedMissionTab == 2, onClick = { selectedMissionTab = 2 }, text = { Text("ACHIEVEMENTS", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredMissions) { m ->
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(m.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(m.description, color = TextSecondary, fontSize = 10.sp)
                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Progress Bar
                                        val frac = if (m.targetCount > 0) (m.currentProgress.toFloat() / m.targetCount.toFloat()).coerceIn(0f, 1f) else 0f
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            LinearProgressIndicator(
                                                progress = { frac },
                                                modifier = Modifier
                                                    .width(160.dp)
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = if (m.isCompleted) HealthGreen else CyberOrange,
                                                trackColor = Color.DarkGray
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${m.currentProgress} / ${m.targetCount}",
                                                color = if (m.isCompleted) HealthGreen else HazardYellow,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        // Reward labels
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (m.rewardBp > 0) Text("+${m.rewardBp} BP", color = HazardYellow, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            if (m.rewardCredits > 0) Text("+${m.rewardCredits} CREDITS", color = ElectricBlueSafe, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            if (m.rewardXp > 0) Text("+${m.rewardXp} XP", color = CyberOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Authoritative Claim Button
                                    if (m.isClaimed) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.DarkGray)
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("CLAIMED ✓", color = HealthGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else if (m.isCompleted) {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    repository?.claimMissionReward(m.id)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = HealthGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(34.dp).testTag("claim_mission_${m.id}")
                                        ) {
                                            Text("CLAIM", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        }
                                    } else {
                                        Button(
                                            onClick = { /* Cannot claim incomplete mission */ },
                                            enabled = false,
                                            colors = ButtonDefaults.buttonColors(disabledContainerColor = Color(0xFF263238)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("IN PROGRESS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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

    // MAP & GAME MODE SELECTION MODAL
    if (showMapSelectionDialog) {
        Dialog(onDismissRequest = { showMapSelectionDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.88f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
                border = androidx.compose.foundation.BorderStroke(2.dp, CyberOrange)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SELECT BATTLEGROUND MAP & MATCH MODE", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        IconButton(onClick = { showMapSelectionDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("ORIGINAL BATTLE ROYALE MAPS (100 PLAYERS)", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    // 5 Original Maps
                    val allMaps = remember { com.example.game.map.MapCatalog.getAllMaps() }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().height(115.dp)
                    ) {
                        items(allMaps) { map ->
                            val isSelected = map.id == selectedMap.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectMap(map) }
                                    .testTag("map_card_${map.id}"),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) CyberOrange.copy(alpha = 0.25f) else TacticalSurface
                                ),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, CyberOrange) else null
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(map.name, color = if (isSelected) CyberOrange else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(map.description, color = TextSecondary, fontSize = 8.sp, maxLines = 3)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("GAMEPLAY MODES", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Modes Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(GameModeType.entries) { mode ->
                            val isSelected = mode == selectedMode
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectMode(mode)
                                        showMapSelectionDialog = false
                                    }
                                    .testTag("mode_card_${mode.name}"),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) CyberOrange.copy(alpha = 0.25f) else TacticalSurface
                                ),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, CyberOrange) else null
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(mode.title, color = if (isSelected) CyberOrange else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(mode.subtitle, color = TextSecondary, fontSize = 9.sp, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // MAILBOX DIALOG
    if (showMailboxDialog) {
        Dialog(onDismissRequest = { showMailboxDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.8f)
                    .testTag("mailbox_dialog"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
                border = androidx.compose.foundation.BorderStroke(2.dp, ElectricBlueSafe)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("OPERATIVE MAILBOX", color = ElectricBlueSafe, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        IconButton(onClick = { showMailboxDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (mailList.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No dispatches in inbox yet.", color = TextSecondary, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(mailList, key = { it.id }) { mail ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(mail.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("From: ${mail.sender}", color = ElectricBlueSafe, fontSize = 10.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(mail.body, color = TextSecondary, fontSize = 11.sp)
                                            if (mail.rewardBp > 0L) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("+${mail.rewardBp} BP Supply Attachment", color = HazardYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        if (mail.rewardClaimed) {
                                            Text("CLAIMED ✓", color = HealthGreen, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                        } else {
                                            Button(
                                                onClick = { onClaimMail(mail) },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyberOrange),
                                                modifier = Modifier.height(34.dp).testTag("claim_mail_${mail.id}")
                                            ) {
                                                Text(if (mail.rewardBp > 0L) "CLAIM" else "MARK READ", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
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
    }

    // EVENTS & PATCH NOTES DIALOG
    if (showNewsDialog) {
        Dialog(onDismissRequest = { showNewsDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.8f)
                    .testTag("news_dialog"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
                border = androidx.compose.foundation.BorderStroke(2.dp, CyberOrange)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SEASON 54 EVENTS & PATCH NOTES", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        IconButton(onClick = { showNewsDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (announcements.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("PATCH 54.0 • WEATHER & REVIVE UPDATE", color = HazardYellow, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "• Dynamic Weather presets (Clear, Sunset Dusk, Heavy Fog, Monsoon Rain).\n• Squadmate knockdown & 3.0s revive mechanics live across Duo & Squad modes.\n• Interactive HUD Layout Studio added in Settings.",
                                            color = TextPrimary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            items(announcements, key = { it.id }) { ann ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("[${ann.severity}] ${ann.title}", color = HazardYellow, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(ann.message, color = TextPrimary, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // CUSTOMER SUPPORT DIALOG
    if (showSupportDialog) {
        Dialog(onDismissRequest = { showSupportDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .testTag("support_dialog"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
                border = androidx.compose.foundation.BorderStroke(2.dp, HealthGreen)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PLAYER SUPPORT DESK", color = HealthGreen, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        IconButton(onClick = { showSupportDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    OutlinedTextField(
                        value = supportSubject,
                        onValueChange = { if (it.length <= 80) supportSubject = it },
                        label = { Text("Subject") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("support_subject_input")
                    )
                    OutlinedTextField(
                        value = supportMessage,
                        onValueChange = { if (it.length <= 400) supportMessage = it },
                        label = { Text("Describe issue or feedback") },
                        minLines = 3,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag("support_message_input")
                    )
                    Button(
                        enabled = supportSubject.isNotBlank() && supportMessage.isNotBlank(),
                        onClick = {
                            onSubmitSupportTicket(supportSubject.trim(), supportMessage.trim())
                            supportSubject = ""
                            supportMessage = ""
                            showSupportDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HealthGreen),
                        modifier = Modifier.fillMaxWidth().testTag("submit_support_button")
                    ) {
                        Text("SUBMIT SUPPORT TICKET", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun CurrencyCapsule(
    label: String,
    amount: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(TacticalSurface.copy(alpha = 0.90f))
            .border(1.dp, iconColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(amount, color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
    }
}

@Composable
fun SidebarNavIcon(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    color: Color
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = TextPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TeamModePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) CyberOrange else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
    }
}
