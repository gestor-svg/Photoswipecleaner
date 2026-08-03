package com.swipecleaner.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swipecleaner.app.data.MediaRepository
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
        // Acumulado de fotos/bytes ya confirmados hacia la papelera en esta
        // sesión del deck (puede pasar más de una vez: botón "Confirmar" a
        // mitad del deck, banner de dañados, o al final).
        val confirmedCount: Int = 0,
        val confirmedBytes: Long = 0L,
        // Archivos detectados como dañados por el escaneo en segundo plano,
        // pendientes de enviar a papelera vía el banner.
        val corruptedPhotos: List<PhotoItem> = emptyList()
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
                scanForCorruptedFiles(photos)
            } catch (e: Exception) {
                _state.value = PhotoDeckState.Error(e.message ?: "Error al cargar fotos")
            }
        }
    }

    /**
     * Escanea la carpeta completa en segundo plano (sin bloquear el swipe)
     * buscando archivos dañados/ilegibles. Al terminar, si encontró alguno,
     * lo publica en el estado para que el banner aparezca en pantalla —
     * el usuario puede seguir deslizando mientras tanto.
     */
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
                // Si el usuario ya cambió de carpeta mientras escaneábamos, descartar el resultado.
                if (s is PhotoDeckState.Loaded && s.photos === photos) {
                    // Excluye los que el usuario ya resolvió a mano por swipe mientras corría el escaneo.
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
     * Se llama tras el resultado del TrashOrchestrator para las fotos
     * marcadas manualmente (swipe/botones). Puede ocurrir a mitad del deck
     * (botón "Confirmar") o al final.
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

    /**
     * Se llama tras enviar a papelera el lote de archivos dañados desde el
     * banner. A diferencia de onTrashConfirmed, aquí sí hay que quitar esas
     * fotos de la lista `photos` (ya no existen para seguir deslizando) y
     * reacomodar currentIndex si alguna ya había sido recorrida.
     */
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
