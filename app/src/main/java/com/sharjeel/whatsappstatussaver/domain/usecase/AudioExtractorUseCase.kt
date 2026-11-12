package com.sharjeel.whatsappstatussaver.domain.usecase

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import javax.inject.Inject

class AudioExtractorUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(videoUri: Uri): Uri? = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            val pfd = context.contentResolver.openFileDescriptor(videoUri, "r") ?: return@withContext null
            extractor.setDataSource(pfd.fileDescriptor)

            val outputDir = File(context.cacheDir, "extracted_audio")
            if (!outputDir.exists()) outputDir.mkdirs()
            val outputFile = File(outputDir, "status_audio_${System.currentTimeMillis()}.m4a")

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            var audioTrackIndex = -1
            val trackCount = extractor.trackCount
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    break
                }
            }

            if (audioTrackIndex == -1) {
                extractor.release()
                pfd.close()
                return@withContext null
            }

            extractor.selectTrack(audioTrackIndex)
            val trackFormat = extractor.getTrackFormat(audioTrackIndex)
            val writeAudioIndex = muxer.addTrack(trackFormat)
            muxer.start()

            val maxBufferSize = 512 * 1024
            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    break
                }
                bufferInfo.presentationTimeUs = extractor.sampleTime
                var flags = 0
                val sampleFlags = extractor.sampleFlags
                if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                    flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                }
                bufferInfo.flags = flags
                muxer.writeSampleData(writeAudioIndex, buffer, bufferInfo)
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            extractor.release()
            pfd.close()

            return@withContext Uri.fromFile(outputFile)
        } catch (e: Exception) {
            Log.e("AudioExtractor", "Failed to extract audio", e)
            try {
                muxer?.stop()
                muxer?.release()
            } catch (_: Exception) {}
            extractor.release()
            return@withContext null
        }
    }
}