/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.preference.PreferenceManager
import android.support.annotation.IdRes
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.IdlingRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4

import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.R
import org.mozilla.focus.helpers.SessionLoadedIdlingResource
import org.mozilla.focus.utils.AppConstants

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.Espresso.pressBack
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.containsString
import org.mozilla.focus.fragment.FirstrunFragment.Companion.FIRSTRUN_PREF
import org.mozilla.focus.helpers.EspressoHelper.openMenu

// This test visits each page and checks whether some essential elements are being displayed
// https://testrail.stage.mozaws.net/index.php?/cases/view/81664
@RunWith(AndroidJUnit4::class)
class AccessAboutAndYourRightsPagesTest {

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
        }
    }

    private var loadingIdlingResource: SessionLoadedIdlingResource? = null

    @Before
    fun setUp() {

        loadingIdlingResource = SessionLoadedIdlingResource()
        IdlingRegistry.getInstance().register(loadingIdlingResource!!)
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(loadingIdlingResource!!)

        mActivityTestRule.activity.finishAndRemoveTask()
    }

    @Test
    fun aboutAndYourRightsPagesTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // What's new page
        openMenu()
        clickMenuItem(R.id.whats_new)
        assertWebsiteUrlContains("support.mozilla.org")
        pressBack()

        // Help page
        openMenu()
        clickMenuItem(R.id.help)
        assertWebsiteUrlContains("what-firefox-focus-android")
        pressBack()

        // Go to settings "About" page
        openMenu()
        clickMenuItem(R.id.settings)
        val mozillaMenuLabel = context.getString(R.string.preference_category_mozilla, context.getString(R.string.app_name))
        onView(withText(mozillaMenuLabel))
            .check(matches(isDisplayed()))
            .perform(click())

        val aboutLabel = context.getString(R.string.preference_about, context.getString(R.string.app_name))
        onView(withText(aboutLabel))
            .check(matches(isDisplayed()))
            .perform(click())
        onView(withId(R.id.infofragment))
            .check(matches(isDisplayed()))
            .perform(click())
        pressBack() // This takes to Mozilla Submenu of settings

        // "Your rights" page
        onView(withText(context.getString(R.string.your_rights)))
            .check(matches(isDisplayed()))
            .perform(click())
        onView(withId(R.id.infofragment))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    private fun clickMenuItem(@IdRes id: Int) {
        onView(withId(id))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    private fun assertWebsiteUrlContains(substring: String) {
        onView(withId(R.id.display_url))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString(substring))))
    }
}
