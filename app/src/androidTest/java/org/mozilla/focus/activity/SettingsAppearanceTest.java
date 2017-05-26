/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.action.ViewActions.click;
import static junit.framework.Assert.assertTrue;
import static org.mozilla.focus.activity.TestHelper.waitingTime;
import static org.mozilla.focus.fragment.FirstrunFragment.FIRSTRUN_PREF;

// This test checks all the headings in the Settings menu are there
@RunWith(AndroidJUnit4.class)
public class SettingsAppearanceTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule
            = new ActivityTestRule<MainActivity>(MainActivity.class) {

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
        }
    };

    @Test
    public void settingsScreenTest() throws InterruptedException, UiObjectNotFoundException {

        UiObject SearchEngineSelection = TestHelper.settingsList.getChild(new UiSelector()
                .className("android.widget.LinearLayout")
                .instance(0));
        UiObject searchHeading = TestHelper.mDevice.findObject(new UiSelector()
                .text("Search")
                .resourceId("android:id/title"));
        UiObject privacyHeading = TestHelper.mDevice.findObject(new UiSelector()
                .text("Privacy")
                .resourceId("android:id/title"));
        UiObject perfHeading = TestHelper.mDevice.findObject(new UiSelector()
                .text("Performance")
                .resourceId("android:id/title"));
        UiObject mozHeading = TestHelper.mDevice.findObject(new UiSelector()
                .text("Mozilla")
                .resourceId("android:id/title"));

        /* Go to Settings */
        TestHelper.urlBar.waitForExists(waitingTime);
        TestHelper.menuButton.perform(click());
        TestHelper.settingsMenuItem.click();
        SearchEngineSelection.waitForExists(waitingTime);

        /* Check the first element and other headings are present */
        assertTrue(SearchEngineSelection.isEnabled());
        assertTrue(searchHeading.exists());
        assertTrue(privacyHeading.exists());
        TestHelper.swipeUpScreen();
        assertTrue(perfHeading.exists());
        mozHeading.waitForExists(waitingTime);
        assertTrue(mozHeading.exists());
    }
}
