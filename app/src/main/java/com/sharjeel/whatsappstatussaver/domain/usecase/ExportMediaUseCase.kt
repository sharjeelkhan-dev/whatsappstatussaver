package com.sharjeel.whatsappstatussaver.domain.usecase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.sharjeel.whatsappstatussaver.data.local.dao.SavedMediaDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class ExportMediaUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: SavedMediaDao
) {
    suspend fun exportToZip(destinationUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val allMedia = dao.getAllSavedMedia().first()
            if (allMedia.isEmpty()) return@withContext false

            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    for (media in allMedia) {
                        try {
                            val uri = Uri.parse(media.uriString)
                            val inputStream = if (uri.scheme == "file") {
                                File(uri.path ?: "").inputStream()
                            } else {
                                context.contentResolver.openInputStream(uri)
                            }
                            
                            inputStream?.use { input ->
                                val zipEntry = ZipEntry(media.name)
                                zipOut.putNextEntry(zipEntry)
                                input.copyTo(zipOut)
                                zipOut.closeEntry()
                            }
                        } catch (e: Exception) {
                            Log.e("ExportMedia", "Failed to zip file: ${media.name}", e)
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e("ExportMedia", "Export failed", e)
            false
        }
    }
}

