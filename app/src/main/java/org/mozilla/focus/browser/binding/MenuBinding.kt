package org.mozilla.focus.browser.binding

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import mozilla.components.browser.state.selector.findTabOrCustomTabOrSelectedTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifAnyChanged
import org.mozilla.focus.menu.browser.BrowserMenu

class MenuBinding(
    store: BrowserStore,
    private val tabId: String?,
    private val menuGetter: () -> BrowserMenu?
) : AbstractBinding(store) {
    override suspend fun onState(flow: Flow<BrowserState>) {
        flow.mapNotNull { state -> state.findTabOrCustomTabOrSelectedTab(tabId) }
            .ifAnyChanged { tab -> arrayOf(tab.trackingProtection.blockedTrackers, tab.content.loading) }
            .collect { tab -> onStateChanged(tab) }
    }

    private fun onStateChanged(tab: SessionState) {
        val menu = menuGetter.invoke() ?: return

        menu.updateTrackers(tab.trackingProtection.blockedTrackers.size)
        menu.updateLoading(tab.content.loading)
    }
}