package com.example.nativestarter.domain.assistant

/** Stable skill identities used by Compose, persisted conversation turns, and backend dispatch. */
data class AiSkill(
    val id: String,
    val displayName: String,
    val description: String,
    val executionInstructions: String,
    val icon: SkillIcon,
    val backendOperation: BackendSkillOperation,
    val requiredContext: Set<SkillContext>,
    val availability: SkillAvailability = SkillAvailability.AVAILABLE,
    val limitation: String? = null,
)

enum class SkillIcon {
    MICROPHONE,
    VOICE,
    SPARKLE,
}

enum class BackendSkillOperation {
    AUTOMATIC_SUBTITLES,
    VOICE_CLEANUP_CHAT,
    SCENE_DETECTION,
}

enum class SkillContext {
    SELECTED_VIDEO,
    AI_CLIP_STUDIO_JOB,
}

enum class SkillAvailability {
    AVAILABLE,
    UNAVAILABLE,
}

object AiSkillRegistry {
    val skills: List<AiSkill> = listOf(
        AiSkill(
            id = "automatic_subtitles",
            displayName = "Automatic subtitles",
            description = "Transcribe speech and render captions through AIClipStudio.",
            executionInstructions = "Generate automatic subtitles for the selected video.",
            icon = SkillIcon.MICROPHONE,
            backendOperation = BackendSkillOperation.AUTOMATIC_SUBTITLES,
            requiredContext = setOf(SkillContext.SELECTED_VIDEO, SkillContext.AI_CLIP_STUDIO_JOB),
            limitation = "Subtitle instructions reach the configured Gemini transcription provider. Translation quality depends on that provider and is not guaranteed by the backend.",
        ),
        AiSkill(
            id = "voice_cleanup",
            displayName = "Voice clean-up",
            description = "Ask the AIClipStudio editor to improve voice audio.",
            executionInstructions = "Clean up the voice audio while preserving natural speech.",
            icon = SkillIcon.VOICE,
            backendOperation = BackendSkillOperation.VOICE_CLEANUP_CHAT,
            requiredContext = setOf(SkillContext.SELECTED_VIDEO, SkillContext.AI_CLIP_STUDIO_JOB),
        ),
        AiSkill(
            id = "scene_detection",
            displayName = "Scene detection",
            description = "Detect visual cuts and add their times to the editor timeline.",
            executionInstructions = "Detect scene cuts in the selected video and report their timestamps.",
            icon = SkillIcon.SPARKLE,
            backendOperation = BackendSkillOperation.SCENE_DETECTION,
            requiredContext = setOf(SkillContext.SELECTED_VIDEO, SkillContext.AI_CLIP_STUDIO_JOB),
            limitation = "The current backend detects visual cuts; semantic highlight ranking is not available in this tool.",
        ),
    )

    private val skillsById = skills.associateBy(AiSkill::id)

    fun find(id: String): AiSkill? = skillsById[id]

    fun resolve(ids: Set<String>): List<AiSkill> = skills.filter { it.id in ids }
}

data class AssistantMessage(
    val id: String,
    val role: AssistantMessageRole,
    val content: String,
    val skillId: String? = null,
    val executionState: SkillExecutionState? = null,
    val retryText: String? = null,
    val retrySkillIds: Set<String> = emptySet(),
)

enum class AssistantMessageRole {
    USER,
    ASSISTANT,
    TOOL,
}

enum class SkillExecutionState {
    RUNNING,
    COMPLETED,
    FAILED,
}
