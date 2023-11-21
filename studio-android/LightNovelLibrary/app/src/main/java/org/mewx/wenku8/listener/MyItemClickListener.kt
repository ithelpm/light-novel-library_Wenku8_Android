package org.mewx.wenku8.listener

import android.view.View

// declare the click interface
interface MyItemClickListener {
    open fun onItemClick(view: View?, position: Int)
}