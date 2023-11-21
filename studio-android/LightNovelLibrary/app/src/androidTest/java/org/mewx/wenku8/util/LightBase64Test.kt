package org.mewx.wenku8.util

import androidx.test.filters.SmallTest

@SmallTest
class LightBase64Test {
    private val source: ByteArray?
    private val sourceStr: String? = "foobar"
    private val encodedStr: String? = "Zm9vYmFy"

    init {
        source = byteArrayOf(0x66, 0x6f, 0x6f, 0x62, 0x61, 0x72)
    }

    @Test
    fun encodeBase64ByteToString() {
        assertEquals(encodedStr, LightBase64.EncodeBase64(source))
    }

    @Test
    fun encodeBase64ByteToStringEmpty() {
        assertTrue(LightBase64.EncodeBase64(ByteArray(0)).isEmpty())
    }

    @Test
    fun encodeBase64StringToStringEmpty() {
        assertTrue(LightBase64.EncodeBase64("").isEmpty())
    }

    @Test
    fun decodeBase64StringToString() {
        assertEquals(sourceStr, LightBase64.DecodeBase64String(encodedStr))
    }

    @Test
    fun decodeBase64StringToStringInvalid() {
        assertTrue(LightBase64.DecodeBase64String("!@#$%^&*()_+").isEmpty())
    }

    @Test
    fun decodeBase64StringToByte() {
        assertArrayEquals(source, LightBase64.DecodeBase64(encodedStr))
    }

    @Test
    fun decodeBase64StringToByteInvalid() {
        assertEquals(0, LightBase64.DecodeBase64("!@#$%^&*()_+").length)
    }
}