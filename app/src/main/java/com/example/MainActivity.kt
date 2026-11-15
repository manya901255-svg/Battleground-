package com.example

import android.content.pm.ActivityInfo
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.credentials.CredentialManager
import com.example.data.GameRepository
import com.example.data.cloud.CloudGameRepository
import com.example.data.cloud.GoogleAuthHelper
import com.example.game.audio.SoundSynthesizer
import com.example.game.engine.BattleRoyaleEngine
import com.example.game.map.MapCatalog
import com.example.game.model.GameModeType
import com.example.game.model.WeatherType
import com.example.game.ui.BattleScreen
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BattlePassScreen
import com.example.ui.screens.CustomizationScreen
import com.example.ui.screens.HudCustomizerScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.screens.SocialScreen
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.LobbyWallpaperTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TacticalDarkBg
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

enum class ScreenState {
    LOBBY,
    BATTLE,
    BATTLE_PASS,
    SHOP,
    CUSTOMIZATION,
    SOCIAL,
    SETTINGS,
    HUD_CUSTOMIZER,
    ADMIN
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                VortexRootApp(
                    repository = repository,
                    soundSynthesizer = soundSynthesizer
                )
            }
        }
    }
}

@Composable
fun VortexRootApp(
    repository: GameRepository,
    soundSynthesizer: SoundSynthesizer
) {
    val auth = remember { Firebase.auth }
    var currentUser by remember { mutableStateOf<FirebaseUser?>(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val signedInUser = currentUser
    if (signedInUser == null) {
        AuthScreen(
            onAuthenticated = {
                currentUser = auth.currentUser
            }
        )
    } else {
        key(signedInUser.uid) {
            MainApp(
                user = signedInUser,
                repository = repository,
                soundSynthesizer = soundSynthesizer
            )
        }
    }
}

@Composable
fun MainApp(
    user: FirebaseUser,
    repository: GameRepository,
    soundSynthesizer: SoundSynthesizer
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val cloudRepository = remember(context) { CloudGameRepository(context) }

    val profile by repository.userProfile.collectAsState(initial = null)
    val settings by repository.settings.collectAsState(initial = null)

    val cloudProfileFlow = remember(cloudRepository, user.uid) {
        cloudRepository.observeProfile(user.uid).catch { emit(null) }
    }
    val cloudAnnouncementsFlow = remember(cloudRepository) {
        cloudRepository.observeAnnouncements().catch { emit(emptyList()) }
    }
    val cloudMailFlow = remember(cloudRepository, user.uid) {
        cloudRepository.observeMail(user.uid).catch { emit(emptyList()) }
    }
    val cloudTransactionsFlow = remember(cloudRepository, user.uid) {
        cloudRepository.observeTransactions(user.uid).catch { emit(emptyList()) }
    }
    val cloudMissionsFlow = remember(cloudRepository, user.uid) {
        cloudRepository.observeMissions(user.uid).catch { emit(emptyList()) }
    }

    val cloudProfile by cloudProfileFlow.collectAsState(initial = null)
    val cloudAnnouncements by cloudAnnouncementsFlow.collectAsState(initial = emptyList())
    val cloudMail by cloudMailFlow.collectAsState(initial = emptyList())
    val cloudTransactions by cloudTransactionsFlow.collectAsState(initial = emptyList())
    val cloudMissions by cloudMissionsFlow.collectAsState(initial = emptyList())

    var currentScreen by remember { mutableStateOf(ScreenState.LOBBY) }
    var selectedMode by remember { mutableStateOf(GameModeType.CLASSIC_BR) }
    var selectedTeamSize by remember { mutableStateOf(4) }
    var selectedMap by remember { mutableStateOf(MapCatalog.getKavalaIsland()) }
    var activeEngine by remember { mutableStateOf<BattleRoyaleEngine?>(null) }

    val currentTheme = remember(settings?.selectedWallpaperTheme) {
        LobbyWallpaperTheme.fromId(settings?.selectedWallpaperTheme ?: "DARK_TACTICAL")
    }
    val selectedWeather = remember(settings?.selectedWeather) {
        WeatherType.entries.find { it.name == settings?.selectedWeather } ?: WeatherType.CLEAR
    }

    LaunchedEffect(user.uid) {
        repository.initializeDefaultDataIfEmpty()
        cloudRepository.ensureUserInitialized(user.displayName)
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

    val effectiveProfile = remember(profile, cloudProfile) {
        val local = profile ?: return@remember null
        val cp = cloudProfile ?: return@remember local
        local.copy(
            username = cp.username.ifBlank { local.username },
            level = cp.level.toInt().coerceAtLeast(1),
            xp = cp.xp.toInt(),
            maxXp = cp.maxXp.toInt().coerceAtLeast(100),
            bpCoins = cp.bpCoins.toInt(),
            vortexCredits = cp.vortexCredits.toInt(),
            rankTier = cp.rankTier.ifBlank { local.rankTier },
            rankPoints = cp.rankPoints.toInt(),
            matchesPlayed = cp.matchesPlayed.toInt(),
            wins = cp.wins.toInt(),
            kills = cp.kills.toInt(),
            deaths = cp.deaths.toInt(),
            petLevel = cp.petLevel.toInt().coerceAtLeast(1),
            petVisible = cp.petVisible,
            signatureBio = cp.bio.ifBlank { local.signatureBio }
        )
    }

    if (effectiveProfile == null) {
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

    BackHandler(enabled = currentScreen != ScreenState.LOBBY) {
        when (currentScreen) {
            ScreenState.BATTLE -> {
                activeEngine?.stop()
                activeEngine = null
                currentScreen = ScreenState.LOBBY
            }
            ScreenState.HUD_CUSTOMIZER -> {
                currentScreen = ScreenState.SETTINGS
            }
            else -> currentScreen = ScreenState.LOBBY
        }
    }

    when (currentScreen) {
        ScreenState.LOBBY -> {
            LobbyScreen(
                profile = effectiveProfile,
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
                selectedWeather = selectedWeather,
                onSelectWeather = { weather ->
                    scope.launch { repository.updateSelectedWeather(weather.name) }
                },
                announcements = cloudAnnouncements,
                mailList = cloudMail,
                onClaimMail = { mail ->
                    scope.launch { cloudRepository.claimMailReward(mail) }
                },
                onSubmitSupportTicket = { subject, msg ->
                    scope.launch { cloudRepository.submitSupportTicket("PLAYER_SUPPORT", subject, msg) }
                },
                onStartMatch = {
                    soundSynthesizer.playUiClick()
                    lateinit var createdEngine: BattleRoyaleEngine
                    createdEngine = BattleRoyaleEngine(
                        gameMode = selectedMode,
                        mapConfig = selectedMap,
                        soundSynthesizer = soundSynthesizer,
                        autoPickupEnabled = settings?.autoPickupEnabled ?: true,
                        aimAssistEnabled = settings?.aimAssistEnabled ?: true,
                        teamSize = selectedTeamSize,
                        characterPresetId = effectiveProfile.selectedCharacterId,
                        activePetId = effectiveProfile.selectedPetId,
                        weatherType = selectedWeather,
                        petVisible = effectiveProfile.petVisible,
                        cameraSensitivity = settings?.cameraSensitivity ?: 1.0f,
                        adsSensitivity = settings?.adsSensitivity ?: 0.85f,
                        onMatchFinished = { placement, kills, damage, durationSec ->
                            val assists = createdEngine.engineState.value.userAssists
                            scope.launch {
                                repository.recordMatchFinished(
                                    mode = selectedMode.title,
                                    map = selectedMap.name,
                                    placement = placement,
                                    kills = kills,
                                    damage = damage,
                                    durationSec = durationSec
                                )
                                cloudRepository.recordCloudMatch(
                                    gameMode = selectedMode.title,
                                    mapName = selectedMap.name,
                                    placement = placement,
                                    kills = kills,
                                    assists = assists,
                                    damage = damage,
                                    durationSec = durationSec
                                )
                            }
                        }
                    )
                    activeEngine = createdEngine
                    createdEngine.start()
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
                    hudLayoutJson = settings?.hudLayoutJson ?: "",
                    graphicsQuality = settings?.graphicsQuality ?: "High",
                    leftHandFireEnabled = settings?.leftHandFireEnabled ?: true,
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
                cloudRepository = cloudRepository,
                cloudMissions = cloudMissions,
                onBack = { currentScreen = ScreenState.LOBBY }
            )
        }

        ScreenState.SHOP -> {
            ShopScreen(
                repository = repository,
                cloudRepository = cloudRepository,
                cloudTransactions = cloudTransactions,
                cloudProfile = cloudProfile,
                onBack = { currentScreen = ScreenState.LOBBY }
            )
        }

        ScreenState.CUSTOMIZATION -> {
            CustomizationScreen(
                repository = repository,
                cloudRepository = cloudRepository,
                onBack = { currentScreen = ScreenState.LOBBY }
            )
        }

        ScreenState.SOCIAL -> {
            SocialScreen(
                repository = repository,
                cloudRepository = cloudRepository,
                cloudProfile = cloudProfile,
                onBack = { currentScreen = ScreenState.LOBBY }
            )
        }

        ScreenState.SETTINGS -> {
            SettingsScreen(
                repository = repository,
                userEmail = user.email,
                onOpenHudCustomizer = { currentScreen = ScreenState.HUD_CUSTOMIZER },
                onSignOut = {
                    soundSynthesizer.stopBackgroundMusic()
                    GoogleAuthHelper.signOut(credentialManager, scope)
                },
                onBack = { currentScreen = ScreenState.LOBBY }
            )
        }

        ScreenState.HUD_CUSTOMIZER -> {
            HudCustomizerScreen(
                initialPreset = settings?.hudLayoutPreset ?: 1,
                initialLayoutJson = settings?.hudLayoutJson ?: "",
                onSaveLayout = { presetId, json ->
                    scope.launch {
                        repository.saveHudLayout(presetId, json)
                        currentScreen = ScreenState.SETTINGS
                    }
                },
                onResetLayout = { presetId ->
                    scope.launch {
                        repository.resetHudLayout(presetId)
                    }
                },
                onBack = { currentScreen = ScreenState.SETTINGS }
            )
        }

        ScreenState.ADMIN -> {
            AdminScreen(
                repository = repository,
                cloudRepository = cloudRepository,
                onBack = { currentScreen = ScreenState.LOBBY }
            )
        }
    }
}
