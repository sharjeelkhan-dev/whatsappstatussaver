package com.sharjeel.whatsappstatussaver.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sharjeel.whatsappstatussaver.R
import com.sharjeel.whatsappstatussaver.theme.PrimaryGreen
import com.sharjeel.whatsappstatussaver.theme.SecondaryGreen
import com.sharjeel.whatsappstatussaver.theme.WhatsAppStatusSaverTheme

private val AccentGold = Color(0xFFFFD700)

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isAutoSave by viewModel.isAutoSave.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val customSaveLocation by viewModel.customSaveLocation.collectAsState()
    val context = LocalContext.current
    var isExporting by remember { mutableStateOf(false) }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            viewModel.setSaveLocation(uri)
            Toast.makeText(context, "Save location updated!", Toast.LENGTH_SHORT).show()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            viewModel.exportMedia(uri) { success ->
                isExporting = false
                if (success) Toast.makeText(context, "Export Successful!", Toast.LENGTH_SHORT).show()
                else Toast.makeText(context, "Export Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    SettingsContent(
        isDarkMode = isDarkMode,
        isAutoSave = isAutoSave,
        isPremium = isPremium,
        customSaveLocation = customSaveLocation,
        isExporting = isExporting,
        onNavigateBack = onNavigateBack,
        onDarkModeToggle = { viewModel.setDarkMode(it) },
        onAutoSaveToggle = { viewModel.setAutoSave(context, it) },
        onSelectFolder = { folderLauncher.launch(null) },
        onUpgradePremium = {
            if (!isPremium) viewModel.launchBillingFlow(context as Activity)
            else viewModel.toggleMockPremium()
        },
        onExportData = { exportLauncher.launch("WhatsApp_Statuses_Backup.zip") },
        onCloudBackup = {
            Toast.makeText(context, "Cloud Backup is coming soon!", Toast.LENGTH_SHORT).show()
        }
    )
}

@Composable
fun SettingsContent(
    isDarkMode: Boolean,
    isAutoSave: Boolean,
    isPremium: Boolean,
    customSaveLocation: String?,
    isExporting: Boolean,
    onNavigateBack: () -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onAutoSaveToggle: (Boolean) -> Unit,
    onSelectFolder: () -> Unit,
    onUpgradePremium: () -> Unit,
    onExportData: () -> Unit,
    onCloudBackup: () -> Unit
) {
    Scaffold(
        // Edge-to-edge ke liye contentWindowInsets ko zero set kiya hai taake layout khud control ho
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsTopBar(onBack = onNavigateBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
                // Navigation bars (gesture bar/buttons) ke peeche content chupne se bachane ke liye padding
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Premium Card
            PremiumStatusCard(isPremium = isPremium, onUpgrade = onUpgradePremium)

            SettingsSection(title = "General") {
                SettingsRow(
                    icon = Icons.Default.DarkMode,
                    iconTint = Color(0xFF7C4DFF),
                    title = "Dark Mode",
                    subtitle = "Switch between light and dark theme",
                    action = {
                        Icon(
                            painter = painterResource(id = if (isDarkMode) R.drawable.toggle_on_line_icon else R.drawable.toggle_off_line_icon),
                            contentDescription = "Toggle Dark Mode",
                            tint = if (isDarkMode) Color(0xFF7C4DFF) else Color.Gray,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { onDarkModeToggle(!isDarkMode) }
                        )
                    }
                )
                SettingsRow(
                    icon = Icons.Default.Autorenew,
                    iconTint = Color(0xFF00A884),
                    title = "Auto Save Statuses",
                    subtitle = "Automatically save new statuses in background",
                    action = {
                        Icon(
                            painter = painterResource(id = if (isAutoSave) R.drawable.toggle_on_line_icon else R.drawable.toggle_off_line_icon),
                            contentDescription = "Toggle Auto Save",
                            tint = if (isAutoSave) PrimaryGreen else Color.Gray,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { onAutoSaveToggle(!isAutoSave) }
                        )
                    }
                )
            }

            SettingsSection(title = "Storage") {
                SettingsRow(
                    icon = Icons.Default.Folder,
                    iconTint = Color(0xFFFBC02D),
                    title = "Save Location",
                    subtitle = if (customSaveLocation != null) Uri.decode(customSaveLocation) else "Default Storage",
                    onClick = onSelectFolder
                )
            }

            SettingsSection(title = "Data Management") {
                SettingsRow(
                    icon = Icons.Default.CloudDownload,
                    iconTint = Color(0xFF2196F3),
                    title = if (isExporting) "Exporting..." else "Export All Media",
                    subtitle = "Backup all saved statuses as a ZIP file",
                    onClick = onExportData,
                    enabled = !isExporting
                )
                SettingsRow(
                    icon = Icons.Default.CloudUpload,
                    iconTint = Color(0xFF00A884),
                    title = "Cloud Sync",
                    subtitle = "Sync your media to the cloud",
                    onClick = onCloudBackup
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(listOf(PrimaryGreen, SecondaryGreen)),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .offset(y = (-5).dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Settings",
                modifier = Modifier.offset(y = (-5).dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun PremiumStatusCard(isPremium: Boolean, onUpgrade: () -> Unit) {
    Card(
        onClick = onUpgrade,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPremium) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(if (isPremium) Color.White.copy(alpha = 0.2f) else AccentGold.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = if (isPremium) Color.White else AccentGold,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPremium) "Premium Member" else "Upgrade to Premium",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isPremium) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isPremium) "Thank you for supporting us!"
                    else "Remove ads and unlock all tools",
                    fontSize = 13.sp,
                    color = if (isPremium) Color.White.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isPremium) {
                Icon(Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && onClick != null) { onClick?.invoke() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(iconTint.copy(alpha = 0.15f),
                    RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
            Text(text = subtitle, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (action != null) {
            action()
        } else if (onClick != null) {
            Icon(Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    WhatsAppStatusSaverTheme {
        SettingsContent(
            isDarkMode = false,
            isAutoSave = false,
            isPremium = false,
            customSaveLocation = null,
            isExporting = false,
            onNavigateBack = {},
            onDarkModeToggle = {},
            onAutoSaveToggle = {},
            onSelectFolder = {},
            onUpgradePremium = {},
            onExportData = {},
            onCloudBackup = {}
        )
    }
}