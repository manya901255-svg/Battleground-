package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameRepository
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.ElectricBlueSafe
import com.example.ui.theme.HazardYellow
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.RedZoneBombardment
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun AdminScreen(
    repository: GameRepository,
    onBack: () -> Unit
) {
    val profile by repository.userProfile.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf("Server Status: Operational (60 Hz Tickrate)") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalDarkBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("admin_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("GAME OPERATIONS CENTER", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("ADMINISTRATIVE MATCH & ECONOMY DASHBOARD", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SERVER TELEMETRY & HEALTH
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DEDICATED MATCH SERVERS", color = CyberOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "OK", tint = HealthGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ALL SYSTEMS 60 HZ", color = HealthGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AdminMetric("ACTIVE ROOMS", "1,428")
                    AdminMetric("ONLINE CONCURRENCY", "142,800")
                    AdminMetric("AVG PING", "24 ms")
                    AdminMetric("PACKET LOSS", "0.01%")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ANTI-CHEAT MONITORING
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("VORTEX GUARDIAN ANTI-CHEAT TELEMETRY", color = CyberOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Speed Hack Detection: 0 violations in last 60 mins", color = TextSecondary, fontSize = 11.sp)
                Text("• Memory Hook Protection: Enforced via App Check & Integrity", color = TextSecondary, fontSize = 11.sp)
                Text("• Recoil Script Heuristics: Zero suspicious aim angles detected", color = HealthGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("• Client-Server State Authority: Active on all 100 physics slots", color = ElectricBlueSafe, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // REWARD & CURRENCY TEST DISPENSING
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("TEST REWARD & ECONOMY GRANT", color = HazardYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Dispense test currency directly into the local persistent wallet:", color = TextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                repository.grantTestCurrency(bp = 5000, credits = 0)
                                statusMessage = "Granted +5,000 BP Coins!"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HazardYellow),
                        modifier = Modifier.weight(1f).testTag("grant_bp_button")
                    ) {
                        Text("+5,000 BP", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                repository.grantTestCurrency(bp = 0, credits = 500)
                                statusMessage = "Granted +500 Vortex Credits!"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlueSafe),
                        modifier = Modifier.weight(1f).testTag("grant_credits_button")
                    ) {
                        Text("+500 CREDITS", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // STATUS TOAST
        Text(
            text = statusMessage,
            color = HealthGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun AdminMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}
