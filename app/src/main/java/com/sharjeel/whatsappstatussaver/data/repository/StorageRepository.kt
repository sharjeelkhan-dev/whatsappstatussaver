package com.sharjeel.whatsappstatussaver.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.sharjeel.whatsappstatussaver.data.local.dao.SavedMediaDao
import com.sharjeel.whatsappstatussaver.data.local.datastore.AppSettings
import com.sharjeel.whatsappstatussaver.data.local.entity.SavedMediaEntity
import com.sharjeel.whatsappstatussaver.data.models.MediaType
import com.sharjeel.whatsappstatussaver.data.models.PlatformType
import com.sharjeel.whatsappstatussaver.data.models.StatusMedia
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

    suspend fun getStatuses(platform: PlatformType): Pair<List<StatusMedia>, String> = withContext(Dispatchers.IO) {
        val statuses = mutableListOf<StatusMedia>()
        val persistedPermissions = context.contentResolver.persistedUriPermissions
        var debugMsg = "No folder access for ${platform.name}. Please grant permission."
        var folderFound = false

        val platformPermissions = persistedPermissions.filter {
            it.isReadPermission && isUriRelevantForFolder(it.uri.toString(), platform, forProfilePhotos = false)
        }

        platformPermissions.forEach { permission ->
            // Try Direct ID Query first (FAST & No Caching)
            val directItems = tryDirectIdQueryEnhanced(permission.uri, platform)
            if (directItems.isNotEmpty()) {
                statuses.addAll(directItems)
                folderFound = true
            }
            
            // Traversal fallback for items missed by direct query
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
                                    val fileName = name.lowercase()
                                    val mediaType = if (fileName.endsWith(".mp4") || fileName.endsWith(".mkv") || fileName.endsWith(".3gp") || fileName.endsWith(".avi") ||
                                        fileName.endsWith(".mov") || fileName.endsWith(".wmv") || fileName.endsWith(".flv") || fileName.endsWith(".webm")) MediaType.VIDEO else MediaType.IMAGE
                                    
                                    statuses.add(
                                        StatusMedia(
                                            uri = file.uri,
                                            name = name,
                                            type = mediaType,
                                            size = file.length(),
                                            dateModified = file.lastModified(),
                                            platform = platform
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("StorageRepo", "Tree traversal error", e)
            }
        }

        if (folderFound || statuses.isNotEmpty()) {
            debugMsg = "Success: Loaded items for ${platform.name}."
        }

        val finalStatuses = statuses.distinctBy { it.name }.sortedByDescending { it.dateModified }
        Pair(finalStatuses, debugMsg)
    }

    private fun tryDirectIdQueryEnhanced(treeUri: Uri, platform: PlatformType): List<StatusMedia> {
        val items = mutableListOf<StatusMedia>()
        try {
            val treeId = DocumentsContract.getTreeDocumentId(treeUri) ?: return emptyList()
            val volumePrefix = if (treeId.contains(":")) treeId.substringBefore(":") + ":" else "primary:"

            val targetId = if (platform == PlatformType.WHATSAPP) {
                "${volumePrefix}Android/media/com.whatsapp/WhatsApp/Media/.Statuses"
            } else {
                "${volumePrefix}Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"
            }

            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, targetId)

            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null, null, null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val dateCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idCol)
                    val name = cursor.getString(nameCol)
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol)
                    val mime = cursor.getString(mimeCol) ?: ""

                    if (!id.isNullOrEmpty() && !name.isNullOrEmpty() && isMediaFile(name)) {
                        val type = if (mime.startsWith("video") || name.lowercase().let { 
                            it.endsWith(".mp4") || it.endsWith(".mkv") || it.endsWith(".3gp") || it.endsWith(".mov") 
                        }) MediaType.VIDEO else MediaType.IMAGE
                        
                        items.add(
                            StatusMedia(
                                uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id),
                                name = name,
                                type = type,
                                size = size,
                                dateModified = date,
                                platform = platform
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("StorageRepo", "Direct query failed", e)
        }
        return items
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
                n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".3gp") || n.endsWith(".avi") ||
                n.endsWith(".mov") || n.endsWith(".wmv") || n.endsWith(".flv") || n.endsWith(".webm")
    }

    // [Profile Photo logic remains similar but optimized below...]

    suspend fun getProfilePhotos(platform: PlatformType): Pair<List<StatusMedia>, String> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<StatusMedia>()
        val persistedPermissions = context.contentResolver.persistedUriPermissions
        var debugMsg = "No folder access for ${platform.name}."
        val activePhotoNames = mutableSetOf<String>()
        var folderFound = false

        Log.d("StorageRepo", "--- Starting Profile Photo Scan for ${platform.name} ---")
        Log.d("StorageRepo", "Total Persisted Permissions: ${persistedPermissions.size}")

        val platformPermissions = persistedPermissions.filter {
            it.isReadPermission && isUriRelevantForFolder(it.uri.toString(), platform, forProfilePhotos = true)
        }

        Log.d("StorageRepo", "Relevant Permissions for ${platform.name}: ${platformPermissions.size}")

        platformPermissions.forEach { permission ->
            Log.d("StorageRepo", "Checking Permission URI: ${permission.uri}")
            try {
                val root = DocumentFile.fromTreeUri(context, permission.uri)
                if (root != null) {
                    Log.d("StorageRepo", "Root Folder Name: ${root.name}")
                    val targetFolders = resolveAllPossibleProfileFolders(root, platform)
                    Log.d("StorageRepo", "Target Folders Found: ${targetFolders.size}")
                    
                    if (targetFolders.isNotEmpty()) {
                        folderFound = true
                        targetFolders.forEach { folder ->
                            Log.d("StorageRepo", "Scanning Folder: ${folder.uri}")
                            val files = folder.listFiles()
                            Log.d("StorageRepo", "Files in Folder: ${files.size}")
                            files.forEach { file ->
                                val name = file.name
                                if (!name.isNullOrEmpty() && !name.endsWith(".nomedia") && isMediaFile(name)) {
                                    if (!activePhotoNames.contains(name)) {
                                        activePhotoNames.add(name)
                                        Log.d("StorageRepo", "Found Photo: $name")
                                        cacheProfilePhotoFromUri(file.uri, name, platform)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Log.e("StorageRepo", "Root DocumentFile is null for URI: ${permission.uri}")
                }
            } catch (e: Exception) {
                Log.e("StorageRepo", "Profile photo scan error", e)
            }
        }

        if (folderFound) debugMsg = "Success: Loaded profile photos."
        else {
            Log.w("StorageRepo", "No profile photo folders were successfully resolved.")
            debugMsg = "No Profile Photos folder found. Please ensure you've viewed DPs in WhatsApp and granted permission to the correct folder."
        }

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
        
        Log.d("StorageRepo", "Final Profile Photo Count: ${photos.size}")
        Pair(photos.distinctBy { it.name }.sortedByDescending { it.dateModified }, debugMsg)
    }

    private fun resolveAllPossibleProfileFolders(root: DocumentFile, platform: PlatformType): List<DocumentFile> {
        val folders = mutableListOf<DocumentFile>()
        
        // Comprehensive path list for WhatsApp Profile Photos
        val paths = if (platform == PlatformType.WHATSAPP) {
            listOf(
                "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Profile Photos",
                "com.whatsapp/WhatsApp/Media/WhatsApp Profile Photos",
                "WhatsApp/Media/WhatsApp Profile Photos",
                "Media/WhatsApp Profile Photos",
                "WhatsApp Profile Photos"
            )
        } else {
            listOf(
                "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Profile Photos",
                "com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Profile Photos",
                "WhatsApp Business/Media/WhatsApp Profile Photos",
                "Media/WhatsApp Profile Photos",
                "WhatsApp Profile Photos"
            )
        }

        paths.forEach { path ->
            var current: DocumentFile? = root
            val segments = path.split("/")
            for (segment in segments) {
                // If the current root's name is part of the path, we skip segments until we align
                if (current?.name == segment) continue 
                
                val found = current?.findFile(segment)
                if (found != null) {
                    current = found
                } else {
                    current = null
                    break
                }
            }
            if (current != null && current.isDirectory) {
                folders.add(current!!)
            }
        }

        // Fallback: If no folder found via direct paths, do a recursive search
        if (folders.isEmpty()) {
            val deepFound = findFolderRecursive(root, "WhatsApp Profile Photos", 8)
            if (deepFound != null) folders.add(deepFound)
        }

        return folders.distinctBy { it.uri }
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
            val uri = statusMedia.uri
            Log.d("StorageRepo", "Deleting file: ${statusMedia.name}, URI: $uri")

            // 1. Attempt to delete the physical file/content entry
            try {
                when (uri.scheme) {
                    "file" -> {
                        val file = File(uri.path ?: "")
                        if (file.exists()) file.delete()
                    }
                    "content" -> {
                        if (DocumentsContract.isDocumentUri(context, uri)) {
                            // SAF Document URI
                            val doc = DocumentFile.fromSingleUri(context, uri)
                            if (doc != null && doc.exists()) doc.delete()
                        } else {
                            // MediaStore or other content URI
                            context.contentResolver.delete(uri, null, null)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("StorageRepo", "Physical deletion failed: ${e.message}")
            }

            // 2. Always delete from local Database to keep UI clean
            val entity = savedMediaDao.getMediaByName(statusMedia.name)
            if (entity != null) {
                savedMediaDao.deleteMedia(entity)
                Log.d("StorageRepo", "Database record deleted for: ${statusMedia.name}")
            }

            true
        } catch (e: Exception) {
            Log.e("StorageRepo", "Error in deleteSavedFile: ${e.message}")
            false
        }
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

