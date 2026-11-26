package com.clenova.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clenova.app.domain.model.AutopilotStatus
import com.clenova.app.domain.model.StorageState
import com.clenova.app.domain.repository.PreferencesRepository
import com.clenova.app.domain.repository.StorageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val storageState: StorageState = StorageState.Loading,
    val autopilotStatus: AutopilotStatus = AutopilotStatus.NotConfigured,
)

class HomeViewModel(
    private val storageRepository: StorageRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        storageRepository.storageState,
        preferencesRepository.userPreferences,
    ) { storage, prefs ->
        HomeUiState(
            storageState = storage,
            autopilotStatus = prefs.autopilotStatus,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HomeUiState(),
    )

    init {
        refreshStorage()
    }

    fun refreshStorage() {
        viewModelScope.launch {
            storageRepository.refreshStorage()
        }
    }

    class Factory(
        private val storageRepository: StorageRepository,
        private val preferencesRepository: PreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(
                storageRepository = storageRepository,
                preferencesRepository = preferencesRepository,
            ) as T
        }
    }
}
