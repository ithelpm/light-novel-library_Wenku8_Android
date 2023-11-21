package org.mewx.wenku8.global.api

import org.junit.Test

class NovelListWithInfoParserTest {
    @Test
    fun getNovelListWithInfoPageNumInvalid() {
        assertEquals(0, NovelListWithInfoParser.getNovelListWithInfoPageNum("1234"))
    }

    @Test
    fun getNovelListWithInfoPageNum() {
        assertEquals(166, NovelListWithInfoParser.getNovelListWithInfoPageNum(XML))
    }

    @Test
    fun getNovelListWithInfoInvalid() {
        val list: List<NovelListWithInfoParser.NovelListWithInfo?> = NovelListWithInfoParser.getNovelListWithInfo("1234")
        assertTrue(list.isEmpty())
    }

    @Test
    fun getNovelListWithInfo() {
        val list: List<NovelListWithInfoParser.NovelListWithInfo?> = NovelListWithInfoParser.getNovelListWithInfo(XML)
        assertEquals(2, list.size())
        var info: NovelListWithInfoParser.NovelListWithInfo? = list[0]
        assertEquals(1034, info.aid)
        assertEquals(14416, info.fav)
        assertEquals(2316361, info.hit)
        assertEquals(153422, info.push)
        assertEquals("恶魔高校DxD(High School DxD)", info.name)
        info = list[1]
        assertEquals(1035, info.aid)
        assertEquals(789, info.fav)
        assertEquals(1234, info.hit)
        assertEquals(4567, info.push)
        assertEquals("High School DxD", info.name)
    }

    companion object {
        private val XML: String? = """<?xml version="1.0" encoding="utf-8"?>
<result>
<page num='166'/>

<item aid='1034'>
<data name='Title'><![CDATA[恶魔高校DxD(High School DxD)]]></data>
<data name='TotalHitsCount' value='2316361'/>
<data name='PushCount' value='153422'/>
<data name='FavCount' value='14416'/>
<data name='Author' value='xxx1'/>
<data name='BookStatus' value='xxx2'/>
<data name='LastUpdate' value='xxx3'/>
<data name='IntroPreview' value='xxx4'/>
</item>

<item aid='1035'>
<data name='Title'><![CDATA[High School DxD]]></data>
<data name='TotalHitsCount' value='1234'/>
<data name='PushCount' value='4567'/>
<data name='FavCount' value='789'/>
<data name='Author' value='yyy1'/>
<data name='BookStatus' value='yyy2'/>
<data name='LastUpdate' value='yyy3'/>
<data name='IntroPreview' value='yyy4'/>
</item>

</result>"""
    }
}