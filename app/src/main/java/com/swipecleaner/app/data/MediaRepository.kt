package com.swipecleaner.app.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.swipecleaner.app.domain.BucketFolder
import com.swipecleaner.app.domain.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaRepository(private val context: Context) {

    private val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME
    )

    /**
     * Agrupa todas las imágenes del dispositivo por carpeta (bucket).
     * Equivalente a un GROUP BY bucket_id hecho manualmente sobre el cursor,
     * ya que MediaStore no soporta agregación nativa multiplataforma de forma simple.
     */
    suspend fun getFolders(): List<BucketFolder> = withContext(Dispatchers.IO) {
        val map = LinkedHashMap<String, MutableList<Pair<String, Long>>>() // bucketId -> (name, size)

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val bucketId = cursor.getString(bucketIdCol) ?: continue
                val bucketName = cursor.getString(bucketNameCol) ?: "Sin nombre"
                val size = cursor.getLong(sizeCol)
                map.getOrPut(bucketId) { mutableListOf() }.add(bucketName to size)
            }
        }

        map.map { (bucketId, entries) ->
            BucketFolder(
                bucketId = bucketId,
                name = entries.first().first,
                photoCount = entries.size,
                totalSizeBytes = entries.sumOf { it.second }
            )
        }.sortedByDescending { it.totalSizeBytes }
    }

    /**
     * Devuelve las fotos de una carpeta específica, más recientes primero.
     */
    suspend fun getPhotosInFolder(bucketId: String): List<PhotoItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<PhotoItem>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.Images.Media.BUCKET_ID} = ?"
        val selectionArgs = arrayOf(bucketId)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                result += PhotoItem(
                    id = id,
                    uri = uri,
                    displayName = cursor.getString(nameCol) ?: "",
                    sizeBytes = cursor.getLong(sizeCol),
                    dateAddedMillis = cursor.getLong(dateCol) * 1000L,
                    bucketId = cursor.getString(bucketIdCol) ?: bucketId
                )
            }
        }
        result
    }
}
