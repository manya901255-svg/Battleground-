package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.ElectricBlueSafe
import com.example.ui.theme.HazardYellow
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

data class HudSlotOffset(
    val id: String,
    val label: String,
    val offsetX: Float = 0f, // normalized [-0.35f..0.35f] of viewport width
    val offsetY: Float = 0f, // normalized [-0.35f..0.35f] of viewport height
    val scale: Float = 1.0f,
    val opacity: Float = 0.92f
)

object HudLayoutCodec {
    val defaultControlLabels = listOf(
        "JOYSTICK" to "Move Stick",
        "LEFT_FIRE" to "Left Fire",
        "FIRE" to "Right Fire",
        "RIGHT_CLUSTER" to "Aim/Jump Arc",
        "UTILITY_COL" to "Bag/Med/Nade",
        "STATUS_BAR" to "HP/EP & Skills",
        "WEAPON_PANEL" to "Weapons",
        "MINIMAP" to "Minimap"
    )

    fun parse(json: String): Map<String, HudSlotOffset> {
        if (json.isBlank()) return defaultMap()
        val result = defaultMap().toMutableMap()
        json.split(";").forEach { entry ->
            val parts = entry.split(",")
            if (parts.size == 5) {
                val id = parts[0]
                val label = defaultControlLabels.find { it.first == id }?.second ?: id
                result[id] = HudSlotOffset(
                    id = id,
                    label = label,
                    offsetX = parts[1].toFloatOrNull()?.coerceIn(-0.35f, 0.35f) ?: 0f,
                    offsetY = parts[2].toFloatOrNull()?.coerceIn(-0.35f, 0.35f) ?: 0f,
                    scale = parts[3].toFloatOrNull()?.coerceIn(0.65f, 1.50f) ?: 1f,
                    opacity = parts[4].toFloatOrNull()?.coerceIn(0.45f, 1.0f) ?: 0.92f
                )
            }
        }
        return result
    }

    fun serialize(slots: Map<String, HudSlotOffset>): String =
        slots.values.joinToString(";") { s ->
            "${s.id},${"%.3f".format(s.offsetX)},${"%.3f".format(s.offsetY)},${"%.2f".format(s.scale)},${"%.2f".format(s.opacity)}"
        }

    fun defaultMap(): Map<String, HudSlotOffset> =
        defaultControlLabels.associate { (id, label) -> id to HudSlotOffset(id = id, label = label) }
}

@Composable
fun HudCustomizerScreen(
    initialPreset: Int,
    initialLayoutJson: String,
    onSaveLayout: (presetId: Int, layoutJson: String) -> Unit,
    onResetLayout: (presetId: Int) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var activePreset by remember(initialPreset) { mutableIntStateOf(initialPreset) }
    val slotMap = remember(initialLayoutJson, activePreset) {
        mutableStateMapOf<String, HudSlotOffset>().apply {
            putAll(HudLayoutCodec.parse(initialLayoutJson))
        }
    }
    var selectedSlotId by remember { mutableStateOf("FIRE") }
    val selectedSlot = slotMap[selectedSlotId] ?: HudSlotOffset("FIRE", "Right Fire")

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A1220), Color(0xFF141D2E), Color(0xFF0A0D14))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("hud_customizer_screen")
    ) {
        val density = LocalDensity.current
        val areaWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val areaHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        // Top Bar: Back + Presets + Selected Sliders + Reset + Save
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(TacticalDarkBg.copy(alpha = 0.90f))
                .border(1.dp, TacticalBorder)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp).testTag("hud_customizer_back")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("HUD STUDIO", color = HazardYellow, fontWeight = FontWeight.Black, fontSize = 14.sp)

            listOf(1 to "PRESET 1", 2 to "PRESET 2").forEach { (pid, label) ->
                val active = activePreset == pid
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) CyberOrange else TacticalSurface)
                        .clickable { activePreset = pid }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("hud_preset_$pid")
                ) {
                    Text(label, color = if (active) Color.Black else TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(modifier = Modifier.width(120.dp)) {
                Text("${selectedSlot.label} • Size ${(selectedSlot.scale * 100).toInt()}%", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = selectedSlot.scale,
                    onValueChange = { s ->
                        slotMap[selectedSlot.id] = selectedSlot.copy(scale = s)
                    },
                    valueRange = 0.70f..1.45f,
                    modifier = Modifier.height(22.dp),
                    colors = SliderDefaults.colors(thumbColor = CyberOrange, activeTrackColor = CyberOrange)
                )
            }

            Column(modifier = Modifier.width(110.dp)) {
                Text("Alpha ${(selectedSlot.opacity * 100).toInt()}%", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = selectedSlot.opacity,
                    onValueChange = { o ->
                        slotMap[selectedSlot.id] = selectedSlot.copy(opacity = o)
                    },
                    valueRange = 0.50f..1.0f,
                    modifier = Modifier.height(22.dp),
                    colors = SliderDefaults.colors(thumbColor = ElectricBlueSafe, activeTrackColor = ElectricBlueSafe)
                )
            }

            Button(
                onClick = {
                    val defaults = HudLayoutCodec.defaultMap()
                    slotMap.clear()
                    slotMap.putAll(defaults)
                    onResetLayout(activePreset)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TacticalSurface),
                modifier = Modifier.height(36.dp).testTag("hud_reset_button")
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("RESET", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    onSaveLayout(activePreset, HudLayoutCodec.serialize(slotMap))
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = HealthGreen),
                modifier = Modifier.height(36.dp).testTag("hud_save_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.Black, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("SAVE", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }

        // Interactive draggable button blocks positioned around tactical preview canvas
        val baseAnchors = mapOf(
            "MINIMAP" to Alignment.TopStart,
            "WEAPON_PANEL" to Alignment.TopEnd,
            "LEFT_FIRE" to Alignment.CenterStart,
            "JOYSTICK" to Alignment.BottomStart,
            "UTILITY_COL" to Alignment.BottomStart,
            "STATUS_BAR" to Alignment.BottomCenter,
            "FIRE" to Alignment.BottomEnd,
            "RIGHT_CLUSTER" to Alignment.BottomEnd
        )

        val baseMargins = mapOf(
            "MINIMAP" to IntOffset(14, 62),
            "WEAPON_PANEL" to IntOffset(-14, 62),
            "LEFT_FIRE" to IntOffset(14, 24),
            "JOYSTICK" to IntOffset(84, -18),
            "UTILITY_COL" to IntOffset(14, -20),
            "STATUS_BAR" to IntOffset(0, -14),
            "FIRE" to IntOffset(-96, -24),
            "RIGHT_CLUSTER" to IntOffset(-18, -18)
        )

        slotMap.values.forEach { slot ->
            val align = baseAnchors[slot.id] ?: Alignment.Center
            val basePad = baseMargins[slot.id] ?: IntOffset.Zero
            val isSelected = selectedSlotId == slot.id
            val dxPx = (slot.offsetX * areaWidthPx).roundToInt()
            val dyPx = (slot.offsetY * areaHeightPx).roundToInt()
            val isRound = slot.id in setOf("JOYSTICK", "FIRE", "LEFT_FIRE")
            val sizeDp = (if (slot.id == "JOYSTICK") 92 else if (slot.id == "FIRE") 74 else if (slot.id == "LEFT_FIRE") 54 else 68) * slot.scale

            Box(
                modifier = Modifier
                    .align(align)
                    .offset { IntOffset(basePad.x + dxPx, basePad.y + dyPx) }
                    .alpha(slot.opacity)
                    .size(
                        width = if (slot.id == "STATUS_BAR") (190 * slot.scale).dp else if (slot.id == "WEAPON_PANEL") (145 * slot.scale).dp else sizeDp.dp,
                        height = if (slot.id == "STATUS_BAR") (44 * slot.scale).dp else if (slot.id == "WEAPON_PANEL") (56 * slot.scale).dp else sizeDp.dp
                    )
                    .clip(if (isRound) CircleShape else RoundedCornerShape(10.dp))
                    .background(if (isSelected) CyberOrange.copy(alpha = 0.35f) else TacticalSurface.copy(alpha = 0.85f))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) HazardYellow else ElectricBlueSafe,
                        shape = if (isRound) CircleShape else RoundedCornerShape(10.dp)
                    )
                    .clickable { selectedSlotId = slot.id }
                    .pointerInput(slot.id, areaWidthPx, areaHeightPx) {
                        detectDragGestures(
                            onDragStart = { selectedSlotId = slot.id },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val cur = slotMap[slot.id] ?: slot
                                slotMap[slot.id] = cur.copy(
                                    offsetX = (cur.offsetX + dragAmount.x / areaWidthPx).coerceIn(-0.35f, 0.35f),
                                    offsetY = (cur.offsetY + dragAmount.y / areaHeightPx).coerceIn(-0.35f, 0.35f)
                                )
                            }
                        )
                    }
                    .testTag("hud_drag_${slot.id.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = slot.label.uppercase(),
                    color = TextPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Text(
            text = "DRAG ANY CONTROL TO REPOSITION • TAP TO ADJUST SIZE & OPACITY",
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Center)
                .clip(RoundedCornerShape(6.dp))
                .background(TacticalDarkBg.copy(alpha = 0.72f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
