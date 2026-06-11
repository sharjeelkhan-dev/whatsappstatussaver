@file:kotlin.OptIn(ExperimentalMaterial3Api::class)

package com.example.whatsappstatussaver.ui.viewer

import android.content.Intent
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.whatsappstatussaver.data.models.MediaType
import com.example.whatsappstatussaver.data.models.PlatformType
import com.example.whatsappstatussaver.data.models.StatusMedia
import com.example.whatsappstatussaver.ui.status.StatusViewModel
import kotlinx.coroutines.launch
import java.io.File

private val AppTeal = Color(0xFF00897B)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    statusMedia: StatusMedia,
    onNavigateBack: () -> Unit,
    onTagUpdate: ((String) -> Unit)? = null,
    onCompressVideo: (suspend (android.net.Uri) -> android.net.Uri?)? = null,
    modifier: Modifier = Modifier,
    viewModel: StatusViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCompressing by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showPlatformDialog by remember { mutableStateOf(false) }
    var pendingShareAction by remember { mutableStateOf<((PlatformType) -> Unit)?>(null) }
    var tagInput by remember { mutableStateOf(statusMedia.tags) }

    fun checkAndShare(action: (PlatformType) -> Unit) {
        val isWhatsappInstalled = try {
            context.packageManager.getPackageInfo("com.whatsapp", 0)
            true
        } catch (e: Exception) {
            false
        }
        val isBusinessInstalled = try {
            context.packageManager.getPackageInfo("com.whatsapp.w4b", 0)
            true
        } catch (e: Exception) {
            false
        }

        when {
            isWhatsappInstalled && isBusinessInstalled -> {
                pendingShareAction = action
                showPlatformDialog = true
            }
            isWhatsappInstalled -> action(PlatformType.WHATSAPP)
            isBusinessInstalled -> action(PlatformType.WHATSAPP_BUSINESS)
            else -> Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
        }
    }
    val exoPlayer = remember(statusMedia.uri) {
        ExoPlayer.Builder(context).build().apply {
            if (statusMedia.type == MediaType.VIDEO) {
                // Better MIME detection for SAF/MediaStore URIs
                val mimeType = if (statusMedia.uri.scheme == "content") {
                    context.contentResolver.getType(statusMedia.uri) ?: "video/mp4"
                } else {
                    val extension = MimeTypeMap.getFileExtensionFromUrl(statusMedia.uri.toString())
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "video/mp4"
                }

                val mediaItem = MediaItem.Builder()
                    .setUri(statusMedia.uri)
                    .setMimeType(mimeType)
                    .build()
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_ONE
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (statusMedia.type == MediaType.VIDEO) "Video" else "Photo", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 16.dp, top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // WhatsApp Share Button (Direct Message)
                IconButton(
                    onClick = {
                        val shareAction: (PlatformType) -> Unit = { platform ->
                            coroutineScope.launch {
                                val packageName = if (platform == PlatformType.WHATSAPP) "com.whatsapp" else "com.whatsapp.w4b"
                                try {
                                    var finalUri = statusMedia.uri
                                    if (statusMedia.type == MediaType.VIDEO && onCompressVideo != null) {
                                        isCompressing = true
                                        onCompressVideo(statusMedia.uri)?.let { finalUri = it }
                                        isCompressing = false
                                    }

                                    val contentUri = if (finalUri.scheme == "file") {
                                        val file = File(finalUri.path ?: "")
                                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    } else finalUri

                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = if (statusMedia.type == MediaType.VIDEO) "video/*" else "image/*"
                                        putExtra(Intent.EXTRA_STREAM, contentUri)
                                        setPackage(packageName)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(shareIntent)
                                } catch (e: Exception) {
                                    isCompressing = false
                                    Toast.makeText(context, "Share failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        checkAndShare(shareAction)
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(AppTeal, CircleShape)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "Share on WhatsApp", tint = Color.White)
                }

                // General Share Button
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                var finalUri = statusMedia.uri
                                if (statusMedia.type == MediaType.VIDEO && onCompressVideo != null) {
                                    isCompressing = true
                                    onCompressVideo(statusMedia.uri)?.let { finalUri = it }
                                    isCompressing = false
                                }

                                val contentUri = if (finalUri.scheme == "file") {
                                    val file = File(finalUri.path ?: "")
                                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                } else finalUri

                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = if (statusMedia.type == MediaType.VIDEO) "video/*" else "image/*"
                                    putExtra(Intent.EXTRA_STREAM, contentUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                            } catch (e: Exception) {
                                isCompressing = false
                                Toast.makeText(context, "Failed to share", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(AppTeal, CircleShape)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                }

                // Save Button
                IconButton(
                    onClick = {
                        viewModel.saveMedia(statusMedia)
                        Toast.makeText(context, "Saved Successfully!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(AppTeal, CircleShape)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White)
                }

                // Repost Button
                IconButton(
                    onClick = {
                        val repostAction: (PlatformType) -> Unit = { platform ->
                            coroutineScope.launch {
                                val packageName = if (platform == PlatformType.WHATSAPP) "com.whatsapp" else "com.whatsapp.w4b"
                                try {
                                    var finalUri = statusMedia.uri
                                    if (statusMedia.type == MediaType.VIDEO && onCompressVideo != null) {
                                        isCompressing = true
                                        onCompressVideo(statusMedia.uri)?.let { finalUri = it }
                                        isCompressing = false
                                    }

                                    val contentUri = if (finalUri.scheme == "file") {
                                        val file = File(finalUri.path ?: "")
                                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    } else finalUri

                                    // For reposting, we often want to target the status specifically
                                    // but ACTION_SEND with package is usually sufficient.
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = if (statusMedia.type == MediaType.VIDEO) "video/*" else "image/*"
                                        putExtra(Intent.EXTRA_STREAM, contentUri)
                                        setPackage(packageName)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(shareIntent)
                                } catch (e: Exception) {
                                    isCompressing = false
                                    Toast.makeText(context, "Repost failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        checkAndShare(repostAction)
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(AppTeal, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Repost", tint = Color.White)
                }
            }
        },
        modifier = modifier.background(Color.White)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (statusMedia.type == MediaType.VIDEO) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            setBackgroundColor(android.graphics.Color.BLACK)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                var scale by remember { mutableStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }

                AsyncImage(
                    model = statusMedia.uri,
                    contentDescription = statusMedia.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                val extraWidth = (scale - 1) * size.width
                                val extraHeight = (scale - 1) * size.height
                                val maxX = extraWidth / 2
                                val maxY = extraHeight / 2
                                offset = Offset(
                                    x = (offset.x + pan.x * scale).coerceIn(-maxX, maxX),
                                    y = (offset.y + pan.y * scale).coerceIn(-maxY, maxY)
                                )
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            }

            if (isCompressing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AppTeal)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Compressing Video...", color = Color.White)
                    }
                }
            }
        }

        if (showTagDialog) {
            AlertDialog(
                onDismissRequest = { showTagDialog = false },
                title = { Text("Tag Contact") },
                text = {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = { Text("Contact Name or Tags") },
                        placeholder = { Text("e.g. John, Funny") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        onTagUpdate?.invoke(tagInput)
                        showTagDialog = false
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTagDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showPlatformDialog) {
            AlertDialog(
                onDismissRequest = { showPlatformDialog = false },
                title = { Text("Choose WhatsApp") },
                text = { Text("Select which WhatsApp to use for this action.") },
                confirmButton = {
                    TextButton(onClick = {
                        pendingShareAction?.invoke(PlatformType.WHATSAPP)
                        showPlatformDialog = false
                    }) {
                        Text("WhatsApp")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        pendingShareAction?.invoke(PlatformType.WHATSAPP_BUSINESS)
                        showPlatformDialog = false
                    }) {
                        Text("Business")
                    }
                }
            )
        }
    }
}

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Preview(showBackground = true)
@Composable
fun MediaViewerScreenPreview() {
    MaterialTheme {
        MediaViewerScreen(
            statusMedia = StatusMedia(
                uri = android.net.Uri.EMPTY,
                name = "Sample Media",
                type = MediaType.IMAGE,
                size = 1024,
                dateModified = System.currentTimeMillis(),
                platform = PlatformType.WHATSAPP
            ),
            onNavigateBack = {}
        )
    }
}
