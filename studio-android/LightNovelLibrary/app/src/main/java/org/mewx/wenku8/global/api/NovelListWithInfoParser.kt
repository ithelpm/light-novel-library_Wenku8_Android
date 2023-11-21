package org.mewx.wenku8.global.api

import android.util.Log

/** Novel List With Info for improve loading speed.
 * Created by MewX on 2015/10/20.
 */
object NovelListWithInfoParser {
    /**
     * get current 'page' attributes
     * @param xml input xml
     * @return the page in the xml file or 0 by default
     */
    fun getNovelListWithInfoPageNum(xml: String?): Int {
        try {
            val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
            val xmlPullParser: XmlPullParser = factory.newPullParser()
            xmlPullParser.setInput(StringReader(xml))
            var eventType: Int = xmlPullParser.getEventType()
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_DOCUMENT -> {}
                    XmlPullParser.START_TAG -> if ("page".equals(xmlPullParser.getName())) {
                        return Integer.valueOf(xmlPullParser.getAttributeValue(0))
                    }
                }
                eventType = xmlPullParser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0 // default
    }

    @NonNull
    fun getNovelListWithInfo(xml: String?): List<NovelListWithInfo?>? {
        val l: List<NovelListWithInfo?> = ArrayList()
        try {
            val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
            val xmlPullParser: XmlPullParser = factory.newPullParser()
            var n = NovelListWithInfo()
            xmlPullParser.setInput(StringReader(xml))
            var eventType: Int = xmlPullParser.getEventType()
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> if ("item".equals(xmlPullParser.getName())) {
                        n = NovelListWithInfo()
                        n.aid = Integer.valueOf(xmlPullParser.getAttributeValue(0))
                    } else if ("data".equals(xmlPullParser.getName())) {
                        if ("Title".equals(xmlPullParser.getAttributeValue(0))) {
                            n.name = xmlPullParser.nextText()
                        } else if ("TotalHitsCount".equals(xmlPullParser.getAttributeValue(0))) {
                            n.hit = Integer.valueOf(xmlPullParser.getAttributeValue(1))
                        } else if ("PushCount".equals(xmlPullParser.getAttributeValue(0))) {
                            n.push = Integer.valueOf(xmlPullParser.getAttributeValue(1))
                        } else if ("FavCount".equals(xmlPullParser.getAttributeValue(0))) {
                            n.fav = Integer.valueOf(xmlPullParser.getAttributeValue(1))
                        }
                    }

                    XmlPullParser.END_TAG -> if ("item".equals(xmlPullParser.getName())) {
                        Log.d("MewX-XML", n.aid.toString() + ";" + n.name + ";" + n.hit + ";" + n.push + ";" + n.fav)
                        l.add(n)
                    }
                }
                eventType = xmlPullParser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return l
    }

    class NovelListWithInfo {
        var aid = 0
        var name: String? = ""
        var hit = 0
        var push = 0
        var fav = 0
    }
}