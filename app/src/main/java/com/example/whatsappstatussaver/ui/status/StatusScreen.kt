package com.example.whatsappstatussaver.ui.status

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.whatsappstatussaver.R
import com.example.whatsappstatussaver.data.models.MediaType
import com.example.whatsappstatussaver.data.models.PlatformType
import com.example.whatsappstatussaver.data.models.StatusMedia
import com.example.whatsappstatussaver.theme.WhatsAppStatusSaverTheme

private val PrimaryGreen = Color(0xFF00A884)
private val SecondaryGreen = Color(0xFF005E4C)
private val SoftGreen = Color(0xFFE7FFFA)
private val DarkText = Color(0xFF1C2D2A)

@Composable
fun StatusScreen(
    initialPlatform: PlatformType,
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (String, MediaType, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatusViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedPlatform by rememberSaveable { mutableStateOf(initialPlatform) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            viewModel.checkPermissionAndLoad(selectedPlatform)
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissionAndLoad(selectedPlatform)
            }
        })
    }

    LaunchedEffect(selectedPlatform) {
        viewModel.checkPermissionAndLoad(selectedPlatform)
    }

    StatusScreenContent(
        uiState = uiState,
        selectedPlatform = selectedPlatform,
        onPlatformChange = {
            selectedPlatform = it
            viewModel.checkPermissionAndLoad(it)
        },
        onNavigateBack = onNavigateBack,
        onSaveSelected = { viewModel.saveSelectedMedia() },
        onSaveSingle = { viewModel.saveMedia(it) },
        onToggleSelection = { viewModel.toggleSelection(it.uri.toString()) },
        onNavigateToViewer = onNavigateToViewer,
        onGrantPermission = {
            val authority = "com.android.externalstorage.documents"
            val documentId = if (selectedPlatform == PlatformType.WHATSAPP_BUSINESS) {
                "primary:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"
            } else {
                "primary:Android/media/com.whatsapp/WhatsApp/Media/.Statuses"
            }
            val initialHintUri = DocumentsContract.buildDocumentUri(authority, documentId)
            try {
                launcher.launch(initialHintUri)
            } catch (_: Exception) {
                launcher.launch(null)
            }
        },
        onRefresh = { viewModel.checkPermissionAndLoad(selectedPlatform) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreenContent(
    uiState: StatusUiState,
    selectedPlatform: PlatformType,
    onPlatformChange: (PlatformType) -> Unit,
    onNavigateBack: () -> Unit,
    onSaveSelected: () -> Unit,
    onSaveSingle: (StatusMedia) -> Unit,
    onToggleSelection: (StatusMedia) -> Unit,
    onNavigateToViewer: (String, MediaType, String) -> Unit,
    onGrantPermission: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val filteredStatuses = uiState.statuses.filter {
        if (selectedTab == 0) it.type == MediaType.IMAGE else it.type == MediaType.VIDEO
    }

    Scaffold(
        topBar = {
            StatusTopBar(
                title = if (uiState.isMultiSelectMode) "${uiState.selectedMedia.size} Selected" 
                        else if (selectedTab == 0) "Photos" else "Videos",
                onBack = onNavigateBack,
                isMultiSelect = uiState.isMultiSelectMode,
                onSaveSelected = onSaveSelected
            )
        },
        containerColor = Color(0xFFFBFDFF)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.downloadProgress != null) {
                LinearProgressIndicator(
                    progress = { uiState.downloadProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = PrimaryGreen,
                    trackColor = SoftGreen
                )
            }

            if (!uiState.isMultiSelectMode) {
                PlatformSwitcher(
                    selectedPlatform = selectedPlatform,
                    onPlatformChange = onPlatformChange
                )

                MediaTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center), 
                        color = PrimaryGreen
                    )
                    !uiState.isAppInstalled -> StatusEmptyState(
                        message = "${if (selectedPlatform == PlatformType.WHATSAPP) "WhatsApp" else "Business"} not installed.",
                        onAction = onRefresh,
                        buttonText = "Refresh"
                    )
                    !uiState.hasPermission -> StatusPermissionGuide(onGrantPermission)
                    filteredStatuses.isEmpty() -> StatusEmptyState(
                        message = "No ${if (selectedTab == 0) "Photos" else "Videos"} found.",
                        onAction = onRefresh,
                        buttonText = "Check Again"
                    )
                    else -> MediaGrid(
                        mediaList = filteredStatuses,
                        selectedMedia = uiState.selectedMedia,
                        isMultiSelectMode = uiState.isMultiSelectMode,
                        onSave = onSaveSingle,
                        onToggleSelection = onToggleSelection,
                        onNavigateToViewer = onNavigateToViewer
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusTopBar(
    title: String,
    onBack: () -> Unit,
    isMultiSelect: Boolean,
    onSaveSelected: () -> Unit
) {
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
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (isMultiSelect) {
            IconButton(
                onClick = onSaveSelected,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(Icons.Default.Download, contentDescription = "Save All", tint = Color.White)
            }
        }
    }
}

@Composable
private fun PlatformSwitcher(
    selectedPlatform: PlatformType,
    onPlatformChange: (PlatformType) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlatformToggle(
                title = "WhatsApp",
                isSelected = selectedPlatform == PlatformType.WHATSAPP,
                onClick = { onPlatformChange(PlatformType.WHATSAPP) },
                modifier = Modifier.weight(1f)
            )
            PlatformToggle(
                title = "Business",
                isSelected = selectedPlatform == PlatformType.WHATSAPP_BUSINESS,
                onClick = { onPlatformChange(PlatformType.WHATSAPP_BUSINESS) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PlatformToggle(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) PrimaryGreen else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else DarkText,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun MediaTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        TabItem(label = "Photos", isSelected = selectedTab == 0, onClick = { onTabSelected(0) })
        TabItem(label = "Videos", isSelected = selectedTab == 1, onClick = { onTabSelected(1) })
    }
}

@Composable
private fun TabItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isSelected) PrimaryGreen else Color.Gray
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen)
            )
        }
    }
}

@Composable
private fun MediaGrid(
    mediaList: List<StatusMedia>,
    selectedMedia: Set<String>,
    isMultiSelectMode: Boolean,
    onSave: (StatusMedia) -> Unit,
    onToggleSelection: (StatusMedia) -> Unit,
    onNavigateToViewer: (String, MediaType, String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(mediaList) { media ->
            StatusMediaItem(
                media = media,
                isSelected = selectedMedia.contains(media.uri.toString()),
                isMultiSelectMode = isMultiSelectMode,
                onSave = { onSave(media) },
                onToggleSelection = { onToggleSelection(media) },
                onClick = {
                    if (isMultiSelectMode) onToggleSelection(media)
                    else onNavigateToViewer(media.uri.toString(), media.type, media.name)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StatusMediaItem(
    media: StatusMedia,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onSave: () -> Unit,
    onToggleSelection: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(24.dp)),
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(onClick = onClick, onLongClick = onToggleSelection)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(media.uri)
                    .decoderFactory(VideoFrameDecoder.Factory())
                    .videoFrameMillis(1000)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (media.type == MediaType.VIDEO) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PrimaryGreen.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            } else if (!isMultiSelectMode) {
                IconButton(
                    onClick = onSave,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.import_icon),
                        contentDescription = "Save",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusEmptyState(message: String, onAction: () -> Unit, buttonText: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message, textAlign = TextAlign.Center, color = Color.Gray, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(buttonText, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusPermissionGuide(onGrant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(100.dp).background(SoftGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(48.dp), tint = PrimaryGreen)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Access Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = DarkText
        )
        Text(
            "We need permission to access WhatsApp status folder to show you photos and videos.",
            textAlign = TextAlign.Center,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onGrant,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Grant Permission", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatusScreenPreview() {
    WhatsAppStatusSaverTheme {
        StatusScreenContent(
            uiState = StatusUiState(hasPermission = true),
            selectedPlatform = PlatformType.WHATSAPP,
            onPlatformChange = {},
            onNavigateBack = {},
            onSaveSelected = {},
            onSaveSingle = {},
            onToggleSelection = {},
            onNavigateToViewer = { _, _, _ -> },
            onGrantPermission = {},
            onRefresh = {}
        )
    }
}
