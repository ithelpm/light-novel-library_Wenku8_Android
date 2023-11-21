package org.mewx.wenku8.reader.slider.base

import android.view.MotionEvent

/**
 * Created by xuzb on 1/16/15.
 */
class OverlappedSlider : BaseSlider() {
    private var mScroller: Scroller? = null
    private var mVelocityTracker: VelocityTracker? = null
    private var mVelocityValue = 0

    /** 商定这个滑动是否有效的距离  */
    private var limitDistance = 0
    private var screenWidth = 0

    /** 最后触摸的结果方向  */
    private var mTouchResult: Int = MOVE_NO_RESULT

    /** 一开始的方向  */
    private var mDirection: Int = MOVE_NO_RESULT
    private var mMode: Int = MODE_NONE

    /** 滑动的view  */
    private var mScrollerView: View? = null
    private var startX = 0
    private var mSlidingLayout: SlidingLayout? = null
    private fun getAdapter(): SlidingAdapter? {
        return mSlidingLayout.getAdapter()
    }

    @Override
    fun init(slidingLayout: SlidingLayout?) {
        mSlidingLayout = slidingLayout
        mScroller = Scroller(slidingLayout.getContext())
        screenWidth = slidingLayout.getContext().getResources().getDisplayMetrics().widthPixels
        limitDistance = screenWidth / 3
    }

    @Override
    fun resetFromAdapter(adapter: SlidingAdapter?) {
        mSlidingLayout.addView(getAdapter().getCurrentView())
        if (getAdapter().hasNext()) {
            val nextView: View = getAdapter().getNextView()
            mSlidingLayout.addView(nextView, 0)
            nextView.scrollTo(0, 0)
        }
        if (getAdapter().hasPrevious()) {
            val prevView: View = getAdapter().getPreviousView()
            mSlidingLayout.addView(prevView)
            prevView.scrollTo(screenWidth, 0)
        }
        mSlidingLayout.slideSelected(getAdapter().getCurrent())
    }

    fun getTopView(): View? {
        return getAdapter().getPreviousView()
    }

    fun getCurrentShowView(): View? {
        return getAdapter().getCurrentView()
    }

    @Override
    fun onTouchEvent(event: MotionEvent?): Boolean {
        obtainVelocityTracker(event)
        when (event.getAction()) {
            MotionEvent.ACTION_DOWN -> {
                if (!mScroller.isFinished()) {
                    break
                }
                startX = event.getX() as Int
            }

            MotionEvent.ACTION_MOVE -> {
                if (!mScroller.isFinished()) {
                    return false
                }
                if (startX == 0) {
                    startX = event.getX() as Int
                }
                val distance = startX - event.getX() as Int
                if (mDirection == MOVE_NO_RESULT) {
                    if (getAdapter().hasNext() && distance > 0) {
                        mDirection = MOVE_TO_LEFT
                    } else if (getAdapter().hasPrevious() && distance < 0) {
                        mDirection = MOVE_TO_RIGHT
                    }
                }
                if (mMode == MODE_NONE
                        && (mDirection == MOVE_TO_LEFT && getAdapter().hasNext() || mDirection == MOVE_TO_RIGHT && getAdapter().hasPrevious())) {
                    mMode = MODE_MOVE
                }
                if (mMode == MODE_MOVE) {
                    if (mDirection == MOVE_TO_LEFT && distance <= 0 || mDirection == MOVE_TO_RIGHT && distance >= 0) {
                        mMode = MODE_NONE
                    }
                }
                if (mDirection != MOVE_NO_RESULT) {
                    mScrollerView = if (mDirection == MOVE_TO_LEFT) {
                        getCurrentShowView()
                    } else {
                        getTopView()
                    }
                    if (mMode == MODE_MOVE) {
                        mVelocityTracker.computeCurrentVelocity(1000, ViewConfiguration.getMaximumFlingVelocity())
                        if (mDirection == MOVE_TO_LEFT) {
                            mScrollerView.scrollTo(distance, 0)
                        } else {
                            mScrollerView.scrollTo(screenWidth + distance, 0)
                        }
                    } else {
                        val scrollX: Int = mScrollerView.getScrollX()
                        if (mDirection == MOVE_TO_LEFT && scrollX != 0 && getAdapter().hasNext()) {
                            mScrollerView.scrollTo(0, 0)
                        } else if (mDirection == MOVE_TO_RIGHT && getAdapter().hasPrevious() && screenWidth != Math.abs(scrollX)) {
                            mScrollerView.scrollTo(screenWidth, 0)
                        }
                    }
                }
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                if (mScrollerView == null) {
                    return false
                }
                val scrollX: Int = mScrollerView.getScrollX()
                mVelocityValue = mVelocityTracker.getXVelocity() as Int
                // scroll左正，右负(),(startX + dx)的值如果为0，即复位
                /*
			 * android.widget.Scroller.startScroll( int startX, int startY, int
			 * dx, int dy, int duration )
			 */
                var time = 500
                if (mMode == MODE_MOVE && mDirection == MOVE_TO_LEFT) {
                    if (scrollX > limitDistance || mVelocityValue < -time) {
                        // 手指向左移动，可以翻屏幕
                        mTouchResult = MOVE_TO_LEFT
                        if (mVelocityValue < -time) {
                            time = 200
                        }
                        mScroller.startScroll(scrollX, 0, screenWidth - scrollX, 0, time)
                    } else {
                        mTouchResult = MOVE_NO_RESULT
                        mScroller.startScroll(scrollX, 0, -scrollX, 0, time)
                    }
                } else if (mMode == MODE_MOVE && mDirection == MOVE_TO_RIGHT) {
                    if (screenWidth - scrollX > limitDistance || mVelocityValue > time) {
                        // 手指向右移动，可以翻屏幕
                        mTouchResult = MOVE_TO_RIGHT
                        if (mVelocityValue > time) {
                            time = 250
                        }
                        mScroller.startScroll(scrollX, 0, -scrollX, 0, time)
                    } else {
                        mTouchResult = MOVE_NO_RESULT
                        mScroller.startScroll(scrollX, 0, screenWidth - scrollX, 0, time)
                    }
                }
                resetVariables()
                invalidate()
            }
        }
        return true
    }

    private fun resetVariables() {
        mDirection = MOVE_NO_RESULT
        mMode = MODE_NONE
        startX = 0
        releaseVelocityTracker()
    }

    fun moveToNext(): Boolean {
        if (!getAdapter().hasNext()) return false

        // Move top view to bottom view
        val prevView: View = getAdapter().getPreviousView()
        if (prevView != null) mSlidingLayout.removeView(prevView)
        var newNextView: View? = prevView
        getAdapter().moveToNext()
        if (getAdapter().hasNext()) {
            // Update content in the old view
            if (newNextView != null) {
                val updateNextView: View = getAdapter().getView(newNextView, getAdapter().getNext())
                if (updateNextView !== newNextView) {
                    getAdapter().setNextView(updateNextView)
                    newNextView = updateNextView
                }
            } else {
                newNextView = getAdapter().getNextView()
            }
            mSlidingLayout.addView(newNextView, 0)
            newNextView.scrollTo(0, 0)
        }
        return true
    }

    fun moveToPrevious(): Boolean {
        if (!getAdapter().hasPrevious()) return false

        // Move top view to bottom view
        val nextView: View = getAdapter().getNextView()
        if (nextView != null) mSlidingLayout.removeView(nextView)
        var newPrevView: View? = nextView
        getAdapter().moveToPrevious()
        mSlidingLayout.slideSelected(getAdapter().getCurrent())
        if (getAdapter().hasPrevious()) {
            // Reuse the previous view as the next view
            // Update content in the old view
            if (newPrevView != null) {
                val updatedPrevView: View = getAdapter().getView(newPrevView, getAdapter().getPrevious())
                if (newPrevView !== updatedPrevView) {
                    getAdapter().setPreviousView(updatedPrevView)
                    newPrevView = updatedPrevView
                }
            } else {
                newPrevView = getAdapter().getPreviousView()
            }
            mSlidingLayout.addView(newPrevView)
            newPrevView.scrollTo(screenWidth, 0)
        }
        return true
    }

    @Override
    fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            mScrollerView.scrollTo(mScroller.getCurrX(), mScroller.getCurrY())
            invalidate()
        } else if (mScroller.isFinished() && mTouchResult != MOVE_NO_RESULT) {
            if (mTouchResult == MOVE_TO_LEFT) {
                moveToNext()
            } else {
                moveToPrevious()
            }
            mTouchResult = MOVE_NO_RESULT
            invalidate()
        }
    }

    private fun invalidate() {
        mSlidingLayout.postInvalidate()
    }

    private fun obtainVelocityTracker(event: MotionEvent?) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
        mVelocityTracker.addMovement(event)
    }

    private fun releaseVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle()
            mVelocityTracker = null
        }
    }

    @Override
    fun slideNext() {
        if (!getAdapter().hasNext() || !mScroller.isFinished()) return
        mScrollerView = getCurrentShowView()
        mScroller.startScroll(0, 0, screenWidth, 0, 500)
        mTouchResult = MOVE_TO_LEFT
        mSlidingLayout.slideScrollStateChanged(MOVE_TO_LEFT)
        invalidate()
    }

    @Override
    fun slidePrevious() {
        if (!getAdapter().hasPrevious() || !mScroller.isFinished()) return
        mScrollerView = getTopView()
        mScroller.startScroll(screenWidth, 0, -screenWidth, 0, 500)
        mTouchResult = MOVE_TO_RIGHT
        mSlidingLayout.slideScrollStateChanged(MOVE_TO_RIGHT)
        invalidate()
    }
}