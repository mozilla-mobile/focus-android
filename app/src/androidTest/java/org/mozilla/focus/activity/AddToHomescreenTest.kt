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

import android.support.test.espresso.action.ViewActions.click
import org.mozilla.focus.fragment.FirstrunFragment.Companion.FIRSTRUN_PREF
import org.mozilla.focus.helpers.TestHelper.waitingTime
import org.mozilla.focus.helpers.TestHelper.webPageLoadwaitingTime

// https://testrail.stage.mozaws.net/index.php?/cases/view/60852
// includes:
// https://testrail.stage.mozaws.net/index.php?/cases/view/40066
@RunWith(AndroidJUnit4::class)
@Suppress("TooManyFunctions")
class AddToHomescreenTest {
    private var webServer: MockWebServer? = null
    private var webServerPort: Int = 0
    private var webServerBookmarkName: String? = null

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
            // note: requesting getPort() will automatically start the mock server,
            //       so if you use the 2 lines, do not try to start server or it will choke.
            webServerPort = webServer?.port ?: 0
            webServerBookmarkName = "localhost_" + Integer.toString(webServerPort)

            try {
                webServer?.enqueue(MockResponse()
                    .setBody(TestHelper.readTestAsset("plain_test.html")))
                webServer?.enqueue(MockResponse()
                    .setBody(TestHelper.readTestAsset("plain_test.html")))
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

    private val welcomeBtn = TestHelper.mDevice.findObject(UiSelector()
        .resourceId("com.android.launcher3:id/cling_dismiss_longpress_info")
        .text("GOT IT")
        .enabled(true))

    @After
    fun tearDown() {
        mActivityTestRule.activity.finishAndRemoveTask()
    }

    @Test
    fun addToHomeScreenTest() {
        openAndAddToHomeScreen()
        openAddToHSDialog()
        checkHomeScreenDialogIsShown()
        editShortcutText()

        // For Android O, we need additional steps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            handleShortcutLayoutDialog()
        }

        checkAppIsInBackground()
    }

    @Test
    fun noNameTest() {
        openAndAddToHomeScreen()
        openAddToHSDialog()
        checkHomeScreenDialogIsShown()
        editShortcutText()

        // For Android O, we need additional steps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            handleShortcutLayoutDialog()
        }
        startFromHomeAndSwipe()
    }

    @Test
    fun searchTermShortcutTest() {
        // Open website, and click 'Add to homescreen'
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime)
        TestHelper.inlineAutocompleteEditText.clearTextField()
        TestHelper.inlineAutocompleteEditText.text = "helloworld"
        TestHelper.hint.waitForExists(waitingTime)
        TestHelper.pressEnterKey()

        waitForProgressBar()
        openAddToHSDialog()
        clickAdd()

        // For Android O, we need additional steps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            handleShortcutLayoutDialog()
        }
        if (welcomeBtn.exists()) {
            welcomeBtn.click()
        }

        clickShortcutIconAndCheckURL()
    }

    private fun handleShortcutLayoutDialog() {
        TestHelper.AddautoBtn.waitForExists(waitingTime)
        TestHelper.AddautoBtn.click()
        TestHelper.AddautoBtn.waitUntilGone(waitingTime)
        TestHelper.pressHomeKey()
    }

    private fun openAddToHSDialog() {
        TestHelper.menuButton.perform(click())
        TestHelper.AddtoHSmenuItem.waitForExists(waitingTime)
        // If the menu item is not clickable, wait and retry
        while (!TestHelper.AddtoHSmenuItem.isClickable) {
            TestHelper.pressBackKey()
            TestHelper.menuButton.perform(click())
        }
        TestHelper.AddtoHSmenuItem.click()
    }

    private fun openAndAddToHomeScreen() {
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime)
        TestHelper.inlineAutocompleteEditText.clearTextField()
        TestHelper.inlineAutocompleteEditText.text = webServer?.url(TEST_PATH).toString()
        TestHelper.hint.waitForExists(waitingTime)
        TestHelper.pressEnterKey()
        TestHelper.progressBar.waitForExists(waitingTime)
        Assert.assertTrue(TestHelper.progressBar.waitUntilGone(webPageLoadwaitingTime))
        if (!AppConstants.isGeckoBuild) {
            TestHelper.waitForWebSiteTitleLoad()
        }
    }

    private fun checkHomeScreenDialogIsShown() {
        TestHelper.shortcutTitle.waitForExists(waitingTime)

        Assert.assertTrue(TestHelper.shortcutTitle.isEnabled)
        Assert.assertEquals(TestHelper.shortcutTitle.text, "gigantic experience")
        Assert.assertTrue(TestHelper.AddtoHSOKBtn.isEnabled)
        Assert.assertTrue(TestHelper.AddtoHSCancelBtn.isEnabled)
    }

    private fun editShortcutText() {
        TestHelper.shortcutTitle.click()
        TestHelper.shortcutTitle.text = webServerBookmarkName
        TestHelper.AddtoHSOKBtn.click()
    }

    private fun checkAppIsInBackground() {
        val shortcutIcon = TestHelper.mDevice.findObject(UiSelector()
            .className("android.widget.TextView")
            .description(webServerBookmarkName)
            .enabled(true))

        //App is sent to background, in launcher now
        if (welcomeBtn.exists()) {
            welcomeBtn.click()
        }
        shortcutIcon.waitForExists(waitingTime)
        Assert.assertTrue(shortcutIcon.isEnabled)
        shortcutIcon.click()
        TestHelper.browserURLbar.waitForExists(waitingTime)
        Assert.assertTrue(
            TestHelper.browserURLbar.text == webServer?.url(TEST_PATH).toString())
    }

    private fun startFromHomeAndSwipe() {
        val shortcutIcon = TestHelper.mDevice.findObject(UiSelector()
            .className("android.widget.TextView")
            .description(webServerBookmarkName)
            .enabled(true))

        if (welcomeBtn.exists()) {
            welcomeBtn.click()
        }
        //App is sent to background, in launcher now
        //Start from home and then swipe, to ensure we land where we want to search for shortcut
        TestHelper.mDevice.pressHome()
        TestHelper.swipeScreenLeft()
        shortcutIcon.waitForExists(waitingTime)
        Assert.assertTrue(shortcutIcon.isEnabled)
        shortcutIcon.click()
        TestHelper.browserURLbar.waitForExists(waitingTime)
        Assert.assertTrue(
            TestHelper.browserURLbar.text == webServer?.url(TEST_PATH).toString())
    }

    private fun waitForProgressBar() {
        // In certain cases, where progressBar disappears immediately, below will return false
        // since it busy waits, it will unblock when the bar isn't visible, regardless of the
        // return value
        TestHelper.progressBar.waitForExists(waitingTime)
        TestHelper.progressBar.waitUntilGone(webPageLoadwaitingTime)
    }

    private fun clickShortcutIconAndCheckURL() {
        val shortcutIcon = TestHelper.mDevice.findObject(UiSelector()
            .className("android.widget.TextView")
            .descriptionContains("helloworld")
            .enabled(true))

        //App is sent to background, in launcher now
        //Start from home and then swipe, to ensure we land where we want to search for shortcut
        TestHelper.mDevice.pressHome()
        TestHelper.swipeScreenLeft()
        shortcutIcon.waitForExists(waitingTime)
        Assert.assertTrue(shortcutIcon.isEnabled)
        shortcutIcon.click()
        TestHelper.waitForIdle()
        TestHelper.waitForWebContent()

        //Tap URL bar and check the search term is still shown
        TestHelper.browserURLbar.waitForExists(waitingTime)
        Assert.assertTrue(TestHelper.browserURLbar.text.contains("helloworld"))
    }

    private fun clickAdd() {
        // "Add to Home screen" dialog is now shown
        TestHelper.shortcutTitle.waitForExists(waitingTime)
        TestHelper.AddtoHSOKBtn.click()
    }

    companion object {
        private val TEST_PATH = "/"
    }
}
