package com.example.ui.lobby

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.MissionEntity
import com.example.data.UserProfileEntity
import com.example.data.cloud.CloudAnnouncement
import com.example.data.cloud.CloudMailMessage
import com.example.game.map.MapCatalog
import com.example.game.map.MapConfig
import com.example.game.model.GameModeType
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.ElectricBlueSafe
import com.example.ui.theme.HazardYellow
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.RedZoneBombardment
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.LobbyWallpaperTheme

@Composable
fun LobbyMatchmakingOverlay(
    selectedMode: GameModeType,
    selectedMap: MapConfig,
    selectedTeamSize: Int,
    playersFound: Int,
    elapsedSec: Int,
    onCancel: () -> Unit
) {
    val teamLabel = when (selectedTeamSize) {
        1 -> "SOLO"
        2 -> "DUO"
        else -> "SQUAD"
    }
    val progress = (playersFound.toFloat() / selectedMode.maxPlayers.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val statusText = when {
        playersFound >= selectedMode.maxPlayers -> "MATCH LOCKED • BOARDING DROPSHIP..."
        playersFound > selectedMode.maxPlayers / 2 -> "SYNCHRONIZING SQUADS & SERVER INSTANCE..."
        else -> "SEARCHING MATCHMAKING QUEUE..."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xBA050811))
            .testTag("matchmaking_status_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.width(420.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
            border = BorderStroke(2.dp, HazardYellow)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = HazardYellow, modifier = Modifier.size(30.dp), strokeWidth = 3.dp)
                    Column {
                        Text(statusText, color = HazardYellow, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text("${selectedMode.title.uppercase()} • $teamLabel • ${selectedMap.name.uppercase()}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = HealthGreen,
                    trackColor = TacticalSurface
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("PLAYERS: $playersFound / ${selectedMode.maxPlayers}", color = HealthGreen, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text("QUEUE TIME: 00:${elapsedSec.toString().padStart(2, '0')} • PING 24ms", color = ElectricBlueSafe, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onCancel,
                    border = BorderStroke(1.2.dp, RedZoneBombardment),
                    modifier = Modifier.fillMaxWidth().testTag("cancel_matchmaking_button")
                ) {
                    Text("CANCEL MATCHMAKING", color = RedZoneBombardment, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

private data class CoreModeShortcut(
    val label: String,
    val subtitle: String,
    val mode: GameModeType,
    val teamSize: Int?
)

@Composable
fun LobbyModeAndMapDialog(
    selectedMode: GameModeType,
    selectedMap: MapConfig,
    selectedTeamSize: Int,
    onSelectMap: (MapConfig) -> Unit,
    onSelectCoreMode: (GameModeType, Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val coreModes = listOf(
        CoreModeShortcut("Battle Royale Solo", "100P • Classic Individual Survival", GameModeType.CLASSIC_BR, 1),
        CoreModeShortcut("Battle Royale Duo", "100P • Two-Operative Tactical Pairs", GameModeType.CLASSIC_BR, 2),
        CoreModeShortcut("Battle Royale Squad", "100P • Four-Operative Squad War", GameModeType.CLASSIC_BR, 4),
        CoreModeShortcut("Ranked Mode", "Season 54 • Competitive RP Climb", GameModeType.RANKED_BR, null),
        CoreModeShortcut("Casual Mode", "Fast 4v4 Clash Squad Skirmish", GameModeType.CLASH_SQUAD, 4),
        CoreModeShortcut("Training Ground", "Weapons Lab, Recoil & Vehicles", GameModeType.TRAINING_GROUND, 1),
        CoreModeShortcut("Custom Room", "Host Tournament & Private Matches", GameModeType.CUSTOM_ROOM, null)
    )
    val allMaps = MapCatalog.getAllMaps()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.90f).testTag("mode_selection_dialog"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
            border = BorderStroke(2.dp, CyberOrange)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("WARX MODE & BATTLEGROUND SELECTOR", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Text("BATTLEGROUND MAPS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(92.dp)
                ) {
                    items(allMaps) { map ->
                        val active = map.id == selectedMap.id
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onSelectMap(map) }.testTag("map_card_${map.id}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = if (active) CyberOrange.copy(alpha = 0.24f) else TacticalSurface),
                            border = if (active) BorderStroke(1.8.dp, CyberOrange) else null
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(map.name, color = if (active) HazardYellow else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
                                Text(map.description, color = TextSecondary, fontSize = 8.sp, maxLines = 2)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("FEATURED BATTLE ROYALE & ARENA MODES", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(coreModes) { entry ->
                        val active = selectedMode == entry.mode && (entry.teamSize == null || selectedTeamSize == entry.teamSize)
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onSelectCoreMode(entry.mode, entry.teamSize)
                                onDismiss()
                            }.testTag("core_mode_${entry.label.lowercase().replace(" ", "_")}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = if (active) HazardYellow.copy(alpha = 0.22f) else TacticalSurface),
                            border = if (active) BorderStroke(1.8.dp, HazardYellow) else null
                        ) {
                            Column(modifier = Modifier.padding(9.dp)) {
                                Text(entry.label, color = if (active) HazardYellow else TextPrimary, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(entry.subtitle, color = TextSecondary, fontSize = 8.sp, maxLines = 2)
                            }
                        }
                    }
                    items(GameModeType.entries.filter {
                        it !in listOf(GameModeType.CLASSIC_BR, GameModeType.RANKED_BR, GameModeType.CLASH_SQUAD, GameModeType.TRAINING_GROUND, GameModeType.CUSTOM_ROOM)
                    }) { mode ->
                        val active = selectedMode == mode
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onSelectCoreMode(mode, null)
                                onDismiss()
                            }.testTag("mode_card_${mode.name}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = if (active) CyberOrange.copy(alpha = 0.24f) else TacticalSurface),
                            border = if (active) BorderStroke(1.8.dp, CyberOrange) else null
                        ) {
                            Column(modifier = Modifier.padding(9.dp)) {
                                Text(mode.title, color = if (active) CyberOrange else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(mode.subtitle, color = TextSecondary, fontSize = 8.sp, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LobbyVipAndSeasonDialog(
    profile: UserProfileEntity,
    onOpenStore: () -> Unit,
    onOpenBattlePass: () -> Unit,
    onDismiss: () -> Unit
) {
    val vipLevel = ((profile.level / 15) + (if (profile.vortexCredits >= 500) 1 else 0)).coerceIn(1, 5)
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(440.dp).testTag("vip_season_dialog"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
            border = BorderStroke(2.dp, HazardYellow)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = "VIP Status", tint = HazardYellow, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("VIP $vipLevel & SEASON 54 REWARDS", color = HazardYellow, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
                }
                Text("Current Standing: ${profile.rankTier} (${profile.rankPoints} RP) • Level ${profile.level}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("• VIP Perks Active: +${vipLevel * 5}% Gold Match Bonus, Priority Squad Emote Frame & Armory Showcase", color = ElectricBlueSafe, fontSize = 11.sp)
                Text("• Season Rewards: Reach Crown / Heroic for Exclusive Weapon Finishes & 2,500 BP Supply Crate", color = HealthGreen, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { onDismiss(); onOpenBattlePass() }, colors = ButtonDefaults.buttonColors(containerColor = CyberOrange), modifier = Modifier.weight(1f)) {
                        Text("WARX PASS", color = Color.White, fontWeight = FontWeight.Black)
                    }
                    OutlinedButton(onClick = { onDismiss(); onOpenStore() }, border = BorderStroke(1.dp, HazardYellow), modifier = Modifier.weight(1f)) {
                        Text("FEATURED OFFERS", color = HazardYellow, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LobbyMissionsDialog(
    missions: List<MissionEntity>,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onClaimMission: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val categories = listOf("DAILY", "WEEKLY", "ACHIEVEMENT")
    val filteredMissions = missions.filter { it.category == categories[selectedTab.coerceIn(0, 2)] }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.88f).testTag("missions_dialog"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
            border = BorderStroke(2.dp, CyberOrange)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("WARX MISSIONS & ACHIEVEMENTS", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text("SERVER-VERIFIED COMBAT OBJECTIVES", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TabRow(selectedTabIndex = selectedTab, containerColor = TacticalSurface, contentColor = CyberOrange) {
                    Tab(selected = selectedTab == 0, onClick = { onSelectTab(0) }, text = { Text("DAILY", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
                    Tab(selected = selectedTab == 1, onClick = { onSelectTab(1) }, text = { Text("SPECIAL / WEEKLY", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
                    Tab(selected = selectedTab == 2, onClick = { onSelectTab(2) }, text = { Text("ACHIEVEMENTS", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredMissions) { m ->
                        val frac = if (m.targetCount > 0) (m.currentProgress.toFloat() / m.targetCount.toFloat()).coerceIn(0f, 1f) else 0f
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = TacticalSurface)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(m.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(m.description, color = TextSecondary, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        LinearProgressIndicator(progress = { frac }, modifier = Modifier.width(150.dp).height(6.dp).clip(RoundedCornerShape(3.dp)), color = if (m.isCompleted) HealthGreen else CyberOrange, trackColor = Color.DarkGray)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${m.currentProgress}/${m.targetCount}", color = if (m.isCompleted) HealthGreen else HazardYellow, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (m.rewardBp > 0) Text("+${m.rewardBp} BP", color = HazardYellow, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        if (m.rewardCredits > 0) Text("+${m.rewardCredits} DIAMONDS", color = ElectricBlueSafe, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        if (m.rewardXp > 0) Text("+${m.rewardXp} XP", color = CyberOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (m.isClaimed) {
                                    Text("CLAIMED ✓", color = HealthGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                } else if (m.isCompleted) {
                                    Button(onClick = { onClaimMission(m.id) }, colors = ButtonDefaults.buttonColors(containerColor = HealthGreen), modifier = Modifier.height(34.dp).testTag("claim_mission_${m.id}")) {
                                        Text("CLAIM", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                } else {
                                    Button(onClick = {}, enabled = false, modifier = Modifier.height(34.dp)) {
                                        Text("LOCKED", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
fun LobbyThemeAndUtilityModals(
    profile: UserProfileEntity,
    currentTheme: LobbyWallpaperTheme,
    showThemeDialog: Boolean,
    showDailyDialog: Boolean,
    showMailboxDialog: Boolean,
    showNewsDialog: Boolean,
    showNotificationsDialog: Boolean,
    showSupportDialog: Boolean,
    mailList: List<CloudMailMessage>,
    announcements: List<CloudAnnouncement>,
    supportSubject: String,
    supportMessage: String,
    onSupportSubjectChange: (String) -> Unit,
    onSupportMessageChange: (String) -> Unit,
    onSelectTheme: (LobbyWallpaperTheme) -> Unit,
    onClaimDailyReward: () -> Unit,
    onClaimMail: (CloudMailMessage) -> Unit,
    onActivateLtmMode: () -> Unit,
    onSubmitSupport: () -> Unit,
    onDismissAll: () -> Unit
) {
    if (showThemeDialog) {
        Dialog(onDismissRequest = onDismissAll) {
            Card(modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.84f), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = TacticalDarkBg), border = BorderStroke(2.dp, CyberOrange)) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("3D BATTLEGROUND ATMOSPHERE & THEMES", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        IconButton(onClick = onDismissAll) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyVerticalGrid(columns = GridCells.Fixed(4), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                        items(LobbyWallpaperTheme.entries) { theme ->
                            val active = theme == currentTheme
                            Card(modifier = Modifier.fillMaxWidth().height(110.dp).clickable { onSelectTheme(theme); onDismissAll() }, shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = TacticalSurface), border = if (active) BorderStroke(2.dp, theme.accentColor) else null) {
                                Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                    Text(theme.title, color = if (active) theme.accentColor else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 2)
                                    Text(theme.subtitle, color = TextSecondary, fontSize = 8.sp, maxLines = 2)
                                    Text(if (active) "ACTIVE" else "SELECT", color = if (active) theme.accentColor else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDailyDialog) {
        val now = System.currentTimeMillis()
        val canClaim = profile.lastDailyClaimTimestamp == 0L || (now - profile.lastDailyClaimTimestamp) >= 86_400_000L
        Dialog(onDismissRequest = onDismissAll) {
            Card(modifier = Modifier.width(370.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = TacticalDarkBg), border = BorderStroke(2.dp, if (canClaim) HealthGreen else CyberOrange)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (canClaim) "DAILY LOGIN BONUS READY!" else "DAILY BONUS COLLECTED", color = if (canClaim) HazardYellow else TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(if (canClaim) "Claim Day 5 Supply Drop: +1,500 Gold Coins!" else "Today's +1,500 Gold Coins are already credited.", color = TextPrimary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { if (canClaim) onClaimDailyReward() else onDismissAll() },
                        colors = ButtonDefaults.buttonColors(containerColor = if (canClaim) HealthGreen else CyberOrange),
                        modifier = Modifier.fillMaxWidth().testTag("collect_daily_reward_button")
                    ) {
                        Text(if (canClaim) "COLLECT +1,500 BP" else "CLOSE", color = if (canClaim) Color.Black else Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }

    if (showMailboxDialog) {
        Dialog(onDismissRequest = onDismissAll) {
            Card(modifier = Modifier.fillMaxWidth(0.90f).fillMaxHeight(0.80f).testTag("mailbox_dialog"), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = TacticalDarkBg), border = BorderStroke(2.dp, ElectricBlueSafe)) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("OPERATIVE MAILBOX", color = ElectricBlueSafe, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        IconButton(onClick = onDismissAll) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
                    }
                    if (mailList.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No dispatches in inbox yet.", color = TextSecondary, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(mailList, key = { it.id }) { mail ->
                                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = TacticalSurface), shape = RoundedCornerShape(10.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(mail.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("From: ${mail.sender}", color = ElectricBlueSafe, fontSize = 10.sp)
                                            Text(mail.body, color = TextSecondary, fontSize = 11.sp)
                                        }
                                        if (mail.rewardClaimed) {
                                            Text("CLAIMED ✓", color = HealthGreen, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                        } else {
                                            Button(onClick = { onClaimMail(mail) }, colors = ButtonDefaults.buttonColors(containerColor = CyberOrange), modifier = Modifier.height(34.dp).testTag("claim_mail_${mail.id}")) {
                                                Text(if (mail.rewardBp > 0L) "CLAIM" else "READ", color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
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

    if (showNewsDialog || showNotificationsDialog) {
        Dialog(onDismissRequest = onDismissAll) {
            Card(modifier = Modifier.fillMaxWidth(0.90f).fillMaxHeight(0.80f).testTag("news_dialog"), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = TacticalDarkBg), border = BorderStroke(2.dp, CyberOrange)) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (showNotificationsDialog) "NOTIFICATIONS & ALERTS" else "LIMITED-TIME EVENTS & PATCH 54.0", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        IconButton(onClick = onDismissAll) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
                    }
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1F162D)), border = BorderStroke(1.dp, HazardYellow)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("BLITZ HOT-DROP • LIMITED-TIME MODE", color = HazardYellow, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                Text("Ultra-fast shrinking zones & 2x Drop Supply Loot!", color = TextPrimary, fontSize = 10.sp)
                            }
                            Button(onClick = onActivateLtmMode, colors = ButtonDefaults.buttonColors(containerColor = HazardYellow)) {
                                Text("SELECT LTM", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                            }
                        }
                    }
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (announcements.isEmpty()) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = TacticalSurface)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("WARX BATTLEGROUND • AAA 3D PBR LOBBY LIVE", color = ElectricBlueSafe, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                        Text("Real-time 3D character showcase, weather atmosphere, 5 battleground maps, and Season 54 Ranked mode active.", color = TextPrimary, fontSize = 11.sp)
                                    }
                                }
                            }
                        } else {
                            items(announcements, key = { it.id }) { ann ->
                                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = TacticalSurface)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("[${ann.severity}] ${ann.title}", color = HazardYellow, fontWeight = FontWeight.Black, fontSize = 12.sp)
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

    if (showSupportDialog) {
        Dialog(onDismissRequest = onDismissAll) {
            Card(modifier = Modifier.fillMaxWidth(0.86f).testTag("support_dialog"), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = TacticalDarkBg), border = BorderStroke(2.dp, HealthGreen)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("PLAYER SUPPORT DESK", color = HealthGreen, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        IconButton(onClick = onDismissAll) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
                    }
                    OutlinedTextField(value = supportSubject, onValueChange = onSupportSubjectChange, label = { Text("Subject") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("support_subject_input"))
                    OutlinedTextField(value = supportMessage, onValueChange = onSupportMessageChange, label = { Text("Describe issue or feedback") }, minLines = 3, maxLines = 4, modifier = Modifier.fillMaxWidth().testTag("support_message_input"))
                    Button(enabled = supportSubject.isNotBlank() && supportMessage.isNotBlank(), onClick = onSubmitSupport, colors = ButtonDefaults.buttonColors(containerColor = HealthGreen), modifier = Modifier.fillMaxWidth().testTag("submit_support_button")) {
                        Text("SUBMIT SUPPORT TICKET", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
