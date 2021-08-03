/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.activity

import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.activity.robots.searchScreen
import org.mozilla.focus.helpers.MainActivityIntentsTestRule
import org.mozilla.focus.helpers.TestHelper.isPackageInstalled
import org.mozilla.focus.helpers.TestHelper.readTestAsset
import org.mozilla.focus.helpers.TestHelper.waitingTime
import org.mozilla.focus.testAnnotations.SmokeTest
import java.io.IOException

// This test verifies the "Open in..." option from the main menu
@RunWith(AndroidJUnit4ClassRunner::class)
class OpenInExternalBrowserDialogueTest {
    private lateinit var webServer: MockWebServer

    @get: Rule
    var mActivityTestRule = MainActivityIntentsTestRule(showFirstRun = false)

    @Before
    fun setUp() {
        webServer = MockWebServer()
        try {
            webServer.enqueue(
                MockResponse()
                    .setBody(readTestAsset("plain_test.html"))
            )
            webServer.start()
        } catch (e: IOException) {
            throw AssertionError("Could not start web server", e)
        }
    }

    @After
    fun tearDown() {
        mActivityTestRule.activity.finishAndRemoveTask()
        webServer.shutdown()
    }

    @SmokeTest
    @Test
    fun openPageInExternalAppTest() {
        val pageUrl = webServer.url("").toString()

        searchScreen {
        }.loadPage(pageUrl) {
        }.openMainMenu {
            clickOpenInOption()
            verifyOpenInDialog()
            clickOpenInChrome()
            assertChromeOpens()
        }
    }
}

private fun assertChromeOpens() {
    val googleChrome = "com.google.android.apps.chrome"
    if (isPackageInstalled(googleChrome)) {
        Intents.intended(IntentMatchers.toPackage(googleChrome))
    } else {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        mDevice.findObject(UiSelector().textContains("No app found")).waitForExists(waitingTime)
    }
}
