package com.draco.comatose

import com.draco.comatose.repositories.profiles.ProfileCatalog
import com.draco.comatose.repositories.profiles.ProfileId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileCatalogTest {
    @Test
    fun everyProfileIdentifiesAsItself() {
        ProfileId.values().forEach {
            assertEquals(it, ProfileCatalog.identify(ProfileCatalog.constantsOf(it)))
        }
    }

    @Test
    fun blankValueMeansDefault() {
        assertEquals(ProfileId.DEFAULT, ProfileCatalog.identify(null))
        assertEquals(ProfileId.DEFAULT, ProfileCatalog.identify(""))
        assertEquals(ProfileId.DEFAULT, ProfileCatalog.identify("   "))
    }

    @Test
    fun orderingAndWhitespaceDoNotMatter() {
        val light = ProfileCatalog.constantsOf(ProfileId.LIGHT)!!
        val shuffled = light.split(',').reversed().joinToString(" , ")
        assertEquals(ProfileId.LIGHT, ProfileCatalog.identify(shuffled))
    }

    @Test
    fun modifiedOrForeignValuesMatchNothing() {
        val light = ProfileCatalog.constantsOf(ProfileId.LIGHT)!!
        assertNull(ProfileCatalog.identify("$light,foo=1"))
        assertNull(ProfileCatalog.identify(light.replace("location_accuracy=50", "location_accuracy=51")))
    }

    @Test
    fun profilesAreDistinctAndWellFormed() {
        val parsed = ProfileId.values().map { ProfileCatalog.parse(ProfileCatalog.constantsOf(it)) }
        assertEquals(parsed.size, parsed.toSet().size)
        parsed.flatMap { it.entries }.forEach {
            assertTrue("${it.key}=${it.value}", it.key.isNotEmpty() && it.value.toDoubleOrNull() != null)
        }
    }
}
