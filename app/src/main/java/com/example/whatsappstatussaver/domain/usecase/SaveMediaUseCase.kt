package com.example.whatsappstatussaver.domain.usecase

import com.example.whatsappstatussaver.data.models.StatusMedia
import com.example.whatsappstatussaver.data.repository.StorageRepository
import javax.inject.Inject

class SaveMediaUseCase @Inject constructor(
    private val repository: StorageRepository
) {
    suspend operator fun invoke(statusMedia: StatusMedia): Boolean {
        return repository.saveStatus(statusMedia)
    }
}
