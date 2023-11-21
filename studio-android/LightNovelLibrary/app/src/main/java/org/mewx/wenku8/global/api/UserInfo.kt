package org.mewx.wenku8.global.api

import android.util.Log

/**
 * Created by MewX on 2015/6/14.
 * User Info.
 */
class UserInfo {
    /*
     * <?xml version="1.0" encoding="utf-8"?>
     * <metadata>
     * <item name="uname"><![CDATA[apptest]]></item>
     * <item name="nickname"><![CDATA[apptest]]></item>
     * <item name="score">10</item>
     * <item name="experience">10</item>
     * <item name="rank"><![CDATA[新手上路]]></item>
     * </metadata>
     */
    var username: String? = null
    var nickyname: String? = null
    var score = 0 // 现有积分
    var experience = 0 // 经验值
    var rank: String? = null

    companion object {
        @Nullable
        fun parseUserInfo(@NonNull xml: String?): UserInfo? {
            return try {
                val ui = UserInfo()
                val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
                val xmlPullParser: XmlPullParser = factory.newPullParser()
                xmlPullParser.setInput(StringReader(xml))
                var eventType: Int = xmlPullParser.getEventType()
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_DOCUMENT -> {}
                        XmlPullParser.START_TAG -> if ("metadata".equals(xmlPullParser.getName())) {
                            // root tag
                            break
                        } else if ("item".equals(xmlPullParser.getName())) {
                            if ("uname".equals(xmlPullParser.getAttributeValue(0))) {
                                ui.username = xmlPullParser.nextText()
                                Log.d("MewX", if (ui.username.length() === 0) GlobalConfig.UNKNOWN else ui.username)
                            } else if ("nickname".equals(xmlPullParser.getAttributeValue(0))) {
                                ui.nickyname = xmlPullParser.nextText()
                                Log.d("MewX", if (ui.nickyname.length() === 0) GlobalConfig.UNKNOWN else ui.nickyname)
                            } else if ("score".equals(xmlPullParser.getAttributeValue(0))) {
                                ui.score = Integer.valueOf(xmlPullParser.nextText())
                                Log.d("MewX", "score:" + ui.score)
                            } else if ("experience".equals(xmlPullParser.getAttributeValue(0))) {
                                ui.experience = Integer.valueOf(xmlPullParser.nextText())
                                Log.d("MewX", "experience:" + ui.experience)
                            } else if ("rank".equals(xmlPullParser.getAttributeValue(0))) {
                                ui.rank = xmlPullParser.nextText()
                                Log.d("MewX", if (ui.rank.length() === 0) GlobalConfig.UNKNOWN else ui.rank)
                            }
                        }
                    }
                    eventType = xmlPullParser.next()
                }
                ui
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}