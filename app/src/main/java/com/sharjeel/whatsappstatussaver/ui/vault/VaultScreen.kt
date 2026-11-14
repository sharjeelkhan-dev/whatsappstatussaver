package com.sharjeel.whatsappstatussaver.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.sharjeel.whatsappstatussaver.data.models.MediaType
import com.sharjeel.whatsappstatussaver.data.models.StatusMedia
import com.sharjeel.whatsappstatussaver.theme.WhatsAppStatusSaverTheme

private val PrimaryGreen = Color(0xFF00A884)
private val SecondaryGreen = Color(0xFF005E4C)

@Composable
fun VaultScreen(
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (StatusMedia) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    LaunchedEffect(Unit) {
        activity?.let { viewModel.authenticate(it) }
    }

    VaultContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToViewer = onNavigateToViewer,
        onLockVault = { viewModel.lockVault() },
        onAuthenticate = { activity?.let { viewModel.authenticate(it) } },
        onRemoveFromVault = { viewModel.removeFromVault(it) },
        modifier = modifier
    )
}

@Composable
fun VaultContent(
    uiState: VaultUiState,
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (StatusMedia) -> Unit,
    onLockVault: () -> Unit,
    onAuthenticate: () -> Unit,
    onRemoveFromVault: (StatusMedia) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            VaultTopBar(
                isUnlocked = uiState.isUnlocked,
                onBack = onNavigateBack,
                onLockVault = onLockVault
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!uiState.isUnlocked) {
                LockedStateView(
                    errorMessage = uiState.errorMessage,
                    onAuthenticate = onAuthenticate
                )
            } else {
                if (uiState.vaultMedia.isEmpty()) {
                    EmptyVaultView()
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.vaultMedia) { media ->
                            VaultMediaItem(
                                media = media,
                                onClick = { onNavigateToViewer(media) },
                                onRemoveFromVault = { onRemoveFromVault(media) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultTopBar(
    isUnlocked: Boolean,
    onBack: () -> Unit,
    onLockVault: () -> Unit
) {
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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Private Vault",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (isUnlocked) "Unlocked & Encrypted" else "Protected Storage",
                    fontSize = 12.sp,
                    modifier = Modifier.offset(y = (-5).dp),
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        if (isUnlocked) {
            IconButton(
                onClick = onLockVault,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(CircleShape)
                    .offset(y = (-3).dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock Vault",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun LockedStateView(
    errorMessage: String?,
    onAuthenticate: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = PrimaryGreen.copy(alpha = 0.15f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Text(
                    text = "Vault is Locked",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Unlock using your Fingerprint, Face ID, or PIN to access personal hidden statuses.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = onAuthenticate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock Vault", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EmptyVaultView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LockOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Your Vault is Empty",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Media added to Vault from Saved Gallery will be protected here.",
                fontSize = 14.sp,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun VaultMediaItem(
    media: StatusMedia,
    onClick: () -> Unit,
    onRemoveFromVault: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.Black)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(media.uri)
                    .decoderFactory(VideoFrameDecoder.Factory())
                    .crossfade(true)
                    .build(),
                contentDescription = media.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (media.type == MediaType.VIDEO) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Video",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center)
                )
            }

            IconButton(
                onClick = onRemoveFromVault,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = "Unhide",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VaultScreenPreview() {
    WhatsAppStatusSaverTheme {
        VaultContent(
            uiState = VaultUiState(isUnlocked = true),
            onNavigateBack = {},
            onNavigateToViewer = {},
            onLockVault = {},
            onAuthenticate = {},
            onRemoveFromVault = {}
        )
    }
}
