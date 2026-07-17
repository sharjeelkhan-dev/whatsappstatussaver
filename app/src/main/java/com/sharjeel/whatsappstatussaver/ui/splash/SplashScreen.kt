package com.sharjeel.whatsappstatussaver.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sharjeel.whatsappstatussaver.R
import com.sharjeel.whatsappstatussaver.theme.WhatsAppStatusSaverTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SplashScreen(onSplashScreenFinished: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Initial logo pop in
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        // Background and logo alpha
        alpha.animateTo(1f,
            animationSpec = tween(800))

        // Staggered text appearance
        delay(300.milliseconds)
        textAlpha.animateTo(1f,
            animationSpec = tween(1000))

        delay(2000.milliseconds)
        onSplashScreenFinished()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00897B),
                        Color(0xFF00695C)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background elements
        BackgroundDecorations()
        Column(
            modifier = Modifier.systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Outer Glow/Pulse
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale)
                        .alpha(0.15f * alpha.value)
                        .background(Color.White, CircleShape)
                )

                // Main Logo Container
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(scale.value)
                        .alpha(alpha.value)
                        .background(Color(0xFF00897B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.import_icon),
                        contentDescription = "Logo",
                        tint = Color(0xFFFFFFFF),
                        modifier = Modifier.size(75.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "STATUS SAVER",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                modifier = Modifier.alpha(textAlpha.value)
            )
        }
    }
}

@Composable
fun BackgroundDecorations() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = Color.White.copy(alpha = 0.05f),
            radius = 400f,
            center = center.copy(x = size.width * 0.1f, y = size.height * 0.1f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.03f),
            radius = 600f,
            center = center.copy(x = size.width * 0.9f, y = size.height * 0.8f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    WhatsAppStatusSaverTheme {
        SplashScreen(onSplashScreenFinished = {})
    }
}

@Preview(showBackground = true)
@Composable
fun BackgroundDecorationsPreview() {
    WhatsAppStatusSaverTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF00897B))
        ) {
            BackgroundDecorations()
        }
    }
}

