package com.swipecleaner.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swipecleaner.app.data.MediaRepository
import com.swipecleaner.app.domain.BucketFolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FolderListState {
    data object Loading : FolderListState
    data class Loaded(val folders: List<BucketFolder>) : FolderListState
    data class Error(val message: String) : FolderListState
}

class FolderListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)

    private val _state = MutableStateFlow<FolderListState>(FolderListState.Loading)
    val state: StateFlow<FolderListState> = _state.asStateFlow()

    init {
        loadFolders()
    }

    fun loadFolders() {
        _state.value = FolderListState.Loading
        viewModelScope.launch {
            try {
                val folders = repository.getFolders()
                _state.value = FolderListState.Loaded(folders)
            } catch (e: Exception) {
                _state.value = FolderListState.Error(e.message ?: "Error al leer la galería")
            }
        }
    }
}
