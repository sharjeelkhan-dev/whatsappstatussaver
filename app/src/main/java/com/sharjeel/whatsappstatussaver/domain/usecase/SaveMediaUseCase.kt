package com.sharjeel.whatsappstatussaver.domain.usecase

import com.sharjeel.whatsappstatussaver.data.models.StatusMedia
import com.sharjeel.whatsappstatussaver.data.repository.StorageRepository
import javax.inject.Inject

class SaveMediaUseCase @Inject constructor(
    private val repository: StorageRepository
) {
    suspend operator fun invoke(statusMedia: StatusMedia): Boolean {
        return repository.saveStatus(statusMedia)
    }
}

