package com.example.game.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.example.game.engine.BattleRoyaleEngine
import com.example.game.model.EmotePreset
import com.example.game.model.GameModeType
import com.example.game.model.MatchPhase
import com.example.game.model.PlayerStance
import com.example.game.model.ScopeType
import com.example.ui.screens.HudLayoutCodec
import com.example.ui.theme.BloodKnockout
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.ElectricBlueSafe
import com.example.ui.theme.HazardYellow
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.RedZoneBombardment
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun BattleScreen(
    engine: BattleRoyaleEngine,
    hudScale: Float = 1.0f,
    hudOpacity: Float = 0.92f,
    hudLayoutJson: String = "",
    graphicsQuality: String = "High",
    graphicsConfig: com.example.game.graphics.GraphicsConfig = com.example.game.graphics.GraphicsConfig.fromPreset(graphicsQuality),
    leftHandFireEnabled: Boolean = true,
    onUpdateGraphicsQuality: (String) -> Unit = {},
    onExitToLobby: () -> Unit
) {
    val state by engine.engineState.collectAsState()
    val user = engine.userPlayer

    var showFullMap by remember { mutableStateOf(false) }
    var showBackpack by remember { mutableStateOf(false) }
    var showCommsMenu by remember { mutableStateOf(false) }
    var showEmoteWheel by remember { mutableStateOf(false) }
    var showInMatchSettings by remember { mutableStateOf(false) }
    var isMicOn by remember { mutableStateOf(true) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var activeGraphics by remember(graphicsQuality) { mutableStateOf(graphicsQuality) }
    var activeConfig by remember(graphicsConfig) { mutableStateOf(graphicsConfig) }
    var surfaceViewRef by remember { mutableStateOf<Battle3DSurfaceView?>(null) }
    var freeLookDeg by remember { mutableFloatStateOf(0f) }
    val customSlots = remember(hudLayoutJson) { HudLayoutCodec.parse(hudLayoutJson) }

    val renderer = remember(engine) {
        Battle3DRenderer(engine = engine, graphicsQuality = activeGraphics, initialConfig = activeConfig)
    }
    LaunchedEffect(activeConfig) {
        renderer.graphicsConfig = activeConfig
    }
    LaunchedEffect(freeLookDeg) {
        renderer.freeLookOffsetDeg = freeLookDeg
    }

    val damagePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            style = Paint.Style.FILL
        }
    }
    val damageOutlinePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeWidth = 7.5f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("battle_screen_container")
    ) {
        // 1. REAL-TIME 3D THIRD-PERSON OVER-THE-SHOULDER OPENGL VIEWPORT
        AndroidView(
            factory = { ctx ->
                Battle3DSurfaceView(ctx, renderer).also { surfaceViewRef = it }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. FULL-SCREEN CAMERA ROTATION LAYER + 3D WORLD-PROJECTED COMBAT FEEDBACK
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        engine.rotateCamera(dragAmount.x, dragAmount.y)
                    }
                }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Project Shield & Health Bars above damaged or radar-scanned enemies
            val showAllEnemyBars = state.radarScanActiveSec > 0f
            state.playersSnapshot
                .filter { !it.isUser && it.isAlive && it.teamId != user.teamId && (showAllEnemyBars || it.health < it.maxHealth || it.shield < it.maxShield) }
                .forEach { enemy ->
                    val proj = renderer.projectWorldToScreen(enemy.x, enemy.y, enemy.z + 27f)
                    if (proj.isVisible) {
                        val barW = 64f
                        drawRect(
                            color = Color.Black.copy(alpha = 0.65f),
                            topLeft = Offset(proj.x - barW / 2f, proj.y - 10f),
                            size = Size(barW, 9f)
                        )
                        drawRect(
                            color = ElectricBlueSafe,
                            topLeft = Offset(proj.x - barW / 2f, proj.y - 10f),
                            size = Size(barW * (enemy.shield / enemy.maxShield).coerceIn(0f, 1f), 3.5f)
                        )
                        drawRect(
                            color = RedZoneBombardment,
                            topLeft = Offset(proj.x - barW / 2f, proj.y - 5.5f),
                            size = Size(barW * (enemy.health / enemy.maxHealth).coerceIn(0f, 1f), 4.5f)
                        )
                    }
                }

            // Floating 3D Damage Popups (Bold outlined crimson/gold crits matching combat reference)
            state.damagePopups.forEach { pop ->
                val progress = (pop.ageSec / pop.durationSec).coerceIn(0f, 1f)
                val proj = renderer.projectWorldToScreen(pop.worldX, pop.worldY, pop.worldZ + progress * 24f)
                if (proj.isVisible) {
                    val alphaInt = ((1f - progress * 0.92f) * 255).toInt().coerceIn(0, 255)
                    val popScale = if (progress < 0.16f) 0.85f + (progress / 0.16f) * 0.30f else 1.15f - (progress - 0.16f) * 0.22f
                    val sizedPx = (if (pop.isHeadshot) 58f else 44f) * popScale
                    damageOutlinePaint.textSize = sizedPx
                    damageOutlinePaint.color = android.graphics.Color.argb(alphaInt, 10, 12, 16)
                    damagePaint.textSize = sizedPx
                    damagePaint.color = when {
                        pop.isHeadshot -> android.graphics.Color.argb(alphaInt, 255, 36, 48)
                        pop.isShieldHit -> android.graphics.Color.argb(alphaInt, 0, 224, 255)
                        else -> android.graphics.Color.argb(alphaInt, 255, 214, 42)
                    }
                    val dmgLabel = "${pop.amount}"
                    drawContext.canvas.nativeCanvas.drawText(dmgLabel, proj.x, proj.y, damageOutlinePaint)
                    drawContext.canvas.nativeCanvas.drawText(dmgLabel, proj.x, proj.y, damagePaint)
                }
            }

            // Dynamic Crosshair & Crisp Outlined Red/White Hitmarker X
            if (state.phase == MatchPhase.ACTIVE_COMBAT && !state.isSpectating) {
                if (engine.isAimingDownSights) {
                    drawScopeReticle(user.activeWeapon?.equippedScope ?: ScopeType.RED_DOT, cx, cy)
                } else {
                    val gap = if (user.isSprinting) 18f else if (engine.isFiringInput) 15f else 10f
                    val arm = 12f
                    val crossColor = Color.White.copy(alpha = 0.90f)
                    drawLine(crossColor, Offset(cx - gap - arm, cy), Offset(cx - gap, cy), strokeWidth = 3f)
                    drawLine(crossColor, Offset(cx + gap, cy), Offset(cx + gap + arm, cy), strokeWidth = 3f)
                    drawLine(crossColor, Offset(cx, cy - gap - arm), Offset(cx, cy - gap), strokeWidth = 3f)
                    drawLine(crossColor, Offset(cx, cy + gap), Offset(cx, cy + gap + arm), strokeWidth = 3f)
                    drawCircle(CyberOrange, center = Offset(cx, cy), radius = 2.8f)
                }

                if (System.currentTimeMillis() - state.lastHitTimestampMs < 240L) {
                    val hmColor = if (state.lastHitWasHeadshot) RedZoneBombardment else Color.White
                    val hmOutline = Color.Black.copy(alpha = 0.72f)
                    val d1 = 9f
                    val d2 = 26f
                    drawLine(hmOutline, Offset(cx - d2, cy - d2), Offset(cx - d1, cy - d1), strokeWidth = 7.4f)
                    drawLine(hmOutline, Offset(cx + d1, cy - d1), Offset(cx + d2, cy - d2), strokeWidth = 7.4f)
                    drawLine(hmOutline, Offset(cx - d2, cy + d2), Offset(cx - d1, cy + d1), strokeWidth = 7.4f)
                    drawLine(hmOutline, Offset(cx + d1, cy + d1), Offset(cx + d2, cy + d2), strokeWidth = 7.4f)
                    drawLine(hmColor, Offset(cx - d2, cy - d2), Offset(cx - d1, cy - d1), strokeWidth = 4.6f)
                    drawLine(hmColor, Offset(cx + d1, cy - d1), Offset(cx + d2, cy - d2), strokeWidth = 4.6f)
                    drawLine(hmColor, Offset(cx - d2, cy + d2), Offset(cx - d1, cy + d1), strokeWidth = 4.6f)
                    drawLine(hmColor, Offset(cx + d1, cy + d1), Offset(cx + d2, cy + d2), strokeWidth = 4.6f)
                }
            }
        }

        // 3. RESPONSIVE 16:9 SAFE-AREA GAMEPLAY HUD (EXACT REFERENCE DIRECTION)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .alpha(hudOpacity.coerceIn(0.6f, 1.0f))
        ) {
            val density = LocalDensity.current
            val areaW = with(density) { maxWidth.toPx() }
            val areaH = with(density) { maxHeight.toPx() }
            fun slotMod(id: String): Modifier {
                val s = customSlots[id] ?: return Modifier
                return Modifier
                    .offset { IntOffset((s.offsetX * areaW).roundToInt(), (s.offsetY * areaH).roundToInt()) }
                    .alpha(s.opacity)
            }
            fun slotScale(id: String): Float = customSlots[id]?.scale ?: 1f

            val uiScale = ((maxHeight.value / 412f).coerceIn(0.76f, 1.24f) * hudScale).coerceIn(0.74f, 1.28f)
            val miniMapSize = (96.dp * uiScale * slotScale("MINIMAP")).coerceIn(78.dp, 138.dp)
            val compassWidth = (240.dp * uiScale).coerceIn(190.dp, 310.dp)
            val weaponPanelWidth = (182.dp * uiScale * slotScale("WEAPON_PANEL")).coerceIn(155.dp, 240.dp)
            val joystickSize = (114.dp * uiScale * slotScale("JOYSTICK")).coerceIn(90.dp, 152.dp)
            val sprintOrbSize = (46.dp * uiScale).coerceIn(38.dp, 58.dp)
            val barWidth = (224.dp * uiScale * slotScale("STATUS_BAR")).coerceIn(175.dp, 295.dp)

            // TOP-LEFT: Square Minimap + Side Comms/Pet Column + Live 4-Player Squad Panel
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .then(slotMod("MINIMAP"))
                    .padding(start = 8.dp, top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    ReferenceMiniMap(
                        engine = engine,
                        players = state.playersSnapshot,
                        safeCenterX = state.safeZone.safeCenterX,
                        safeCenterY = state.safeZone.safeCenterY,
                        safeRadius = state.safeZone.safeRadius,
                        blueCenterX = state.safeZone.currentBlueCenterX,
                        blueCenterY = state.safeZone.currentBlueCenterY,
                        blueRadius = state.safeZone.currentBlueRadius,
                        radarActive = state.radarScanActiveSec > 0f,
                        sizeDp = miniMapSize,
                        onClick = { showFullMap = true }
                    )

                    MapSideCommsColumn(
                        isSpeakerOn = isSpeakerOn,
                        onToggleSpeaker = {
                            isSpeakerOn = !isSpeakerOn
                            engine.setAudioMuted(!isSpeakerOn)
                        },
                        isMicOn = isMicOn,
                        onToggleMic = {
                            isMicOn = !isMicOn
                            engine.sendQuickComms(if (isMicOn) "Voice Channel Open" else "Voice Muted")
                        },
                        onEmoteClick = { showEmoteWheel = !showEmoteWheel },
                        onChatClick = { showCommsMenu = !showCommsMenu },
                        onCompanionScanClick = { engine.activateRadarScan() },
                        scanReady = state.scanCooldownSec <= 0f,
                        uiScale = uiScale
                    )
                }

                // Live Squad Panel directly below Minimap
                if (state.squadMembers.isNotEmpty()) {
                    SquadStatusPanel(
                        members = state.squadMembers,
                        widthDp = miniMapSize + 18.dp
                    )
                }
            }

            // TOP-CENTER: Sliding Degree Compass + Safe Zone Alert Banner + Takedown Alert
            TacticalCompassBar(
                yawDegrees = state.cameraYawDegrees,
                isShrinking = state.safeZone.isShrinking,
                zoneTimerSec = state.safeZone.timeUntilShrinkSec,
                phase = state.safeZone.phase,
                widthDp = compassWidth,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
            )

            AnimatedVisibility(
                visible = state.takedownBanner != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, RedZoneBombardment.copy(alpha = 0.88f), Color.Transparent)
                            )
                        )
                        .border(1.dp, HazardYellow, RoundedCornerShape(8.dp))
                        .padding(horizontal = 22.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = state.takedownBanner ?: "",
                        color = HazardYellow,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            // RADIO COMMS CALLOUT BANNER
            if (state.radioCommsBanner != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 92.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TacticalDarkBg.copy(alpha = 0.88f))
                        .border(1.dp, ElectricBlueSafe, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(state.radioCommsBanner!!, color = ElectricBlueSafe, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            // QUICK CHAT POPUP MENU
            AnimatedVisibility(
                visible = showCommsMenu,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = miniMapSize + 44.dp, top = 74.dp)
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TacticalDarkBg.copy(alpha = 0.94f))
                        .border(1.dp, ElectricBlueSafe.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Enemies Ahead!", "Need Ammo!", "Get to Safe Zone!", "Cover Me!").forEach { msg ->
                        Text(
                            text = msg,
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    engine.sendQuickComms(msg)
                                    showCommsMenu = false
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // EMOTE WHEEL POPUP
            AnimatedVisibility(
                visible = showEmoteWheel,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = miniMapSize + 44.dp, top = 46.dp)
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(TacticalDarkBg.copy(alpha = 0.95f))
                        .border(1.dp, HazardYellow.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("EMOTE WHEEL", color = HazardYellow, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    EmotePreset.entries.forEach { emote ->
                        Text(
                            text = "${emote.badge} ${emote.label}",
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    engine.triggerEmote(emote)
                                    showEmoteWheel = false
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // TOP-RIGHT: Latency Pill + Coins, Alive, Kills, Settings Strip + Weapon Inventory & Ammo Panel + Kill Feed
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .then(slotMod("WEAPON_PANEL"))
                    .padding(top = 6.dp, end = 8.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.58f))
                            .border(1.dp, HealthGreen.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("${state.latencyMs}ms", color = HealthGreen, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    TopRightStatusStrip(
                        coins = state.matchCoins,
                        aliveCount = state.aliveCount,
                        killsCount = state.userKills,
                        onCoinsClick = { engine.spendCoinsForSupply() },
                        onSettingsClick = { showInMatchSettings = true }
                    )
                }

                WeaponInventoryPanel(
                    user = user,
                    isReloading = state.isReloading,
                    reloadProgress = state.reloadProgress,
                    panelWidth = weaponPanelWidth,
                    onReloadClick = { engine.reloadWeapon(user) },
                    onSelectSlot = { slot -> user.activeSlot = slot }
                )

                if (engine.gameMode == GameModeType.TRAINING_GROUND) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TacticalDarkBg.copy(alpha = 0.85f))
                            .border(1.dp, CyberOrange.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "RANGE HITS: ${state.trainingDummyHits} • DMG: ${state.trainingDummyDamage}",
                            color = HazardYellow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                KillFeedDisplay(entries = state.killFeed)
            }

            // PLANE FLIGHT / SKYDIVING OVERLAYS
            if (state.phase == MatchPhase.PLANE_FLIGHT && state.isUserInPlane) {
                PlaneJumpOverlay(
                    onJump = { engine.jumpFromPlane() },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (state.phase == MatchPhase.SKYDIVING) {
                SkydivingAltitudeOverlay(
                    altitudeMeters = state.altitudeMeters,
                    isParachuteDeployed = state.isParachuteDeployed,
                    onDeployParachute = { engine.deployParachute() },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp)
                )
            }

            // UAV RECON DRONE ACTIVE BADGE
            if (state.radarScanActiveSec > 0f && !state.isSpectating) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 116.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xD80A1624))
                        .border(1.2.dp, ElectricBlueSafe, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "UAV DRONE SCANNING • ${state.radarScanActiveSec.roundToInt()}s",
                        color = ElectricBlueSafe,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // WARRIOR'S LOOT BOMB CRATE COUNTDOWN & DISARM CTA
            val nearbyTrap = state.nearbyWarriorTrap
            if (nearbyTrap != null && !state.isSpectating) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 172.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xEB1A080C))
                        .border(1.8.dp, CyberOrange, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("warrior_loot_trap_banner"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column {
                        Text(
                            text = "WARRIOR'S LOOT BOMB • ${String.format(java.util.Locale.US, "%.1fs", nearbyTrap.fuseRemainingSec.coerceAtLeast(0f))}",
                            color = HazardYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Explodes if not looted! Drops Lv4 Armor + ${nearbyTrap.rewardWeapon?.name ?: "Vault Gun"}",
                            color = TextPrimary,
                            fontSize = 9.sp
                        )
                    }
                    Button(
                        onClick = { engine.disarmAndLootWarriorTrap(nearbyTrap.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberOrange, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp).testTag("disarm_warrior_loot_button")
                    ) {
                        Text("DISARM & LOOT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // NEARBY LOOT QUICK-PICKUP
            if (state.nearbyLoot.isNotEmpty() && !state.isSpectating) {
                NearbyLootOverlay(
                    lootList = state.nearbyLoot,
                    onPickUp = { engine.pickUpLoot(it) },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 66.dp, top = 12.dp)
                )
            }

            // KNOCKED SQUADMATE REVIVE PROMPT & PROGRESS
            if (state.isRevivingTeammate) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TacticalDarkBg.copy(alpha = 0.90f))
                        .border(1.5.dp, HealthGreen, RoundedCornerShape(10.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("REVIVING SQUADMATE... ${(state.reviveProgress * 100).toInt()}%", color = HealthGreen, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { state.reviveProgress },
                        modifier = Modifier.width(150.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = HealthGreen,
                        trackColor = TacticalSurface
                    )
                }
            } else if (state.nearbyKnockedAllyName != null && !state.isSpectating) {
                Button(
                    onClick = { engine.startRevive() },
                    colors = ButtonDefaults.buttonColors(containerColor = HealthGreen),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 126.dp)
                        .testTag("revive_ally_button")
                ) {
                    Text("REVIVE ${state.nearbyKnockedAllyName?.uppercase()}", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }

            // VEHICLE INTERACTION & DASHBOARD
            if (state.nearbyVehicle != null && user.vehicleId == null && !state.isSpectating) {
                Button(
                    onClick = { engine.enterVehicle(state.nearbyVehicle!!) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberOrange, contentColor = Color.White),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 86.dp)
                        .testTag("drive_vehicle_button")
                ) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = "Drive", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DRIVE ${state.nearbyVehicle!!.type.displayName}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else if (user.vehicleId != null && !state.isSpectating) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 86.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(TacticalDarkBg.copy(alpha = 0.90f))
                        .border(1.dp, CyberOrange, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${state.vehicleSpeedKmh}", color = HazardYellow, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("KM/H • FUEL 92%", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    val nitroFg = if (state.vehicleNitroActive) Color.Black else Color.White
                    Button(
                        onClick = { engine.triggerNitroBoost() },
                        colors = ButtonDefaults.buttonColors(containerColor = if (state.vehicleNitroActive) HazardYellow else CyberOrange, contentColor = nitroFg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp).testTag("nitro_button")
                    ) {
                        Icon(Icons.Default.ElectricBolt, contentDescription = "Nitro", tint = nitroFg, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (state.vehicleNitroActive) "NITRO!" else "NITRO", color = nitroFg, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { engine.honkVehicleHorn() },
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalSurface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp).testTag("horn_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Horn", tint = TextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("HORN", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { engine.exitVehicle() },
                        colors = ButtonDefaults.buttonColors(containerColor = BloodKnockout),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp).testTag("exit_vehicle_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Exit", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EXIT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // SPECTATOR MODE OVERLAY
            if (state.isSpectating) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TacticalDarkBg.copy(alpha = 0.92f))
                        .border(1.5.dp, BloodKnockout, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ELIMINATED • RANK #${state.userPlacement}", color = BloodKnockout, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text(
                        text = "SPECTATING: ${state.spectatedPlayer?.name ?: "Unknown"} (HP: ${state.spectatedPlayer?.health?.toInt() ?: 0}%)",
                        color = HazardYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 18.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(TacticalDarkBg.copy(alpha = 0.94f))
                        .border(1.5.dp, CyberOrange, RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { engine.spectatePrev() },
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalSurface)
                    ) {
                        Text("◀ PREV", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { engine.spectateNext() },
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalSurface)
                    ) {
                        Text("NEXT ▶", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { engine.exitSpectatingToSettlement() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberOrange, contentColor = Color.White),
                        modifier = Modifier.testTag("exit_spectating_button")
                    ) {
                        Text("VIEW RESULTS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // MAIN PLAYABLE COMBAT HUD CONTROLS
            if (!state.isSpectating) {
                // FREE-LOOK 360° EYE PEEK ORB (Drag to peek around operative without turning aim)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 14.dp, bottom = 52.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = if (freeLookDeg != 0f) 0.72f else 0.42f))
                        .border(
                            width = 1.4.dp,
                            color = if (freeLookDeg != 0f) HazardYellow else Color.White.copy(alpha = 0.68f),
                            shape = CircleShape
                        )
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    freeLookDeg = 0f
                                    renderer.freeLookOffsetDeg = 0f
                                },
                                onDragCancel = {
                                    freeLookDeg = 0f
                                    renderer.freeLookOffsetDeg = 0f
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                freeLookDeg = (freeLookDeg + dragAmount.x * 0.45f).coerceIn(-165f, 165f)
                                renderer.freeLookOffsetDeg = freeLookDeg
                            }
                        }
                        .testTag("eye_peek_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Free Look Eye Peek",
                        tint = if (freeLookDeg != 0f) HazardYellow else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // LEFT CLAW FIRE BUTTON (3-finger/4-finger Free Fire & BGMI style)
                if (leftHandFireEnabled) {
                    LeftHandFireButton(
                        isFiring = engine.isFiringInput,
                        sizeDp = (46.dp * uiScale * slotScale("LEFT_FIRE")).coerceIn(38.dp, 64.dp),
                        onFireChange = { engine.isFiringInput = it },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .then(slotMod("LEFT_FIRE"))
                            .padding(start = 12.dp, top = 28.dp)
                    )
                }

                // LEFT EDGE & BOTTOM-LEFT: Backpack/Medkit/Grenade Column + Golden Sprint Lock + Virtual Joystick
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .then(slotMod("JOYSTICK"))
                        .padding(start = 10.dp, bottom = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LeftUtilityColumn(
                            backpackLevel = user.backpackLevel,
                            medkitCount = state.medkitCount,
                            throwableCount = state.throwableCount,
                            onOpenBackpack = { showBackpack = true },
                            onUseMedkit = { engine.useQuickHeal() },
                            onThrowGrenade = { engine.throwItem() },
                            uiScale = uiScale * slotScale("UTILITY_COL")
                        )

                        Box(modifier = Modifier.padding(bottom = 2.dp)) {
                            ReferenceVirtualJoystick(
                                sizeDp = joystickSize,
                                onJoystickMoved = { x, y ->
                                    engine.moveJoystickX = x
                                    engine.moveJoystickY = y
                                }
                            )
                        }
                    }

                    // Golden Glowing Auto-Sprint Orb positioned above-right of Joystick (exact reference position)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 18.dp, y = (-26).dp)
                    ) {
                        GoldenSprintOrb(
                            isSprintLocked = state.isSprintLocked,
                            sizeDp = sprintOrbSize,
                            onClick = { engine.toggleSprintLock() }
                        )
                    }
                }

                // BOTTOM-CENTER: 4 Tactical Skill Chips + Helmet/Vest Badges + Dual EP & HP Bars
                BottomCenterStatusAndPerks(
                    user = user,
                    health = state.userHealth,
                    maxHealth = state.userMaxHealth,
                    shield = state.userShield,
                    ep = state.userEp,
                    maxEp = state.userMaxEp,
                    dashCooldownSec = state.dashCooldownSec,
                    shieldCooldownSec = state.shieldCooldownSec,
                    medicCooldownSec = state.medicCooldownSec,
                    scanCooldownSec = state.scanCooldownSec,
                    onActivateDash = { engine.activateDashSkill() },
                    onDeployShield = { engine.deployTacticalShield() },
                    onActivateMedic = { engine.activateMedicSkill() },
                    onActivateScan = { engine.activateRadarScan() },
                    barWidth = barWidth,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .then(slotMod("STATUS_BAR"))
                        .padding(bottom = 8.dp)
                )

                // BOTTOM-RIGHT: Large Bullet-Splash Fire Button + Arc of Aim / Jump / Crouch / Prone / Reload / Gloo Shield
                RightCombatCluster(
                    isFiring = engine.isFiringInput,
                    onFireChange = { engine.isFiringInput = it },
                    onFireDragAim = { dx, dy -> engine.rotateCamera(dx, dy) },
                    isAds = engine.isAimingDownSights,
                    onAdsToggle = { engine.isAimingDownSights = !engine.isAimingDownSights },
                    stance = user.stance,
                    onJump = { engine.jump() },
                    onCrouchToggle = {
                        user.stance = if (user.stance == PlayerStance.CROUCH) PlayerStance.STAND else PlayerStance.CROUCH
                    },
                    onProneToggle = {
                        user.stance = if (user.stance == PlayerStance.PRONE) PlayerStance.STAND else PlayerStance.PRONE
                    },
                    onReload = { engine.reloadWeapon(user) },
                    isReloading = state.isReloading,
                    onDeployShield = { engine.deployTacticalShield() },
                    shieldCooldownSec = state.shieldCooldownSec,
                    uiScale = uiScale * slotScale("RIGHT_CLUSTER"),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .then(slotMod("FIRE"))
                        .padding(end = 10.dp, bottom = 8.dp)
                )
            }
        }
    }

    if (showFullMap) {
        FullTacticalMapDialog(engine = engine, onDismiss = { showFullMap = false })
    }

    if (showBackpack) {
        BackpackInventoryDialog(
            user = user,
            usedUnits = state.backpackUsedUnits,
            maxUnits = state.backpackMaxUnits,
            onUseConsumable = { engine.useConsumable(it) },
            onDropConsumable = { engine.dropConsumable(it) },
            onDropWeaponSlot = { engine.dropWeaponSlot(it) },
            onDismiss = { showBackpack = false }
        )
    }

    if (showInMatchSettings) {
        InMatchSettingsDialog(
            engine = engine,
            currentGraphics = activeGraphics,
            onSelectGraphics = { q ->
                activeGraphics = q
                activeConfig = com.example.game.graphics.GraphicsConfig.fromPreset(q)
                onUpdateGraphicsQuality(q)
            },
            onExitMatch = {
                showInMatchSettings = false
                onExitToLobby()
            },
            onDismiss = { showInMatchSettings = false }
        )
    }

    if (state.phase == MatchPhase.MATCH_ENDED) {
        MatchSettlementDialog(
            placement = state.userPlacement,
            totalPlayers = state.totalPlayers,
            kills = state.userKills,
            assists = state.userAssists,
            damage = state.userDamage,
            durationSec = state.matchDurationSec,
            onReturnToLobby = onExitToLobby
        )
    }
}

private fun DrawScope.drawScopeReticle(scope: ScopeType, cx: Float, cy: Float) {
    when (scope) {
        ScopeType.RED_DOT, ScopeType.HOLO, ScopeType.IRON_SIGHT -> {
            val radius = if (scope == ScopeType.IRON_SIGHT) 140f else 185f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                    center = Offset(cx, cy),
                    radius = radius * 1.3f
                ),
                center = Offset(cx, cy),
                radius = radius * 1.3f
            )
            drawCircle(Color.Black.copy(alpha = 0.78f), center = Offset(cx, cy), radius = radius, style = Stroke(width = 8f))
            drawCircle(Color.Red.copy(alpha = 0.48f), center = Offset(cx, cy), radius = 18f, style = Stroke(width = 2f))
            drawLine(Color.Red.copy(alpha = 0.75f), Offset(cx - 28f, cy), Offset(cx - 8f, cy), strokeWidth = 2f)
            drawLine(Color.Red.copy(alpha = 0.75f), Offset(cx + 8f, cy), Offset(cx + 28f, cy), strokeWidth = 2f)
            drawLine(Color.Red.copy(alpha = 0.75f), Offset(cx, cy - 28f), Offset(cx, cy - 8f), strokeWidth = 2f)
            drawLine(Color.Red.copy(alpha = 0.75f), Offset(cx, cy + 8f), Offset(cx, cy + 28f), strokeWidth = 2f)
            drawCircle(Color.Red, center = Offset(cx, cy), radius = 4.2f)
        }
        ScopeType.SCOPE_2X, ScopeType.SCOPE_4X, ScopeType.SCOPE_8X -> {
            drawRect(Color.Black.copy(alpha = 0.48f))
            drawCircle(Color.Black, center = Offset(cx, cy), radius = 250f, style = Stroke(width = 10f))
            drawCircle(Color.Cyan.copy(alpha = 0.35f), center = Offset(cx, cy), radius = 244f, style = Stroke(width = 2f))
            drawLine(Color.Black, start = Offset(cx - 245f, cy), end = Offset(cx + 245f, cy), strokeWidth = 2.5f)
            drawLine(Color.Black, start = Offset(cx, cy - 245f), end = Offset(cx, cy + 245f), strokeWidth = 2.5f)
            for (i in 1..4) {
                val offset = i * 35f
                drawLine(Color.Red, start = Offset(cx - 10f, cy + offset), end = Offset(cx + 10f, cy + offset), strokeWidth = 2.2f)
            }
            drawCircle(Color.Red, center = Offset(cx, cy), radius = 4f)
        }
    }
}
