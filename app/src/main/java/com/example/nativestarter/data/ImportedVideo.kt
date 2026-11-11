package com.example.nativestarter.data

import android.net.Uri

data class ImportedVideo(
    val uri: Uri,
    val displayName: String,
    val durationMs: Long?,
    val width: Int?,
    val height: Int?,
)