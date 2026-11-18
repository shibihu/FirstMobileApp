package com.example.nativestarter.data.caption

/**
 * UI snapshot for the active automatic-subtitle job shown in the AI Studio
 * card. [progress] is the backend-reported percentage; null means the backend
 * has not reported progress yet and the UI should show an indeterminate bar.
 */
data class CaptionJobUiState(
    val stage: CaptionStage? = null,
    val message: String = "",
    val progress: Int? = null,
    val cancellable: Boolean = false,
    val retryable: Boolean = false,
    val finished: Boolean = false,
)
