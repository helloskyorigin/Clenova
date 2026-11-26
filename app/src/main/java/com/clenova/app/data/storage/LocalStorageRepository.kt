package com.clenova.app.data.storage

import com.clenova.app.core.storage.DeviceStorageProvider
import com.clenova.app.domain.model.StorageOverview
import com.clenova.app.domain.model.StorageState
import com.clenova.app.domain.repository.StorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

class LocalStorageRepository(
    private val readStorage: suspend () -> StorageOverview?,
) : StorageRepository {
    constructor(storageProvider: DeviceStorageProvider) : this(storageProvider::readInternalStorage)

    private val refreshLock = Mutex()
    private val _storageState = MutableStateFlow<StorageState>(StorageState.Loading)
    override val storageState: StateFlow<StorageState> = _storageState.asStateFlow()

    override suspend fun refreshStorage() {
        // Coalesce overlapping refreshes rather than queue duplicate disk reads.
        if (!refreshLock.tryLock()) return
        try {
            val overview = readStorage()
            _storageState.value = if (overview != null) StorageState.Ready(overview)
                else StorageState.Unavailable
        } finally {
            refreshLock.unlock()
        }
    }
}
