package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.cloud.GoogleAuthHelper
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.ElectricBlueSafe
import com.example.ui.theme.HazardYellow
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.RedZoneBombardment
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isCheckingAuto by remember { mutableStateOf(true) }
    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        GoogleAuthHelper.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingAuto = false
                onAuthenticated()
            },
            onUnauthenticated = {
                isCheckingAuto = false
            },
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalDarkBg)
            .testTag("auth_screen")
    ) {
        Image(
            painter = painterResource(id = R.drawable.vortex_lobby_banner_1794457279200),
            contentDescription = "Vortex Strike Tactical Dropzone",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.88f),
                            Color.Black.copy(alpha = 0.56f),
                            Color.Black.copy(alpha = 0.82f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Hero Character Showcase & Brand Header
            Row(
                modifier = Modifier.weight(1.1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.vortex_hero_soldier_1794458613075),
                    contentDescription = "Vortex Operative",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxHeight(0.92f)
                        .widthIn(max = 220.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("V", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        }
                        Text(
                            "SEASON 54 • CLOUD MULTIPLAYER",
                            color = HazardYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = "VORTEX STRIKE",
                        style = MaterialTheme.typography.displayLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 36.sp
                    )
                    Text(
                        text = "100-Player Tactical Survival • Ranked Battle Royale & 4v4 Clash Squad",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FeatureChip("Cloud Sync")
                        FeatureChip("Ranked S54")
                        FeatureChip("Fair Play Anti-Cheat")
                    }
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Right: Authoritative Google Sign-In Card
            Card(
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 360.dp)
                    .border(1.5.dp, CyberOrange.copy(alpha = 0.7f), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface.copy(alpha = 0.94f))
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = "Cloud Security", tint = ElectricBlueSafe)
                        Text(
                            "OPERATIVE DEPLOYMENT GATE",
                            color = ElectricBlueSafe,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Text(
                        text = "Sign in with Google to sync your Armory, Ranked MMR, Guilds, Match Rooms, and Vortex Pass across devices.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    if (errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(RedZoneBombardment.copy(alpha = 0.2f))
                                .border(1.dp, RedZoneBombardment, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = RedZoneBombardment,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = {
                            errorMessage = null
                            isSigningIn = true
                            GoogleAuthHelper.onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isSigningIn = false
                                    onAuthenticated()
                                },
                                onAuthError = { msg ->
                                    isSigningIn = false
                                    errorMessage = msg
                                },
                                onAuthCancelled = {
                                    isSigningIn = false
                                    errorMessage = "Sign-in cancelled. Google authentication is required."
                                },
                                scope = scope
                            )
                        },
                        enabled = !isCheckingAuto && !isSigningIn,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isCheckingAuto || isSigningIn) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isCheckingAuto) "VERIFYING SESSION..." else "AUTHENTICATING...",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("G", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "SIGN IN WITH GOOGLE",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = HealthGreen, modifier = Modifier.size(14.dp))
                        Text(
                            "Zero-Trust Cloud Verification Active",
                            color = HealthGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(TacticalSurface.copy(alpha = 0.85f))
            .border(1.dp, CyberOrange.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
