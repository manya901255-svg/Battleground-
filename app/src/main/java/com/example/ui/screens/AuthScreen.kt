package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.GameRepository
import com.example.data.cloud.GoogleAuthHelper
import com.example.game.audio.SoundSynthesizer
import com.example.game.graphics.GraphicsConfig
import com.example.ui.auth.Auth3DStageView
import com.example.ui.auth.AuthFooterLinksRow
import com.example.ui.auth.AuthLanguage
import com.example.ui.auth.AuthLoginPanelCard
import com.example.ui.auth.AuthSettingsDialog
import com.example.ui.auth.AuthTopBar
import com.example.ui.auth.BattleRoyaleRevolutionFooter
import com.example.ui.auth.EmailSignInDialog
import com.example.ui.auth.ForgotPasswordDialog
import com.example.ui.auth.GameServerRegion
import com.example.ui.auth.LegalPolicyDialog
import com.example.ui.auth.WarXBlack
import com.example.ui.auth.WarXLogoCrest
import com.example.ui.auth.WarXServerRegions
import com.example.ui.auth.WelcomeSoldierCornerBadge
import com.example.ui.auth.authCopyFor
import kotlinx.coroutines.flow.flowOf

@Composable
fun AuthScreen(
    repository: GameRepository? = null,
    soundSynthesizer: SoundSynthesizer? = null,
    selectedServerId: String = "in",
    onSelectServer: (String) -> Unit = {},
    onGuestLogin: () -> Unit = {},
    onAuthenticated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val settingsEntity by (repository?.settings ?: flowOf(null)).collectAsState(initial = null)
    var activeGraphicsConfig by remember(settingsEntity) {
        mutableStateOf(GraphicsConfig.fromSettings(settingsEntity))
    }

    var isCheckingAuto by remember { mutableStateOf(true) }
    var activeAuthProvider by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedLanguage by remember { mutableStateOf(AuthLanguage.EN) }
    var isMuted by remember { mutableStateOf(false) }

    var showEmailDialog by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var legalDialogType by remember { mutableStateOf<String?>(null) }

    var rawCameraDrag by remember { mutableFloatStateOf(0f) }
    val smoothCameraOffset by animateFloatAsState(
        targetValue = rawCameraDrag,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "auth_cam_drag"
    )
    var uiVisible by remember { mutableStateOf(false) }

    val activeServer: GameServerRegion = remember(selectedServerId) {
        WarXServerRegions.firstOrNull { it.id == selectedServerId } ?: WarXServerRegions.first()
    }
    val copy = remember(selectedLanguage) { authCopyFor(selectedLanguage) }

    val infiniteAnim = rememberInfiniteTransition(label = "warx_auth_pulse")
    val guestPulse by infiniteAnim.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "guest_btn_pulse"
    )

    LaunchedEffect(Unit) {
        uiVisible = true
        GoogleAuthHelper.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingAuto = false
                onAuthenticated()
            },
            onUnauthenticated = { isCheckingAuto = false },
            scope = scope
        )
    }

    val isBusy = isCheckingAuto || activeAuthProvider != null

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(WarXBlack)
            .testTag("auth_screen")
    ) {
        val compactHeight = maxHeight < 500.dp
        val wideLayout = maxWidth >= 860.dp

        Auth3DStageView(
            cameraDragOffset = smoothCameraOffset,
            graphicsConfig = activeGraphicsConfig,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = { rawCameraDrag = 0f },
                        onDragCancel = { rawCameraDrag = 0f }
                    ) { _, dragAmount ->
                        rawCameraDrag = (rawCameraDrag - dragAmount * 0.008f).coerceIn(-1.4f, 1.4f)
                    }
                }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to Color.Black.copy(alpha = 0.25f),
                        0.45f to Color.Transparent,
                        0.72f to Color(0x55081226),
                        1.0f to Color(0x99060912)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0x66060E1F),
                        0.20f to Color.Transparent,
                        0.72f to Color.Transparent,
                        1.0f to Color(0xB804060A)
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(
                    horizontal = if (wideLayout) 24.dp else 14.dp,
                    vertical = if (compactHeight) 4.dp else 12.dp
                )
        ) {
            AuthTopBar(
                copy = copy,
                activeServer = activeServer,
                selectedLanguage = selectedLanguage,
                isMuted = isMuted,
                compactHeight = compactHeight,
                onSelectServer = onSelectServer,
                onSelectLanguage = { selectedLanguage = it },
                onToggleMute = {
                    isMuted = !isMuted
                    soundSynthesizer?.setMuted(isMuted)
                    if (!isMuted) soundSynthesizer?.playUiClick()
                },
                onOpenSettings = {
                    soundSynthesizer?.playUiClick()
                    showSettingsDialog = true
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            BattleRoyaleRevolutionFooter(
                copy = copy,
                compact = compactHeight,
                modifier = Modifier.align(Alignment.BottomStart)
            )

            AnimatedVisibility(
                visible = uiVisible,
                enter = fadeIn(tween(420)) + slideInHorizontally(
                    initialOffsetX = { it / 4 },
                    animationSpec = tween(480, easing = FastOutSlowInEasing)
                ),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(
                        end = if (wideLayout) 72.dp else 56.dp,
                        top = if (compactHeight) 14.dp else 16.dp,
                        bottom = if (compactHeight) 2.dp else 28.dp
                    )
            ) {
                val panelWidth = if (wideLayout) 360.dp else if (compactHeight) 304.dp else 330.dp

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(min = 270.dp, max = panelWidth)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    WarXLogoCrest(tagline = copy.tagline, compact = compactHeight)

                    Spacer(modifier = Modifier.height(if (compactHeight) 2.dp else 6.dp))

                    AuthLoginPanelCard(
                        copy = copy,
                        compactHeight = compactHeight,
                        isBusy = isBusy,
                        isCheckingAuto = isCheckingAuto,
                        activeAuthProvider = activeAuthProvider,
                        errorMessage = errorMessage,
                        guestPulseProvider = { guestPulse },
                        onGoogleClick = {
                            soundSynthesizer?.playUiClick()
                            errorMessage = null
                            activeAuthProvider = "google"
                            GoogleAuthHelper.onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    activeAuthProvider = null
                                    onAuthenticated()
                                },
                                onAuthError = { msg ->
                                    activeAuthProvider = null
                                    errorMessage = msg
                                },
                                onAuthCancelled = {
                                    activeAuthProvider = null
                                    errorMessage = "Google sign-in cancelled."
                                },
                                scope = scope
                            )
                        },
                        onAppleClick = {
                            soundSynthesizer?.playUiClick()
                            isCheckingAuto = false
                            errorMessage = null
                            activeAuthProvider = "apple"
                            GoogleAuthHelper.signInWithOAuthProvider(
                                context = context,
                                providerId = "apple.com",
                                languageCode = selectedLanguage.code,
                                onAuthSuccess = {
                                    activeAuthProvider = null
                                    onAuthenticated()
                                },
                                onAuthError = { msg ->
                                    activeAuthProvider = null
                                    errorMessage = msg
                                },
                                onAuthCancelled = {
                                    activeAuthProvider = null
                                    errorMessage = "Apple sign-in cancelled."
                                },
                                scope = scope
                            )
                        },
                        onEmailClick = {
                            soundSynthesizer?.playUiClick()
                            isCheckingAuto = false
                            errorMessage = null
                            showEmailDialog = true
                        },
                        onFacebookClick = {
                            soundSynthesizer?.playUiClick()
                            isCheckingAuto = false
                            errorMessage = null
                            activeAuthProvider = "facebook"
                            GoogleAuthHelper.signInWithOAuthProvider(
                                context = context,
                                providerId = "facebook.com",
                                languageCode = selectedLanguage.code,
                                onAuthSuccess = {
                                    activeAuthProvider = null
                                    onAuthenticated()
                                },
                                onAuthError = { msg ->
                                    activeAuthProvider = null
                                    errorMessage = msg
                                },
                                onAuthCancelled = {
                                    activeAuthProvider = null
                                    errorMessage = "Facebook sign-in cancelled."
                                },
                                scope = scope
                            )
                        },
                        onGuestClick = {
                            soundSynthesizer?.playUiClick()
                            isCheckingAuto = false
                            onGuestLogin()
                        }
                    )

                    Spacer(modifier = Modifier.height(if (compactHeight) 3.dp else 7.dp))

                    AuthFooterLinksRow(
                        copy = copy,
                        compactHeight = compactHeight,
                        onPrivacyClick = {
                            soundSynthesizer?.playUiClick()
                            legalDialogType = "privacy"
                        },
                        onTermsClick = {
                            soundSynthesizer?.playUiClick()
                            legalDialogType = "terms"
                        },
                        onForgotPasswordClick = {
                            soundSynthesizer?.playUiClick()
                            showForgotPasswordDialog = true
                        }
                    )
                }
            }
        }

        WelcomeSoldierCornerBadge(
            topText = copy.welcomeSoldierTop,
            bottomText = copy.welcomeSoldierBottom,
            compact = compactHeight,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .testTag("welcome_soldier_badge")
        )
    }

    if (showEmailDialog) {
        EmailSignInDialog(
            onAuthenticated = {
                showEmailDialog = false
                onAuthenticated()
            },
            onDismiss = { showEmailDialog = false }
        )
    }
    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(onDismiss = { showForgotPasswordDialog = false })
    }
    if (showSettingsDialog) {
        AuthSettingsDialog(
            currentSettings = settingsEntity,
            repository = repository,
            soundSynthesizer = soundSynthesizer,
            onGraphicsUpdated = { activeGraphicsConfig = it },
            onDismiss = { showSettingsDialog = false }
        )
    }
    if (legalDialogType != null) {
        LegalPolicyDialog(
            isPrivacyPolicy = legalDialogType == "privacy",
            onDismiss = { legalDialogType = null }
        )
    }
}
