package org.mewx.wenku8.reader.loader

import android.graphics.Bitmap

/**
 * Created by MewX on 2015/7/8.
 *
 * Raw data loader. Need async call!
 */
class WenkuReaderLoaderXML(@NonNull onc: List<OldNovelContentParser.NovelContent?>?) : WenkuReaderLoader() {
    private var currentIndex = 0
    private var nc: List<OldNovelContentParser.NovelContent?>?
    var chapterName: String? = null

    init {
        nc = onc
    }

    @Override
    fun setChapterName(name: String?) {
        chapterName = name
    }

    @Override
    fun getChapterName(): String? {
        return chapterName
    }

    @Override
    fun hasNext(wordIndex: Int): Boolean {
        if (currentIndex < nc.size() && currentIndex >= 0) {
            // size legal
            if (currentIndex + 1 < nc.size()) {
                // remain one more
                return true
            } else {
                // last one
                if (nc.get(currentIndex).type === NovelContentType.TEXT && wordIndex + 1 < nc.get(currentIndex).content.length()) {
                    // text but not last word
                    return true
                } else if (nc.get(currentIndex).type !== NovelContentType.TEXT && wordIndex == 0) {
                    // image
                    return true
                }
            }
        }
        return false
    }

    @Override
    fun hasPrevious(wordIndex: Int): Boolean {
        if (currentIndex < nc.size() && currentIndex >= 0) {
            // size legal
            if (currentIndex - 1 >= 0) {
                // one more ahead
                return true
            } else {
                // first one
                if (nc.get(currentIndex).type === NovelContentType.TEXT && wordIndex - 1 >= 0) {
                    // one more word ahead
                    return true
                } else if (nc.get(currentIndex).type !== NovelContentType.TEXT && wordIndex == nc.get(currentIndex).content.length() - 1) // image previous use index last
                    return true
            }
        }
        return false
    }

    @Override
    fun getNextType(): ElementType? {
        // nullable
        if (currentIndex + 1 < nc.size() && currentIndex >= 0) {
            if (currentIndex != nc.size() - 1) return intepreteOldSign(nc.get(currentIndex + 1).type)
        }
        return null
    }

    @Override
    fun getNextAsString(): String? {
        if (currentIndex + 1 < nc.size() && currentIndex >= 0) {
            currentIndex++
            return nc.get(currentIndex).content
        }
        return null
    }

    @Override
    fun getNextAsBitmap(): Bitmap? {
        // Async get bitmap from local or internet
        if (currentIndex + 1 < nc.size() && currentIndex >= 0) {
            currentIndex++
            val imgFileName: String = GlobalConfig.generateImageFileNameByURL(nc.get(currentIndex).content)
            var path: String = GlobalConfig.getAvailableNovelContentImagePath(imgFileName)
            if (path == null || path.equals("")) {
                GlobalConfig.saveNovelContentImage(nc.get(currentIndex).content)
                val name: String = GlobalConfig.generateImageFileNameByURL(nc.get(currentIndex).content)
                path = GlobalConfig.getAvailableNovelContentImagePath(name)
            }

            // load bitmap
            val options: BitmapFactory.Options = Options()
            options.inSampleSize = 2
            val bm: Bitmap = BitmapFactory.decodeFile(path, options)
            if (bm != null) return bm
        }
        return null
    }

    @Override
    fun getCurrentType(): ElementType? {
        // nullable
        return if (currentIndex < nc.size() && currentIndex >= 0) {
            intepreteOldSign(nc.get(currentIndex).type)
        } else null
    }

    /**
     * Use with cautious! This method copies the whole text for each call!
     * @return the whole chapter text.
     */
    @Override
    fun getCurrentAsString(): String? {
        return if (currentIndex < nc.size() && currentIndex >= 0) {
            nc.get(currentIndex).content
        } else null
    }

    @Override
    fun getCurrentStringLength(): Int {
        return getStringLength(currentIndex)
    }

    @Override
    fun getCurrentAsBitmap(): Bitmap? {
        // Async get bitmap from local or internet
        if (currentIndex < nc.size() && currentIndex >= 0) {
            val imgFileName: String = GlobalConfig.generateImageFileNameByURL(nc.get(currentIndex).content)
            var path: String = GlobalConfig.getAvailableNovelContentImagePath(imgFileName)
            if (path == null || path.equals("")) {
                GlobalConfig.saveNovelContentImage(nc.get(currentIndex).content)
                val name: String = GlobalConfig.generateImageFileNameByURL(nc.get(currentIndex).content)
                path = GlobalConfig.getAvailableNovelContentImagePath(name)
            }

            // load bitmap
            val options: BitmapFactory.Options = Options()
            options.inSampleSize = 2
            val bm: Bitmap = BitmapFactory.decodeFile(path, options)
            if (bm != null) return bm
        }
        return null
    }

    @Override
    fun getPreviousType(): ElementType? {
        // nullable
        if (currentIndex < nc.size() && currentIndex - 1 >= 0) {
            if (currentIndex != 0) return intepreteOldSign(nc.get(currentIndex - 1).type)
        }
        return null
    }

    @Override
    fun getPreviousAsString(): String? {
        if (currentIndex < nc.size() && currentIndex - 1 >= 0) {
            currentIndex--
            return nc.get(currentIndex).content
        }
        return null
    }

    @Override
    fun getPreviousAsBitmap(): Bitmap? {
        // Async get bitmap from local or internet
        if (currentIndex < nc.size() && currentIndex - 1 >= 0) {
            currentIndex--
            val imgFileName: String = GlobalConfig.generateImageFileNameByURL(nc.get(currentIndex).content)
            var path: String = GlobalConfig.getAvailableNovelContentImagePath(imgFileName)
            if (path == null || path.equals("")) {
                GlobalConfig.saveNovelContentImage(nc.get(currentIndex).content)
                val name: String = GlobalConfig.generateImageFileNameByURL(nc.get(currentIndex).content)
                path = GlobalConfig.getAvailableNovelContentImagePath(name)
            }

            // load bitmap
            val options: BitmapFactory.Options = Options()
            options.inSampleSize = 2
            val bm: Bitmap = BitmapFactory.decodeFile(path, options)
            if (bm != null) return bm
        }
        return null
    }

    @Override
    fun getStringLength(n: Int): Int {
        return if (n >= 0 && n < getElementCount()) nc.get(n).content.length() else 0
    }

    @Override
    fun getElementCount(): Int {
        return nc.size()
    }

    @Override
    fun getCurrentIndex(): Int {
        return currentIndex
    }

    @Override
    fun setCurrentIndex(i: Int) {
        currentIndex = i
    }

    @Override
    fun closeLoader() {
        nc = null
    }

    private fun intepreteOldSign(type: NovelContentType?): ElementType? {
        return when (type) {
            TEXT -> ElementType.TEXT
            IMAGE -> ElementType.IMAGE_DEPENDENT
            else -> ElementType.TEXT
        }
    }
}