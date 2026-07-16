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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sharjeel.whatsappstatussaver.theme.WhatsAppStatusSaverTheme

private val PrimaryGreen = Color(0xFF00A884)
private val SecondaryGreen = Color(0xFF005E4C)
private val DarkText = Color(0xFF1C2D2A)
private val AccentGold = Color(0xFFFFD700)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
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
        isPremium = isPremium,
        customSaveLocation = customSaveLocation,
        isExporting = isExporting,
        onNavigateBack = onNavigateBack,
        onDarkModeToggle = { viewModel.setDarkMode(it) },
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
    isPremium: Boolean,
    customSaveLocation: String?,
    isExporting: Boolean,
    onNavigateBack: () -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onSelectFolder: () -> Unit,
    onUpgradePremium: () -> Unit,
    onExportData: () -> Unit,
    onCloudBackup: () -> Unit
) {
    Scaffold(
        topBar = {
            SettingsTopBar(onBack = onNavigateBack)
        },
        containerColor = Color(0xFFFBFDFF)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Premium Card
            PremiumStatusCard(isPremium = isPremium, onUpgrade = onUpgradePremium)

            SettingsSection(title = "General") {
                SettingsRow(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Switch between light and dark theme",
                    action = {
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = onDarkModeToggle,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryGreen)
                        )
                    }
                )
            }

            SettingsSection(title = "Storage") {
                SettingsRow(
                    icon = Icons.Default.Folder,
                    title = "Save Location",
                    subtitle = if (customSaveLocation != null) Uri.decode(customSaveLocation) else "Default Storage",
                    onClick = onSelectFolder
                )
            }

            SettingsSection(title = "Data Management") {
                SettingsRow(
                    icon = Icons.Default.CloudDownload,
                    title = if (isExporting) "Exporting..." else "Export All Media",
                    subtitle = "Backup all saved statuses as a ZIP file",
                    onClick = onExportData,
                    enabled = !isExporting
                )
                SettingsRow(
                    icon = Icons.Default.CloudUpload,
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
            .height(110.dp)
            .background(
                brush = Brush.verticalGradient(listOf(PrimaryGreen, SecondaryGreen)),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Settings",
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
            containerColor = if (isPremium) SecondaryGreen else Color.White
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
                    color = if (isPremium) Color.White else DarkText
                )
                Text(
                    text = if (isPremium) "Thank you for supporting us!" else "Remove ads and unlock all tools",
                    fontSize = 13.sp,
                    color = if (isPremium) Color.White.copy(alpha = 0.7f) else Color.Gray
                )
            }

            if (!isPremium) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
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
            color = DarkText,
            modifier = Modifier.padding(start = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
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
                .background(PrimaryGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(22.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, color = DarkText, fontSize = 16.sp)
            Text(text = subtitle, fontSize = 13.sp, color = Color.Gray)
        }

        if (action != null) {
            action()
        } else if (onClick != null) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    WhatsAppStatusSaverTheme {
        SettingsContent(
            isDarkMode = false,
            isPremium = false,
            customSaveLocation = null,
            isExporting = false,
            onNavigateBack = {},
            onDarkModeToggle = {},
            onSelectFolder = {},
            onUpgradePremium = {},
            onExportData = {},
            onCloudBackup = {}
        )
    }
}

