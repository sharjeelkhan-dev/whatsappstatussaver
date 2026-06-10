package com.example.whatsappstatussaver.domain.usecase

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CompressVideoUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    suspend operator fun invoke(inputUri: Uri): Uri? = suspendCancellableCoroutine { continuation ->
        try {
            val outputDir = File(context.cacheDir, "compressed_videos")
            if (!outputDir.exists()) outputDir.mkdirs()
            
            val outputFile = File(outputDir, "compressed_${System.currentTimeMillis()}.mp4")

            val transformer = Transformer.Builder(context)
                .setVideoMimeType("video/avc") // H.264
                .setAudioMimeType("audio/mp4a-latm") // AAC
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        Log.d("CompressVideo", "Compression successful")
                        continuation.resume(Uri.fromFile(outputFile))
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        Log.e("CompressVideo", "Compression failed", exportException)
                        continuation.resume(null)
                    }
                })
                .build()

            val mediaItem = MediaItem.fromUri(inputUri)
            val editedMediaItem = EditedMediaItem.Builder(mediaItem).build()
            
            transformer.start(editedMediaItem, outputFile.absolutePath)

            continuation.invokeOnCancellation {
                transformer.cancel()
            }
        } catch (e: Exception) {
            Log.e("CompressVideo", "Error initializing compression", e)
            continuation.resume(null)
        }
    }
}
