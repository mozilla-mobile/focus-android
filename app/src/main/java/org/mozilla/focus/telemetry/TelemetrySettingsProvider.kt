/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.telemetry

import android.content.Context
import org.mozilla.focus.Components
import org.mozilla.focus.R
import org.mozilla.focus.search.CustomSearchEngineStore
import org.mozilla.focus.utils.Browsers
import org.mozilla.focus.utils.Settings
import org.mozilla.telemetry.TelemetryHolder
import org.mozilla.telemetry.measurement.SettingsMeasurement

/**
 * SharedPreferenceSettingsProvider implementation that additionally injects settings value for
 * runtime preferences like "default browser" and "search engine".
 */
internal class TelemetrySettingsProvider(
        private val context: Context
) : SettingsMeasurement.SharedPreferenceSettingsProvider() {
    private val prefKeyDefaultBrowser: String
    private val prefKeySearchEngine: String

    init {
        val resources = context.resources
        prefKeyDefaultBrowser = resources.getString(R.string.pref_key_default_browser)
        prefKeySearchEngine = resources.getString(R.string.pref_key_search_engine)
    }

    override fun containsKey(key: String): Boolean {
        if (key == prefKeyDefaultBrowser) {
            // Not actually a setting - but we want to report this like a setting.
            return true
        }

        return if (key == prefKeySearchEngine) {
            // We always want to report the current search engine - even if it's not in settings yet.
            true
        } else super.containsKey(key)
    }

    override fun getValue(key: String): Any {
        if (key == prefKeyDefaultBrowser) {
            // The default browser is not actually a setting. We determine if we are the
            // default and then inject this into telemetry.
            val context = TelemetryHolder.get().configuration.context
            val browsers = Browsers(context, Browsers.TRADITIONAL_BROWSER_URL)
            return java.lang.Boolean.toString(browsers.isDefaultBrowser(context))
        }

        if (key == prefKeySearchEngine) {
            var value: Any? = super.getValue(key)
            if (value == null) {
                // If the user has never selected a search engine then this value is null.
                // However we still want to report the current search engine of the user.
                // Therefore we inject this value at runtime.
                value = Components.searchEngineManager.getDefaultSearchEngine(
                        context,
                        Settings.getInstance(context).defaultSearchEngineName
                ).name
            } else if (CustomSearchEngineStore.isCustomSearchEngine((value as String?)!!, context)) {
                // Don't collect possibly sensitive info for custom search engines, send "custom" instead
                value = CustomSearchEngineStore.ENGINE_TYPE_CUSTOM
            }
            return value
        }

        return super.getValue(key)
    }
}
