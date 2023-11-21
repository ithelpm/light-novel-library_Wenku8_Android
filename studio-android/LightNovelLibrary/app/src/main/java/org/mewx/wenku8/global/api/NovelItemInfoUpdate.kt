package org.mewx.wenku8.global.api

import org.xmlpull.v1.XmlPullParser

/**
 * Created by MewX on 2015/1/20.
 * The updated version of novel item info.
 */
class NovelItemInfoUpdate( // Variables
        var aid: Int) {
    var title: String?
    var author = LOADING_STRING
    var status = LOADING_STRING
    var update = LOADING_STRING // last update time
    var intro_short = LOADING_STRING
    var latest_chapter = LOADING_STRING // only used in bookshelf

    init {
        title = Integer.toString(aid)
    }

    fun isInitialized(): Boolean {
        return title.equals(Integer.toString(aid))
    }

    companion object {
        private val LOADING_STRING: String? = "Loading..."

        // static function
        @NonNull
        fun convertFromMeta(@NonNull nim: NovelItemMeta?): NovelItemInfoUpdate? {
            val niiu = NovelItemInfoUpdate(0)
            niiu.title = nim.title
            niiu.aid = nim.aid
            niiu.author = nim.author
            niiu.status = nim.bookStatus
            niiu.update = nim.lastUpdate
            niiu.latest_chapter = nim.latestSectionName
            return niiu
        }

        @Nullable
        fun parse(@NonNull xml: String?): NovelItemInfoUpdate? {
            return try {
                val niiu = NovelItemInfoUpdate(0)
                val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
                val xmlPullParser: XmlPullParser = factory.newPullParser()
                xmlPullParser.setInput(StringReader(xml))
                var eventType: Int = xmlPullParser.getEventType()
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_DOCUMENT -> {}
                        XmlPullParser.START_TAG -> if ("metadata".equals(xmlPullParser.getName())) {
                            // Init all the value
                            niiu.aid = 0
                            niiu.title = ""
                            niiu.author = ""
                            niiu.status = ""
                            niiu.update = ""
                            niiu.intro_short = ""
                            niiu.latest_chapter = ""
                        } else if ("data".equals(xmlPullParser.getName())) {
                            if ("Title".equals(xmlPullParser.getAttributeValue(0))) {
                                niiu.aid = Integer.valueOf(
                                        xmlPullParser.getAttributeValue(1))
                                niiu.title = xmlPullParser.nextText()
                            } else if ("Author".equals(xmlPullParser
                                            .getAttributeValue(0))) {
                                niiu.author = xmlPullParser.getAttributeValue(1)
                            } else if ("BookStatus".equals(xmlPullParser
                                            .getAttributeValue(0))) {
                                niiu.status = xmlPullParser.getAttributeValue(1)
                            } else if ("LastUpdate".equals(xmlPullParser
                                            .getAttributeValue(0))) {
                                niiu.update = xmlPullParser.getAttributeValue(1)
                            } else if ("IntroPreview".equals(xmlPullParser
                                            .getAttributeValue(0))) {
                                // need to remove leading space '\u3000'
                                niiu.intro_short = xmlPullParser.nextText().replaceAll("[ |　]", " ").trim() //.trim().replaceAll("\u3000","");
                            }
                        }
                    }
                    eventType = xmlPullParser.next()
                }
                niiu
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}