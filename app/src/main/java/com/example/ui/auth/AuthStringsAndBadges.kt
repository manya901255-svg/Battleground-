package com.example.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WarXLogoCrest(
    tagline: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val scale = if (compact) 0.50f else 1f
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .width((58 * scale).dp)
                .height((30 * scale).dp)
        ) {
            val w = size.width
            val h = size.height
            val crownPath = Path().apply {
                moveTo(w * 0.14f, h * 0.90f)
                lineTo(w * 0.04f, h * 0.28f)
                lineTo(w * 0.30f, h * 0.56f)
                lineTo(w * 0.50f, h * 0.06f)
                lineTo(w * 0.70f, h * 0.56f)
                lineTo(w * 0.96f, h * 0.28f)
                lineTo(w * 0.86f, h * 0.90f)
                close()
            }
            drawPath(
                path = crownPath,
                brush = Brush.verticalGradient(listOf(WarXGoldBright, WarXGold, Color(0xFFB36D00)))
            )
            drawPath(
                path = crownPath,
                color = Color(0xFF3E2200),
                style = Stroke(width = 2.4f, join = StrokeJoin.Round)
            )
            drawCircle(WarXGoldBright, radius = 3.5f, center = Offset(w * 0.50f, h * 0.07f))
            drawCircle(WarXGoldBright, radius = 2.8f, center = Offset(w * 0.05f, h * 0.28f))
            drawCircle(WarXGoldBright, radius = 2.8f, center = Offset(w * 0.95f, h * 0.28f))
        }

        Box(contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .width((295 * scale).dp)
                    .height((88 * scale).dp)
            ) {
                val w = size.width
                val h = size.height
                val slash = Path().apply {
                    moveTo(w * 0.04f, h * 0.20f)
                    lineTo(w * 0.94f, h * 0.08f)
                    lineTo(w * 0.99f, h * 0.52f)
                    lineTo(w * 0.92f, h * 0.88f)
                    lineTo(w * 0.02f, h * 0.96f)
                    lineTo(w * 0.08f, h * 0.55f)
                    close()
                }
                drawPath(slash, color = Color(0xEC06080D))
                drawLine(
                    color = WarXRedBright.copy(alpha = 0.45f),
                    start = Offset(w * 0.03f, h * 0.94f),
                    end = Offset(w * 0.96f, h * 0.78f),
                    strokeWidth = 3f
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { rotationZ = -3.4f }
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = Color(0xFFF6F8FC),
                                fontWeight = FontWeight.Black,
                                fontSize = (42 * scale).sp,
                                letterSpacing = 1.5.sp
                            )
                        ) {
                            append("WAR")
                        }
                        withStyle(
                            SpanStyle(
                                color = WarXRedBright,
                                fontWeight = FontWeight.Black,
                                fontSize = (46 * scale).sp,
                                fontStyle = FontStyle.Italic
                            )
                        ) {
                            append("X")
                        }
                    },
                    style = TextStyle(lineHeight = (44 * scale).sp)
                )
                Spacer(modifier = Modifier.height(if (compact) 0.dp else 1.dp))
                Text(
                    text = "BATTLEGROUND",
                    color = WarXGoldBright,
                    fontSize = (21 * scale).sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 2.2.sp,
                    modifier = Modifier.offset(y = (-6 * scale).dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .offset(y = (-8 * scale).dp)
                .graphicsLayer { rotationZ = -2.2f }
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF9F0A18), WarXRed, Color(0xFF9F0A18))
                    )
                )
                .border(1.dp, Color(0xFFFF7D87).copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                .padding(horizontal = (16 * scale).dp, vertical = (3 * scale).dp)
        ) {
            Text(
                text = tagline,
                color = Color.White,
                fontSize = (9.5f * scale).sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.1.sp
            )
        }
    }
}

@Composable
fun BattleRoyaleRevolutionFooter(
    copy: AuthLocalizedCopy,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Column(modifier = Modifier.graphicsLayer { rotationZ = -2.2f }) {
            Text(
                text = copy.joinPrefix,
                color = Color.White,
                fontSize = if (compact) 10.sp else 12.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = 1.2.sp
            )
            Text(
                text = copy.battleRoyale,
                color = WarXGoldBright,
                fontSize = if (compact) 16.sp else 21.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = 0.8.sp
            )
            Text(
                text = copy.revolution,
                color = Color.White,
                fontSize = if (compact) 15.sp else 20.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = 1.sp
            )
            Box(
                modifier = Modifier
                    .width(if (compact) 115.dp else 155.dp)
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(listOf(WarXRedBright, WarXRed, Color.Transparent))
                    )
            )
        }

        Text(
            text = copy.subBrandNote,
            color = Color(0xFFD6DCE8),
            fontSize = if (compact) 8.sp else 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.9.sp
        )
    }
}

@Composable
fun WelcomeSoldierCornerBadge(
    topText: String,
    bottomText: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val badgeW = if (compact) 116.dp else 142.dp
    val badgeH = if (compact) 68.dp else 82.dp
    Box(
        modifier = modifier.size(badgeW, badgeH),
        contentAlignment = Alignment.BottomEnd
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val bg = Path().apply {
                moveTo(0f, h)
                lineTo(w * 0.22f, h * 0.24f)
                lineTo(w, 0f)
                lineTo(w, h)
                close()
            }
            drawPath(bg, color = Color(0xF0080A0F))
            drawLine(WarXGoldBright, Offset(w * 0.22f, h * 0.24f), Offset(w, 0f), strokeWidth = 5f)
            drawLine(WarXGold, Offset(0f, h), Offset(w * 0.22f, h * 0.24f), strokeWidth = 4f)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(end = if (compact) 8.dp else 12.dp, bottom = if (compact) 5.dp else 8.dp)
                .graphicsLayer { rotationZ = -13f }
        ) {
            Canvas(modifier = Modifier.size(if (compact) 22.dp else 28.dp, if (compact) 13.dp else 16.dp)) {
                val w = size.width
                val h = size.height
                val crown = Path().apply {
                    moveTo(w * 0.12f, h * 0.9f)
                    lineTo(0f, h * 0.3f)
                    lineTo(w * 0.3f, h * 0.58f)
                    lineTo(w * 0.5f, h * 0.08f)
                    lineTo(w * 0.7f, h * 0.58f)
                    lineTo(w, h * 0.3f)
                    lineTo(w * 0.88f, h * 0.9f)
                    close()
                }
                drawPath(crown, color = WarXGoldBright)
            }
            Text(
                text = topText,
                color = WarXGoldBright,
                fontSize = if (compact) 9.sp else 11.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic
            )
            Text(
                text = bottomText,
                color = Color.White,
                fontSize = if (compact) 9.sp else 11.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
fun GoogleBrandMark(size: Dp = 20.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = this.size.minDimension * 0.22f
        val inset = stroke / 2f
        val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
        drawArc(Color(0xFFEA4335), 215f, 110f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
        drawArc(Color(0xFFFBBC05), 140f, 78f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
        drawArc(Color(0xFF34A853), 45f, 98f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
        drawArc(Color(0xFF4285F4), -15f, 64f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
        drawLine(Color(0xFF4285F4), Offset(this.size.width * 0.50f, this.size.height * 0.50f), Offset(this.size.width * 0.90f, this.size.height * 0.50f), strokeWidth = stroke)
    }
}

@Composable
fun AppleBrandMark(size: Dp = 20.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val leaf = Path().apply {
            moveTo(w * 0.52f, h * 0.22f)
            quadraticTo(w * 0.55f, h * 0.04f, w * 0.72f, h * 0.02f)
            quadraticTo(w * 0.69f, h * 0.20f, w * 0.52f, h * 0.22f)
            close()
        }
        drawPath(leaf, color = Color(0xFF101216))
        val body = Path().apply {
            moveTo(w * 0.50f, h * 0.28f)
            cubicTo(w * 0.68f, h * 0.22f, w * 0.86f, h * 0.36f, w * 0.84f, h * 0.58f)
            cubicTo(w * 0.82f, h * 0.78f, w * 0.68f, h * 0.94f, w * 0.58f, h * 0.92f)
            cubicTo(w * 0.53f, h * 0.91f, w * 0.47f, h * 0.91f, w * 0.42f, h * 0.92f)
            cubicTo(w * 0.31f, h * 0.94f, w * 0.16f, h * 0.76f, w * 0.15f, h * 0.56f)
            cubicTo(w * 0.14f, h * 0.34f, w * 0.32f, h * 0.22f, w * 0.50f, h * 0.28f)
            close()
        }
        drawPath(body, color = Color(0xFF101216))
        drawCircle(color = Color.White, radius = w * 0.14f, center = Offset(w * 0.84f, h * 0.52f))
    }
}

@Composable
fun FacebookBrandMark(size: Dp = 20.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawRoundRect(color = WarXFacebookBlue, cornerRadius = CornerRadius(w * 0.5f, h * 0.5f), size = this.size)
        val fPath = Path().apply {
            moveTo(w * 0.58f, h * 0.96f)
            lineTo(w * 0.58f, h * 0.58f)
            lineTo(w * 0.72f, h * 0.58f)
            lineTo(w * 0.75f, h * 0.44f)
            lineTo(w * 0.58f, h * 0.44f)
            lineTo(w * 0.58f, h * 0.35f)
            quadraticTo(w * 0.58f, h * 0.28f, w * 0.66f, h * 0.28f)
            lineTo(w * 0.75f, h * 0.28f)
            lineTo(w * 0.75f, h * 0.16f)
            lineTo(w * 0.61f, h * 0.16f)
            quadraticTo(w * 0.44f, h * 0.16f, w * 0.44f, h * 0.34f)
            lineTo(w * 0.44f, h * 0.44f)
            lineTo(w * 0.34f, h * 0.44f)
            lineTo(w * 0.34f, h * 0.58f)
            lineTo(w * 0.44f, h * 0.58f)
            lineTo(w * 0.44f, h * 0.96f)
            close()
        }
        drawPath(fPath, color = Color.White)
    }
}
