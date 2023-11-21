package org.mewx.wenku8.global.api

import org.junit.Test

class NovelItemInfoUpdateTest {
    @Test
    fun convertFromMeta() {
        val META_XML = """<?xml version="1.0" encoding="utf-8"?>
<metadata>
<data name="Title" aid="1306"><![CDATA[向森之魔物献上花束(向森林的魔兽少女献花)]]></data>
<data name="Author" value="小木君人"/>
<data name="DayHitsCount" value="26"/>
<data name="TotalHitsCount" value="43984"/>
<data name="PushCount" value="1735"/>
<data name="FavCount" value="848"/>
<data name="PressId" value="小学馆" sid="10"/>
<data name="BookStatus" value="已完成"/>
<data name="BookLength" value="105985"/>
<data name="LastUpdate" value="2012-11-02"/>
<data name="LatestSection" cid="41897"><![CDATA[第一卷 插图]]></data>
</metadata>"""
        val meta: NovelItemMeta = Wenku8Parser.parseNovelFullMeta(META_XML)
        assertNotNull(meta)
        val info: NovelItemInfoUpdate = NovelItemInfoUpdate.convertFromMeta(meta)
        assertEquals("向森之魔物献上花束(向森林的魔兽少女献花)", info.title)
        assertEquals(1306, info.aid)
        assertEquals("小木君人", info.author)
        assertEquals("已完成", info.status)
        assertEquals("2012-11-02", info.update)
        assertEquals("第一卷 插图", info.latest_chapter)
    }

    @Test
    fun parseNovelItemInfoUpdate() {
        val XML = """<?xml version="1.0" encoding="utf-8"?>
<metadata>
<data name="Title" aid="1305"><![CDATA[绝对双刃absolute duo]]></data>
<data name="Author" value="柊★巧"/>
<data name="BookStatus" value="连载中"/>
<data name="LastUpdate" value="2014-10-01"/>
<data
name="IntroPreview"><![CDATA[　　「焰牙」——那是藉由超化之后的精神力将自身灵...]]></data>
</metadata>"""
        val info: NovelItemInfoUpdate = NovelItemInfoUpdate.parse(XML)
        assertNotNull(info)
        assertEquals("绝对双刃absolute duo", info.title)
        assertEquals(1305, info.aid)
        assertEquals("柊★巧", info.author)
        assertEquals("连载中", info.status)
        assertEquals("2014-10-01", info.update)
        assertEquals("「焰牙」——那是藉由超化之后的精神力将自身灵...", info.intro_short)
    }

    @Test
    fun parseNovelItemInfoUpdateInvalid() {
        val info: NovelItemInfoUpdate = NovelItemInfoUpdate.parse("1234")
        assertNull(info)
    }
}