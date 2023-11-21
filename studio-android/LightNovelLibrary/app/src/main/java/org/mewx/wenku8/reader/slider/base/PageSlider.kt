package org.mewx.wenku8.reader.slider.base

import android.view.MotionEvent

/**
 * Created by xuzb on 1/16/15.
 */
class PageSlider : BaseSlider() {
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
    private var mMoveLastPage = false
    private var mMoveFirstPage = false
    private var startX = 0

    /** 滑动的view  */
    private var mLeftScrollerView: View? = null
    private var mRightScrollerView: View? = null
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
        val curView: View = getAdapter().getUpdatedCurrentView()
        mSlidingLayout.addView(curView)
        curView.scrollTo(0, 0)
        if (getAdapter().hasPrevious()) {
            val prevView: View = getAdapter().getUpdatedPreviousView()
            mSlidingLayout.addView(prevView)
            prevView.scrollTo(screenWidth, 0)
        }
        if (getAdapter().hasNext()) {
            val nextView: View = getAdapter().getUpdatedNextView()
            mSlidingLayout.addView(nextView)
            nextView.scrollTo(-screenWidth, 0)
        }
        mSlidingLayout.slideSelected(getAdapter().getCurrent())
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
                    if (distance > 0) {
                        mDirection = MOVE_TO_LEFT
                        mMoveLastPage = !getAdapter().hasNext()
                        mMoveFirstPage = false
                        mSlidingLayout.slideScrollStateChanged(MOVE_TO_LEFT)
                    } else if (distance < 0) {
                        mDirection = MOVE_TO_RIGHT
                        mMoveFirstPage = !getAdapter().hasPrevious()
                        mMoveLastPage = false
                        mSlidingLayout.slideScrollStateChanged(MOVE_TO_RIGHT)
                    }
                }
                if (mMode == MODE_NONE
                        && (mDirection == MOVE_TO_LEFT || mDirection == MOVE_TO_RIGHT)) {
                    mMode = MODE_MOVE
                }
                if (mMode == MODE_MOVE) {
                    if (mDirection == MOVE_TO_LEFT && distance <= 0 || mDirection == MOVE_TO_RIGHT && distance >= 0) {
                        mMode = MODE_NONE
                    }
                }
                if (mDirection != MOVE_NO_RESULT) {
                    if (mDirection == MOVE_TO_LEFT) {
                        mLeftScrollerView = getCurrentShowView()
                        mRightScrollerView = if (!mMoveLastPage) getBottomView() else null
                    } else {
                        mRightScrollerView = getCurrentShowView()
                        mLeftScrollerView = if (!mMoveFirstPage) getTopView() else null
                    }
                    if (mMode == MODE_MOVE) {
                        mVelocityTracker.computeCurrentVelocity(1000, ViewConfiguration.getMaximumFlingVelocity())
                        if (mDirection == MOVE_TO_LEFT) {
                            if (mMoveLastPage) {
                                mLeftScrollerView.scrollTo(distance / 2, 0)
                            } else {
                                mLeftScrollerView.scrollTo(distance, 0)
                                mRightScrollerView.scrollTo(-screenWidth + distance, 0)
                            }
                        } else {
                            if (mMoveFirstPage) {
                                mRightScrollerView.scrollTo(distance / 2, 0)
                            } else {
                                mLeftScrollerView.scrollTo(screenWidth + distance, 0)
                                mRightScrollerView.scrollTo(distance, 0)
                            }
                        }
                    } else {
                        var scrollX = 0
                        if (mLeftScrollerView != null) {
                            scrollX = mLeftScrollerView.getScrollX()
                        } else if (mRightScrollerView != null) {
                            scrollX = mRightScrollerView.getScrollX()
                        }
                        if (mDirection == MOVE_TO_LEFT && scrollX != 0 && getAdapter().hasNext()) {
                            mLeftScrollerView.scrollTo(0, 0)
                            if (mRightScrollerView != null) mRightScrollerView.scrollTo(screenWidth, 0)
                        } else if (mDirection == MOVE_TO_RIGHT && getAdapter().hasPrevious() && screenWidth != Math.abs(scrollX)) {
                            if (mLeftScrollerView != null) mLeftScrollerView.scrollTo(-screenWidth, 0)
                            mRightScrollerView.scrollTo(0, 0)
                        }
                    }
                }
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                if (mLeftScrollerView == null && mDirection == MOVE_TO_LEFT || mRightScrollerView == null && mDirection == MOVE_TO_RIGHT) {
                    return false
                }
                var time = 500
                if (mMoveFirstPage && mRightScrollerView != null) {
                    val rscrollx: Int = mRightScrollerView.getScrollX()
                    mScroller.startScroll(rscrollx, 0, -rscrollx, 0, time * Math.abs(rscrollx) / screenWidth)
                    mTouchResult = MOVE_NO_RESULT
                }
                if (mMoveLastPage && mLeftScrollerView != null) {
                    val lscrollx: Int = mLeftScrollerView.getScrollX()
                    mScroller.startScroll(lscrollx, 0, -lscrollx, 0, time * Math.abs(lscrollx) / screenWidth)
                    mTouchResult = MOVE_NO_RESULT
                }
                if (!mMoveLastPage && !mMoveFirstPage && mLeftScrollerView != null) {
                    val scrollX: Int = mLeftScrollerView.getScrollX()
                    mVelocityValue = mVelocityTracker.getXVelocity() as Int
                    // scroll左正，右负(),(startX + dx)的值如果为0，即复位
                    /*
			 * android.widget.Scroller.startScroll( int startX, int startY, int
			 * dx, int dy, int duration )
			 */if (mMode == MODE_MOVE && mDirection == MOVE_TO_LEFT) {
                        if (scrollX > limitDistance || mVelocityValue < -time) {
                            // 手指向左移动，可以翻屏幕
                            mTouchResult = MOVE_TO_LEFT
                            if (mVelocityValue < -time) {
                                val tmptime: Int = 1000 * 1000 / Math.abs(mVelocityValue)
                                time = if (tmptime > 500) 500 else tmptime
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
                                val tmptime: Int = 1000 * 1000 / Math.abs(mVelocityValue)
                                time = if (tmptime > 500) 500 else tmptime
                            }
                            mScroller.startScroll(scrollX, 0, -scrollX, 0, time)
                        } else {
                            mTouchResult = MOVE_NO_RESULT
                            mScroller.startScroll(scrollX, 0, screenWidth - scrollX, 0, time)
                        }
                    }
                }
                resetVariables()
                invalidate()
            }
        }
        return true
    }

    private fun invalidate() {
        mSlidingLayout.postInvalidate()
    }

    private fun resetVariables() {
        mDirection = MOVE_NO_RESULT
        mMode = MODE_NONE
        startX = 0
        releaseVelocityTracker()
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

    private fun moveToNext(): Boolean {
        if (!getAdapter().hasNext()) return false

        // Move top view to bottom view
        val prevView: View = getAdapter().getPreviousView()
        if (prevView != null) mSlidingLayout.removeView(prevView)
        var newNextView: View? = prevView
        getAdapter().moveToNext()
        mSlidingLayout.slideSelected(getAdapter().getCurrent())
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
            newNextView.scrollTo(-screenWidth, 0)
            mSlidingLayout.addView(newNextView)
        }
        return true
    }

    private fun moveToPrevious(): Boolean {
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
            newPrevView.scrollTo(screenWidth, 0)
            mSlidingLayout.addView(newPrevView)
        }
        return true
    }

    @Override
    fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            if (mLeftScrollerView != null) {
                mLeftScrollerView.scrollTo(mScroller.getCurrX(), mScroller.getCurrY())
            }
            if (mRightScrollerView != null) {
                if (mMoveFirstPage) mRightScrollerView.scrollTo(mScroller.getCurrX(), mScroller.getCurrY()) else mRightScrollerView.scrollTo(mScroller.getCurrX() - screenWidth, mScroller.getCurrY())
            }
            invalidate()
        } else if (mScroller.isFinished()) {
            if (mTouchResult != MOVE_NO_RESULT) {
                if (mTouchResult == MOVE_TO_LEFT) {
                    moveToNext()
                } else {
                    moveToPrevious()
                }
                mTouchResult = MOVE_NO_RESULT
                mSlidingLayout.slideScrollStateChanged(MOVE_NO_RESULT)
                invalidate()
            }
        }
    }

    @Override
    fun slideNext() {
        if (!getAdapter().hasNext() || !mScroller.isFinished()) return
        mLeftScrollerView = getCurrentShowView()
        mRightScrollerView = getBottomView()
        mScroller.startScroll(0, 0, screenWidth, 0, 500)
        mTouchResult = MOVE_TO_LEFT
        mSlidingLayout.slideScrollStateChanged(MOVE_TO_LEFT)
        invalidate()
    }

    @Override
    fun slidePrevious() {
        if (!getAdapter().hasPrevious() || !mScroller.isFinished()) return
        mLeftScrollerView = getTopView()
        mRightScrollerView = getCurrentShowView()
        mScroller.startScroll(screenWidth, 0, -screenWidth, 0, 500)
        mTouchResult = MOVE_TO_RIGHT
        mSlidingLayout.slideScrollStateChanged(MOVE_TO_RIGHT)
        invalidate()
    }

    fun getTopView(): View? {
        return getAdapter().getPreviousView()
    }

    fun getCurrentShowView(): View? {
        return getAdapter().getCurrentView()
    }

    fun getBottomView(): View? {
        return getAdapter().getNextView()
    }
}