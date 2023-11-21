/*
 * Copyright (C) 2013 Andreas Stuetz <andreas.stuetz@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mewx.wenku8.component

import android.annotation.SuppressLint

class PagerSlidingTabStrip(context: Context?, attrs: AttributeSet?, defStyle: Int) : HorizontalScrollView(context, attrs, defStyle) {
    interface CustomTabProvider {
        open fun getCustomTabView(parent: ViewGroup?, position: Int): View?
    }

    interface OnTabReselectedListener {
        open fun onTabReselected(position: Int)
    }

    // @formatter:on
    private val adapterObserver: PagerAdapterObserver? = PagerAdapterObserver()
    private val defaultTabLayoutParams: LinearLayout.LayoutParams?
    private val expandedTabLayoutParams: LinearLayout.LayoutParams?
    private val pageListener: PageListener? = PageListener()
    private var tabReselectedListener: OnTabReselectedListener? = null
    var delegatePageListener: OnPageChangeListener? = null
    private val tabsContainer: LinearLayout?
    private var pager: ViewPager? = null
    private var tabCount = 0
    private var currentPosition = 0
    private var currentPositionOffset = 0f
    private val rectPaint: Paint?
    private val dividerPaint: Paint?
    private var indicatorColor: Int
    private var indicatorHeight = 2
    private var underlineHeight = 0
    private var underlineColor: Int
    private var dividerWidth = 0
    private var dividerPadding = 0
    private var dividerColor: Int
    private var tabPadding = 12
    private var tabTextSize = 14
    private var tabTextColor: ColorStateList? = null
    private val tabTextAlpha = HALF_TRANSP
    private val tabTextSelectedAlpha = OPAQUE
    private var padding = 0
    private var shouldExpand = false
    private var textAllCaps = true
    private val isPaddingMiddle = false
    private var tabTypeface: Typeface? = null
    private val tabTypefaceStyle: Int = Typeface.BOLD
    private var tabTypefaceSelectedStyle: Int = Typeface.BOLD
    private var scrollOffset: Int
    private var lastScrollX = 0
    private var tabBackgroundResId: Int = R.drawable.bg_tab
    private val locale: Locale? = null

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    private fun setMarginBottomTabContainer() {
        val mlp: MarginLayoutParams = tabsContainer.getLayoutParams() as MarginLayoutParams
        val bottomMargin = if (indicatorHeight >= underlineHeight) indicatorHeight else underlineHeight
        mlp.setMargins(mlp.leftMargin, mlp.topMargin, mlp.rightMargin, bottomMargin)
        tabsContainer.setLayoutParams(mlp)
    }

    fun setViewPager(pager: ViewPager?) {
        this.pager = pager
        if (pager.getAdapter() == null) {
            throw IllegalStateException("ViewPager does not have adapter instance.")
        }
        pager.setOnPageChangeListener(pageListener)
        pager.getAdapter().registerDataSetObserver(adapterObserver)
        adapterObserver.setAttached(true)
        notifyDataSetChanged()
    }

    fun notifyDataSetChanged() {
        tabsContainer.removeAllViews()
        tabCount = pager.getAdapter().getCount()
        var tabView: View
        for (i in 0 until tabCount) {
            tabView = if (pager.getAdapter() is CustomTabProvider) {
                (pager.getAdapter() as CustomTabProvider).getCustomTabView(this, i)
            } else {
                LayoutInflater.from(getContext()).inflate(R.layout.view_tab, this, false)
            }
            val title: CharSequence = pager.getAdapter().getPageTitle(i)
            addTab(i, title, tabView)
        }
        updateTabStyles()
        getViewTreeObserver().addOnGlobalLayoutListener(object : OnGlobalLayoutListener() {
            @SuppressWarnings("deprecation")
            @SuppressLint("NewApi")
            @Override
            fun onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this)
                currentPosition = pager.getCurrentItem()
                currentPositionOffset = 0f
                scrollToChild(currentPosition, 0)
                updateSelection(currentPosition)
            }
        })
    }

    private fun addTab(position: Int, title: CharSequence?, tabView: View?) {
        val textView: TextView = tabView.findViewById(R.id.tab_title) as TextView
        if (textView != null) {
            if (title != null) textView.setText(title)
            val alpha = if (pager.getCurrentItem() === position) tabTextSelectedAlpha else tabTextAlpha
            ViewCompat.setAlpha(textView, alpha)
        }
        tabView.setFocusable(true)
        tabView.setOnClickListener(object : OnClickListener() {
            @Override
            fun onClick(v: View?) {
                if (pager.getCurrentItem() !== position) {
                    val tab: View = tabsContainer.getChildAt(pager.getCurrentItem())
                    notSelected(tab)
                    pager.setCurrentItem(position)
                } else if (tabReselectedListener != null) {
                    tabReselectedListener.onTabReselected(position)
                }
            }
        })
        tabsContainer.addView(tabView, position, if (shouldExpand) expandedTabLayoutParams else defaultTabLayoutParams)
    }

    private fun updateTabStyles() {
        for (i in 0 until tabCount) {
            val v: View = tabsContainer.getChildAt(i)
            v.setBackgroundResource(tabBackgroundResId)
            v.setPadding(tabPadding, v.getPaddingTop(), tabPadding, v.getPaddingBottom())
            val tab_title: TextView = v.findViewById(R.id.tab_title) as TextView
            if (tab_title != null) {
                tab_title.setTextSize(TypedValue.COMPLEX_UNIT_PX, tabTextSize)
                tab_title.setTypeface(tabTypeface, if (pager.getCurrentItem() === i) tabTypefaceSelectedStyle else tabTypefaceStyle)
                if (tabTextColor != null) {
                    tab_title.setTextColor(tabTextColor)
                }
                // setAllCaps() is only available from API 14, so the upper case is made manually if we are on a
                // pre-ICS-build
                if (textAllCaps) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        tab_title.setAllCaps(true)
                    } else {
                        tab_title.setText(tab_title.getText().toString().toUpperCase(locale))
                    }
                }
            }
        }
    }

    private fun scrollToChild(position: Int, offset: Int) {
        if (tabCount == 0) {
            return
        }
        var newScrollX: Int = tabsContainer.getChildAt(position).getLeft() + offset
        if (position > 0 || offset > 0) {

            //Half screen offset.
            //- Either tabs start at the middle of the view scrolling straight away
            //- Or tabs start at the begging (no padding) scrolling when indicator gets
            //  to the middle of the view width
            newScrollX -= scrollOffset
            val lines: Pair<Float?, Float?>? = getIndicatorCoordinates()
            newScrollX += (lines.second - lines.first) / 2
        }
        if (newScrollX != lastScrollX) {
            lastScrollX = newScrollX
            scrollTo(newScrollX, 0)
        }
    }

    private fun getIndicatorCoordinates(): Pair<Float?, Float?>? {
        // default: line below current view_tab
        val currentTab: View = tabsContainer.getChildAt(currentPosition)
        var lineLeft: Float = currentTab.getLeft()
        var lineRight: Float = currentTab.getRight()

        // if there is an offset, start interpolating left and right coordinates between current and next view_tab
        if (currentPositionOffset > 0f && currentPosition < tabCount - 1) {
            val nextTab: View = tabsContainer.getChildAt(currentPosition + 1)
            val nextTabLeft: Float = nextTab.getLeft()
            val nextTabRight: Float = nextTab.getRight()
            lineLeft = currentPositionOffset * nextTabLeft + (1f - currentPositionOffset) * lineLeft
            lineRight = currentPositionOffset * nextTabRight + (1f - currentPositionOffset) * lineRight
        }
        return Pair(lineLeft, lineRight)
    }

    @Override
    protected fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (isPaddingMiddle || padding > 0) {
            //Make sure tabContainer is bigger than the HorizontalScrollView to be able to scroll
            tabsContainer.setMinimumWidth(getWidth())
            //Clipping padding to false to see the tabs while we pass them swiping
            setClipToPadding(false)
        }
        if (tabsContainer.getChildCount() > 0) {
            tabsContainer
                    .getChildAt(0)
                    .getViewTreeObserver()
                    .addOnGlobalLayoutListener(firstTabGlobalLayoutListener)
        }
        super.onLayout(changed, l, t, r, b)
    }

    private val firstTabGlobalLayoutListener: OnGlobalLayoutListener? = object : OnGlobalLayoutListener() {
        @Override
        fun onGlobalLayout() {
            val view: View = tabsContainer.getChildAt(0)
            getViewTreeObserver().removeOnGlobalLayoutListener(this)
            if (isPaddingMiddle) {
                val mHalfWidthFirstTab: Int = view.getWidth() / 2
                padding = getWidth() / 2 - mHalfWidthFirstTab
            }
            setPadding(padding, getPaddingTop(), padding, getPaddingBottom())
            if (scrollOffset == 0) scrollOffset = getWidth() / 2 - padding
        }
    }

    init {
        setFillViewport(true)
        setWillNotDraw(false)
        tabsContainer = LinearLayout(context)
        tabsContainer.setOrientation(LinearLayout.HORIZONTAL)
        tabsContainer.setLayoutParams(LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(tabsContainer)
        val dm: DisplayMetrics = getResources().getDisplayMetrics()
        scrollOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, scrollOffset, dm) as Int
        indicatorHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, indicatorHeight, dm) as Int
        underlineHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, underlineHeight, dm) as Int
        dividerPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dividerPadding, dm) as Int
        tabPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, tabPadding, dm) as Int
        dividerWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dividerWidth, dm) as Int
        tabTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, tabTextSize, dm) as Int

        // get system attrs (android:textSize and android:textColor)
        var a: TypedArray = context.obtainStyledAttributes(attrs, ATTRS)
        tabTextSize = a.getDimensionPixelSize(TEXT_SIZE_INDEX, tabTextSize)
        val colorStateList: ColorStateList = a.getColorStateList(TEXT_COLOR_INDEX)
        val textPrimaryColor: Int = a.getColor(TEXT_COLOR_PRIMARY, getResources().getColor(android.R.color.white))
        tabTextColor = if (colorStateList != null) {
            colorStateList
        } else {
            getColorStateList(textPrimaryColor)
        }
        underlineColor = textPrimaryColor
        dividerColor = textPrimaryColor
        indicatorColor = textPrimaryColor
        val paddingLeft: Int = a.getDimensionPixelSize(PADDING_LEFT_INDEX, padding)
        val paddingRight: Int = a.getDimensionPixelSize(PADDING_RIGHT_INDEX, padding)
        a.recycle()

        //In case we have the padding they must be equal so we take the biggest
        padding = Math.max(paddingLeft, paddingRight)

        // get custom attrs
        a = context.obtainStyledAttributes(attrs, R.styleable.PagerSlidingTabStrip)
        indicatorColor = a.getColor(R.styleable.PagerSlidingTabStrip_pstsIndicatorColor, indicatorColor)
        underlineColor = a.getColor(R.styleable.PagerSlidingTabStrip_pstsUnderlineColor, underlineColor)
        dividerColor = a.getColor(R.styleable.PagerSlidingTabStrip_pstsDividerColor, dividerColor)
        dividerWidth = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsDividerWidth, dividerWidth)
        indicatorHeight = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsIndicatorHeight, indicatorHeight)
        underlineHeight = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsUnderlineHeight, underlineHeight)
        dividerPadding = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsDividerPadding, dividerPadding)
        tabPadding = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsTabPaddingLeftRight, tabPadding)
        tabBackgroundResId = a.getResourceId(R.styleable.PagerSlidingTabStrip_pstsTabBackground, tabBackgroundResId)
        shouldExpand = a.getBoolean(R.styleable.PagerSlidingTabStrip_pstsShouldExpand, shouldExpand)
        scrollOffset = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsScrollOffset, scrollOffset)
        textAllCaps = a.getBoolean(R.styleable.PagerSlidingTabStrip_pstsTextAllCaps, textAllCaps)
        isPaddingMiddle = a.getBoolean(R.styleable.PagerSlidingTabStrip_pstsPaddingMiddle, isPaddingMiddle)
        tabTypefaceStyle = a.getInt(R.styleable.PagerSlidingTabStrip_pstsTextStyle, Typeface.BOLD)
        tabTypefaceSelectedStyle = a.getInt(R.styleable.PagerSlidingTabStrip_pstsTextSelectedStyle, Typeface.BOLD)
        tabTextAlpha = a.getFloat(R.styleable.PagerSlidingTabStrip_pstsTextAlpha, HALF_TRANSP)
        tabTextSelectedAlpha = a.getFloat(R.styleable.PagerSlidingTabStrip_pstsTextSelectedAlpha, OPAQUE)
        a.recycle()
        setMarginBottomTabContainer()
        rectPaint = Paint()
        rectPaint.setAntiAlias(true)
        rectPaint.setStyle(Style.FILL)
        dividerPaint = Paint()
        dividerPaint.setAntiAlias(true)
        dividerPaint.setStrokeWidth(dividerWidth)
        defaultTabLayoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
        expandedTabLayoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f)
        if (locale == null) {
            locale = getResources().getConfiguration().locale
        }
    }

    @Override
    protected fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (isInEditMode() || tabCount == 0) {
            return
        }
        val height: Int = getHeight()
        // draw indicator line
        rectPaint.setColor(indicatorColor)
        val lines: Pair<Float?, Float?>? = getIndicatorCoordinates()
        canvas.drawRect(lines.first + padding, height - indicatorHeight, lines.second + padding, height, rectPaint)
        // draw underline
        rectPaint.setColor(underlineColor)
        canvas.drawRect(padding, height - underlineHeight, tabsContainer.getWidth() + padding, height, rectPaint)
        // draw divider
        if (dividerWidth != 0) {
            dividerPaint.setStrokeWidth(dividerWidth)
            dividerPaint.setColor(dividerColor)
            for (i in 0 until tabCount - 1) {
                val tab: View = tabsContainer.getChildAt(i)
                canvas.drawLine(tab.getRight(), dividerPadding, tab.getRight(), height - dividerPadding, dividerPaint)
            }
        }
    }

    fun setOnTabReselectedListener(tabReselectedListener: OnTabReselectedListener?) {
        this.tabReselectedListener = tabReselectedListener
    }

    fun setOnPageChangeListener(listener: OnPageChangeListener?) {
        delegatePageListener = listener
    }

    private inner class PageListener : OnPageChangeListener {
        @Override
        fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            currentPosition = position
            currentPositionOffset = positionOffset
            val offset = if (tabCount > 0) (positionOffset * tabsContainer.getChildAt(position).getWidth()) as Int else 0
            scrollToChild(position, offset)
            invalidate()
            if (delegatePageListener != null) {
                delegatePageListener.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }
        }

        @Override
        fun onPageScrollStateChanged(state: Int) {
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                scrollToChild(pager.getCurrentItem(), 0)
            }
            //Full alpha for current item
            val currentTab: View = tabsContainer.getChildAt(pager.getCurrentItem())
            selected(currentTab)
            //Half transparent for prev item
            if (pager.getCurrentItem() - 1 >= 0) {
                val prevTab: View = tabsContainer.getChildAt(pager.getCurrentItem() - 1)
                notSelected(prevTab)
            }
            //Half transparent for next item
            if (pager.getCurrentItem() + 1 <= pager.getAdapter().getCount() - 1) {
                val nextTab: View = tabsContainer.getChildAt(pager.getCurrentItem() + 1)
                notSelected(nextTab)
            }
            if (delegatePageListener != null) {
                delegatePageListener.onPageScrollStateChanged(state)
            }
        }

        @Override
        fun onPageSelected(position: Int) {
            updateSelection(position)
            if (delegatePageListener != null) {
                delegatePageListener.onPageSelected(position)
            }
        }
    }

    private fun updateSelection(position: Int) {
        for (i in 0 until tabCount) {
            val tv: View = tabsContainer.getChildAt(i)
            val selected = i == position
            tv.setSelected(selected)
            if (selected) {
                selected(tv)
            } else {
                notSelected(tv)
            }
        }
    }

    private fun notSelected(tab: View?) {
        if (tab != null) {
            val title: TextView = tab.findViewById(R.id.tab_title) as TextView
            if (title != null) {
                title.setTypeface(tabTypeface, tabTypefaceStyle)
                ViewCompat.setAlpha(title, tabTextAlpha)
            }
        }
    }

    private fun selected(tab: View?) {
        if (tab != null) {
            val title: TextView = tab.findViewById(R.id.tab_title) as TextView
            if (title != null) {
                title.setTypeface(tabTypeface, tabTypefaceSelectedStyle)
                ViewCompat.setAlpha(title, tabTextSelectedAlpha)
            }
        }
    }

    private inner class PagerAdapterObserver : DataSetObserver() {
        private var attached = false
        @Override
        fun onChanged() {
            notifyDataSetChanged()
        }

        fun setAttached(attached: Boolean) {
            this.attached = attached
        }

        fun isAttached(): Boolean {
            return attached
        }
    }

    @Override
    protected fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (pager != null) {
            if (!adapterObserver.isAttached()) {
                pager.getAdapter().registerDataSetObserver(adapterObserver)
                adapterObserver.setAttached(true)
            }
        }
    }

    @Override
    protected fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (pager != null) {
            if (adapterObserver.isAttached()) {
                try {
                    pager.getAdapter().unregisterDataSetObserver(adapterObserver)
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
                adapterObserver.setAttached(false)
            }
        }
    }

    @Override
    fun onRestoreInstanceState(state: Parcelable?) {
        val savedState = state as SavedState?
        super.onRestoreInstanceState(savedState.getSuperState())
        currentPosition = savedState.currentPosition
        if (currentPosition != 0 && tabsContainer.getChildCount() > 0) {
            notSelected(tabsContainer.getChildAt(0))
            selected(tabsContainer.getChildAt(currentPosition))
        }
        requestLayout()
    }

    @Override
    fun onSaveInstanceState(): Parcelable? {
        val superState: Parcelable = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        savedState.currentPosition = currentPosition
        return savedState
    }

    internal class SavedState : BaseSavedState {
        var currentPosition = 0

        constructor(superState: Parcelable?) : super(superState)
        private constructor(`in`: Parcel?) : super(`in`) {
            currentPosition = `in`.readInt()
        }

        @Override
        fun writeToParcel(@NonNull dest: Parcel?, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(currentPosition)
        }

        companion object {
            val CREATOR: Creator<SavedState?>? = object : Creator<SavedState?>() {
                @Override
                fun createFromParcel(`in`: Parcel?): SavedState? {
                    return SavedState(`in`)
                }

                @Override
                fun newArray(size: Int): Array<SavedState?>? {
                    return arrayOfNulls<SavedState?>(size)
                }
            }
        }
    }

    fun getIndicatorColor(): Int {
        return indicatorColor
    }

    fun getIndicatorHeight(): Int {
        return indicatorHeight
    }

    fun getUnderlineColor(): Int {
        return underlineColor
    }

    fun getDividerColor(): Int {
        return dividerColor
    }

    fun getDividerWidth(): Int {
        return dividerWidth
    }

    fun getUnderlineHeight(): Int {
        return underlineHeight
    }

    fun getDividerPadding(): Int {
        return dividerPadding
    }

    fun getScrollOffset(): Int {
        return scrollOffset
    }

    fun getShouldExpand(): Boolean {
        return shouldExpand
    }

    fun getTextSize(): Int {
        return tabTextSize
    }

    fun isTextAllCaps(): Boolean {
        return textAllCaps
    }

    fun getTextColor(): ColorStateList? {
        return tabTextColor
    }

    fun getTabBackground(): Int {
        return tabBackgroundResId
    }

    fun getTabPaddingLeftRight(): Int {
        return tabPadding
    }

    fun setIndicatorColor(indicatorColor: Int) {
        this.indicatorColor = indicatorColor
        invalidate()
    }

    fun setIndicatorColorResource(resId: Int) {
        indicatorColor = getResources().getColor(resId)
        invalidate()
    }

    fun setIndicatorHeight(indicatorLineHeightPx: Int) {
        indicatorHeight = indicatorLineHeightPx
        invalidate()
    }

    fun setUnderlineColor(underlineColor: Int) {
        this.underlineColor = underlineColor
        invalidate()
    }

    fun setUnderlineColorResource(resId: Int) {
        underlineColor = getResources().getColor(resId)
        invalidate()
    }

    fun setDividerColor(dividerColor: Int) {
        this.dividerColor = dividerColor
        invalidate()
    }

    fun setDividerColorResource(resId: Int) {
        dividerColor = getResources().getColor(resId)
        invalidate()
    }

    fun setDividerWidth(dividerWidthPx: Int) {
        dividerWidth = dividerWidthPx
        invalidate()
    }

    fun setUnderlineHeight(underlineHeightPx: Int) {
        underlineHeight = underlineHeightPx
        invalidate()
    }

    fun setDividerPadding(dividerPaddingPx: Int) {
        dividerPadding = dividerPaddingPx
        invalidate()
    }

    fun setScrollOffset(scrollOffsetPx: Int) {
        scrollOffset = scrollOffsetPx
        invalidate()
    }

    fun setShouldExpand(shouldExpand: Boolean) {
        this.shouldExpand = shouldExpand
        if (pager != null) {
            requestLayout()
        }
    }

    fun setAllCaps(textAllCaps: Boolean) {
        this.textAllCaps = textAllCaps
    }

    fun setTextSize(textSizePx: Int) {
        tabTextSize = textSizePx
        updateTabStyles()
    }

    fun setTextColor(textColor: Int) {
        setTextColor(getColorStateList(textColor))
    }

    private fun getColorStateList(textColor: Int): ColorStateList? {
        return ColorStateList(arrayOf<IntArray?>(intArrayOf()), intArrayOf(textColor))
    }

    fun setTextColor(colorStateList: ColorStateList?) {
        tabTextColor = colorStateList
        updateTabStyles()
    }

    fun setTextColorResource(resId: Int) {
        setTextColor(getResources().getColor(resId))
    }

    fun setTextColorStateListResource(resId: Int) {
        setTextColor(getResources().getColorStateList(resId))
    }

    fun setTypeface(typeface: Typeface?, style: Int) {
        tabTypeface = typeface
        tabTypefaceSelectedStyle = style
        updateTabStyles()
    }

    fun setTabBackground(resId: Int) {
        tabBackgroundResId = resId
    }

    fun setTabPaddingLeftRight(paddingPx: Int) {
        tabPadding = paddingPx
        updateTabStyles()
    }

    companion object {
        private const val OPAQUE = 1.0f
        private const val HALF_TRANSP = 0.5f

        // @formatter:off
        private val ATTRS: IntArray? = intArrayOf(
                android.R.attr.textColorPrimary,
                android.R.attr.textSize,
                android.R.attr.textColor,
                android.R.attr.paddingLeft,
                android.R.attr.paddingRight)

        //These indexes must be related with the ATTR array above
        private const val TEXT_COLOR_PRIMARY = 0
        private const val TEXT_SIZE_INDEX = 1
        private const val TEXT_COLOR_INDEX = 2
        private const val PADDING_LEFT_INDEX = 3
        private const val PADDING_RIGHT_INDEX = 4
    }
}