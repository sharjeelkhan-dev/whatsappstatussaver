@file:kotlin.OptIn(ExperimentalMaterial3Api::class)

package com.sharjeel.whatsappstatussaver.ui.viewer
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.vectorResource
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
import com.sharjeel.whatsappstatussaver.data.models.MediaType
import com.sharjeel.whatsappstatussaver.data.models.PlatformType
import com.sharjeel.whatsappstatussaver.data.models.StatusMedia
import com.sharjeel.whatsappstatussaver.theme.WhatsAppStatusSaverTheme
import kotlinx.coroutines.launch
import java.io.File
import com.sharjeel.whatsappstatussaver.R
import kotlin.time.Duration.Companion.milliseconds

private val PrimaryGreen = Color(0xFF00A884)
private val SecondaryGreen = Color(0xFF005E4C)

private fun copyUriToCache(context: Context, uri: Uri): Uri {
    return try {
        if (uri.scheme == "content") {
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(uri)) ?: "bin"
            val tempFile = File(context.cacheDir, "share_temp_${System.currentTimeMillis()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
        } else {
            val path = uri.path ?: uri.toString()
            val file = if (uri.scheme == "file") File(uri.path ?: "") else File(path)
            if (file.exists()) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                uri
            }
        }
    } catch (_: Exception) {
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
    var showPlatformDialog by remember { mutableStateOf(false) }
    var pendingShareAction by remember { mutableStateOf<((PlatformType) -> Unit)?>(null) }

    fun checkAndShare(action: (PlatformType) -> Unit) {
        val isWhatsappInstalled = try { context.packageManager.getPackageInfo("com.whatsapp", 0); true } catch (e: Exception) { false }
        val isBusinessInstalled = try { context.packageManager.getPackageInfo("com.whatsapp.w4b", 0); true } catch (e: Exception) { false }

        when {
            isWhatsappInstalled && isBusinessInstalled -> { pendingShareAction = action; showPlatformDialog = true }
            isWhatsappInstalled -> action(PlatformType.WHATSAPP)
            isBusinessInstalled -> action(PlatformType.WHATSAPP_BUSINESS)
            else -> Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
        }
    }
    
    val isInspectionMode = LocalInspectionMode.current
    val exoPlayer = remember(statusMedia.uri) {
        if (statusMedia.type == MediaType.VIDEO && !isInspectionMode) {
            ExoPlayer.Builder(context).build().apply {
                val mimeType = if (statusMedia.uri.scheme == "content") context.contentResolver.getType(statusMedia.uri) ?: "video/mp4"
                               else "video/mp4"
                setMediaItem(MediaItem.Builder().setUri(statusMedia.uri).setMimeType(mimeType).build())
                prepare()
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_ONE
            }
        } else null
    }

    var isPlaying by remember { mutableStateOf(true) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var playbackPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    // Update progress
    LaunchedEffect(isPlaying, isControlsVisible) {
        if (isPlaying && isControlsVisible) {
            while (true) {
                playbackPosition = exoPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L
                duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
                kotlinx.coroutines.delay(500.milliseconds)
            }
        }
    }

    // Auto-hide controls after 3 seconds of inactivity if playing
    LaunchedEffect(isControlsVisible, isPlaying) {
        if (isControlsVisible && isPlaying) {
            kotlinx.coroutines.delay(3000.milliseconds)
            isControlsVisible = false
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
                }
            }
        }
        exoPlayer?.addListener(listener)
        onDispose {
            exoPlayer?.removeListener(listener)
            exoPlayer?.release()
        }
    }

    Scaffold(
        topBar = { ViewerTopBar(title = if (statusMedia.type == MediaType.VIDEO) "Video" else "Photo", onBack = onNavigateBack) },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ViewerActionButton(icon = ImageVector.vectorResource(id = R.drawable.navigate_icon), label = "Send", color = PrimaryGreen) {
                        checkAndShare { platform ->
                            coroutineScope.launch {
                                val packageName = if (platform == PlatformType.WHATSAPP) "com.whatsapp" else "com.whatsapp.w4b"
                                try {
                                    var finalUri = statusMedia.uri
                                    if (statusMedia.type == MediaType.VIDEO && onCompressVideo != null) {
                                        isCompressing = true; onCompressVideo(statusMedia.uri)?.let { finalUri = it }; isCompressing = false
                                    }
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = if (statusMedia.type == MediaType.VIDEO) "video/*" else "image/*"
                                        putExtra(Intent.EXTRA_STREAM, copyUriToCache(context, finalUri))
                                        setPackage(packageName)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(shareIntent)
                                } catch (e: Exception) { isCompressing = false; Toast.makeText(context, "Share failed", Toast.LENGTH_SHORT).show() }
                            }
                        }
                    }

                    ViewerActionButton(icon = ImageVector.vectorResource(id = R.drawable.share_line_icon), label = "Share", color = Color(0xFF2196F3)) {
                        coroutineScope.launch {
                            try {
                                var finalUri = statusMedia.uri
                                if (statusMedia.type == MediaType.VIDEO && onCompressVideo != null) {
                                    isCompressing = true; onCompressVideo(statusMedia.uri)?.let { finalUri = it }; isCompressing = false
                                }
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = if (statusMedia.type == MediaType.VIDEO) "video/*" else "image/*"
                                    putExtra(Intent.EXTRA_STREAM, copyUriToCache(context, finalUri))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                            } catch (e: Exception) { isCompressing = false }
                        }
                    }

                    ViewerActionButton(icon = ImageVector.vectorResource(id = R.drawable.import_icon),
                        label = "Save", color = Color(0xFF4CAF50)) {
                        try { onSaveMedia(statusMedia); Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show() } catch (e: Exception) { }
                    }
                }
            }
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
            if (statusMedia.type == MediaType.VIDEO) {
                if (isInspectionMode) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayCircle,
                            contentDescription = null, tint = Color.White,
                            modifier = Modifier.size(64.dp))
                        PlaybackControls(
                            isPlaying = true,
                            onPlayPauseToggle = {},
                            onForward = {},
                            onBackward = {},
                            onSkipNext = {},
                            onSkipPrevious = {},
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isControlsVisible = !isControlsVisible
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                setBackgroundColor(android.graphics.Color.BLACK)
                            }
                        }, modifier = Modifier.fillMaxSize())
                        
                        AnimatedVisibility(
                            visible = isControlsVisible,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            val formatTime = { ms: Long ->
                                val seconds = (ms / 1000) % 60
                                val minutes = (ms / (1000 * 60)) % 60
                                java.util.Locale.getDefault().let { locale ->
                                    String.format(locale, "%02d:%02d", minutes, seconds)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f))
                            ) {
                                // Center Playback Controls (Standalone)
                                PlaybackControls(
                                    isPlaying = isPlaying,
                                    onPlayPauseToggle = {
                                        if (isPlaying) exoPlayer?.pause() else exoPlayer?.play()
                                    },
                                    onForward = {
                                        exoPlayer?.seekTo(exoPlayer.currentPosition + 15000L)
                                    },
                                    onBackward = {
                                        exoPlayer?.seekTo(exoPlayer.currentPosition - 5000L)
                                    },
                                    onSkipNext = {
                                        exoPlayer?.seekTo(exoPlayer.duration)
                                    },
                                    onSkipPrevious = {
                                        exoPlayer?.seekTo(0L)
                                    },
                                    modifier = Modifier.align(Alignment.Center)
                                )

                                // Bottom Progress and Time (Separated Section)
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp)
                                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                        .padding(top = 8.dp, bottom = 12.dp, start = 12.dp, end = 12.dp)
                                ) {
                                    val sliderInteractionSource = remember { MutableInteractionSource() }
                                    val isPressed by sliderInteractionSource.collectIsPressedAsState()
                                    val isDragged by sliderInteractionSource.collectIsDraggedAsState()
                                    val isInteracting = isPressed || isDragged
                                    
                                    val thumbSize by animateDpAsState(
                                        targetValue = if (isInteracting) 20.dp else 14.dp,
                                        label = "thumbSize"
                                    )

                                    Slider(
                                        value = if (duration > 0) playbackPosition.toFloat() / duration.toFloat() else 0f,
                                        onValueChange = { 
                                            val seekPos = (it * duration).toLong()
                                            exoPlayer?.seekTo(seekPos)
                                            playbackPosition = seekPos
                                        },
                                        interactionSource = sliderInteractionSource,
                                        track = { sliderState ->
                                            SliderDefaults.Track(
                                                sliderState = sliderState,
                                                modifier = Modifier.height(if (isInteracting) 6.dp else 4.dp),
                                                colors = SliderDefaults.colors(
                                                    activeTrackColor = PrimaryGreen,
                                                    inactiveTrackColor = PrimaryGreen.copy(alpha = 0.25f)
                                                )
                                            )
                                        },
                                        thumb = {
                                            Surface(
                                                modifier = Modifier.size(thumbSize),
                                                shape = CircleShape,
                                                color = PrimaryGreen,
                                                shadowElevation = if (isInteracting) 8.dp else 4.dp
                                            ) {}
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${formatTime(playbackPosition)} / ${formatTime(duration)}",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        IconButton(onClick = { /* Settings Action */ }) {
                                            Icon(
                                                Icons.Default.Settings,
                                                contentDescription = "Settings",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }
                AsyncImage(
                    model = statusMedia.uri, contentDescription = null, contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            val extraWidth = (scale - 1) * size.width; val extraHeight = (scale - 1) * size.height
                            val maxX = extraWidth / 2; val maxY = extraHeight / 2
                            offset = Offset(x = (offset.x + pan.x * scale).coerceIn(-maxX, maxX), y = (offset.y + pan.y * scale).coerceIn(-maxY, maxY))
                        }
                    }.graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                )
            }

            if (isCompressing) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(color = PrimaryGreen); Spacer(modifier = Modifier.height(16.dp)); Text("Processing...", color = Color.White) }
                }
            }
        }
        if (showPlatformDialog) {
            AlertDialog(
                onDismissRequest = { showPlatformDialog = false },
                title = { Text("Open with") },
                text = { Text("Select your WhatsApp version") },
                confirmButton = { TextButton(onClick = { pendingShareAction?.invoke(PlatformType.WHATSAPP); showPlatformDialog = false }) { Text("WhatsApp", color = PrimaryGreen) } },
                dismissButton = { TextButton(onClick = { pendingShareAction?.invoke(PlatformType.WHATSAPP_BUSINESS); showPlatformDialog = false }) { Text("Business", color = PrimaryGreen) } },
                shape = RoundedCornerShape(24.dp), containerColor = Color.White
            )
        }
    }
}

@Composable
private fun ViewerTopBar(title: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(110.dp).background(brush = Brush.verticalGradient(listOf(PrimaryGreen, SecondaryGreen)), shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(modifier = Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.2f))) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun ViewerActionButton(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = onClick, modifier = Modifier.size(56.dp).background(color, CircleShape)) {
            Icon(icon, contentDescription = null,
                tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Text(label, color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onForward: () -> Unit,
    onBackward: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Skip Previous
        IconButton(onClick = onSkipPrevious) {
            Icon(ImageVector.vectorResource(id = R.drawable.step_backward_icon),
                contentDescription = null, tint = Color.White,
                modifier = Modifier.size(25.dp))
        }

        // Replay (Backward 5s)
        IconButton(onClick = onBackward) {
            Icon(ImageVector.vectorResource(id = R.drawable.reset_update_icon),
                contentDescription = null, tint = Color.White,
                modifier = Modifier.size(28.dp))
        }

        // Play/Pause (Circular White Background)
        IconButton(
            onClick = onPlayPauseToggle,
            modifier = Modifier.size(64.dp).background(Color.White, CircleShape)
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(40.dp)
            )
        }
        
        // Forward (Forward 15s)
        IconButton(onClick = onForward) {
            Icon(ImageVector.vectorResource(id = R.drawable.forward_restore_icon__1_),
                contentDescription = null, tint = Color.White,
                modifier = Modifier.size(28.dp))
        }
        
        // Skip Next
        IconButton(onClick = onSkipNext) {
            Icon(ImageVector.vectorResource(id = R.drawable.step_forward_icon),
                contentDescription = null,
                tint = Color.White, modifier = Modifier.size(25.dp))
        }
    }
}
@OptIn(UnstableApi::class)
@Preview(showBackground = true)
@Composable
fun MediaViewerScreenPreview() {
    WhatsAppStatusSaverTheme {
        MediaViewerScreen(statusMedia = StatusMedia(uri = Uri.EMPTY, name = "Sample", type = MediaType.IMAGE, size = 0, dateModified = 0, platform = PlatformType.WHATSAPP), onNavigateBack = {}, onSaveMedia = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun ViewerActionButtonPreview() {
    WhatsAppStatusSaverTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ViewerActionButton(
                icon = ImageVector.vectorResource(id = R.drawable.navigate_icon),
                label = "Send",
                color = PrimaryGreen,
                onClick = {}
            )
            ViewerActionButton(
                icon = ImageVector.vectorResource(id = R.drawable.share_line_icon),
                label = "Share",
                color = Color(0xFF2196F3),
                onClick = {}
            )
            ViewerActionButton(
                icon = ImageVector.vectorResource(id = R.drawable.import_icon),
                label = "Save",
                color = Color(0xFF4CAF50),
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun VideoPlaybackPreview() {
    WhatsAppStatusSaverTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Video Playback Icons", color = Color.White, fontWeight = FontWeight.Bold)
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Skip Previous
                IconButton(onClick = {}) {
                    Icon(ImageVector.vectorResource(id = R.drawable.step_backward_icon),
                        contentDescription = null, tint = Color.White,
                        modifier = Modifier.size(28.dp))
                }
                
                // Replay
                IconButton(onClick = {}) {
                    Icon(ImageVector.vectorResource(id = R.drawable.reset_update_icon),
                        contentDescription = null, tint = Color.White,
                        modifier = Modifier.size(28.dp))
                }
                
                // Play/Pause (Circular White Background)
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(64.dp).background(Color.White, CircleShape)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }
                // Forward
                IconButton(onClick = {}) {
                    Icon(ImageVector.vectorResource(id = R.drawable.forward_restore_icon__1_),
                        contentDescription = null, tint = Color.White,
                        modifier = Modifier.size(28.dp))
                }
                // Skip Next
                IconButton(onClick = {}) {
                    Icon(ImageVector.vectorResource(id = R.drawable.step_forward_icon),
                        contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}