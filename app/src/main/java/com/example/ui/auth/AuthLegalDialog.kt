package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TextSecondary

@Composable
fun LegalPolicyDialog(
    isPrivacyPolicy: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag(if (isPrivacyPolicy) "privacy_policy_dialog" else "terms_of_service_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
            border = BorderStroke(1.5.dp, WarXGoldBright)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            if (isPrivacyPolicy) Icons.Default.PrivacyTip else Icons.Default.Gavel,
                            contentDescription = null,
                            tint = WarXGoldBright
                        )
                        Text(
                            if (isPrivacyPolicy) "WARX PRIVACY POLICY" else "TERMS OF SERVICE",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("legal_dialog_close_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isPrivacyPolicy) {
                        Text("1. Account & Authentication Data", color = WarXGoldBright, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            "When signing in via Google, Apple, Facebook, or Email Link, WarX Battleground stores only your Firebase user UID, display name, ranked progress, and cosmetic inventory in encrypted Cloud Firestore.",
                            color = TextSecondary, fontSize = 11.sp
                        )
                        Text("2. Local Guest Storage", color = WarXGoldBright, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            "Playing as Guest stores loadout preferences, sensitivity settings, and practice records exclusively in on-device storage without creating cloud telemetry.",
                            color = TextSecondary, fontSize = 11.sp
                        )
                        Text("3. Fair Play & Security", color = WarXGoldBright, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            "Matchmaking telemetry and integrity checks run solely to protect competitive Ranked Season fairness. Your credentials are never shared with third parties.",
                            color = TextSecondary, fontSize = 11.sp
                        )
                    } else {
                        Text("1. Operative Conduct & Fair Play", color = WarXGoldBright, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            "WarX Battleground enforces a zero-tolerance anti-cheat and fair-play policy. Tampering with match data, unauthorized memory tools, or abusive comms may result in account suspension.",
                            color = TextSecondary, fontSize = 11.sp
                        )
                        Text("2. Virtual Items & Progression", color = WarXGoldBright, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            "Gold Coins (BP), Vortex Credits, character skins, weapons, and Battle Pass unlocks are licensed in-game digital items validated by server rules and cannot be exchanged for real currency.",
                            color = TextSecondary, fontSize = 11.sp
                        )
                        Text("3. Guest vs. Cloud Accounts", color = WarXGoldBright, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            "Guest sessions are tied to local device storage. To protect progress across reinstalls or devices, sign in anytime via Settings or the Login Screen.",
                            color = TextSecondary, fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = WarXGoldBright),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text("UNDERSTOOD", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}
