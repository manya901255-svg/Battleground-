package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingBag
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
import com.example.ui.theme.AirdropGold
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
    onBack: () -> Unit
) {
    val profile by repository.userProfile.collectAsState(initial = null)
    val ownedInventory by repository.inventory.collectAsState(initial = emptyList())
    val ownedIds = remember(ownedInventory) { ownedInventory.map { it.id }.toSet() }
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var openedCrateItem by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var showOddsDialog by remember { mutableStateOf(false) }
    var shopBannerMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalDarkBg)
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
                    Text("SUPPLY ARMORY & SHOP", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("COSMETICS & CRATES", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Wallet Display
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (shopBannerMsg != null) {
                    Text(shopBannerMsg!!, color = HealthGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text("BP: ${profile?.bpCoins ?: 0}", color = HazardYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("VC: ${profile?.vortexCredits ?: 0}", color = ElectricBlueSafe, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs: Crates vs Direct Shop
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = TacticalSurface,
            contentColor = CyberOrange
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("MYTHIC CRATES", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("DIRECT COSMETICS", fontWeight = FontWeight.Bold) })
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == 0) {
            // Crates Showcase
            Column(modifier = Modifier.fillMaxSize()) {
                // Info banner on transparent odds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TacticalSurface)
                        .clickable { showOddsDialog = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Odds", tint = HazardYellow, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verified Drop Probabilities: Mythic 0.5% • Legendary 3.5% • Epic 16%", color = TextSecondary, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Crate Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CrateCard(
                        title = "Vortex Weapon Crate",
                        subtitle = "Features Glacier M416 & Dragonfire AKM",
                        price = "1,000 BP",
                        color = CyberOrange,
                        modifier = Modifier.weight(1f),
                        onOpen = {
                            scope.launch {
                                val item = repository.openLootCrate(useCredits = false)
                                if (item != null) {
                                    openedCrateItem = item
                                    shopBannerMsg = "Unboxed ${item.name}!"
                                } else {
                                    shopBannerMsg = "Need 1,000 BP Coins!"
                                }
                            }
                        }
                    )
                    CrateCard(
                        title = "Apex Special Forces",
                        subtitle = "Features Cybernetic Assassin Suit & L3 Visor",
                        price = "120 VC",
                        color = ElectricBlueSafe,
                        modifier = Modifier.weight(1f),
                        onOpen = {
                            scope.launch {
                                val item = repository.openLootCrate(useCredits = true)
                                if (item != null) {
                                    openedCrateItem = item
                                    shopBannerMsg = "Unboxed ${item.name}!"
                                } else {
                                    shopBannerMsg = "Need 120 Vortex Credits!"
                                }
                            }
                        }
                    )
                }
            }
        } else {
            // Direct Cosmetic Purchases
            val shopCatalog = listOf(
                InventoryItemEntity("skin_m416_glacier", "M416 - Glacier Frost", "WEAPON_SKIN", "m416", "MYTHIC", priceCredits = 1200),
                InventoryItemEntity("outfit_cyber_assassin", "Cyber Assassin Cyberwear", "OUTFIT", "player", "LEGENDARY", priceCredits = 850),
                InventoryItemEntity("para_matrix", "Matrix Neon Parachute", "PARACHUTE", "parachute", "EPIC", priceBp = 6500),
                InventoryItemEntity("vehicle_buggy_neon", "Offroad Buggy - Neon Beast", "VEHICLE", "buggy", "LEGENDARY", priceCredits = 950),
                InventoryItemEntity("skin_pan_gold", "Golden Pan Supreme", "WEAPON_SKIN", "pan", "EPIC", priceBp = 8000),
                InventoryItemEntity("outfit_desert_spec", "Desert Spec-Ops Camo", "OUTFIT", "player", "RARE", priceBp = 4500)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(shopCatalog) { item ->
                    val isOwned = ownedIds.contains(item.id)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = TacticalSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(item.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                            Text(item.type, color = TextSecondary, fontSize = 10.sp)

                            Spacer(modifier = Modifier.height(10.dp))

                            val cost = if (item.priceCredits > 0) "${item.priceCredits} VC" else "${item.priceBp} BP"
                            Button(
                                enabled = !isOwned,
                                onClick = {
                                    scope.launch {
                                        val ok = repository.purchaseShopItem(item, useCredits = item.priceCredits > 0)
                                        shopBannerMsg = if (ok) "Purchased ${item.name}!" else "Insufficient funds for ${item.name}"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (item.priceCredits > 0) ElectricBlueSafe else CyberOrange,
                                    disabledContainerColor = Color.DarkGray
                                ),
                                modifier = Modifier.fillMaxWidth().height(32.dp)
                            ) {
                                Text(
                                    text = if (isOwned) "OWNED ✓" else cost,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOwned) HealthGreen else Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Crate Reveal Dialog
    if (openedCrateItem != null) {
        Dialog(onDismissRequest = { openedCrateItem = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
                border = androidx.compose.foundation.BorderStroke(2.dp, CyberOrange)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("CRATE UNBOXED!", color = HazardYellow, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(CyberOrange.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = "Item", tint = CyberOrange, modifier = Modifier.size(36.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(openedCrateItem!!.name, color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text(openedCrateItem!!.rarity, color = AirdropGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { openedCrateItem = null },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberOrange),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("COLLECT TO INVENTORY", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Transparent Odds Dialog
    if (showOddsDialog) {
        Dialog(onDismissRequest = { showOddsDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalDarkBg)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("OFFICIAL CRATE PROBABILITIES", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("• Mythic Items (Glacier / Apex): 0.5%", color = RarityMythic, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("• Legendary Items (Dragonfire / Visors): 3.5%", color = RarityLegendary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("• Epic Items (Golden Pan / Special Camo): 16.0%", color = RarityEpic, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("• Rare & Common Items: 80.0%", color = ElectricBlueSafe, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showOddsDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("GOT IT")
                    }
                }
            }
        }
    }
}

@Composable
fun CrateCard(
    title: String,
    subtitle: String,
    price: String,
    color: Color,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit
) {
    Card(
        modifier = modifier.height(180.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.CardGiftcard, contentDescription = "Crate", tint = color, modifier = Modifier.size(36.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(subtitle, color = TextSecondary, fontSize = 9.sp, maxLines = 1)
            }
            Button(
                onClick = onOpen,
                colors = ButtonDefaults.buttonColors(containerColor = color),
                modifier = Modifier.fillMaxWidth().height(34.dp)
            ) {
                Text("OPEN ($price)", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
            }
        }
    }
}
