package org.mewx.wenku8.listener

import android.view.View

/**
 * MyDeleteClickListener
 * Created by MewX on 2015/10/21.
 */
interface MyOptionClickListener {
    open fun onOptionButtonClick(view: View?, position: Int)
}