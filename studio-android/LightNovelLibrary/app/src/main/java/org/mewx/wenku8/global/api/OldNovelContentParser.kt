package org.mewx.wenku8.global.api

import android.util.Log

/**
 * Created by MewX on 2015/6/6.
 * Old Novel Content Parser.
 */
object OldNovelContentParser {
    private val TAG: String? = OldNovelContentParser::class.java.getSimpleName()
    private val IMAGE_ENTRY: String? = "<!--image-->"
    @NonNull
    fun parseNovelContent(@NonNull raw: String?, @Nullable pDialog: MaterialDialog?): List<NovelContent?>? {
        val result: List<NovelContent?> = ArrayList()

        // use split
        val s: Array<String?> = raw.split("\r\n")
        var temp: Int
        for (t in s) {
            // escape empty line
            var isEmpty = true
            for (i in 0 until t.length()) {
                if (t.charAt(i) !== ' ') {
                    isEmpty = false
                    break
                }
            }
            if (isEmpty) continue

            // test
            temp = t.indexOf(IMAGE_ENTRY, 0)
            if (temp == -1) {
                val nc = NovelContent()
                nc.type = NovelContentType.TEXT
                nc.content = t.trim()
                result.add(nc)

                // update progress
                if (pDialog != null) pDialog.setMaxProgress(result.size())
            } else {
                Log.d(TAG, "img index = $temp")

                // one line contains more than one images
                temp = 0
                while (true) {
                    temp = t.indexOf(IMAGE_ENTRY, temp)
                    if (temp == -1) break
                    val nc2 = NovelContent()
                    val t2: Int = t.indexOf(IMAGE_ENTRY, temp + IMAGE_ENTRY.length())
                    if (t2 < 0) {
                        Log.d(TAG, "Incomplete image pair, t2 = $t2")
                        val nc = NovelContent()
                        nc.type = NovelContentType.TEXT
                        nc.content = t.trim()
                        result.add(nc)
                        break
                    }
                    nc2.content = t.substring(temp + IMAGE_ENTRY.length(), t2)
                    nc2.type = NovelContentType.IMAGE
                    result.add(nc2)
                    temp = t2 + IMAGE_ENTRY.length()

                    // update progress
                    if (pDialog != null) pDialog.setMaxProgress(result.size())
                }
            }
        }
        return result
    }

    @NonNull
    fun NovelContentParser_onlyImage(@NonNull raw: String?): List<NovelContent?>? {
        val result: List<NovelContent?> = ArrayList()

        // use split
        val s: Array<String?> = raw.split("\r\n")
        var temp: Int
        for (t in s) {
            // test
            temp = t.indexOf(IMAGE_ENTRY, 0)
            if (temp != -1) {
                Log.d(TAG, "img index = $temp")

                // one line contains more than one images
                temp = 0
                while (true) {
                    temp = t.indexOf(IMAGE_ENTRY, temp)
                    if (temp == -1) break
                    val nc2 = NovelContent()
                    val t2: Int = t.indexOf(IMAGE_ENTRY, temp + IMAGE_ENTRY.length())
                    if (t2 < 0) {
                        Log.d(TAG, "Breaked in NovelContentParser_onlyImage, t2 = $t2")
                        break
                    }
                    nc2.content = t.substring(temp + IMAGE_ENTRY.length(), t2)
                    nc2.type = NovelContentType.IMAGE
                    result.add(nc2)
                    temp = t2 + IMAGE_ENTRY.length()
                }
            }
        }
        return result
    }

    enum class NovelContentType {
        TEXT, IMAGE
    }

    class NovelContent {
        var type: NovelContentType? = NovelContentType.TEXT
        var content: String? = ""
    }
}