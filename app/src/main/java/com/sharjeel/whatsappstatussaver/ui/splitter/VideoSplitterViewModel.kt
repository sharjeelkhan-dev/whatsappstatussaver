package com.sharjeel.whatsappstatussaver.ui.splitter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharjeel.whatsappstatussaver.data.models.MediaType
import com.sharjeel.whatsappstatussaver.data.models.PlatformType
import com.sharjeel.whatsappstatussaver.data.models.StatusMedia
import com.sharjeel.whatsappstatussaver.data.repository.StorageRepository
import com.sharjeel.whatsappstatussaver.domain.usecase.SplitVideoSegment
import com.sharjeel.whatsappstatussaver.domain.usecase.SplitVideoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class VideoSplitterUiState(
    val selectedVideoUri: Uri? = null,
    val totalDurationMs: Long = 0L,
    val isSplitting: Boolean = false,
    val progressText: String = "",
    val segments: List<SplitVideoSegment> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class VideoSplitterViewModel @Inject constructor(
    private val splitVideoUseCase: SplitVideoUseCase,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoSplitterUiState())
    val uiState: StateFlow<VideoSplitterUiState> = _uiState.asStateFlow()

    fun selectVideo(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedVideoUri = uri, isSplitting = false, segments = emptyList(), errorMessage = null) }
            val duration = splitVideoUseCase.getVideoDuration(uri)
            _uiState.update { it.copy(totalDurationMs = duration) }
        }
    }

    fun splitVideo() {
        val uri = _uiState.value.selectedVideoUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSplitting = true, progressText = "Preparing video split...", errorMessage = null) }
            try {
                val segments = splitVideoUseCase.splitVideo(inputUri = uri, segmentDurationMs = 30000L) { current, total ->
                    _uiState.update { it.copy(progressText = "Splitting part $current of $total...") }
                }
                if (segments.isEmpty()) {
                    _uiState.update { it.copy(isSplitting = false, errorMessage = "Could not split video. Video may be too short or corrupted.") }
                } else {
                    _uiState.update { it.copy(isSplitting = false, segments = segments, progressText = "Split into ${segments.size} parts!") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSplitting = false, errorMessage = "Error: ${e.localizedMessage}") }
            }
        }
    }

    fun saveSegmentToGallery(context: Context, segment: SplitVideoSegment) {
        viewModelScope.launch {
            try {
                val file = File(segment.uri.path ?: "")
                val media = StatusMedia(
                    uri = segment.uri,
                    name = "Split_Clip_${segment.index}_${System.currentTimeMillis()}.mp4",
                    type = MediaType.VIDEO,
                    size = if (file.exists()) file.length() else 0L,
                    dateModified = System.currentTimeMillis(),
                    platform = PlatformType.SAVED
                )
                val success = storageRepository.saveStatus(media)
                if (success) {
                    Toast.makeText(context, "Saved to Gallery successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to save to gallery", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving clip: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareSegment(context: Context, segment: SplitVideoSegment) {
        try {
            val contentUri = getShareableUri(context, segment.uri)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Clip ${segment.index}"))
        } catch (_: Exception) {
            Toast.makeText(context, "Could not share video segment", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareAllSegments(context: Context) {
        val segments = _uiState.value.segments
        if (segments.isEmpty()) return

        try {
            val uriList = ArrayList<Uri>()
            segments.forEach { seg ->
                uriList.add(getShareableUri(context, seg.uri))
            }

            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "video/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share All ${segments.size} Clips"))
        } catch (_: Exception) {
            Toast.makeText(context, "Could not share all segments", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getShareableUri(context: Context, uri: Uri): Uri {
        return if (uri.scheme == "file") {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(uri.path ?: ""))
        } else {
            uri
        }
    }
}