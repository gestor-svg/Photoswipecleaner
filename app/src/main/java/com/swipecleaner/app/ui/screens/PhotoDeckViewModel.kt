package com.swipecleaner.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swipecleaner.app.data.MediaRepository
import com.swipecleaner.app.domain.PhotoItem
import com.swipecleaner.app.domain.SwipeDirection
import com.swipecleaner.app.domain.SwipeResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PhotoDeckState {
    data object Loading : PhotoDeckState
    data class Loaded(
        val photos: List<PhotoItem>,
        val currentIndex: Int = 0,
        val history: List<SwipeResult> = emptyList(),
        val freedBytes: Long = 0L,
        // Acumulado de fotos/bytes ya confirmados hacia la papelera en esta
        // sesión del deck. Se puede confirmar más de una vez (botón
        // "Confirmar" a mitad del deck, además del final).
        val confirmedCount: Int = 0,
        val confirmedBytes: Long = 0L
    ) : PhotoDeckState {
        /** Fotos marcadas para papelera, aún no enviadas. */
        val trashCandidates: List<PhotoItem>
            get() = history.filter { it.direction == SwipeDirection.LEFT }.map { it.photo }
    }
    data class Error(val message: String) : PhotoDeckState
}

class PhotoDeckViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)

    private val _state = MutableStateFlow<PhotoDeckState>(PhotoDeckState.Loading)
    val state: StateFlow<PhotoDeckState> = _state.asStateFlow()

    private var currentBucketId: String? = null

    fun loadPhotos(bucketId: String) {
        if (currentBucketId == bucketId && _state.value is PhotoDeckState.Loaded) return
        currentBucketId = bucketId
        _state.value = PhotoDeckState.Loading
        viewModelScope.launch {
            try {
                val photos = repository.getPhotosInFolder(bucketId)
                _state.value = PhotoDeckState.Loaded(photos)
            } catch (e: Exception) {
                _state.value = PhotoDeckState.Error(e.message ?: "Error al cargar fotos")
            }
        }
    }

    fun onSwipe(direction: SwipeDirection) {
        val s = _state.value
        if (s !is PhotoDeckState.Loaded) return
        val photo = s.photos.getOrNull(s.currentIndex) ?: return

        val result = SwipeResult(photo, direction)
        val freedDelta = if (direction == SwipeDirection.LEFT) photo.sizeBytes else 0L

        _state.value = s.copy(
            currentIndex = s.currentIndex + 1,
            history = s.history + result,
            freedBytes = s.freedBytes + freedDelta
        )
    }

    fun undo() {
        val s = _state.value
        if (s !is PhotoDeckState.Loaded || s.history.isEmpty()) return
        val last = s.history.last()
        val freedDelta = if (last.direction == SwipeDirection.LEFT) -last.photo.sizeBytes else 0L

        _state.value = s.copy(
            currentIndex = (s.currentIndex - 1).coerceAtLeast(0),
            history = s.history.dropLast(1),
            freedBytes = s.freedBytes + freedDelta
        )
    }

    /**
     * Se llama tras el resultado del TrashOrchestrator. Puede ocurrir a mitad
     * del deck (botón "Confirmar" en la barra superior) o al final. En ambos
     * casos limpia el historial pendiente y acumula el conteo/bytes ya
     * confirmados, sin tocar currentIndex, para poder seguir deslizando.
     */
    fun onTrashConfirmed(deletedCount: Int) {
        val s = _state.value
        if (s !is PhotoDeckState.Loaded) return
        _state.value = s.copy(
            history = emptyList(),
            confirmedCount = s.confirmedCount + deletedCount,
            confirmedBytes = s.confirmedBytes + s.freedBytes,
            freedBytes = 0L
        )
    }
}
