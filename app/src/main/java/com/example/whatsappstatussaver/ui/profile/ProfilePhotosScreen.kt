package com.example.whatsappstatussaver.ui.profile

import android.content.Intent
import android.provider.DocumentsContract
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import coil.request.ImageRequest
import com.example.whatsappstatussaver.R
import com.example.whatsappstatussaver.data.models.MediaType
import com.example.whatsappstatussaver.data.models.PlatformType
import com.example.whatsappstatussaver.data.models.StatusMedia
import com.example.whatsappstatussaver.theme.WhatsAppStatusSaverTheme

private val AppTeal = Color(0xFF00897B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePhotosScreen(
    initialPlatform: PlatformType,
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (String, MediaType, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedPlatform by remember { mutableStateOf(initialPlatform) }

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

    ProfilePhotosScreenContent(
        uiState = uiState,
        selectedPlatform = selectedPlatform,
        onPlatformChange = {
            selectedPlatform = it
            viewModel.checkPermissionAndLoad(it)
        },
        onNavigateBack = onNavigateBack,
        onSaveSelected = {
            viewModel.saveSelectedMedia()
            Toast.makeText(context, "Saving selected photos...", Toast.LENGTH_SHORT).show()
        },
        onSaveSingle = {
            viewModel.saveMedia(it)
            Toast.makeText(context, "Photo saved to gallery!", Toast.LENGTH_SHORT).show()
        },
        onToggleSelection = { viewModel.toggleSelection(it.uri.toString()) },
        onNavigateToViewer = onNavigateToViewer,
        onGrantPermission = {
            val authority = "com.android.externalstorage.documents"
            
            // Updated deep paths to point directly to the Profile Photos directory
            val documentId = if (selectedPlatform == PlatformType.WHATSAPP_BUSINESS) {
                "primary:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Profile Photos"
            } else {
                "primary:Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Profile Photos"
            }
            val initialHintUri = DocumentsContract.buildDocumentUri(authority, documentId)

            try {
                launcher.launch(initialHintUri)
            } catch (e: Exception) {
                // Fallback 1: Media directory
                try {
                    val mediaFallbackId = if (selectedPlatform == PlatformType.WHATSAPP_BUSINESS) {
                        "primary:Android/media/com.whatsapp.w4b/WhatsApp Business/Media"
                    } else {
                        "primary:Android/media/com.whatsapp/WhatsApp/Media"
                    }
                    launcher.launch(DocumentsContract.buildDocumentUri(authority, mediaFallbackId))
                } catch (e2: Exception) {
                    // Fallback 2: Package root
                    try {
                        val pkgFallbackId = if (selectedPlatform == PlatformType.WHATSAPP_BUSINESS) {
                            "primary:Android/media/com.whatsapp.w4b"
                        } else {
                            "primary:Android/media/com.whatsapp"
                        }
                        launcher.launch(DocumentsContract.buildDocumentUri(authority, pkgFallbackId))
                    } catch (e3: Exception) {
                        launcher.launch(null) // System default
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
fun ProfilePhotosScreenContent(
    uiState: ProfileUiState,
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Profile Photos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp)
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(36.dp)
                            .background(Color(0xFFE0F2F1), RoundedCornerShape(8.dp))
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
            }

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
                } else if (uiState.photos.isEmpty()) {
                    EmptyState(
                        message = "No Profile Photos found",
                        buttonText = "Refresh",
                        onRefresh = onRefresh
                    )
                } else {
                    MediaGrid(
                        mediaList = uiState.photos,
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
        Icon(Icons.Default.FolderSpecial,
            contentDescription = null, modifier = Modifier.size(64.dp),
            tint = AppTeal)
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

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE0E0E0))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onToggleSelection
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(media.uri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(if (isSelected) 4.dp else 8.dp)),
            contentScale = ContentScale.Crop
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
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
                IconButton(
                    onClick = onSave,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        .size(28.dp)
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
            Text(buttonText,
                fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyStatePreview() {
    WhatsAppStatusSaverTheme {
        EmptyState(
            message = "No Profile Photos found.",
            buttonText = "Refresh",
            onRefresh = {}
        )
    }
}
