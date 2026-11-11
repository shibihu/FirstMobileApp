package com.example.nativestarter.domain.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiSkillRegistryTest {

    @Test
    fun initialSkillsHaveStableIdsAndExplicitBackendOperations() {
        assertEquals(
            listOf("automatic_subtitles", "voice_cleanup", "scene_detection"),
            AiSkillRegistry.skills.map(AiSkill::id),
        )
        assertEquals(
            listOf(
                BackendSkillOperation.AUTOMATIC_SUBTITLES,
                BackendSkillOperation.VOICE_CLEANUP_CHAT,
                BackendSkillOperation.SCENE_DETECTION,
            ),
            AiSkillRegistry.skills.map(AiSkill::backendOperation),
        )
        assertTrue(AiSkillRegistry.skills.all { SkillContext.SELECTED_VIDEO in it.requiredContext })
    }

    @Test
    fun registryResolvesOnlyKnownSkillsInRegistryOrder() {
        val selected = AiSkillRegistry.resolve(setOf("scene_detection", "unknown", "automatic_subtitles"))

        assertEquals(listOf("automatic_subtitles", "scene_detection"), selected.map(AiSkill::id))
        assertNotNull(AiSkillRegistry.find("voice_cleanup"))
        assertNull(AiSkillRegistry.find("future_unimplemented_skill"))
    }
}
