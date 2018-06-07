/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.helpers.SessionLoadedIdlingResource;
import org.mozilla.focus.helpers.TestHelper;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasFocus;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.mozilla.focus.fragment.FirstrunFragment.FIRSTRUN_PREF;

// This test toggles blocking within the browser view
// mozilla.org site has one google analytics tracker - tests will see whether this gets blocked properly
@RunWith(AndroidJUnit4.class)
public class ToggleBlockTest {
    private static final String TEST_PATH = "/";
    private MockWebServer webServer;

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<MainActivity>(MainActivity.class) {
        @Override
        protected void beforeActivityLaunched() {
            super.beforeActivityLaunched();

            Context appContext = InstrumentationRegistry.getInstrumentation()
                    .getTargetContext()
                    .getApplicationContext();

            PreferenceManager.getDefaultSharedPreferences(appContext)
                    .edit()
                    .putBoolean(FIRSTRUN_PREF, true)
                    .apply();

            webServer = new MockWebServer();

            try {
                webServer.enqueue(new MockResponse()
                        .setBody(TestHelper.readTestAsset("ad.html")));
                webServer.enqueue(new MockResponse()
                        .setBody(TestHelper.readTestAsset("ad.html")));
                webServer.enqueue(new MockResponse()
                        .setBody(TestHelper.readTestAsset("ad.html")));

                webServer.start();
            } catch (IOException e) {
                throw new AssertionError("Could not start web server", e);
            }
        }

        @Override
        protected void afterActivityFinished() {
            super.afterActivityFinished();

            try {
                webServer.close();
                webServer.shutdown();
            } catch (IOException e) {
                throw new AssertionError("Could not stop web server", e);
            }
        }
    };

    private SessionLoadedIdlingResource loadingIdlingResource;

    @Before
    public void setUp() {
        loadingIdlingResource = new SessionLoadedIdlingResource();
        IdlingRegistry.getInstance().register(loadingIdlingResource);
    }

    @After
    public void tearDown() {
        mActivityTestRule.getActivity().finishAndRemoveTask();

        IdlingRegistry.getInstance().unregister(loadingIdlingResource);
    }

    @Test
    public void SimpleToggleTest() {
        // Load mozilla.org
        onView(withId(R.id.urlView))
                .check(matches(isDisplayed()))
                .check(matches(hasFocus()))
                .perform(typeText(webServer.url(TEST_PATH).toString()), pressImeActionButton());

        TestHelper.waitForWebContent();
        TestHelper.progressBar.waitUntilGone(TestHelper.waitingTime);

        // The blocking badge is not disabled
        onView(withId(R.id.block))
                .check(matches(not(isDisplayed())));

        // Open the menu
        onView(withId(R.id.menuView))
                .perform(click());

        onView(withId(R.id.trackers_count))
                .check(matches(not(withText("-"))));
        onView(withId(R.id.trackers_count))
                .check(matches(not(withText("0"))));

        // Disable blocking
        onView(withId(R.id.blocking_switch))
                .check(matches(isChecked()));

        onView(withId(R.id.blocking_switch))
                .perform(click());
        TestHelper.waitForWebContent();

        // Now the blocking badge is visible
        onView(withId(R.id.block))
                .check(matches(isDisplayed()));

        // Open the menu again. Now the switch is disabled and the tracker count should be disabled.
        onView(withId(R.id.menuView))
                .perform(click());
        onView(withId(R.id.blocking_switch))
                .check(matches(not(isChecked())));
        onView(withId(R.id.trackers_count))
                .check(matches(withText("-")));
    }
}