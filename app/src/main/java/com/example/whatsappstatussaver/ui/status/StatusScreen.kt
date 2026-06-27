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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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

private val AppTeal = Color(0xFF00897B)

@OptIn(ExperimentalMaterial3Api::class)
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

    // Storage Access Framework Launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // Persist read and write access permissions for the selected folder
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            viewModel.checkPermissionAndLoad(selectedPlatform)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val isGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
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

            // FIX: Explicit deep paths configured to point directly to the hidden .Statuses directory
            val documentId = if (selectedPlatform == PlatformType.WHATSAPP_BUSINESS) {
                "primary:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"
            } else {
                "primary:Android/media/com.whatsapp/WhatsApp/Media/.Statuses"
            }

            // Generate the strictly formatted hint URI required by OpenDocumentTree contract
            val initialHintUri = DocumentsContract.buildDocumentUri(authority, documentId)

            try {
                launcher.launch(initialHintUri)
            } catch (e: Exception) {
                // Fallback 1: If OS prevents direct structural entry to hidden folder, default to Media
                try {
                    val mediaFallbackId = if (selectedPlatform == PlatformType.WHATSAPP_BUSINESS) {
                        "primary:Android/media/com.whatsapp.w4b/WhatsApp Business/Media"
                    } else {
                        "primary:Android/media/com.whatsapp/WhatsApp/Media"
                    }
                    launcher.launch(DocumentsContract.buildDocumentUri(authority, mediaFallbackId))
                } catch (e2: Exception) {
                    // Fallback 2: Fallback to root package directory
                    try {
                        val pkgFallbackId = if (selectedPlatform == PlatformType.WHATSAPP_BUSINESS) {
                            "primary:Android/media/com.whatsapp.w4b"
                        } else {
                            "primary:Android/media/com.whatsapp"
                        }
                        launcher.launch(DocumentsContract.buildDocumentUri(authority, pkgFallbackId))
                    } catch (e3: Exception) {
                        launcher.launch(null) // Final fallback to default storage root
                    }
                }
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
            TopAppBar(
                title = {
                    Text(if (selectedTab == 0) "Photos" else "Videos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp)
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE0F2F1))
                    ) {
                        Icon(Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = AppTeal,
                            modifier = Modifier.size(18.dp))
                    }
                },
                actions = {
                    if (uiState.isMultiSelectMode) {
                        IconButton(onClick = onSaveSelected) {
                            Icon(Icons.Default.Download, contentDescription = "Save Selected", tint = AppTeal)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            if (uiState.downloadProgress != null) {
                LinearProgressIndicator(
                    progress = { uiState.downloadProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = AppTeal,
                    trackColor = Color(0xFFE0F2F1)
                )
            }

            if (!uiState.isMultiSelectMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(44.dp)
                        .background(Color(0xFFE0F2F1), RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlatformButton(
                        title = "WhatsApp",
                        selected = selectedPlatform == PlatformType.WHATSAPP,
                        onClick = { onPlatformChange(PlatformType.WHATSAPP) },
                        modifier = Modifier.weight(1f)
                    )
                    PlatformButton(
                        title = "WhatsApp Business",
                        selected = selectedPlatform == PlatformType.WHATSAPP_BUSINESS,
                        onClick = { onPlatformChange(PlatformType.WHATSAPP_BUSINESS) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Media Type Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = AppTeal,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = AppTeal
                        )
                    },
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Photos", fontWeight = if(selectedTab == 0) FontWeight.Bold else FontWeight.Medium) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Videos", fontWeight = if(selectedTab == 1) FontWeight.Bold else FontWeight.Medium) }
                    )
                }
            }

            // Content Container
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AppTeal)
                } else if (!uiState.isAppInstalled) {
                    val appName = if (selectedPlatform == PlatformType.WHATSAPP) "WhatsApp" else "WhatsApp Business"
                    EmptyState(
                        message = "$appName is not installed on your device.",
                        buttonText = "Refresh",
                        onRefresh = onRefresh
                    )
                } else if (!uiState.hasPermission) {
                    PermissionGuide(
                        platform = selectedPlatform,
                        onGrant = onGrantPermission
                    )
                } else if (filteredStatuses.isEmpty()) {
                    EmptyState(
                        message = "No ${if (selectedTab == 0) "Photos" else "Videos"} found.",
                        buttonText = "Refresh",
                        onRefresh = onRefresh
                    )
                } else {
                    MediaGrid(
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

@Composable
fun PermissionGuide(platform: PlatformType, onGrant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.FolderSpecial, contentDescription = null, modifier = Modifier.size(64.dp), tint = AppTeal)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Storage Permission Needed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onGrant,
            colors = ButtonDefaults.buttonColors(containerColor = AppTeal),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Grant Permission", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PlatformButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) AppTeal else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) Color.White else AppTeal,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
fun MediaGrid(
    mediaList: List<StatusMedia>,
    selectedMedia: Set<String>,
    isMultiSelectMode: Boolean,
    onSave: (StatusMedia) -> Unit,
    onToggleSelection: (StatusMedia) -> Unit,
    onNavigateToViewer: (String, MediaType, String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(mediaList) { media ->
            MediaItem(
                media = media,
                isSelected = selectedMedia.contains(media.uri.toString()),
                isMultiSelectMode = isMultiSelectMode,
                onSave = { onSave(media) },
                onToggleSelection = { onToggleSelection(media) },
                onClick = {
                    if (isMultiSelectMode) {
                        onToggleSelection(media)
                    } else {
                        onNavigateToViewer(media.uri.toString(), media.type, media.name)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaItem(
    media: StatusMedia,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onSave: () -> Unit,
    onToggleSelection: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE0E0E0))
    ) {
        // Main Image/Video Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onToggleSelection
                )
        ) {
            if (media.type == MediaType.VIDEO) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(media.uri)
                        .decoderFactory(VideoFrameDecoder.Factory())
                        .videoFrameMillis(1000)
                        .crossfade(true)
                        .size(512)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(36.dp).align(Alignment.Center)
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(media.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable { onToggleSelection() }
            )
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = AppTeal,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        } else {
            if (!isMultiSelectMode) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Quick Save Button
                    IconButton(
                        onClick = {
                            Log.d("StatusScreen", "Grid Save Clicked")
                            Toast.makeText(context, "Saving...", Toast.LENGTH_SHORT).show()
                            onSave()
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(32.dp)
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
}

@Composable
fun EmptyState(
    message: String,
    buttonText: String,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = Color.DarkGray,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRefresh,
            colors = ButtonDefaults.buttonColors(containerColor = AppTeal),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.height(44.dp).padding(horizontal = 32.dp)
        ) {
            Text(buttonText, fontWeight = FontWeight.Bold)
        }
    }
}

// ==================== PREVIEWS ====================

@ExperimentalMaterial3Api
@Preview(showBackground = true, name = "Full Screen - Data")
@Composable
fun StatusScreenDataPreview() {
    MaterialTheme {
        StatusScreenContent(
            uiState = StatusUiState(
                statuses = listOf(
                    StatusMedia(Uri.EMPTY, "status1.jpg", MediaType.IMAGE, 1024, 0, PlatformType.WHATSAPP),
                    StatusMedia(Uri.EMPTY, "status2.mp4", MediaType.VIDEO, 2048, 0, PlatformType.WHATSAPP),
                    StatusMedia(Uri.EMPTY, "status3.jpg", MediaType.IMAGE, 1024, 0, PlatformType.WHATSAPP)
                ),
                hasPermission = true
            ),
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

@ExperimentalMaterial3Api
@Preview(showBackground = true, name = "Full Screen - Loading")
@Composable
fun StatusScreenLoadingPreview() {
    MaterialTheme {
        StatusScreenContent(
            uiState = StatusUiState(isLoading = true),
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

@ExperimentalMaterial3Api
@Preview(showBackground = true, name = "Full Screen - No Permission")
@Composable
fun StatusScreenNoPermissionPreview() {
    MaterialTheme {
        StatusScreenContent(
            uiState = StatusUiState(hasPermission = false),
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

@Preview(showBackground = true, name = "Permission Guide")
@Composable
fun PermissionGuidePreview() {
    MaterialTheme {
        PermissionGuide(platform = PlatformType.WHATSAPP, onGrant = {})
    }
}

@Preview(showBackground = true, name = "Platform Buttons")
@Composable
fun PlatformButtonsPreview() {
    MaterialTheme {
        Row(modifier = Modifier.padding(16.dp).height(44.dp)) {
            PlatformButton(title = "WhatsApp", selected = true, onClick = {}, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            PlatformButton(title = "Business", selected = false, onClick = {}, modifier = Modifier.weight(1f))
        }
    }
}

@Preview(showBackground = true, name = "Media Item - Image")
@Composable
fun MediaItemImagePreview() {
    MaterialTheme {
        Box(modifier = Modifier.size(120.dp).padding(8.dp)) {
            MediaItem(
                media = StatusMedia(Uri.EMPTY, "img.jpg", MediaType.IMAGE, 0, 0, PlatformType.WHATSAPP),
                isSelected = false,
                isMultiSelectMode = false,
                onSave = {},
                onToggleSelection = {},
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Media Item - Video")
@Composable
fun MediaItemVideoPreview() {
    MaterialTheme {
        Box(modifier = Modifier.size(120.dp).padding(8.dp)) {
            MediaItem(
                media = StatusMedia(Uri.EMPTY, "vid.mp4", MediaType.VIDEO, 0, 0, PlatformType.WHATSAPP),
                isSelected = false,
                isMultiSelectMode = false,
                onSave = {},
                onToggleSelection = {},
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Media Item - Selected")
@Composable
fun MediaItemSelectedPreview() {
    MaterialTheme {
        Box(modifier = Modifier.size(120.dp).padding(8.dp)) {
            MediaItem(
                media = StatusMedia(Uri.EMPTY, "img.jpg", MediaType.IMAGE, 0, 0, PlatformType.WHATSAPP),
                isSelected = true,
                isMultiSelectMode = true,
                onSave = {},
                onToggleSelection = {},
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Media Grid")
@Composable
fun MediaGridPreview() {
    MaterialTheme {
        MediaGrid(
            mediaList = listOf(
                StatusMedia(Uri.EMPTY, "1.jpg", MediaType.IMAGE, 0, 0, PlatformType.WHATSAPP),
                StatusMedia(Uri.EMPTY, "2.mp4", MediaType.VIDEO, 0, 0, PlatformType.WHATSAPP),
                StatusMedia(Uri.EMPTY, "3.jpg", MediaType.IMAGE, 0, 0, PlatformType.WHATSAPP),
                StatusMedia(Uri.EMPTY, "4.mp4", MediaType.VIDEO, 0, 0, PlatformType.WHATSAPP)
            ),
            selectedMedia = emptySet(),
            isMultiSelectMode = false,
            onSave = {},
            onToggleSelection = {},
            onNavigateToViewer = { _, _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "Empty State")
@Composable
fun EmptyStatePreview() {
    MaterialTheme {
        EmptyState(message = "No Photos found.", buttonText = "Refresh", onRefresh = {})
    }
}