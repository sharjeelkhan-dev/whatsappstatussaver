@file:kotlin.OptIn(ExperimentalMaterial3Api::class)

package com.example.whatsappstatussaver.ui.viewer
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.whatsappstatussaver.data.models.MediaType
import com.example.whatsappstatussaver.data.models.PlatformType
import com.example.whatsappstatussaver.data.models.StatusMedia
import com.example.whatsappstatussaver.theme.WhatsAppStatusSaverTheme
import kotlinx.coroutines.launch
import java.io.File

private val AppTeal = Color(0xFF00897B)

private fun copyUriToCache(context: Context, uri: Uri): Uri {
    Log.d("MediaViewer", "Copying URI to cache: $uri")
    return try {
        if (uri.scheme == "content") {
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(uri)) ?: "bin"
            val tempFile = File(context.cacheDir, "share_temp_${System.currentTimeMillis()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val providerUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            Log.d("MediaViewer", "Content URI cached to: $providerUri")
            providerUri
        } else {
            val path = uri.path ?: uri.toString()
            val file = if (uri.scheme == "file") File(uri.path ?: "") else File(path)
            if (file.exists()) {
                val providerUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                Log.d("MediaViewer", "File URI converted to: $providerUri")
                providerUri
            } else {
                Log.w("MediaViewer", "File does not exist: $path")
                uri
            }
        }
    } catch (e: Exception) {
        Log.e("MediaViewer", "Error copying uri to cache: ${e.message}", e)
        uri
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    statusMedia: StatusMedia,
    onNavigateBack: () -> Unit,
    onSaveMedia: (StatusMedia) -> Unit,
    onTagUpdate: ((String) -> Unit)? = null,
    onCompressVideo: (suspend (android.net.Uri) -> android.net.Uri?)? = null,
    modifier: Modifier = Modifier
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
    val isInspectionMode = LocalInspectionMode.current
    val exoPlayer = remember(statusMedia.uri) {
        if (statusMedia.type == MediaType.VIDEO && !isInspectionMode) {
            ExoPlayer.Builder(context).build().apply {
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
        } else {
            null
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release()
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
                            .padding(12.dp)
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
                        Toast.makeText(context, "WhatsApp Clicked", Toast.LENGTH_SHORT).show()
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

                                    val contentUri = copyUriToCache(context, finalUri)
                                    Log.d("MediaViewer", "Sharing to WhatsApp ($packageName): $contentUri")

                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = if (statusMedia.type == MediaType.VIDEO) "video/*" else "image/*"
                                        putExtra(Intent.EXTRA_STREAM, contentUri)
                                        setPackage(packageName)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(shareIntent)
                                } catch (e: Exception) {
                                    isCompressing = false
                                    Log.e("MediaViewer", "WhatsApp Share failed: ${e.message}", e)
                                    Toast.makeText(context, "Share failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        coroutineScope.launch { checkAndShare(shareAction) }
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
                        Toast.makeText(context, "Share Clicked", Toast.LENGTH_SHORT).show()
                        coroutineScope.launch {
                            try {
                                var finalUri = statusMedia.uri
                                if (statusMedia.type == MediaType.VIDEO && onCompressVideo != null) {
                                    isCompressing = true
                                    onCompressVideo(statusMedia.uri)?.let { finalUri = it }
                                    isCompressing = false
                                }

                                val contentUri = copyUriToCache(context, finalUri)
                                Log.d("MediaViewer", "General Share: $contentUri")

                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = if (statusMedia.type == MediaType.VIDEO) "video/*" else "image/*"
                                    putExtra(Intent.EXTRA_STREAM, contentUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                            } catch (e: Exception) {
                                isCompressing = false
                                Log.e("MediaViewer", "General Share failed: ${e.message}", e)
                                Toast.makeText(context, "Failed to share: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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
                        Toast.makeText(context, "Save Clicked", Toast.LENGTH_SHORT).show()
                        try {
                            onSaveMedia(statusMedia)
                            Toast.makeText(context, "Media saved!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Log.e("MediaViewer", "Save failed: ${e.message}", e)
                            Toast.makeText(context, "Save failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(AppTeal, CircleShape)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White)
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
                if (isInspectionMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                } else {
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
                }
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
@Preview(showBackground = true, name = "Photo Viewer")
@Composable
fun MediaViewerScreenPhotoPreview() {
    WhatsAppStatusSaverTheme {
        MediaViewerScreen(
            statusMedia = StatusMedia(
                uri = Uri.EMPTY,
                name = "Sample Photo",
                type = MediaType.IMAGE,
                size = 1024,
                dateModified = System.currentTimeMillis(),
                platform = PlatformType.WHATSAPP
            ),
            onNavigateBack = {},
            onSaveMedia = {}
        )
    }
}

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Preview(showBackground = true, name = "Video Viewer")
@Composable
fun MediaViewerScreenVideoPreview() {
    WhatsAppStatusSaverTheme {
        MediaViewerScreen(
            statusMedia = StatusMedia(
                uri = Uri.EMPTY,
                name = "Sample Video",
                type = MediaType.VIDEO,
                size = 1024,
                dateModified = System.currentTimeMillis(),
                platform = PlatformType.WHATSAPP
            ),
            onNavigateBack = {},
            onSaveMedia = {}
        )
    }
}
