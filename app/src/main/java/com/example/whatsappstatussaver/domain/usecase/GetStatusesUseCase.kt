package com.example.whatsappstatussaver.domain.usecase

import com.example.whatsappstatussaver.data.models.PlatformType
import com.example.whatsappstatussaver.data.models.StatusMedia
import com.example.whatsappstatussaver.data.repository.StorageRepository
import javax.inject.Inject

class GetStatusesUseCase @Inject constructor(
    private val repository: StorageRepository
) {
    suspend operator fun invoke(platform: PlatformType): List<StatusMedia> {
        return repository.getStatuses(platform).first
    }
}
