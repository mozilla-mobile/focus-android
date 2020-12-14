/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

/**
 * A CoordinatorLayout implementation that resizes dynamically based on whether a keyboard is visible or not.
 */
class ResizableKeyboardLinearLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val delegate: ResizableKeyboardViewDelegate

    init {

        delegate = ResizableKeyboardViewDelegate(this, attrs!!)
    }

    public override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        delegate.onAttachedToWindow()
    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        delegate.onDetachedFromWindow()
    }

    fun reset() {
        delegate.reset()
    }
}
