package com.example.whatsappstatussaver.ui.saved

import android.content.Intent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.whatsappstatussaver.R
import com.example.whatsappstatussaver.data.models.MediaType
import com.example.whatsappstatussaver.data.models.PlatformType
import com.example.whatsappstatussaver.data.models.StatusMedia
import com.example.whatsappstatussaver.theme.WhatsAppStatusSaverTheme

private val AppTeal = Color(0xFF00897B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedFilesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (String, MediaType, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavedFilesViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    SavedFilesContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToViewer = onNavigateToViewer,
        onClearSelection = { viewModel.clearSelection() },
        onDeleteSelectedFiles = { viewModel.deleteSelectedFiles() },
        onDeleteFile = { viewModel.deleteFile(it) },
        onToggleSelection = { viewModel.toggleSelection(it) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedFilesContent(
    uiState: SavedFilesUiState,
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (String, MediaType, String) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelectedFiles: () -> Unit,
    onDeleteFile: (StatusMedia) -> Unit,
    onToggleSelection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current

    val isMultiSelectMode = uiState.isMultiSelectMode
    val selectedMedia = uiState.selectedMedia

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isMultiSelectMode) "${selectedMedia.size} Selected" else "Saved Files",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    if (isMultiSelectMode) {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = AppTeal)
                        }
                    } else {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE0F2F1))
                        ) {
                            Icon(Icons.Default.ArrowBackIosNew,
                                contentDescription = "Back", tint = AppTeal,
                                modifier = Modifier.size(18.dp))
                        }
                    }
                },
                actions = {
                    if (isMultiSelectMode) {
                        IconButton(onClick = {
                            val urisToShare = uiState.media
                                .filter { selectedMedia.contains(it.uri.toString()) }
                                .map { it.uri }
                                .let { ArrayList(it) }

                            if (urisToShare.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = "*/*"
                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, urisToShare)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Media"))
                            }
                            onClearSelection()
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = AppTeal)
                        }
                        IconButton(onClick = onDeleteSelectedFiles) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
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
            if (!isMultiSelectMode) {
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

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AppTeal)
                } else {
                    val mediaList = if (selectedTab == 0) {
                        uiState.media.filter { it.type == MediaType.IMAGE }
                    } else {
                        uiState.media.filter { it.type == MediaType.VIDEO }
                    }

                    if (mediaList.isEmpty()) {
                        val emptyText = if (selectedTab == 0) "No Saved Photos" else "No Saved Videos"
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = emptyText, color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            contentPadding = PaddingValues(5.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(mediaList) { media ->
                                SavedMediaItem(
                                    media = media,
                                    isSelected = selectedMedia.contains(media.uri.toString()),
                                    isMultiSelectMode = isMultiSelectMode,
                                    onDelete = { onDeleteFile(media) },
                                    onToggleSelection = { onToggleSelection(media.uri.toString()) },
                                    onClick = {
                                        if (isMultiSelectMode) {
                                            onToggleSelection(media.uri.toString())
                                        } else {
                                            onNavigateToViewer(media.uri.toString(), media.type, media.name)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavedMediaItem(
    media: StatusMedia,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onDelete: () -> Unit,
    onToggleSelection: () -> Unit,
    onClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete File") },
            text = { Text("Are you sure you want to delete this file permanently?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppTeal)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onToggleSelection
                    )
            ) {
                // High Quality Thumbnail Fix
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

                if (media.type == MediaType.VIDEO) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(36.dp).align(Alignment.Center)
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (!isMultiSelectMode) showDeleteDialog = true }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.recycle_bin_icon),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Delete",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Saved Files Screen")
@Composable
fun SavedFilesScreenPreview() {
    WhatsAppStatusSaverTheme {
        SavedFilesContent(
            uiState = SavedFilesUiState(
                isLoading = false,
                media = listOf(
                    StatusMedia(android.net.Uri.EMPTY, "Saved 1", MediaType.IMAGE, 1024, 0, PlatformType.WHATSAPP),
                    StatusMedia(android.net.Uri.EMPTY, "Saved 2", MediaType.VIDEO, 2048, 0, PlatformType.WHATSAPP, isFavorite = true),
                    StatusMedia(android.net.Uri.EMPTY, "Saved 3", MediaType.IMAGE, 512, 0, PlatformType.WHATSAPP)
                ),
                totalStorageUsed = 3584,
                searchQuery = ""
            ),
            onNavigateBack = {},
            onNavigateToViewer = { _, _, _ -> },
            onClearSelection = {},
            onDeleteSelectedFiles = {},
            onDeleteFile = {},
            onToggleSelection = {}
        )
    }
}

@Preview(showBackground = true, name = "Media Item - Saved Image")
@Composable
fun SavedMediaItemImagePreview() {
    WhatsAppStatusSaverTheme {
        Box(modifier = Modifier.padding(16.dp).size(120.dp)) {
            SavedMediaItem(
                media = StatusMedia(android.net.Uri.EMPTY, "img.jpg", MediaType.IMAGE, 0, 0, PlatformType.WHATSAPP),
                isSelected = false,
                isMultiSelectMode = false,
                onDelete = {},
                onToggleSelection = {},
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Media Item - Saved Video")
@Composable
fun SavedMediaItemVideoPreview() {
    WhatsAppStatusSaverTheme {
        Box(modifier = Modifier.padding(16.dp).size(120.dp)) {
            SavedMediaItem(
                media = StatusMedia(android.net.Uri.EMPTY, "vid.mp4", MediaType.VIDEO, 0, 0, PlatformType.WHATSAPP),
                isSelected = false,
                isMultiSelectMode = false,
                onDelete = {},
                onToggleSelection = {},
                onClick = {}
            )
        }
    }
}

