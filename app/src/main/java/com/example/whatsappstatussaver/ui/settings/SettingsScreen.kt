package com.example.whatsappstatussaver.ui.settings

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.whatsappstatussaver.theme.WhatsAppStatusSaverTheme

private val AppTeal = Color(0xFF00897B)

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
            Toast.makeText(context, "Save location updated!",
                Toast.LENGTH_SHORT).show()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            viewModel.exportMedia(uri) { success ->
                isExporting = false
                if (success) {
                    Toast.makeText(context, "Export Successful!",
                        Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Export Failed",
                        Toast.LENGTH_SHORT).show()
                }
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
            Toast.makeText(context, "Firebase Cloud Backup is a placeholder.",
                Toast.LENGTH_LONG).show()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE0F2F1))
                    ) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = AppTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF7F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsSection(title = "General") {
                SettingsRow(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Toggle dark theme for the app",
                    action = {
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = onDarkModeToggle,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White,
                                checkedTrackColor = AppTeal)
                        )
                    }
                )
            }

            SettingsSection(title = "Storage") {
                SettingsRow(
                    icon = Icons.Default.Folder,
                    title = "Custom Save Location",
                    subtitle = if (customSaveLocation != null)
                        Uri.decode(customSaveLocation)
                    else "Default (Internal Storage)",
                    onClick = onSelectFolder
                )
            }

            SettingsSection(title = "Premium Features") {
                SettingsRow(
                    icon = Icons.Default.WorkspacePremium,
                    iconColor = if (isPremium) Color(0xFFFFB300) else AppTeal,
                    title = if (isPremium) "Premium Active" else "Upgrade to Premium",
                    subtitle = if (isPremium) "Enjoying an ad-free experience"
                    else "Remove ads and unlock all features",
                    onClick = onUpgradePremium
                )
            }

            SettingsSection(title = "Data Management") {
                SettingsRow(
                    icon = Icons.Default.FileDownload,
                    title = if (isExporting) "Exporting..."
                    else "Export All Media",
                    subtitle = "Create a ZIP backup of all saved statuses",
                    onClick = onExportData,
                    enabled = !isExporting
                )
                SettingsRow(
                    icon = Icons.Default.CloudUpload,
                    title = "Cloud Backup",
                    subtitle = "Securely backup your media to Firebase",
                    onClick = onCloudBackup
                )
            }
        }
    }
}
@Composable
fun SettingsSection(title: String, content:
@Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = AppTeal,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}
@Composable
fun SettingsRow(
    icon: ImageVector,
    iconColor: Color = AppTeal,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && onClick != null)
            { onClick?.invoke() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold, color = Color(0xFF263238))
            Text(text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray)
        }
        if (action != null) {
            action()
        } else if (onClick != null) {
            Icon(Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray)
        }
    }
}

// ==================== PREVIEWS ====================

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
@Preview(showBackground = true, name = "Premium Active")
@Composable
fun SettingsScreenPremiumPreview() {
    WhatsAppStatusSaverTheme {
        SettingsContent(
            isDarkMode = true,
            isPremium = true,
            customSaveLocation = "primary:Documents/WhatsAppSaver",
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
