package org.mewx.wenku8.reader.setting

import android.graphics.Color

/**
 * Created by MewX on 2015/7/8.
 *
 * This is the first version of reader activity setting.
 * New version extends from this setting class.
 */
class WenkuReaderSettingV1 {
    /**
     * Setting values, containing default value;
     * Setting Class must be defined before reader activity created.
     */
    // enum type
    enum class PAGE_BACKGROUND_TYPE {
        SYSTEM_DEFAULT, CUSTOM
    }

    // global settings
    val fontColorLight: Int = Color.parseColor("#32414E") // for dark background (ARGB)
    val fontColorDark: Int = Color.parseColor("#444444") // for light background
    val bgColorLight: Int = Color.parseColor("#CFBEB6")
    val bgColorDark: Int = Color.parseColor("#090C13")
    val widgetHeight = 24 // in "dp"
    val widgetTextSize = 12 // in "sp"

    // font setting
    private var fontSize = 18 // in "sp"
    private var useCustomFont = false // Custom font must declare this first!
    private var customFontPath: String? = ""

    // paragraph setting
    private var lineDistance = 16 // in "dp"
    private var paragraphDistance = 20 // in "dp"

    // page setting
    private var pageEdgeDistance = 8 // in "dp", page edge distance
    private var pageBackgroundType: PAGE_BACKGROUND_TYPE? = PAGE_BACKGROUND_TYPE.SYSTEM_DEFAULT
    private var pageBackgrounCustomPath: String? = ""

    /**
     * Construct Function
     */
    init {
        // font size
        var size: String = GlobalConfig.getFromAllSetting(GlobalConfig.SettingItems.reader_font_size)
        if (size != null && LightTool.isInteger(size)) {
            val temp: Int = Integer.parseInt(size)
            if (8 <= temp && temp <= 32) fontSize = temp
        }

        // line distance
        size = GlobalConfig.getFromAllSetting(GlobalConfig.SettingItems.reader_line_distance)
        if (size != null && LightTool.isInteger(size)) {
            val temp: Int = Integer.parseInt(size)
            if (0 <= temp && temp <= 32) lineDistance = temp
        }

        // paragraph distance
        size = GlobalConfig.getFromAllSetting(GlobalConfig.SettingItems.reader_paragraph_distance)
        if (size != null && LightTool.isInteger(size)) {
            val temp: Int = Integer.parseInt(size)
            if (0 <= temp && temp <= 48) paragraphDistance = temp
        }

        // paragraph edge distance
        size = GlobalConfig.getFromAllSetting(GlobalConfig.SettingItems.reader_paragraph_edge_distance)
        if (size != null && LightTool.isInteger(size)) {
            val temp: Int = Integer.parseInt(size)
            if (0 <= temp && temp <= 32) pageEdgeDistance = temp
        }

        // background path
        size = GlobalConfig.getFromAllSetting(GlobalConfig.SettingItems.reader_background_path)
        if (size != null) {
            if (size.equals("0")) {
                pageBackgroundType = PAGE_BACKGROUND_TYPE.SYSTEM_DEFAULT
            } else if (LightCache.testFileExist(size)) {
                pageBackgroundType = PAGE_BACKGROUND_TYPE.CUSTOM
                pageBackgrounCustomPath = size
            }
        }

        // font path
        size = GlobalConfig.getFromAllSetting(GlobalConfig.SettingItems.reader_font_path)
        if (size != null) {
            if (size.equals("0")) {
                useCustomFont = false
            } else if (LightCache.testFileExist(size)) {
                useCustomFont = true
                customFontPath = size
            }
        }
    }

    /**
     * gets & sets functions
     */
    fun setFontSize(s: Int) {
        fontSize = s
        GlobalConfig.setToAllSetting(GlobalConfig.SettingItems.reader_font_size, Integer.toString(s))
    }

    fun getFontSize(): Int {
        return fontSize
    }

    fun setUseCustomFont(b: Boolean) {
        if (!b) setCustomFontPath("0")
        useCustomFont = b
    }

    fun getUseCustomFont(): Boolean {
        return useCustomFont
    }

    fun setCustomFontPath(s: String?) {
        // should test file before set this value, allow setting, but not allow use!
        customFontPath = s
        useCustomFont = !s.equals("0")
        GlobalConfig.setToAllSetting(GlobalConfig.SettingItems.reader_font_path, s)
    }

    fun getCustomFontPath(): String? {
        return customFontPath
    }

    fun setLineDistance(l: Int) {
        lineDistance = l
        GlobalConfig.setToAllSetting(GlobalConfig.SettingItems.reader_line_distance, Integer.toString(l))
    }

    fun getLineDistance(): Int {
        return lineDistance
    }

    fun setParagraphDistance(l: Int) {
        paragraphDistance = l
        GlobalConfig.setToAllSetting(GlobalConfig.SettingItems.reader_paragraph_distance, Integer.toString(l))
    }

    fun getParagraphDistance(): Int {
        return paragraphDistance
    }

    fun setPageEdgeDistance(l: Int) {
        pageEdgeDistance = l
        GlobalConfig.setToAllSetting(GlobalConfig.SettingItems.reader_paragraph_edge_distance, Integer.toString(l))
    }

    fun getPageEdgeDistance(): Int {
        return pageEdgeDistance
    }

    fun setPageBackgroundType(t: PAGE_BACKGROUND_TYPE?) {
        if (t == PAGE_BACKGROUND_TYPE.SYSTEM_DEFAULT) setPageBackgroundCustomPath("0")
        pageBackgroundType = t
    }

    fun getPageBackgroundType(): PAGE_BACKGROUND_TYPE? {
        return pageBackgroundType
    }

    fun setPageBackgroundCustomPath(s: String?) {
        pageBackgrounCustomPath = s
        pageBackgroundType = if (s.equals("0")) PAGE_BACKGROUND_TYPE.SYSTEM_DEFAULT else PAGE_BACKGROUND_TYPE.CUSTOM
        GlobalConfig.setToAllSetting(GlobalConfig.SettingItems.reader_background_path, s)
    }

    fun getPageBackgrounCustomPath(): String? {
        return pageBackgrounCustomPath
    }
}