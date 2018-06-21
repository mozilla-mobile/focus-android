/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.distribution

import android.content.Context
import android.preference.PreferenceManager
import org.mozilla.focus.R

sealed class Device {
    class KEY2 : Device() {
        override fun onApplicationLaunch(context: Context) {
            // Disable Telemetry on Launch
            val resources = context.resources
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)

            preferences.edit()
                    .putBoolean(resources.getString(R.string.pref_key_telemetry), false)
                    .apply()
        }

        companion object {
            val BRAND = "blackberry"
            val DEVICE = "bbf100"
        }
    }

    abstract fun onApplicationLaunch(context: Context)

    companion object {
        fun create(brand: String, device: String): Device? = when {
            (brand == KEY2.BRAND && device == KEY2.DEVICE) -> KEY2()
            else -> null
        }
    }
}
