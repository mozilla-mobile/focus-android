/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.biometrics

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mozilla.components.browser.state.selector.privateTabs
import mozilla.components.browser.state.store.BrowserStore
import org.mozilla.focus.GleanMetrics.TabCount
import org.mozilla.focus.ext.components
import org.mozilla.focus.state.AppAction
import org.mozilla.focus.state.AppStore
import org.mozilla.focus.topsites.DefaultTopSitesStorage

class LockObserver(
    private val context: Context,
    private val browserStore: BrowserStore,
    private val appStore: AppStore
) : DefaultLifecycleObserver {

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        triggerAppLock()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        triggerAppLock()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun triggerAppLock() {
        GlobalScope.launch(Dispatchers.IO) {
            val tabCount = browserStore.state.privateTabs.size.toLong()
            TabCount.appBackgrounded.accumulateSamples(listOf(tabCount))
            val topSitesList = context.components.topSitesStorage.getTopSites(
                totalSites = DefaultTopSitesStorage.TOP_SITES_MAX_LIMIT,
                frecencyConfig = null
            )
            if (tabCount == 0L && topSitesList.isNullOrEmpty()) {
                return@launch
            }
            if (Biometrics.isBiometricsEnabled(context)) {
                appStore.dispatch(AppAction.Lock)
            }
        }
    }
}
