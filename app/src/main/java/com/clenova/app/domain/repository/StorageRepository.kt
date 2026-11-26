package com.clenova.app.domain.repository

import com.clenova.app.domain.model.StorageState
import kotlinx.coroutines.flow.StateFlow

interface StorageRepository {
    val storageState: StateFlow<StorageState>
    suspend fun refreshStorage()
}
