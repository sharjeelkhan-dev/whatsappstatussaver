package com.example.whatsappstatussaver.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.example.whatsappstatussaver.data.local.dao.SavedMediaDao
import com.example.whatsappstatussaver.data.local.datastore.AppSettings
import com.example.whatsappstatussaver.data.local.entity.SavedMediaEntity
import com.example.whatsappstatussaver.data.models.MediaType
import com.example.whatsappstatussaver.data.models.PlatformType
import com.example.whatsappstatussaver.data.models.StatusMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val context: Context,
    private val savedMediaDao: SavedMediaDao,
    private val appSettings: AppSettings
) {

    fun isAppInstalled(platform: PlatformType): Boolean {
        val packageName = if (platform == PlatformType.WHATSAPP) "com.whatsapp" else "com.whatsapp.w4b"
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun hasPermission(platform: PlatformType, forProfilePhotos: Boolean = false): Boolean {
        val permissions = context.contentResolver.persistedUriPermissions
        return permissions.any { permission ->
            permission.isReadPermission && isUriRelevantForFolder(permission.uri.toString(), platform, forProfilePhotos)
        }
    }

    private fun isUriRelevantForFolder(uriStr: String, platform: PlatformType, forProfilePhotos: Boolean): Boolean {
        val decoded = Uri.decode(uriStr)
        val isExplicitBusiness = decoded.contains("com.whatsapp.w4b", ignoreCase = true) || 
                                decoded.contains("WhatsApp Business", ignoreCase = true)
        val isExplicitNormal = (decoded.contains("com.whatsapp", ignoreCase = true) || 
                               decoded.contains("WhatsApp", ignoreCase = true)) && !isExplicitBusiness
        
        val platformMatch = when (platform) {
            PlatformType.WHATSAPP -> !isExplicitBusiness
            PlatformType.WHATSAPP_BUSINESS -> !isExplicitNormal
            else -> true
        }

        if (!platformMatch) return false

        val isStatusesSpecific = decoded.endsWith(".Statuses", ignoreCase = true)
        val isProfilePhotosSpecific = decoded.endsWith("WhatsApp Profile Photos", ignoreCase = true)

        return if (forProfilePhotos) {
            !isStatusesSpecific
        } else {
            !isProfilePhotosSpecific
        }
    }

    private fun getCacheFolder(platform: PlatformType): File {
        val folder = File(context.filesDir, "status_cache/${platform.name}")
        if (!folder.exists()) folder.mkdirs()
        return folder
    }

    private fun cacheStatusFileFromUri(fileUri: Uri, name: String, platform: PlatformType) {
        try {
            val cacheFolder = getCacheFolder(platform)
            val destFile = File(cacheFolder, name)

            context.contentResolver.openInputStream(fileUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Log.e("StorageRepo", "Caching failed for file: $name", e)
        }
    }

    suspend fun getStatuses(platform: PlatformType): Pair<List<StatusMedia>, String> = withContext(Dispatchers.IO) {
        val statuses = mutableListOf<StatusMedia>()
        val persistedPermissions = context.contentResolver.persistedUriPermissions
        var debugMsg = "No folder access for ${platform.name}. Please grant permission."
        val activeStatusNames = mutableSetOf<String>()
        var folderFound = false

        // Strategy 1: Systematic search across all relevant permissions
        val platformPermissions = persistedPermissions.filter {
            it.isReadPermission && isUriRelevantForFolder(it.uri.toString(), platform, forProfilePhotos = false)
        }

        platformPermissions.forEach { permission ->
            // Try Direct ID Query first (FAST)
            val directLoaded = tryDirectIdQuery(permission.uri, platform, activeStatusNames)
            
            // Even if direct loaded, some files might be missing from direct ID, so we also traverse
            try {
                val root = DocumentFile.fromTreeUri(context, permission.uri)
                if (root != null) {
                    val targetFolders = resolveAllPossibleStatusFolders(root, platform)
                    if (targetFolders.isNotEmpty()) {
                        folderFound = true
                        targetFolders.forEach { folder ->
                            folder.listFiles().forEach { file ->
                                val name = file.name
                                if (!name.isNullOrEmpty() && !name.endsWith(".nomedia") && isMediaFile(name)) {
                                    if (!activeStatusNames.contains(name)) {
                                        activeStatusNames.add(name)
                                        cacheStatusFileFromUri(file.uri, name, platform)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("StorageRepo", "Tree traversal error", e)
            }
            if (directLoaded) folderFound = true
        }

        if (folderFound) {
            debugMsg = "Success: Loaded items for ${platform.name}."
        }

        // Process cached elements and strictly CLEANUP stale data
        val cacheFolder = getCacheFolder(platform)
        cacheFolder.listFiles()?.forEach { file ->
            if (folderFound && !activeStatusNames.contains(file.name)) {
                file.delete()
                return@forEach
            }

            val fileName = file.name.lowercase()
            val mediaType = when {
                fileName.endsWith(".mp4") || fileName.endsWith(".mkv") || fileName.endsWith(".3gp") || fileName.endsWith(".avi") -> MediaType.VIDEO
                fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") || fileName.endsWith(".webp") -> MediaType.IMAGE
                else -> null
            }

            if (mediaType != null) {
                statuses.add(
                    StatusMedia(
                        uri = Uri.fromFile(file),
                        name = file.name,
                        type = mediaType,
                        size = file.length(),
                        dateModified = file.lastModified(),
                        platform = platform
                    )
                )
            }
        }

        val finalStatuses = statuses.distinctBy { it.name }.sortedByDescending { it.dateModified }
        Pair(finalStatuses, debugMsg)
    }

    private fun resolveAllPossibleStatusFolders(root: DocumentFile, platform: PlatformType): List<DocumentFile> {
        val folders = mutableListOf<DocumentFile>()
        if (root.name.equals(".Statuses", ignoreCase = true)) {
            folders.add(root)
            return folders
        }

        // Multi-Path Search to capture all statuses
        val paths = if (platform == PlatformType.WHATSAPP) {
            listOf(
                "com.whatsapp/WhatsApp/Media/.Statuses",
                "WhatsApp/Media/.Statuses",
                "Media/.Statuses",
                ".Statuses"
            )
        } else {
            listOf(
                "com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
                "WhatsApp Business/Media/.Statuses",
                "Media/.Statuses",
                ".Statuses"
            )
        }

        paths.forEach { path ->
            var current: DocumentFile? = root
            val segments = path.split("/")
            for (segment in segments) {
                if (current?.name == segment) continue // Skip if root already matches segment
                current = current?.findFile(segment)
                if (current == null) break
            }
            if (current != null && current.isDirectory) folders.add(current)
        }

        // Deep fallback if no folder found via known paths
        if (folders.isEmpty()) {
            val deepFound = findFolderRecursive(root, ".Statuses", 8)
            if (deepFound != null) folders.add(deepFound)
        }

        return folders
    }

    private fun isMediaFile(name: String): Boolean {
        val n = name.lowercase()
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp") ||
                n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".3gp") || n.endsWith(".avi")
    }

    private fun tryDirectIdQuery(treeUri: Uri, platform: PlatformType, activeNames: MutableSet<String>): Boolean {
        try {
            val treeId = DocumentsContract.getTreeDocumentId(treeUri) ?: return false
            val volumePrefix = if (treeId.contains(":")) treeId.substringBefore(":") + ":" else "primary:"

            val targetId = if (platform == PlatformType.WHATSAPP) {
                "${volumePrefix}Android/media/com.whatsapp/WhatsApp/Media/.Statuses"
            } else {
                "${volumePrefix}Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"
            }

            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, targetId)
            var found = false

            context.contentResolver.query(
                childrenUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val id = cursor.getString(idCol)
                    val name = cursor.getString(nameCol)
                    if (!id.isNullOrEmpty() && !name.isNullOrEmpty() && isMediaFile(name)) {
                        if (!activeNames.contains(name)) {
                            activeNames.add(name)
                            cacheStatusFileFromUri(DocumentsContract.buildDocumentUriUsingTree(treeUri, id), name, platform)
                            found = true
                        }
                    }
                }
            }
            return found
        } catch (e: Exception) {
            return false
        }
    }

    // [Profile Photo logic remains similar but optimized below...]

    suspend fun getProfilePhotos(platform: PlatformType): Pair<List<StatusMedia>, String> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<StatusMedia>()
        val persistedPermissions = context.contentResolver.persistedUriPermissions
        var debugMsg = "No folder access for ${platform.name}."
        val activePhotoNames = mutableSetOf<String>()
        var folderFound = false

        val platformPermissions = persistedPermissions.filter {
            it.isReadPermission && isUriRelevantForFolder(it.uri.toString(), platform, forProfilePhotos = true)
        }

        platformPermissions.forEach { permission ->
            try {
                val root = DocumentFile.fromTreeUri(context, permission.uri)
                if (root != null) {
                    val targetFolders = resolveAllPossibleProfileFolders(root, platform)
                    if (targetFolders.isNotEmpty()) {
                        folderFound = true
                        targetFolders.forEach { folder ->
                            folder.listFiles().forEach { file ->
                                val name = file.name
                                if (!name.isNullOrEmpty() && !name.endsWith(".nomedia") && isMediaFile(name)) {
                                    if (!activePhotoNames.contains(name)) {
                                        activePhotoNames.add(name)
                                        cacheProfilePhotoFromUri(file.uri, name, platform)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("StorageRepo", "Profile photo scan error", e)
            }
        }

        if (folderFound) debugMsg = "Success: Loaded profile photos."

        val cacheFolder = getProfileCacheFolder(platform)
        cacheFolder.listFiles()?.forEach { file ->
            if (folderFound && !activePhotoNames.contains(file.name)) {
                file.delete()
                return@forEach
            }
            val fileName = file.name.lowercase()
            val mediaType = if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") || fileName.endsWith(".webp")) MediaType.IMAGE else null
            if (mediaType != null) {
                photos.add(StatusMedia(uri = Uri.fromFile(file), name = file.name, type = mediaType, size = file.length(), dateModified = file.lastModified(), platform = platform, isProfilePhoto = true))
            }
        }
        Pair(photos.distinctBy { it.name }.sortedByDescending { it.dateModified }, debugMsg)
    }

    private fun resolveAllPossibleProfileFolders(root: DocumentFile, platform: PlatformType): List<DocumentFile> {
        val folders = mutableListOf<DocumentFile>()
        val paths = if (platform == PlatformType.WHATSAPP) {
            listOf("com.whatsapp/WhatsApp/Media/WhatsApp Profile Photos", "WhatsApp/Media/WhatsApp Profile Photos", "Media/WhatsApp Profile Photos")
        } else {
            listOf("com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Profile Photos", "WhatsApp Business/Media/WhatsApp Profile Photos", "Media/WhatsApp Profile Photos")
        }
        paths.forEach { path ->
            var current: DocumentFile? = root
            path.split("/").forEach { segment ->
                if (current?.name == segment) return@forEach
                current = current?.findFile(segment)
                if (current == null) return@forEach
            }
            if (current != null && current.isDirectory) folders.add(current)
        }
        return folders
    }

    private fun getProfileCacheFolder(platform: PlatformType): File {
        val folder = File(context.filesDir, "profile_cache/${platform.name}")
        if (!folder.exists()) folder.mkdirs()
        return folder
    }

    private fun cacheProfilePhotoFromUri(fileUri: Uri, name: String, platform: PlatformType) {
        try {
            val cacheFolder = getProfileCacheFolder(platform)
            val destFile = File(cacheFolder, name)
            context.contentResolver.openInputStream(fileUri)?.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            }
        } catch (e: Exception) { Log.e("StorageRepo", "Profile caching failed", e) }
    }

    suspend fun saveStatus(statusMedia: StatusMedia): Boolean = withContext(Dispatchers.IO) {
        try {
            val existing = savedMediaDao.getMediaByName(statusMedia.name)
            if (existing != null) return@withContext true

            val customLocationUri = appSettings.customSaveLocationFlow.firstOrNull()
            val savedUri = if (!customLocationUri.isNullOrEmpty()) saveToCustomLocation(statusMedia, customLocationUri) else saveToDefaultLocation(statusMedia)
            if (savedUri == null) return@withContext false

            savedMediaDao.insertMedia(SavedMediaEntity(uriString = savedUri.toString(), name = statusMedia.name, type = statusMedia.type, size = statusMedia.size, dateSaved = System.currentTimeMillis(), sourcePlatform = statusMedia.platform, isFavorite = false, isProfilePhoto = statusMedia.isProfilePhoto))
            
            val extension = MimeTypeMap.getFileExtensionFromUrl(statusMedia.name)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            MediaScannerConnection.scanFile(context, arrayOf(Uri.decode(savedUri.toString())), arrayOf(mimeType), null)
            true
        } catch (e: Exception) { false }
    }

    private fun saveToCustomLocation(statusMedia: StatusMedia, customUriStr: String): Uri? {
        return try {
            val parentFolder = DocumentFile.fromTreeUri(context, Uri.parse(customUriStr)) ?: return null
            val existingFile = parentFolder.findFile(statusMedia.name)
            if (existingFile != null) return existingFile.uri
            val mimeType = if (statusMedia.type == MediaType.VIDEO) "video/mp4" else "image/jpeg"
            val newFile = parentFolder.createFile(mimeType, statusMedia.name) ?: return null
            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                val inputStream = if (statusMedia.uri.scheme == "file") File(statusMedia.uri.path ?: "").inputStream() else context.contentResolver.openInputStream(statusMedia.uri)
                inputStream?.use { it.copyTo(output) }
            }
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(newFile.uri, takeFlags)
            } catch (e: Exception) {}
            newFile.uri
        } catch (e: Exception) { null }
    }

    private fun saveToDefaultLocation(statusMedia: StatusMedia): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, statusMedia.name)
            put(MediaStore.MediaColumns.MIME_TYPE, if (statusMedia.type == MediaType.VIDEO) "video/mp4" else "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, if (statusMedia.type == MediaType.VIDEO) "Movies/StatusSaver" else "Pictures/StatusSaver")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val collectionUri = if (statusMedia.type == MediaType.VIDEO) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val savedUri = context.contentResolver.insert(collectionUri, contentValues) ?: return null
        try {
            context.contentResolver.openOutputStream(savedUri)?.use { output ->
                val inputStream = if (statusMedia.uri.scheme == "file") File(statusMedia.uri.path ?: "").inputStream() else context.contentResolver.openInputStream(statusMedia.uri)
                inputStream?.use { it.copyTo(output) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(savedUri, contentValues, null, null)
            }
            return savedUri
        } catch (e: Exception) { return null }
    }

    suspend fun deleteSavedFile(statusMedia: StatusMedia): Boolean = withContext(Dispatchers.IO) {
        try {
            if (statusMedia.uri.scheme == "file") {
                val file = File(statusMedia.uri.path ?: "")
                if (file.exists()) file.delete()
            } else {
                context.contentResolver.delete(statusMedia.uri, null, null)
            }
            savedMediaDao.getMediaByName(statusMedia.name)?.let { savedMediaDao.deleteMedia(it) }
            true
        } catch (e: Exception) { false }
    }

    suspend fun updateTags(media: StatusMedia, tags: String) = withContext(Dispatchers.IO) {
        savedMediaDao.getMediaByName(media.name)?.let { savedMediaDao.updateTags(it.id, tags) }
    }

    private fun findFolderRecursive(parent: DocumentFile, targetName: String, depth: Int): DocumentFile? {
        if (depth <= 0) return null
        val children = parent.listFiles()
        for (child in children) {
            if (child.isDirectory && child.name == targetName) return child
        }
        for (child in children) {
            if (child.isDirectory) {
                if (child.name == "Android" || child.name == "data") continue 
                val found = findFolderRecursive(child, targetName, depth - 1)
                if (found != null) return found
            }
        }
        return null
    }
}
