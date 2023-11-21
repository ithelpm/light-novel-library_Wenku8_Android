package org.mewx.wenku8.reader.slider

import android.os.Bundle

/**
 * Created by xuzb on 10/22/14.
 */
abstract class SlidingAdapter<T> {
    private val mViews: Array<View?>?
    private var currentViewIndex = 0
    private var slidingLayout: SlidingLayout? = null

    init {
        mViews = arrayOfNulls<View?>(3)
    }

    fun setSlidingLayout(slidingLayout: SlidingLayout?) {
        this.slidingLayout = slidingLayout
    }

    fun getUpdatedCurrentView(): View? {
        var curView: View? = mViews.get(currentViewIndex)
        if (curView == null) {
            curView = getView(null, getCurrent())
            mViews.get(currentViewIndex) = curView
        } else {
            val updateView: View? = getView(curView, getCurrent())
            if (curView !== updateView) {
                curView = updateView
                mViews.get(currentViewIndex) = updateView
            }
        }
        return curView
    }

    fun getCurrentView(): View? {
        var curView: View? = mViews.get(currentViewIndex)
        if (curView == null) {
            curView = getView(null, getCurrent())
            mViews.get(currentViewIndex) = curView
        }
        return curView
    }

    private fun getView(index: Int): View? {
        return mViews.get((index + 3) % 3)
    }

    private fun setView(index: Int, view: View?) {
        mViews.get((index + 3) % 3) = view
    }

    fun getUpdatedNextView(): View? {
        var nextView: View? = getView(currentViewIndex + 1)
        val hasnext = hasNext()
        if (nextView == null && hasnext) {
            nextView = getView(null, getNext())
            setView(currentViewIndex + 1, nextView)
        } else if (hasnext) {
            val updatedView: View? = getView(nextView, getNext())
            if (updatedView !== nextView) {
                nextView = updatedView
                setView(currentViewIndex + 1, nextView)
            }
        }
        return nextView
    }

    fun getNextView(): View? {
        var nextView: View? = getView(currentViewIndex + 1)
        if (nextView == null && hasNext()) {
            nextView = getView(null, getNext())
            setView(currentViewIndex + 1, nextView)
        }
        return nextView
    }

    fun getUpdatedPreviousView(): View? {
        var prevView: View? = getView(currentViewIndex - 1)
        val hasprev = hasPrevious()
        if (prevView == null && hasprev) {
            prevView = getView(null, getPrevious())
            setView(currentViewIndex - 1, prevView)
        } else if (hasprev) {
            val updatedView: View? = getView(prevView, getPrevious())
            if (updatedView !== prevView) {
                prevView = updatedView
                setView(currentViewIndex - 1, prevView)
            }
        }
        return prevView
    }

    fun setPreviousView(view: View?) {
        setView(currentViewIndex - 1, view)
    }

    fun setNextView(view: View?) {
        setView(currentViewIndex + 1, view)
    }

    fun setCurrentView(view: View?) {
        setView(currentViewIndex, view)
    }

    fun getPreviousView(): View? {
        var prevView: View? = getView(currentViewIndex - 1)
        if (prevView == null && hasPrevious()) {
            prevView = getView(null, getPrevious())
            setView(currentViewIndex - 1, prevView)
        }
        return prevView
    }

    fun moveToNext() {
        // Move to next element
        computeNext()

        // Increase view index
        currentViewIndex = (currentViewIndex + 1) % 3
    }

    fun moveToPrevious() {
        // Move to next element
        computePrevious()

        // Increase view index
        currentViewIndex = (currentViewIndex + 2) % 3
    }

    abstract fun getView(contentView: View?, t: T?): View?
    abstract fun getCurrent(): T?
    abstract fun getNext(): T?
    abstract fun getPrevious(): T?
    abstract operator fun hasNext(): Boolean
    abstract fun hasPrevious(): Boolean
    protected abstract fun computeNext()
    protected abstract fun computePrevious()
    fun saveState(): Bundle? {
        return null
    }

    fun restoreState(parcelable: Parcelable?, loader: ClassLoader?) {
        currentViewIndex = 0
        if (mViews != null) {
            mViews[0] = null
            mViews[1] = null
            mViews[2] = null
        }
    }

    fun notifyDataSetChanged() {
        if (slidingLayout != null) {
            slidingLayout.resetFromAdapter()
            slidingLayout.postInvalidate()
        }
    }
}