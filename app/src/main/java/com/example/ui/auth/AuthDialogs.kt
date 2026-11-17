package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GameRepository
import com.example.data.GameSettingsEntity
import com.example.data.cloud.GoogleAuthHelper
import com.example.game.audio.SoundSynthesizer
import com.example.game.graphics.GraphicsConfig
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun EmailSignInDialog(
    onAuthenticated: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf(GoogleAuthHelper.getPendingEmail(context)) }
    var emailLink by remember { mutableStateOf("") }
    var linkSent by remember { mutableStateOf(email.isNotBlank()) }
    var isBusy by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("email_sign_in_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
            border = BorderStroke(1.5.dp, WarXRedBright)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = WarXGoldBright)
                        Text("PASSWORDLESS EMAIL SIGN-IN", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("email_dialog_close_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Text(
                    "Receive a secure one-tap sign-in link via Firebase Authentication. No password required.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address", fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarXGoldBright,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = WarXGoldBright,
                        unfocusedLabelColor = TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("email_sign_in_input")
                )

                if (linkSent) {
                    OutlinedTextField(
                        value = emailLink,
                        onValueChange = { emailLink = it },
                        label = { Text("Paste Email Sign-In Link from Inbox", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HealthGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("email_link_paste_input")
                    )
                }

                statusMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = if (isError) WarXRedBright else HealthGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isBusy = true
                            isError = false
                            statusMessage = null
                            GoogleAuthHelper.sendEmailSignInLink(
                                context = context,
                                email = email,
                                onLinkSent = {
                                    isBusy = false
                                    linkSent = true
                                    statusMessage = "Verification link sent to ${email.trim()}. Paste the link below or tap it on this device."
                                },
                                onAuthError = { err ->
                                    isBusy = false
                                    isError = true
                                    statusMessage = err
                                },
                                scope = scope
                            )
                        },
                        enabled = !isBusy,
                        border = BorderStroke(1.dp, WarXGoldBright),
                        modifier = Modifier.weight(1f).height(44.dp).testTag("send_email_link_button")
                    ) {
                        if (isBusy && !linkSent) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = WarXGoldBright, strokeWidth = 2.dp)
                        } else {
                            Text(if (linkSent) "RESEND LINK" else "SEND SIGN-IN LINK", color = WarXGoldBright, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }

                    if (linkSent) {
                        Button(
                            onClick = {
                                isBusy = true
                                isError = false
                                statusMessage = null
                                GoogleAuthHelper.completeEmailLinkSignIn(
                                    context = context,
                                    email = email,
                                    emailLink = emailLink,
                                    onAuthSuccess = {
                                        isBusy = false
                                        onAuthenticated()
                                    },
                                    onAuthError = { err ->
                                        isBusy = false
                                        isError = true
                                        statusMessage = err
                                    },
                                    scope = scope
                                )
                            },
                            enabled = !isBusy && emailLink.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = WarXGoldBright),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("verify_email_link_button")
                        ) {
                            Text("VERIFY & DEPLOY", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ForgotPasswordDialog(
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("forgot_password_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
            border = BorderStroke(1.5.dp, WarXGoldBright)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.LockReset, contentDescription = null, tint = WarXGoldBright)
                        Text("RECOVER ACCOUNT ACCESS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("forgot_dialog_close_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Text(
                    "Enter the email registered to your WarX Battleground account to receive an official password reset email.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Registered Email", fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarXGoldBright,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("forgot_password_email_input")
                )

                statusMsg?.let { msg ->
                    Text(msg, color = if (isError) WarXRedBright else HealthGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        isBusy = true
                        isError = false
                        statusMsg = null
                        GoogleAuthHelper.sendPasswordReset(
                            email = email,
                            onResetSent = {
                                isBusy = false
                                statusMsg = "Reset email sent to ${email.trim()}. Check your inbox and spam folder."
                            },
                            onAuthError = { err ->
                                isBusy = false
                                isError = true
                                statusMsg = err
                            },
                            scope = scope
                        )
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = WarXGoldBright),
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("send_password_reset_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text("SEND RESET EMAIL", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AuthSettingsDialog(
    currentSettings: GameSettingsEntity?,
    repository: GameRepository?,
    soundSynthesizer: SoundSynthesizer?,
    onGraphicsUpdated: (GraphicsConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var graphics by remember(currentSettings) { mutableStateOf(currentSettings?.graphicsQuality ?: "High") }
    var targetFps by remember(currentSettings) { mutableIntStateOf(currentSettings?.targetFps ?: 60) }
    var masterVol by remember(currentSettings) { mutableFloatStateOf(currentSettings?.masterVolume ?: 1.0f) }
    var bgmVol by remember(currentSettings) { mutableFloatStateOf(currentSettings?.bgmVolume ?: 0.55f) }

    fun persist(newQuality: String = graphics, newFps: Int = targetFps, newMaster: Float = masterVol, newBgm: Float = bgmVol) {
        val cfg = GraphicsConfig.fromPreset(newQuality).copy(targetFps = newFps)
        onGraphicsUpdated(cfg)
        soundSynthesizer?.setVolume(newMaster, currentSettings?.sfxVolume ?: 1.0f, newBgm)
        scope.launch {
            val base = currentSettings ?: GameSettingsEntity()
            repository?.updateSettings(
                base.copy(
                    graphicsQuality = cfg.presetName,
                    resolutionScale = cfg.resolutionScale,
                    shadowQuality = cfg.shadowQuality,
                    textureQuality = cfg.textureQuality,
                    antiAliasing = cfg.antiAliasing,
                    viewDistance = cfg.viewDistance,
                    effectsQuality = cfg.effectsQuality,
                    targetFps = newFps,
                    masterVolume = newMaster,
                    bgmVolume = newBgm
                )
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("auth_settings_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalDarkBg),
            border = BorderStroke(1.5.dp, WarXRoyalBlue)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = WarXGoldBright)
                        Text("DISPLAY & AUDIO SETTINGS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("auth_settings_close_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Text("3D GRAPHICS QUALITY", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Smooth", "Standard", "High", "Ultra").forEach { preset ->
                        val active = graphics.equals(preset, ignoreCase = true) ||
                            (preset == "Smooth" && graphics.equals("Low", true)) ||
                            (preset == "Standard" && (graphics.equals("Medium", true) || graphics.equals("Balanced", true))) ||
                            (preset == "Ultra" && graphics.equals("Ultra HD", true))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) WarXGoldBright else Color.White.copy(alpha = 0.08f))
                                .border(1.dp, if (active) WarXGoldBright else Color.White.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                                .clickable {
                                    val cfg = com.example.game.graphics.GraphicsConfig.fromPreset(preset)
                                    graphics = cfg.presetName
                                    persist(newQuality = cfg.presetName)
                                }
                                .padding(vertical = 9.dp)
                                .testTag("auth_gfx_${preset.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(preset, color = if (active) Color.Black else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text("TARGET FPS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(30, 60, 90, 120).forEach { fps ->
                        val active = targetFps == fps
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) WarXRedBright else Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    targetFps = fps
                                    persist(newFps = fps)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$fps", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Master Volume", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("${(masterVol * 100).toInt()}%", color = WarXGoldBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = masterVol,
                    onValueChange = {
                        masterVol = it
                        persist(newMaster = it)
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = WarXGoldBright, activeTrackColor = WarXGoldBright)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Lobby Music", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("${(bgmVol * 100).toInt()}%", color = WarXRedBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = bgmVol,
                    onValueChange = {
                        bgmVol = it
                        persist(newBgm = it)
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = WarXRedBright, activeTrackColor = WarXRedBright)
                )
            }
        }
    }
}
