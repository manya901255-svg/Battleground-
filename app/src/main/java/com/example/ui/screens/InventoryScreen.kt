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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.example.data.StoreCategory
import com.example.data.cloud.CloudGameRepository
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
fun InventoryScreen(
    repository: GameRepository,
    cloudRepository: CloudGameRepository? = null,
    onOpenStoreCategory: (StoreCategory) -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val allItems by repository.allInventory.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var ownershipFilter by remember { mutableStateOf("ALL") } // ALL, OWNED, LOCKED
    var toastMsg by remember { mutableStateOf<String?>(null) }

    val categories = remember {
        listOf(
            "ALL" to "ALL",
            "CHARACTER" to "CHARACTERS",
            "GUN" to "GUNS",
            "WEAPON_SKIN" to "WEAPON SKINS",
            "OUTFIT" to "OUTFITS",
            "BACKPACK" to "BACKPACKS",
            "HELMET" to "HELMETS",
            "PARACHUTE" to "PARACHUTES",
            "VEHICLE" to "VEHICLES",
            "EMOTE" to "EMOTES",
            "PET" to "PETS",
            "BUNDLE" to "BUNDLES"
        )
    }

    val filteredItems = remember(allItems, selectedTypeFilter, ownershipFilter) {
        allItems
            .filter { selectedTypeFilter == "ALL" || it.type == selectedTypeFilter }
            .filter {
                when (ownershipFilter) {
                    "OWNED" -> it.isOwned
                    "LOCKED" -> !it.isOwned
                    else -> true
                }
            }
            .sortedWith(compareByDescending<InventoryItemEntity> { it.isOwned }.thenByDescending { it.isEquipped }.thenBy { it.name })
    }

    val ownedTotal = allItems.count { it.isOwned }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalDarkBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("inventory_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("inventory_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.Backpack, contentDescription = null, tint = ElectricBlueSafe, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text("TACTICAL INVENTORY & VAULT", color = ElectricBlueSafe, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text("OWNED & LOCKED ITEMS • EQUIP/UNEQUIP • RARITY BADGES", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                toastMsg?.let {
                    Text(it, color = HealthGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TacticalSurface)
                        .border(1.dp, HealthGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text("$ownedTotal / ${allItems.size} OWNED", color = HealthGreen, fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
                Button(
                    onClick = { onOpenStoreCategory(StoreCategory.PREMIUM_BUNDLES) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberOrange),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = "Open Store", tint = Color.Black, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("STORE", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filters Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(categories, key = { it.first }) { (typeKey, label) ->
                    val selected = selectedTypeFilter == typeKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) ElectricBlueSafe else TacticalSurface)
                            .clickable { selectedTypeFilter = typeKey }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("inv_cat_${typeKey.lowercase()}")
                    ) {
                        Text(label, color = if (selected) Color.Black else TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("ALL", "OWNED", "LOCKED").forEach { mode ->
                    val selected = ownershipFilter == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) CyberOrange else TacticalSurface)
                            .clickable { ownershipFilter = mode }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                            .testTag("inv_filter_${mode.lowercase()}")
                    ) {
                        Text(mode, color = if (selected) Color.Black else TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Inventory Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredItems, key = { it.id }) { item ->
                val spec = StoreCatalog.findSpec(item.id)
                val rarityColor = when (item.rarity) {
                    "MYTHIC" -> RarityMythic
                    "LEGENDARY" -> RarityLegendary
                    "EPIC" -> RarityEpic
                    "RARE" -> ElectricBlueSafe
                    else -> TextSecondary
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inventory_item_${item.id}"),
                    shape = RoundedCornerShape(11.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.isOwned) TacticalSurface else Color(0xFF101520)
                    ),
                    border = BorderStroke(
                        width = if (item.isEquipped) 2.dp else 1.dp,
                        color = when {
                            item.isEquipped -> HealthGreen
                            item.isOwned -> rarityColor.copy(alpha = 0.55f)
                            else -> Color.White.copy(alpha = 0.10f)
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                Text(item.rarity, color = rarityColor, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                            when {
                                item.isEquipped -> Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Equipped", tint = HealthGreen, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("EQUIPPED", color = HealthGreen, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                }
                                !item.isOwned -> Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = HazardYellow, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("LOCKED", color = HazardYellow, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                }
                                item.upgradeLevel > 1 -> Text("LV.${item.upgradeLevel}", color = CyberOrange, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        Text(item.name, color = if (item.isOwned) TextPrimary else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = spec?.description ?: "${item.type} • Slot ${item.targetItemId}",
                            color = TextSecondary,
                            fontSize = 8.sp,
                            maxLines = 2,
                            lineHeight = 10.sp,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        if (item.isOwned) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        if (item.isEquipped) {
                                            repository.unequipItem(item)
                                            cloudRepository?.setInventoryItemEquipped(item.id, false)
                                            toastMsg = "Unequipped ${item.name}"
                                        } else {
                                            repository.equipItem(item)
                                            cloudRepository?.setInventoryItemEquipped(item.id, true)
                                            toastMsg = "Equipped ${item.name}"
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (item.isEquipped) TacticalSurfaceVariant else CyberOrange
                                ),
                                contentPadding = PaddingValues(vertical = 2.dp),
                                modifier = Modifier.fillMaxWidth().height(30.dp).testTag("inv_action_${item.id}")
                            ) {
                                Text(
                                    text = if (item.isEquipped) "UNEQUIP" else "EQUIP",
                                    color = if (item.isEquipped) TextPrimary else Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    val targetCat = spec?.category ?: StoreCategory.entries.firstOrNull { it.itemType == item.type } ?: StoreCategory.PREMIUM_BUNDLES
                                    onOpenStoreCategory(targetCat)
                                },
                                contentPadding = PaddingValues(vertical = 2.dp),
                                modifier = Modifier.fillMaxWidth().height(30.dp).testTag("inv_unlock_${item.id}")
                            ) {
                                val priceStr = when {
                                    item.priceCredits > 0 -> "${item.priceCredits} VC"
                                    item.priceBp > 0 -> "${item.priceBp} BP"
                                    else -> "STORE"
                                }
                                Text("UNLOCK • $priceStr", color = HazardYellow, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}
