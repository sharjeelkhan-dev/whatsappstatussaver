@file:OptIn(ExperimentalMaterial3Api::class)

package com.sharjeel.whatsappstatussaver.ui.viewer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sharjeel.whatsappstatussaver.R
import com.sharjeel.whatsappstatussaver.data.models.MediaType
import com.sharjeel.whatsappstatussaver.data.models.PlatformType
import com.sharjeel.whatsappstatussaver.data.models.StatusMedia
import com.sharjeel.whatsappstatussaver.theme.WhatsAppStatusSaverTheme
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

private val PrimaryGreen = Color(0xFF00A884)
private val SecondaryGreen = Color(0xFF005E4C)
private val DarkText = Color(0xFF1C2D2A)

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
    onCompressVideo: (suspend (Uri) -> Uri?)? = null,
    modifier: Modifier = Modifier,
    magicViewModel: MagicViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCompressing by remember { mutableStateOf(false) }
    var showPlatformDialog by remember { mutableStateOf(false) }
    var pendingShareAction by remember { mutableStateOf<((PlatformType) -> Unit)?>(null) }
    var showMagicSheet by remember { mutableStateOf(false) }
    val magicUiState by magicViewModel.uiState.collectAsState()

    if (showMagicSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMagicSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            MagicContent(
                uiState = magicUiState,
                onAction = { type ->
                    coroutineScope.launch {
                        if (statusMedia.type == MediaType.VIDEO) {
                            val bytes = context.contentResolver.openInputStream(statusMedia.uri)?.readBytes()
                            val mime = context.contentResolver.getType(statusMedia.uri) ?: "video/mp4"
                            magicViewModel.triggerMagic(type, null, bytes, mime)
                        } else {
                            val request = ImageRequest.Builder(context)
                                .data(statusMedia.uri)
                                .allowHardware(false)
                                .build()
                            val result = coil.ImageLoader(context).execute(request)
                            val bitmap = result.drawable?.toBitmap()
                            if (bitmap != null) {
                                magicViewModel.triggerMagic(type, bitmap)
                            } else {
                                Toast.makeText(context, "Could not process image", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onReset = { magicViewModel.resetState() }
            )
        }
    }

    fun checkAndShare(action: (PlatformType) -> Unit) {
        val isWhatsappInstalled = try { context.packageManager.getPackageInfo("com.whatsapp", 0); true } catch (_: Exception) { false }
        val isBusinessInstalled = try { context.packageManager.getPackageInfo("com.whatsapp.w4b", 0); true } catch (_: Exception) { false }

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

    LaunchedEffect(isPlaying, isControlsVisible) {
        if (isPlaying && isControlsVisible) {
            while (true) {
                playbackPosition = exoPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L
                duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
                kotlinx.coroutines.delay(500.milliseconds)
            }
        }
    }

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
        modifier = modifier.fillMaxSize(),
        topBar = {
            ViewerTopBar(
                title = if (statusMedia.type == MediaType.VIDEO) "Video" else "Photo",
                onBack = onNavigateBack,
                onMagicClick = { showMagicSheet = true }
            )
        },
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
                                } catch (_: Exception) { isCompressing = false; Toast.makeText(context, "Share failed", Toast.LENGTH_SHORT).show() }
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
                            } catch (_: Exception) { isCompressing = false }
                        }
                    }

                    ViewerActionButton(icon = ImageVector.vectorResource(id = R.drawable.import_icon),
                        label = "Save", color = Color(0xFF4CAF50)) {
                        try { onSaveMedia(statusMedia); Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show() } catch (_: Exception) { }
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
private fun ViewerTopBar(title: String, onBack: () -> Unit, onMagicClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(110.dp).background(brush = Brush.verticalGradient(listOf(PrimaryGreen, SecondaryGreen)), shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(modifier = Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.2f))) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        }

        IconButton(
            onClick = onMagicClick,
            modifier = Modifier.align(Alignment.CenterEnd).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Magic", tint = Color.White)
        }
    }
}

@Composable
private fun MagicContent(uiState: MagicUiState, onAction: (MagicType) -> Unit, onReset: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("AI Status Magic", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = DarkText)
        Spacer(modifier = Modifier.height(24.dp))

        when (uiState) {
            is MagicUiState.Idle -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MagicButton(icon = Icons.Default.Psychology, label = "AI Analysis", Modifier.weight(1f)) { onAction(MagicType.ANALYSIS) }
                        MagicButton(icon = Icons.Default.TextFields, label = "Text OCR", Modifier.weight(1f)) { onAction(MagicType.OCR) }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MagicButton(icon = ImageVector.vectorResource(id = R.drawable.closed_captions_video_clip_black_icon), label = "Captions", Modifier.weight(1f)) { onAction(MagicType.CAPTION) }
                        MagicButton(icon = ImageVector.vectorResource(id = R.drawable.feather_icon), label = "Shayari", Modifier.weight(1f)) { onAction(MagicType.SHAYARI) }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MagicButton(icon = ImageVector.vectorResource(id = R.drawable.object_selected_icon), label = "Objects", Modifier.weight(1f)) { onAction(MagicType.OBJECT_DETECTION) }
                        MagicButton(icon = ImageVector.vectorResource(id = R.drawable.language_translate_speech_bubbles_black_icon), label = "Translate", Modifier.weight(1f)) { onAction(MagicType.TRANSLATE_URDU) }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MagicButton(icon = ImageVector.vectorResource(id = R.drawable.emoji_tongue_black_icon), label = "Mood", Modifier.weight(1f)) { onAction(MagicType.MOOD) }
                        MagicButton(icon = Icons.Default.AutoAwesome, label = "Magic Picks", Modifier.weight(1f)) { onAction(MagicType.RECOMMENDATION) }
                    }
                }
            }
            is MagicUiState.Loading -> {
                CircularProgressIndicator(color = PrimaryGreen)
                Spacer(modifier = Modifier.height(12.dp))
                Text(uiState.message, color = Color.Gray)
            }
            is MagicUiState.Success -> {
                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F8E9), RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(uiState.type.name, fontWeight = FontWeight.Bold, color = PrimaryGreen, modifier = Modifier.weight(1f))
                            IconButton(onClick = { 
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("AI Result", uiState.result)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(uiState.result, color = DarkText)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = onReset) {
                    Text("Try Another Magic", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                }
            }
            is MagicUiState.Error -> {
                Text("Error: ${uiState.message}", color = Color.Red)
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onReset) { Text("Retry", color = PrimaryGreen) }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun MagicButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF7F8F9),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkText)
        }
    }
}

@Composable
private fun ViewerActionButton(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = onClick, modifier = Modifier.size(56.dp).background(color, CircleShape)) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        IconButton(onClick = onSkipPrevious) { Icon(ImageVector.vectorResource(id = R.drawable.step_backward_icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(25.dp)) }
        IconButton(onClick = onBackward) { Icon(ImageVector.vectorResource(id = R.drawable.reset_update_icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) }
        IconButton(onClick = onPlayPauseToggle, modifier = Modifier.size(64.dp).background(Color.White, CircleShape)) {
            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(40.dp))
        }
        IconButton(onClick = onForward) { Icon(ImageVector.vectorResource(id = R.drawable.forward_restore_icon__1_), contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) }
        IconButton(onClick = onSkipNext) { Icon(ImageVector.vectorResource(id = R.drawable.step_forward_icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(25.dp)) }
    }
}

// Previews
@Preview(showBackground = true)
@Composable
fun MagicContentIdlePreview() {
    WhatsAppStatusSaverTheme {
        Surface(color = Color.White) {
            MagicContent(uiState = MagicUiState.Idle, onAction = {}, onReset = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MagicContentSuccessPreview() {
    WhatsAppStatusSaverTheme {
        Surface(color = Color.White) {
            MagicContent(
                uiState = MagicUiState.Success("This is a sample AI result.", MagicType.ANALYSIS),
                onAction = {},
                onReset = {}
            )
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Preview(showBackground = true)
@Composable
fun MediaViewerScreenPreview() {
    WhatsAppStatusSaverTheme {
        MediaViewerScreen(
            statusMedia = StatusMedia(Uri.EMPTY, "Sample", MediaType.IMAGE, 0, 0, PlatformType.WHATSAPP),
            onNavigateBack = {},
            onSaveMedia = {}
        )
    }
}
