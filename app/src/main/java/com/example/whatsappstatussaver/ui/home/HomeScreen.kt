package com.example.whatsappstatussaver.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whatsappstatussaver.R
import com.example.whatsappstatussaver.data.models.PlatformType
import com.example.whatsappstatussaver.theme.WhatsAppStatusSaverTheme

private val AppTeal = Color(0xFF00897B)
private val LightTeal = Color(0xFFE0F2F1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToStatus: (PlatformType) -> Unit,
    onNavigateToDirectChat: () -> Unit,
    onNavigateToSavedFiles: () -> Unit,
    onNavigateToReminder: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.padding(start = 4.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Status Saver",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = AppTeal
                        )
                        Text(
                            "All-in-one downloader",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(LightTeal, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = AppTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),
                modifier = Modifier.shadow(4.dp)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFB))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Main Platform Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Choose Platform",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF263238)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PremiumPlatformCard(
                        title = "WhatsApp",
                        icon = ImageVector.vectorResource(id = R.drawable.speaking_bubbles_line_icon),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToStatus(PlatformType.WHATSAPP) }
                    )
                    PremiumPlatformCard(
                        title = "Business",
                        icon = ImageVector.vectorResource(id = R.drawable.briefcase_line_icon),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToStatus(PlatformType.WHATSAPP_BUSINESS) }
                    )
                }
            }

            // Quick Tools Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Quick Tools",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF263238)
                )

                ModernToolCard(
                    title = "Direct Chat",
                    subtitle = "Message without saving number",
                    icon = ImageVector.vectorResource(id = R.drawable.navigate_icon),
                    onClick = onNavigateToDirectChat
                )
                ModernToolCard(
                    title = "Saved Gallery",
                    subtitle = "Manage your downloaded media",
                    icon = Icons.Default.AutoAwesomeMotion,
                    onClick = onNavigateToSavedFiles
                )
                ModernToolCard(
                    title = "Daily Reminder",
                    subtitle = "Never miss a status again",
                    icon = ImageVector.vectorResource(id = R.drawable.alarm_clock_icon),
                    onClick = onNavigateToReminder
                )
            }
        }
    }
}

@Composable
fun PremiumPlatformCard(title: String,
                        icon: ImageVector,
                        onClick: () -> Unit,
                        modifier: Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(LightTeal),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon,
                    contentDescription = title,
                    tint = AppTeal,
                    modifier = Modifier.size(30.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF263238),
                fontSize = 16.sp)
        }
    }
}

@Composable
fun ModernToolCard(title: String, subtitle: String,
                   icon: ImageVector,
                   onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LightTeal.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon,
                    contentDescription = null,
                    tint = AppTeal,
                    modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f))
            {
                Text(title, fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238),
                    fontSize = 15.sp)
                Text(subtitle, color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFFCFD8DC),
                modifier = Modifier.size(14.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    WhatsAppStatusSaverTheme {
        HomeScreen({},
            {},
            {},
            {},
            {})
    }
}
