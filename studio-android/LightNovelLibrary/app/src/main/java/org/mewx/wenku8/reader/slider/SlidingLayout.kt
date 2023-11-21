package org.mewx.wenku8.reader.slider

import android.content.Context

/**
 * Created by xuzb on 10/23/14.
 */
class SlidingLayout : ViewGroup {
    // 用于记录点击事件
    private var mDownMotionX = 0
    private var mDownMotionY = 0
    private var mDownMotionTime: Long = 0
    private var mOnTapListener: OnTapListener? = null
    private var mSlider: Slider? = null
    var mAdapter: SlidingAdapter? = null
    private var mRestoredAdapterState: Parcelable? = null
    private var mRestoredClassLoader: ClassLoader? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    fun setSlider(slider: Slider?) {
        mSlider = slider
        slider.init(this)
        resetFromAdapter()
    }

    fun getAdapter(): SlidingAdapter? {
        return mAdapter
    }

    fun setAdapter(adapter: SlidingAdapter?) {
        mAdapter = adapter
        mAdapter.setSlidingLayout(this)
        if (mRestoredAdapterState != null) {
            mAdapter.restoreState(mRestoredAdapterState, mRestoredClassLoader)
            mRestoredAdapterState = null
            mRestoredClassLoader = null
        }
        resetFromAdapter()
        postInvalidate()
    }

    @Override
    fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event.getAction()) {
            MotionEvent.ACTION_DOWN -> {
                mDownMotionX = event.getX() as Int
                mDownMotionY = event.getY() as Int
                mDownMotionTime = System.currentTimeMillis()
            }

            MotionEvent.ACTION_UP -> computeTapMotion(event)
        }
        return mSlider.onTouchEvent(event) || super.onTouchEvent(event)
    }

    fun setOnTapListener(l: OnTapListener?) {
        mOnTapListener = l
    }

    private fun computeTapMotion(event: MotionEvent?) {
        if (mOnTapListener == null) return
        val xDiff: Int = Math.abs(event.getX() as Int - mDownMotionX)
        val yDiff: Int = Math.abs(event.getY() as Int - mDownMotionY)
        val timeDiff: Long = System.currentTimeMillis() - mDownMotionTime
        if (xDiff < 5 && yDiff < 5 && timeDiff < 200) {
            mOnTapListener.onSingleTap(event)
        }
    }

    @Override
    fun computeScroll() {
        super.computeScroll()
        mSlider.computeScroll()
    }

    fun slideNext() {
        mSlider.slideNext()
    }

    fun slidePrevious() {
        mSlider.slidePrevious()
    }

    interface OnTapListener {
        open fun onSingleTap(event: MotionEvent?)
    }

    @Override
    protected fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until getChildCount()) {
            val child: View = getChildAt(i)
            val height: Int = child.getMeasuredHeight()
            val width: Int = child.getMeasuredWidth()
            child.layout(0, 0, width, height)
        }
    }

    @Override
    protected fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width: Int = MeasureSpec.getSize(widthMeasureSpec)
        val height: Int = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
        for (i in 0 until getChildCount()) {
            getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    class SavedState : BaseSavedState {
        var adapterState: Parcelable? = null
        var loader: ClassLoader? = null

        constructor(superState: Parcelable?) : super(superState)

        @Override
        fun writeToParcel(out: Parcel?, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeParcelable(adapterState, flags)
        }

        @Override
        fun toString(): String? {
            return ("BaseSlidingLayout.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    ) + "}"
        }

        internal constructor(`in`: Parcel?, loader: ClassLoader?) : super(`in`) {
            var loader: ClassLoader? = loader
            if (loader == null) {
                loader = getClass().getClassLoader()
            }
            adapterState = `in`.readParcelable(loader)
            this.loader = loader
        }

        companion object {
            val CREATOR: Creator<SavedState?>? = ParcelableCompat.newCreator(object : ParcelableCompatCreatorCallbacks<SavedState?>() {
                @Override
                fun createFromParcel(`in`: Parcel?, loader: ClassLoader?): SavedState? {
                    return SavedState(`in`, loader)
                }

                @Override
                fun newArray(size: Int): Array<SavedState?>? {
                    return arrayOfNulls<SavedState?>(size)
                }
            })
        }
    }

    @Override
    protected fun onSaveInstanceState(): Parcelable? {
        val superState: Parcelable = super.onSaveInstanceState()
        val ss = SavedState(superState)
        if (mAdapter != null) {
            ss.adapterState = mAdapter.saveState()
        }
        return ss
    }

    @Override
    protected fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        val ss = state as SavedState?
        super.onRestoreInstanceState(ss.getSuperState())
        if (mAdapter != null) {
            mAdapter.restoreState(ss.adapterState, ss.loader)
            resetFromAdapter()
        } else {
            mRestoredAdapterState = ss.adapterState
            mRestoredClassLoader = ss.loader
        }
    }

    fun resetFromAdapter() {
        removeAllViews()
        if (mSlider != null && mAdapter != null) mSlider.resetFromAdapter(mAdapter)
    }

    private var mSlideChangeListener: OnSlideChangeListener? = null
    fun setOnSlideChangeListener(l: OnSlideChangeListener?) {
        mSlideChangeListener = l
    }

    interface OnSlideChangeListener {
        open fun onSlideScrollStateChanged(touchResult: Int)
        open fun onSlideSelected(obj: Object?)
    }

    fun slideScrollStateChanged(moveDirection: Int) {
        if (mSlideChangeListener != null) mSlideChangeListener.onSlideScrollStateChanged(moveDirection)
    }

    fun slideSelected(obj: Object?) {
        if (mSlideChangeListener != null) mSlideChangeListener.onSlideSelected(obj)
    }
}