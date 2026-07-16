package com.sharjeel.whatsappstatussaver.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudBackupRepository @Inject constructor() {
    
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance().reference

    suspend fun signInAnonymously(): Boolean {
        return try {
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }
            true
        } catch (e: Exception) {
            Log.e("CloudBackup", "Auth failed", e)
            false
        }
    }

    suspend fun backupFile(fileUri: Uri, fileName: String): Boolean {
        return try {
            val user = auth.currentUser ?: return false
            val fileRef = storage.child("backups/${user.uid}/$fileName")
            fileRef.putFile(fileUri).await()
            true
        } catch (e: Exception) {
            Log.e("CloudBackup", "Upload failed", e)
            false
        }
    }

    suspend fun listBackups(): List<String> {
        return try {
            val user = auth.currentUser ?: return emptyList()
            val listResult = storage.child("backups/${user.uid}/").listAll().await()
            listResult.items.map { it.name }
        } catch (e: Exception) {
            Log.e("CloudBackup", "List failed", e)
            emptyList()
        }
    }
}

