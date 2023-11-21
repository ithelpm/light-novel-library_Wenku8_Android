package org.mewx.wenku8.reader.view

import android.app.Activity

/**
 * Created by MewX on 2015/7/8.
 *
 * Implement whole view of page, and use full screen page size.
 *
 * Default Elements:
 * - Top: ChapterTitle, WIFI/DC
 * - Bot: Battery, Paragraph/All, CurrentTime
 *
 * Click Elements:
 * - Top: NovelTitle
 * - Bot: ToolBar
 */
class WenkuReaderPageView(context: Context?, lineIndex: Int, wordIndex: Int, directionForward: LOADING_DIRECTION?) : View(context) {
    // enum
    enum class LOADING_DIRECTION {
        FORWARDS,  // go to next page
        CURRENT,  // get this page
        BACKWARDS // go to previous page
    }

    // class
    private inner class LineInfo {
        var type: WenkuReaderLoader.ElementType? = null
        var text: String? = null
    }

    var lineInfoList: List<LineInfo?>?

    private inner class BitmapInfo {
        var idxLineInfo = 0
        var width = 0
        var height = 0
        var x_beg = 0
        var y_beg = 0
        var bm: Bitmap? = null
    }

    var bitmapInfoList: List<BitmapInfo?>?
    private val textAreaSize // TODO: remove this variable.
            : Point?
    private val lineCount = 0

    // vars
    private var firstLineIndex = 0
    private var firstWordIndex = 0
    private var lastLineIndex = 0
    private var lastWordIndex = 0 // last paragraph's last word's index

    /**
     * Calculate the actual area for drawing.
     * @return the two points forming a rectangle that can actually hold content.
     * The two points are: top left point and bottom right point.
     */
    private fun getScreenLayout(): Pair<Point?, Point?>? {
        val statusBarHeight: Int = LightTool.getStatusBarHeightValue(MyApp.getContext())
        val navBarHeight: Int = LightTool.getNavigationBarHeightValue(MyApp.getContext())

        // Add cutting positions.
        val cutout: Rect = LightTool.getDisplayCutout()
        var top: Int = pxPageEdgeDistance + Math.max(cutout.top, statusBarHeight)
        val left: Int = pxPageEdgeDistance + cutout.left
        val right: Int = pxPageEdgeDistance + cutout.right
        var bottom: Int = pxPageEdgeDistance + pxWidgetHeight + cutout.bottom
        if (Build.VERSION.SDK_INT < 19) {
            // Status bar didn't support transparent.
            top -= statusBarHeight
            // Navigation bar didn't support transparent.
            bottom += navBarHeight
        }
        val topLeft = Point(left, top)
        val bottomRight = Point(screenSize.x - right, screenSize.y - bottom)
        return Pair(topLeft, bottomRight)
    }

    /**
     * This function init the view class。
     * Notice: (-1, -1), (-1, 0), (0, -1) means first page.
     * @param context current context, should be WenkuReaderActivity
     * @param lineIndex if FORWARDS, this is the last index of last page;
     * if CURRENT, this is the first index of this page;
     * if BACKWARDS, this is the first index of last page;
     * @param directionForward get next or get previous
     */
    init {
        Log.d("MewX", "-- view: construct my")
        lineInfoList = ArrayList()
        bitmapInfoList = ArrayList()
        mLoader.setCurrentIndex(lineIndex)

        // first.x = left
        // first.y = top
        // second.x = screen.x - right
        // second.y = screen.y - bottom
        screenDrawArea = getScreenLayout()

        // get environmental vars, use actual layout size: width x height
        textAreaSize = Point(screenDrawArea.second.x - screenDrawArea.first.x,
                screenDrawArea.second.y - screenDrawArea.first.y)
        when (directionForward) {
            LOADING_DIRECTION.FORWARDS -> {
                if (wordIndex + 1 < mLoader.getCurrentStringLength()) {
                    firstLineIndex = lineIndex
                    firstWordIndex = if (lineIndex == 0 && wordIndex == 0) 0 else wordIndex + 1
                } else if (lineIndex + 1 < mLoader.getElementCount()) {
                    firstLineIndex = lineIndex + 1
                    firstWordIndex = 0
                } else {
                    Log.d("MewX", "-- view: end construct A, just return")
                    return
                }
                mLoader.setCurrentIndex(firstLineIndex)
                calcFromFirst()
            }

            LOADING_DIRECTION.CURRENT -> {
                firstLineIndex = lineIndex
                firstWordIndex = wordIndex
                mLoader.setCurrentIndex(firstLineIndex)
                calcFromFirst()
            }

            LOADING_DIRECTION.BACKWARDS -> {
                // fit first and last
                if (wordIndex > 0) {
                    lastLineIndex = lineIndex
                    lastWordIndex = wordIndex - 1
                } else if (lineIndex > 0) {
                    lastLineIndex = lineIndex - 1
                    lastWordIndex = mLoader.getStringLength(lastLineIndex) - 1
                }

                // firstLineIndex firstWordIndex; and last values changeable
                mLoader.setCurrentIndex(lastLineIndex)
                calcFromLast()
            }
        }
        for (li in lineInfoList) Log.d("MewX", "get: " + li.text)
    }

    /**
     * Calc page from first to last.
     * firstLineIndex & firstWordIndex set.
     */
    private fun calcFromFirst() {
        var widthSum = 0
        var heightSum = fontHeight
        var tempText: StringBuilder? = StringBuilder()
        Log.d("MewX", "firstLineIndex = $firstLineIndex; firstWordIndex = $firstWordIndex")
        var curLineIndex = firstLineIndex
        var curWordIndex = firstWordIndex
        while (curLineIndex < mLoader.getElementCount()) {

            // init paragraph head vars
            if (curWordIndex == 0 && mLoader.getCurrentType() === WenkuReaderLoader.ElementType.TEXT) {
                // leading space
                widthSum = 2 * fontHeight
                tempText = StringBuilder("　　")
            } else if (mLoader.getCurrentType() === WenkuReaderLoader.ElementType.IMAGE_DEPENDENT) {
                if (lineInfoList.size() !== 0) {
                    // end a page first
                    lastLineIndex = mLoader.getCurrentIndex() - 1
                    mLoader.setCurrentIndex(lastLineIndex)
                    lastWordIndex = mLoader.getCurrentStringLength() - 1
                    break
                }

                // one image on page
                firstLineIndex = mLoader.getCurrentIndex()
                lastLineIndex = firstLineIndex
                firstWordIndex = 0
                lastWordIndex = mLoader.getCurrentStringLength() - 1
                val li: LineInfo = LineInfo()
                li.type = WenkuReaderLoader.ElementType.IMAGE_DEPENDENT
                li.text = mLoader.getCurrentAsString()
                lineInfoList.add(li)
                break
            }

            // get a record of line
            if (mLoader.getCurrentAsString() == null || mLoader.getCurrentStringLength() === 0) {
                Log.d("MewX", "empty string! in $curLineIndex($curWordIndex)")
                curWordIndex = 0
                if (curLineIndex >= mLoader.getElementCount()) {
                    // out of bounds
                    break
                }
                mLoader.setCurrentIndex(++curLineIndex)
                continue
            }
            val temp: String = mLoader.getCurrentAsString().charAt(curWordIndex) + ""
            val tempWidth = textPaint.measureText(temp) as Int

            // Line full?
            if (widthSum + tempWidth > textAreaSize.x) {
                // wrap line, save line
                val li: LineInfo = LineInfo()
                li.type = WenkuReaderLoader.ElementType.TEXT
                li.text = tempText.toString()
                lineInfoList.add(li)
                heightSum += pxLineDistance

                // change vars for next line
                if (heightSum + fontHeight > textAreaSize.y) {
                    // reverse one index
                    if (curWordIndex > 0) {
                        lastLineIndex = curLineIndex
                        lastWordIndex = curWordIndex - 1
                    } else if (curLineIndex > 0) {
                        mLoader.setCurrentIndex(--curLineIndex)
                        lastLineIndex = curLineIndex
                        lastWordIndex = mLoader.getCurrentStringLength() - 1
                    } else {
                        lastWordIndex = 0
                        lastLineIndex = lastWordIndex
                    }
                    break // height overflow
                }

                // height acceptable
                tempText = StringBuilder(temp)
                widthSum = tempWidth
                heightSum += fontHeight
            } else {
                tempText.append(temp)
                widthSum += tempWidth
            }

            // String end?
            if (curWordIndex + 1 >= mLoader.getCurrentStringLength()) {
                // next paragraph, wrap line
                val li: LineInfo = LineInfo()
                li.type = WenkuReaderLoader.ElementType.TEXT
                li.text = tempText.toString()
                lineInfoList.add(li)
                heightSum += pxParagraphDistance

                // height not acceptable
                if (heightSum + fontHeight > textAreaSize.y) {
                    lastLineIndex = mLoader.getCurrentIndex()
                    lastWordIndex = mLoader.getCurrentStringLength() - 1
                    break // height overflow
                }

                // height acceptable
                heightSum += fontHeight
                widthSum = 0
                tempText = StringBuilder()
                curWordIndex = 0
                if (curLineIndex + 1 >= mLoader.getElementCount()) {
                    // out of bounds
                    lastLineIndex = curLineIndex
                    lastWordIndex = mLoader.getCurrentStringLength() - 1
                    break
                }
                mLoader.setCurrentIndex(++curLineIndex)
            } else {
                curWordIndex++
            }
        }
    }

    /**
     * Calc page from last to first
     * lastLineIndex & lastWordIndex set.
     */
    private fun calcFromLast() {
        var heightSum = 0
        var isFirst = true
        mLoader.setCurrentIndex(lastLineIndex)
        var curLineIndex = lastLineIndex
        var curWordIndex = lastWordIndex
        LineLoop@ while (curLineIndex >= 0) {

            // calc curLine to curWord(contained), make a String list
            val curType: WenkuReaderLoader.ElementType = mLoader.getCurrentType()
            val curString: String = mLoader.getCurrentAsString()

            // special to image
            if (curType === WenkuReaderLoader.ElementType.IMAGE_DEPENDENT && lineInfoList.size() !== 0) {
                Log.d("MewX", "jump 1")
                firstLineIndex = curLineIndex + 1
                firstWordIndex = 0
                mLoader.setCurrentIndex(firstLineIndex)
                lineInfoList = ArrayList()
                calcFromFirst()
                break
            } else if (curType === WenkuReaderLoader.ElementType.IMAGE_DEPENDENT) {
                // one image on page
                firstLineIndex = mLoader.getCurrentIndex()
                lastLineIndex = firstLineIndex
                firstWordIndex = 0
                lastWordIndex = mLoader.getCurrentStringLength() - 1
                val li: LineInfo = LineInfo()
                li.type = WenkuReaderLoader.ElementType.IMAGE_DEPENDENT
                li.text = mLoader.getCurrentAsString()
                lineInfoList.add(li)
                break
            }
            var tempWidth = 0
            val curList: List<LineInfo?> = ArrayList()
            var temp = ""
            run {
                var i = 0
                while (i < curString.length()) {
                    if (i == 0) {
                        tempWidth += fontHeight + fontHeight
                        temp = "　　"
                    }
                    val c: String = curString.charAt(i) + ""
                    val width = textPaint.measureText(c) as Int
                    if (tempWidth + width > textAreaSize.x) {
                        // save line to next
                        val li: LineInfo = LineInfo()
                        li.type = WenkuReaderLoader.ElementType.TEXT
                        li.text = temp
                        curList.add(li)

                        // fit needs
                        if (i >= curWordIndex) break

                        // goto next round
                        tempWidth = 0
                        temp = ""
                        continue
                    } else {
                        temp = temp + c
                        tempWidth += width
                        i++
                    }

                    // string end
                    if (i == curString.length()) {
                        val li: LineInfo = LineInfo()
                        li.type = WenkuReaderLoader.ElementType.TEXT
                        li.text = temp
                        curList.add(li)
                    }
                }
            }

            // reverse to add to lineInfoList, full to break, image to do calcFromFirst then break
            for (i in curList.size() - 1 downTo 0) {
                if (isFirst) isFirst = false else if (i == curList.size() - 1) heightSum += pxParagraphDistance else heightSum += pxLineDistance
                heightSum += fontHeight
                if (heightSum > textAreaSize.y) {
                    // calc first index
                    var indexCount = -2
                    for (j in 0..i) indexCount += curList[j].text.length()
                    firstLineIndex = curLineIndex
                    firstWordIndex = indexCount + 1

                    // out of index
                    if (firstWordIndex + 1 >= curString.length()) {
                        firstLineIndex = curLineIndex + 1
                        firstWordIndex = 0
                    }
                    break@LineLoop
                }
                lineInfoList.add(0, curList[i])
            }
            for (li in lineInfoList) Log.d("MewX", "full: " + li.text)

            // not full to continue, set curWord as last index of the string
            if (curLineIndex - 1 >= 0) {
                mLoader.setCurrentIndex(--curLineIndex)
                curWordIndex = mLoader.getCurrentStringLength()
            } else {
                Log.d("MewX", "jump 2")
                firstLineIndex = 0
                firstWordIndex = 0
                mLoader.setCurrentIndex(firstLineIndex)
                lineInfoList = ArrayList()
                calcFromFirst()
                break
            }
        }
    }

    private fun drawBackground(canvas: Canvas?) {
        if (getInDayMode()) {
            // day
            if (bmdBackground != null) bmdBackground.draw(canvas)
            if (bmBackgroundYellow.getWidth() !== screenSize.x || bmBackgroundYellow.getHeight() !== screenSize.y) bmBackgroundYellow = Bitmap.createScaledBitmap(bmBackgroundYellow, screenSize.x, screenSize.y, true)
            canvas.drawBitmap(bmBackgroundYellow, 0, 0, null)
        } else {
            // night
            val paintBackground = Paint()
            paintBackground.setColor(mSetting.bgColorDark)
            canvas.drawRect(0, 0, screenSize.x, screenSize.y, paintBackground)
        }
    }

    private fun drawWidgets(canvas: Canvas?) {
        canvas.drawText(mLoader.getChapterName(), screenDrawArea.first.x, screenDrawArea.second.y + widgetFontHeihgt, widgetTextPaint)
        val percentage = "( " + (lastLineIndex + 1) * 100 / mLoader.getElementCount() + "% )"
        val textWidth = widgetTextPaint.measureText(percentage) as Int
        canvas.drawText(percentage, screenDrawArea.second.x - textWidth, screenDrawArea.second.y + widgetFontHeihgt, widgetTextPaint)
    }

    private fun drawContent(canvas: Canvas?) {
        var heightSum: Int = screenDrawArea.first.y + fontHeight // The baseline (i.e. y).
        for (i in 0 until lineInfoList.size()) {
            val li = lineInfoList.get(i)
            if (i != 0) {
                heightSum += if (li.text.length() > 2 && li.text.substring(0, 2).equals("　　")) {
                    pxParagraphDistance
                } else {
                    pxLineDistance
                }
            }
            Log.d(WenkuReaderPageView::class.java.getSimpleName(), "draw: " + li.text)
            if (li.type === WenkuReaderLoader.ElementType.TEXT) {
                canvas.drawText(li.text, screenDrawArea.first.x as Float, heightSum.toFloat(), textPaint)
                heightSum += fontHeight
            } else if (li.type === WenkuReaderLoader.ElementType.IMAGE_DEPENDENT) {
                if (bitmapInfoList == null) {
                    // TODO: fix this magic number 21.
                    canvas.drawText("Unexpected array: " + li.text.substring(21), screenDrawArea.first.x as Float, heightSum.toFloat(), textPaint)
                    continue
                }
                var bi: BitmapInfo? = null
                for (bitmapInfo in bitmapInfoList) {
                    if (bitmapInfo.idxLineInfo == i) {
                        bi = bitmapInfo
                        break
                    }
                }
                if (bi == null) {
                    // not found, new load task
                    // TODO: fix this magic number 21.
                    canvas.drawText("正在加载图片：" + li.text.substring(21), screenDrawArea.first.x as Float, heightSum.toFloat(), textPaint)
                    bi = BitmapInfo()
                    bi.idxLineInfo = i
                    bi.x_beg = screenDrawArea.first.x
                    bi.y_beg = screenDrawArea.first.y
                    bi.height = textAreaSize.y
                    bi.width = textAreaSize.x
                    bitmapInfoList.add(0, bi)
                    val ali: AsyncLoadImage = AsyncLoadImage()
                    ali.execute(bitmapInfoList.get(0))
                } else {
                    if (bi.bm == null) {
                        // TODO: fix this magic number 21.
                        canvas.drawText("正在加载图片：" + li.text.substring(21), screenDrawArea.first.x as Float, heightSum.toFloat(), textPaint)
                    } else {
                        val new_x: Int = (screenDrawArea.second.x - screenDrawArea.first.x - bi.width) / 2 + bi.x_beg
                        val new_y: Int = (screenDrawArea.second.y - screenDrawArea.first.y - bi.height) / 2 + bi.y_beg
                        canvas.drawBitmap(bi.bm, new_x, new_y, Paint())
                    }
                }
            } else {
                // TODO: fix this magic number 21.
                canvas.drawText("（！请先用旧引擎浏览）图片" + li.text.substring(21), screenDrawArea.first.x as Float, heightSum.toFloat(), textPaint)
            }
        }
    }

    @Override
    protected fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (mSetting == null || mLoader == null) return

        // Draw everything.
        Log.d(WenkuReaderPageView::class.java.getSimpleName(), "onDraw()")
        drawBackground(canvas)
        drawWidgets(canvas)
        drawContent(canvas)
    }

    fun getFirstLineIndex(): Int {
        return firstLineIndex
    }

    fun getFirstWordIndex(): Int {
        return firstWordIndex
    }

    fun getLastLineIndex(): Int {
        return lastLineIndex
    }

    fun getLastWordIndex(): Int {
        return lastWordIndex
    }

    private inner class AsyncLoadImage : AsyncTask<BitmapInfo?, Integer?, Wenku8Error.ErrorCode?>() {
        @Override
        protected fun doInBackground(vararg params: BitmapInfo?): Wenku8Error.ErrorCode? {
            // Make an alias for the bitmap info.
            val bitmapInfo = params[0]
            var imgFileName: String = GlobalConfig.generateImageFileNameByURL(lineInfoList.get(bitmapInfo.idxLineInfo).text)
            if (GlobalConfig.getAvailableNovelContentImagePath(imgFileName) == null) {
                if (!GlobalConfig.saveNovelContentImage(lineInfoList.get(bitmapInfo.idxLineInfo).text)) {
                    return Wenku8Error.ErrorCode.NETWORK_ERROR
                }

                // Double check if the image exists in local storage.
                if (GlobalConfig.getAvailableNovelContentImagePath(imgFileName) == null) {
                    return Wenku8Error.ErrorCode.STORAGE_ERROR
                }

                // The image should be downloaded.
                imgFileName = GlobalConfig.generateImageFileNameByURL(lineInfoList.get(bitmapInfo.idxLineInfo).text)
            }
            val targetSize = ImageSize(bitmapInfo.width, bitmapInfo.height) // result Bitmap will be fit to this size
            bitmapInfo.bm = ImageLoader.getInstance().loadImageSync("file://" + GlobalConfig.getAvailableNovelContentImagePath(imgFileName), targetSize)
            if (bitmapInfo.bm == null) {
                return Wenku8Error.ErrorCode.IMAGE_LOADING_ERROR
            }
            val width: Int = bitmapInfo.bm.getWidth()
            val height: Int = bitmapInfo.bm.getHeight()
            if (bitmapInfo.height / bitmapInfo.width.toFloat() > height / width.toFloat()) {
                // fit width
                val percentage = height.toFloat() / width
                bitmapInfo.height = (bitmapInfo.width * percentage).toInt()
            } else {
                // fit height
                val percentage = width.toFloat() / height
                bitmapInfo.width = (bitmapInfo.height * percentage).toInt()
            }
            bitmapInfo.bm = Bitmap.createScaledBitmap(bitmapInfo.bm, bitmapInfo.width, bitmapInfo.height, true)
            return Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED
        }

        @Override
        protected fun onPostExecute(errorCode: Wenku8Error.ErrorCode?) {
            super.onPostExecute(errorCode)
            if (errorCode === Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED) {
                this@WenkuReaderPageView.postInvalidate()
            } else {
                Log.e(TAG, "onPostExecute: image cannot be loaded " + errorCode.toString())
                Toast.makeText(getContext(), errorCode.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun watchImageDetailed(activity: Activity?) {
        if (bitmapInfoList == null || bitmapInfoList.size() === 0 || bitmapInfoList.get(0).bm == null) {
            Toast.makeText(getContext(), getResources().getString(R.string.reader_view_image_no_image), Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(activity, ViewImageDetailActivity::class.java)
            intent.putExtra("path", GlobalConfig.getAvailableNovelContentImagePath(GlobalConfig.generateImageFileNameByURL(lineInfoList.get(bitmapInfoList.get(0).idxLineInfo).text)))
            activity.startActivity(intent)
            activity.overridePendingTransition(R.anim.fade_in, R.anim.hold) // fade in animation
        }
    }

    companion object {
        private val TAG: String? = WenkuReaderPageView::class.java.getSimpleName()

        // core variables
        private var inDayMode = true
        private val sampleText: String? = "轻"
        private var mLoader: WenkuReaderLoader? = null
        private var mSetting: WenkuReaderSettingV1? = null
        private var pxLineDistance = 0
        private var pxParagraphDistance = 0
        private var pxPageEdgeDistance = 0
        private var pxWidgetHeight = 0
        private var screenSize: Point? = null // Screen real size.
        private var screenDrawArea // The area we want to draw text/images in.
                : Pair<Point?, Point?>?
        private var typeface: Typeface? = null
        private var textPaint: TextPaint? = null
        private var widgetTextPaint: TextPaint? = null
        private var fontHeight = 0
        private var widgetFontHeihgt = 0

        // background
        private var bmBackgroundYellow: Bitmap? = null
        private var bmTextureYellow: Array<Bitmap?>?
        private var bmdBackground: BitmapDrawable? = null
        private val random: Random? = Random()
        private var isBackgroundSet = false

        // view components (battery, page number, etc.)
        fun getInDayMode(): Boolean {
            return inDayMode
        }

        fun switchDayMode(): Boolean {
            inDayMode = !inDayMode
            return inDayMode
        }

        /**
         * Set view static variables, before first onDraw()
         * @param wrl loader
         * @param wrs setting
         */
        fun setViewComponents(wrl: WenkuReaderLoader?, wrs: WenkuReaderSettingV1?, forceMode: Boolean) {
            mLoader = wrl
            mSetting = wrs
            pxLineDistance = LightTool.dip2px(MyApp.getContext(), mSetting.getLineDistance()) // 行间距
            pxParagraphDistance = LightTool.dip2px(MyApp.getContext(), mSetting.getParagraphDistance()) // 段落间距
            pxPageEdgeDistance = LightTool.dip2px(MyApp.getContext(), mSetting.getPageEdgeDistance()) // 页面边距

            // calc general var
            try {
                if (mSetting.getUseCustomFont()) typeface = Typeface.createFromFile(mSetting.getCustomFontPath()) // custom font
            } catch (e: Exception) {
                Toast.makeText(MyApp.getContext(), e.toString() + "\n可能的原因有：字体文件不在内置SD卡；内存太小字体太大，请使用简体中文字体，而不是CJK或GBK，谢谢，此功能为试验性功能；", Toast.LENGTH_SHORT).show()
            }
            textPaint = TextPaint()
            textPaint.setColor(if (getInDayMode()) mSetting.fontColorDark else mSetting.fontColorLight)
            textPaint.setTextSize(LightTool.sp2px(MyApp.getContext(), mSetting.getFontSize() as Float))
            if (mSetting.getUseCustomFont() && typeface != null) textPaint.setTypeface(typeface)
            textPaint.setAntiAlias(true)
            fontHeight = textPaint.measureText(sampleText) as Int // in "px"
            widgetTextPaint = TextPaint()
            widgetTextPaint.setColor(if (getInDayMode()) mSetting.fontColorDark else mSetting.fontColorLight)
            widgetTextPaint.setTextSize(LightTool.sp2px(MyApp.getContext(), mSetting.widgetTextSize as Float))
            widgetTextPaint.setAntiAlias(true)
            widgetFontHeihgt = textPaint.measureText(sampleText) as Int

            // Update widget height.
            pxWidgetHeight = LightTool.dip2px(MyApp.getContext(), mSetting.widgetHeight) // default.
            pxWidgetHeight = 3 * widgetFontHeihgt / 2 // 2/3 font height

            // load bitmap
            if (forceMode || !isBackgroundSet) {
                screenSize = LightTool.getRealScreenSize(MyApp.getContext())
                if (Build.VERSION.SDK_INT < 19) {
                    screenSize.y -= LightTool.getStatusBarHeightValue(MyApp.getContext())
                }
                if (mSetting.getPageBackgroundType() === WenkuReaderSettingV1.PAGE_BACKGROUND_TYPE.CUSTOM) {
                    try {
                        bmBackgroundYellow = BitmapFactory.decodeFile(mSetting.getPageBackgrounCustomPath())
                    } catch (oome: OutOfMemoryError) {
                        try {
                            val options: BitmapFactory.Options = Options()
                            options.inSampleSize = 2
                            bmBackgroundYellow = BitmapFactory.decodeFile(mSetting.getPageBackgrounCustomPath(), options)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            return
                        }
                    }
                    bmdBackground = null
                }
                if (mSetting.getPageBackgroundType() === WenkuReaderSettingV1.PAGE_BACKGROUND_TYPE.SYSTEM_DEFAULT || bmBackgroundYellow == null) {
                    // use system default
                    bmBackgroundYellow = BitmapFactory.decodeResource(MyApp.getContext().getResources(), R.drawable.reader_bg_yellow_edge)
                    bmTextureYellow = arrayOfNulls<Bitmap?>(3)
                    bmTextureYellow.get(0) = BitmapFactory.decodeResource(MyApp.getContext().getResources(), R.drawable.reader_bg_yellow1)
                    bmTextureYellow.get(1) = BitmapFactory.decodeResource(MyApp.getContext().getResources(), R.drawable.reader_bg_yellow2)
                    bmTextureYellow.get(2) = BitmapFactory.decodeResource(MyApp.getContext().getResources(), R.drawable.reader_bg_yellow3)
                    bmdBackground = BitmapDrawable(MyApp.getContext().getResources(), bmTextureYellow.get(random.nextInt(bmTextureYellow.size)))
                    bmdBackground.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                    bmdBackground.setBounds(0, 0, screenSize.x, screenSize.y)
                }
                isBackgroundSet = true
            }
        }

        /**
         * Reset text color, to fit day/night mode.
         * If textPaint is null, then do nothing.
         */
        fun resetTextColor() {
            textPaint.setColor(if (getInDayMode()) mSetting.fontColorDark else mSetting.fontColorLight)
            widgetTextPaint.setColor(if (getInDayMode()) mSetting.fontColorDark else mSetting.fontColorLight)
        }
    }
}