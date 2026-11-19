package com.healthconnect.export.util

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.security.MessageDigest

/**
 * Identity of the installed APK as Google sees it.
 *
 * Registering an Android OAuth client needs the package name and the SHA-1 of the
 * signing certificate, so the app shows both: the user can set up their own Cloud
 * project for the APK they installed without rebuilding it and without running
 * `keytool` or `apksigner` on a machine that does not have the keystore.
 */
object AppSignature {
    private const val TAG = "AppSignature"

    /** Package name the OAuth client has to use. */
    fun packageName(context: Context): String = context.packageName

    /** SHA-1 of the signing certificate, `AA:BB:…` (as the Cloud Console shows it), or null. */
    fun sha1(context: Context): String? =
        try {
            val info =
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES,
                )
            val signers = info.signingInfo?.apkContentsSigners
            val certificate = signers?.firstOrNull()
            if (certificate == null) {
                Log.w(TAG, "sha1: no signing certificate on the package info")
                null
            } else {
                sha1Hex(certificate.toByteArray())
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "sha1: package not found", e)
            null
        } catch (e: Exception) {
            Log.w(TAG, "sha1: unable to read the signing certificate", e)
            null
        }

    /** Upper-case, colon-separated SHA-1 of [bytes]. */
    internal fun sha1Hex(bytes: ByteArray): String =
        MessageDigest
            .getInstance("SHA-1")
            .digest(bytes)
            .joinToString(":") { "%02X".format(it) }
}
