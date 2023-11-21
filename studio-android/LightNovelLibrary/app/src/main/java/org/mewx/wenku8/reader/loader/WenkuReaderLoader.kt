package org.mewx.wenku8.reader.loader

import android.graphics.Bitmap

/**
 * Created by MewX on 2015/7/8.
 * Parent of all loaders.
 */
abstract class WenkuReaderLoader {
    /**
     * ElementType: the type of next element to get
     */
    enum class ElementType {
        TEXT, IMAGE_INDEPENDENT,  // be in different line from text
        IMAGE_DEPENDENT
        // may be in the same line with text
    }

    // public abstract void initLoader(String srcPath);
    abstract fun setChapterName(name: String?) // set chapter name
    abstract fun getChapterName(): String? // get chapter name
    abstract fun hasNext(wordIndex: Int): Boolean // word in current line
    abstract fun hasPrevious(wordIndex: Int): Boolean // word in current line
    abstract fun getNextType(): ElementType? // next is {index}, nullable (index keep)
    abstract fun getNextAsString(): String? // index ++
    abstract fun getNextAsBitmap(): Bitmap? // index ++
    abstract fun getCurrentType(): ElementType? //
    abstract fun getCurrentAsString(): String? // index keep
    abstract fun getCurrentStringLength(): Int
    abstract fun getCurrentAsBitmap(): Bitmap? // index keep
    abstract fun getPreviousType(): ElementType? // nullable (index keep)
    abstract fun getPreviousAsString(): String? // index --
    abstract fun getPreviousAsBitmap(): Bitmap? // index --
    abstract fun getStringLength(n: Int): Int
    abstract fun getElementCount(): Int
    abstract fun getCurrentIndex(): Int // from 0, to {Count - 1}
    abstract fun setCurrentIndex(i: Int) // set a index, should optimize for the same or relation lines
    abstract fun closeLoader()
}