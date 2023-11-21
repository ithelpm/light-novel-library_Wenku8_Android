package org.mewx.wenku8.global.api

import androidx.test.filters.SmallTest

@SmallTest
class UserInfoTest {
    @Test
    fun parseUserInfo() {
        val ui: UserInfo = UserInfo.parseUserInfo(USER_INFO_XML)
        assertNotNull(ui)
        assertEquals("apptest", ui.username)
        assertEquals("apptest nick", ui.nickyname)
        assertEquals(10, ui.experience)
        assertEquals(100, ui.score)
        assertEquals("新手上路", ui.rank)
    }

    @Test
    fun parseInvalidUserInfo() {
        val ui: UserInfo = UserInfo.parseUserInfo("adfsdfasdfasdf")
        assertNull(ui)
    }

    companion object {
        private val USER_INFO_XML: String? = """<?xml version="1.0" encoding="utf-8"?>
<metadata>
<item name="uname"><![CDATA[apptest]]></item>
<item name="nickname"><![CDATA[apptest nick]]></item>
<item name="score">100</item>
<item name="experience">10</item>
<item name="rank"><![CDATA[新手上路]]></item>
</metadata>"""
    }
}