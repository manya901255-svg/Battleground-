package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.credentials.CredentialManager
import com.example.data.GameRepository
import com.example.data.StoreCategory
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
import com.example.ui.screens.CharacterSelectionScreen
import com.example.ui.screens.CustomizationScreen
import com.example.ui.screens.HudCustomizerScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.screens.SocialScreen
import com.example.ui.screens.WeaponCollectionScreen
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.HazardYellow
import com.example.ui.theme.LobbyWallpaperTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TacticalDarkBg
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

enum class ScreenState {
    LOBBY,
    BATTLE,
    BATTLE_PASS,
    SHOP,
    CHARACTERS,
    WEAPONS,
    INVENTORY,
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

        // Immersive full-screen gaming mode without triggering redundant initial wmsSync passes
        window.decorView.post {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val repository = GameRepository.create(this)
        val soundSynthesizer = SoundSynthesizer(this)

        val initialIncomingLink = intent?.data?.toString()

        setContent {
            MyApplicationTheme {
                VortexRootApp(
                    repository = repository,
                    soundSynthesizer = soundSynthesizer,
                    incomingEmailLink = initialIncomingLink
                )
            }
        }
    }
}

@Composable
fun VortexRootApp(
    repository: GameRepository,
    soundSynthesizer: SoundSynthesizer,
    incomingEmailLink: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { Firebase.auth }
    var currentUser by remember { mutableStateOf<FirebaseUser?>(auth.currentUser) }
    var isGuestMode by remember { mutableStateOf(false) }
    var selectedServerId by remember { mutableStateOf("in") }
    var showColdStartWelcome by remember { mutableStateOf(auth.currentUser != null) }

    LaunchedEffect(incomingEmailLink) {
        if (!incomingEmailLink.isNullOrBlank() && auth.isSignInWithEmailLink(incomingEmailLink)) {
            val savedEmail = GoogleAuthHelper.getPendingEmail(context)
            if (savedEmail.isNotBlank()) {
                GoogleAuthHelper.completeEmailLinkSignIn(
                    context = context,
                    email = savedEmail,
                    emailLink = incomingEmailLink,
                    onAuthSuccess = {
                        isGuestMode = false
                        currentUser = auth.currentUser
                    },
                    onAuthError = {},
                    scope = scope
                )
            }
        }
    }

    LaunchedEffect(showColdStartWelcome) {
        if (showColdStartWelcome) {
            delay(2000L)
            showColdStartWelcome = false
        }
    }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
            if (firebaseAuth.currentUser != null) {
                isGuestMode = false
            }
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val signedInUser = currentUser
    when {
        showColdStartWelcome && signedInUser != null -> {
            WarXColdStartWelcomeOverlay(
                onSkipToLobby = { showColdStartWelcome = false }
            )
        }
        signedInUser == null && !isGuestMode -> {
            AuthScreen(
                repository = repository,
                soundSynthesizer = soundSynthesizer,
                selectedServerId = selectedServerId,
                onSelectServer = { selectedServerId = it },
                onGuestLogin = { isGuestMode = true },
                onAuthenticated = {
                    isGuestMode = false
                    currentUser = auth.currentUser
                }
            )
        }
        else -> {
            key(signedInUser?.uid ?: "guest_local_session") {
                MainApp(
                    user = signedInUser,
                    repository = repository,
                    soundSynthesizer = soundSynthesizer,
                    onRequestSignIn = { isGuestMode = false }
                )
            }
        }
    }
}

@Composable
private fun WarXColdStartWelcomeOverlay(
    onSkipToLobby: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalDarkBg)
            .clickable(onClick = onSkipToLobby)
            .testTag("cold_start_welcome_photo")
    ) {
        Image(
            painter = painterResource(id = R.drawable.warx_welcome_hero),
            contentDescription = "WARX Battleground Welcome",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xE6080B14))
                .border(1.2.dp, HazardYellow, RoundedCornerShape(10.dp))
                .padding(horizontal = 18.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LinearProgressIndicator(
                color = CyberOrange,
                trackColor = Color(0xFF1D2436),
                modifier = Modifier.width(110.dp)
            )
            Text(
                text = "ENTERING WARX BATTLEGROUND • TAP TO SKIP",
                color = HazardYellow,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun MainApp(
    user: FirebaseUser?,
    repository: GameRepository,
    soundSynthesizer: SoundSynthesizer,
    onRequestSignIn: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val cloudRepository = remember(context, user?.uid) {
        if (user != null) CloudGameRepository(context) else null
    }

    val profile by repository.userProfile.collectAsState(initial = null)
    val allInventory by repository.allInventory.collectAsState(initial = emptyList())
    val settings by repository.settings.collectAsState(initial = null)

    val cloudProfileFlow = remember(cloudRepository, user?.uid) {
        val uid = user?.uid
        if (cloudRepository != null && uid != null) {
            cloudRepository.observeProfile(uid).catch { emit(null) }
        } else flowOf(null)
    }
    val cloudAnnouncementsFlow = remember(cloudRepository, user?.uid) {
        if (cloudRepository != null && user != null) {
            cloudRepository.observeAnnouncements().catch { emit(emptyList()) }
        } else flowOf(emptyList())
    }
    val cloudMailFlow = remember(cloudRepository, user?.uid) {
        val uid = user?.uid
        if (cloudRepository != null && uid != null) {
            cloudRepository.observeMail(uid).catch { emit(emptyList()) }
        } else flowOf(emptyList())
    }
    val cloudTransactionsFlow = remember(cloudRepository, user?.uid) {
        val uid = user?.uid
        if (cloudRepository != null && uid != null) {
            cloudRepository.observeTransactions(uid).catch { emit(emptyList()) }
        } else flowOf(emptyList())
    }
    val cloudMissionsFlow = remember(cloudRepository, user?.uid) {
        val uid = user?.uid
        if (cloudRepository != null && uid != null) {
            cloudRepository.observeMissions(uid).catch { emit(emptyList()) }
        } else flowOf(emptyList())
    }

    val cloudProfile by cloudProfileFlow.collectAsState(initial = null)
    val cloudAnnouncements by cloudAnnouncementsFlow.collectAsState(initial = emptyList())
    val cloudMail by cloudMailFlow.collectAsState(initial = emptyList())
    val cloudTransactions by cloudTransactionsFlow.collectAsState(initial = emptyList())
    val cloudMissions by cloudMissionsFlow.collectAsState(initial = emptyList())

    var currentScreen by remember { mutableStateOf(ScreenState.LOBBY) }
    var socialInitialTab by remember { mutableIntStateOf(0) }
    var customizationInitialTab by remember { mutableIntStateOf(0) }
    var shopInitialTab by remember { mutableIntStateOf(0) }
    var shopInitialCategory by remember { mutableStateOf<StoreCategory?>(null) }
    var selectedMode by remember { mutableStateOf(GameModeType.CLASSIC_BR) }
    var selectedTeamSize by remember { mutableIntStateOf(4) }
    var selectedMap by remember { mutableStateOf(MapCatalog.getKavalaIsland()) }
    var activeEngine by remember { mutableStateOf<BattleRoyaleEngine?>(null) }

    val lobbyGraphicsConfig = remember(settings) {
        com.example.game.graphics.GraphicsConfig.fromSettings(settings)
    }

    val currentTheme = remember(settings?.selectedWallpaperTheme) {
        LobbyWallpaperTheme.fromId(settings?.selectedWallpaperTheme ?: "DARK_TACTICAL")
    }
    val selectedWeather = remember(settings?.selectedWeather) {
        WeatherType.entries.find { it.name == settings?.selectedWeather } ?: WeatherType.CLEAR
    }

    LaunchedEffect(user?.uid) {
        repository.initializeDefaultDataIfEmpty()
        if (user != null) {
            cloudRepository?.ensureUserInitialized(user.displayName)
        }
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
            selectedCharacterId = cp.selectedCharacterId.ifBlank { local.selectedCharacterId },
            selectedPetId = cp.selectedPetId.ifBlank { local.selectedPetId },
            petLevel = cp.petLevel.toInt().coerceAtLeast(1),
            petVisible = cp.petVisible,
            signatureBio = cp.bio.ifBlank { local.signatureBio }
        )
    }

    val weaponSkinMap = remember(allInventory) {
        allInventory
            .filter { it.type == "WEAPON_SKIN" && it.isOwned && it.isEquipped }
            .associate { it.targetItemId.lowercase() to it.name.substringAfter(" - ").trim() }
    }
    val weaponUpgradeMap = remember(allInventory) {
        allInventory
            .filter { it.type == "GUN" && it.isOwned }
            .associate { it.targetItemId.lowercase() to it.upgradeLevel.coerceIn(1, 5) }
    }
    val equippedWeaponId = remember(effectiveProfile, allInventory) {
        effectiveProfile?.equippedWeaponId?.ifBlank { null }
            ?: allInventory.firstOrNull { it.type == "GUN" && it.isOwned && it.isEquipped }?.targetItemId
            ?: "m416"
    }
    val equippedWeaponSkin = remember(weaponSkinMap, equippedWeaponId) {
        weaponSkinMap[equippedWeaponId.lowercase()] ?: "Tactical Slate"
    }
    val equippedMeleeId = remember(effectiveProfile, allInventory) {
        allInventory.firstOrNull { it.type == "MELEE" && it.isOwned && it.isEquipped }?.targetItemId
            ?: effectiveProfile?.equippedMeleeId?.ifBlank { null }
            ?: "katana"
    }
    val equippedGlooWallId = remember(effectiveProfile, allInventory) {
        allInventory.firstOrNull { it.type == "GLOO_WALL" && it.isOwned && it.isEquipped }?.id
            ?: effectiveProfile?.equippedGlooWallId?.ifBlank { null }
            ?: "gloo_cyber"
    }
    val equippedParachuteId = remember(effectiveProfile, allInventory) {
        allInventory.firstOrNull { it.type == "PARACHUTE" && it.isOwned && it.isEquipped }?.id
            ?: effectiveProfile?.equippedParachuteId?.ifBlank { null }
            ?: "para_flame"
    }
    val equippedVehicleSkinId = remember(effectiveProfile, allInventory) {
        allInventory.firstOrNull { it.type == "VEHICLE" && it.isOwned && it.isEquipped }?.id
            ?: effectiveProfile?.equippedVehicleSkinId?.ifBlank { null }
            ?: "vehicle_uaz_camo"
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

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
        },
        label = "warx_screen_navigation"
    ) { targetScreen ->
        when (targetScreen) {
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
                    graphicsConfig = lobbyGraphicsConfig,
                    announcements = cloudAnnouncements,
                    mailList = cloudMail,
                    onClaimMail = { mail ->
                        scope.launch { cloudRepository?.claimMailReward(mail) }
                    },
                    onSubmitSupportTicket = { subject, msg ->
                        scope.launch { cloudRepository?.submitSupportTicket("PLAYER_SUPPORT", subject, msg) }
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
                            equippedPrimaryWeaponId = equippedWeaponId,
                            equippedMeleeId = equippedMeleeId,
                            glooWallSkinId = equippedGlooWallId,
                            parachuteSkinId = equippedParachuteId,
                            vehicleSkinId = equippedVehicleSkinId,
                            weaponSkinMap = weaponSkinMap,
                            weaponUpgradeMap = weaponUpgradeMap,
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
                                    cloudRepository?.recordCloudMatch(
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
                    onNavigateToShop = {
                        soundSynthesizer.playUiClick()
                        shopInitialTab = 0
                        shopInitialCategory = null
                        currentScreen = ScreenState.SHOP
                    },
                    onNavigateToCustomization = { soundSynthesizer.playUiClick(); customizationInitialTab = 0; currentScreen = ScreenState.CUSTOMIZATION },
                    onNavigateToSocial = { soundSynthesizer.playUiClick(); socialInitialTab = 0; currentScreen = ScreenState.SOCIAL },
                    onNavigateToSocialTab = { tab -> soundSynthesizer.playUiClick(); socialInitialTab = tab; currentScreen = ScreenState.SOCIAL },
                    onNavigateToCustomizationTab = { tab -> soundSynthesizer.playUiClick(); customizationInitialTab = tab; currentScreen = ScreenState.CUSTOMIZATION },
                    onNavigateToShopTab = { tab ->
                        soundSynthesizer.playUiClick()
                        shopInitialTab = tab
                        shopInitialCategory = null
                        currentScreen = ScreenState.SHOP
                    },
                    onNavigateToCharacters = { soundSynthesizer.playUiClick(); currentScreen = ScreenState.CHARACTERS },
                    onNavigateToWeapons = { soundSynthesizer.playUiClick(); currentScreen = ScreenState.WEAPONS },
                    onNavigateToInventory = { soundSynthesizer.playUiClick(); currentScreen = ScreenState.INVENTORY },
                    equippedWeaponId = equippedWeaponId,
                    equippedWeaponSkin = equippedWeaponSkin,
                    equippedVehicleSkinId = equippedVehicleSkinId,
                    onNavigateToSettings = { soundSynthesizer.playUiClick(); currentScreen = ScreenState.SETTINGS },
                    onNavigateToAdmin = { soundSynthesizer.playUiClick(); currentScreen = ScreenState.ADMIN },
                    onPlayEmote = { soundSynthesizer.playVictoryFanfare() }
                )
            }

            ScreenState.CHARACTERS -> {
                CharacterSelectionScreen(
                    repository = repository,
                    cloudRepository = cloudRepository,
                    cloudProfile = cloudProfile,
                    equippedWeaponId = equippedWeaponId,
                    equippedWeaponSkin = equippedWeaponSkin,
                    onBack = { currentScreen = ScreenState.LOBBY }
                )
            }

            ScreenState.WEAPONS -> {
                WeaponCollectionScreen(
                    repository = repository,
                    cloudRepository = cloudRepository,
                    cloudProfile = cloudProfile,
                    onBack = { currentScreen = ScreenState.LOBBY }
                )
            }

            ScreenState.INVENTORY -> {
                InventoryScreen(
                    repository = repository,
                    cloudRepository = cloudRepository,
                    onOpenStoreCategory = { category ->
                        shopInitialTab = 0
                        shopInitialCategory = category
                        currentScreen = ScreenState.SHOP
                    },
                    onBack = { currentScreen = ScreenState.LOBBY }
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
                        graphicsConfig = com.example.game.graphics.GraphicsConfig.fromSettings(settings),
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
                    initialTab = shopInitialTab,
                    initialCategory = shopInitialCategory,
                    onBack = { currentScreen = ScreenState.LOBBY }
                )
            }

            ScreenState.CUSTOMIZATION -> {
                CustomizationScreen(
                    repository = repository,
                    cloudRepository = cloudRepository,
                    initialTab = customizationInitialTab,
                    onBack = { currentScreen = ScreenState.LOBBY }
                )
            }

            ScreenState.SOCIAL -> {
                SocialScreen(
                    repository = repository,
                    cloudRepository = cloudRepository,
                    cloudProfile = cloudProfile,
                    initialTab = socialInitialTab,
                    onBack = { currentScreen = ScreenState.LOBBY }
                )
            }

            ScreenState.SETTINGS -> {
                SettingsScreen(
                    repository = repository,
                    userEmail = user?.email,
                    isGuestMode = user == null,
                    onOpenHudCustomizer = { currentScreen = ScreenState.HUD_CUSTOMIZER },
                    onSignOut = {
                        soundSynthesizer.stopBackgroundMusic()
                        if (user == null) {
                            onRequestSignIn()
                        } else {
                            GoogleAuthHelper.signOut(credentialManager, scope)
                        }
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
}
