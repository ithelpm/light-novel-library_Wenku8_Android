package org.mewx.wenku8.util

import android.util.Base64

/**
 * Light Base64
 * *
 * This class achieve the basic base64 en/decryption:
 * use "Base64.DEFAULT" to encrypt or decrypt text.
 */
object LightBase64 {
    @NonNull
    fun EncodeBase64(@NonNull b: ByteArray?): String? {
        return Base64.encodeToString(b, Base64.DEFAULT).trim()
    }

    @NonNull
    fun EncodeBase64(@NonNull s: String?): String? {
        return EncodeBase64(s.getBytes(Charset.forName("UTF-8")))
    }

    @NonNull
    fun DecodeBase64(@NonNull s: String?): ByteArray? {
        return try {
            val b: ByteArray
            b = Base64.decode(s, Base64.DEFAULT)
            b
        } catch (e: IllegalArgumentException) {
            ByteArray(0)
        }
    }

    @NonNull
    fun DecodeBase64String(@NonNull s: String?): String? {
        return String(DecodeBase64(s), Charset.forName("UTF-8"))
    }
}