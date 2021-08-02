/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser.binding

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import mozilla.components.browser.state.selector.findTabOrCustomTabOrSelectedTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.lib.state.helpers.AbstractBinding
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged

/**
 * Binding responsible for updating the total number of tracked blockers since install.
 */
class BlockedTrackersBinding(
    store: BrowserStore,
    private val tabId: String,
    private val updateCount: (Int) -> Unit
) : AbstractBinding<BrowserState>(store) {

    private var currentCount: Int = 0

    override suspend fun onState(flow: Flow<BrowserState>) {
        flow.mapNotNull { state -> state.findTabOrCustomTabOrSelectedTab(tabId) }
            .ifChanged { tab ->
                tab.trackingProtection.blockedTrackers
            }
            .collect { tab ->
                val newCount = tab.trackingProtection.blockedTrackers.size
                // If the new value is lower than the previous stored one that means
                // the list has updated and we can count new trackers.
                if (newCount > currentCount) {
                    updateCount.invoke(newCount - currentCount)
                } else {
                    updateCount.invoke(newCount)
                }
                currentCount = newCount
            }
    }
}
