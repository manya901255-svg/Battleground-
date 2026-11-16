package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.GameRepository
import com.example.data.InventoryItemEntity
import com.example.data.cloud.CloudGameRepository
import com.example.data.cloud.CloudTransaction
import com.example.data.cloud.CloudUserProfile
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.ElectricBlueSafe
import com.example.ui.theme.HazardYellow
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.RarityEpic
import com.example.ui.theme.RarityLegendary
import com.example.ui.theme.RarityMythic
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
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val profile by repository.userProfile.collectAsState(initial = null)
    val ownedInventory by repository.inventory.collectAsState(initial = emptyList())
    val ownedIds = remember(ownedInventory) { ownedInventory.map { it.id }.toSet() }
    val scope = rememberCoroutineScope()

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    var previewItem by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var shopBannerMsg by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val displayBp = cloudProfile?.bpCoins ?: profile?.bpCoins?.toLong() ?: 0L
    val displayVc = cloudProfile?.vortexCredits ?: profile?.vortexCredits?.toLong() ?: 0L

    val directBundles = remember {
        listOf(
            InventoryItemEntity("skin_m416_glacier", "M416 - Glacier Frost", "WEAPON_SKIN", "m416", "MYTHIC", priceCredits = 1200),
            InventoryItemEntity("outfit_cyber_assassin", "Cyber Assassin Cyberwear", "OUTFIT", "player", "LEGENDARY", priceCredits = 850),
            InventoryItemEntity("para_matrix", "Matrix Neon Parachute", "PARACHUTE", "parachute", "EPIC", priceBp = 6500),
            InventoryItemEntity("vehicle_buggy_neon", "Offroad Buggy - Neon Beast", "VEHICLE", "buggy", "LEGENDARY", priceCredits = 950),
            InventoryItemEntity("skin_pan_gold", "Golden Pan Supreme", "WEAPON_SKIN", "pan", "EPIC", priceBp = 8000),
            InventoryItemEntity("outfit_desert_spec", "Desert Spec-Ops Camo", "OUTFIT", "player", "RARE", priceBp = 4500),
            InventoryItemEntity("skin_awm_nova", "AWM - Solar Flare Direct Pack", "WEAPON_SKIN", "awm", "LEGENDARY", priceBp = 9200),
            InventoryItemEntity("helmet_titan_gold", "Titan Command Visor L3", "HELMET", "helmet", "EPIC", priceBp = 5400)
        )
    }

    fun executeDirectPurchase(item: InventoryItemEntity) {
        if (isProcessing) return
        if (ownedIds.contains(item.id)) {
            shopBannerMsg = "${item.name} is already in your armory."
            return
        }
        val useCredits = item.priceCredits > 0
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
                    repository.purchaseShopItem(item, useCredits = useCredits)
                    shopBannerMsg = msg
                    previewItem = null
                }.onFailure { err ->
                    shopBannerMsg = err.message ?: "Purchase failed."
                }
            } else {
                val ok = repository.purchaseShopItem(item, useCredits = useCredits)
                shopBannerMsg = if (ok) {
                    previewItem = null
                    "Purchased ${item.name}!"
                } else {
                    "Insufficient funds for ${item.name}"
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
            .padding(16.dp)
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
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("DIRECT ARMORY STORE", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("FIXED-PRICE BUNDLES & SERVER TRANSACTION LEDGER", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (shopBannerMsg != null) {
                    Text(shopBannerMsg!!, color = HealthGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text("BP: $displayBp", color = HazardYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("VC: $displayVc", color = ElectricBlueSafe, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = TacticalSurface,
            contentColor = CyberOrange
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.testTag("shop_tab_bundles"),
                text = { Text("DIRECT BUNDLES & COSMETICS", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.testTag("shop_tab_history"),
                text = { Text("TRANSACTION HISTORY (${cloudTransactions.size})", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(directBundles, key = { it.id }) { item ->
                    val isOwned = ownedIds.contains(item.id)
                    val rarityColor = when (item.rarity) {
                        "MYTHIC" -> RarityMythic
                        "LEGENDARY" -> RarityLegendary
                        "EPIC" -> RarityEpic
                        else -> ElectricBlueSafe
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("shop_item_${item.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, rarityColor.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.rarity, color = rarityColor, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                if (isOwned) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Owned", tint = HealthGreen, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("OWNED", color = HealthGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(item.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                            Text("${item.type} • Guaranteed Direct Unlock", color = TextSecondary, fontSize = 10.sp)

                            Spacer(modifier = Modifier.height(10.dp))

                            val cost = if (item.priceCredits > 0) "${item.priceCredits} VC" else "${item.priceBp} BP"
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { previewItem = item },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(34.dp).testTag("inspect_${item.id}")
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = "Inspect", tint = TextPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("INSPECT", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    enabled = !isOwned && !isProcessing,
                                    onClick = { executeDirectPurchase(item) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (item.priceCredits > 0) ElectricBlueSafe else CyberOrange,
                                        disabledContainerColor = TacticalSurfaceVariant
                                    ),
                                    modifier = Modifier.weight(1f).height(34.dp).testTag("buy_${item.id}")
                                ) {
                                    Text(
                                        text = if (isOwned) "OWNED ✓" else cost,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isOwned) HealthGreen else Color.Black
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

    // Item Inspection & Confirmation Modal
    val itemToPreview = previewItem
    if (itemToPreview != null) {
        val isOwned = ownedIds.contains(itemToPreview.id)
        val priceLabel = if (itemToPreview.priceCredits > 0) "${itemToPreview.priceCredits} Vortex Credits" else "${itemToPreview.priceBp} BP Coins"
        Dialog(onDismissRequest = { previewItem = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("shop_preview_dialog"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
                border = androidx.compose.foundation.BorderStroke(2.dp, CyberOrange)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = "Bundle", tint = CyberOrange, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("BUNDLE INSPECTION", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                        IconButton(onClick = { previewItem = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Text(itemToPreview.name, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("Category: ${itemToPreview.type} • Rarity: ${itemToPreview.rarity}", color = ElectricBlueSafe, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(TacticalSurface)
                            .padding(12.dp)
                    ) {
                        Text(
                            "Fixed-price direct cosmetic unlock for slot '${itemToPreview.targetItemId}'. Equip immediately inside Armory after unlocking. Zero randomized drops.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
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
                            color = Color.Black,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}
