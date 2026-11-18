package com.example.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.engine.BattleRoyaleEngine
import com.example.game.model.ConsumableType
import com.example.game.model.GroundLoot
import com.example.game.model.KillFeedEntry
import com.example.game.model.Player
import com.example.game.model.PlayerStance
import com.example.game.model.ScopeType
import com.example.game.model.Weapon
import com.example.game.model.WeaponCategory
import com.example.ui.theme.AirdropGold
import com.example.ui.theme.BloodKnockout
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.ElectricBlueSafe
import com.example.ui.theme.ElectricBlueZone
import com.example.ui.theme.HazardYellow
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.MilitaryGreen
import com.example.ui.theme.RarityEpic
import com.example.ui.theme.RedZoneBombardment
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun ReferenceMiniMap(
    engine: BattleRoyaleEngine,
    players: List<Player>,
    safeCenterX: Float,
    safeCenterY: Float,
    safeRadius: Float,
    blueCenterX: Float,
    blueCenterY: Float,
    blueRadius: Float,
    radarActive: Boolean,
    sizeDp: Dp,
    onClick: () -> Unit
) {
    val user = engine.userPlayer
    val conePath = remember { Path() }
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(RoundedCornerShape(3.dp))
            .background(MilitaryGreen.copy(alpha = 0.34f))
            .border(1.5.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(3.dp))
            .clickable { onClick() }
            .testTag("mini_map_widget")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Tactical terrain grid background
            drawRect(color = Color(0xFF314B29).copy(alpha = 0.88f))

            val scale = size.width / engine.mapConfig.worldSize
            // Roads
            drawLine(
                color = Color(0xFF6B6554),
                start = Offset(0f, size.height * 0.5f),
                end = Offset(size.width, size.height * 0.5f),
                strokeWidth = 3.5f
            )
            drawLine(
                color = Color(0xFF6B6554),
                start = Offset(size.width * 0.5f, 0f),
                end = Offset(size.width * 0.5f, size.height),
                strokeWidth = 3.5f
            )
            // Buildings
            engine.mapConfig.buildings.forEach { b ->
                drawRect(
                    color = AirdropGold.copy(alpha = 0.72f),
                    topLeft = Offset(b.x * scale, b.y * scale),
                    size = Size((b.width * scale).coerceAtLeast(2.5f), (b.height * scale).coerceAtLeast(2.5f))
                )
            }
            // Safe & Blue Zones
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                center = Offset(safeCenterX * scale, safeCenterY * scale),
                radius = safeRadius * scale,
                style = Stroke(width = 1.5f)
            )
            drawCircle(
                color = ElectricBlueZone,
                center = Offset(blueCenterX * scale, blueCenterY * scale),
                radius = blueRadius * scale,
                style = Stroke(width = 2.0f)
            )

            // Squadmates
            players.filter { !it.isUser && it.isAlive && it.teamId == user.teamId }.forEach { ally ->
                drawCircle(
                    color = HazardYellow,
                    center = Offset(ally.x * scale, ally.y * scale),
                    radius = 3.2f
                )
            }

            // Nearby/Scanned Enemies
            val revealRadius = if (radarActive) 1600f else 620f
            players.filter { !it.isUser && it.isAlive && it.teamId != user.teamId }.forEach { bot ->
                val dx = bot.x - user.x
                val dy = bot.y - user.y
                if (dx * dx + dy * dy < revealRadius * revealRadius) {
                    drawCircle(
                        color = RedZoneBombardment,
                        center = Offset(bot.x * scale, bot.y * scale),
                        radius = if (radarActive) 3.6f else 2.5f
                    )
                }
            }

            // Vision Cone + Cyan Player Arrow
            val ux = user.x * scale
            val uy = user.y * scale
            val yawRad = Math.toRadians(engine.cameraYawDegrees.toDouble())
            val coneLeft = yawRad - Math.toRadians(28.0)
            val coneRight = yawRad + Math.toRadians(28.0)
            val coneLen = size.width * 0.42f
            conePath.reset()
            conePath.apply {
                moveTo(ux, uy)
                lineTo(ux + (cos(coneLeft) * coneLen).toFloat(), uy + (sin(coneLeft) * coneLen).toFloat())
                lineTo(ux + (cos(coneRight) * coneLen).toFloat(), uy + (sin(coneRight) * coneLen).toFloat())
                close()
            }
            drawPath(
                path = conePath,
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.50f), Color.Transparent),
                    center = Offset(ux, uy),
                    radius = coneLen
                )
            )
            drawCircle(color = ElectricBlueZone, center = Offset(ux, uy), radius = 4.5f)
            drawCircle(color = Color.White, center = Offset(ux, uy), radius = 4.5f, style = Stroke(width = 1.2f))
        }
    }
}

@Composable
fun MapSideCommsColumn(
    isSpeakerOn: Boolean,
    onToggleSpeaker: () -> Unit,
    isMicOn: Boolean,
    onToggleMic: () -> Unit,
    onEmoteClick: () -> Unit,
    onChatClick: () -> Unit,
    onCompanionScanClick: () -> Unit,
    scanReady: Boolean,
    uiScale: Float
) {
    val btnSize = (26.dp * uiScale).coerceIn(22.dp, 32.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Top) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            SmallHudCircleButton(
                icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                contentDesc = "Audio Toggle",
                tint = if (isSpeakerOn) Color.White else TextSecondary,
                size = btnSize,
                testTag = "speaker_toggle_button",
                onClick = onToggleSpeaker
            )
            SmallHudCircleButton(
                icon = if (isMicOn) Icons.Default.Mic else Icons.Default.MicOff,
                contentDesc = "Mic Toggle",
                tint = if (isMicOn) Color.White else TextSecondary,
                size = btnSize,
                testTag = "mic_toggle_button",
                onClick = onToggleMic
            )
            SmallHudCircleButton(
                icon = Icons.Default.Mood,
                contentDesc = "Tactical Emote",
                tint = Color.White,
                size = btnSize,
                testTag = "emote_button",
                onClick = onEmoteClick
            )
            SmallHudCircleButton(
                icon = Icons.AutoMirrored.Filled.Chat,
                contentDesc = "Quick Radio Chat",
                tint = Color.White,
                size = btnSize,
                testTag = "quick_comms_button",
                onClick = onChatClick
            )
        }
        SmallHudCircleButton(
            icon = Icons.Default.Pets,
            contentDesc = "Companion Radar Scan",
            tint = if (scanReady) Color.White else TextSecondary,
            size = btnSize,
            borderColor = if (scanReady) ElectricBlueSafe else Color.White.copy(alpha = 0.45f),
            testTag = "companion_scan_button",
            onClick = onCompanionScanClick
        )
    }
}

@Composable
private fun SmallHudCircleButton(
    icon: ImageVector,
    contentDesc: String,
    tint: Color,
    size: Dp,
    borderColor: Color = Color.White.copy(alpha = 0.5f),
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.42f))
            .border(1.dp, borderColor, CircleShape)
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDesc, tint = tint, modifier = Modifier.size(size * 0.56f))
    }
}

@Composable
fun SquadStatusPanel(
    members: List<Player>,
    widthDp: Dp
) {
    val badgeColors = listOf(ElectricBlueZone, HazardYellow, RarityEpic, ElectricBlueSafe)
    Column(
        modifier = Modifier
            .width(widthDp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.42f))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        members.take(4).forEachIndexed { idx, member ->
            val badgeColor = badgeColors.getOrElse(idx) { ElectricBlueSafe }
            val hpRatio = (member.health / member.maxHealth).coerceIn(0f, 1f)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(badgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${idx + 1}",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.name,
                        color = if (member.isAlive) Color.White else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 11.sp
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(hpRatio)
                                .background(
                                    if (!member.isAlive || hpRatio < 0.28f) RedZoneBombardment
                                    else Color.White
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TacticalCompassBar(
    yawDegrees: Float,
    isShrinking: Boolean,
    zoneTimerSec: Float,
    phase: Int,
    widthDp: Dp,
    modifier: Modifier = Modifier
) {
    val normYaw = ((yawDegrees % 360f) + 360f) % 360f
    val bearing = normYaw.roundToInt()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Horizontal Sliding Bearing Compass Ribbon
        val triPath = remember { Path() }
        Box(
            modifier = Modifier
                .width(widthDp)
                .height(30.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Downward center pointer triangle
                val cx = size.width / 2f
                triPath.reset()
                triPath.apply {
                    moveTo(cx - 5f, 0f)
                    lineTo(cx + 5f, 0f)
                    lineTo(cx, 6f)
                    close()
                }
                drawPath(triPath, Color.White)

                val pxPerDeg = size.width / 120f
                for (degOffset in -55..55 step 15) {
                    val snappedDeg = (((bearing / 15) * 15) + degOffset + 360) % 360
                    val diff = ((snappedDeg - normYaw + 540f) % 360f) - 180f
                    val x = cx + diff * pxPerDeg
                    if (x in 12f..(size.width - 12f)) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.65f),
                            start = Offset(x, 6f),
                            end = Offset(x, 11f),
                            strokeWidth = 1.5f
                        )
                    }
                }
            }

            // Bearing labels along strip + central degree badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val left60 = compassLabel((bearing - 60 + 360) % 360)
                val left30 = compassLabel((bearing - 30 + 360) % 360)
                val left15 = compassLabel((bearing - 15 + 360) % 360)
                val right15 = compassLabel((bearing + 15) % 360)
                val right30 = compassLabel((bearing + 30) % 360)
                val right60 = compassLabel((bearing + 60) % 360)

                Text(left60, color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(left30, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text(left15, color = Color.White.copy(alpha = 0.78f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(TacticalDarkBg.copy(alpha = 0.85f))
                        .border(1.dp, ElectricBlueSafe.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text("$bearing", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
                Text(right15, color = Color.White.copy(alpha = 0.78f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(right30, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text(right60, color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Safe Zone Shrinking Alert / Phase Timer Pill
        val zM = (zoneTimerSec / 60).toInt().coerceAtLeast(0)
        val zS = (zoneTimerSec % 60).toInt().coerceAtLeast(0)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.56f))
                .padding(horizontal = 12.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isShrinking) {
                Icon(Icons.Default.Warning, contentDescription = "Zone Shrinking", tint = RedZoneBombardment, modifier = Modifier.size(14.dp))
                Text("Safe Zone is now shrinking", color = HazardYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Bolt, contentDescription = "Zone Timer", tint = ElectricBlueSafe, modifier = Modifier.size(13.dp))
                Text(
                    text = String.format(Locale.US, "Phase %d • Safe Zone in %02d:%02d", phase, zM, zS),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun compassLabel(deg: Int): String {
    val snapped = ((deg + 7) / 15 * 15) % 360
    return when (snapped) {
        0 -> "N"
        45 -> "NE"
        90 -> "E"
        135 -> "SE"
        180 -> "S"
        225 -> "SW"
        270 -> "W"
        315 -> "NW"
        else -> "$snapped"
    }
}

@Composable
fun TopRightStatusStrip(
    coins: Int,
    aliveCount: Int,
    killsCount: Int,
    onCoinsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // Gold Coins Pill (Tap to spend 300 coins for supply refill)
        Row(
            modifier = Modifier
                .height(26.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable { onCoinsClick() }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(HazardYellow)
                    .border(1.dp, AirdropGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("V", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            Text("$coins", color = HazardYellow, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }

        // Cyan Alive Block
        Row(
            modifier = Modifier
                .height(26.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(ElectricBlueSafe.copy(alpha = 0.85f))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Default.Person, contentDescription = "Alive", tint = Color.White, modifier = Modifier.size(14.dp))
            Text("$aliveCount", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }

        // Red Kills Block
        Row(
            modifier = Modifier
                .height(26.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(BloodKnockout.copy(alpha = 0.88f))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Default.GpsFixed, contentDescription = "Kills", tint = Color.White, modifier = Modifier.size(13.dp))
            Text("$killsCount", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }

        // Settings Gear Button
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onSettingsClick() }
                .testTag("in_match_settings_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Match Settings", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun WeaponInventoryPanel(
    user: Player,
    isReloading: Boolean,
    reloadProgress: Float,
    panelWidth: Dp,
    onReloadClick: () -> Unit,
    onSelectSlot: (Int) -> Unit
) {
    val activeWeapon = user.activeWeapon
    val activeReserve = activeWeapon?.let { user.ammoInventory[it.ammoType] ?: 0 } ?: 0
    val altPrimarySlot = if (user.activeSlot == 1) 2 else 1
    val altPrimaryWeapon = if (altPrimarySlot == 1) user.primaryWeapon1 else user.primaryWeapon2

    Column(
        modifier = Modifier.width(panelWidth),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // Big Primary Active Weapon Card (Tap to Reload)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.56f))
                .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                .clickable { onReloadClick() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("primary_weapon_reload_card")
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (activeWeapon != null && activeWeapon.maxCapacity > 0) "${activeWeapon.currentAmmo}" else "∞",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 18.sp
                            )
                            if (activeWeapon != null && activeWeapon.maxCapacity > 0) {
                                Text(
                                    text = "/$activeReserve",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = if (isReloading) "Reloading..." else "${activeWeapon?.name ?: "Weapon"} • Quick Reload",
                            color = if (isReloading) HazardYellow else Color.White.copy(alpha = 0.9f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    // Crisp Weapon Silhouette
                    Canvas(modifier = Modifier.width(74.dp).height(26.dp)) {
                        drawWeaponSilhouette(activeWeapon?.category ?: WeaponCategory.ASSAULT_RIFLE, activeWeapon?.id ?: "")
                    }
                }

                // Magazine / Reload Progress Strip
                val barFrac = if (isReloading) {
                    reloadProgress.coerceIn(0f, 1f)
                } else if (activeWeapon != null && activeWeapon.totalCapacity > 0) {
                    (activeWeapon.currentAmmo.toFloat() / activeWeapon.totalCapacity.toFloat()).coerceIn(0f, 1f)
                } else 1f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(barFrac)
                            .background(if (isReloading) HazardYellow else Color.White)
                    )
                }
            }
        }

        // 3 Sub-Slot Cards (Secondary/Alt Primary, Pistol, Melee ∞)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            WeaponMiniSlotCard(
                weapon = altPrimaryWeapon,
                fallbackCategory = WeaponCategory.SHOTGUN,
                ammoText = altPrimaryWeapon?.let { "${it.currentAmmo}" } ?: "0",
                isSelected = user.activeSlot == altPrimarySlot,
                testTag = "weapon_slot_$altPrimarySlot",
                modifier = Modifier.weight(1f),
                onClick = { onSelectSlot(altPrimarySlot) }
            )
            WeaponMiniSlotCard(
                weapon = user.pistol,
                fallbackCategory = WeaponCategory.PISTOL,
                ammoText = user.pistol?.let { "${it.currentAmmo}" } ?: "12",
                isSelected = user.activeSlot == 3,
                testTag = "weapon_slot_3",
                modifier = Modifier.weight(1f),
                onClick = { onSelectSlot(3) }
            )
            WeaponMiniSlotCard(
                weapon = user.melee,
                fallbackCategory = WeaponCategory.MELEE,
                ammoText = "${user.melee?.name?.take(6)?.uppercase() ?: "MELEE"} ∞",
                isSelected = user.activeSlot == 4,
                testTag = "weapon_slot_4",
                modifier = Modifier.weight(1f),
                onClick = { onSelectSlot(4) }
            )
        }
    }
}

@Composable
private fun WeaponMiniSlotCard(
    weapon: Weapon?,
    fallbackCategory: WeaponCategory,
    ammoText: String,
    isSelected: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (isSelected) CyberOrange.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.50f))
            .border(
                width = if (isSelected) 1.5.dp else 0.8.dp,
                color = if (isSelected) HazardYellow else Color.White.copy(alpha = 0.55f),
                shape = RoundedCornerShape(3.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag(testTag)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 8.dp)) {
            drawWeaponSilhouette(weapon?.category ?: fallbackCategory, weapon?.id ?: "")
        }
        Text(
            text = ammoText,
            color = Color.White,
            fontSize = 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

private fun DrawScope.drawWeaponSilhouette(category: WeaponCategory, weaponId: String = "") {
    val w = size.width
    val h = size.height
    val c = Color.White
    when (category) {
        WeaponCategory.ASSAULT_RIFLE, WeaponCategory.SMG -> {
            drawRect(c, topLeft = Offset(w * 0.08f, h * 0.34f), size = Size(w * 0.82f, h * 0.18f))
            drawRect(c, topLeft = Offset(w * 0.06f, h * 0.36f), size = Size(w * 0.18f, h * 0.30f))
            drawRect(c, topLeft = Offset(w * 0.42f, h * 0.50f), size = Size(w * 0.11f, h * 0.36f))
            drawRect(c, topLeft = Offset(w * 0.28f, h * 0.50f), size = Size(w * 0.08f, h * 0.26f))
            drawRect(c, topLeft = Offset(w * 0.36f, h * 0.22f), size = Size(w * 0.14f, h * 0.12f))
        }
        WeaponCategory.SNIPER_RIFLE, WeaponCategory.SHOTGUN -> {
            drawRect(c, topLeft = Offset(w * 0.06f, h * 0.40f), size = Size(w * 0.88f, h * 0.14f))
            drawRect(c, topLeft = Offset(w * 0.05f, h * 0.42f), size = Size(w * 0.22f, h * 0.28f))
            drawRect(c, topLeft = Offset(w * 0.34f, h * 0.24f), size = Size(w * 0.24f, h * 0.12f))
        }
        WeaponCategory.PISTOL -> {
            drawRect(c, topLeft = Offset(w * 0.24f, h * 0.25f), size = Size(w * 0.52f, h * 0.22f))
            drawRect(c, topLeft = Offset(w * 0.26f, h * 0.45f), size = Size(w * 0.16f, h * 0.38f))
        }
        WeaponCategory.MELEE -> {
            when (weaponId.lowercase()) {
                "katana" -> {
                    rotate(degrees = -22f) {
                        drawRect(c, topLeft = Offset(w * 0.10f, h * 0.46f), size = Size(w * 0.78f, h * 0.11f))
                        drawRect(HazardYellow, topLeft = Offset(w * 0.28f, h * 0.34f), size = Size(w * 0.05f, h * 0.34f))
                    }
                }
                "scythe" -> {
                    drawRect(c, topLeft = Offset(w * 0.12f, h * 0.48f), size = Size(w * 0.72f, h * 0.10f))
                    drawArc(RedZoneBombardment, startAngle = -110f, sweepAngle = 150f, useCenter = false, topLeft = Offset(w * 0.58f, h * 0.10f), size = Size(w * 0.32f, h * 0.72f), style = Stroke(width = 4.5f))
                }
                "bat" -> {
                    rotate(degrees = -18f) {
                        drawRoundRect(c, topLeft = Offset(w * 0.14f, h * 0.40f), size = Size(w * 0.74f, h * 0.20f), cornerRadius = CornerRadius(8f, 8f))
                    }
                }
                "machete" -> {
                    rotate(degrees = -15f) {
                        drawRect(c, topLeft = Offset(w * 0.12f, h * 0.44f), size = Size(w * 0.24f, h * 0.14f))
                        drawRect(c, topLeft = Offset(w * 0.36f, h * 0.34f), size = Size(w * 0.50f, h * 0.28f))
                    }
                }
                else -> {
                    drawOval(c, topLeft = Offset(w * 0.34f, h * 0.18f), size = Size(w * 0.46f, h * 0.62f))
                    drawRect(c, topLeft = Offset(w * 0.12f, h * 0.44f), size = Size(w * 0.26f, h * 0.14f))
                }
            }
        }
        else -> {
            drawRoundRect(c, topLeft = Offset(w * 0.30f, h * 0.24f), size = Size(w * 0.40f, h * 0.52f), cornerRadius = CornerRadius(6f, 6f))
        }
    }
}

@Composable
fun LeftUtilityColumn(
    backpackLevel: Int,
    medkitCount: Int,
    throwableCount: Int,
    onOpenBackpack: () -> Unit,
    onUseMedkit: () -> Unit,
    onThrowGrenade: () -> Unit,
    uiScale: Float
) {
    val itemSize = (42.dp * uiScale).coerceIn(36.dp, 50.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Backpack (Gold outer ring)
        UtilityOrbButton(
            size = itemSize,
            ringColor = HazardYellow,
            badgeText = "$backpackLevel",
            testTag = "backpack_button",
            onClick = onOpenBackpack
        ) {
            Icon(Icons.Default.Backpack, contentDescription = "Backpack", tint = Color.White, modifier = Modifier.size(itemSize * 0.52f))
        }

        // Medkit (White outer ring + count badge)
        UtilityOrbButton(
            size = itemSize,
            ringColor = Color.White.copy(alpha = 0.85f),
            badgeText = "$medkitCount",
            testTag = "medkit_quick_button",
            onClick = onUseMedkit
        ) {
            Icon(Icons.Default.LocalHospital, contentDescription = "Medkit", tint = Color.White, modifier = Modifier.size(itemSize * 0.52f))
        }

        // Throwable Grenade (White outer ring + count badge)
        UtilityOrbButton(
            size = itemSize,
            ringColor = Color.White.copy(alpha = 0.85f),
            badgeText = "$throwableCount",
            testTag = "grenade_quick_button",
            onClick = onThrowGrenade
        ) {
            Canvas(modifier = Modifier.size(itemSize * 0.52f)) {
                drawOval(Color.White, topLeft = Offset(size.width * 0.24f, size.height * 0.26f), size = Size(size.width * 0.52f, size.height * 0.64f))
                drawRect(Color.White, topLeft = Offset(size.width * 0.38f, size.height * 0.08f), size = Size(size.width * 0.24f, size.height * 0.20f))
            }
        }
    }
}

@Composable
private fun UtilityOrbButton(
    size: Dp,
    ringColor: Color,
    badgeText: String,
    testTag: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .border(2.dp, ringColor, CircleShape)
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Box(modifier = Modifier.align(Alignment.Center)) {
            content()
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 3.dp, bottom = 2.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(horizontal = 3.dp)
        ) {
            Text(badgeText, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun GoldenSprintOrb(
    isSprintLocked: Boolean,
    sizeDp: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = if (isSprintLocked) {
                        listOf(HazardYellow.copy(alpha = 0.92f), CyberOrange.copy(alpha = 0.72f), Color.Transparent)
                    } else {
                        listOf(AirdropGold.copy(alpha = 0.45f), Color.Black.copy(alpha = 0.55f))
                    }
                )
            )
            .border(
                width = if (isSprintLocked) 2.dp else 1.5.dp,
                color = if (isSprintLocked) Color.White else HazardYellow.copy(alpha = 0.8f),
                shape = CircleShape
            )
            .clickable { onClick() }
            .testTag("run_button"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
            contentDescription = "Auto Sprint Lock",
            tint = if (isSprintLocked) Color.White else HazardYellow,
            modifier = Modifier.size(sizeDp * 0.58f)
        )
    }
}

@Composable
fun ReferenceVirtualJoystick(
    sizeDp: Dp,
    onJoystickMoved: (x: Float, y: Float) -> Unit
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val knobSize = sizeDp * 0.39f

    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.25f))
            .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        dragOffset = Offset.Zero
                        onJoystickMoved(0f, 0f)
                    },
                    onDragCancel = {
                        dragOffset = Offset.Zero
                        onJoystickMoved(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = dragOffset + dragAmount
                        val dist = sqrt(newOffset.x * newOffset.x + newOffset.y * newOffset.y)
                        val maxPx = (sizeDp.toPx() * 0.42f).coerceAtLeast(36f)
                        dragOffset = if (dist <= maxPx) {
                            newOffset
                        } else {
                            Offset((newOffset.x / dist) * maxPx, (newOffset.y / dist) * maxPx)
                        }
                        onJoystickMoved(dragOffset.x / maxPx, dragOffset.y / maxPx)
                    }
                )
            }
            .testTag("virtual_joystick"),
        contentAlignment = Alignment.Center
    ) {
        // Directional N/S/E/W Triangle Indicators
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Color.White.copy(alpha = 0.65f)
            val m = size.width / 2f
            val edge = 10f
            // Top
            drawLine(c, Offset(m - 5f, edge + 5f), Offset(m, edge), strokeWidth = 2f)
            drawLine(c, Offset(m, edge), Offset(m + 5f, edge + 5f), strokeWidth = 2f)
            // Bottom
            drawLine(c, Offset(m - 5f, size.height - edge - 5f), Offset(m, size.height - edge), strokeWidth = 2f)
            drawLine(c, Offset(m, size.height - edge), Offset(m + 5f, size.height - edge - 5f), strokeWidth = 2f)
            // Left
            drawLine(c, Offset(edge + 5f, m - 5f), Offset(edge, m), strokeWidth = 2f)
            drawLine(c, Offset(edge, m), Offset(edge + 5f, m + 5f), strokeWidth = 2f)
            // Right
            drawLine(c, Offset(size.width - edge - 5f, m - 5f), Offset(size.width - edge, m), strokeWidth = 2f)
            drawLine(c, Offset(size.width - edge, m), Offset(size.width - edge - 5f, m + 5f), strokeWidth = 2f)
        }

        // Crisp White Center Thumbstick Knob
        Box(
            modifier = Modifier
                .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                .size(knobSize)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color.White, Color(0xFFD5DCE6))))
                .border(1.5.dp, Color.White, CircleShape)
        )
    }
}

@Composable
fun BottomCenterStatusAndPerks(
    user: Player,
    health: Float,
    maxHealth: Float,
    shield: Float,
    ep: Float,
    maxEp: Float,
    dashCooldownSec: Float,
    shieldCooldownSec: Float,
    medicCooldownSec: Float,
    scanCooldownSec: Float,
    onActivateDash: () -> Unit,
    onDeployShield: () -> Unit,
    onActivateMedic: () -> Unit,
    onActivateScan: () -> Unit,
    barWidth: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // 4 Interactive Tactical Skill / Perk Chips above the bars
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkillChipButton(
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                label = "Dash",
                cooldownSec = dashCooldownSec,
                isFeatured = false,
                testTag = "skill_dash_button",
                onClick = onActivateDash
            )
            SkillChipButton(
                icon = Icons.Default.Shield,
                label = "Shield",
                cooldownSec = shieldCooldownSec,
                isFeatured = false,
                testTag = "deploy_shield_button",
                onClick = onDeployShield
            )
            SkillChipButton(
                icon = Icons.Default.LocalHospital,
                label = "Medic",
                cooldownSec = medicCooldownSec,
                isFeatured = false,
                testTag = "skill_medic_button",
                onClick = onActivateMedic
            )
            SkillChipButton(
                icon = Icons.Default.GpsFixed,
                label = "Pulse",
                cooldownSec = scanCooldownSec,
                isFeatured = true,
                testTag = "skill_scan_button",
                onClick = onActivateScan
            )
        }

        // Helmet & Armor Vest Badges + Dual EP / HP Bars
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Helmet & Vest Level Icons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                GearLevelBadge(level = user.helmetLevel.coerceIn(1, 4), isHelmet = true)
                GearLevelBadge(level = user.armorLevel.coerceIn(1, 4), isHelmet = false)
            }

            Column(
                modifier = Modifier.width(barWidth),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // EP Row (Golden Yellow Bar)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "EP ${ep.roundToInt()}/${maxEp.roundToInt()}",
                        color = HazardYellow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.width(66.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((ep / maxEp).coerceIn(0f, 1f))
                                .background(HazardYellow)
                        )
                    }
                }

                // HP Row (Crisp White Bar + Blue Shield Accent if active)
                val displayHp = (health * 2f).roundToInt()
                val displayMaxHp = (maxHealth * 2f).roundToInt()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "HP $displayHp/$displayMaxHp",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.width(66.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((health / maxHealth).coerceIn(0f, 1f))
                                .background(if (health < 35f) RedZoneBombardment else Color.White)
                        )
                        if (shield > 0f) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .height(2.dp)
                                    .fillMaxWidth((shield / user.maxShield).coerceIn(0f, 1f))
                                    .background(ElectricBlueSafe)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillChipButton(
    icon: ImageVector,
    label: String,
    cooldownSec: Float,
    isFeatured: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val isReady = cooldownSec <= 0f
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(
                when {
                    !isReady -> Color.Black.copy(alpha = 0.65f)
                    isFeatured -> HazardYellow.copy(alpha = 0.30f)
                    else -> Color.Black.copy(alpha = 0.55f)
                }
            )
            .border(
                width = 1.5.dp,
                color = when {
                    !isReady -> TacticalBorder
                    isFeatured -> HazardYellow
                    else -> Color.White.copy(alpha = 0.8f)
                },
                shape = CircleShape
            )
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        if (isReady) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isFeatured) HazardYellow else Color.White,
                modifier = Modifier.size(15.dp)
            )
        } else {
            Text(
                text = "${cooldownSec.roundToInt()}",
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun GearLevelBadge(level: Int, isHelmet: Boolean) {
    val tierColor = when (level) {
        4 -> Color(0xFFFF3846)
        3 -> Color(0xFF00E5FF)
        2 -> Color(0xFFE69122)
        else -> Color(0xFF8AA29E)
    }
    Box(modifier = Modifier.size(22.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (isHelmet) {
                drawArc(
                    color = tierColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(size.width * 0.1f, size.height * 0.15f),
                    size = Size(size.width * 0.8f, size.height * 0.8f)
                )
                if (level >= 4) {
                    drawCircle(
                        color = HazardYellow,
                        center = Offset(size.width * 0.38f, size.height * 0.58f),
                        radius = 2.2f
                    )
                    drawCircle(
                        color = HazardYellow,
                        center = Offset(size.width * 0.62f, size.height * 0.58f),
                        radius = 2.2f
                    )
                }
            } else {
                drawRoundRect(
                    color = tierColor,
                    topLeft = Offset(size.width * 0.18f, size.height * 0.14f),
                    size = Size(size.width * 0.64f, size.height * 0.74f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }
        Text(
            text = "Lv$level",
            color = if (level >= 4) HazardYellow else Color.White,
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(Color.Black.copy(alpha = 0.82f), RoundedCornerShape(3.dp))
                .padding(horizontal = 2.dp)
        )
    }
}

@Composable
fun LeftHandFireButton(
    isFiring: Boolean,
    sizeDp: Dp,
    onFireChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(
                if (isFiring) RedZoneBombardment.copy(alpha = 0.72f)
                else Color.Black.copy(alpha = 0.40f)
            )
            .border(1.8.dp, if (isFiring) HazardYellow else Color.White.copy(alpha = 0.78f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onFireChange(true) },
                    onDragEnd = { onFireChange(false) },
                    onDragCancel = { onFireChange(false) },
                    onDrag = { change, _ ->
                        change.consume()
                        onFireChange(true)
                    }
                )
            }
            .clickable {
                onFireChange(!isFiring)
            }
            .testTag("left_hand_fire_button"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp * 0.58f)) {
            val c = Color.White
            rotate(degrees = -42f) {
                drawRoundRect(
                    color = c,
                    topLeft = Offset(size.width * 0.30f, size.height * 0.36f),
                    size = Size(size.width * 0.42f, size.height * 0.24f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawLine(c, Offset(size.width * 0.14f, size.height * 0.48f), Offset(size.width * 0.28f, size.height * 0.48f), strokeWidth = 2.5f, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
fun RightCombatCluster(
    isFiring: Boolean,
    onFireChange: (Boolean) -> Unit,
    onFireDragAim: (dx: Float, dy: Float) -> Unit,
    isAds: Boolean,
    onAdsToggle: () -> Unit,
    stance: PlayerStance,
    onJump: () -> Unit,
    onCrouchToggle: () -> Unit,
    onProneToggle: () -> Unit,
    onReload: () -> Unit,
    isReloading: Boolean,
    onDeployShield: () -> Unit = {},
    shieldCooldownSec: Float = 0f,
    uiScale: Float,
    modifier: Modifier = Modifier
) {
    val clusterW = (195.dp * uiScale).coerceIn(165.dp, 235.dp)
    val clusterH = (195.dp * uiScale).coerceIn(165.dp, 235.dp)
    val fireSize = (80.dp * uiScale).coerceIn(66.dp, 96.dp)
    val actionSize = (46.dp * uiScale).coerceIn(40.dp, 56.dp)

    Box(modifier = modifier.size(clusterW, clusterH)) {
        // 1. Primary Fire Button (Large Dark Translucent Disc + White Diagonal Bullet-Splash)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 8.dp, y = 12.dp)
                .size(fireSize)
                .clip(CircleShape)
                .background(
                    if (isFiring) RedZoneBombardment.copy(alpha = 0.72f)
                    else Color.Black.copy(alpha = 0.42f)
                )
                .border(2.dp, if (isFiring) HazardYellow else Color.White.copy(alpha = 0.85f), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { onFireChange(true) },
                        onDragEnd = { onFireChange(false) },
                        onDragCancel = { onFireChange(false) },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onFireChange(true)
                            onFireDragAim(dragAmount.x, dragAmount.y)
                        }
                    )
                }
                .testTag("fire_button"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(fireSize * 0.62f)) {
                val c = Color.White
                // Diagonal bullet + impact rays
                rotate(degrees = -42f) {
                    drawRoundRect(
                        color = c,
                        topLeft = Offset(size.width * 0.30f, size.height * 0.36f),
                        size = Size(size.width * 0.42f, size.height * 0.24f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawLine(c, Offset(size.width * 0.14f, size.height * 0.48f), Offset(size.width * 0.28f, size.height * 0.48f), strokeWidth = 3f, cap = StrokeCap.Round)
                    drawLine(c, Offset(size.width * 0.18f, size.height * 0.28f), Offset(size.width * 0.32f, size.height * 0.38f), strokeWidth = 2.5f, cap = StrokeCap.Round)
                    drawLine(c, Offset(size.width * 0.18f, size.height * 0.68f), Offset(size.width * 0.32f, size.height * 0.58f), strokeWidth = 2.5f, cap = StrokeCap.Round)
                }
            }
        }

        // 2. ADS / Scope Aim Button (Top-Right arc)
        ArcActionCircle(
            size = actionSize,
            isActive = isAds,
            activeColor = ElectricBlueSafe,
            testTag = "ads_scope_button",
            modifier = Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = 0.dp),
            onClick = onAdsToggle
        ) {
            Icon(
                Icons.Default.GpsFixed,
                contentDescription = "Aim Down Sights",
                tint = if (isAds) Color.Black else Color.White,
                modifier = Modifier.size(actionSize * 0.56f)
            )
        }

        // 3. Jump Button (Upper-Right arc)
        ArcActionCircle(
            size = actionSize,
            isActive = stance == PlayerStance.JUMPING,
            activeColor = CyberOrange,
            testTag = "jump_button",
            modifier = Modifier.align(Alignment.CenterEnd).offset(x = 0.dp, y = (-26).dp),
            onClick = onJump
        ) {
            Canvas(modifier = Modifier.size(actionSize * 0.56f)) {
                drawStanceSilhouette(PlayerStance.JUMPING, Color.White)
            }
        }

        // 4. Crouch Button (Lower-Right arc)
        ArcActionCircle(
            size = actionSize,
            isActive = stance == PlayerStance.CROUCH,
            activeColor = CyberOrange,
            testTag = "crouch_button",
            modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-2).dp, y = 26.dp),
            onClick = onCrouchToggle
        ) {
            Canvas(modifier = Modifier.size(actionSize * 0.56f)) {
                drawStanceSilhouette(PlayerStance.CROUCH, if (stance == PlayerStance.CROUCH) Color.Black else Color.White)
            }
        }

        // 5. Prone Button (Bottom-Right arc)
        ArcActionCircle(
            size = actionSize,
            isActive = stance == PlayerStance.PRONE,
            activeColor = CyberOrange,
            testTag = "prone_button",
            modifier = Modifier.align(Alignment.BottomCenter).offset(x = 20.dp, y = 0.dp),
            onClick = onProneToggle
        ) {
            Canvas(modifier = Modifier.size(actionSize * 0.62f)) {
                drawStanceSilhouette(PlayerStance.PRONE, if (stance == PlayerStance.PRONE) Color.Black else Color.White)
            }
        }

        // 6. Quick Reload Circle (Upper-Left of Fire for fast thumb access)
        ArcActionCircle(
            size = (34.dp * uiScale).coerceIn(30.dp, 40.dp),
            isActive = isReloading,
            activeColor = HazardYellow,
            testTag = "reload_button",
            modifier = Modifier.align(Alignment.TopStart).offset(x = 6.dp, y = 26.dp),
            onClick = onReload
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Reload Weapon",
                tint = if (isReloading) Color.Black else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        // 7. Quick Gloo Shield Deploy Orb (Upper-Center-Left of Fire)
        ArcActionCircle(
            size = (36.dp * uiScale).coerceIn(32.dp, 42.dp),
            isActive = shieldCooldownSec <= 0f,
            activeColor = Color.Black.copy(alpha = 0.52f),
            testTag = "gloo_shield_quick_button",
            modifier = Modifier.align(Alignment.TopCenter).offset(x = (-10).dp, y = 6.dp),
            onClick = onDeployShield
        ) {
            if (shieldCooldownSec <= 0f) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = "Deploy Gloo Shield",
                    tint = ElectricBlueSafe,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = "${shieldCooldownSec.roundToInt()}",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun ArcActionCircle(
    size: Dp,
    isActive: Boolean,
    activeColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isActive) activeColor else Color.Black.copy(alpha = 0.42f))
            .border(1.5.dp, if (isActive) Color.White else Color.White.copy(alpha = 0.65f), CircleShape)
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private fun DrawScope.drawStanceSilhouette(stance: PlayerStance, color: Color) {
    val w = size.width
    val h = size.height
    when (stance) {
        PlayerStance.JUMPING -> {
            drawCircle(color, radius = w * 0.13f, center = Offset(w * 0.52f, h * 0.18f))
            drawLine(color, Offset(w * 0.50f, h * 0.30f), Offset(w * 0.44f, h * 0.58f), strokeWidth = 4f, cap = StrokeCap.Round)
            drawLine(color, Offset(w * 0.44f, h * 0.58f), Offset(w * 0.25f, h * 0.78f), strokeWidth = 3.5f, cap = StrokeCap.Round)
            drawLine(color, Offset(w * 0.44f, h * 0.58f), Offset(w * 0.68f, h * 0.80f), strokeWidth = 3.5f, cap = StrokeCap.Round)
            drawLine(color, Offset(w * 0.48f, h * 0.40f), Offset(w * 0.76f, h * 0.32f), strokeWidth = 3f, cap = StrokeCap.Round)
        }
        PlayerStance.CROUCH -> {
            drawCircle(color, radius = w * 0.13f, center = Offset(w * 0.46f, h * 0.24f))
            drawLine(color, Offset(w * 0.44f, h * 0.36f), Offset(w * 0.36f, h * 0.64f), strokeWidth = 4f, cap = StrokeCap.Round)
            drawLine(color, Offset(w * 0.36f, h * 0.64f), Offset(w * 0.58f, h * 0.66f), strokeWidth = 3.5f, cap = StrokeCap.Round)
            drawLine(color, Offset(w * 0.58f, h * 0.66f), Offset(w * 0.56f, h * 0.86f), strokeWidth = 3.5f, cap = StrokeCap.Round)
            drawLine(color, Offset(w * 0.44f, h * 0.42f), Offset(w * 0.76f, h * 0.42f), strokeWidth = 3f, cap = StrokeCap.Round)
        }
        PlayerStance.PRONE -> {
            drawCircle(color, radius = w * 0.11f, center = Offset(w * 0.72f, h * 0.42f))
            drawLine(color, Offset(w * 0.14f, h * 0.62f), Offset(w * 0.74f, h * 0.58f), strokeWidth = 4.5f, cap = StrokeCap.Round)
            drawLine(color, Offset(w * 0.64f, h * 0.54f), Offset(w * 0.90f, h * 0.46f), strokeWidth = 3f, cap = StrokeCap.Round)
        }
        else -> Unit
    }
}

@Composable
fun KillFeedDisplay(
    entries: List<KillFeedEntry>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        entries.takeLast(3).forEach { entry ->
            Row(
                modifier = Modifier
                    .padding(vertical = 1.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.60f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(entry.killerName, color = CyberOrange, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(3.dp))
                Text("eliminated", color = TextSecondary, fontSize = 7.sp)
                Spacer(modifier = Modifier.width(3.dp))
                Text(entry.victimName, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(3.dp))
                Text("(${entry.weaponName})", color = HazardYellow, fontSize = 7.sp)
            }
        }
    }
}

@Composable
fun PlaneJumpOverlay(onJump: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onJump,
        colors = ButtonDefaults.buttonColors(containerColor = CyberOrange),
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(2.dp, HazardYellow, RoundedCornerShape(24.dp))
            .testTag("jump_plane_button")
    ) {
        Icon(Icons.Default.FlightTakeoff, contentDescription = "Jump", tint = Color.Black)
        Spacer(modifier = Modifier.width(8.dp))
        Text("JUMP FROM PLANE", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}

@Composable
fun SkydivingAltitudeOverlay(
    altitudeMeters: Float,
    isParachuteDeployed: Boolean,
    onDeployParachute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(TacticalSurface.copy(alpha = 0.85f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ALTITUDE", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("${altitudeMeters.roundToInt()}m", color = HazardYellow, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(8.dp))
        if (!isParachuteDeployed) {
            Button(
                onClick = onDeployParachute,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlueSafe),
                modifier = Modifier.testTag("deploy_parachute_button")
            ) {
                Text("OPEN CHUTE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        } else {
            Text("GLIDING", color = HealthGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun NearbyLootOverlay(
    lootList: List<GroundLoot>,
    onPickUp: (GroundLoot) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TacticalSurface.copy(alpha = 0.88f))
            .border(1.dp, CyberOrange.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        Text("NEARBY LOOT", color = HazardYellow, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(3.dp))
        lootList.take(3).forEach { item ->
            Row(
                modifier = Modifier
                    .padding(vertical = 1.5.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { onPickUp(item) }
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.name, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(6.dp))
                Text("PICK", color = CyberOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InMatchSettingsDialog(
    engine: BattleRoyaleEngine,
    currentGraphics: String,
    onSelectGraphics: (String) -> Unit,
    onExitMatch: () -> Unit,
    onDismiss: () -> Unit
) {
    var camSens by remember { mutableFloatStateOf(engine.cameraSensitivity) }
    var adsSens by remember { mutableFloatStateOf(engine.adsSensitivity) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberOrange)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("MATCH CONTROLS & GRAPHICS", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Text("3D GRAPHICS QUALITY", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Smooth", "Standard", "High", "Ultra").forEach { q ->
                        val active = currentGraphics.equals(q, ignoreCase = true) ||
                            (q == "Smooth" && currentGraphics.equals("Low", true)) ||
                            (q == "Standard" && (currentGraphics.equals("Medium", true) || currentGraphics.equals("Balanced", true))) ||
                            (q == "Ultra" && currentGraphics.equals("Ultra HD", true))
                        Button(
                            onClick = { onSelectGraphics(q) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) CyberOrange else TacticalSurface
                            ),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(q, color = if (active) Color.Black else TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Camera Rotation Sensitivity", color = TextPrimary, fontSize = 11.sp)
                    Text("${(camSens * 100).toInt()}%", color = CyberOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = camSens,
                    onValueChange = {
                        camSens = it
                        engine.cameraSensitivity = it
                    },
                    valueRange = 0.4f..2.2f,
                    colors = SliderDefaults.colors(thumbColor = CyberOrange, activeTrackColor = CyberOrange)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ADS Aim Sensitivity", color = TextPrimary, fontSize = 11.sp)
                    Text("${(adsSens * 100).toInt()}%", color = HazardYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = adsSens,
                    onValueChange = {
                        adsSens = it
                        engine.adsSensitivity = it
                    },
                    valueRange = 0.3f..1.8f,
                    colors = SliderDefaults.colors(thumbColor = HazardYellow, activeTrackColor = HazardYellow)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(
                        onClick = onExitMatch,
                        colors = ButtonDefaults.buttonColors(containerColor = RedZoneBombardment)
                    ) {
                        Text("LEAVE MATCH", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = HealthGreen)
                    ) {
                        Text("RESUME", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FullTacticalMapDialog(
    engine: BattleRoyaleEngine,
    onDismiss: () -> Unit
) {
    val state by engine.engineState.collectAsState()
    val user = engine.userPlayer

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalDarkBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TACTICAL MAP • ${engine.mapConfig.name.uppercase()}", color = CyberOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MilitaryGreen.copy(alpha = 0.25f))
                ) {
                    val scale = size.width / engine.mapConfig.worldSize
                    engine.mapConfig.zones.forEach { z ->
                        drawCircle(
                            color = Color.White.copy(alpha = 0.15f),
                            center = Offset(z.x * scale, z.y * scale),
                            radius = z.radius * scale
                        )
                    }
                    drawCircle(
                        color = ElectricBlueSafe,
                        center = Offset(state.safeZone.safeCenterX * scale, state.safeZone.safeCenterY * scale),
                        radius = state.safeZone.safeRadius * scale,
                        style = Stroke(width = 2.5f)
                    )
                    drawCircle(
                        color = ElectricBlueZone,
                        center = Offset(state.safeZone.currentBlueCenterX * scale, state.safeZone.currentBlueCenterY * scale),
                        radius = state.safeZone.currentBlueRadius * scale,
                        style = Stroke(width = 3.5f)
                    )
                    drawCircle(
                        color = HazardYellow,
                        center = Offset(user.x * scale, user.y * scale),
                        radius = 6f
                    )
                }
            }
        }
    }
}

@Composable
fun BackpackInventoryDialog(
    user: Player,
    usedUnits: Int = 0,
    maxUnits: Int = 180,
    onUseConsumable: (ConsumableType) -> Unit,
    onDropConsumable: (ConsumableType) -> Unit = {},
    onDropWeaponSlot: (Int) -> Unit = {},
    onDismiss: () -> Unit
) {
    val capRatio = if (maxUnits > 0) (usedUnits.toFloat() / maxUnits.toFloat()).coerceIn(0f, 1f) else 0f
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalDarkBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TACTICAL BACKPACK (LV.${user.backpackLevel})", color = CyberOrange, fontWeight = FontWeight.Bold)
                        Text("Capacity: $usedUnits / $maxUnits units", color = TextSecondary, fontSize = 10.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                LinearProgressIndicator(
                    progress = { capRatio },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color = if (capRatio > 0.85f) RedZoneBombardment else ElectricBlueSafe,
                    trackColor = TacticalSurface
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("EQUIPPED WEAPONS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                listOf(1 to user.primaryWeapon1, 2 to user.primaryWeapon2).forEach { (slot, w) ->
                    if (w != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TacticalSurface)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(w.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("${w.currentAmmo} / ${w.totalCapacity} (${w.ammoType.displayName})", color = HazardYellow, fontSize = 10.sp)
                            }
                            Text(
                                text = "DROP",
                                color = RedZoneBombardment,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onDropWeaponSlot(slot) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("drop_weapon_slot_$slot")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("MEDICAL & EP SUPPLIES", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                ConsumableType.entries.forEach { c ->
                    val count = user.consumableInventory[c] ?: 0
                    if (count > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TacticalSurface)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${c.displayName} x$count", color = HealthGreen, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "DROP",
                                    color = RedZoneBombardment,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.clickable { onDropConsumable(c) }
                                )
                                Text(
                                    text = "USE",
                                    color = CyberOrange,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable { onUseConsumable(c) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchSettlementDialog(
    placement: Int,
    totalPlayers: Int = 100,
    kills: Int,
    assists: Int = 0,
    damage: Float,
    durationSec: Int,
    onReturnToLobby: () -> Unit
) {
    val isVictory = placement == 1

    Dialog(onDismissRequest = { /* Require explicit button click */ }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
            border = androidx.compose.foundation.BorderStroke(2.dp, if (isVictory) HazardYellow else RedZoneBombardment)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isVictory) "VICTORY ROYALE (#1)" else "#$placement / $totalPlayers",
                    color = if (isVictory) HazardYellow else Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (isVictory) "BOOYAH! APEX SURVIVOR" else "DEFEAT • TACTICAL EXTRACTION",
                    color = if (isVictory) CyberOrange else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatBox("KILLS", "$kills")
                    StatBox("ASSISTS", "$assists")
                    StatBox("DAMAGE", "${damage.roundToInt()}")
                    val m = durationSec / 60
                    val s = durationSec % 60
                    StatBox("TIME", String.format(Locale.US, "%02d:%02d", m, s))
                    StatBox("RATING", if (isVictory) "+38 RP" else "+12 RP")
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onReturnToLobby,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberOrange),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("return_to_lobby_button")
                ) {
                    Text("CONTINUE TO LOBBY", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}
