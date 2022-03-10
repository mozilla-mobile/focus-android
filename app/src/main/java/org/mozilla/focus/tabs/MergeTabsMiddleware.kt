/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs

import android.content.Context
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.selector.privateTabs
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.concept.engine.EngineSession
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import org.mozilla.focus.ext.components
import org.mozilla.focus.ext.isMultiTabsEnabled

/**
 * If the tabs feature is disabled then this middleware will look at incoming [TabListAction.AddTabAction]
 * actions and, instead of creating a new tab, will merge the new tab with the existing tab to create
 * a single tab with a merged state.
 */
class MergeTabsMiddleware(
    private val context: Context
) : Middleware<BrowserState, BrowserAction> {
    override fun invoke(
        context: MiddlewareContext<BrowserState, BrowserAction>,
        next: (BrowserAction) -> Unit,
        action: BrowserAction
    ) {
        if (this.context.components.experiments.isMultiTabsEnabled || action !is TabListAction.AddTabAction) {
            // If the feature flag for tabs is enabled then we can just let the reducer create a
            // new tab.
            next(action)
            return
        }

        if (context.state.privateTabs.isEmpty()) {
            // If we do not have any tabs yet then we can let the reducer create one.
            next(action)
            return
        }

        val currentTab = context.state.privateTabs.first()
        val newTab = action.tab

        if (!shouldLoadInExistingTab(newTab.content.url)) {
            // This is a URL we do not want to load in an existing tab. Let's just bail.
            return
        }

        val mergedTab = mergeTabs(currentTab, newTab)

        // First we add the merged tab. The engine middleware will take care of linking the existing
        // engine session to this tab.
        next(TabListAction.AddTabAction(mergedTab, select = true))

        // If the new tab does not have an engine session we will reuse the engine session of
        // the current tab so we unlink it first to prevent the engine middleware from closing it.
        if (newTab.engineState.engineSession == null) {
            context.dispatch(EngineAction.UnlinkEngineSessionAction(currentTab.id))
        }

        // If the new tab has an engine session (e.g. when it was opened by a web extension) we will
        // use the new engine session, close the current tab with its engine session.
        context.dispatch(TabListAction.RemoveTabAction(currentTab.id))

        // Now we load the URL in the new tab.
        context.dispatch(
            EngineAction.LoadUrlAction(
                mergedTab.id,
                url = newTab.content.url,
                flags = EngineSession.LoadUrlFlags.select(
                    // To be safe we use the external flag here, since its not the user who decided to
                    // load this URL in this existing session.
                    EngineSession.LoadUrlFlags.EXTERNAL
                )
            )
        )
    }
}

private fun mergeTabs(
    currentTab: TabSessionState,
    newTab: TabSessionState
): TabSessionState {
    // In case a new tab is being opened by a web extension, the new tab will have its own new engine/gecko session,
    // which will have to be used
    val newEngineState = if (newTab.engineState.engineSession != null) {
        newTab.engineState
    } else {
        currentTab.engineState.copy(
            // We are clearing the engine observer, which would update the state of the tab with the
            // old ID. The engine middleware will create a new observer.
            engineObserver = null
        )
    }

    // We are giving the ID of the new tab. This will make
    // sure that code that created the tab can still access it with the ID.
    return currentTab.copy(
        newTab.id,
        engineState = newEngineState
    )
}

private fun shouldLoadInExistingTab(url: String): Boolean {
    val cleanedUrl = url.lowercase().trim()
    return cleanedUrl.startsWith("http:") ||
        cleanedUrl.startsWith("https:") ||
        cleanedUrl.startsWith("data:") ||
        cleanedUrl.startsWith("focus:")
}
