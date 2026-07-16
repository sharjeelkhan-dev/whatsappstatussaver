package com.example.whatsappstatussaver.ui.home

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.whatsappstatussaver.R
import com.example.whatsappstatussaver.data.models.PlatformType
import com.example.whatsappstatussaver.theme.WhatsAppStatusSaverTheme

private val PrimaryGreen = Color(0xFF00A884)
private val SecondaryGreen = Color(0xFF005E4C)
private val DarkText = Color(0xFF1C2D2A)

@Composable
fun HomeScreen(
    onNavigateToStatus: (PlatformType) -> Unit,
    onNavigateToDirectChat: () -> Unit,
    onNavigateToSavedFiles: () -> Unit,
    onNavigateToReminder: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onNavigateToReminder() else showPermissionDialog = true
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun checkAndNavigateToReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val needsExactAlarmPerm = !alarmManager.canScheduleExactAlarms()
        val needsNotificationPerm =
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

        if (needsNotificationPerm) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (needsExactAlarmPerm) {
            showPermissionDialog = true
        } else {
            onNavigateToReminder()
        }
    }

    if (showPermissionDialog) {
        PermissionDialog(
            onDismiss = { showPermissionDialog = false },
            onOpenSettings = {
                showPermissionDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFFBFDFF),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            HeaderSection(onNavigateToSettings)

            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Main Platform Selection
                PlatformSection(onNavigateToStatus)

                // Tools Section
                ToolsSection(
                    onDirectChat = onNavigateToDirectChat,
                    onSavedFiles = onNavigateToSavedFiles,
                    onReminder = { checkAndNavigateToReminder() }
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(onSettingsClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(PrimaryGreen, SecondaryGreen)
                ),
                shape = RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp)
            )
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 30.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                "Status Saver",
                modifier = Modifier.offset(x = 10.dp, y = 10.dp),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Keep your favorite stories forever",
                modifier = Modifier.offset(x = 10.dp,y = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 15.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(painter = painterResource(id = R.drawable.setting_icon),
                contentDescription = "Settings",
                modifier = Modifier.size(25.dp),
                tint = Color.White)
        }
    }
}

@Composable
private fun PlatformSection(onPlatformClick: (PlatformType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(title = "Choose Platform")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PlatformCard(
                title = "WhatsApp",
                subtitle = "Regular",
                icon = ImageVector.vectorResource(id = R.drawable.double_chat_icon),
                gradient = listOf(Color(0xFF25D366), Color(0xFF075E54)),
                modifier = Modifier.weight(1f),
                onClick = { onPlatformClick(PlatformType.WHATSAPP) }
            )
            PlatformCard(
                title = "Business",
                subtitle = "Professional",
                icon = ImageVector.vectorResource(id = R.drawable.briefcase_icon),
                gradient = listOf(Color(0xFF34B7F1), Color(0xFF075E54)),
                modifier = Modifier.weight(1f),
                onClick = { onPlatformClick(PlatformType.WHATSAPP_BUSINESS) }
            )
        }
    }
}

@Composable
private fun ToolsSection(
    onDirectChat: () -> Unit,
    onSavedFiles: () -> Unit,
    onReminder: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(title = "Premium Tools")
        
        ToolRow(
            title = "Direct Chat",
            description = "Message without saving number",
            icon = ImageVector.vectorResource(id = R.drawable.navigate_icon),
            iconContainerColor = Color(0xFFE8F5E9),
            iconTint = PrimaryGreen,
            onClick = onDirectChat
        )

        ToolRow(
            title = "Saved Gallery",
            description = "Manage your downloaded media",
            icon = ImageVector.vectorResource(id = R.drawable.picture_icon),
            iconContainerColor = Color(0xFFFFF3E0),
            iconTint = Color(0xFFFF9800),
            onClick = onSavedFiles
        )

        ToolRow(
            title = "Daily Reminder",
            description = "Get notified for status updates",
            icon = ImageVector.vectorResource(id = R.drawable.alarm_clock_icon),
            iconContainerColor = Color(0xFFE3F2FD),
            iconTint = Color(0xFF2196F3),
            onClick = onReminder
        )
    }
}

@Composable
private fun PlatformCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(180.dp),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradient))
                .padding(20.dp)
        ) {
            Column {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(title, fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color.White)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(subtitle, fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f))
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_long_right_icon),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolRow(
    title: String,
    description: String,
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null,
                    tint = iconTint, modifier = Modifier.size(26.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold,
                    fontSize = 16.sp, color = DarkText)
                Text(
                    description,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                painter = painterResource(id = R.drawable.arrow_long_right_icon),
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 1f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = DarkText,
        modifier = Modifier.padding(start = 8.dp)
    )
}

@Composable
private fun PermissionDialog(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permissions Needed", fontWeight = FontWeight.Bold) },
        text = { Text("To set reminders, we need notification and alarm permissions. Please enable them in settings.") },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Settings", color = PrimaryGreen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later", color = Color.Gray)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    WhatsAppStatusSaverTheme {
        HomeScreen({}, {}, {}, {}, {})
    }
}
