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
import org.mozilla.focus.R
import org.mozilla.focus.helpers.TestHelper

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.pressImeActionButton
import android.support.test.espresso.action.ViewActions.replaceText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.view.KeyEvent.KEYCODE_SPACE
import android.view.View
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertTrue
import org.mozilla.focus.fragment.FirstrunFragment.Companion.FIRSTRUN_PREF
import org.mozilla.focus.helpers.EspressoHelper.childAtPosition
import org.mozilla.focus.helpers.TestHelper.waitingTime
import org.mozilla.focus.helpers.TestHelper.webPageLoadwaitingTime

// This test checks whether URL and displayed site are in sync
@RunWith(AndroidJUnit4::class)
class URLMismatchTest {

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
        }
    }

    @After
    fun tearDown() {
        mActivityTestRule.activity.finishAndRemoveTask()
    }

    @Test
    @Throws(UiObjectNotFoundException::class)
    fun mismatchTest() {
        val searchString = String.format("mozilla focus - %s Search", "google")
        val googleWebView = TestHelper.mDevice.findObject(UiSelector()
            .description(searchString)
            .className("android.webkit.WebView"))

        verifySearchSuggestions()
        verifySearchHint()

        // WebView is displayed
        TestHelper.pressEnterKey()
        googleWebView.waitForExists(waitingTime)
        TestHelper.progressBar.waitUntilGone(webPageLoadwaitingTime)

        checkDisplayUrl()
        verifyAutoComplete()
        loadSiteAndVerifyUrl()
    }

    private fun verifySearchHint() {

        // Verify that at least 1 search hint is displayed and click it.
        TestHelper.mDevice.pressKeyCode(KEYCODE_SPACE)
        TestHelper.suggestionList.waitForExists(waitingTime)
        assertTrue(TestHelper.suggestionList.childCount >= 1)

        onView(allOf<View>(withText(containsString("mozilla")),
            withId(R.id.suggestion),
            isDescendantOfA(childAtPosition(withId(R.id.suggestionList), 1))))
            .check(matches(isDisplayed()))
    }

    private fun checkDisplayUrl() {

        // The displayed URL contains mozilla. Click on it to edit it again.
        onView(withId(R.id.display_url))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString("mozilla"))))
            .perform(click())
    }

    private fun verifyAutoComplete() {
        // Type "moz" - Verify that it auto-completes to "mozilla.org" and then load the website
        onView(withId(R.id.urlView))
            .perform(click(), replaceText("mozilla"))
            .check(matches(withText("mozilla.org")))
            .perform(pressImeActionButton())
    }

    private fun loadSiteAndVerifyUrl() {
        // Wait until site is loaded
        onView(withId(R.id.webview))
            .check(matches(isDisplayed()))
        TestHelper.progressBar.waitUntilGone(webPageLoadwaitingTime)

        // The displayed URL contains www.mozilla.org
        onView(withId(R.id.display_url))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString("www.mozilla.org"))))
    }

    private fun verifySearchSuggestions() {
        // Do search on text string
        TestHelper.inlineAutocompleteEditText.clearTextField()
        TestHelper.inlineAutocompleteEditText.text = "mozilla "
        // Would you like to turn on search suggestions? Yes No
        // fresh install only)
        if (TestHelper.searchSuggestionsTitle.exists()) {
            TestHelper.searchSuggestionsButtonYes.waitForExists(waitingTime)
            TestHelper.searchSuggestionsButtonYes.click()
        }
    }
}
