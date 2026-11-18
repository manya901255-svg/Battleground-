package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameRepository
import com.example.data.StoreCatalog
import com.example.data.cloud.CloudGameRepository
import com.example.data.cloud.CloudUserProfile
import com.example.game.model.CharacterPreset
import com.example.ui.components.Character3DShowcaseView
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.ElectricBlueSafe
import com.example.ui.theme.HazardYellow
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.RarityLegendary
import com.example.ui.theme.RarityMythic
import com.example.ui.theme.RedZoneBombardment
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun CharacterSelectionScreen(
    repository: GameRepository,
    cloudRepository: CloudGameRepository? = null,
    cloudProfile: CloudUserProfile? = null,
    equippedWeaponId: String = "m416",
    equippedWeaponSkin: String = "Tactical Slate",
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val profile by repository.userProfile.collectAsState(initial = null)
    val allInventory by repository.allInventory.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val activeCharacterId = profile?.selectedCharacterId ?: "vortex_ghost"
    var focusedPreset by remember(activeCharacterId) {
        mutableStateOf(CharacterPreset.fromId(activeCharacterId))
    }
    var genderFilter by remember { mutableStateOf("ALL") }
    var animMode by remember { mutableStateOf("IDLE") }
    var autoRotate by remember { mutableStateOf(false) }
    var statusBanner by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val ownedIds = remember(allInventory) {
        allInventory.filter { it.isOwned }.map { it.id }.toSet()
    }
    val ownedOutfits = remember(allInventory) {
        allInventory.filter { it.type == "OUTFIT" && it.isOwned }
    }

    val displayBp = cloudProfile?.bpCoins ?: profile?.bpCoins?.toLong() ?: 0L
    val displayVc = cloudProfile?.vortexCredits ?: profile?.vortexCredits?.toLong() ?: 0L
    val voiceEnabled = profile?.voiceCalloutsEnabled ?: true

    val visiblePresets = remember(genderFilter) {
        CharacterPreset.entries.filter {
            genderFilter == "ALL" || it.gender.equals(genderFilter, ignoreCase = true)
        }
    }

    val focusedSpec = remember(focusedPreset) { StoreCatalog.findSpec("char_${focusedPreset.id}") }
    val focusedItemEntity = remember(focusedPreset, allInventory) {
        allInventory.firstOrNull { it.id == "char_${focusedPreset.id}" } ?: focusedSpec?.toEntity()
    }
    val isFocusedUnlocked = focusedPreset.starterUnlocked || ownedIds.contains("char_${focusedPreset.id}")
    val isFocusedEquipped = activeCharacterId == focusedPreset.id
    val unlockedCount = CharacterPreset.entries.count { it.starterUnlocked || ownedIds.contains("char_${it.id}") }

    fun unlockCharacter(preset: CharacterPreset) {
        if (isProcessing) return
        val spec = StoreCatalog.findSpec("char_${preset.id}") ?: return
        val entity = spec.toEntity()
        val useCredits = spec.priceCredits > 0

        scope.launch {
            isProcessing = true
            val cloudAttempt = if (cloudRepository != null && cloudProfile != null) {
                cloudRepository.purchaseDirectBundle(
                    itemId = entity.id,
                    itemName = entity.name,
                    type = entity.type,
                    targetItemId = entity.targetItemId,
                    rarity = entity.rarity,
                    priceBp = spec.priceBp.toLong(),
                    priceCredits = spec.priceCredits.toLong()
                )
            } else null

            if (cloudAttempt != null) {
                cloudAttempt.onSuccess {
                    repository.syncCloudUnlockedItem(entity)
                    repository.equipItem(entity.copy(isOwned = true))
                    repository.updateSelectedCharacter(preset.id)
                    cloudRepository?.updateLoadoutSelection(
                        characterId = preset.id,
                        petId = profile?.selectedPetId ?: "draco_flame",
                        petVisible = profile?.petVisible ?: true
                    )
                    statusBanner = "Unlocked & deployed ${preset.displayName}!"
                }.onFailure { err ->
                    statusBanner = err.message ?: "Unlock failed. Currency untouched."
                }
            } else {
                val purchased = repository.purchaseShopItem(entity, useCredits = useCredits)
                if (purchased) {
                    repository.equipItem(entity.copy(isOwned = true))
                    repository.updateSelectedCharacter(preset.id)
                    statusBanner = "Unlocked & deployed ${preset.displayName}!"
                } else {
                    val needed = if (useCredits) "${spec.priceCredits} VC" else "${spec.priceBp} BP"
                    statusBanner = "Insufficient balance — requires $needed."
                }
            }
            isProcessing = false
        }
    }

    fun equipCharacter(preset: CharacterPreset) {
        scope.launch {
            focusedItemEntity?.takeIf { isFocusedUnlocked }?.let {
                repository.equipItem(it.copy(isOwned = true))
            }
            repository.updateSelectedCharacter(preset.id)
            cloudRepository?.updateLoadoutSelection(
                characterId = preset.id,
                petId = profile?.selectedPetId ?: "draco_flame",
                petVisible = profile?.petVisible ?: true
            )
            statusBanner = "${preset.displayName} deployed to Lobby & Battleground!"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalDarkBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("character_selection_screen")
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("char_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Lobby", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text("OPERATIVE ROSTER & 3D LOADOUT", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text("360° LIVE INSPECTION • ABILITIES • OUTFIT SYNC", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                statusBanner?.let { msg ->
                    Text(
                        text = msg,
                        color = if (msg.contains("Insufficient") || msg.contains("failed", true)) RedZoneBombardment else HealthGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TacticalSurface)
                        .border(1.dp, HazardYellow.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text("BP: $displayBp", color = HazardYellow, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TacticalSurface)
                        .border(1.dp, ElectricBlueSafe.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text("VC: $displayVc", color = ElectricBlueSafe, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Split 16:9 Workspace
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // LEFT PANE: Interactive 3D Character Viewport + Controls + Attributes
            val accentCol = Color(
                focusedPreset.accentRgb.first,
                focusedPreset.accentRgb.second,
                focusedPreset.accentRgb.third
            )
            Column(
                modifier = Modifier
                    .weight(0.46f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TacticalSurface)
                    .border(1.2.dp, accentCol.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                ) {
                    Character3DShowcaseView(
                        characterPresetId = focusedPreset.id,
                        weaponId = equippedWeaponId,
                        weaponSkin = equippedWeaponSkin,
                        animMode = animMode,
                        autoRotate = autoRotate,
                        modifier = Modifier.fillMaxSize()
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xB8080D16))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${focusedPreset.displayName.uppercase()} • DRAG 360°",
                            color = accentCol,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (autoRotate) CyberOrange else Color(0xB8080D16))
                            .clickable { autoRotate = !autoRotate }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("char_auto_rotate_toggle")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Autorenew,
                                contentDescription = "Toggle 360 Auto Rotate",
                                tint = if (autoRotate) Color.Black else Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                if (autoRotate) "360° ON" else "ROTATE",
                                color = if (autoRotate) Color.Black else Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Pose & Voice Callout Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("IDLE", "COMBAT", "VICTORY").forEach { mode ->
                            val selected = animMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (selected) CyberOrange else TacticalSurfaceVariant)
                                    .clickable { animMode = mode }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                    .testTag("char_anim_${mode.lowercase()}")
                            ) {
                                Text(mode, color = if (selected) Color.Black else TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Voice", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = voiceEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch { repository.updateVoiceCallouts(enabled) }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberOrange)
                        )
                    }
                }

                // Attributes Bars
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    StatProgressBar("MOBILITY", focusedPreset.mobilityStat / 100f, ElectricBlueSafe)
                    StatProgressBar("COMBAT", focusedPreset.combatStat / 100f, CyberOrange)
                    StatProgressBar("SURVIVAL", focusedPreset.survivalStat / 100f, HealthGreen)
                }

                // Quick Outfit Strip
                if (ownedOutfits.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ownedOutfits, key = { it.id }) { outfit ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (outfit.isEquipped) accentCol.copy(alpha = 0.25f) else TacticalSurfaceVariant)
                                    .border(
                                        1.dp,
                                        if (outfit.isEquipped) accentCol else Color.White.copy(alpha = 0.14f),
                                        RoundedCornerShape(7.dp)
                                    )
                                    .clickable {
                                        scope.launch {
                                            repository.equipItem(outfit)
                                            cloudRepository?.setInventoryItemEquipped(outfit.id, true)
                                            statusBanner = "Equipped outfit: ${outfit.name}"
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (outfit.isEquipped) "✓ ${outfit.name}" else outfit.name,
                                    color = if (outfit.isEquipped) accentCol else TextSecondary,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // RIGHT PANE: Filter + Character Cards + Action Footer
            Column(
                modifier = Modifier
                    .weight(0.54f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ALL", "MALE", "FEMALE").forEach { filter ->
                            val active = genderFilter == filter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) CyberOrange else TacticalSurface)
                                    .clickable { genderFilter = filter }
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                                    .testTag("char_filter_${filter.lowercase()}")
                            ) {
                                Text(filter, color = if (active) Color.Black else TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Text(
                        text = "$unlockedCount / ${CharacterPreset.entries.size} UNLOCKED",
                        color = HazardYellow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(visiblePresets, key = { it.id }) { preset ->
                        val isFocused = focusedPreset.id == preset.id
                        val isActive = activeCharacterId == preset.id
                        val isUnlocked = preset.starterUnlocked || ownedIds.contains("char_${preset.id}")
                        val pAccent = Color(preset.accentRgb.first, preset.accentRgb.second, preset.accentRgb.third)
                        val pArmor = Color(preset.armorRgb.first, preset.armorRgb.second, preset.armorRgb.third)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { focusedPreset = preset }
                                .testTag("character_card_${preset.id}"),
                            shape = RoundedCornerShape(11.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isFocused) Color(0xFF1A2336) else TacticalSurface
                            ),
                            border = BorderStroke(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = when {
                                    isActive -> HealthGreen
                                    isFocused -> CyberOrange
                                    else -> pAccent.copy(alpha = 0.25f)
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(pArmor).border(1.dp, Color.White, CircleShape))
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(pAccent).border(1.dp, Color.White, CircleShape))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(preset.gender.uppercase(), color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                    when {
                                        isActive -> Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = HealthGreen, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("DEPLOYED", color = HealthGreen, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                        }
                                        !isUnlocked -> Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = HazardYellow, modifier = Modifier.size(11.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                if (preset.unlockCredits > 0) "${preset.unlockCredits} VC" else "${preset.unlockBp} BP",
                                                color = HazardYellow,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                        else -> Text("OWNED", color = ElectricBlueSafe, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(preset.displayName, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(pAccent.copy(alpha = 0.16f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = preset.styleTag.replace("_", " "),
                                            color = pAccent,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Text(preset.roleTitle, color = pAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("ACTIVE: ${preset.activeSkillName} — ${preset.activeSkillDesc}", color = ElectricBlueSafe, fontSize = 8.sp, maxLines = 2, lineHeight = 10.sp)
                                Text("PASSIVE: ${preset.passiveSkillName} — ${preset.passiveSkillDesc}", color = HazardYellow, fontSize = 8.sp, maxLines = 2, lineHeight = 10.sp)
                            }
                        }
                    }
                }

                // Action Bar for Focused Operative
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                    border = BorderStroke(1.dp, accentCol.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = accentCol, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "${focusedPreset.displayName} • ${focusedPreset.activeSkillName}",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "PASSIVE: ${focusedPreset.passiveSkillName} (${focusedPreset.passiveSkillDesc})",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        if (!isFocusedUnlocked) {
                            val costLabel = if ((focusedSpec?.priceCredits ?: focusedPreset.unlockCredits) > 0) {
                                "UNLOCK (${focusedSpec?.priceCredits ?: focusedPreset.unlockCredits} VC)"
                            } else {
                                "UNLOCK (${focusedSpec?.priceBp ?: focusedPreset.unlockBp} BP)"
                            }
                            Button(
                                enabled = !isProcessing,
                                onClick = { unlockCharacter(focusedPreset) },
                                colors = ButtonDefaults.buttonColors(containerColor = HazardYellow),
                                modifier = Modifier.height(40.dp).testTag("unlock_character_button")
                            ) {
                                Text(costLabel, color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                enabled = !isFocusedEquipped,
                                onClick = { equipCharacter(focusedPreset) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFocusedEquipped) TacticalSurfaceVariant else CyberOrange,
                                    disabledContainerColor = TacticalSurfaceVariant
                                ),
                                modifier = Modifier.height(40.dp).testTag("equip_character_button")
                            ) {
                                Text(
                                    text = if (isFocusedEquipped) "DEPLOYED IN LOBBY ✓" else "DEPLOY OPERATIVE",
                                    color = if (isFocusedEquipped) HealthGreen else Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
