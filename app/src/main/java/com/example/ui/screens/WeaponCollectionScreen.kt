package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.InventoryItemEntity
import com.example.data.StoreCatalog
import com.example.data.cloud.CloudGameRepository
import com.example.data.cloud.CloudUserProfile
import com.example.game.model.MagType
import com.example.game.model.MuzzleType
import com.example.game.model.ScopeType
import com.example.game.model.WeaponCatalog
import com.example.game.model.WeaponCategory
import com.example.ui.components.Weapon3DShowcaseView
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.ElectricBlueSafe
import com.example.ui.theme.HazardYellow
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.RarityEpic
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
fun WeaponCollectionScreen(
    repository: GameRepository,
    cloudRepository: CloudGameRepository? = null,
    cloudProfile: CloudUserProfile? = null,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val profile by repository.userProfile.collectAsState(initial = null)
    val allInventory by repository.allInventory.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val baseWeapons = remember { WeaponCatalog.primaryFirearms() }
    val meleeWeapons = remember { WeaponCatalog.meleeWeapons() }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    val equippedPrimaryId = profile?.equippedWeaponId ?: "m416"
    val equippedMeleeId = profile?.equippedMeleeId ?: "katana"
    var selectedWeaponId by remember(equippedPrimaryId) { mutableStateOf(equippedPrimaryId) }

    var selectedScope by remember { mutableStateOf(ScopeType.SCOPE_4X) }
    var selectedMuzzle by remember { mutableStateOf(MuzzleType.COMPENSATOR) }
    var selectedMag by remember { mutableStateOf(MagType.EXTENDED_QUICKDRAW) }
    var autoInspect by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val displayBp = cloudProfile?.bpCoins ?: profile?.bpCoins?.toLong() ?: 0L
    val displayVc = cloudProfile?.vortexCredits ?: profile?.vortexCredits?.toLong() ?: 0L

    val filteredWeapons = remember(selectedCategoryFilter) {
        when (selectedCategoryFilter) {
            "AR" -> baseWeapons.filter { it.category == WeaponCategory.ASSAULT_RIFLE }
            "SMG" -> baseWeapons.filter { it.category == WeaponCategory.SMG }
            "SHOTGUN" -> baseWeapons.filter { it.category == WeaponCategory.SHOTGUN }
            "SNIPER" -> baseWeapons.filter { it.category == WeaponCategory.SNIPER_RIFLE }
            "PISTOL" -> baseWeapons.filter { it.category == WeaponCategory.PISTOL }
            "MELEE" -> meleeWeapons
            else -> baseWeapons + meleeWeapons
        }
    }

    val isSelectedMelee = remember(selectedWeaponId) {
        meleeWeapons.any { it.id.equals(selectedWeaponId, ignoreCase = true) }
    }
    val activeGunItemId = if (isSelectedMelee) {
        "melee_${selectedWeaponId.lowercase()}"
    } else {
        "gun_${selectedWeaponId.lowercase()}"
    }
    val gunEntity = allInventory.firstOrNull { it.id == activeGunItemId }
        ?: StoreCatalog.findSpec(activeGunItemId)?.toEntity()
    val gunSpec = StoreCatalog.findSpec(activeGunItemId)
    val isGunOwned = gunEntity?.isOwned ?: (selectedWeaponId == "m416" || isSelectedMelee)
    val isGunPrimary = if (isSelectedMelee) {
        equippedMeleeId.equals(selectedWeaponId, ignoreCase = true)
    } else {
        equippedPrimaryId.equals(selectedWeaponId, ignoreCase = true)
    }
    val currentLevel = (gunEntity?.upgradeLevel ?: 1).coerceIn(1, 5)

    val skinsForSelected = remember(selectedWeaponId, allInventory) {
        val specs = StoreCatalog.skinsForWeapon(selectedWeaponId)
        specs.map { spec ->
            allInventory.firstOrNull { it.id == spec.id } ?: spec.toEntity()
        }
    }
    val equippedSkinEntity = skinsForSelected.firstOrNull { it.isOwned && it.isEquipped }
    val activeSkinName = equippedSkinEntity?.name?.substringAfter(" - ")?.trim() ?: "Standard Military"

    val displayWeapon = remember(selectedWeaponId, activeSkinName, currentLevel, selectedScope, selectedMuzzle, selectedMag) {
        WeaponCatalog.createById(selectedWeaponId, activeSkinName, currentLevel).apply {
            this.equippedScope = selectedScope
            this.equippedMuzzle = selectedMuzzle
            this.equippedMag = selectedMag
        }
    }

    fun unlockWeaponOrSkin(entity: InventoryItemEntity) {
        if (isProcessing) return
        val useCredits = entity.priceCredits > 0
        scope.launch {
            isProcessing = true
            val cloudAttempt = if (cloudRepository != null && cloudProfile != null) {
                cloudRepository.purchaseDirectBundle(
                    itemId = entity.id,
                    itemName = entity.name,
                    type = entity.type,
                    targetItemId = entity.targetItemId,
                    rarity = entity.rarity,
                    priceBp = entity.priceBp.toLong(),
                    priceCredits = entity.priceCredits.toLong()
                )
            } else null

            if (cloudAttempt != null) {
                cloudAttempt.onSuccess { msg ->
                    val unlocked = entity.copy(isOwned = true)
                    repository.syncCloudUnlockedItem(unlocked)
                    repository.equipItem(unlocked)
                    cloudRepository?.setInventoryItemEquipped(unlocked.id, true)
                    statusMessage = msg
                }.onFailure { err ->
                    statusMessage = err.message ?: "Purchase failed."
                }
            } else {
                val ok = repository.purchaseShopItem(entity, useCredits = useCredits)
                if (ok) {
                    repository.equipItem(entity.copy(isOwned = true))
                    statusMessage = "Unlocked & equipped ${entity.name}!"
                } else {
                    statusMessage = "Insufficient currency to unlock ${entity.name}."
                }
            }
            isProcessing = false
        }
    }

    fun upgradeCurrentWeapon() {
        if (isProcessing || !isGunOwned || currentLevel >= 5) return
        val item = gunEntity ?: return
        val costBp = StoreCatalog.upgradeCostBp(currentLevel)

        scope.launch {
            isProcessing = true
            val cloudAttempt = if (cloudRepository != null && cloudProfile != null) {
                cloudRepository.upgradeInventoryItem(
                    itemId = item.id,
                    itemName = item.name,
                    type = item.type,
                    targetItemId = item.targetItemId,
                    rarity = item.rarity,
                    costBp = costBp.toLong()
                )
            } else null

            if (cloudAttempt != null) {
                cloudAttempt.onSuccess { nextLvl ->
                    repository.upgradeInventoryItemLocal(item.id)
                    statusMessage = "${displayWeapon.name} upgraded to Lv.$nextLvl!"
                }.onFailure { err ->
                    statusMessage = err.message ?: "Upgrade failed."
                }
            } else {
                val ok = repository.upgradeInventoryItemLocal(item.id)
                statusMessage = if (ok) {
                    "${displayWeapon.name} upgraded to Lv.${currentLevel + 1}!"
                } else {
                    "Need $costBp BP Coins to upgrade ${displayWeapon.name}."
                }
            }
            isProcessing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalDarkBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("weapon_collection_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("weapon_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text("3D WEAPON COLLECTION & ARMORY", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text("LIVE BALLISTIC STATS • SKINS • UPGRADE LEVELS • MATCH SYNC", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                statusMessage?.let { msg ->
                    Text(
                        msg,
                        color = if (msg.contains("Need") || msg.contains("Insufficient") || msg.contains("failed", true)) RedZoneBombardment else HealthGreen,
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

        Spacer(modifier = Modifier.height(6.dp))

        // Category Filter Row + Weapon Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val cats = listOf("ALL", "AR", "SMG", "SHOTGUN", "SNIPER", "PISTOL", "MELEE")
                items(cats) { cat ->
                    val selected = selectedCategoryFilter == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) CyberOrange else TacticalSurface)
                            .clickable {
                                selectedCategoryFilter = cat
                                val nextList = when (cat) {
                                    "AR" -> baseWeapons.filter { it.category == WeaponCategory.ASSAULT_RIFLE }
                                    "SMG" -> baseWeapons.filter { it.category == WeaponCategory.SMG }
                                    "SHOTGUN" -> baseWeapons.filter { it.category == WeaponCategory.SHOTGUN }
                                    "SNIPER" -> baseWeapons.filter { it.category == WeaponCategory.SNIPER_RIFLE }
                                    "PISTOL" -> baseWeapons.filter { it.category == WeaponCategory.PISTOL }
                                    "MELEE" -> meleeWeapons
                                    else -> baseWeapons + meleeWeapons
                                }
                                if (nextList.none { it.id == selectedWeaponId } && nextList.isNotEmpty()) {
                                    selectedWeaponId = nextList.first().id
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("weapon_cat_${cat.lowercase()}")
                    ) {
                        Text(cat, color = if (selected) Color.Black else TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filteredWeapons, key = { it.id }) { weapon ->
                    val selected = weapon.id == selectedWeaponId
                    val isEquippedSlot = if (weapon.category == WeaponCategory.MELEE) {
                        equippedMeleeId.equals(weapon.id, ignoreCase = true)
                    } else {
                        equippedPrimaryId.equals(weapon.id, ignoreCase = true)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) ElectricBlueSafe else TacticalSurface)
                            .border(
                                1.dp,
                                if (isEquippedSlot) HealthGreen else Color.White.copy(alpha = 0.14f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedWeaponId = weapon.id }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("select_weapon_${weapon.id}")
                    ) {
                        Text(
                            text = if (isEquippedSlot) "★ ${weapon.name}" else weapon.name,
                            color = if (selected) Color.Black else TextPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Split Panel
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // LEFT PANE: 3D Weapon Viewport + Attachment Cyclers + Skins Strip
            Column(
                modifier = Modifier
                    .weight(0.49f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TacticalSurface)
                    .border(1.dp, CyberOrange.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                ) {
                    Weapon3DShowcaseView(
                        weapon = displayWeapon,
                        skinName = activeSkinName,
                        scopeType = selectedScope,
                        muzzleType = selectedMuzzle,
                        magType = selectedMag,
                        autoInspect = autoInspect,
                        upgradeLevel = currentLevel,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xB8080D16))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${displayWeapon.name.uppercase()} • $activeSkinName • DRAG 360°",
                            color = HazardYellow,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (autoInspect) CyberOrange else Color(0xC80C1220))
                            .border(1.dp, CyberOrange, RoundedCornerShape(8.dp))
                            .clickable { autoInspect = !autoInspect }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                            .testTag("weapon_auto_inspect_toggle")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Autorenew, contentDescription = "Auto Inspect", tint = if (autoInspect) Color.Black else CyberOrange, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (autoInspect) "AUTO INSPECT ON" else "AUTO INSPECT", color = if (autoInspect) Color.Black else CyberOrange, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                // Attachment Quick Preview Cyclers (or Melee Combat Role Info)
                if (!isSelectedMelee) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AttachmentMiniPill("OPTIC", selectedScope.displayName, ElectricBlueSafe, Modifier.weight(1f)) {
                            val entries = ScopeType.entries
                            selectedScope = entries[(selectedScope.ordinal + 1) % entries.size]
                        }
                        AttachmentMiniPill("MUZZLE", selectedMuzzle.displayName, HazardYellow, Modifier.weight(1f)) {
                            val entries = MuzzleType.entries
                            selectedMuzzle = entries[(selectedMuzzle.ordinal + 1) % entries.size]
                        }
                        AttachmentMiniPill("MAG", selectedMag.displayName, HealthGreen, Modifier.weight(1f)) {
                            val entries = MagType.entries
                            selectedMag = entries[(selectedMag.ordinal + 1) % entries.size]
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(TacticalSurfaceVariant)
                            .border(1.dp, HazardYellow.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SLOT 4 • MELEE STRIKER", color = HazardYellow, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        Text("CLEAVE RANGE: 1.8m • ∞ DURABILITY", color = ElectricBlueSafe, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Weapon Skins Strip
                Text("AVAILABLE SKINS FOR ${displayWeapon.name.uppercase()}", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                if (skinsForSelected.isEmpty()) {
                    Text("Standard tactical finish active.", color = TextSecondary, fontSize = 9.sp)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(skinsForSelected, key = { it.id }) { skin ->
                            val rCol = when (skin.rarity) {
                                "MYTHIC" -> RarityMythic
                                "LEGENDARY" -> RarityLegendary
                                "EPIC" -> RarityEpic
                                else -> ElectricBlueSafe
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (skin.isEquipped) rCol.copy(alpha = 0.2f) else TacticalSurfaceVariant)
                                    .border(1.dp, if (skin.isEquipped) rCol else Color.White.copy(alpha = 0.14f), RoundedCornerShape(9.dp))
                                    .clickable {
                                        if (skin.isOwned) {
                                            scope.launch {
                                                repository.equipItem(skin)
                                                cloudRepository?.setInventoryItemEquipped(skin.id, true)
                                                statusMessage = "Equipped ${skin.name}!"
                                            }
                                        } else {
                                            unlockWeaponOrSkin(skin)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("weapon_skin_${skin.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (skin.isOwned) Icons.Default.CheckCircle else Icons.Default.Lock,
                                    contentDescription = if (skin.isOwned) "Owned" else "Locked",
                                    tint = if (skin.isEquipped) HealthGreen else if (skin.isOwned) rCol else HazardYellow,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Column {
                                    Text(skin.name.substringAfter(" - "), color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                    Text(
                                        when {
                                            skin.isEquipped -> "EQUIPPED"
                                            skin.isOwned -> "TAP TO EQUIP"
                                            skin.priceCredits > 0 -> "${skin.priceCredits} VC"
                                            else -> "${skin.priceBp} BP"
                                        },
                                        color = if (skin.isEquipped) HealthGreen else HazardYellow,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // RIGHT PANE: Ballistics + Upgrades + Primary Loadout Actions
            Column(
                modifier = Modifier
                    .weight(0.51f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${displayWeapon.name.uppercase()} (${displayWeapon.category.displayName.uppercase()})",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${displayWeapon.ammoType.displayName} • ${displayWeapon.supportedFireModes.joinToString("/") { it.name }}",
                                    color = ElectricBlueSafe,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberOrange.copy(alpha = 0.2f))
                                    .border(1.dp, CyberOrange, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text("LV.$currentLevel / 5", color = HazardYellow, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }

                        gunSpec?.let {
                            Text(it.description, color = TextSecondary, fontSize = 9.sp)
                        }

                        val damageNorm = (displayWeapon.effectiveDamage / 120f).coerceIn(0.15f, 1f)
                        val fireRateNorm = (1f - (displayWeapon.effectiveFireRateMs / 2000f)).coerceIn(0.15f, 0.98f)
                        val stabilityNorm = (1f - (displayWeapon.effectiveRecoil / 1.5f)).coerceIn(0.2f, 0.98f)
                        val capacityNorm = (displayWeapon.totalCapacity / 45f).coerceIn(0.15f, 1f)

                        StatProgressBar("DAMAGE (${displayWeapon.effectiveDamage.toInt()})", damageNorm, CyberOrange)
                        StatProgressBar("FIRE RATE (${displayWeapon.effectiveFireRateMs}ms)", fireRateNorm, ElectricBlueSafe)
                        StatProgressBar("STABILITY", stabilityNorm, HealthGreen)
                        StatProgressBar("CAPACITY (${displayWeapon.totalCapacity} RDS)", capacityNorm, HazardYellow)
                    }
                }

                // Upgrade Tier Box
                val nextUpgradeCost = StoreCatalog.upgradeCostBp(currentLevel)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                    border = BorderStroke(1.dp, HazardYellow.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("WEAPON MASTERY UPGRADE", color = HazardYellow, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            Text(
                                if (currentLevel >= 5) "Maximum Lv.5 Mastery Achieved (+24% DMG)"
                                else "Next Level Lv.${currentLevel + 1}: +6% Damage • +4% Stability • Faster Cycle",
                                color = TextSecondary,
                                fontSize = 9.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            enabled = isGunOwned && currentLevel < 5 && !isProcessing,
                            onClick = { upgradeCurrentWeapon() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HealthGreen,
                                disabledContainerColor = TacticalSurfaceVariant
                            ),
                            modifier = Modifier.height(38.dp).testTag("upgrade_weapon_button")
                        ) {
                            Icon(Icons.Default.Upgrade, contentDescription = null, tint = if (currentLevel >= 5) TextSecondary else Color.Black, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    !isGunOwned -> "LOCKED"
                                    currentLevel >= 5 -> "MAX LV.5"
                                    else -> "UPGRADE ($nextUpgradeCost BP)"
                                },
                                color = if (!isGunOwned || currentLevel >= 5) TextSecondary else Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Primary Loadout Action Button
                val gunTarget = gunEntity
                if (!isGunOwned && gunTarget != null) {
                    val costTxt = if (gunTarget.priceCredits > 0) "${gunTarget.priceCredits} VC" else "${gunTarget.priceBp} BP"
                    Button(
                        enabled = !isProcessing,
                        onClick = { unlockWeaponOrSkin(gunTarget) },
                        colors = ButtonDefaults.buttonColors(containerColor = HazardYellow),
                        modifier = Modifier.fillMaxWidth().height(42.dp).testTag("unlock_weapon_button")
                    ) {
                        Text("UNLOCK ${displayWeapon.name.uppercase()} FOR $costTxt", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                } else {
                    Button(
                        enabled = !isGunPrimary,
                        onClick = {
                            scope.launch {
                                if (isSelectedMelee) {
                                    repository.updateEquippedMelee(selectedWeaponId)
                                } else {
                                    repository.updateEquippedWeapon(selectedWeaponId)
                                }
                                gunTarget?.let { cloudRepository?.setInventoryItemEquipped(it.id, true) }
                                statusMessage = if (isSelectedMelee) {
                                    "${displayWeapon.name} equipped to Melee Slot 4!"
                                } else {
                                    "${displayWeapon.name} equipped for Lobby & Match Spawn!"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isGunPrimary) TacticalSurfaceVariant else CyberOrange,
                            disabledContainerColor = TacticalSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag(if (isSelectedMelee) "equip_melee_weapon_button" else "equip_primary_weapon_button")
                    ) {
                        Text(
                            text = when {
                                isGunPrimary && isSelectedMelee -> "EQUIPPED IN MELEE SLOT 4 ✓"
                                isGunPrimary -> "EQUIPPED AS PRIMARY LOADOUT ✓"
                                isSelectedMelee -> "EQUIP ${displayWeapon.name.uppercase()} TO SLOT 4"
                                else -> "EQUIP ${displayWeapon.name.uppercase()} TO LOBBY & MATCH"
                            },
                            color = if (isGunPrimary) HealthGreen else Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentMiniPill(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TacticalSurfaceVariant)
            .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 5.dp)
    ) {
        Text(label, color = TextSecondary, fontSize = 7.sp, fontWeight = FontWeight.Black)
        Text(value, color = tint, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
