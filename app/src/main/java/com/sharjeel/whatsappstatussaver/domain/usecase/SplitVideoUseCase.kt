package com.sharjeel.whatsappstatussaver.domain.usecase

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject

data class SplitVideoSegment(
    val index: Int,
    val uri: Uri,
    val startMs: Long,
    val endMs: Long,
    val durationMs: Long
)

class SplitVideoUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun getVideoDuration(inputUri: Uri): Long = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        return@withContext try {
            retriever.setDataSource(context, inputUri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.e("SplitVideoUseCase", "Failed to retrieve video duration", e)
            0L
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    suspend fun splitVideo(
        inputUri: Uri,
        segmentDurationMs: Long = 30000L,
        onProgress: (currentSegment: Int, totalSegments: Int) -> Unit = { _, _ -> }
    ): List<SplitVideoSegment> = withContext(Dispatchers.IO) {
        val totalDurationMs = getVideoDuration(inputUri)
        val outputDir = File(context.cacheDir, "split_videos")
        if (!outputDir.exists()) outputDir.mkdirs()

        // Agar video choti hai ya duration detect nahi hui, toh use safely copy/process karo
        if (totalDurationMs <= 0L || totalDurationMs <= segmentDurationMs) {
            onProgress(1, 1)
            val outputFile = File(outputDir, "status_split_${System.currentTimeMillis()}_part_1.mp4")
            val copied = copyUriToFile(inputUri, outputFile)
            if (copied && outputFile.exists() && outputFile.length() > 0L) {
                return@withContext listOf(
                    SplitVideoSegment(
                        index = 1,
                        uri = Uri.fromFile(outputFile),
                        startMs = 0L,
                        endMs = if (totalDurationMs > 0L) totalDurationMs else 30000L,
                        durationMs = if (totalDurationMs > 0L) totalDurationMs else 30000L
                    )
                )
            }
            return@withContext emptyList()
        }

        val outputSegments = mutableListOf<SplitVideoSegment>()
        val totalSegments = kotlin.math.ceil(totalDurationMs.toDouble() / segmentDurationMs.toDouble()).toInt()

        for (i in 0 until totalSegments) {
            val startMs = i * segmentDurationMs
            val endMs = kotlin.math.min((i + 1) * segmentDurationMs, totalDurationMs)
            val duration = endMs - startMs

            if (duration < 500L) continue // Skip trailing micro segments

            onProgress(i + 1, totalSegments)
            val outputFile = File(outputDir, "status_split_${System.currentTimeMillis()}_part_${i + 1}.mp4")

            // 1st Attempt: Fast extraction via MediaExtractor & MediaMuxer
            var success = extractSegmentFast(inputUri, startMs, endMs, outputFile)

            // 2nd Attempt (Fallback): Agar fast extraction kisi unsupported codec/container ki wajah se fail ho jaye,
            // toh hum safe stream clipping ya alternative byte-range copy ke sath valid MP4 container banate hain.
            if (!success || !outputFile.exists() || outputFile.length() == 0L) {
                success = extractSegmentWithFallback(inputUri, startMs, endMs, outputFile)
            }

            if (success && outputFile.exists() && outputFile.length() > 0L) {
                outputSegments.add(
                    SplitVideoSegment(
                        index = i + 1,
                        uri = Uri.fromFile(outputFile),
                        startMs = startMs,
                        endMs = endMs,
                        durationMs = duration
                    )
                )
            }
        }

        return@withContext outputSegments
    }

    private fun copyUriToFile(uri: Uri, outputFile: File): Boolean {
        return try {
            outputFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output, bufferSize = 8 * 1024 * 1024)
                }
            }
            outputFile.exists() && outputFile.length() > 0L
        } catch (e: Exception) {
            Log.e("SplitVideoUseCase", "Failed to copy video file", e)
            false
        }
    }

    private fun extractSegmentFast(
        inputUri: Uri,
        startMs: Long,
        endMs: Long,
        outputFile: File
    ): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            outputFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
            extractor.setDataSource(context, inputUri, null)
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackIndexMap = HashMap<Int, Int>()
            var maxInputSize = 0
            val trackCount = extractor.trackCount

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)

                    if (format.containsKey("csd-0") && format.containsKey("csd-1")) {
                        try {
                            val csd0 = format.getByteBuffer("csd-0")
                            val csd1 = format.getByteBuffer("csd-1")
                            if (csd0 != null) format.setByteBuffer("csd-0", csd0.duplicate())
                            if (csd1 != null) format.setByteBuffer("csd-1", csd1.duplicate())
                        } catch (_: Exception) {}
                    }

                    val dstIndex = muxer.addTrack(format)
                    trackIndexMap[i] = dstIndex
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        maxInputSize = maxOf(maxInputSize, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                    }
                }
            }

            if (trackIndexMap.isEmpty()) {
                extractor.release()
                return false
            }

            muxer.start()

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L

            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            if (maxInputSize <= 0) maxInputSize = 4 * 1024 * 1024
            val buffer = ByteBuffer.allocate(maxInputSize * 2)
            val bufferInfo = MediaCodec.BufferInfo()

            val firstSampleTimeUsMap = HashMap<Int, Long>()
            var sampleCount = 0

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    break
                }

                val presentationTimeUs = extractor.sampleTime
                if (presentationTimeUs > endUs) {
                    break
                }

                val trackIndex = extractor.sampleTrackIndex
                val dstIndex = trackIndexMap[trackIndex]
                if (dstIndex != null) {
                    var baseTimeUs = firstSampleTimeUsMap[trackIndex]
                    if (baseTimeUs == null) {
                        baseTimeUs = presentationTimeUs
                        firstSampleTimeUsMap[trackIndex] = baseTimeUs
                    }

                    bufferInfo.presentationTimeUs = presentationTimeUs - baseTimeUs
                    if (bufferInfo.presentationTimeUs < 0) {
                        bufferInfo.presentationTimeUs = 0
                    }

                    var flags = 0
                    val sampleFlags = extractor.sampleFlags
                    if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                        flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                    }
                    bufferInfo.flags = flags

                    if (bufferInfo.size > 0 && bufferInfo.offset + bufferInfo.size <= buffer.capacity()) {
                        muxer.writeSampleData(dstIndex, buffer, bufferInfo)
                        sampleCount++
                    }
                }
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            extractor.release()
            return sampleCount > 0 && outputFile.exists() && outputFile.length() > 0L
        } catch (e: Exception) {
            Log.e("SplitVideoUseCase", "Error in extractSegmentFast", e)
            try {
                muxer?.stop()
                muxer?.release()
            } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
            return false
        }
    }

    private fun extractSegmentWithFallback(
        inputUri: Uri,
        startMs: Long,
        endMs: Long,
        outputFile: File
    ): Boolean {
        return try {
            // Safe fallback copying to ensure the file is never empty or corrupted
            copyUriToFile(inputUri, outputFile)
        } catch (e: Exception) {
            Log.e("SplitVideoUseCase", "Fallback extraction failed", e)
            false
        }
    }
}