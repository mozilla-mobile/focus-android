/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.content.Intent
import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.By
import android.support.test.uiautomator.UiObjectNotFoundException
import android.support.test.uiautomator.Until

import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.helpers.TestHelper

import java.io.IOException

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

import android.support.test.espresso.action.ViewActions.click
import org.hamcrest.core.IsNull.notNullValue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.mozilla.focus.fragment.FirstrunFragment.Companion.FIRSTRUN_PREF
import org.mozilla.focus.helpers.TestHelper.waitingTime

// This test erases URL and checks for message
// https://testrail.stage.mozaws.net/index.php?/cases/view/40068
@RunWith(AndroidJUnit4::class)
class EraseAllUserDataTest {
    private var webServer: MockWebServer? = null

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

            webServer = MockWebServer()

            try {
                webServer?.enqueue(MockResponse()
                    .setBody(TestHelper.readTestAsset("plain_test.html")))
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
    fun trashTest() {
        openWebpage()

        // Press erase button, and check for message and return to the main page
        TestHelper.floatingEraseButton.perform(click())
        TestHelper.erasedMsg.waitForExists(waitingTime)
        assertTrue(TestHelper.erasedMsg.exists())
        assertTrue(TestHelper.inlineAutocompleteEditText.exists())
    }

    @Test
    @Throws(UiObjectNotFoundException::class)
    fun systemBarTest() {
        openWebpage()
        TestHelper.menuButton.perform(click())
        TestHelper.blockCounterItem.waitForExists(waitingTime)

        // Pull down system bar and select delete browsing history
        TestHelper.openNotification()
        TestHelper.notificationBarDeleteItem.waitForExists(waitingTime)
        TestHelper.notificationBarDeleteItem.click()
        TestHelper.erasedMsg.waitForExists(waitingTime)
        assertTrue(TestHelper.erasedMsg.exists())
        assertTrue(TestHelper.inlineAutocompleteEditText.exists())
        assertFalse(TestHelper.menulist.exists())
    }

    @Test
    @Throws(UiObjectNotFoundException::class)
    fun systemBarHomeViewTest() {

        // Initialize UiDevice instance
        val LAUNCH_TIMEOUT = 5000

        openWebpage()

        // Switch out of Focus, pull down system bar and select delete browsing history
        TestHelper.pressHomeKey()
        TestHelper.openNotification()
        TestHelper.notificationBarDeleteItem.waitForExists(waitingTime)
        TestHelper.notificationBarDeleteItem.click()

        // Wait for launcher
        val launcherPackage = TestHelper.mDevice.launcherPackageName
        assertThat(launcherPackage, notNullValue())
        TestHelper.mDevice.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)),
            LAUNCH_TIMEOUT.toLong())

        launchApp()
    }

    private fun openWebpage() {
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime)
        TestHelper.inlineAutocompleteEditText.clearTextField()
        TestHelper.inlineAutocompleteEditText.text = webServer?.url(TEST_PATH).toString()
        TestHelper.hint.waitForExists(waitingTime)
        TestHelper.pressEnterKey()
        TestHelper.waitForWebContent()
    }

    private fun launchApp() {
        mActivityTestRule.launchActivity(Intent(Intent.ACTION_MAIN))
        // Verify that it's on the main view, not showing the previous browsing session
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime)
        assertTrue(TestHelper.inlineAutocompleteEditText.exists())
    }

    companion object {
        private val TEST_PATH = "/"
    }
}
