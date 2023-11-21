package org.mewx.wenku8.global.api

import org.mewx.wenku8.global.GlobalConfig

/**
 * Created by MewX on 2015/5/13.
 * Volume List.
 */
class VolumeList : Serializable {
    var volumeName: String? = null
    var vid = 0
    var inLocal = false
    var chapterList: ArrayList<ChapterInfo?>? = null
    fun cleanLocalCache() {
        for (tempCi in chapterList) {
            val xml: String = GlobalConfig.loadFullFileFromSaveFolder("novel", tempCi.cid + ".xml")
            if (xml.length() === 0) {
                return
            }
            val nc: List<OldNovelContentParser.NovelContent?> = OldNovelContentParser.NovelContentParser_onlyImage(xml)
            for (i in 0 until nc.size()) {
                if (nc[i].type === OldNovelContentParser.NovelContentType.IMAGE) {
                    val imgFileName: String = GlobalConfig.generateImageFileNameByURL(nc[i].content)
                    LightCache.deleteFile(
                            GlobalConfig.getFirstFullSaveFilePath() +
                                    GlobalConfig.imgsSaveFolderName + File.separator + imgFileName)
                    LightCache.deleteFile(
                            GlobalConfig.getSecondFullSaveFilePath() +
                                    GlobalConfig.imgsSaveFolderName + File.separator + imgFileName)
                }
            }
            LightCache.deleteFile(GlobalConfig.getFirstFullSaveFilePath(), "novel" + File.separator + tempCi.cid + ".xml")
            LightCache.deleteFile(GlobalConfig.getSecondFullSaveFilePath(), "novel" + File.separator + tempCi.cid + ".xml")
        }
        inLocal = false
    }
}