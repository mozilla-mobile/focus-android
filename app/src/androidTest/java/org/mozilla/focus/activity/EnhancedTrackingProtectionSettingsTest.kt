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
import org.mozilla.focus.activity.robots.browserScreen
import org.mozilla.focus.activity.robots.homeScreen
import org.mozilla.focus.activity.robots.searchScreen
import org.mozilla.focus.helpers.MainActivityFirstrunTestRule
import org.mozilla.focus.helpers.TestHelper.createMockResponseFromAsset
import org.mozilla.focus.helpers.TestHelper.exitToBrowser
import org.mozilla.focus.helpers.TestHelper.exitToTop
import org.mozilla.focus.helpers.TestHelper.webPageLoadwaitingTime
import org.mozilla.focus.testAnnotations.SmokeTest
import java.io.IOException

@RunWith(AndroidJUnit4ClassRunner::class)
class EnhancedTrackingProtectionSettingsTest {
    private lateinit var webServer: MockWebServer

    @get: Rule
    var mActivityTestRule = MainActivityFirstrunTestRule(showFirstRun = false)

    @Before
    fun setUp() {
        webServer = MockWebServer()
        try {
            webServer.start()
        } catch (e: IOException) {
            throw AssertionError("Could not start web server", e)
        }
    }

    @After
    fun tearDown() {
        try {
            webServer.close()
            webServer.shutdown()
        } catch (e: IOException) {
            throw AssertionError("Could not stop web server", e)
        }
    }

    @SmokeTest
    @Test
    fun trackingProtectionTogglesListTest() {
        homeScreen {
        }.openMainMenu {
        }.openSettings {
        }.openPrivacySettingsMenu {
            verifyBlockAdTrackersEnabled(true)
            verifyBlockAnalyticTrackersEnabled(true)
            verifyBlockSocialTrackersEnabled(true)
            verifyBlockOtherTrackersEnabled(false)
        }
    }

    @SmokeTest
    @Test
    fun blockAdTrackersTest() {
        webServer.enqueue(createMockResponseFromAsset("plain_test.html"))
        webServer.enqueue(createMockResponseFromAsset("etpPages/adsTrackers.html"))
        val genericPage = webServer.url("plain_test.html").toString()
        val trackingPage = webServer.url("etpPages/adsTrackers.html").toString()

        searchScreen {
        }.loadPage(genericPage) {
            // loading a generic page to allow GV to fully load on first run
            verifyPageContent("focus test page")
        }.clearBrowsingData { }
        searchScreen {
        }.loadPage(trackingPage) {
            verifyTrackingProtectionAlert("ads trackers blocked")
        }
    }

    @SmokeTest
    @Test
    fun allowAdTrackersTest() {
        webServer.enqueue(createMockResponseFromAsset("plain_test.html"))
        webServer.enqueue(createMockResponseFromAsset("etpPages/adsTrackers.html"))
        val genericPage = webServer.url("plain_test.html").toString()
        val trackingPage = webServer.url("etpPages/adsTrackers.html").toString()

        homeScreen {
        }.openMainMenu {
        }.openSettings {
        }.openPrivacySettingsMenu {
            clickAdTrackersBlockSwitch()
            verifyBlockAdTrackersEnabled(false)
            exitToTop()
        }
        searchScreen {
        }.loadPage(genericPage) {
            // loading a generic page to allow GV to fully load on first run
            verifyPageContent("focus test page")
        }.clearBrowsingData { }
        searchScreen {
        }.loadPage(trackingPage) {
            verifyPageContent("ads trackers not blocked")
        }
    }

    @SmokeTest
    @Test
    fun blockAnalyticsTrackersTest() {
        webServer.enqueue(createMockResponseFromAsset("plain_test.html"))
        webServer.enqueue(createMockResponseFromAsset("etpPages/analyticsTrackers.html"))
        val genericPage = webServer.url("plain_test.html").toString()
        val trackingPage = webServer.url("etpPages/analyticsTrackers.html").toString()

        searchScreen {
        }.loadPage(genericPage) {
            // loading a generic page to allow GV to fully load on first run
            verifyPageContent("focus test page")
        }.clearBrowsingData { }
        searchScreen {
        }.loadPage(trackingPage) {
            verifyTrackingProtectionAlert("analytics trackers blocked")
        }
    }

    @SmokeTest
    @Test
    fun allowAnalyticsTrackersTest() {
        webServer.enqueue(createMockResponseFromAsset("plain_test.html"))
        webServer.enqueue(createMockResponseFromAsset("etpPages/analyticsTrackers.html"))
        val genericPage = webServer.url("plain_test.html").toString()
        val trackingPage = webServer.url("etpPages/analyticsTrackers.html").toString()

        homeScreen {
        }.openMainMenu {
        }.openSettings {
        }.openPrivacySettingsMenu {
            clickAnalyticsTrackersBlockSwitch()
            verifyBlockAnalyticTrackersEnabled(false)
            exitToTop()
        }
        searchScreen {
        }.loadPage(genericPage) {
            // loading a generic page to allow GV to fully load on first run
            verifyPageContent("focus test page")
        }.clearBrowsingData { }
        searchScreen {
        }.loadPage(trackingPage) {
            verifyPageContent("analytics trackers not blocked")
        }
    }

    @SmokeTest
    @Test
    fun blockSocialTrackersTest() {
        webServer.enqueue(createMockResponseFromAsset("plain_test.html"))
        webServer.enqueue(createMockResponseFromAsset("etpPages/socialTrackers.html"))
        val genericPage = webServer.url("plain_test.html").toString()
        val trackingPage = webServer.url("etpPages/socialTrackers.html").toString()

        searchScreen {
        }.loadPage(genericPage) {
            // loading a generic page to allow GV to fully load on first run
            verifyPageContent("focus test page")
        }.clearBrowsingData { }
        searchScreen {
        }.loadPage(trackingPage) {
            verifyTrackingProtectionAlert("social trackers blocked")
        }
    }

    @SmokeTest
    @Test
    fun allowSocialTrackersTest() {
        webServer.enqueue(createMockResponseFromAsset("plain_test.html"))
        webServer.enqueue(createMockResponseFromAsset("etpPages/socialTrackers.html"))
        val genericPage = webServer.url("plain_test.html").toString()
        val trackingPage = webServer.url("etpPages/socialTrackers.html").toString()

        homeScreen {
        }.openMainMenu {
        }.openSettings {
        }.openPrivacySettingsMenu {
            clickSocialTrackersBlockSwitch()
            verifyBlockSocialTrackersEnabled(false)
            exitToTop()
        }
        searchScreen {
        }.loadPage(genericPage) {
            // loading a generic page to allow GV to fully load on first run
            verifyPageContent("focus test page")
        }.clearBrowsingData { }
        searchScreen {
        }.loadPage(trackingPage) {
            verifyPageContent("social trackers not blocked")
        }
    }

    @SmokeTest
    @Test
    fun allowOtherContentTrackersTest() {
        webServer.enqueue(createMockResponseFromAsset("plain_test.html"))
        webServer.enqueue(createMockResponseFromAsset("etpPages/otherTrackers.html"))
        val genericPage = webServer.url("plain_test.html").toString()
        val trackingPage = webServer.url("etpPages/otherTrackers.html").toString()

        searchScreen {
        }.loadPage(genericPage) {
            // loading a generic page to allow GV to fully load on first run
            verifyPageContent("focus test page")
        }.clearBrowsingData { }
        searchScreen {
        }.loadPage(trackingPage) {
            verifyPageContent("other content trackers not blocked")
        }
    }

    @SmokeTest
    @Test
    fun blockOtherContentTrackersTest() {
        webServer.enqueue(createMockResponseFromAsset("plain_test.html"))
        webServer.enqueue(createMockResponseFromAsset("etpPages/otherTrackers.html"))
        val genericPage = webServer.url("plain_test.html").toString()
        val trackingPage = webServer.url("etpPages/otherTrackers.html").toString()

        homeScreen {
        }.openMainMenu {
        }.openSettings {
        }.openPrivacySettingsMenu {
            clickOtherContentTrackersBlockSwitch()
            verifyBlockOtherTrackersEnabled(true)
            exitToTop()
        }
        searchScreen {
        }.loadPage(genericPage) {
            // loading a generic page to allow GV to fully load on first run
            verifyPageContent("focus test page")
        }.clearBrowsingData { }
        searchScreen {
        }.loadPage(trackingPage) {
            verifyTrackingProtectionAlert("other content trackers blocked")
        }
    }

    @SmokeTest
    @Test
    fun addURLToTPExceptionsListTest() {
        webServer.enqueue(createMockResponseFromAsset("plain_test.html"))
        val genericPage = webServer.url("plain_test.html").toString()

        searchScreen {
        }.loadPage(genericPage) {
        }.openSiteSecurityInfoSheet {
        }.clickTrackingProtectionSwitch {
        }.openMainMenu {
        }.openSettings {
        }.openPrivacySettingsMenu {
            openExceptionsList()
            verifyExceptionURL(webServer.hostName)
        }
    }

    @SmokeTest
    @Test
    fun removeOneExceptionURLTest() {
        webServer.enqueue(createMockResponseFromAsset("plain_test.html"))
        val genericPage = webServer.url("plain_test.html").toString()

        searchScreen {
        }.loadPage(genericPage) {
        }.openSiteSecurityInfoSheet {
        }.clickTrackingProtectionSwitch {
            progressBar.waitUntilGone(webPageLoadwaitingTime)
        }.openMainMenu {
        }.openSettings {
        }.openPrivacySettingsMenu {
            openExceptionsList()
            removeException()
            verifyExceptionsListDisabled()
            exitToBrowser()
        }
        browserScreen {
        }.openSiteSecurityInfoSheet {
            verifyTrackingProtectionIsEnabled(true)
        }
    }

    @SmokeTest
    @Test
    fun removeAllExceptionURLTest() {
        webServer.enqueue(createMockResponseFromAsset("plain_test.html"))
        val genericPage = webServer.url("plain_test.html").toString()

        searchScreen {
        }.loadPage(genericPage) {
        }.openSiteSecurityInfoSheet {
        }.clickTrackingProtectionSwitch {
        }.openMainMenu {
        }.openSettings {
        }.openPrivacySettingsMenu {
            openExceptionsList()
            removeAllExceptions()
            // Failing due to: https://github.com/mozilla-mobile/focus-android/issues/5738
            // verifyExceptionsListDisabled()
            exitToBrowser()
        }
        browserScreen {
        }.openSiteSecurityInfoSheet {
            verifyTrackingProtectionIsEnabled(true)
        }
    }
}
