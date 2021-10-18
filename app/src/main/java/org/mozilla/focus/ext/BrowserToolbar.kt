/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.content.Context
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.browser.toolbar.behavior.BrowserToolbarBehavior
import mozilla.components.browser.toolbar.behavior.ToolbarPosition
import mozilla.components.concept.engine.EngineView
import mozilla.components.feature.session.behavior.EngineViewBrowserToolbarBehavior
import org.mozilla.focus.R

/**
 * Collapse the toolbar and block it from appearing until calling [enableDynamicBehavior].
 * Useful in situations like entering fullscreen.
 *
 * @param engineView [EngineView] previously set to react to toolbar's dynamic behavior.
 * Will now go through a bit of cleanup to ensure everything will be displayed nicely even without a toolbar.
 */
fun BrowserToolbar.disableDynamicBehavior(engineView: EngineView) {
    (layoutParams as? CoordinatorLayout.LayoutParams)?.behavior = null

    engineView.setDynamicToolbarMaxHeight(0)
    engineView.asView().translationY = 0f
    (engineView.asView().layoutParams as? CoordinatorLayout.LayoutParams)?.behavior = null
}

/**
 * Expand the toolbar and reenable the dynamic behavior.
 * Useful after [disableDynamicBehavior] for situations like exiting fullscreen.
 *
 * @param context [Context] used in setting up the dynamic behavior.
 * @param engineView [EngineView] that should react to toolbar's dynamic behavior.
 */
fun BrowserToolbar.enableDynamicBehavior(context: Context, engineView: EngineView) {
    (layoutParams as? CoordinatorLayout.LayoutParams)?.behavior = BrowserToolbarBehavior(
        context, null, ToolbarPosition.TOP
    )

    engineView.setDynamicToolbarMaxHeight(height)
    (engineView.asView().layoutParams as? CoordinatorLayout.LayoutParams)?.apply {
        topMargin = 0
        behavior = EngineViewBrowserToolbarBehavior(
            context, null, engineView.asView(), context.resources.getDimension(R.dimen.browser_toolbar_height).toInt(),
            mozilla.components.feature.session.behavior.ToolbarPosition.TOP
        )
    }
}

/**
 * Show this toolbar at the top of the screen, fixed in place, with the EngineView immediately below it.
 *
 * @param context [Context] used for various system interactions
 * @param engineView [EngineView] that must be shown immediately below the toolbar.
 */
fun BrowserToolbar.showAsFixed(context: Context, engineView: EngineView) {
    visibility = View.VISIBLE

    engineView.setDynamicToolbarMaxHeight(0)

    val toolbarHeight = context.resources.getDimension(R.dimen.browser_toolbar_height).toInt()
    (engineView.asView().layoutParams as? CoordinatorLayout.LayoutParams)?.topMargin = toolbarHeight
}

/**
 * Remove this toolbar from the screen and allow the EngineView to occupy the entire screen.
 *
 * @param engineView [EngineView] that will be configured to occupy the entire screen.
 */
fun BrowserToolbar.hide(engineView: EngineView) {
    engineView.setDynamicToolbarMaxHeight(0)
    (engineView.asView().layoutParams as? CoordinatorLayout.LayoutParams)?.topMargin = 0

    visibility = View.GONE
}
