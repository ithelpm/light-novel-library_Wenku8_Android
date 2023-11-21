package org.mewx.wenku8.global.api

import org.junit.Test

class OldNovelContentParserTest {
    @Test
    fun parseNovelContent() {
        val contents: List<OldNovelContentParser.NovelContent?> = OldNovelContentParser.parseNovelContent(NOVEL_CONTENT, null)
        assertEquals(5, contents.size())
        var tempContent: OldNovelContentParser.NovelContent? = contents[0]
        assertEquals(TEXT, tempContent.type)
        assertEquals("line 1", tempContent.content)
        tempContent = contents[1]
        assertEquals(IMAGE, tempContent.type)
        assertEquals("http://bbbbb.com/pictures/1/1339/90903/107724.jpg", tempContent.content)
        tempContent = contents[2]
        assertEquals(TEXT, tempContent.type)
        assertEquals("line 2", tempContent.content)
        tempContent = contents[3]
        assertEquals(IMAGE, tempContent.type)
        assertEquals("http://bbbbb.com/pictures/1/1339/90903/107725.jpg", tempContent.content)
        tempContent = contents[4]
        assertEquals(TEXT, tempContent.type)
        assertEquals("line 3", tempContent.content)
    }

    @Test
    fun parseNovelContentWithIncompleteImageTag() {
        val contents: List<OldNovelContentParser.NovelContent?> = OldNovelContentParser.parseNovelContent(NOVEL_CONTENt_BROKEN_IMAGE, null)
        assertEquals(5, contents.size())
        var tempContent: OldNovelContentParser.NovelContent? = contents[0]
        assertEquals(TEXT, tempContent.type)
        assertEquals("line 1", tempContent.content)
        tempContent = contents[1]
        assertEquals(IMAGE, tempContent.type)
        assertEquals("http://bbbbb.com/pictures/1/1339/90903/107724.jpg", tempContent.content)
        tempContent = contents[2]
        assertEquals(TEXT, tempContent.type)
        assertEquals("line 2", tempContent.content)
        tempContent = contents[3]
        assertEquals(TEXT, tempContent.type)
        assertEquals("<!--image-->http://bbbbb.com/pictures/1/1339/90903/107725.jpg", tempContent.content)
        tempContent = contents[4]
        assertEquals(TEXT, tempContent.type)
        assertEquals("line 3", tempContent.content)
    }

    @Test
    fun novelContentParser_onlyImage() {
        val contents: List<OldNovelContentParser.NovelContent?> = OldNovelContentParser.NovelContentParser_onlyImage(NOVEL_CONTENT)
        assertEquals(2, contents.size())
        var tempContent: OldNovelContentParser.NovelContent? = contents[0]
        assertEquals(IMAGE, tempContent.type)
        assertEquals("http://bbbbb.com/pictures/1/1339/90903/107724.jpg", tempContent.content)
        tempContent = contents[1]
        assertEquals(IMAGE, tempContent.type)
        assertEquals("http://bbbbb.com/pictures/1/1339/90903/107725.jpg", tempContent.content)
    }

    @Test
    fun novelContentParser_onlyImageBroken() {
        val contents: List<OldNovelContentParser.NovelContent?> = OldNovelContentParser.NovelContentParser_onlyImage(NOVEL_CONTENt_BROKEN_IMAGE)
        assertEquals(1, contents.size())
        val tempContent: OldNovelContentParser.NovelContent? = contents[0]
        assertEquals(IMAGE, tempContent.type)
        assertEquals("http://bbbbb.com/pictures/1/1339/90903/107724.jpg", tempContent.content)
    }

    companion object {
        private val NOVEL_CONTENT: String? = "line 1\r\n" +
                "    <!--image-->http://bbbbb.com/pictures/1/1339/90903/107724.jpg<!--image-->   \r\n" +
                "    line 2     \r\n\r\n" +
                "<!--image-->http://bbbbb.com/pictures/1/1339/90903/107725.jpg<!--image-->\r\n" +
                "line 3\r\n"
        private val NOVEL_CONTENt_BROKEN_IMAGE: String? = "line 1\r\n" +
                "    <!--image-->http://bbbbb.com/pictures/1/1339/90903/107724.jpg<!--image-->   \r\n" +
                "    line 2     \r\n\r\n" +
                "  <!--image-->http://bbbbb.com/pictures/1/1339/90903/107725.jpg \r\n" +
                "line 3\r\n"
    }
}