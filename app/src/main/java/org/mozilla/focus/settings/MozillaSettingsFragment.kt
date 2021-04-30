/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import mozilla.components.browser.state.state.SessionState
import org.mozilla.focus.R
import org.mozilla.focus.browser.LocalizedContent
import org.mozilla.focus.ext.components
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.AppConstants
import org.mozilla.focus.utils.SupportUtils

class MozillaSettingsFragment : BaseSettingsFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onCreatePreferences(p0: Bundle?, p1: String?) {
        addPreferencesFromResource(R.xml.mozilla_settings)
    }

    override fun onResume() {
        super.onResume()

        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        updateTitle(R.string.preference_category_mozilla)
    }

    override fun onPause() {
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        // AppCompatActivity has a Toolbar that is used as the ActionBar, and it conflicts with the ActionBar
        // used by PreferenceScreen to create the headers (with title, back navigation), so we wrap all these
        // "preference screens" into separate activities.
        val activity = activity ?: return super.onPreferenceTreeClick(preference)

        when (preference.key) {
            resources.getString(R.string.pref_key_about) -> run {
                activity.components.tabsUseCases.addPrivateTab(
                    LocalizedContent.URL_ABOUT,
                    source = SessionState.Source.MENU,
                    selectTab = true
                )
                activity.finish()
            }
            resources.getString(R.string.pref_key_help) -> run {
                activity.components.tabsUseCases.addPrivateTab(
                    SupportUtils.HELP_URL,
                    source = SessionState.Source.MENU,
                    selectTab = true
                )
                activity.finish()
            }
            resources.getString(R.string.pref_key_rights) -> run {
                activity.components.tabsUseCases.addPrivateTab(
                    LocalizedContent.URL_RIGHTS,
                    source = SessionState.Source.MENU,
                    selectTab = true
                )
                activity.finish()
            }
            resources.getString(R.string.pref_key_privacy_notice) -> {
                val url = if (AppConstants.isKlarBuild)
                    SupportUtils.PRIVACY_NOTICE_KLAR_URL
                else
                    SupportUtils.PRIVACY_NOTICE_URL

                activity.components.tabsUseCases.addPrivateTab(
                    url,
                    source = SessionState.Source.MENU,
                    selectTab = true
                )
                activity.finish()
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        TelemetryWrapper.settingsEvent(key, sharedPreferences.all[key].toString())
    }

    companion object {
        fun newInstance(): MozillaSettingsFragment = MozillaSettingsFragment()
    }
}
