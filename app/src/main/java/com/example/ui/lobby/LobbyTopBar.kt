package com.example.ui.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfileEntity
import com.example.game.model.CharacterPreset
import com.example.game.model.RankTierBadge
import com.example.ui.components.RankTierCrest
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.ElectricBlueSafe
import com.example.ui.theme.HazardYellow
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

@Composable
fun LobbyTopBar(
    profile: UserProfileEntity,
    unreadMailCount: Int,
    notificationCount: Int,
    accentColor: Color,
    onOpenProfile: () -> Unit,
    onOpenVip: () -> Unit,
    onOpenStore: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenMail: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRankGuide: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeCharacter = CharacterPreset.fromId(profile.selectedCharacterId)
    val charAccent = Color(activeCharacter.accentRgb.first, activeCharacter.accentRgb.second, activeCharacter.accentRgb.third)
    val vipLevel = ((profile.level / 15) + (if (profile.vortexCredits >= 500) 1 else 0)).coerceIn(1, 5)
    val xpFraction = (profile.xp.toFloat() / profile.maxXp.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val numberFmt = NumberFormat.getIntegerInstance(Locale.US)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xD9070B14), Color(0x880B1220), Color.Transparent)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Avatar + Username + Level/XP + VIP + Currencies
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Player Profile Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(TacticalSurface.copy(alpha = 0.90f))
                    .border(1.2.dp, charAccent.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenProfile)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("top_profile_card"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            Brush.linearGradient(listOf(charAccent, CyberOrange))
                        )
                        .border(1.5.dp, HazardYellow, RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.username.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.width(126.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = profile.username,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = "Lv.${profile.level}",
                            color = HazardYellow,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { xpFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = HazardYellow,
                        trackColor = Color(0xFF1F2A40)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${profile.rankTier} • ${profile.xp}/${profile.maxXp} XP",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Rank Crest Pill (Tap for Bronze -> Grandmaster Rank Guide)
            val rankBadge = RankTierBadge.fromRp(profile.rankPoints)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(TacticalSurface.copy(alpha = 0.90f))
                    .border(1.dp, Color(rankBadge.primaryHex).copy(alpha = 0.72f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenRankGuide)
                    .padding(horizontal = 7.dp, vertical = 5.dp)
                    .testTag("top_rank_crest_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RankTierCrest(tier = rankBadge, size = 24.dp)
                Text(
                    text = rankBadge.title.uppercase(),
                    color = Color(rankBadge.accentHex),
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp
                )
            }

            // Gold Coins Capsule
            TopCurrencyChip(
                amount = numberFmt.format(profile.bpCoins),
                icon = Icons.Default.MonetizationOn,
                tint = HazardYellow,
                showPlus = false,
                contentDesc = "Gold Coins Balance",
                tag = "top_gold_balance",
                onClick = onOpenStore
            )

            // Diamond Balance Capsule
            TopCurrencyChip(
                amount = numberFmt.format(profile.vortexCredits),
                icon = Icons.Default.Diamond,
                tint = ElectricBlueSafe,
                showPlus = true,
                contentDesc = "Diamond Currency Balance",
                tag = "top_diamond_balance",
                onClick = onOpenStore
            )

            // VIP Membership Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF422806), Color(0xFF2B1804)))
                    )
                    .border(1.dp, HazardYellow.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenVip)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("vip_membership_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "VIP Membership",
                    tint = HazardYellow,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("VIP $vipLevel", color = HazardYellow, fontWeight = FontWeight.Black, fontSize = 10.sp)
            }
        }

        // Right: Original WARX BATTLEGROUND Header Emblem + Quick System Icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xD90E1422), Color(0xD9240C14))))
                    .border(1.2.dp, HazardYellow.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Stars, contentDescription = "WarX Crown", tint = HazardYellow, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("WAR", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 0.8.sp)
                Text("X", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(5.dp))
                Text("BATTLEGROUND", color = HazardYellow, fontWeight = FontWeight.ExtraBold, fontSize = 9.sp, letterSpacing = 0.6.sp)
            }

            TopUtilityIcon(
                icon = Icons.Default.Group,
                contentDesc = "Squad Friends",
                tint = TextPrimary,
                badgeCount = 0,
                tag = "top_friends_button",
                onClick = onOpenFriends
            )
            TopUtilityIcon(
                icon = Icons.Default.Email,
                contentDesc = "Mailbox",
                tint = if (unreadMailCount > 0) HazardYellow else TextPrimary,
                badgeCount = unreadMailCount,
                tag = "mailbox_button",
                onClick = onOpenMail
            )
            TopUtilityIcon(
                icon = Icons.Default.Notifications,
                contentDesc = "Notifications",
                tint = if (notificationCount > 0) CyberOrange else TextPrimary,
                badgeCount = notificationCount,
                tag = "notification_button",
                onClick = onOpenNotifications
            )
            TopUtilityIcon(
                icon = Icons.Default.ColorLens,
                contentDesc = "Lobby Themes",
                tint = accentColor,
                badgeCount = 0,
                tag = "theme_selector_button",
                onClick = onOpenThemes
            )
            TopUtilityIcon(
                icon = Icons.Default.AdminPanelSettings,
                contentDesc = "Admin Console",
                tint = CyberOrange,
                badgeCount = 0,
                tag = "admin_button",
                onClick = onOpenAdmin
            )
            TopUtilityIcon(
                icon = Icons.Default.Settings,
                contentDesc = "Settings",
                tint = TextPrimary,
                badgeCount = 0,
                tag = "settings_button",
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun TopCurrencyChip(
    amount: String,
    icon: ImageVector,
    tint: Color,
    showPlus: Boolean,
    contentDesc: String,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(TacticalSurface.copy(alpha = 0.90f))
            .border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = contentDesc, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(amount, color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
        if (showPlus) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Top Up", tint = tint, modifier = Modifier.size(11.dp))
            }
        }
    }
}

@Composable
private fun TopUtilityIcon(
    icon: ImageVector,
    contentDesc: String,
    tint: Color,
    badgeCount: Int,
    tag: String,
    onClick: () -> Unit
) {
    Box {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(TacticalSurface.copy(alpha = 0.88f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                .testTag(tag)
        ) {
            Icon(icon, contentDescription = contentDesc, tint = tint, modifier = Modifier.size(18.dp))
        }
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(CyberOrange),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (badgeCount > 9) "9+" else "$badgeCount",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
