/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.view.animation.AnimationUtils
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.mozilla.focus.R

class FloatingEraseButton : FloatingActionButton {
    private var keepHidden: Boolean = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun updateSessionsCount(tabCount: Int) {
        val params = layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior as FloatingActionButtonBehavior?
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

        keepHidden = tabCount != 1

        if (behavior != null) {
            if (accessibilityManager.isTouchExplorationEnabled) {
                // Always display erase button if Talk Back is enabled
                behavior.setEnabled(false)
            } else {
                behavior.setEnabled(!keepHidden)
            }
        }

        if (keepHidden) {
            visibility = View.GONE
        }
    }

    override fun onFinishInflate() {
        if (!keepHidden) {
            this.visibility = View.VISIBLE
            this.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fab_reveal))
        }

        super.onFinishInflate()
    }

    override fun setVisibility(visibility: Int) {
        if (keepHidden && visibility == View.VISIBLE) {
            // There are multiple callbacks updating the visibility of the button. Let's make sure
            // we do not show the button if we do not want to.
            return
        }

        if (visibility == View.VISIBLE) {
            show()
        } else {
            hide()
        }
    }
}
