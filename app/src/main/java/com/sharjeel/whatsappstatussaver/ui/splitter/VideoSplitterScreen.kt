package com.sharjeel.whatsappstatussaver.ui.splitter

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.sharjeel.whatsappstatussaver.R
import com.sharjeel.whatsappstatussaver.data.models.MediaType
import com.sharjeel.whatsappstatussaver.domain.usecase.SplitVideoSegment

private val PrimaryGreen = Color(0xFF00A884)
private val SecondaryGreen = Color(0xFF005E4C)

@Composable
fun VideoSplitterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (Uri, MediaType, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VideoSplitterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.selectVideo(it) }
    }

    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            VideoSplitterTopBar(onBack = onNavigateBack)
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 24.dp,
                bottom = navigationBarInset.calculateBottomPadding() + 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                VideoSelectionCard(
                    selectedUri = uiState.selectedVideoUri,
                    durationMs = uiState.totalDurationMs,
                    onSelectVideo = { videoPickerLauncher.launch("video/*") }
                )
            }
            if (uiState.selectedVideoUri != null) {
                item {
                    Button(
                        onClick = { viewModel.splitVideo() },
                        enabled = !uiState.isSplitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            if (uiState.isSplitting) "Splitting Video..." else "Auto-Split into 30s Clips",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            if (uiState.isSplitting) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = PrimaryGreen, modifier = Modifier.size(32.dp))
                            Column {
                                Text("Processing Clips", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(uiState.progressText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            if (uiState.errorMessage != null) {
                item {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
            if (uiState.segments.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Split Segments (${uiState.segments.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = { viewModel.shareAllSegments(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.share_line_icon),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share All", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                items(uiState.segments) { segment ->
                    SegmentItemCard(
                        segment = segment,
                        onPlaySegment = {
                            onNavigateToViewer(segment.uri, MediaType.VIDEO, "Clip_${segment.index}.mp4")
                        },
                        onSaveSegment = { viewModel.saveSegmentToGallery(context, segment) },
                        onShareSegment = { viewModel.shareSegment(context, segment) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoSplitterTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(listOf(PrimaryGreen, SecondaryGreen)),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .offset(y = (-5).dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "30s Status Splitter",
                modifier = Modifier.offset(y = (-5).dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun VideoSelectionCard(
    selectedUri: Uri?,
    durationMs: Long,
    onSelectVideo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (selectedUri == null) {
                Icon(
                    imageVector = Icons.Default.VideoFile,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    "Select a video to split",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Choose any long video from gallery to split into 30-second clips.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val durationSec = durationMs / 1000
                val estimatedParts = kotlin.math.ceil(durationMs.toDouble() / 30000L).toInt()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(selectedUri)
                                .decoderFactory(VideoFrameDecoder.Factory())
                                .videoFrameMillis(1000)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Video Loaded",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Duration: ${durationSec / 60}m ${durationSec % 60}s",
                            fontSize = 14.sp,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Will create ~ $estimatedParts status clips",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onSelectVideo,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(if (selectedUri == null) "Browse Gallery Video" else "Change Video")
            }
        }
    }
}

@Composable
private fun SegmentItemCard(
    segment: SplitVideoSegment,
    onPlaySegment: () -> Unit,
    onSaveSegment: () -> Unit,
    onShareSegment: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onPlaySegment),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(segment.uri)
                        .decoderFactory(VideoFrameDecoder.Factory())
                        .videoFrameMillis(500)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    shape = CircleShape,
                    color = PrimaryGreen,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "${segment.index}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Clip ${segment.index}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val startSec = segment.startMs / 1000
                val endSec = segment.endMs / 1000
                Text(
                    text = "${startSec / 60}:${"%02d".format(startSec % 60)} - ${endSec / 60}:${"%02d".format(endSec % 60)} (${segment.durationMs / 1000}s)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onSaveSegment,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                        .size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.import_icon),
                        contentDescription = "Save to Gallery",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onShareSegment,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(PrimaryGreen.copy(alpha = 0.15f))
                        .size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.share_line_icon),
                        contentDescription = "Share",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Empty State", showBackground = true)
@Composable
private fun VideoSplitterScreenEmptyPreview() {
    MaterialTheme {
        Surface {
            Column(modifier = Modifier.fillMaxSize()) {
                VideoSplitterTopBar(onBack = {})
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        VideoSelectionCard(
                            selectedUri = null,
                            durationMs = 0L,
                            onSelectVideo = {}
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Segments Loaded State", showBackground = true)
@Composable
private fun VideoSplitterScreenLoadedPreview() {
    val dummySegments = listOf(
        SplitVideoSegment(1, Uri.EMPTY, 0L, 30000L, 30000L),
        SplitVideoSegment(2, Uri.EMPTY, 30000L, 60000L, 30000L),
        SplitVideoSegment(3, Uri.EMPTY, 60000L, 75000L, 15000L)
    )

    MaterialTheme {
        Surface {
            Column(modifier = Modifier.fillMaxSize()) {
                VideoSplitterTopBar(onBack = {})
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        VideoSelectionCard(
                            selectedUri = "content://media/external/video/media/123".toUri(),
                            durationMs = 75000L,
                            onSelectVideo = {}
                        )
                    }
                    item {
                        Button(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Icon(Icons.Default.ContentCut, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Auto-Split into 30s Clips", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Split Segments (${dummySegments.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = {},
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.share_line_icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share All", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    items(dummySegments) { segment ->
                        SegmentItemCard(
                            segment = segment,
                            onPlaySegment = {},
                            onSaveSegment = {},
                            onShareSegment = {}
                        )
                    }
                }
            }
        }
    }
}