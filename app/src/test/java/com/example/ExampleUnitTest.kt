package com.example

import com.example.core.audio.AudioEffectType
import com.example.core.call.CharacterRepository
import com.example.core.call.SupportedLanguage
import com.example.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testScreenBottomNavItemsNotNull() {
        val navItems = Screen.bottomNavItems
        assertEquals(5, navItems.size)
        navItems.forEach { screen ->
            assertNotNull(screen)
            assertNotNull(screen.route)
            assertTrue(screen.route.isNotBlank())
            assertTrue(screen.title.isNotBlank())
        }
    }

    @Test
    fun testAudioEffectsValid() {
        val effects = AudioEffectType.entries
        assertTrue(effects.size >= 8)
        effects.forEach { effect ->
            assertTrue(effect.basePitch in 0.4f..2.5f)
            assertTrue(effect.baseSpeed in 0.4f..2.5f)
        }
    }

    @Test
    fun testCharacterRepositoryValid() {
        val characters = CharacterRepository.characters
        assertTrue(characters.size >= 5)
        characters.forEach { char ->
            assertTrue(char.name.isNotBlank())
            assertTrue(char.greetingEnglish.isNotBlank())
            assertTrue(char.greetingHindi.isNotBlank())
            assertTrue(char.greetingHinglish.isNotBlank())
            assertTrue(char.supportedLanguages.contains(SupportedLanguage.ENGLISH))
        }
    }
}
