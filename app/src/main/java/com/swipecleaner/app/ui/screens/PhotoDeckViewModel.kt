package com.swipecleaner.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swipecleaner.app.data.DAILY_SWIPE_LIMIT
import com.swipecleaner.app.data.MediaRepository
import com.swipecleaner.app.data.SwipeLimitManager
import com.swipecleaner.app.domain.PhotoItem
import com.swipecleaner.app.domain.SwipeDirection
import com.swipecleaner.app.domain.SwipeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface PhotoDeckState {
    data object Loading : PhotoDeckState
    data class Loaded(
        val photos: List<PhotoItem>,
        val currentIndex: Int = 0,
        val history: List<SwipeResult> = emptyList(),
        val freedBytes: Long = 0L,
        val confirmedCount: Int = 0,
        val confirmedBytes: Long = 0L,
        val corruptedPhotos: List<PhotoItem> = emptyList(),
        // Swipes restantes hoy (0..30), independiente de la carpeta.
        val remainingSwipesToday: Int = DAILY_SWIPE_LIMIT
    ) : PhotoDeckState {
        val trashCandidates: List<PhotoItem>
            get() = history.filter { it.direction == SwipeDirection.LEFT }.map { it.photo }
    }
    data class Error(val message: String) : PhotoDeckState
}

class PhotoDeckViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)
    private val swipeLimitManager = SwipeLimitManager(application)

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
                _state.value = PhotoDeckState.Loaded(
                    photos = photos,
                    remainingSwipesToday = swipeLimitManager.remaining()
                )
                scanForCorruptedFiles(photos)
            } catch (e: Exception) {
                _state.value = PhotoDeckState.Error(e.message ?: "Error al cargar fotos")
            }
        }
    }

    private fun scanForCorruptedFiles(photos: List<PhotoItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            val corrupted = mutableListOf<PhotoItem>()
            for (photo in photos) {
                if (repository.isCorrupted(photo.uri)) {
                    corrupted += photo
                }
            }
            if (corrupted.isEmpty()) return@launch

            withContext(Dispatchers.Main) {
                val s = _state.value
                if (s is PhotoDeckState.Loaded && s.photos === photos) {
                    val alreadyHandledIds = s.history.map { it.photo.id }.toSet()
                    val filtered = corrupted.filterNot { it.id in alreadyHandledIds }
                    if (filtered.isNotEmpty()) {
                        _state.value = s.copy(corruptedPhotos = filtered)
                    }
                }
            }
        }
    }

    fun onSwipe(direction: SwipeDirection) {
        val s = _state.value
        if (s !is PhotoDeckState.Loaded) return
        // Límite diario ya alcanzado: ignora el intento de swipe. La UI no
        // debería permitir llegar aquí (oculta el gesto/botones), esto es
        // un resguardo adicional.
        if (s.remainingSwipesToday <= 0) return

        val photo = s.photos.getOrNull(s.currentIndex) ?: return
        val result = SwipeResult(photo, direction)
        val freedDelta = if (direction == SwipeDirection.LEFT) photo.sizeBytes else 0L
        swipeLimitManager.registerSwipe()

        _state.value = s.copy(
            currentIndex = s.currentIndex + 1,
            history = s.history + result,
            freedBytes = s.freedBytes + freedDelta,
            // Se relee del manager (no se recalcula a mano) para que respete
            // el estado "desbloqueado" por código maestro correctamente.
            remainingSwipesToday = swipeLimitManager.remaining()
        )
    }
    /**
     * Deshacer NO devuelve cupo del límite diario a propósito: si lo hiciera,
     * swipe→deshacer→swipe repetido evadiría el límite por completo.
     */
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

    fun onCorruptedTrashConfirmed(sentPhotos: List<PhotoItem>, deletedCount: Int) {
        if (sentPhotos.isEmpty()) return
        val s = _state.value
        if (s !is PhotoDeckState.Loaded) return

        val sentIds = sentPhotos.map { it.id }.toSet()
        val removedBeforeCurrent = s.photos.take(s.currentIndex).count { it.id in sentIds }
        val newPhotos = s.photos.filterNot { it.id in sentIds }
        val sentBytes = sentPhotos.sumOf { it.sizeBytes }

        _state.value = s.copy(
            photos = newPhotos,
            currentIndex = (s.currentIndex - removedBeforeCurrent).coerceAtLeast(0),
            confirmedCount = s.confirmedCount + deletedCount,
            confirmedBytes = s.confirmedBytes + sentBytes,
            corruptedPhotos = emptyList()
        )
    }
}
