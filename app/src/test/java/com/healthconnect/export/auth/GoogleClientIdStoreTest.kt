package com.healthconnect.export.auth

import android.content.Context
import android.content.SharedPreferences
import com.healthconnect.export.testing.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class GoogleClientIdStoreTest {
    private val prefs: SharedPreferences = FakeSharedPreferences()
    private val context =
        mock<Context> {
            on { getSharedPreferences(GoogleClientIdStore.PREFS_NAME, Context.MODE_PRIVATE) } doReturn prefs
        }

    private val store = GoogleClientIdStore(context)

    private val customId = "123456789012-abcdefghijklmnop.apps.googleusercontent.com"

    @Test
    fun `bundled client id is used until the user sets one`() {
        assertNull(store.custom())
        assertFalse(store.hasCustom())
        assertEquals(GoogleClientIdStore.DEFAULT_CLIENT_ID, store.current())
    }

    @Test
    fun `custom client id replaces the bundled one`() {
        store.save(customId)

        assertEquals(customId, store.custom())
        assertTrue(store.hasCustom())
        assertEquals(customId, store.current())
    }

    @Test
    fun `save trims what a Console copy-paste adds`() {
        store.save("  https://123456789012-abcdefghijklmnop.apps.googleusercontent.com/  ")

        assertEquals(customId, store.custom())
    }

    @Test
    fun `saving the bundled client id clears the override`() {
        store.save(customId)
        store.save(GoogleClientIdStore.DEFAULT_CLIENT_ID)

        assertNull(store.custom())
        assertEquals(GoogleClientIdStore.DEFAULT_CLIENT_ID, store.current())
    }

    @Test
    fun `saving a blank value clears the override`() {
        store.save(customId)
        store.save("   ")

        assertNull(store.custom())
    }

    @Test
    fun `clear restores the bundled client id`() {
        store.save(customId)
        store.clear()

        assertNull(store.custom())
        assertEquals(GoogleClientIdStore.DEFAULT_CLIENT_ID, store.current())
    }

    @Test
    fun `a context without preferences falls back to the bundled client id`() {
        val storeWithoutPrefs = GoogleClientIdStore(mock<Context>())

        assertNull(storeWithoutPrefs.custom())
        assertEquals(GoogleClientIdStore.DEFAULT_CLIENT_ID, storeWithoutPrefs.current())
        // Saving must not crash, it just does not persist.
        storeWithoutPrefs.save(customId)
        assertNull(storeWithoutPrefs.custom())
    }

    @Test
    fun `only web application client ids are accepted`() {
        assertTrue(GoogleClientIdStore.isValid(customId))
        assertTrue(GoogleClientIdStore.isValid("  $customId  "))
        assertTrue(GoogleClientIdStore.isValid("https://$customId/"))
        // An Android/iOS client ID, a bare project number or a random string.
        assertFalse(GoogleClientIdStore.isValid("123456789012"))
        assertFalse(GoogleClientIdStore.isValid("123456789012-abcdef.apps.googleusercontent.com.example.com"))
        assertFalse(GoogleClientIdStore.isValid("abcd-abcdef.apps.googleusercontent.com"))
        assertFalse(GoogleClientIdStore.isValid(""))
    }

    @Test
    fun `normalize keeps a valid id untouched`() {
        assertEquals(customId, GoogleClientIdStore.normalize(" $customId "))
        assertEquals(customId, GoogleClientIdStore.normalize("https://$customId/"))
        assertEquals(customId, GoogleClientIdStore.normalize("http://$customId"))
    }

    @Test
    fun `stored value is read back through the preferences`() {
        prefs.edit().putString("client_id", customId).apply()

        assertEquals(customId, store.current())
    }

    @Test
    fun `a stored blank value is ignored`() {
        prefs.edit().putString("client_id", "   ").apply()

        assertNull(store.custom())
        assertEquals(GoogleClientIdStore.DEFAULT_CLIENT_ID, store.current())
    }
}
