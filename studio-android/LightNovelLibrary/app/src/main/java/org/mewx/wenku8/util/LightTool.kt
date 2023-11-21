package org.mewx.wenku8.util

import android.content.Context

/**
 * Created by MewX on 2015/6/13.
 *
 * Util tools collects.
 */
object LightTool {
    private var displayCutout: Rect? = Rect(0, 0, 0, 0)

    /* Number related useful functions */
    fun isInteger(@NonNull value: String?): Boolean {
        return try {
            Integer.parseInt(value)
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    fun isDouble(@NonNull value: String?): Boolean {
        return try {
            Double.parseDouble(value)
            value.contains(".")
        } catch (e: NumberFormatException) {
            false
        }
    }

    fun isNumber(value: String?): Boolean {
        return isInteger(value) || isDouble(value)
    }

    /* Navigation bar useful functions */
    fun getNavigationBarSize(context: Context?): Point? {
        val appUsableSize: Point? = getAppUsableScreenSize(context)
        val realScreenSize: Point? = getRealScreenSize(context)

        // navigation bar on the right
        if (appUsableSize.x < realScreenSize.x) {
            return Point(realScreenSize.x - appUsableSize.x, appUsableSize.y)
        }

        // navigation bar at the bottom
        return if (appUsableSize.y < realScreenSize.y) {
            Point(appUsableSize.x, realScreenSize.y - appUsableSize.y)
        } else Point()

        // navigation bar is not present
    }

    fun getAppUsableScreenSize(context: Context?): Point? {
        val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display: Display = windowManager.getDefaultDisplay()
        val size = Point()
        display.getSize(size)
        return size
    }

    fun setDisplayCutout(rect: Rect?) {
        displayCutout = rect
    }

    fun getDisplayCutout(): Rect? {
        // Make a copy.
        return Rect(displayCutout)
    }

    fun getRealScreenSize(context: Context?): Point? {
        val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display: Display = windowManager.getDefaultDisplay()
        val size = Point()
        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealSize(size)
        } else {
            try {
                size.x = Display::class.java.getMethod("getRawWidth").invoke(display) as Integer
                size.y = Display::class.java.getMethod("getRawHeight").invoke(display) as Integer
            } catch (e: IllegalAccessException) {
            } catch (e: InvocationTargetException) {
            } catch (e: NoSuchMethodException) {
            }
        }
        return size
    }

    fun getStatusBarHeightValue(context: Context?): Int {
        var result = 0
        val resourceId: Int = context.getResources().getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId)
        }
        return result
    }

    fun getNavigationBarHeightValue(context: Context?): Int {
        var result = 0
        val resourceId: Int = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId)
        }
        return result
    }

    /* dp px sp tools */
    fun dip2px(context: Context?, dpValue: Float): Int {
        val scale: Float = context.getResources().getDisplayMetrics().density
        return (dpValue * scale + 0.5f).toInt()
    }

    fun px2dip(context: Context?, pxValue: Float): Int {
        val scale: Float = context.getResources().getDisplayMetrics().density
        return (pxValue / scale + 0.5f).toInt()
    }

    fun px2sp(context: Context?, px: Float): Float {
        val scaledDensity: Float = context.getResources().getDisplayMetrics().scaledDensity
        return px / scaledDensity
    }

    fun sp2px(context: Context?, sp: Float): Float {
        val scaledDensity: Float = context.getResources().getDisplayMetrics().scaledDensity
        return sp * scaledDensity
    }
}