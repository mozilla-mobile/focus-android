/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.content.Context
import android.support.design.widget.AppBarLayout
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

import org.mozilla.focus.R
import org.mozilla.focus.widget.AnimatedProgressBar

/**
 * The toolbar of the BrowserFragment; displaying the URL and other controls.
 */
class DisplayToolbar : AppBarLayout, AppBarLayout.OnOffsetChangedListener {
    private lateinit var toolbarContent: View
    private lateinit var progressView: AnimatedProgressBar

    private val collapsedProgressTranslationY: Float by lazy {
        TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onFinishInflate() {
        super.onFinishInflate()

        toolbarContent = findViewById<View>(R.id.toolbar_content)
        progressView = findViewById<AnimatedProgressBar>(R.id.progress)

        addOnOffsetChangedListener(this)
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
        // When scrolling the toolbar away we want to fade out the content on the toolbar
        // with an alpha animation. This will avoid that the text clashes with the status bar.

        val totalScrollRange = appBarLayout.totalScrollRange
        val isCollapsed = Math.abs(verticalOffset) == totalScrollRange

        // If the toolbar is collapsed then we will move the progress view so that it's
        // still fully visible.
        progressView.translationY = if (isCollapsed) collapsedProgressTranslationY else 0f

        if (verticalOffset == 0 || isCollapsed) {
            // If the app bar is completely expanded or collapsed we want full opacity. We
            // even want full opacity for a collapsed app bar because while loading a website
            // the toolbar sometimes pops out when the URL changes. Without setting it to
            // opaque the toolbar content might be invisible in this case (See issue #1126)
            toolbarContent.alpha = 1f
            return
        }

        // The toolbar content should have 100% alpha when the AppBarLayout is expanded and
        // should have 0 alpha when the toolbar is collapsed 50% or more.
        var alpha = -1 * (100f / (totalScrollRange * 0.5f) * verticalOffset / 100)

        // We never want to go lower than 0 or higher than 1.
        alpha = Math.max(0f, alpha)
        alpha = Math.min(1f, alpha)

        // The calculated value is reversed and we need to invert it (e.g. 0.8 -> 0.2)
        alpha = 1 - alpha

        toolbarContent.alpha = alpha
    }
}
