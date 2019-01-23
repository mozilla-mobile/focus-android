/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.os.Build
import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiObject
import android.support.test.uiautomator.UiObjectNotFoundException
import android.support.test.uiautomator.UiSelector

import org.junit.After
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.helpers.TestHelper
import org.mozilla.focus.utils.AppConstants

import java.io.IOException

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

import org.mozilla.focus.fragment.FirstrunFragment.Companion.FIRSTRUN_PREF
import org.mozilla.focus.helpers.TestHelper.waitingTime

@RunWith(AndroidJUnit4::class)
// https://testrail.stage.mozaws.net/index.php?/cases/view/53141
class DownloadFileTest {

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
            // This test is for API 25 and greater. see https://github.com/mozilla-mobile/focus-android/issues/2696
            org.junit.Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)

            PreferenceManager.getDefaultSharedPreferences(appContext)
                .edit()
                .putBoolean(FIRSTRUN_PREF, true)
                .apply()

            webServer = MockWebServer()

            try {
                webServer?.enqueue(MockResponse()
                    .setBody(TestHelper.readTestAsset("image_test.html"))
                    .addHeader("Set-Cookie", "sphere=battery; Expires=Wed, 21 Oct 2035 07:28:00 GMT;"))
                webServer?.enqueue(MockResponse()
                    .setBody(TestHelper.readTestAsset("rabbit.jpg")))
                webServer?.enqueue(MockResponse()
                    .setBody(TestHelper.readTestAsset("download.jpg")))
                webServer?.enqueue(MockResponse()
                    .setBody(TestHelper.readTestAsset("download.jpg")))
                webServer?.enqueue(MockResponse()
                    .setBody(TestHelper.readTestAsset("download.jpg")))

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

        // If notification is still up, this will take it off screen
        TestHelper.pressBackKey()

        mActivityTestRule.activity.finishAndRemoveTask()
    }

    @Test
    @Throws(UiObjectNotFoundException::class)
    fun downloadTest() {
        val downloadIcon: UiObject = TestHelper.mDevice.findObject(UiSelector()
            .resourceId("download")
            .enabled(true))

        loadWebsite()
        downloadIcon.click()

        // If permission dialog appears, grant it
        if (TestHelper.permAllowBtn.waitForExists(waitingTime)) {
            TestHelper.permAllowBtn.click()
        }

        checkDownloadValues()
        checkCompletedMessage()
        checkNotification()
    }

    private fun loadWebsite() {
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime)
        TestHelper.inlineAutocompleteEditText.clearTextField()
        TestHelper.inlineAutocompleteEditText.text = webServer?.url(TEST_PATH).toString()
        TestHelper.hint.waitForExists(waitingTime)
        TestHelper.pressEnterKey()
        TestHelper.waitForWebContent()
        TestHelper.progressBar.waitUntilGone(waitingTime)
    }

    private fun checkDownloadValues() {
        TestHelper.downloadTitle.waitForExists(waitingTime)
        Assert.assertTrue(TestHelper.downloadTitle.isEnabled)
        Assert.assertTrue(TestHelper.downloadCancelBtn.isEnabled)
        Assert.assertTrue(TestHelper.downloadBtn.isEnabled)
        Assert.assertEquals(TestHelper.downloadFileName.text, "download.jpg")
        Assert.assertEquals(TestHelper.downloadWarning.text,
            "Downloaded files will not be deleted when you erase Firefox Focus history.")
    }

    private fun checkCompletedMessage() {
        TestHelper.downloadBtn.click()
        TestHelper.completedMsg.waitForExists(waitingTime)
        Assert.assertTrue(TestHelper.completedMsg.isEnabled)
        Assert.assertTrue(TestHelper.completedMsg.text.contains("finished"))
    }

    private fun checkNotification() {
        TestHelper.mDevice.openNotification()
        TestHelper.mDevice.waitForIdle()
        TestHelper.savedNotification.waitForExists(waitingTime)
        TestHelper.savedNotification.swipeRight(50)
    }

    companion object {
        private val TEST_PATH = "/"
    }
}
