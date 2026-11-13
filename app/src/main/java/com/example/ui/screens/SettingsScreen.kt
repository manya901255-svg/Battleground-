package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameRepository
import com.example.data.GameSettingsEntity
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.HazardYellow
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: GameRepository,
    onBack: () -> Unit
) {
    val settingsState by repository.settings.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    var graphics by remember(settingsState) { mutableStateOf(settingsState?.graphicsQuality ?: "Ultra HD") }
    var fps by remember(settingsState) { mutableIntStateOf(settingsState?.targetFps ?: 60) }
    var aimAssist by remember(settingsState) { mutableStateOf(settingsState?.aimAssistEnabled ?: true) }
    var autoPickup by remember(settingsState) { mutableStateOf(settingsState?.autoPickupEnabled ?: true) }
    var gyro by remember(settingsState) { mutableStateOf(settingsState?.gyroscopeEnabled ?: false) }
    var cameraSens by remember(settingsState) { mutableFloatStateOf(settingsState?.cameraSensitivity ?: 1.0f) }
    var adsSens by remember(settingsState) { mutableFloatStateOf(settingsState?.adsSensitivity ?: 0.85f) }
    var hudScale by remember(settingsState) { mutableFloatStateOf(settingsState?.hudScale ?: 1.0f) }
    var hudOpacity by remember(settingsState) { mutableFloatStateOf(settingsState?.hudOpacity ?: 0.90f) }
    var masterVol by remember(settingsState) { mutableFloatStateOf(settingsState?.masterVolume ?: 1.0f) }
    var sfxVol by remember(settingsState) { mutableFloatStateOf(settingsState?.sfxVolume ?: 1.0f) }
    var bgmVol by remember(settingsState) { mutableFloatStateOf(settingsState?.bgmVolume ?: 0.55f) }

    fun saveSettings() {
        scope.launch {
            repository.updateSettings(
                (settingsState ?: GameSettingsEntity()).copy(
                    graphicsQuality = graphics,
                    targetFps = fps,
                    aimAssistEnabled = aimAssist,
                    autoPickupEnabled = autoPickup,
                    gyroscopeEnabled = gyro,
                    cameraSensitivity = cameraSens,
                    adsSensitivity = adsSens,
                    hudScale = hudScale,
                    hudOpacity = hudOpacity,
                    masterVolume = masterVol,
                    sfxVolume = sfxVol,
                    bgmVolume = bgmVol
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalDarkBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("GAME SETTINGS", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("GRAPHICS, CONTROLS & SENSITIVITY", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = {
                    saveSettings()
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberOrange),
                modifier = Modifier.height(34.dp).testTag("save_settings_button")
            ) {
                Text("SAVE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // GRAPHICS & FPS
        SettingSectionTitle("GRAPHICS & DISPLAY")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("GRAPHICS QUALITY", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Low", "Medium", "High", "Ultra").forEach { q ->
                        val isSelected = graphics.startsWith(q, ignoreCase = true) ||
                            (q == "Low" && graphics.equals("Smooth", true)) ||
                            (q == "Medium" && graphics.equals("Balanced", true)) ||
                            (q == "High" && graphics.equals("HD", true)) ||
                            (q == "Ultra" && graphics.equals("Ultra HD", true))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyberOrange else Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    graphics = q
                                    saveSettings()
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                q,
                                color = if (isSelected) Color.Black else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("TARGET FRAME RATE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 60, 90).forEach { f ->
                        val isSelected = fps == f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyberOrange else Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    fps = f
                                    saveSettings()
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$f FPS",
                                color = if (isSelected) Color.Black else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CONTROLS & ASSISTANCE
        SettingSectionTitle("CONTROLS & GAMEPLAY")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                ToggleSettingRow("Aim Assistance", "Helps align crosshair on moving targets", aimAssist) {
                    aimAssist = it
                    saveSettings()
                }
                Spacer(modifier = Modifier.height(10.dp))
                ToggleSettingRow("Auto-Pickup Loot", "Automatically grab high-tier armor and matching ammo", autoPickup) {
                    autoPickup = it
                    saveSettings()
                }
                Spacer(modifier = Modifier.height(10.dp))
                ToggleSettingRow("Gyroscope Aim", "Use device motion sensor for micro-recoil control", gyro) {
                    gyro = it
                    saveSettings()
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SENSITIVITY SLIDERS
        SettingSectionTitle("SENSITIVITY SETTINGS")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Camera Sensitivity", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${(cameraSens * 100).toInt()}%", color = CyberOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = cameraSens,
                    onValueChange = {
                        cameraSens = it
                        saveSettings()
                    },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = CyberOrange, activeTrackColor = CyberOrange)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ADS Scope Sensitivity (2x / 4x / 8x)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${(adsSens * 100).toInt()}%", color = HazardYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = adsSens,
                    onValueChange = {
                        adsSens = it
                        saveSettings()
                    },
                    valueRange = 0.3f..1.8f,
                    colors = SliderDefaults.colors(thumbColor = HazardYellow, activeTrackColor = HazardYellow)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // HUD & CONTROLS CUSTOMIZATION
        SettingSectionTitle("CUSTOMIZABLE HUD & BUTTON LAYOUT")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Button Scale / Size", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${(hudScale * 100).toInt()}%", color = CyberOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = hudScale,
                    onValueChange = {
                        hudScale = it
                        saveSettings()
                    },
                    valueRange = 0.8f..1.35f,
                    colors = SliderDefaults.colors(thumbColor = CyberOrange, activeTrackColor = CyberOrange)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("HUD Transparency / Opacity", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${(hudOpacity * 100).toInt()}%", color = HealthGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = hudOpacity,
                    onValueChange = {
                        hudOpacity = it
                        saveSettings()
                    },
                    valueRange = 0.4f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = HealthGreen, activeTrackColor = HealthGreen)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AUDIO SETTINGS
        SettingSectionTitle("AUDIO & SFX SETTINGS")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Master Volume", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${(masterVol * 100).toInt()}%", color = CyberOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = masterVol,
                    onValueChange = {
                        masterVol = it
                        saveSettings()
                    },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = CyberOrange, activeTrackColor = CyberOrange)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Gunshot & Explosion SFX", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${(sfxVol * 100).toInt()}%", color = HazardYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = sfxVol,
                    onValueChange = {
                        sfxVol = it
                        saveSettings()
                    },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = HazardYellow, activeTrackColor = HazardYellow)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Background Tactical Music", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${(bgmVol * 100).toInt()}%", color = HealthGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = bgmVol,
                    onValueChange = {
                        bgmVol = it
                        saveSettings()
                    },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = HealthGreen, activeTrackColor = HealthGreen)
                )
            }
        }
    }
}

@Composable
fun SettingSectionTitle(title: String) {
    Text(
        text = title,
        color = CyberOrange,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun ToggleSettingRow(title: String, desc: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(desc, color = TextSecondary, fontSize = 10.sp)
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = CyberOrange, checkedTrackColor = CyberOrange.copy(alpha = 0.5f))
        )
    }
}
