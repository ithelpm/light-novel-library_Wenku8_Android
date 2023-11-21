package org.mewx.wenku8.util

import androidx.test.filters.SmallTest

@SmallTest
class LightUserSessionTest {
    @Test
    fun decodeThenEncodeUserInfo() {
        val src = "Z1NxZFhlPT0=|Z1dwUlhiPT0="
        LightUserSession.decAndSetUserFile(src)
        assertEquals("abc", LightUserSession.getUsernameOrEmail())
        assertEquals("123", LightUserSession.getPassword())
        assertEquals(src, LightUserSession.encUserFile())
    }

    @Test
    fun encodeThenDecodeUserInfo() {
        LightUserSession.setUserInfo("xyz", "987")
        LightUserSession.decAndSetUserFile(LightUserSession.encUserFile())
        assertEquals("xyz", LightUserSession.getUsernameOrEmail())
        assertEquals("987", LightUserSession.getPassword())
    }
}