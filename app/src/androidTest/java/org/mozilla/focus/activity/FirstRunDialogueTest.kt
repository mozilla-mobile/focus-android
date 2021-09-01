/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.activity

import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.activity.robots.homeScreen
import org.mozilla.focus.activity.robots.searchScreen
import org.mozilla.focus.helpers.MainActivityFirstrunTestRule
import org.mozilla.focus.helpers.TestHelper.createMockResponseFromAsset
import org.mozilla.focus.testAnnotations.SmokeTest

// Tests the First run onboarding screens
@RunWith(AndroidJUnit4ClassRunner::class)
class FirstRunDialogueTest {
    private lateinit var webServer: MockWebServer

    @get: Rule
    val mActivityTestRule = MainActivityFirstrunTestRule(showFirstRun = true)

    @Before
    fun startWebServer() {
        webServer = MockWebServer()
    }

    @After
    fun stopWebServer() {
        webServer.shutdown()
    }

    @Test
    fun firstRunOnboardingTest() {
        homeScreen {
            verifyOnboardingFirstSlide()
            clickOnboardingNextBtn()
            verifyOnboardingSecondSlide()
            clickOnboardingNextBtn()
            verifyOnboardingThirdSlide()
            clickOnboardingNextBtn()
            verifyOnboardingLastSlide()
            clickOnboardingFinishBtn()
            verifyEmptySearchBar()
        }
    }

    @Test
    fun skipFirstRunOnboardingTest() {
        homeScreen {
            verifyOnboardingFirstSlide()
            clickOnboardingNextBtn()
            verifyOnboardingSecondSlide()
            skipFirstRun()
            verifyEmptySearchBar()
        }
    }

    @SmokeTest
    @Test
    fun homeScreenTipsTest() {
        webServer.start()
        webServer.enqueue(createMockResponseFromAsset("tab1.html"))
        val pageUrl = webServer.url("tab1.html").toString()

        homeScreen {
            skipFirstRun()
            verifyHomeScreenTipIsDisplayed(true)
            // hide the keyboard to have a scrollable tips carousel
            closeSoftKeyboard()
            verifyHomeScreenTipIsDisplayed(true)
            scrollLeftTipsCarousel()
            verifyHomeScreenTipIsDisplayed(true)
        }
        searchScreen {
        }.loadPage(pageUrl) {
        }.clearBrowsingData {
            verifyHomeScreenTipIsDisplayed(true)
        }
    }
}
