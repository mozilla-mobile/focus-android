/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.content.Context
import android.support.v7.preference.ListPreference
import android.support.v7.preference.PreferenceViewHolder
import android.util.AttributeSet
import org.mozilla.focus.R
import org.mozilla.focus.utils.Settings

/**
 * Cookies preference that will show a list to configure blocking Cookies options
 */
class CookiesPreference(context: Context?, attrs: AttributeSet?) : ListPreference(context, attrs) {

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        updateSummary()
    }

    override fun notifyChanged() {
        super.notifyChanged()
        updateSummary()
    }

    fun updateSummary() {
        val settings = Settings.getInstance(context)
        val value = when (settings.getCookiesPrefValue()) {
            context.getString(R.string.pref_key_should_block_cookies_no) -> {
                context.resources.getString(R.string.preference_privacy_should_block_cookies_no_option)
            }
            context.resources.getString(R.string.pref_key_should_block_cookies_third_party_only) -> {
                context.getString(R.string.preference_privacy_should_block_cookies_third_party_only_option)
            }
            context.resources.getString(R.string.pref_key_should_block_cookies_third_party_trackers_only) -> {
                context.getString(
                    R.string.preference_privacy_should_block_cookies_third_party_tracker_cookies_option
                )
            }
            else -> context.getString(R.string.preference_privacy_should_block_cookies_yes_option)
        }
        super.setSummary(value)
    }
}
