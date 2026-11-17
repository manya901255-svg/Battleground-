package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.R

enum class LobbyWallpaperTheme(
    val id: String,
    val title: String,
    val subtitle: String,
    val drawableResId: Int?,
    val accentColor: Color,
    val fallbackGradient: Brush
) {
    DARK_TACTICAL(
        id = "DARK_TACTICAL",
        title = "WarX Crimson Revolution",
        subtitle = "Obsidian & crimson coastal battleground with hero crest",
        drawableResId = R.drawable.warx_welcome_hero,
        accentColor = CyberOrange,
        fallbackGradient = Brush.verticalGradient(listOf(Color(0xFF0A1F4A), Color(0xFF1B070D), Color(0xFF070910)))
    ),
    DESERT_WARFARE(
        id = "DESERT_WARFARE",
        title = "Desert Military Warfare",
        subtitle = "Golden dunes, armored convoys & refinery smoke",
        drawableResId = R.drawable.vortex_theme_desert_1794458568561,
        accentColor = Color(0xFFFF9800),
        fallbackGradient = Brush.verticalGradient(listOf(Color(0xFF3E2723), Color(0xFFBF360C)))
    ),
    NEON_CYBERPUNK(
        id = "NEON_CYBERPUNK",
        title = "Neon Cyberpunk City",
        subtitle = "Rainy neon skyscrapers & cyber battle arena",
        drawableResId = R.drawable.vortex_theme_cyberpunk_1794458584831,
        accentColor = Color(0xFF00E5FF),
        fallbackGradient = Brush.verticalGradient(listOf(Color(0xFF0D1B2A), Color(0xFF1B263B)))
    ),
    SNOW_BASE(
        id = "SNOW_BASE",
        title = "Snow Mountain Military Base",
        subtitle = "Arctic blizzard, radar domes & frozen peaks",
        drawableResId = R.drawable.vortex_theme_snow_1794458598409,
        accentColor = Color(0xFF80D8FF),
        fallbackGradient = Brush.verticalGradient(listOf(Color(0xFF1A237E), Color(0xFF0D47A1)))
    ),
    TROPICAL_ISLAND(
        id = "TROPICAL_ISLAND",
        title = "Tropical Island Battleground",
        subtitle = "Emerald lagoons, palm ruins & coastal military docks",
        drawableResId = null,
        accentColor = Color(0xFF00E676),
        fallbackGradient = Brush.verticalGradient(listOf(Color(0xFF004D40), Color(0xFF00251A)))
    ),
    JUNGLE_SURVIVAL(
        id = "JUNGLE_SURVIVAL",
        title = "Jungle Survival Theme",
        subtitle = "Dense rainforest, ancient monoliths & mist valleys",
        drawableResId = null,
        accentColor = Color(0xFF76FF03),
        fallbackGradient = Brush.verticalGradient(listOf(Color(0xFF1B5E20), Color(0xFF003300)))
    ),
    SCIFI_ARENA(
        id = "SCIFI_ARENA",
        title = "Futuristic Sci-Fi Battle Arena",
        subtitle = "Orbital station battleground with energy shield dome",
        drawableResId = null,
        accentColor = Color(0xFFE040FB),
        fallbackGradient = Brush.verticalGradient(listOf(Color(0xFF311B92), Color(0xFF12005E)))
    ),
    APOCALYPSE(
        id = "APOCALYPSE",
        title = "Apocalypse Battleground",
        subtitle = "Ruined metropolis, volcanic ash embers & crimson dusk",
        drawableResId = null,
        accentColor = Color(0xFFFF1744),
        fallbackGradient = Brush.verticalGradient(listOf(Color(0xFF210000), Color(0xFF3E0000)))
    );

    companion object {
        fun fromId(id: String): LobbyWallpaperTheme {
            return values().firstOrNull { it.id == id } ?: DARK_TACTICAL
        }
    }
}
