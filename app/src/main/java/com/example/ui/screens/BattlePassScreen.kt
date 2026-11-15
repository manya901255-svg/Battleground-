package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.data.BattlePassTierEntity
import com.example.data.GameRepository
import com.example.data.cloud.CloudGameRepository
import com.example.data.cloud.CloudMission
import com.example.ui.theme.AirdropGold
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.HazardYellow
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun BattlePassScreen(
    repository: GameRepository,
    cloudRepository: CloudGameRepository? = null,
    cloudMissions: List<CloudMission> = emptyList(),
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val tiers by repository.battlePassTiers.collectAsState(initial = emptyList())
    val profile by repository.userProfile.collectAsState(initial = null)
    val missions by repository.missions.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var statusText by remember { mutableStateOf<String?>(null) }

    val currentTier = ((profile?.level ?: 1) / 2).coerceIn(1, 50)
    val xpFraction = if ((profile?.maxXp ?: 1) > 0) {
        ((profile?.xp ?: 0).toFloat() / (profile?.maxXp ?: 1).toFloat()).coerceIn(0f, 1f)
    } else 0f

    val eligibleLocalCount = missions.count { !it.isClaimed && it.currentProgress >= it.targetCount }
    val eligibleCloudCount = cloudMissions.count { !it.isClaimed && it.currentProgress >= it.targetCount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalDarkBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
            .testTag("battle_pass_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("bp_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("VORTEX PASS • SEASON 54", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("CYBER ONSLAUGHT • VERIFIED MISSIONS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (statusText != null) {
                    Text(statusText!!, color = HealthGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(TacticalSurface)
                        .border(1.dp, HazardYellow, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Stars, contentDescription = "Tier", tint = HazardYellow, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TIER $currentTier / 50", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress bar to next tier
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TacticalSurface)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tier $currentTier Progress", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("${profile?.xp ?: 0} / ${profile?.maxXp ?: 5000} XP", color = CyberOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { xpFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = CyberOrange,
                trackColor = Color.DarkGray
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = TacticalSurface,
            contentColor = CyberOrange
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("REWARDS TRACK", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("SEASON MISSIONS", fontWeight = FontWeight.Bold) })
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tiers) { tier ->
                    BattlePassTierCard(
                        tier = tier,
                        userCurrentTier = currentTier,
                        onClaimFree = {
                            scope.launch {
                                repository.claimBattlePassTier(tier, isElite = false)
                                statusText = "Claimed Tier ${tier.tier} Free Reward"
                            }
                        },
                        onClaimElite = {
                            scope.launch {
                                repository.claimBattlePassTier(tier, isElite = true)
                                statusText = "Claimed Tier ${tier.tier} Elite Reward"
                            }
                        }
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Eligible Rewards: ${eligibleLocalCount + eligibleCloudCount}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        enabled = (eligibleLocalCount + eligibleCloudCount) > 0,
                        onClick = {
                            scope.launch {
                                val localCount = repository.claimAllEligibleMissions()
                                val cloudCount = cloudRepository?.claimAllEligibleMissions(cloudMissions)?.getOrDefault(0) ?: 0
                                val total = localCount + cloudCount
                                statusText = if (total > 0) "Claimed $total completed mission(s)!" else "No completed missions ready."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HealthGreen, disabledContainerColor = TacticalSurfaceVariant),
                        modifier = Modifier.height(34.dp).testTag("claim_all_missions_button")
                    ) {
                        Text("CLAIM ALL ELIGIBLE", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (cloudMissions.isNotEmpty()) {
                        items(cloudMissions, key = { "cloud_${it.id}" }) { cm ->
                            val isComplete = cm.currentProgress >= cm.targetCount
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = TacticalSurface)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("[CLOUD ${cm.category}] ${cm.title}", color = HazardYellow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(cm.description, color = TextSecondary, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Progress: ${cm.currentProgress} / ${cm.targetCount} • +${cm.rewardXp} XP / +${cm.rewardBp} BP / +${cm.rewardCredits} VC",
                                            color = CyberOrange,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    when {
                                        cm.isClaimed -> Text("CLAIMED ✓", color = HealthGreen, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                        isComplete -> Button(
                                            onClick = {
                                                scope.launch {
                                                    cloudRepository?.claimVerifiedMission(cm)?.onSuccess {
                                                        statusText = "Claimed Cloud Mission: ${cm.title}"
                                                    }?.onFailure { e ->
                                                        statusText = e.message
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = HealthGreen),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("CLAIM", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                        }
                                        else -> Text("ACTIVE", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    items(missions, key = { it.id }) { mission ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = TacticalSurface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("[${mission.category}] ${mission.title}", color = CyberOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(mission.description, color = TextSecondary, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Progress: ${mission.currentProgress} / ${mission.targetCount} • +${mission.rewardXp} XP / +${mission.rewardBp} BP",
                                        color = HazardYellow,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                if (mission.isClaimed) {
                                    Text("CLAIMED ✓", color = HealthGreen, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                } else if (mission.isCompleted) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val ok = repository.claimMissionReward(mission.id)
                                                statusText = if (ok) "Claimed ${mission.title}!" else "Not eligible yet."
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = HealthGreen),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("CLAIM", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                    }
                                } else {
                                    Text("ACTIVE", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BattlePassTierCard(
    tier: BattlePassTierEntity,
    userCurrentTier: Int,
    onClaimFree: () -> Unit,
    onClaimElite: () -> Unit
) {
    val isUnlocked = tier.tier <= userCurrentTier

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        border = if (tier.tier == userCurrentTier) androidx.compose.foundation.BorderStroke(1.5.dp, CyberOrange) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (isUnlocked) CyberOrange else TacticalSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text("${tier.tier}", color = if (isUnlocked) Color.Black else TextSecondary, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("FREE", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(tier.freeReward, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                if (tier.isFreeClaimed) {
                    Text("CLAIMED", color = HealthGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                } else if (isUnlocked) {
                    Button(
                        onClick = onClaimFree,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberOrange),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("CLAIM", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("ELITE", color = AirdropGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(tier.eliteReward, color = HazardYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                if (tier.isEliteClaimed) {
                    Text("CLAIMED", color = HealthGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                } else if (isUnlocked) {
                    Button(
                        onClick = onClaimElite,
                        colors = ButtonDefaults.buttonColors(containerColor = AirdropGold),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("CLAIM", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
