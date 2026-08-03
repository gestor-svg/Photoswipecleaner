package com.swipecleaner.app.domain

import android.net.Uri

/**
 * Representa una foto individual leída desde MediaStore.
 */
data class PhotoItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val dateAddedMillis: Long,
    val bucketId: String
)

/**
 * Representa una carpeta (bucket) de la galería, agrupada por MediaStore.
 */
data class BucketFolder(
    val bucketId: String,
    val name: String,
    val photoCount: Int,
    val totalSizeBytes: Long
)

enum class SwipeDirection { LEFT, RIGHT }

data class SwipeResult(
    val photo: PhotoItem,
    val direction: SwipeDirection
)
