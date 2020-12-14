package org.mozilla.focus.widget

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator

class ShiftDrawable @JvmOverloads constructor(d: Drawable,
                                              duration: Int = DEFAULT_DURATION,
                                              interpolator: Interpolator? = LinearInterpolator()
) : DrawableWrapper(d) {

    private val mAnimator = ValueAnimator.ofFloat(0f, 1f)
    private val mVisibleRect = Rect()
    private var mPath: Path? = null


    init {
        mAnimator.duration = duration.toLong()
        mAnimator.repeatCount = ValueAnimator.INFINITE
        mAnimator.interpolator = interpolator ?: LinearInterpolator()
        mAnimator.addUpdateListener {
            if (isVisible) {
                invalidateSelf()
            }
        }
        mAnimator.start()
    }

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        val result = super.setVisible(visible, restart)
        if (isVisible) {
            mAnimator.start()
        } else {
            mAnimator.end()
        }
        return result
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        updateBounds()
    }

    override fun onLevelChange(level: Int): Boolean {
        val result = super.onLevelChange(level)
        updateBounds()
        return result
    }

    override fun draw(canvas: Canvas) {
        val d = wrappedDrawable
        val fraction = mAnimator.animatedFraction
        val width = mVisibleRect.width()
        val offset = (width * fraction).toInt()
        val stack = canvas.save()

        canvas.clipPath(mPath!!)

        // shift from right to left.
        // draw left-half part
        canvas.save()
        canvas.translate((-offset).toFloat(), 0f)
        d.draw(canvas)
        canvas.restore()

        // draw right-half part
        canvas.save()
        canvas.translate((width - offset).toFloat(), 0f)
        d.draw(canvas)
        canvas.restore()

        canvas.restoreToCount(stack)
    }

    private fun updateBounds() {
        val b = bounds
        val width = (b.width().toFloat() * level / MAX_LEVEL).toInt()
        val radius = b.height() / 2f
        mVisibleRect.set(b.left, b.top, b.left + width, b.height())

        // draw round to head of progressbar. I know it looks stupid, don't blame me now.
        mPath = Path()
        mPath!!.addRect(b.left.toFloat(), b.top.toFloat(),
                b.left + width - radius, b.height().toFloat(), Path.Direction.CCW)
        mPath!!.addCircle(b.left + width - radius, radius, radius, Path.Direction.CCW)
    }

    companion object {

        // align to ScaleDrawable implementation
        private val MAX_LEVEL = 10000

        private val DEFAULT_DURATION = 1000
    }
}
