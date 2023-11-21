package org.mewx.wenku8

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

/**
 * The class is for getting context everywhere
 */
class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        context = getApplicationContextLocal()
    }

    /**
     * wrap the getApplicationContext() function for easier unit testing
     * @return the results from getApplicationContext()
     */
    fun getApplicationContextLocal(): Context? {
        return getApplicationContext()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var context: Context? = null
        fun getContext(): Context? {
            return context
        }
    }
}