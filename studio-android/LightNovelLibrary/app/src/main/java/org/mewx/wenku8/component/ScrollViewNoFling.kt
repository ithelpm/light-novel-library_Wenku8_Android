package org.mewx.wenku8.component

import android.content.Context

/**
 * Created by MewX on 2015/6/6.
 *
 * A sticky scroll view.
 */
class ScrollViewNoFling : ScrollView {
    val f = 1 // sticky

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    @Override
    fun fling(velocityY: Int) {
        /*Scroll view is no longer gonna handle scroll velocity.
     * super.fling(velocityY);
    */
        super.fling(velocityY / f)
    }
}