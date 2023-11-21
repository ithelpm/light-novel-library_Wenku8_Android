package org.mewx.wenku8.reader.slider.base

import kotlin.Throws

/**
 * Created by xuzb on 1/16/15.
 */
object BaseSlider : Slider {
    /** 手指移动的方向  */
    const val MOVE_TO_LEFT = 0 // Move to next
    const val MOVE_TO_RIGHT = 1 // Move to previous
    const val MOVE_NO_RESULT = 4

    /** 触摸的模式  */
    const val MODE_NONE = 0
    const val MODE_MOVE = 1
}