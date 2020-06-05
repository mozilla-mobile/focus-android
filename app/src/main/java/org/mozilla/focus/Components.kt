/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import mozilla.components.browser.search.SearchEngineManager
import mozilla.components.browser.search.provider.AssetsSearchEngineProvider
import mozilla.components.browser.search.provider.localization.LocaleSearchLocalizationProvider
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.DefaultSettings
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineSessionState
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.engine.utils.EngineVersion
import mozilla.components.feature.contextmenu.ContextMenuUseCases
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.session.SettingsUseCases
import mozilla.components.feature.session.TrackingProtectionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import org.json.JSONObject
import org.mozilla.focus.components.EngineProvider
import org.mozilla.focus.engine.LocalizedContentInterceptor
import org.mozilla.focus.ext.requireComponents
import org.mozilla.focus.search.BingSearchEngineFilter
import org.mozilla.focus.search.CustomSearchEngineProvider
import org.mozilla.focus.search.HiddenSearchEngineFilter
import org.mozilla.focus.utils.Settings

/**
 * Helper object for lazily initializing components.
 */
class Components(
    context: Context
) {
    val engineDefaultSettings by lazy {
        DefaultSettings(
            requestInterceptor = LocalizedContentInterceptor(context),
            trackingProtectionPolicy = Settings.getInstance(context).createTrackingProtectionPolicy()
        )
    }

    // TODO: Add actual settings
    val engine: Engine by lazy {
        EngineProvider.createEngine(context, engineDefaultSettings)
    }

    val trackingProtectionUseCases by lazy { TrackingProtectionUseCases(sessionManager, engine) }

    val settingsUseCases by lazy { SettingsUseCases(engine.settings, sessionManager) }

    val store: BrowserStore by lazy { BrowserStore() }

    val sessionUseCases: SessionUseCases by lazy { SessionUseCases(sessionManager) }

    val tabsUseCases: TabsUseCases by lazy { TabsUseCases(sessionManager) }

    val contextMenuUseCases: ContextMenuUseCases by lazy { ContextMenuUseCases(sessionManager, store) }

    val sessionManager by lazy {
        SessionManager(engine, store)
    }

    val searchEngineManager by lazy {
        val assetsProvider = AssetsSearchEngineProvider(
                LocaleSearchLocalizationProvider(),
                filters = listOf(BingSearchEngineFilter(), HiddenSearchEngineFilter()),
                additionalIdentifiers = listOf("ddg"))

        val customProvider = CustomSearchEngineProvider()

        SearchEngineManager(listOf(assetsProvider, customProvider))
    }
}
