package org.mewx.wenku8.global.api

import org.mewx.wenku8.global.GlobalConfig

/**
 * Created by MewX on 2015/5/13.
 * Novel Item Meta data.
 */
class NovelItemMeta internal constructor() {
    var aid = 1
    var title: String?
    var author: String?
    var dayHitsCount = 0
    var totalHitsCount = 0
    var pushCount = 0
    var favCount = 0
    var pressId: String?
    var bookStatus // just text, differ from "NovelIntro"
            : String?
    var bookLength = 0
    var lastUpdate: String?
    var latestSectionCid = 0
    var latestSectionName: String?
    var fullIntro // fetch from another place
            : String?

    init {
        title = Integer.toString(aid)
        author = GlobalConfig.UNKNOWN
        pressId = GlobalConfig.UNKNOWN
        bookStatus = GlobalConfig.UNKNOWN
        lastUpdate = GlobalConfig.UNKNOWN
        latestSectionName = GlobalConfig.UNKNOWN
        fullIntro = GlobalConfig.UNKNOWN
    }
}