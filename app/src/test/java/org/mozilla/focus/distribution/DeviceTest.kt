/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.distribution

import android.preference.PreferenceManager
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Before
import org.mozilla.focus.R
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DeviceTest {
    @Before
    fun setUp() {
        // Reset all saved and cached values before running a test
        PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .edit()
                .clear()
                .apply()
    }

    @Test
    fun testKEY2WillDisableTelemetry() {
        val brand = "blackberry"
        val device = "bbf100"
        Device.create(brand, device)
                ?.onApplicationLaunch(RuntimeEnvironment.application)

        val resources = RuntimeEnvironment.application.resources
        val context = RuntimeEnvironment.application
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)

        val isTelemetryEnabled = preferences.getBoolean(resources.getString(R.string.pref_key_telemetry), true)
        Assert.assertFalse(isTelemetryEnabled)
    }
}