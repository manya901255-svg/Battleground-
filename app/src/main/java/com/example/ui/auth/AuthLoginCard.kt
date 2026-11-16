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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthLoginPanelCard(
    copy: AuthLocalizedCopy,
    compactHeight: Boolean,
    isBusy: Boolean,
    isCheckingAuto: Boolean,
    activeAuthProvider: String?,
    errorMessage: String?,
    guestPulseProvider: () -> Float,
    onGoogleClick: () -> Unit,
    onAppleClick: () -> Unit,
    onEmailClick: () -> Unit,
    onFacebookClick: () -> Unit,
    onGuestClick: () -> Unit
) {
    val btnHeight = if (compactHeight) 27.dp else 44.dp
    val guestBtnHeight = if (compactHeight) 30.dp else 48.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(WarXGoldBright.copy(alpha = 0.45f), Color.White.copy(alpha = 0.20f))
                ),
                shape = RoundedCornerShape(10.dp)
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = WarXPanelBg)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compactHeight) 10.dp else 14.dp,
                vertical = if (compactHeight) 5.dp else 10.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (compactHeight) 2.5.dp else 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("///", color = WarXGoldBright, fontWeight = FontWeight.Black, fontSize = 11.sp)
                Text(
                    text = copy.loginHeader,
                    color = Color.White,
                    fontSize = if (compactHeight) 11.5.sp else 13.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text("///", color = WarXGoldBright, fontWeight = FontWeight.Black, fontSize = 11.sp)
            }

            if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(WarXRed.copy(alpha = 0.28f))
                        .border(1.dp, WarXRedBright, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val actionLocked = activeAuthProvider != null
            SocialAuthPillButton(
                label = copy.continueGoogle,
                enabled = !actionLocked && !isCheckingAuto,
                isLoading = isCheckingAuto || activeAuthProvider == "google",
                height = btnHeight,
                testTag = "google_sign_in_button",
                leadingIcon = { GoogleBrandMark(size = if (compactHeight) 17.dp else 20.dp) },
                onClick = onGoogleClick
            )

            SocialAuthPillButton(
                label = copy.continueApple,
                enabled = !actionLocked,
                isLoading = activeAuthProvider == "apple",
                height = btnHeight,
                testTag = "apple_sign_in_button",
                leadingIcon = { AppleBrandMark(size = if (compactHeight) 17.dp else 20.dp) },
                onClick = onAppleClick
            )

            SocialAuthPillButton(
                label = copy.loginEmail,
                enabled = !actionLocked,
                isLoading = activeAuthProvider == "email",
                height = btnHeight,
                testTag = "email_sign_in_button",
                leadingIcon = {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = "Email",
                        tint = Color(0xFF141820),
                        modifier = Modifier.size(if (compactHeight) 17.dp else 20.dp)
                    )
                },
                onClick = onEmailClick
            )

            SocialAuthPillButton(
                label = copy.continueFacebook,
                enabled = !actionLocked,
                isLoading = activeAuthProvider == "facebook",
                height = btnHeight,
                testTag = "facebook_sign_in_button",
                leadingIcon = { FacebookBrandMark(size = if (compactHeight) 17.dp else 20.dp) },
                onClick = onFacebookClick
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.24f))
                Text(
                    text = "  ${copy.orDivider}  ",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.24f))
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (!actionLocked) WarXGoldBright else WarXGold.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, Color(0xFFFFF2A6)),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(guestBtnHeight)
                    .graphicsLayer {
                        val s = if (!actionLocked) guestPulseProvider() else 1f
                        scaleX = s
                        scaleY = s
                    }
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = !actionLocked, onClick = onGuestClick)
                    .testTag("guest_sign_in_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Guest Operative",
                        tint = Color(0xFF12141A),
                        modifier = Modifier.size(if (compactHeight) 18.dp else 20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = copy.playGuest,
                        color = Color(0xFF101216),
                        fontWeight = FontWeight.Black,
                        fontSize = if (compactHeight) 12.5.sp else 14.sp,
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF101216),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AuthFooterLinksRow(
    copy: AuthLocalizedCopy,
    compactHeight: Boolean = false,
    onPrivacyClick: () -> Unit,
    onTermsClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FooterUtilityPill(copy.privacyPolicy, Icons.Default.Shield, compactHeight, Modifier.weight(1f), "privacy_policy_link", onPrivacyClick)
        FooterUtilityPill(copy.termsOfService, Icons.Default.Description, compactHeight, Modifier.weight(1f), "terms_of_service_link", onTermsClick)
        FooterUtilityPill(copy.forgotPassword, Icons.AutoMirrored.Filled.HelpOutline, compactHeight, Modifier.weight(1f), "forgot_password_link", onForgotPasswordClick)
    }
}

@Composable
private fun SocialAuthPillButton(
    label: String,
    enabled: Boolean,
    isLoading: Boolean,
    height: Dp,
    testTag: String,
    leadingIcon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val isCompact = height < 34.dp
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (enabled) Color(0xFFFDFEFE) else Color(0xFFD0D4DC),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isCompact) 10.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(if (isCompact) 18.dp else 22.dp), contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(color = WarXRedBright, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                } else {
                    leadingIcon()
                }
            }
            Spacer(modifier = Modifier.width(if (isCompact) 9.dp else 12.dp))
            Text(
                text = if (isLoading) "CONNECTING..." else label,
                color = Color(0xFF101319),
                fontSize = if (isCompact) 11.sp else 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF1B1E26),
                modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
            )
        }
    }
}

@Composable
private fun FooterUtilityPill(
    label: String,
    icon: ImageVector,
    compact: Boolean,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xB80A111F))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = if (compact) 3.dp else 5.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(11.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            color = Color(0xFFEAEFF8),
            fontSize = if (compact) 8.5.sp else 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
