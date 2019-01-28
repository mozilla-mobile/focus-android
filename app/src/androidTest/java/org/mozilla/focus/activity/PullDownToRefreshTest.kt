/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.support.test.espresso.UiController
import android.support.test.espresso.ViewAction
import android.support.test.espresso.web.webdriver.Locator
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.view.View

import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.R
import org.mozilla.focus.helpers.MainActivityFirstrunTestRule
import org.mozilla.focus.helpers.TestHelper

import java.io.IOException

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.pressImeActionButton
import android.support.test.espresso.action.ViewActions.replaceText
import android.support.test.espresso.action.ViewActions.swipeDown
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.hasChildCount
import android.support.test.espresso.matcher.ViewMatchers.hasFocus
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.espresso.web.assertion.WebViewAssertions.webMatches
import android.support.test.espresso.web.sugar.Web.onWebView
import android.support.test.espresso.web.webdriver.DriverAtoms.findElement
import android.support.test.espresso.web.webdriver.DriverAtoms.getText
import org.hamcrest.Matchers.containsString

// https://testrail.stage.mozaws.net/index.php?/cases/view/94146
@RunWith(AndroidJUnit4::class)
@Ignore("Pull to refresh is currently disabled in all builds")
class PullDownToRefreshTest {

    private var webServer: MockWebServer? = null
    @Rule
    var mActivityTestRule: ActivityTestRule<MainActivity> = MainActivityFirstrunTestRule(false)

    @Before
    @Throws(IOException::class)
    fun setUpWebServer() {
        webServer = MockWebServer()

        // Test page
        webServer?.enqueue(MockResponse().setBody(TestHelper.readTestAsset("counter.html")))
        webServer?.enqueue(MockResponse().setBody(TestHelper.readTestAsset("counter.html")))
    }

    @After
    fun tearDownWebServer() {
        try {
            webServer?.close()
            webServer?.shutdown()
        } catch (e: IOException) {
            throw AssertionError("Could not stop web server", e)
        }
    }

    @Test
    fun pullDownToRefreshTest() {

        onView(withId(R.id.urlView))
            .check(matches(isDisplayed()))
            .check(matches(hasFocus()))
            .perform(click(), replaceText(webServer?.url("/").toString()), pressImeActionButton())

        onView(withId(R.id.display_url))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString(webServer?.hostName))))

        onWebView()
            .withElement(findElement(Locator.ID, COUNTER))
            .check(webMatches(getText(), containsString(FIRST_TIME)))

        onView(withId(R.id.swipe_refresh))
            .check(matches(isDisplayed()))

        checkSpinnerAndProgressBarAreShown()
    }

    companion object {
        private val COUNTER = "counter"
        private val FIRST_TIME = "1"
        private val SECOND_TIME = "2"

        fun withCustomConstraints(action: ViewAction, constraints: Matcher<View>): ViewAction {
            return object : ViewAction {
                override fun getConstraints(): Matcher<View> {
                    return constraints
                }

                override fun getDescription(): String {
                    return action.description
                }

                override fun perform(uiController: UiController, view: View) {
                    action.perform(uiController, view)
                }
            }
        }
    }

    private fun checkSpinnerAndProgressBarAreShown() {

        // Swipe down to refresh, spinner is shown (2nd child) and progress bar is shown
        onView(withId(R.id.swipe_refresh))
            .perform(withCustomConstraints(swipeDown(), isDisplayingAtLeast(85)))
            .check(matches(hasChildCount(2)))

        onWebView()
            .withElement(findElement(Locator.ID, COUNTER))
            .check(webMatches(getText(), containsString(SECOND_TIME)))
    }
}
