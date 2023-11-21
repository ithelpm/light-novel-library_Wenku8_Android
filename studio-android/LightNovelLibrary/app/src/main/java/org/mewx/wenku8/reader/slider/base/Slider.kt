package org.mewx.wenku8.reader.slider.base

import android.view.MotionEvent

/**
 * Created by xuzb on 1/16/15.
 */
interface Slider {
    open fun init(slidingLayout: SlidingLayout?)
    open fun resetFromAdapter(adapter: SlidingAdapter?)
    open fun onTouchEvent(event: MotionEvent?): Boolean
    open fun computeScroll()
    open fun slideNext()
    open fun slidePrevious()
}