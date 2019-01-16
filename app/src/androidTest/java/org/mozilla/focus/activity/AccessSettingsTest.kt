/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiSelector

import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.helpers.TestHelper

import org.mozilla.focus.fragment.FirstrunFragment.Companion.FIRSTRUN_PREF
import org.mozilla.focus.helpers.EspressoHelper.openSettings
import org.mozilla.focus.helpers.TestHelper.waitingTime

// This test checks all the headings in the Settings menu are there
// https://testrail.stage.mozaws.net/index.php?/cases/view/40064
@RunWith(AndroidJUnit4::class)
class AccessSettingsTest {

    @Rule
    var mActivityTestRule: ActivityTestRule<MainActivity> = object : ActivityTestRule<MainActivity>(MainActivity::class.java) {

        override fun beforeActivityLaunched() {
            super.beforeActivityLaunched()

            val appContext = InstrumentationRegistry.getInstrumentation()
                .targetContext
                .applicationContext

            PreferenceManager.getDefaultSharedPreferences(appContext)
                .edit()
                .putBoolean(FIRSTRUN_PREF, true)
                .apply()
            // This test runs on both GV and WV.
            // Klar is used to test Geckoview. make sure it's set to Gecko
            TestHelper.selectGeckoForKlar()
        }
    }

    @After
    fun tearDown() {
        mActivityTestRule.activity.finishAndRemoveTask()
    }

    @Test
    fun accessSettingsTest() {

        val generalHeading = TestHelper.mDevice.findObject(UiSelector()
            .text("General")
            .resourceId("android:id/title"))

        val privacyHeading = TestHelper.mDevice.findObject(UiSelector()
            .text("Privacy & Security")
            .resourceId("android:id/title"))

        val searchHeading = TestHelper.mDevice.findObject(UiSelector()
            .text("Search")
            .resourceId("android:id/title"))

        val mozHeading = TestHelper.mDevice.findObject(UiSelector()
            .text("Mozilla")
            .resourceId("android:id/title"))

        /* Go to Settings */
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime)
        openSettings()
        generalHeading.waitForExists(waitingTime)

        /* Check the first element and other headings are present */
        assertTrue(generalHeading.exists())
        assertTrue(searchHeading.exists())
        assertTrue(privacyHeading.exists())
        assertTrue(mozHeading.exists())
    }
}
