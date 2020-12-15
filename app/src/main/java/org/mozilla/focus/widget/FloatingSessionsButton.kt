/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.mozilla.focus.R

class FloatingSessionsButton : FloatingActionButton {

    private var textPaint: TextPaint? = null
    private var tabCount: Int = 0

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        val paint = Paint()
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val textSize = resources.getDimensionPixelSize(R.dimen.tabs_button_text_size)

        textPaint = TextPaint(paint)
        textPaint!!.textAlign = Paint.Align.CENTER
        textPaint!!.textSize = textSize.toFloat()

        setImageResource(R.drawable.tab_number_border)
    }

    fun updateSessionsCount(tabCount: Int) {
        this.tabCount = tabCount

        contentDescription = resources.getString(R.string.content_description_tab_counter, tabCount.toString())

        val params = layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior as FloatingActionButtonBehavior?

        val shouldBeVisible = tabCount >= 2

        behavior?.setEnabled(shouldBeVisible)

        if (shouldBeVisible) {
            show()
            invalidate()
        } else {
            hide()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val x = canvas.width / 2f
        val y = canvas.height / 2f - (textPaint!!.descent() + textPaint!!.ascent()) / 2f

        val text = if (tabCount < TOO_MANY_TABS) tabCount.toString() else TOO_MANY_TABS_SYMBOL

        canvas.drawText(text, x, y, textPaint!!)
    }

    companion object {
        /**
         * The Answer to the Ultimate Question of Life, the Universe, and Everything. And the number of
         * tabs that is just too many.
         */
        private val TOO_MANY_TABS = 42
        private val TOO_MANY_TABS_SYMBOL = ":("
    }
}
