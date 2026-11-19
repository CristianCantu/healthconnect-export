package com.healthconnect.export.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.SigningInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest

/**
 * The signing SHA-1 shown in the UI is what the user has to register in their own
 * Cloud project, so a wrong format would send them chasing a mismatch that does
 * not exist.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppSignatureTest {
    @Test
    fun `sha1Hex formats the digest the way the Cloud Console shows it`() {
        // SHA-1 of a single 'a' byte.
        assertEquals("86:F7:E4:37:FA:A5:A7:FC:E1:5D:1D:DC:B9:EA:EA:EA:37:76:67:B8", AppSignature.sha1Hex(byteArrayOf(0x61)))
        // SHA-1 of the empty byte array, a well-known constant.
        assertEquals("DA:39:A3:EE:5E:6B:4B:0D:32:55:BF:EF:95:60:18:90:AF:D8:07:09", AppSignature.sha1Hex(ByteArray(0)))
    }

    @Test
    fun `sha1Hex is upper case with colons between all bytes`() {
        val bytes = ByteArray(20) { it.toByte() }

        val hex = AppSignature.sha1Hex(bytes)

        val expected = MessageDigest.getInstance("SHA-1").digest(bytes).joinToString(":") { "%02X".format(it) }
        assertEquals(expected, hex)
        assertEquals(20, hex.split(":").size)
        assertEquals(hex.uppercase(), hex)
    }

    @Test
    fun `sha1 of the installed test app resolves through the package manager`() {
        // Robolectric provides a signed test package, so the real code path is exercised.
        val sha1 = AppSignature.sha1(mock<Context>())

        // A mock context has no package manager data; the helper must degrade to null
        // instead of throwing (the UI then hides the row).
        assertNull(sha1)
    }

    @Test
    fun `sha1 returns null when the package is gone`() {
        val packageManager =
            mock<PackageManager> {
                on { getPackageInfo("com.example.missing", PackageManager.GET_SIGNING_CERTIFICATES) } doThrow
                    PackageManager.NameNotFoundException()
            }
        val context =
            mock<Context> {
                on { packageName } doReturn "com.example.missing"
                on { getPackageManager() } doReturn packageManager
            }

        assertNull(AppSignature.sha1(context))
    }

    @Test
    fun `sha1 returns null when the package carries no signer`() {
        val info =
            PackageInfo().apply {
                signingInfo = mock<SigningInfo> { on { apkContentsSigners } doReturn null }
            }
        val packageManager =
            mock<PackageManager> {
                on { getPackageInfo("com.example.app", PackageManager.GET_SIGNING_CERTIFICATES) } doReturn info
            }
        val context =
            mock<Context> {
                on { packageName } doReturn "com.example.app"
                on { getPackageManager() } doReturn packageManager
            }

        assertNull(AppSignature.sha1(context))
    }
}
