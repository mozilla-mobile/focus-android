/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import com.google.android.material.appbar.AppBarLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.util.AttributeSet
import android.view.View

/**
 * A Behavior implementation that will hide/show a FloatingActionButton based on whether an AppBarLayout
 * is visible or not.
 */
// This behavior is set from xml (fragment_browser.xml)
class FloatingActionButtonBehavior(context: Context, attrs: AttributeSet) : CoordinatorLayout.Behavior<FloatingActionButton>(), AppBarLayout.OnOffsetChangedListener {

    private var layout: AppBarLayout? = null
    private var button: FloatingActionButton? = null
    private var visible: Boolean = false
    private var enabled: Boolean = false

    init {

        enabled = true
        visible = false
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: FloatingActionButton, dependency: View): Boolean {
        if (button !== child) {
            button = child
        }

        if (dependency is AppBarLayout && layout !== dependency) {
            layout = dependency
            layout!!.addOnOffsetChangedListener(this)

            return true
        }

        return super.layoutDependsOn(parent, child, dependency)
    }

    override fun onDependentViewRemoved(parent: CoordinatorLayout, child: FloatingActionButton, dependency: View) {
        super.onDependentViewRemoved(parent, child, dependency)

        layout!!.removeOnOffsetChangedListener(this)
        layout = null
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
        if (!enabled) {
            return
        }

        if (verticalOffset == 0 && !visible) {
            showButton()
        } else if (Math.abs(verticalOffset) >= appBarLayout.totalScrollRange && visible) {
            hideButton()
        }
    }

    private fun showButton() {
        animate(button!!, false)
    }

    private fun hideButton() {
        animate(button!!, true)
    }

    private fun animate(child: View, hide: Boolean) {
        child.animate()
                .scaleX((if (hide) 0 else 1).toFloat())
                .scaleY((if (hide) 0 else 1).toFloat())
                .setDuration(ANIMATION_DURATION.toLong())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        if (!hide) {
                            // Ensure the child will be visible before starting animation: if it's hidden, we've also
                            // set it to View.GONE, so we need to restore that now, _before_ the animation starts.
                            child.visibility = View.VISIBLE
                        }
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        visible = !hide

                        // Hide the FAB: even when it has size=0x0, it still intercept click events,
                        // so we get phantom clicks causing focus to erase if the user presses
                        // near where the FAB would usually be shown.
                        if (hide) {
                            child.visibility = View.GONE
                        }
                    }
                })
                .start()
    }

    companion object {
        private val ANIMATION_DURATION = 300
    }
}
