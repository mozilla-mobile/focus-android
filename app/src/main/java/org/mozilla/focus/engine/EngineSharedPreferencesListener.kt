/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.engine

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.mozilla.focus.R
import org.mozilla.focus.ext.components
import org.mozilla.focus.utils.Settings

class EngineSharedPreferencesListener(
    private val context: Context
) : SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String) {
        Log.w("SKDBG", "onSharedPreferenceChanged() $key")

        when (key) {
            context.getString(R.string.pref_key_privacy_block_social),
            context.getString(R.string.pref_key_privacy_block_ads),
            context.getString(R.string.pref_key_privacy_block_analytics),
            context.getString(R.string.pref_key_privacy_block_other) ->
                updateTrackingProtectionPolicy()
        }
    }

    private fun updateTrackingProtectionPolicy() {
        Log.w("SKDBG", "updateTrackingProtectionPolicy() ${Thread.currentThread().name}")

        val policy = Settings.getInstance(context).createTrackingProtectionPolicy()
        val components = context.components

        components.engineDefaultSettings.trackingProtectionPolicy = policy
        components.settingsUseCases.updateTrackingProtection(policy)
    }
}
