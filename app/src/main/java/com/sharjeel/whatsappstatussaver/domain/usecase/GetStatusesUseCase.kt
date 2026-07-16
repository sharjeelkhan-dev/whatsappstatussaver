package com.sharjeel.whatsappstatussaver.domain.usecase

import com.sharjeel.whatsappstatussaver.data.models.PlatformType
import com.sharjeel.whatsappstatussaver.data.models.StatusMedia
import com.sharjeel.whatsappstatussaver.data.repository.StorageRepository
import javax.inject.Inject

class GetStatusesUseCase @Inject constructor(
    private val repository: StorageRepository
) {
    suspend operator fun invoke(platform: PlatformType): List<StatusMedia> {
        return repository.getStatuses(platform).first
    }
}

