package com.example.ui.lobby

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfileEntity
import com.example.game.map.MapConfig
import com.example.game.model.EmotePreset
import com.example.game.model.GameModeType
import com.example.game.model.WeatherType
import com.example.ui.theme.AirdropGold
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.ElectricBlueSafe
import com.example.ui.theme.HazardYellow
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class LobbyFriendStatus(
    val id: String,
    val name: String,
    val rank: String,
    val isOnline: Boolean,
    val activity: String
)

@Composable
fun LobbyLeftSideMenu(
    unclaimedMissionsCount: Int,
    unreadMailCount: Int,
    onOpenProfile: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenClan: () -> Unit,
    onOpenMissions: () -> Unit,
    onOpenEvents: () -> Unit,
    onOpenRanking: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenMail: () -> Unit,
    onOpenBattlePass: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(136.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xD00B1120))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // WARX PASS SEASON 1 Promo Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF3E114F), Color(0xFF9E1028))))
                .border(1.2.dp, HazardYellow.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenBattlePass)
                .padding(horizontal = 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("WARX PASS", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text("SEASON 1", color = HazardYellow, fontSize = 7.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(HazardYellow)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text("CLAIM", color = Color.Black, fontSize = 7.sp, fontWeight = FontWeight.Black)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            LeftMenuTile("PROFILE", Icons.Default.Person, ElectricBlueSafe, 0, "left_nav_profile", Modifier.weight(1f), onOpenProfile)
            LeftMenuTile("FRIENDS", Icons.Default.Group, HealthGreen, 0, "left_nav_friends", Modifier.weight(1f), onOpenFriends)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            LeftMenuTile("CLAN", Icons.Default.Shield, Color(0xFFE040FB), 0, "left_nav_clan", Modifier.weight(1f), onOpenClan)
            LeftMenuTile("MISSIONS", Icons.AutoMirrored.Filled.Assignment, HazardYellow, unclaimedMissionsCount, "left_nav_missions", Modifier.weight(1f), onOpenMissions)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            LeftMenuTile("EVENTS", Icons.Default.Whatshot, CyberOrange, 0, "left_nav_events", Modifier.weight(1f), onOpenEvents)
            LeftMenuTile("RANKING", Icons.Default.Leaderboard, AirdropGold, 0, "left_nav_ranking", Modifier.weight(1f), onOpenRanking)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            LeftMenuTile("ACHIEVE", Icons.Default.MilitaryTech, Color(0xFF80D8FF), 0, "left_nav_achievements", Modifier.weight(1f), onOpenAchievements)
            LeftMenuTile("MAIL", Icons.Default.Email, HazardYellow, unreadMailCount, "left_nav_mail", Modifier.weight(1f), onOpenMail)
        }
    }
}

@Composable
private fun LeftMenuTile(
    label: String,
    icon: ImageVector,
    tint: Color,
    badgeCount: Int,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0xE011192A))
            .border(1.dp, tint.copy(alpha = 0.28f), RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(1.dp))
            Text(label, color = TextPrimary, fontSize = 7.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(CyberOrange),
                contentAlignment = Alignment.Center
            ) {
                Text("$badgeCount", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun LobbyRightSideMenu(
    profile: UserProfileEntity,
    dailyRewardReady: Boolean,
    onlineFriends: List<LobbyFriendStatus>,
    invitedFriendIds: Set<String>,
    onInviteFriend: (LobbyFriendStatus) -> Unit,
    onOpenBattlePass: () -> Unit,
    onOpenDailyRewards: () -> Unit,
    onOpenLimitedEvents: () -> Unit,
    onOpenSpecialMissions: () -> Unit,
    onOpenFeaturedOffers: () -> Unit,
    onOpenSeasonRewards: () -> Unit,
    modifier: Modifier = Modifier
) {
    var friendsDrawerOpen by remember { mutableStateOf(true) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Right Season / Pass / Events Cards Column
        Column(
            modifier = Modifier.width(154.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenBattlePass)
                    .testTag("right_battle_pass_card"),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xE01A1328)),
                border = BorderStroke(1.2.dp, HazardYellow.copy(alpha = 0.85f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Stars, contentDescription = "Rank Season 1", tint = HazardYellow, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("RANK SEASON 1", color = HazardYellow, fontWeight = FontWeight.Black, fontSize = 9.5.sp)
                        Text("TIER ${(profile.level / 2).coerceIn(1, 50)} • PASS REWARDS", color = TextPrimary, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenDailyRewards)
                    .testTag("right_daily_login_card"),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xE00F1C26)),
                border = BorderStroke(1.dp, if (dailyRewardReady) CyberOrange else ElectricBlueSafe.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CardGiftcard,
                        contentDescription = "Free Rewards",
                        tint = if (dailyRewardReady) HazardYellow else ElectricBlueSafe,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("FREE REWARDS", color = if (dailyRewardReady) HazardYellow else TextPrimary, fontWeight = FontWeight.Black, fontSize = 9.sp)
                        Text(
                            if (dailyRewardReady) "CLAIM +1,500 BP NOW" else "CLAIMED • NEXT 24H",
                            color = if (dailyRewardReady) Color.White else TextSecondary,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                RightMiniCard("LTM", "BLITZ 2X", Icons.Default.Bolt, CyberOrange, "right_ltm_events", Modifier.weight(1f), onOpenLimitedEvents)
                RightMiniCard("OPS", "WEEKLY", Icons.Default.Stars, ElectricBlueSafe, "right_special_missions", Modifier.weight(1f), onOpenSpecialMissions)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                RightMiniCard("PACK", "OFFERS", Icons.Default.LocalOffer, HazardYellow, "right_featured_offers", Modifier.weight(1f), onOpenFeaturedOffers)
                RightMiniCard("RANK", profile.rankTier, Icons.Default.EmojiEvents, Color(0xFFE040FB), "right_season_rewards", Modifier.weight(1f), onOpenSeasonRewards)
            }
        }

        // Visible Right-Edge Vertical Squad Invite Rail (collapsible toggle preserved)
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 9.dp, bottomStart = 9.dp))
                    .background(Color(0xE00C1322))
                    .border(1.dp, ElectricBlueSafe.copy(alpha = 0.55f), RoundedCornerShape(topStart = 9.dp, bottomStart = 9.dp))
                    .clickable { friendsDrawerOpen = !friendsDrawerOpen }
                    .padding(horizontal = 3.dp, vertical = 12.dp)
                    .testTag("online_friends_drawer_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (friendsDrawerOpen) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                        contentDescription = "Toggle Online Friends",
                        tint = ElectricBlueSafe,
                        modifier = Modifier.size(14.dp)
                    )
                    Text("${onlineFriends.count { it.isOnline }}", color = HealthGreen, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }

            AnimatedVisibility(visible = friendsDrawerOpen) {
                Column(
                    modifier = Modifier
                        .width(118.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xE60A101C))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                        .padding(5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("SQUAD SLOTS", color = HazardYellow, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    onlineFriends.take(4).forEach { friend ->
                        val invited = friend.id in invitedFriendIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color(0xFF131B2E))
                                .border(0.8.dp, if (friend.isOnline) HealthGreen.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(7.dp))
                                .padding(horizontal = 5.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (friend.isOnline) CyberOrange.copy(alpha = 0.85f) else Color(0xFF283042)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(friend.name.take(1).uppercase(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(modifier = Modifier.width(5.dp))
                                Column {
                                    Text(friend.name, color = TextPrimary, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(friend.rank, color = TextSecondary, fontSize = 7.sp, maxLines = 1)
                                }
                            }
                            if (friend.isOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (invited) HealthGreen.copy(alpha = 0.25f) else HazardYellow.copy(alpha = 0.22f))
                                        .clickable { onInviteFriend(friend) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (invited) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = "Invite ${friend.name}",
                                        tint = if (invited) HealthGreen else HazardYellow,
                                        modifier = Modifier.size(12.dp)
                                    )
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
private fun RightMiniCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0xDC0F1625))
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(tag),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(title, color = tint, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
        Text(subtitle, color = TextPrimary, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun LobbyBottomBar(
    selectedMode: GameModeType,
    selectedMap: MapConfig,
    selectedTeamSize: Int,
    selectedWeather: WeatherType,
    selectedEmote: EmotePreset,
    isMatchmaking: Boolean,
    startPulse: Float,
    onOpenCharacter: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenWeapons: () -> Unit,
    onOpenStore: () -> Unit,
    onOpenPets: () -> Unit,
    onOpenCustomization: () -> Unit,
    onCycleWeather: () -> Unit,
    onTriggerEmote: () -> Unit,
    onSelectTeamSize: (Int) -> Unit,
    onOpenModeSelector: () -> Unit,
    onStartMatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xCC060A12), Color(0xF2050810))
                )
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // Left & Center-Left: Bottom Navigation Bar + Quick Emote/Weather
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val isNight = selectedWeather == WeatherType.NIGHT_OPS
                val weatherTint = if (isNight) HazardYellow else ElectricBlueSafe
                val weatherIcon = when (selectedWeather) {
                    WeatherType.CLEAR -> Icons.Default.WbSunny
                    WeatherType.SUNSET_STORM -> Icons.Default.WbTwilight
                    WeatherType.DENSE_FOG -> Icons.Default.Cloud
                    WeatherType.NIGHT_OPS -> Icons.Default.NightsStay
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TacticalSurface.copy(alpha = 0.86f))
                        .border(1.dp, weatherTint.copy(alpha = 0.50f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onCycleWeather)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("weather_cycle_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(weatherIcon, contentDescription = "Day Night Weather", tint = weatherTint, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(selectedWeather.displayName.uppercase(), color = weatherTint, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TacticalSurface.copy(alpha = 0.86f))
                        .border(1.dp, CyberOrange.copy(alpha = 0.50f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onTriggerEmote)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("lobby_emote_pill"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SportsKabaddi, contentDescription = "Play Emote", tint = CyberOrange, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("EMOTE: ${selectedEmote.label.uppercase()}", color = TextPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xE00D1422))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomDockItem("VAULT", Icons.Default.Backpack, ElectricBlueSafe, "bottom_nav_inventory", onOpenInventory)
                BottomDockItem("WEAPON", Icons.Default.Security, HazardYellow, "bottom_nav_weapons", onOpenWeapons)
                BottomDockItem("CHARACTER", Icons.Default.Person, CyberOrange, "bottom_nav_character", onOpenCharacter)
                BottomDockItem("PET", Icons.Default.Pets, HealthGreen, "bottom_nav_pets", onOpenPets)
                BottomDockItem("LAB", Icons.Default.Tune, Color(0xFF80D8FF), "bottom_nav_customization", onOpenCustomization)
                BottomDockItem("STORE", Icons.Default.ShoppingBag, AirdropGold, "bottom_nav_store", onOpenStore)
            }
        }

        // Bottom-Right: Team Size Pills + Game Mode Selector + Sharp Golden START Match Button
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xD90E1524))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(9.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    TeamSizeMiniPill("SOLO", selectedTeamSize == 1) { onSelectTeamSize(1) }
                    TeamSizeMiniPill("DUO", selectedTeamSize == 2) { onSelectTeamSize(2) }
                    TeamSizeMiniPill("SQUAD", selectedTeamSize == 4) { onSelectTeamSize(4) }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xE6111B30), Color(0xE61B2947)))
                        )
                        .border(1.2.dp, ElectricBlueSafe.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenModeSelector)
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                        .testTag("mode_selector_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = "Select Mode", tint = HazardYellow, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(7.dp))
                    Column {
                        Text(selectedMode.title.uppercase(), color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        Text("${selectedMap.name.uppercase()} • TAP TO CHANGE", color = ElectricBlueSafe, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Button(
                onClick = onStartMatch,
                enabled = !isMatchmaking,
                colors = ButtonDefaults.buttonColors(containerColor = HazardYellow),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(62.dp)
                    .width(142.dp)
                    .scale(if (!isMatchmaking) startPulse else 1f)
                    .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                    .testTag("start_match_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Start Match", tint = Color.Black, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (isMatchmaking) "JOINING" else "START",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun BottomDockItem(
    label: String,
    icon: ImageVector,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 5.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(17.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = TextPrimary, fontSize = 7.5.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun TeamSizeMiniPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) CyberOrange else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 4.dp)
            .testTag("team_size_${label.lowercase()}")
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black
        )
    }
}
