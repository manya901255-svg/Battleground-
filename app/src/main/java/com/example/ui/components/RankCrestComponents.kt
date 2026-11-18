package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.model.RankTierBadge
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.HazardYellow
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RankTierCrest(
    tier: RankTierBadge,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    val primary = Color(tier.primaryHex)
    val secondary = Color(tier.secondaryHex)
    val accent = Color(tier.accentHex)
    val isTopTier = tier == RankTierBadge.HEROIC || tier == RankTierBadge.GRANDMASTER

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.5f

        // Winged Halo for Diamond, Heroic & Grandmaster
        if (tier.ordinal >= RankTierBadge.PLATINUM.ordinal) {
            val leftWing = Path().apply {
                moveTo(cx - w * 0.14f, h * 0.34f)
                lineTo(w * 0.04f, h * 0.20f)
                lineTo(w * 0.10f, h * 0.44f)
                lineTo(w * 0.04f, h * 0.56f)
                lineTo(cx - w * 0.14f, h * 0.62f)
                close()
            }
            val rightWing = Path().apply {
                moveTo(cx + w * 0.14f, h * 0.34f)
                lineTo(w * 0.96f, h * 0.20f)
                lineTo(w * 0.90f, h * 0.44f)
                lineTo(w * 0.96f, h * 0.56f)
                lineTo(cx + w * 0.14f, h * 0.62f)
                close()
            }
            drawPath(leftWing, accent.copy(alpha = 0.85f))
            drawPath(rightWing, accent.copy(alpha = 0.85f))
        }

        // Outer Hexagonal / Shield Crest
        val shield = Path().apply {
            moveTo(cx, h * 0.08f)
            lineTo(w * 0.82f, h * 0.26f)
            lineTo(w * 0.75f, h * 0.70f)
            lineTo(cx, h * 0.94f)
            lineTo(w * 0.25f, h * 0.70f)
            lineTo(w * 0.18f, h * 0.26f)
            close()
        }
        drawPath(
            path = shield,
            brush = Brush.verticalGradient(listOf(primary, secondary))
        )
        drawPath(
            path = shield,
            color = accent,
            style = Stroke(width = if (isTopTier) 2.6f else 1.8f)
        )

        // Inner Diamond Core
        val inner = Path().apply {
            moveTo(cx, h * 0.24f)
            lineTo(w * 0.65f, h * 0.45f)
            lineTo(cx, h * 0.74f)
            lineTo(w * 0.35f, h * 0.45f)
            close()
        }
        drawPath(inner, accent.copy(alpha = 0.9f))

        // Top Crown / Star Pip for Heroic & Grandmaster
        if (isTopTier) {
            drawCircle(
                color = HazardYellow,
                radius = w * 0.09f,
                center = Offset(cx, h * 0.14f)
            )
        }
    }
}

@Composable
fun RankGuideDialog(
    currentRp: Int,
    onDismiss: () -> Unit
) {
    val activeTier = RankTierBadge.fromRp(currentRp)
    val activeLabel = RankTierBadge.formattedRankLabel(currentRp)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("rank_guide_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
            border = BorderStroke(2.dp, HazardYellow)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RankTierCrest(tier = activeTier, size = 40.dp)
                        Column {
                            Text("RANKED TIER LADDER & REWARDS", color = HazardYellow, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text("CURRENT: ${activeLabel.uppercase()} • $currentRp RP", color = CyberOrange, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                LazyColumn(
                    modifier = Modifier.height(265.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(RankTierBadge.entries) { _, tier ->
                        val unlocked = currentRp >= tier.minRp
                        val isCurrent = tier == activeTier
                        val rewardDesc = when (tier) {
                            RankTierBadge.BRONZE -> "1,000 BP + Starter Tactical Avatar"
                            RankTierBadge.SILVER -> "1,800 BP + Silver Rank Crest Banner"
                            RankTierBadge.GOLD -> "3,000 BP + Golden Parachute Emblem"
                            RankTierBadge.PLATINUM -> "120 VC + Platinum Rifle Trail"
                            RankTierBadge.DIAMOND -> "220 VC + Diamond Squad Banner"
                            RankTierBadge.HEROIC -> "450 VC + Heroic Crimson Finisher Emote"
                            RankTierBadge.GRANDMASTER -> "800 VC + Top 500 Animated Sovereign Crest"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) Color(0xFF1D243A) else TacticalSurface)
                                .border(
                                    1.2.dp,
                                    if (isCurrent) HazardYellow else if (unlocked) Color(tier.primaryHex).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RankTierCrest(tier = tier, size = 34.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${tier.title.uppercase()} (${tier.minRp}+ RP)",
                                    color = if (isCurrent) HazardYellow else TextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                                Text(rewardDesc, color = TextSecondary, fontSize = 9.sp)
                            }
                            Text(
                                text = when {
                                    isCurrent -> "CURRENT"
                                    unlocked -> "UNLOCKED ✓"
                                    else -> "LOCKED"
                                },
                                color = when {
                                    isCurrent -> CyberOrange
                                    unlocked -> HealthGreen
                                    else -> TextSecondary
                                },
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
