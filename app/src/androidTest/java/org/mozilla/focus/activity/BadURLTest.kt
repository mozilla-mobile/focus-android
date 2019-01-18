/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.filters.RequiresDevice
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiSelector

import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.R
import org.mozilla.focus.helpers.TestHelper
import org.mozilla.focus.utils.AppConstants

import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.espresso.web.sugar.Web.onWebView
import org.junit.Assert.assertTrue
import org.mozilla.focus.fragment.FirstrunFragment.Companion.FIRSTRUN_PREF
import org.mozilla.focus.helpers.TestHelper.waitingTime

// This test opens enters and invalid URL, and Focus should provide an appropriate error message
@RunWith(AndroidJUnit4::class)
@RequiresDevice
class BadURLTest {

    @Rule
    var mActivityTestRule: ActivityTestRule<MainActivity> = object : ActivityTestRule<MainActivity>(MainActivity::class.java) {

        override fun beforeActivityLaunched() {
            super.beforeActivityLaunched()

            val appContext = InstrumentationRegistry.getInstrumentation()
                .targetContext
                .applicationContext

            // This test is for webview only. Debug is defaulted to Webview, and Klar is used for GV testing.
            org.junit.Assume.assumeTrue(!AppConstants.isGeckoBuild && !AppConstants.isKlarBuild)

            PreferenceManager.getDefaultSharedPreferences(appContext)
                .edit()
                .putBoolean(FIRSTRUN_PREF, true)
                .apply()
        }
    }

    @After
    fun tearDown() {
        mActivityTestRule.activity.finishAndRemoveTask()
    }

    @Test
    fun badURLcheckTest() {
        provideInvalidURL()
        checkErrorMessage()
        provideMarketURL()
        waitForDialog()
    }

    private fun checkErrorMessage() {
        onWebView(withText(R.string.error_malformedURI_title))
        onWebView(withText(R.string.error_malformedURI_message))
        onWebView(withText("Try Again"))

        TestHelper.floatingEraseButton.perform(click())
    }

    private fun provideInvalidURL() {
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime)
        TestHelper.inlineAutocompleteEditText.clearTextField()
        TestHelper.inlineAutocompleteEditText.text = "htps://www.mozilla.org"
        TestHelper.hint.waitForExists(waitingTime)
        TestHelper.pressEnterKey()
    }

    private fun provideMarketURL() {
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime)
        TestHelper.inlineAutocompleteEditText.clearTextField()
        TestHelper.inlineAutocompleteEditText.text = "market://details?id=org.mozilla.firefox&referrer=" + "utm_source%3Dmozilla%26utm_medium%3DReferral%26utm_campaign%3Dmozilla-org"
        TestHelper.pressEnterKey()
    }

    private fun waitForDialog() {
        val cancelOpenAppBtn = TestHelper.mDevice.findObject(UiSelector()
            .resourceId("android:id/button2"))
        val openAppAlert = TestHelper.mDevice.findObject(UiSelector()
            .text("Open link in another app"))

        cancelOpenAppBtn.waitForExists(waitingTime)
        assertTrue(openAppAlert.exists())
        assertTrue(cancelOpenAppBtn.exists())
        cancelOpenAppBtn.click()
        TestHelper.floatingEraseButton.perform(click())
        TestHelper.erasedMsg.waitForExists(waitingTime)
    }
}
