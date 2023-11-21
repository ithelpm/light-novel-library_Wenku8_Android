package org.mewx.wenku8.global.api

import android.util.Log

/**
 * Created by MewX on 2015/4/21.
 * Wenku8 parsers.
 */
object Wenku8Parser {
    @NonNull
    fun parseNovelItemList(@NonNull str: String?): List<Integer?>? {
        val list: List<Integer?> = ArrayList()

        // <?xml version="1.0" encoding="utf-8"?>
        // <result>
        // <page num='166'/>
        // <item aid='1143'/>
        // <item aid='1034'/>
        // <item aid='1213'/>
        // <item aid='1'/>
        // <item aid='1011'/>
        // <item aid='1192'/>
        // <item aid='433'/>
        // <item aid='47'/>
        // <item aid='7'/>
        // <item aid='374'/>
        // </result>

        // The returning list of this xml is: (total page, aids)
        // { 166, 1143, 1034, 1213, 1, 1011, 1192, 433, 47, 7, 374 }
        val SEPARATOR = '\'' // seperator

        // get total page
        var beg: Int
        var temp: Int
        beg = str.indexOf(SEPARATOR)
        temp = str.indexOf(SEPARATOR, beg + 1)
        if (beg == -1 || temp == -1) return list // empty, this is an exception
        if (LightTool.isInteger(str.substring(beg + 1, temp))) list.add(Integer.parseInt(str.substring(beg + 1, temp)))
        beg = temp + 1 // prepare for loop

        // init array
        while (true) {
            beg = str.indexOf(SEPARATOR, beg)
            temp = str.indexOf(SEPARATOR, beg + 1)
            if (beg == -1 || temp == -1) break
            if (LightTool.isInteger(str.substring(beg + 1, temp))) list.add(Integer.parseInt(str.substring(beg + 1, temp)))
            Log.v("MewX", "Add novel aid: " + list[list.size() - 1])
            beg = temp + 1 // prepare for next round
        }
        return list
    }

    fun parseNovelFullMeta(xml: String?): NovelItemMeta? {
        // get full XML metadata of a novel, here is an example:
        // -----------------------------------------------------
        // <?xml version="1.0" encoding="utf-8"?>
        // <metadata>
        // <data name="Title" aid="1306"><![CDATA[向森之魔物献上花束(向森林的魔兽少女献花)]]></data>
        // <data name="Author" value="小木君人"/>
        // <data name="DayHitsCount" value="26"/>
        // <data name="TotalHitsCount" value="43984"/>
        // <data name="PushCount" value="1735"/>
        // <data name="FavCount" value="848"/>
        // <data name="PressId" value="小学馆" sid="10"/>
        // <data name="BookStatus" value="已完成"/>
        // <data name="BookLength" value="105985"/>
        // <data name="LastUpdate" value="2012-11-02"/>
        // <data name="LatestSection" cid="41897"><![CDATA[第一卷 插图]]></data>
        // </metadata>
        Log.d(Wenku8Parser::class.java.getSimpleName(), xml)
        return try {
            val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
            val xmlPullParser: XmlPullParser = factory.newPullParser()
            val nfi = NovelItemMeta()
            xmlPullParser.setInput(StringReader(xml))
            var eventType: Int = xmlPullParser.getEventType()
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_DOCUMENT -> {}
                    XmlPullParser.START_TAG -> if ("metadata".equals(xmlPullParser.getName())) {
                        break
                    } else if ("data".equals(xmlPullParser.getName())) {
                        if ("Title".equals(xmlPullParser.getAttributeValue(0))) {
                            nfi.aid = Integer.valueOf(xmlPullParser.getAttributeValue(1))
                            nfi.title = xmlPullParser.nextText()
                        } else if ("Author".equals(xmlPullParser
                                        .getAttributeValue(0))) {
                            nfi.author = xmlPullParser.getAttributeValue(1)
                        } else if ("DayHitsCount".equals(xmlPullParser
                                        .getAttributeValue(0))) {
                            nfi.dayHitsCount = Integer.valueOf(xmlPullParser.getAttributeValue(1))
                        } else if ("TotalHitsCount".equals(xmlPullParser
                                        .getAttributeValue(0))) {
                            nfi.totalHitsCount = Integer.valueOf(xmlPullParser.getAttributeValue(1))
                        } else if ("PushCount".equals(xmlPullParser
                                        .getAttributeValue(0))) {
                            nfi.pushCount = Integer.valueOf(xmlPullParser.getAttributeValue(1))
                        } else if ("FavCount".equals(xmlPullParser
                                        .getAttributeValue(0))) {
                            nfi.favCount = Integer.valueOf(xmlPullParser.getAttributeValue(1))
                        } else if ("PressId".equals(xmlPullParser
                                        .getAttributeValue(0))) {
                            nfi.pressId = xmlPullParser.getAttributeValue(1)
                        } else if ("BookStatus".equals(xmlPullParser
                                        .getAttributeValue(0))) {
                            nfi.bookStatus = xmlPullParser.getAttributeValue(1)
                        } else if ("BookLength".equals(xmlPullParser
                                        .getAttributeValue(0))) {
                            nfi.bookLength = Integer.valueOf(xmlPullParser.getAttributeValue(1))
                        } else if ("LastUpdate".equals(xmlPullParser
                                        .getAttributeValue(0))) {
                            nfi.lastUpdate = xmlPullParser.getAttributeValue(1)
                        } else if ("LatestSection".equals(xmlPullParser
                                        .getAttributeValue(0))) {
                            nfi.latestSectionCid = Integer.valueOf(xmlPullParser.getAttributeValue(1))
                            nfi.latestSectionName = xmlPullParser.nextText()
                        }
                    }
                }
                eventType = xmlPullParser.next()
            }
            nfi
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @NonNull
    fun getVolumeList(@NonNull xml: String?): ArrayList<VolumeList?>? {
        val l: ArrayList<VolumeList?> = ArrayList()
        try {
            val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
            val xmlPullParser: XmlPullParser = factory.newPullParser()
            var vl: VolumeList? = null
            var ci: ChapterInfo?
            xmlPullParser.setInput(StringReader(xml))
            var eventType: Int = xmlPullParser.getEventType()
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_DOCUMENT -> {}
                    XmlPullParser.START_TAG -> if ("volume".equals(xmlPullParser.getName())) {
                        vl = VolumeList()
                        vl.chapterList = ArrayList()
                        vl.vid = Integer.valueOf(xmlPullParser.getAttributeValue(0))

                        // Here the returned text has some format error
                        // And I will handle them then
                        Log.v("MewX-XML", "+ " + vl.vid + "; ")
                    } else if ("chapter".equals(xmlPullParser.getName())) {
                        ci = ChapterInfo()
                        ci.cid = Integer.valueOf(xmlPullParser.getAttributeValue(0))
                        ci.chapterName = xmlPullParser.nextText()
                        Log.v("MewX-XML", ci.cid + "; " + ci.chapterName)
                        if (vl != null) vl.chapterList.add(ci)
                    }

                    XmlPullParser.END_TAG -> if ("volume".equals(xmlPullParser.getName())) {
                        l.add(vl)
                        vl = null
                    }
                }
                eventType = xmlPullParser.next()
            }

            /* Handle the rest problem */
            // Problem like this:
            // <volume vid="41748"><![CDATA[第一卷 告白于苍刻之夜]]>
            // <chapter cid="41749"><![CDATA[序章]]></chapter>
            var currentIndex = 0
            for (i in 0 until l.size()) {
                currentIndex = xml.indexOf("volume", currentIndex)
                if (currentIndex != -1) {
                    currentIndex = xml.indexOf("CDATA[", currentIndex)
                    if (xml.indexOf("volume", currentIndex) !== -1) {
                        val beg = currentIndex + 6
                        val end: Int = xml.indexOf("]]", currentIndex)
                        if (end != -1) {
                            l.get(i).volumeName = xml.substring(beg, end)
                            Log.v("MewX-XML", "+ " + l.get(i).volumeName + "; ")
                            currentIndex = end + 1
                        } else break
                    } else break
                } else break
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return l
    }

    /**
     * save the new xsl into an existing review list
     * @param reviewList the existing review list object
     * @param xml the fetched xml
     */
    fun parseReviewList(reviewList: ReviewList?, xml: String?) {
        reviewList.setCurrentPage(reviewList.getCurrentPage() + 1)
        try {
            val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
            val xmlPullParser: XmlPullParser = factory.newPullParser()
            xmlPullParser.setInput(StringReader(xml))
            var eventType: Int = xmlPullParser.getEventType()
            var rid = 0 // review id
            var postTime = Date()
            var noReplies = 0
            var lastReplyTime = Date()
            var userName = ""
            var uid = 0 // post user
            var title = "" // review title
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_DOCUMENT -> {}
                    XmlPullParser.START_TAG -> if ("page".equals(xmlPullParser.getName())) {
                        reviewList.setTotalPage(Integer.valueOf(xmlPullParser.getAttributeValue(null, "num")))
                    } else if ("item".equals(xmlPullParser.getName())) {
                        rid = Integer.valueOf(xmlPullParser.getAttributeValue(null, "rid"))
                        noReplies = Integer.valueOf(xmlPullParser.getAttributeValue(null, "replies"))
                        val postTimeStr: String = xmlPullParser.getAttributeValue(null, "posttime")
                        postTime = GregorianCalendar(
                                Integer.valueOf(postTimeStr.substring(0, 4), 10),
                                Integer.valueOf(postTimeStr.substring(4, 6), 10) - 1,  // start from 0 - Calendar.JANUARY
                                Integer.valueOf(postTimeStr.substring(6, 8), 10),
                                Integer.valueOf(postTimeStr.substring(8, 10), 10),
                                Integer.valueOf(postTimeStr.substring(10, 12), 10),
                                Integer.valueOf(postTimeStr.substring(12), 10)
                        ).getTime()
                        val replyTimeStr: String = xmlPullParser.getAttributeValue(null, "replytime")
                        lastReplyTime = GregorianCalendar(
                                Integer.valueOf(replyTimeStr.substring(0, 4), 10),
                                Integer.valueOf(replyTimeStr.substring(4, 6), 10) - 1,
                                Integer.valueOf(replyTimeStr.substring(6, 8), 10),
                                Integer.valueOf(replyTimeStr.substring(8, 10), 10),
                                Integer.valueOf(replyTimeStr.substring(10, 12), 10),
                                Integer.valueOf(replyTimeStr.substring(12), 10)
                        ).getTime()
                    } else if ("user".equals(xmlPullParser.getName())) {
                        uid = Integer.valueOf(xmlPullParser.getAttributeValue(null, "uid"))
                        userName = xmlPullParser.nextText()
                    } else if ("content".equals(xmlPullParser.getName())) {
                        title = xmlPullParser.nextText().trim()
                    }

                    XmlPullParser.END_TAG -> if ("item".equals(xmlPullParser.getName())) {
                        reviewList.getList().add(
                                Review(rid, postTime, noReplies, lastReplyTime, userName, uid, title))
                    }
                }
                eventType = xmlPullParser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * save the new xsl into an existing review reply list
     * @param reviewReplyList the existing review reply list object
     * @param xml the fetched xml
     */
    fun parseReviewReplyList(reviewReplyList: ReviewReplyList?, xml: String?) {
        reviewReplyList.setCurrentPage(reviewReplyList.getCurrentPage() + 1)
        try {
            val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
            val xmlPullParser: XmlPullParser = factory.newPullParser()
            xmlPullParser.setInput(StringReader(xml))
            var eventType: Int = xmlPullParser.getEventType()
            var replyTime = Date()
            var userName = ""
            var uid = 0 // post user
            var content = ""
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_DOCUMENT -> {}
                    XmlPullParser.START_TAG -> if ("page".equals(xmlPullParser.getName())) {
                        reviewReplyList.setTotalPage(Integer.valueOf(xmlPullParser.getAttributeValue(null, "num")))
                    } else if ("item".equals(xmlPullParser.getName())) {
                        val replyTimeStr: String = xmlPullParser.getAttributeValue(null, "timestamp")
                        replyTime = GregorianCalendar(
                                Integer.valueOf(replyTimeStr.substring(0, 4), 10),
                                Integer.valueOf(replyTimeStr.substring(4, 6), 10) - 1,  // start from 0 - Calendar.JANUARY
                                Integer.valueOf(replyTimeStr.substring(6, 8), 10),
                                Integer.valueOf(replyTimeStr.substring(8, 10), 10),
                                Integer.valueOf(replyTimeStr.substring(10, 12), 10),
                                Integer.valueOf(replyTimeStr.substring(12), 10)
                        ).getTime()
                    } else if ("user".equals(xmlPullParser.getName())) {
                        uid = Integer.valueOf(xmlPullParser.getAttributeValue(null, "uid"))
                        userName = xmlPullParser.nextText()
                    } else if ("content".equals(xmlPullParser.getName())) {
                        content = xmlPullParser.nextText().trim()
                    }

                    XmlPullParser.END_TAG -> if ("item".equals(xmlPullParser.getName())) {
                        reviewReplyList.getList().add(
                                ReviewReply(replyTime, userName, uid, content))
                    }
                }
                eventType = xmlPullParser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}