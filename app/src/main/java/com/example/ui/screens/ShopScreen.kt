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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import com.example.data.CatalogItemSpec
import com.example.data.GameRepository
import com.example.data.StoreCatalog
import com.example.data.StoreCategory
import com.example.data.cloud.CloudGameRepository
import com.example.data.cloud.CloudTransaction
import com.example.data.cloud.CloudUserProfile
import com.example.game.model.MagType
import com.example.game.model.MuzzleType
import com.example.game.model.ScopeType
import com.example.game.model.WeaponCatalog
import com.example.ui.components.Character3DShowcaseView
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
fun ShopScreen(
    repository: GameRepository,
    cloudRepository: CloudGameRepository? = null,
    cloudTransactions: List<CloudTransaction> = emptyList(),
    cloudProfile: CloudUserProfile? = null,
    initialTab: Int = 0,
    initialCategory: StoreCategory? = null,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val profile by repository.userProfile.collectAsState(initial = null)
    val ownedInventory by repository.inventory.collectAsState(initial = emptyList())
    val ownedIds = remember(ownedInventory) { ownedInventory.map { it.id }.toSet() }
    val scope = rememberCoroutineScope()

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    var selectedCategory by remember(initialCategory) {
        mutableStateOf(initialCategory ?: StoreCategory.CHARACTER_STORE)
    }
    var previewSpec by remember { mutableStateOf<CatalogItemSpec?>(null) }
    var shopBannerMsg by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val displayBp = cloudProfile?.bpCoins ?: profile?.bpCoins?.toLong() ?: 0L
    val displayVc = cloudProfile?.vortexCredits ?: profile?.vortexCredits?.toLong() ?: 0L

    val categoryItems = remember(selectedCategory) {
        StoreCatalog.byCategory(selectedCategory)
    }

    fun executeDirectPurchase(spec: CatalogItemSpec) {
        if (isProcessing) return
        if (ownedIds.contains(spec.id)) {
            shopBannerMsg = "${spec.name} is already owned."
            return
        }
        val item = spec.toEntity()
        val useCredits = spec.priceCredits > 0

        scope.launch {
            isProcessing = true
            val cloudResult = if (cloudRepository != null && cloudProfile != null) {
                cloudRepository.purchaseDirectBundle(
                    itemId = item.id,
                    itemName = item.name,
                    type = item.type,
                    targetItemId = item.targetItemId,
                    rarity = item.rarity,
                    priceBp = item.priceBp.toLong(),
                    priceCredits = item.priceCredits.toLong()
                )
            } else null

            if (cloudResult != null) {
                cloudResult.onSuccess { msg ->
                    repository.syncCloudUnlockedItem(item)
                    shopBannerMsg = msg
                    previewSpec = null
                }.onFailure { err ->
                    shopBannerMsg = err.message ?: "Purchase validation failed."
                }
            } else {
                val ok = repository.purchaseShopItem(item, useCredits = useCredits)
                shopBannerMsg = if (ok) {
                    previewSpec = null
                    "Purchased ${item.name}!"
                } else {
                    "Insufficient balance for ${item.name}."
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
            .testTag("shop_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("shop_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text("WARX ADVANCED ARMORY STORE", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text("11 CATEGORIES • BACKEND-VALIDATED DIRECT UNLOCKS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                shopBannerMsg?.let { msg ->
                    Text(
                        text = msg,
                        color = if (msg.contains("Insufficient") || msg.contains("failed", true)) RedZoneBombardment else HealthGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
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

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = TacticalSurface,
            contentColor = CyberOrange
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.testTag("shop_tab_bundles"),
                text = { Text("STORE CATALOG (11 CATEGORIES)", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.testTag("shop_tab_history"),
                text = { Text("TRANSACTION HISTORY (${cloudTransactions.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedTab == 0) {
            // 11 Store Categories Strip
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(StoreCategory.entries, key = { it.name }) { cat ->
                    val isSelected = selectedCategory == cat
                    val accent = if (isSelected) Color.Black else TextPrimary
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CyberOrange else TacticalSurface)
                            .border(
                                1.dp,
                                if (isSelected) HazardYellow else Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("store_cat_${cat.name.lowercase()}")
                    ) {
                        Text(cat.title.uppercase(), color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(categoryItems, key = { it.id }) { spec ->
                    val isOwned = ownedIds.contains(spec.id) || spec.starterOwned
                    val rarityColor = when (spec.rarity) {
                        "MYTHIC" -> RarityMythic
                        "LEGENDARY" -> RarityLegendary
                        "EPIC" -> RarityEpic
                        "RARE" -> ElectricBlueSafe
                        else -> TextSecondary
                    }
                    val accentColor = Color(spec.accentHex)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shop_item_${spec.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                        border = BorderStroke(1.2.dp, rarityColor.copy(alpha = 0.48f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(rarityColor.copy(alpha = 0.16f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(spec.rarity, color = rarityColor, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                }
                                if (isOwned) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Owned", tint = HealthGreen, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("OWNED", color = HealthGreen, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }

                            Text(spec.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = spec.description,
                                color = TextSecondary,
                                fontSize = 9.sp,
                                maxLines = 2,
                                lineHeight = 11.sp,
                                overflow = TextOverflow.Ellipsis
                            )

                            val cost = when {
                                spec.priceCredits > 0 -> "${spec.priceCredits} VC"
                                spec.priceBp > 0 -> "${spec.priceBp} BP"
                                else -> "STARTER"
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                                OutlinedButton(
                                    onClick = { previewSpec = spec },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp).testTag("inspect_${spec.id}")
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = "Inspect", tint = accentColor, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("3D", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                                Button(
                                    enabled = !isOwned && !isProcessing,
                                    onClick = { executeDirectPurchase(spec) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (spec.priceCredits > 0) ElectricBlueSafe else CyberOrange,
                                        disabledContainerColor = TacticalSurfaceVariant
                                    ),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f).height(32.dp).testTag("buy_${spec.id}")
                                ) {
                                    Text(
                                        text = if (isOwned) "OWNED ✓" else cost,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isOwned) HealthGreen else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (cloudTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = "Ledger", tint = TextSecondary, modifier = Modifier.size(38.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No cloud transactions recorded yet.", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(cloudTransactions, key = { it.id }) { tx ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = TacticalSurface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.itemName.ifBlank { tx.txType }, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${tx.txType} • ${tx.status}", color = ElectricBlueSafe, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (tx.bpDelta != 0L) {
                                        Text(
                                            text = if (tx.bpDelta > 0) "+${tx.bpDelta} BP" else "${tx.bpDelta} BP",
                                            color = if (tx.bpDelta > 0) HealthGreen else HazardYellow,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (tx.creditsDelta != 0L) {
                                        Text(
                                            text = if (tx.creditsDelta > 0) "+${tx.creditsDelta} VC" else "${tx.creditsDelta} VC",
                                            color = ElectricBlueSafe,
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
    }

    // Interactive 3D Preview & Verified Purchase Modal
    val itemToPreview = previewSpec
    if (itemToPreview != null) {
        val isOwned = ownedIds.contains(itemToPreview.id) || itemToPreview.starterOwned
        val priceLabel = when {
            itemToPreview.priceCredits > 0 -> "${itemToPreview.priceCredits} Vortex Credits"
            itemToPreview.priceBp > 0 -> "${itemToPreview.priceBp} BP Coins"
            else -> "Starter Unlock"
        }

        Dialog(onDismissRequest = { previewSpec = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("shop_preview_dialog"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
                border = BorderStroke(2.dp, CyberOrange)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = "Inspect Item", tint = CyberOrange, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("3D ITEM INSPECTION", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                        IconButton(onClick = { previewSpec = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    // Contextual 3D Preview Viewport
                    when (itemToPreview.type) {
                        "GUN", "WEAPON_SKIN" -> {
                            val wModel = remember(itemToPreview.id) {
                                WeaponCatalog.createById(itemToPreview.targetItemId, itemToPreview.name.substringAfter(" - "))
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(175.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                            ) {
                                Weapon3DShowcaseView(
                                    weapon = wModel,
                                    skinName = itemToPreview.name.substringAfter(" - "),
                                    scopeType = ScopeType.SCOPE_4X,
                                    muzzleType = MuzzleType.COMPENSATOR,
                                    magType = MagType.EXTENDED_QUICKDRAW,
                                    autoInspect = true,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        "CHARACTER", "OUTFIT", "BUNDLE" -> {
                            val charId = if (itemToPreview.type == "CHARACTER") itemToPreview.targetItemId else (profile?.selectedCharacterId ?: "vortex_ghost")
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(175.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                            ) {
                                Character3DShowcaseView(
                                    characterPresetId = charId,
                                    animMode = "VICTORY",
                                    autoRotate = true,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Text(itemToPreview.name, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text("${itemToPreview.category.title} • Rarity: ${itemToPreview.rarity}", color = ElectricBlueSafe, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(TacticalSurface)
                            .padding(10.dp)
                    ) {
                        Text(itemToPreview.description, color = TextSecondary, fontSize = 11.sp)
                    }

                    Text("Price: $priceLabel", color = HazardYellow, fontWeight = FontWeight.Black, fontSize = 13.sp)

                    Button(
                        enabled = !isOwned && !isProcessing,
                        onClick = { executeDirectPurchase(itemToPreview) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberOrange),
                        modifier = Modifier.fillMaxWidth().testTag("confirm_bundle_purchase")
                    ) {
                        Text(
                            if (isOwned) "ALREADY OWNED ✓" else "CONFIRM DIRECT PURCHASE",
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}
