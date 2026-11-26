package com.clenova.app.data.storage

import com.clenova.app.core.storage.DeviceStorageProvider
import com.clenova.app.domain.model.StorageState
import com.clenova.app.domain.repository.StorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalStorageRepository(
    private val storageProvider: DeviceStorageProvider,
) : StorageRepository {

    private val _storageState = MutableStateFlow<StorageState>(StorageState.Loading)
    override val storageState: StateFlow<StorageState> = _storageState.asStateFlow()

    override suspend fun refreshStorage() {
        val overview = storageProvider.readInternalStorage()
        _storageState.value = if (overview != null) {
            StorageState.Ready(overview)
        } else {
            StorageState.Unavailable
        }
    }
}
