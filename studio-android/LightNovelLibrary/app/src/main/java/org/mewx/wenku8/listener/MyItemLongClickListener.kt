package org.mewx.wenku8.listener

import android.view.View

// declare the long click interface
interface MyItemLongClickListener {
    open fun onItemLongClick(view: View?, position: Int)
}