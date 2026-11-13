package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.GameRepository
import com.example.data.UserProfileEntity
import com.example.game.audio.SoundSynthesizer
import com.example.game.engine.BattleRoyaleEngine
import com.example.game.map.MapCatalog
import com.example.game.model.GameModeType
import com.example.game.ui.BattleScreen
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.BattlePassScreen
import com.example.ui.screens.CustomizationScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.screens.SocialScreen
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.MyApplicationTheme
import android.content.pm.ActivityInfo
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.theme.LobbyWallpaperTheme
import com.example.ui.theme.TacticalDarkBg
import kotlinx.coroutines.launch

enum class ScreenState {
    LOBBY,
    BATTLE,
    BATTLE_PASS,
    SHOP,
    CUSTOMIZATION,
    SOCIAL,
    SETTINGS,
    ADMIN
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // Immersive full-screen gaming mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val repository = GameRepository.create(this)
        val soundSynthesizer = SoundSynthesizer(this)

        setContent {
            MyApplicationTheme {
                MainApp(
                    repository = repository,
                    soundSynthesizer = soundSynthesizer
                )
            }
        }
    }
}

@Composable
fun MainApp(
    repository: GameRepository,
    soundSynthesizer: SoundSynthesizer
) {
    val scope = rememberCoroutineScope()
    val profile by repository.userProfile.collectAsState(initial = null)
    val settings by repository.settings.collectAsState(initial = null)

    var currentScreen by remember { mutableStateOf(ScreenState.LOBBY) }
    var selectedMode by remember { mutableStateOf(GameModeType.CLASSIC_BR) }
    var selectedTeamSize by remember { mutableStateOf(4) }
    var selectedMap by remember { mutableStateOf(MapCatalog.getKavalaIsland()) }
    var activeEngine by remember { mutableStateOf<BattleRoyaleEngine?>(null) }

    val currentTheme = remember(settings?.selectedWallpaperTheme) {
        LobbyWallpaperTheme.fromId(settings?.selectedWallpaperTheme ?: "DARK_TACTICAL")
    }

    LaunchedEffect(Unit) {
        repository.initializeDefaultDataIfEmpty()
        soundSynthesizer.startBackgroundMusic()
    }

    DisposableEffect(Unit) {
        onDispose {
            soundSynthesizer.stopBackgroundMusic()
        }
    }

    LaunchedEffect(settings?.masterVolume, settings?.sfxVolume, settings?.bgmVolume) {
        soundSynthesizer.setVolume(
            master = settings?.masterVolume ?: 1.0f,
            sfx = settings?.sfxVolume ?: 1.0f,
            bgm = settings?.bgmVolume ?: 0.55f
        )
    }

    if (profile == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TacticalDarkBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = CyberOrange)
        }
        return
    }

    // Android Hardware Back button handling
    BackHandler(enabled = currentScreen != ScreenState.LOBBY) {
        if (currentScreen == ScreenState.BATTLE) {
            activeEngine?.stop()
            activeEngine = null
            currentScreen = ScreenState.LOBBY
        } else {
            currentScreen = ScreenState.LOBBY
        }
    }

    when (currentScreen) {
        ScreenState.LOBBY -> {
            LobbyScreen(
                profile = profile!!,
                repository = repository,
                selectedMode = selectedMode,
                selectedMap = selectedMap,
                onSelectMap = { selectedMap = it },
                currentTheme = currentTheme,
                onSelectTheme = { theme ->
                    scope.launch { repository.updateWallpaperTheme(theme.id) }
                },
                onSelectMode = { selectedMode = it },
                selectedTeamSize = selectedTeamSize,
                onSelectTeamSize = { selectedTeamSize = it },
                onStartMatch = {
                    soundSynthesizer.playUiClick()
                    val engine = BattleRoyaleEngine(
                        gameMode = selectedMode,
                        mapConfig = selectedMap,
                        soundSynthesizer = soundSynthesizer,
                        autoPickupEnabled = settings?.autoPickupEnabled ?: true,
                        aimAssistEnabled = settings?.aimAssistEnabled ?: true,
                        teamSize = selectedTeamSize,
                        characterPresetId = profile?.selectedCharacterId ?: "vortex_ghost",
                        cameraSensitivity = settings?.cameraSensitivity ?: 1.0f,
                        adsSensitivity = settings?.adsSensitivity ?: 0.85f,
                        onMatchFinished = { placement, kills, damage, durationSec ->
                            scope.launch {
                                repository.recordMatchFinished(
                                    mode = selectedMode.title,
                                    map = selectedMap.name,
                                    placement = placement,
                                    kills = kills,
                                    damage = damage,
                                    durationSec = durationSec
                                )
                            }
                        }
                    )
                    activeEngine = engine
                    engine.start()
                    currentScreen = ScreenState.BATTLE
                },
                onNavigateToBattlePass = { soundSynthesizer.playUiClick(); currentScreen = ScreenState.BATTLE_PASS },
                onNavigateToShop = { soundSynthesizer.playUiClick(); currentScreen = ScreenState.SHOP },
                onNavigateToCustomization = { soundSynthesizer.playUiClick(); currentScreen = ScreenState.CUSTOMIZATION },
                onNavigateToSocial = { soundSynthesizer.playUiClick(); currentScreen = ScreenState.SOCIAL },
                onNavigateToSettings = { soundSynthesizer.playUiClick(); currentScreen = ScreenState.SETTINGS },
                onNavigateToAdmin = { soundSynthesizer.playUiClick(); currentScreen = ScreenState.ADMIN },
                onPlayEmote = { soundSynthesizer.playVictoryFanfare() }
            )
        }

        ScreenState.BATTLE -> {
            val engine = activeEngine
            if (engine != null) {
                DisposableEffect(engine) {
                    onDispose {
                        engine.stop()
                    }
                }
                BattleScreen(
                    engine = engine,
                    hudScale = settings?.hudScale ?: 1.0f,
                    hudOpacity = settings?.hudOpacity ?: 0.90f,
                    graphicsQuality = settings?.graphicsQuality ?: "High",
                    onUpdateGraphicsQuality = { q ->
                        scope.launch { repository.updateGraphicsQuality(q) }
                    },
                    onExitToLobby = {
                        engine.stop()
                        activeEngine = null
                        currentScreen = ScreenState.LOBBY
                    }
                )
            } else {
                currentScreen = ScreenState.LOBBY
            }
        }

        ScreenState.BATTLE_PASS -> {
            BattlePassScreen(
                repository = repository,
                onBack = { currentScreen = ScreenState.LOBBY }
            )
        }

        ScreenState.SHOP -> {
            ShopScreen(
                repository = repository,
                onBack = { currentScreen = ScreenState.LOBBY }
            )
        }

        ScreenState.CUSTOMIZATION -> {
            CustomizationScreen(
                repository = repository,
                onBack = { currentScreen = ScreenState.LOBBY }
            )
        }

        ScreenState.SOCIAL -> {
            SocialScreen(
                repository = repository,
                onBack = { currentScreen = ScreenState.LOBBY }
            )
        }

        ScreenState.SETTINGS -> {
            SettingsScreen(
                repository = repository,
                onBack = { currentScreen = ScreenState.LOBBY }
            )
        }

        ScreenState.ADMIN -> {
            AdminScreen(
                repository = repository,
                onBack = { currentScreen = ScreenState.LOBBY }
            )
        }
    }
}
