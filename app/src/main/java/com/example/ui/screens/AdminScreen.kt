package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameRepository
import com.example.data.cloud.CloudGameRepository
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.ElectricBlueSafe
import com.example.ui.theme.HazardYellow
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.RedZoneBombardment
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun AdminScreen(
    repository: GameRepository,
    cloudRepository: CloudGameRepository? = null,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    val currentEmail = remember { runCatching { Firebase.auth.currentUser?.email }.getOrNull() }
    val isVerifiedAdmin = currentEmail.equals("maanudhiman85@gmail.com", ignoreCase = true)

    var statusMessage by remember { mutableStateOf("Server Status: Operational (60 Hz Tickrate)") }
    var annTitle by remember { mutableStateOf("") }
    var annMessage by remember { mutableStateOf("") }

    val adminUsersFlow = remember(cloudRepository, isVerifiedAdmin) {
        if (cloudRepository != null && isVerifiedAdmin) {
            cloudRepository.observeAllUsersAdmin().catch { emit(emptyList()) }
        } else flowOf(emptyList())
    }
    val adminTicketsFlow = remember(cloudRepository, isVerifiedAdmin) {
        if (cloudRepository != null && isVerifiedAdmin) {
            cloudRepository.observeAllSupportTicketsAdmin().catch { emit(emptyList()) }
        } else flowOf(emptyList())
    }
    val adminReportsFlow = remember(cloudRepository, isVerifiedAdmin) {
        if (cloudRepository != null && isVerifiedAdmin) {
            cloudRepository.observeAllReportsAdmin().catch { emit(emptyList()) }
        } else flowOf(emptyList())
    }

    val allUsers by adminUsersFlow.collectAsState(initial = emptyList())
    val tickets by adminTicketsFlow.collectAsState(initial = emptyList())
    val reports by adminReportsFlow.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalDarkBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("admin_screen")
    ) {
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
                    Text("SERVER TELEMETRY, MODERATION & ECONOMY DASHBOARD", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(statusMessage, color = HealthGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SERVER TELEMETRY & HEALTH
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = TacticalSurface)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("DEDICATED MATCH SERVERS", color = CyberOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "OK", tint = HealthGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ALL SYSTEMS 60 HZ", color = HealthGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    AdminMetric("ACTIVE ROOMS", "1,428")
                    AdminMetric("ONLINE CONCURRENCY", "142,800")
                    AdminMetric("AVG PING", "24 ms")
                    AdminMetric("PACKET LOSS", "0.01%")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ANTI-CHEAT MONITORING
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = TacticalSurface)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("VORTEX GUARDIAN ANTI-CHEAT TELEMETRY", color = CyberOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("• Memory Hook & Speed Protection: Enforced via App Check & Firestore Rules", color = TextSecondary, fontSize = 11.sp)
                Text("• Server Authority: Append-only transactions & mission target verification active", color = ElectricBlueSafe, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // REWARD & CURRENCY TEST DISPENSING
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = TacticalSurface)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("LOCAL SANDBOX CURRENCY DISPENSING", color = HazardYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                repository.grantTestCurrency(bp = 5000, credits = 0)
                                statusMessage = "Granted +5,000 Local BP Coins!"
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
                                statusMessage = "Granted +500 Local Credits!"
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

        Spacer(modifier = Modifier.height(12.dp))

        // CLOUD ADMIN CONSOLE
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = TacticalSurface)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Campaign, contentDescription = "Cloud Ops", tint = CyberOrange, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("LIVE CLOUD MODERATION & ANNOUNCEMENTS", color = CyberOrange, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }

                if (!isVerifiedAdmin || cloudRepository == null) {
                    Text(
                        "Admin cloud controls are restricted to the verified operator account (maanudhiman85@gmail.com). Signed in as: ${currentEmail ?: "Guest"}.",
                        color = HazardYellow,
                        fontSize = 11.sp
                    )
                } else {
                    OutlinedTextField(
                        value = annTitle,
                        onValueChange = { annTitle = it },
                        label = { Text("Announcement Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = annMessage,
                        onValueChange = { annMessage = it },
                        label = { Text("Announcement Message") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        enabled = annTitle.isNotBlank() && annMessage.isNotBlank(),
                        onClick = {
                            scope.launch {
                                cloudRepository.publishAnnouncement(annTitle, annMessage, "EVENT")
                                    .onSuccess {
                                        statusMessage = "Published global announcement!"
                                        annTitle = ""; annMessage = ""
                                    }
                                    .onFailure { statusMessage = it.message ?: "Publish failed" }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberOrange),
                        modifier = Modifier.testTag("publish_announcement_button")
                    ) {
                        Text("PUBLISH GLOBAL ANNOUNCEMENT", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }

                    // Support Tickets Queue
                    Text("SUPPORT TICKETS (${tickets.size})", color = ElectricBlueSafe, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    tickets.take(6).forEach { tkt ->
                        Row(
                            modifier = Modifier.fillMaxWidth().background(TacticalDarkBg, RoundedCornerShape(8.dp)).padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("[${tkt.status}] ${tkt.subject}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(tkt.description, color = TextSecondary, fontSize = 9.sp, maxLines = 1)
                            }
                            if (tkt.status != "RESOLVED") {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            cloudRepository.resolveSupportTicket(tkt.id)
                                                .onSuccess { statusMessage = "Resolved ticket ${tkt.id}" }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = HealthGreen),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("RESOLVE", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }

                    // Player Reports Queue
                    Text("FAIR PLAY REPORTS (${reports.size})", color = HazardYellow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    reports.take(6).forEach { rpt ->
                        Row(
                            modifier = Modifier.fillMaxWidth().background(TacticalDarkBg, RoundedCornerShape(8.dp)).padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("[${rpt.status}] ${rpt.targetName}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(rpt.reason, color = TextSecondary, fontSize = 9.sp, maxLines = 1)
                            }
                            if (rpt.status == "PENDING") {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { scope.launch { cloudRepository.reviewPlayerReport(rpt.id, "ACTIONED") } },
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = RedZoneBombardment),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("ACTION", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                    }
                                    Button(
                                        onClick = { scope.launch { cloudRepository.reviewPlayerReport(rpt.id, "DISMISSED") } },
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = TacticalSurface),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("DISMISS", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Player Directory & Ban Control
                    Text("CLOUD PLAYERS (${allUsers.size})", color = CyberOrange, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    allUsers.take(8).forEach { user ->
                        Row(
                            modifier = Modifier.fillMaxWidth().background(TacticalDarkBg, RoundedCornerShape(8.dp)).padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${user.username} (${user.rankTier})", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("UID: ${user.userId} • ${user.bpCoins} BP", color = TextSecondary, fontSize = 9.sp)
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        cloudRepository.setUserBannedStatus(user.userId, !user.isBanned)
                                            .onSuccess { statusMessage = "${user.username} ban=${!user.isBanned}" }
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (user.isBanned) HealthGreen else RedZoneBombardment),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(if (user.isBanned) "UNBAN" else "BAN", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
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
