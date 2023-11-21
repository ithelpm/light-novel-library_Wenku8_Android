package android.utilimport

kotlin.Throws
/**
 * Faking the Android API.
 *
 *
 * From: [How to mock Base64 in Android?](https://stackoverflow.com/a/60318356/4206925)
 */
object Base64 {
    fun encodeToString(input: ByteArray?, flags: Int): String? {
        return java.util.Base64.getEncoder().encodeToString(input)
    }

    fun decode(str: String?, flags: Int): ByteArray? {
        return java.util.Base64.getDecoder().decode(str)
    } // add other methods if required...
}