package com.healthconnect.export.auth

import android.content.Context
import android.content.SharedPreferences
import com.healthconnect.export.BuildConfig

/**
 * The OAuth "server" (Web application) client ID handed to Credential Manager.
 *
 * The released APK is tied to the bundled Cloud project, and its Drive scope
 * (`drive.file`, a restricted scope) cannot be granted to arbitrary users: that
 * needs OAuth verification plus a paid annual security assessment. The client ID
 * is the only project-specific value the app itself consumes, so it is replaceable
 * at runtime — a user registers this package + the APK's signing SHA-1 in their own
 * Cloud project, pastes the Web client ID into the app and signs in against their
 * own project, without rebuilding anything.
 */
class GoogleClientIdStore(
    context: Context,
) {
    // Nullable so a Context without SharedPreferences support (e.g. a test stub)
    // falls back to the bundled client ID instead of crashing.
    private val prefs: SharedPreferences? = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Client ID in effect: the custom one when the user set it, else the bundled default. */
    fun current(): String = custom() ?: DEFAULT_CLIENT_ID

    /** The client ID the user entered, or null while the bundled one is in use. */
    fun custom(): String? = prefs?.getString(KEY_CLIENT_ID, null)?.takeIf { it.isNotBlank() }

    /** True when a custom client ID replaces the bundled one. */
    fun hasCustom(): Boolean = custom() != null

    /**
     * Stores a custom client ID. Blank input, or the bundled ID itself, restores
     * the default so the setting never points at the same project twice.
     */
    fun save(clientId: String) {
        val normalized = normalize(clientId)
        if (normalized.isEmpty() || normalized == DEFAULT_CLIENT_ID) {
            clear()
        } else {
            prefs?.edit()?.putString(KEY_CLIENT_ID, normalized)?.apply()
        }
    }

    /** Restores the bundled client ID. */
    fun clear() {
        prefs?.edit()?.remove(KEY_CLIENT_ID)?.apply()
    }

    companion object {
        const val PREFS_NAME = "google_client_id"
        private const val KEY_CLIENT_ID = "client_id"

        /** Web application client ID of the bundled Cloud project. */
        val DEFAULT_CLIENT_ID: String = BuildConfig.GOOGLE_CLIENT_ID

        /** Strips whitespace plus the `https://` prefix and trailing slash a Console copy-paste adds. */
        fun normalize(raw: String): String =
            raw
                .trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .removeSuffix("/")
                .trim()

        /** A Web application OAuth client ID looks like `730530422387-abc123.apps.googleusercontent.com`. */
        fun isValid(clientId: String): Boolean = CLIENT_ID_PATTERN.matches(normalize(clientId))

        private val CLIENT_ID_PATTERN = Regex("""^\d{6,}-[0-9A-Za-z_-]+\.apps\.googleusercontent\.com$""")
    }
}
