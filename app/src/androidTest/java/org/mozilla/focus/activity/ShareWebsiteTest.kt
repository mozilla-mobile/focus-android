/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiObjectNotFoundException
import android.support.test.uiautomator.UiSelector

import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.helpers.TestHelper
import org.mozilla.focus.utils.AppConstants

import java.io.IOException

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

import android.support.test.espresso.action.ViewActions.click
import org.junit.Assert.assertTrue
import org.mozilla.focus.fragment.FirstrunFragment.Companion.FIRSTRUN_PREF
import org.mozilla.focus.helpers.TestHelper.waitingTime

// This test opens share menu
// https://testrail.stage.mozaws.net/index.php?/cases/view/47592
@RunWith(AndroidJUnit4::class)
class ShareWebsiteTest {
    private var webServer: MockWebServer? = null

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

            webServer = MockWebServer()

            try {
                webServer?.enqueue(MockResponse()
                    .setBody(TestHelper.readTestAsset("plain_test.html")))
                webServer?.enqueue(MockResponse()
                    .setBody(TestHelper.readTestAsset("plain_test.html")))

                webServer?.start()
            } catch (e: IOException) {
                throw AssertionError("Could not start web server", e)
            }
        }

        override fun afterActivityFinished() {
            super.afterActivityFinished()

            try {
                webServer?.close()
                webServer?.shutdown()
            } catch (e: IOException) {
                throw AssertionError("Could not stop web server", e)
            }
        }
    }

    @After
    fun tearDown() {
        mActivityTestRule.activity.finishAndRemoveTask()
    }

    @Test
    @Throws(UiObjectNotFoundException::class)
    fun shareWebsiteTest() {

        val shareBtn = TestHelper.mDevice.findObject(UiSelector()
            .resourceId(TestHelper.getAppName() + ":id/share")
            .enabled(true))

        /* Go to a webpage */
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime)
        TestHelper.inlineAutocompleteEditText.clearTextField()
        TestHelper.inlineAutocompleteEditText.text = webServer?.url(TEST_PATH).toString()
        TestHelper.hint.waitForExists(waitingTime)
        TestHelper.pressEnterKey()
        assertTrue(TestHelper.webView.waitForExists(waitingTime))

        /* Select share */
        TestHelper.menuButton.perform(click())
        shareBtn.waitForExists(waitingTime)
        shareBtn.click()

        // For simulators, where apps are not installed, it'll take to message app
        TestHelper.shareMenuHeader.waitForExists(waitingTime)
        assertTrue(TestHelper.shareMenuHeader.exists())
        assertTrue(TestHelper.shareAppList.exists())
        TestHelper.pressBackKey()
    }

    companion object {
        private val TEST_PATH = "/"
    }
}
