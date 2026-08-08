package com.swipecleaner.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swipecleaner.app.data.DAILY_SWIPE_LIMIT
import com.swipecleaner.app.data.FolderProgressManager
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
import kotlin.random.Random

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
        val remainingSwipesToday: Int = DAILY_SWIPE_LIMIT,
        val totalPhotosInFolder: Int = 0,
        // Tarjeta interstitial de donar — no corresponde a ninguna foto real,
        // aparece de forma intercalada en el mazo cada cierto número de swipes.
        val showDonateCard: Boolean = false
    ) : PhotoDeckState {
        val trashCandidates: List<PhotoItem>
            get() = history.filter { it.direction == SwipeDirection.LEFT }.map { it.photo }
    }
    data class Error(val message: String) : PhotoDeckState
}

class PhotoDeckViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)
    private val swipeLimitManager = SwipeLimitManager(application)
    private val folderProgressManager = FolderProgressManager(application)

    private val _state = MutableStateFlow<PhotoDeckState>(PhotoDeckState.Loading)
    val state: StateFlow<PhotoDeckState> = _state.asStateFlow()

    private var currentBucketId: String? = null

    // Control de la tarjeta de donar: cuenta swipes reales desde la última
    // aparición, comparado contra un umbral aleatorio. Vive como estado de
    // instancia (no persistido) a propósito — el rango se cuenta "desde que
    // abres la app", y como este ViewModel se reutiliza mientras la app
    // sigue abierta (aunque cambies de carpeta), se reinicia solo al volver
    // a abrir la app desde cero.
    private var swipesSinceLastDonateCard = 0
    private var donateCardThreshold = randomDonateThreshold()

    private fun randomDonateThreshold(): Int {
        val unlocked = swipeLimitManager.isMasterUnlocked() || swipeLimitManager.isTier1Active()
        return if (unlocked) {
            Random.nextInt(40, 71) // 40..70 inclusive, menos seguido si ya está desbloqueado
        } else {
            Random.nextInt(1, DAILY_SWIPE_LIMIT + 1) // 1..30 inclusive
        }
    }

    fun loadPhotos(bucketId: String) {
        if (currentBucketId == bucketId && _state.value is PhotoDeckState.Loaded) {
            val current = _state.value as PhotoDeckState.Loaded
            _state.value = current.copy(remainingSwipesToday = swipeLimitManager.remaining())
            return
        }
        currentBucketId = bucketId
        _state.value = PhotoDeckState.Loading
        viewModelScope.launch {
            try {
                val allPhotos = repository.getPhotosInFolder(bucketId)
                val keptIds = folderProgressManager.getKeptIds(bucketId)
                val photos = allPhotos.filterNot { it.id in keptIds }
                _state.value = PhotoDeckState.Loaded(
                    photos = photos,
                    remainingSwipesToday = swipeLimitManager.remaining(),
                    totalPhotosInFolder = allPhotos.size
                )
                scanForCorruptedFiles(photos)
            } catch (e: Exception) {
                _state.value = PhotoDeckState.Error(e.message ?: "Error al cargar fotos")
            }
        }
    }

    /** "Ver desde el principio" / "Revisar de nuevo" — olvida el progreso guardado y recarga todo. */
    fun resetFolderProgress(bucketId: String) {
        folderProgressManager.resetFolder(bucketId)
        currentBucketId = null
        loadPhotos(bucketId)
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
        if (s.showDonateCard) return // no se puede deslizar una foto real mientras la tarjeta de donar está encima
        if (s.remainingSwipesToday <= 0) return

        val photo = s.photos.getOrNull(s.currentIndex) ?: return
        val result = SwipeResult(photo, direction)
        val freedDelta = if (direction == SwipeDirection.LEFT) photo.sizeBytes else 0L
        swipeLimitManager.registerSwipe()

        if (direction == SwipeDirection.RIGHT) {
            currentBucketId?.let { folderProgressManager.addKeptId(it, photo.id) }
        }

        swipesSinceLastDonateCard++
        val shouldShowDonateCard = swipesSinceLastDonateCard >= donateCardThreshold

        _state.value = s.copy(
            currentIndex = s.currentIndex + 1,
            history = s.history + result,
            freedBytes = s.freedBytes + freedDelta,
            remainingSwipesToday = swipeLimitManager.remaining(),
            showDonateCard = shouldShowDonateCard
        )
    }

    /**
     * Descarta la tarjeta de donar (con ✕ o con ✓, ambos casos). Cuenta
     * como un swipe del cupo — la tarjeta "ocupa un lugar" en el mazo,
     * igual que una foto real. Reinicia el conteo y sortea un nuevo umbral
     * para la siguiente aparición.
     */
    fun dismissDonateCard() {
        val s = _state.value
        if (s !is PhotoDeckState.Loaded || !s.showDonateCard) return

        swipeLimitManager.registerSwipe()
        swipesSinceLastDonateCard = 0
        donateCardThreshold = randomDonateThreshold()

        _state.value = s.copy(
            showDonateCard = false,
            remainingSwipesToday = swipeLimitManager.remaining()
        )
    }

    fun undo() {
        val s = _state.value
        if (s !is PhotoDeckState.Loaded || s.history.isEmpty()) return
        val last = s.history.last()
        val freedDelta = if (last.direction == SwipeDirection.LEFT) -last.photo.sizeBytes else 0L

        if (last.direction == SwipeDirection.RIGHT) {
            currentBucketId?.let { folderProgressManager.removeKeptId(it, last.photo.id) }
        }

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
