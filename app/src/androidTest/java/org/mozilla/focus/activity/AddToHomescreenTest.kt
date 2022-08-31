/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.activity

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.activity.robots.searchScreen
import org.mozilla.focus.helpers.FeatureSettingsHelper
import org.mozilla.focus.helpers.MainActivityFirstrunTestRule
import org.mozilla.focus.helpers.MockWebServerHelper
import org.mozilla.focus.helpers.RetryTestRule
import org.mozilla.focus.helpers.TestAssetHelper.getGenericTabAsset
import org.mozilla.focus.helpers.TestHelper.randomString
import org.mozilla.focus.helpers.TestHelper.waitingTime
import org.mozilla.focus.testAnnotations.SmokeTest

/**
 * Tests to verify the functionality of Add to homescreen from the main menu
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class AddToHomescreenTest {
    private lateinit var webServer: MockWebServer
    private val featureSettingsHelper = FeatureSettingsHelper()

    @get: Rule
    var mActivityTestRule = MainActivityFirstrunTestRule(showFirstRun = false)

    @Rule
    @JvmField
    val retryTestRule = RetryTestRule(3)

    @Before
    fun setup() {
        webServer = MockWebServer().apply {
            dispatcher = MockWebServerHelper.AndroidAssetDispatcher()
            start()
        }
        featureSettingsHelper.setCfrForTrackingProtectionEnabled(false)
    }

    @After
    fun tearDown() {
        webServer.shutdown()
        featureSettingsHelper.resetAllFeatureFlags()
    }

    @SmokeTest
    @Test
    fun addPageToHomeScreenTest() {
        val pageUrl = getGenericTabAsset(webServer, 1).url
        val pageTitle = randomString(5)

        searchScreen {
        }.loadPage(pageUrl) {
            progressBar.waitUntilGone(waitingTime)
        }.openMainMenu {
        }.openAddToHSDialog {
            addShortcutWithTitle(pageTitle)
            handleAddAutomaticallyDialog()
        }.searchAndOpenHomeScreenShortcut(pageTitle) {
            verifyPageURL(pageUrl)
        }
    }

    @SmokeTest
    @Test
    fun noNameShortcutTest() {
        val pageUrl = getGenericTabAsset(webServer, 1).url

        searchScreen {
        }.loadPage(pageUrl) {
        }.openMainMenu {
        }.openAddToHSDialog {
            // leave shortcut title empty and add it to HS
            addShortcutNoTitle()
            handleAddAutomaticallyDialog()
        }.searchAndOpenHomeScreenShortcut(webServer.hostName) {
            // only checking a part of the URL that is constant,
            // in case it opens a different shortcut on a retry
            verifyPageURL("tab1.html")
        }
    }
}
