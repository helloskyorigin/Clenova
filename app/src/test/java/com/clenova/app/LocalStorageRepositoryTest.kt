package com.clenova.app

import com.clenova.app.data.storage.LocalStorageRepository
import com.clenova.app.domain.model.StorageOverview
import com.clenova.app.domain.model.StorageState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocalStorageRepositoryTest {
    @Test
    fun overlappingRefreshesUseOneRead() = runTest {
        val release = CompletableDeferred<Unit>()
        val snapshot = StorageOverview(100L, 40L, 60L)
        var reads = 0
        val repository = LocalStorageRepository {
            reads++
            release.await()
            snapshot
        }
        val first = launch { repository.refreshStorage() }
        runCurrent()
        repository.refreshStorage()
        assertEquals(1, reads)
        release.complete(Unit)
        first.join()
        assertEquals(StorageState.Ready(snapshot), repository.storageState.value)
    }

    @Test
    fun cancellationDoesNotBlockLaterRefresh() = runTest {
        var reads = 0
        val release = CompletableDeferred<Unit>()
        val repository = LocalStorageRepository {
            if (++reads == 1) release.await()
            null
        }
        val first = launch { repository.refreshStorage() }
        runCurrent()
        first.cancelAndJoin()
        repository.refreshStorage()
        assertEquals(2, reads)
        assertEquals(StorageState.Unavailable, repository.storageState.value)
    }
}
