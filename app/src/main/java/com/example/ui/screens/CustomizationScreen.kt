package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Security
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameRepository
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.ElectricBlueSafe
import com.example.ui.theme.HazardYellow
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.RarityEpic
import com.example.ui.theme.RarityLegendary
import com.example.ui.theme.RarityMythic
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun CustomizationScreen(
    repository: GameRepository,
    onBack: () -> Unit
) {
    val items by repository.inventory.collectAsState(initial = emptyList())
    val profile by repository.userProfile.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableIntStateOf(0) }
    var inspectingItem by remember { androidx.compose.runtime.mutableStateOf<com.example.data.InventoryItemEntity?>(null) }

    val categories = listOf("CHARACTER", "WEAPON_SKIN", "OUTFIT", "HELMET", "BACKPACK", "VEHICLE")

    val filteredItems = if (selectedCategory == 0) emptyList() else items.filter { it.type == categories[selectedCategory] }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalDarkBg)
            .padding(16.dp)
            .testTag("customization_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("armory_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("TACTICAL ARMORY & WARDROBE", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("EQUIP WEAPONS, SKINS & GEAR", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Categories TabRow
        TabRow(
            selectedTabIndex = selectedCategory,
            containerColor = TacticalSurface,
            contentColor = CyberOrange
        ) {
            Tab(selected = selectedCategory == 0, onClick = { selectedCategory = 0 }, text = { Text("CHARACTERS", fontSize = 10.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = selectedCategory == 1, onClick = { selectedCategory = 1 }, text = { Text("GUNS", fontSize = 10.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = selectedCategory == 2, onClick = { selectedCategory = 2 }, text = { Text("SUITS", fontSize = 10.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = selectedCategory == 3, onClick = { selectedCategory = 3 }, text = { Text("HELMETS", fontSize = 10.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = selectedCategory == 4, onClick = { selectedCategory = 4 }, text = { Text("PACKS", fontSize = 10.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = selectedCategory == 5, onClick = { selectedCategory = 5 }, text = { Text("VEHICLES", fontSize = 10.sp, fontWeight = FontWeight.Bold) })
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedCategory == 0) {
            val activePresetId = profile?.selectedCharacterId ?: "vortex_ghost"
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(com.example.game.model.CharacterPreset.entries) { preset ->
                    val isSelected = preset.id == activePresetId
                    val accentColor = Color(preset.accentRgb.first, preset.accentRgb.second, preset.accentRgb.third)
                    val armorColor = Color(preset.armorRgb.first, preset.armorRgb.second, preset.armorRgb.third)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { scope.launch { repository.updateSelectedCharacter(preset.id) } }
                            .testTag("select_character_${preset.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, CyberOrange) else null
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(armorColor).border(1.dp, Color.White, RoundedCornerShape(4.dp)))
                                    Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(accentColor).border(1.dp, Color.White, RoundedCornerShape(4.dp)))
                                }
                                if (isSelected) {
                                    Text("ACTIVE", color = HealthGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Text(preset.displayName, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            Text(preset.roleTitle, color = TextSecondary, fontSize = 10.sp)
                            Button(
                                onClick = { scope.launch { repository.updateSelectedCharacter(preset.id) } },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) Color.DarkGray else CyberOrange),
                                modifier = Modifier.fillMaxWidth().height(32.dp)
                            ) {
                                Text(
                                    text = if (isSelected) "DEPLOYED ✓" else "SELECT OPERATIVE",
                                    color = if (isSelected) Color.White else Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Grid of owned items
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
            items(filteredItems) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { repository.equipItem(item) }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                    border = if (item.isEquipped) androidx.compose.foundation.BorderStroke(2.dp, CyberOrange) else null
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                item.rarity,
                                color = when (item.rarity) {
                                    "MYTHIC" -> RarityMythic
                                    "LEGENDARY" -> RarityLegendary
                                    "EPIC" -> RarityEpic
                                    else -> ElectricBlueSafe
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                            if (item.isEquipped) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = "Equipped", tint = HealthGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("EQUIPPED", color = HealthGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(item.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch { repository.equipItem(item) }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (item.isEquipped) Color.DarkGray else CyberOrange
                                ),
                                modifier = Modifier.weight(1f).height(32.dp)
                            ) {
                                Text(
                                    text = if (item.isEquipped) "IN USE" else "EQUIP",
                                    color = if (item.isEquipped) Color.White else Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            if (item.type == "WEAPON_SKIN") {
                                Button(
                                    onClick = { inspectingItem = item },
                                    colors = ButtonDefaults.buttonColors(containerColor = TacticalSurface),
                                    modifier = Modifier.height(32.dp).border(1.dp, ElectricBlueSafe, RoundedCornerShape(8.dp))
                                ) {
                                    Text("INSPECT", color = ElectricBlueSafe, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    // 3D WEAPON INSPECTION MODAL
    if (inspectingItem != null) {
        WeaponInspectionDialog(
            item = inspectingItem!!,
            onDismiss = { inspectingItem = null }
        )
    }
}

@Composable
fun WeaponInspectionDialog(
    item: com.example.data.InventoryItemEntity,
    onDismiss: () -> Unit
) {
    var rotationAngleX by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var rotationAngleY by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var isAutoInspecting by remember { androidx.compose.runtime.mutableStateOf(false) }

    var selectedMuzzle by remember { androidx.compose.runtime.mutableStateOf(com.example.game.model.MuzzleType.COMPENSATOR) }
    var selectedScope by remember { androidx.compose.runtime.mutableStateOf(com.example.game.model.ScopeType.SCOPE_4X) }
    var selectedMag by remember { androidx.compose.runtime.mutableStateOf(com.example.game.model.MagType.EXTENDED_QUICKDRAW) }

    // Auto Inspection Animation
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "inspect_turntable")
    val animatedTurntable by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(6000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "spin"
    )

    val currentYaw = if (isAutoInspecting) animatedTurntable else rotationAngleY

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxSize(0.92f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
            border = androidx.compose.foundation.BorderStroke(2.dp, CyberOrange)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("3D WEAPON INSPECTION & ARMORY", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text("${item.name.uppercase()} • DRAG TO ROTATE 360°", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(androidx.compose.material.icons.Icons.Default.Check, contentDescription = "Done", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxSize()) {
                    // Left: Interactive 3D Canvas
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(TacticalDarkBg)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    isAutoInspecting = false
                                    rotationAngleY = (rotationAngleY + dragAmount.x * 0.5f) % 360f
                                    rotationAngleX = (rotationAngleX - dragAmount.y * 0.3f).coerceIn(-45f, 45f)
                                }
                            }
                    ) {
                        // 3D Wireframe / Shaded Weapon Render
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val yawRad = Math.toRadians(currentYaw.toDouble())
                            val pitchRad = Math.toRadians(rotationAngleX.toDouble())
                            val cosY = kotlin.math.cos(yawRad).toFloat()
                            val sinY = kotlin.math.sin(yawRad).toFloat()

                            // Base weapon silhouette length
                            val gunLength = 260f
                            val p1x = cx - (gunLength * 0.5f) * cosY
                            val p1y = cy + (gunLength * 0.5f) * sinY * 0.35f
                            val p2x = cx + (gunLength * 0.5f) * cosY
                            val p2y = cy - (gunLength * 0.5f) * sinY * 0.35f

                            // Barrel & Receiver
                            drawLine(
                                color = when (item.rarity) {
                                    "MYTHIC" -> RarityMythic
                                    "LEGENDARY" -> RarityLegendary
                                    "EPIC" -> RarityEpic
                                    else -> CyberOrange
                                },
                                start = androidx.compose.ui.geometry.Offset(p1x, p1y),
                                end = androidx.compose.ui.geometry.Offset(p2x, p2y),
                                strokeWidth = 14f * (0.8f + cosY * 0.2f)
                            )

                            // Stock
                            drawLine(
                                color = Color.Gray,
                                start = androidx.compose.ui.geometry.Offset(p1x, p1y),
                                end = androidx.compose.ui.geometry.Offset(p1x - 30f * cosY, p1y + 25f),
                                strokeWidth = 18f
                            )

                            // Magazine
                            drawLine(
                                color = HazardYellow,
                                start = androidx.compose.ui.geometry.Offset(cx, cy),
                                end = androidx.compose.ui.geometry.Offset(cx - 15f * cosY, cy + 55f),
                                strokeWidth = 12f
                            )

                            // Scope Mounted
                            if (selectedScope != com.example.game.model.ScopeType.IRON_SIGHT) {
                                drawCircle(
                                    color = ElectricBlueSafe,
                                    center = androidx.compose.ui.geometry.Offset(cx + 20f * cosY, cy - 25f),
                                    radius = 16f
                                )
                            }

                            // Muzzle Mounted
                            if (selectedMuzzle != com.example.game.model.MuzzleType.NONE) {
                                drawCircle(
                                    color = if (selectedMuzzle.isSilenced) Color.Black else HazardYellow,
                                    center = androidx.compose.ui.geometry.Offset(p2x + 10f * cosY, p2y),
                                    radius = 10f
                                )
                            }
                        }

                        // Inspect Animation Toggle Pill
                        Button(
                            onClick = { isAutoInspecting = !isAutoInspecting },
                            colors = ButtonDefaults.buttonColors(containerColor = TacticalSurface.copy(alpha = 0.85f)),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .border(1.dp, CyberOrange, RoundedCornerShape(12.dp))
                        ) {
                            Text(if (isAutoInspecting) "PAUSE INSPECT" else "AUTO INSPECT", color = CyberOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Right: Weapon Stats & Attachment Slot Selection
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ATTRIBUTES & PERFORMANCE", color = HazardYellow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            val stabilityBonus = selectedMuzzle.recoilReduction
                            val capacityBonus = selectedMag.extraCapacity

                            StatProgressBar("DAMAGE", 0.72f, CyberOrange)
                            StatProgressBar("FIRE RATE", 0.85f, ElectricBlueSafe)
                            StatProgressBar("STABILITY", (0.60f + stabilityBonus).coerceAtMost(1f), HealthGreen)
                            StatProgressBar("CAPACITY", ((30f + capacityBonus) / 50f).coerceAtMost(1f), HazardYellow)
                        }

                        Column {
                            Text("ATTACHMENT SLOTS", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(6.dp))

                            // Scope slot pill
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TacticalSurface)
                                    .clickable {
                                        val nextScope = when (selectedScope) {
                                            com.example.game.model.ScopeType.IRON_SIGHT -> com.example.game.model.ScopeType.RED_DOT
                                            com.example.game.model.ScopeType.RED_DOT -> com.example.game.model.ScopeType.SCOPE_4X
                                            com.example.game.model.ScopeType.SCOPE_4X -> com.example.game.model.ScopeType.SCOPE_8X
                                            else -> com.example.game.model.ScopeType.IRON_SIGHT
                                        }
                                        selectedScope = nextScope
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("OPTICS", color = TextSecondary, fontSize = 10.sp)
                                Text(selectedScope.displayName, color = ElectricBlueSafe, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Muzzle slot pill
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TacticalSurface)
                                    .clickable {
                                        val nextMuzzle = when (selectedMuzzle) {
                                            com.example.game.model.MuzzleType.NONE -> com.example.game.model.MuzzleType.COMPENSATOR
                                            com.example.game.model.MuzzleType.COMPENSATOR -> com.example.game.model.MuzzleType.SUPPRESSOR
                                            else -> com.example.game.model.MuzzleType.NONE
                                        }
                                        selectedMuzzle = nextMuzzle
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("MUZZLE", color = TextSecondary, fontSize = 10.sp)
                                Text(selectedMuzzle.displayName, color = HazardYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Mag slot pill
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TacticalSurface)
                                    .clickable {
                                        val nextMag = when (selectedMag) {
                                            com.example.game.model.MagType.STANDARD -> com.example.game.model.MagType.EXTENDED
                                            com.example.game.model.MagType.EXTENDED -> com.example.game.model.MagType.EXTENDED_QUICKDRAW
                                            else -> com.example.game.model.MagType.STANDARD
                                        }
                                        selectedMag = nextMag
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("MAGAZINE", color = TextSecondary, fontSize = 10.sp)
                                Text(selectedMag.displayName, color = HealthGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberOrange),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Text("SAVE CONFIGURATION", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatProgressBar(label: String, progress: Float, color: Color) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text("${(progress * 100).toInt()}%", color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        androidx.compose.material3.LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color.DarkGray
        )
    }
}
