package org.mewx.wenku8.global.api

import androidx.test.filters.SmallTest

@SmallTest
class Wenku8ParserTest {
    private val REVIEW_LIST_XML: String? = """<?xml version="1.0" encoding="utf-8"?>
<metadata>
<page num='12'/>

<item rid='79800' posttime='20130525171631' replies='1' replytime='20130528184916'>
<user uid='81669'><![CDATA[老衲0轻音]]></user>
<content><![CDATA[前排……]]></content>
</item>

<item rid='79826' posttime='20130525232002' replies='4' replytime='20130527234259'>
<user uid='34924'><![CDATA[冒险奏鸣]]></user>
<content><![CDATA[有种神曲奏界的既视感]]></content>
</item>

</metadata>"""
    private val REVIEW_REPLY_LIST_XML: String? = """<?xml version="1.0" encoding="utf-8"?>
<metadata>
<page num='2'/>
<item timestamp='20180713101811'>
<user uid='233516'><![CDATA[a1b2c3d4]]></user>
<content><![CDATA[嗯…………至少是一樓發完]]></content>
</item>

<item timestamp='20180713135735'>
<user uid='230041'><![CDATA[156126]]></user>
<content><![CDATA[滑稽✧(`ῧ′)机智]]></content>
</item>

</metadata>"""

    @Test
    fun testParseNovelItemList() {
        val XML = """<?xml version="1.0" encoding="utf-8"?>
<result>
<page num='166'/>
<item aid='1143'/>
<item aid='1034'/>
<item aid='1213'/>
<item aid='1'/>
<item aid='1011'/>
<item aid='1192'/>
<item aid='433'/>
<item aid='47'/>
<item aid='7'/>
<item aid='374'/>
</result>"""
        val list: List<Integer?> = Wenku8Parser.parseNovelItemList(XML)
        assertEquals(Arrays.asList(166, 1143, 1034, 1213, 1, 1011, 1192, 433, 47, 7, 374), list)
    }

    @Test
    fun testParseNovelItemListInvalid() {
        val list: List<Integer?> = Wenku8Parser.parseNovelItemList("1234")
        assertTrue(list.isEmpty())
    }

    @Test
    fun testParseNovelFullMeta() {
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
        assertEquals(1306, meta.aid)
        assertEquals("向森之魔物献上花束(向森林的魔兽少女献花)", meta.title)
        assertEquals("小木君人", meta.author)
        assertEquals(26, meta.dayHitsCount)
        assertEquals(43984, meta.totalHitsCount)
        assertEquals(1735, meta.pushCount)
        assertEquals(848, meta.favCount)
        assertEquals("小学馆", meta.pressId)
        assertEquals("已完成", meta.bookStatus)
        assertEquals(105985, meta.bookLength)
        assertEquals("2012-11-02", meta.lastUpdate)
        assertEquals(41897, meta.latestSectionCid)
        assertEquals("第一卷 插图", meta.latestSectionName)
    }

    @Test
    fun testParseNovelFullMetaInvalid() {
        val meta: NovelItemMeta = Wenku8Parser.parseNovelFullMeta("1234")
        assertNull(meta)
    }

    @Test
    fun testGetVolumeList() {
        val XML = """<?xml version="1.0" encoding="utf-8"?>
<package>
<volume vid="41748"><![CDATA[第一卷 告白于苍刻之夜]]>
<chapter cid="41749"><![CDATA[序章]]></chapter>
<chapter cid="41750"><![CDATA[第一章「去对我的『楯』说吧——」]]></chapter>
<chapter cid="41751"><![CDATA[第二章「我真的对你非常感兴趣」]]></chapter>
<chapter cid="41752"><![CDATA[第三章「揍我吧！」]]></chapter>
<chapter cid="41753"><![CDATA[第四章「下次，再来喝苹果茶」]]></chapter>
<chapter cid="41754"><![CDATA[第五章「这是约定」]]></chapter>
<chapter cid="41755"><![CDATA[第六章「你的背后——由我来守护！」]]></chapter>
<chapter cid="41756"><![CDATA[第七章「茱莉——爱交给你！」]]></chapter>
<chapter cid="41757"><![CDATA[尾声]]></chapter>
<chapter cid="41758"><![CDATA[后记]]></chapter>
<chapter cid="41759"><![CDATA[插图]]></chapter>
</volume>
<volume vid="45090"><![CDATA[第二卷 谎言、真相与赤红]]>
<chapter cid="45091"><![CDATA[序章]]></chapter>
<chapter cid="45092"><![CDATA[第一章「莉莉丝·布里斯托」]]></chapter>
</volume>
</package>"""
        val volumeLists: List<VolumeList?> = Wenku8Parser.getVolumeList(XML)
        assertEquals(2, volumeLists.size())

        // ----
        var vList: VolumeList? = volumeLists[0]
        assertEquals(41748, vList.vid)
        assertEquals("第一卷 告白于苍刻之夜", vList.volumeName)
        assertEquals(11, vList.chapterList.size())
        var cInfo: ChapterInfo = vList.chapterList.get(0)
        assertEquals(41749, cInfo.cid)
        assertEquals("序章", cInfo.chapterName)
        cInfo = vList.chapterList.get(1)
        assertEquals(41750, cInfo.cid)
        assertEquals("第一章「去对我的『楯』说吧——」", cInfo.chapterName)
        cInfo = vList.chapterList.get(2)
        assertEquals(41751, cInfo.cid)
        assertEquals("第二章「我真的对你非常感兴趣」", cInfo.chapterName)
        cInfo = vList.chapterList.get(3)
        assertEquals(41752, cInfo.cid)
        assertEquals("第三章「揍我吧！」", cInfo.chapterName)
        cInfo = vList.chapterList.get(4)
        assertEquals(41753, cInfo.cid)
        assertEquals("第四章「下次，再来喝苹果茶」", cInfo.chapterName)
        cInfo = vList.chapterList.get(5)
        assertEquals(41754, cInfo.cid)
        assertEquals("第五章「这是约定」", cInfo.chapterName)
        cInfo = vList.chapterList.get(6)
        assertEquals(41755, cInfo.cid)
        assertEquals("第六章「你的背后——由我来守护！」", cInfo.chapterName)
        cInfo = vList.chapterList.get(7)
        assertEquals(41756, cInfo.cid)
        assertEquals("第七章「茱莉——爱交给你！」", cInfo.chapterName)
        cInfo = vList.chapterList.get(8)
        assertEquals(41757, cInfo.cid)
        assertEquals("尾声", cInfo.chapterName)
        cInfo = vList.chapterList.get(9)
        assertEquals(41758, cInfo.cid)
        assertEquals("后记", cInfo.chapterName)
        cInfo = vList.chapterList.get(10)
        assertEquals(41759, cInfo.cid)
        assertEquals("插图", cInfo.chapterName)


        // ----
        vList = volumeLists[1]
        assertEquals(45090, vList.vid)
        assertEquals("第二卷 谎言、真相与赤红", vList.volumeName)
        assertEquals(2, vList.chapterList.size())
        cInfo = vList.chapterList.get(0)
        assertEquals(45091, cInfo.cid)
        assertEquals("序章", cInfo.chapterName)
        cInfo = vList.chapterList.get(1)
        assertEquals(45092, cInfo.cid)
        assertEquals("第一章「莉莉丝·布里斯托」", cInfo.chapterName)
    }

    @Test
    fun testGetVolumeListInvalid() {
        val volumeLists: List<VolumeList?> = Wenku8Parser.getVolumeList("1234")
        assertTrue(volumeLists.isEmpty())
    }

    @Test
    fun testParseReviewList() {
        val reviewList = ReviewList()
        Wenku8Parser.parseReviewList(reviewList, REVIEW_LIST_XML)
        assertEquals(reviewList.getTotalPage(), 12)
        assertEquals(reviewList.getCurrentPage(), 1)
        assertEquals(reviewList.getList().size(), 2)
        var review: ReviewList.Review = reviewList.getList().get(0)
        assertEquals(review.getRid(), 79800)
        assertEquals(review.getPostTime().getTime(),
                GregorianCalendar(2013, Calendar.MAY, 25, 17, 16, 31).getTime().getTime())
        assertEquals(review.getNoReplies(), 1)
        assertEquals(review.getLastReplyTime().getTime(),
                GregorianCalendar(2013, Calendar.MAY, 28, 18, 49, 16).getTime().getTime())
        assertEquals(review.getUid(), 81669)
        assertEquals(review.getUserName(), "老衲0轻音")
        assertEquals(review.getTitle(), "前排……")
        review = reviewList.getList().get(1)
        assertEquals(review.getRid(), 79826)
        assertEquals(review.getPostTime().getTime(),
                GregorianCalendar(2013, Calendar.MAY, 25, 23, 20, 2).getTime().getTime())
        assertEquals(review.getNoReplies(), 4)
        assertEquals(review.getLastReplyTime().getTime(),
                GregorianCalendar(2013, Calendar.MAY, 27, 23, 42, 59).getTime().getTime())
        assertEquals(review.getUid(), 34924)
        assertEquals(review.getUserName(), "冒险奏鸣")
        assertEquals(review.getTitle(), "有种神曲奏界的既视感")
    }

    @Test
    fun testParseReviewListInvalid() {
        val reviewList = ReviewList()
        Wenku8Parser.parseReviewList(reviewList, "1324")
        assertTrue(reviewList.getList().isEmpty())
    }

    @Test
    fun testParseReviewListPageTwo() {
        val REVIEW_LIST_XML_PAGE_2 = """<?xml version="1.0" encoding="utf-8"?>
<metadata>
<page num='12'/>

<item rid='79801' posttime='20130525171631' replies='1' replytime='20130528184916'>
<user uid='81670'><![CDATA[老衲0轻音]]></user>
<content><![CDATA[前排……]]></content>
</item>

<item rid='79827' posttime='20130525232002' replies='4' replytime='20130527234259'>
<user uid='34925'><![CDATA[冒险奏鸣]]></user>
<content><![CDATA[有种神曲奏界的既视感]]></content>
</item>

</metadata>"""
        val reviewList = ReviewList()
        Wenku8Parser.parseReviewList(reviewList, REVIEW_LIST_XML)
        Wenku8Parser.parseReviewList(reviewList, REVIEW_LIST_XML_PAGE_2)
        assertEquals(reviewList.getTotalPage(), 12)
        assertEquals(reviewList.getCurrentPage(), 2)
        assertEquals(reviewList.getList().size(), 4)
        var review: ReviewList.Review = reviewList.getList().get(0)
        assertEquals(review.getRid(), 79800)
        assertEquals(review.getPostTime().getTime(),
                GregorianCalendar(2013, Calendar.MAY, 25, 17, 16, 31).getTime().getTime())
        assertEquals(review.getNoReplies(), 1)
        assertEquals(review.getLastReplyTime().getTime(),
                GregorianCalendar(2013, Calendar.MAY, 28, 18, 49, 16).getTime().getTime())
        assertEquals(review.getUid(), 81669)
        assertEquals(review.getUserName(), "老衲0轻音")
        assertEquals(review.getTitle(), "前排……")
        review = reviewList.getList().get(1)
        assertEquals(review.getRid(), 79826)
        assertEquals(review.getPostTime().getTime(),
                GregorianCalendar(2013, Calendar.MAY, 25, 23, 20, 2).getTime().getTime())
        assertEquals(review.getNoReplies(), 4)
        assertEquals(review.getLastReplyTime().getTime(),
                GregorianCalendar(2013, Calendar.MAY, 27, 23, 42, 59).getTime().getTime())
        assertEquals(review.getUid(), 34924)
        assertEquals(review.getUserName(), "冒险奏鸣")
        assertEquals(review.getTitle(), "有种神曲奏界的既视感")
        review = reviewList.getList().get(2)
        assertEquals(review.getRid(), 79801)
        assertEquals(review.getPostTime().getTime(),
                GregorianCalendar(2013, Calendar.MAY, 25, 17, 16, 31).getTime().getTime())
        assertEquals(review.getNoReplies(), 1)
        assertEquals(review.getLastReplyTime().getTime(),
                GregorianCalendar(2013, Calendar.MAY, 28, 18, 49, 16).getTime().getTime())
        assertEquals(review.getUid(), 81670)
        assertEquals(review.getUserName(), "老衲0轻音")
        assertEquals(review.getTitle(), "前排……")
        review = reviewList.getList().get(3)
        assertEquals(review.getRid(), 79827)
        assertEquals(review.getPostTime().getTime(),
                GregorianCalendar(2013, Calendar.MAY, 25, 23, 20, 2).getTime().getTime())
        assertEquals(review.getNoReplies(), 4)
        assertEquals(review.getLastReplyTime().getTime(),
                GregorianCalendar(2013, Calendar.MAY, 27, 23, 42, 59).getTime().getTime())
        assertEquals(review.getUid(), 34925)
        assertEquals(review.getUserName(), "冒险奏鸣")
        assertEquals(review.getTitle(), "有种神曲奏界的既视感")
    }

    @Test
    fun testParseReviewReplyList() {
        val reviewReplyList = ReviewReplyList()
        Wenku8Parser.parseReviewReplyList(reviewReplyList, REVIEW_REPLY_LIST_XML)
        assertEquals(reviewReplyList.getTotalPage(), 2)
        assertEquals(reviewReplyList.getCurrentPage(), 1)
        assertEquals(reviewReplyList.getList().size(), 2)
        var reviewReply: ReviewReplyList.ReviewReply = reviewReplyList.getList().get(0)
        assertEquals(reviewReply.getReplyTime().getTime(),
                GregorianCalendar(2018, Calendar.JULY, 13, 10, 18, 11).getTime().getTime())
        assertEquals(reviewReply.getUid(), 233516)
        assertEquals(reviewReply.getUserName(), "a1b2c3d4")
        assertEquals(reviewReply.getContent(), "嗯…………至少是一樓發完")
        reviewReply = reviewReplyList.getList().get(1)
        assertEquals(reviewReply.getReplyTime().getTime(),
                GregorianCalendar(2018, Calendar.JULY, 13, 13, 57, 35).getTime().getTime())
        assertEquals(reviewReply.getUid(), 230041)
        assertEquals(reviewReply.getUserName(), "156126")
        assertEquals(reviewReply.getContent(), "滑稽✧(`ῧ′)机智")
    }

    @Test
    fun testParseReviewReplyListInvalid() {
        val reviewReplyList = ReviewReplyList()
        Wenku8Parser.parseReviewReplyList(reviewReplyList, "1234")
        assertTrue(reviewReplyList.getList().isEmpty())
    }

    @Test
    fun testParseReviewReplyListPage2() {
        val REVIEW_REPLY_LIST_XML_PAGE_2 = """<?xml version="1.0" encoding="utf-8"?>
<metadata>
<page num='2'/>
<item timestamp='20180713101811'>
<user uid='233517'><![CDATA[a1b2c3d4]]></user>
<content><![CDATA[嗯…………至少是一樓發完]]></content>
</item>

<item timestamp='20180713135735'>
<user uid='230042'><![CDATA[156126]]></user>
<content><![CDATA[滑稽✧(`ῧ′)机智]]></content>
</item>

</metadata>"""
        val reviewReplyList = ReviewReplyList()
        Wenku8Parser.parseReviewReplyList(reviewReplyList, REVIEW_REPLY_LIST_XML)
        Wenku8Parser.parseReviewReplyList(reviewReplyList, REVIEW_REPLY_LIST_XML_PAGE_2)
        assertEquals(reviewReplyList.getTotalPage(), 2)
        assertEquals(reviewReplyList.getCurrentPage(), 2)
        assertEquals(reviewReplyList.getList().size(), 4)
        var reviewReply: ReviewReplyList.ReviewReply = reviewReplyList.getList().get(0)
        assertEquals(reviewReply.getReplyTime().getTime(),
                GregorianCalendar(2018, Calendar.JULY, 13, 10, 18, 11).getTime().getTime())
        assertEquals(reviewReply.getUid(), 233516)
        assertEquals(reviewReply.getUserName(), "a1b2c3d4")
        assertEquals(reviewReply.getContent(), "嗯…………至少是一樓發完")
        reviewReply = reviewReplyList.getList().get(1)
        assertEquals(reviewReply.getReplyTime().getTime(),
                GregorianCalendar(2018, Calendar.JULY, 13, 13, 57, 35).getTime().getTime())
        assertEquals(reviewReply.getUid(), 230041)
        assertEquals(reviewReply.getUserName(), "156126")
        assertEquals(reviewReply.getContent(), "滑稽✧(`ῧ′)机智")
        reviewReply = reviewReplyList.getList().get(2)
        assertEquals(reviewReply.getReplyTime().getTime(),
                GregorianCalendar(2018, Calendar.JULY, 13, 10, 18, 11).getTime().getTime())
        assertEquals(reviewReply.getUid(), 233517)
        assertEquals(reviewReply.getUserName(), "a1b2c3d4")
        assertEquals(reviewReply.getContent(), "嗯…………至少是一樓發完")
        reviewReply = reviewReplyList.getList().get(3)
        assertEquals(reviewReply.getReplyTime().getTime(),
                GregorianCalendar(2018, Calendar.JULY, 13, 13, 57, 35).getTime().getTime())
        assertEquals(reviewReply.getUid(), 230042)
        assertEquals(reviewReply.getUserName(), "156126")
        assertEquals(reviewReply.getContent(), "滑稽✧(`ῧ′)机智")
    }
}