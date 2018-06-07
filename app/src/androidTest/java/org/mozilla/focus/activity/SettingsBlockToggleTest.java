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

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.helpers.TestHelper;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static junit.framework.Assert.assertTrue;
import static org.mozilla.focus.fragment.FirstrunFragment.FIRSTRUN_PREF;
import static org.mozilla.focus.helpers.EspressoHelper.openSettings;
import static org.mozilla.focus.helpers.TestHelper.waitingTime;

@RunWith(AndroidJUnit4.class)
public class SettingsBlockToggleTest {
    private static final String TEST_PATH = "/";
    private MockWebServer webServer;

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
            webServer = new MockWebServer();

            try {
                webServer.enqueue(new MockResponse()
                        .setBody(TestHelper.readTestAsset("plain_test.html")));
                webServer.enqueue(new MockResponse()
                        .setBody(TestHelper.readTestAsset("plain_test.html")));

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

    @After
    public void tearDown() {
        mActivityTestRule.getActivity().finishAndRemoveTask();
    }

    @Test
    public void SettingsToggleTest() throws UiObjectNotFoundException {

        UiObject privacyHeading = TestHelper.mDevice.findObject(new UiSelector()
                .text("Privacy & Security")
                .resourceId("android:id/title"));
        UiObject blockAdTrackerEntry = TestHelper.settingsList.getChild(new UiSelector()
                .className("android.widget.LinearLayout")
                .instance(1));
        UiObject blockAdTrackerValue = blockAdTrackerEntry.getChild(new UiSelector()
                .className("android.widget.Switch"));

        UiObject blockAnalyticTrackerEntry = TestHelper.settingsList.getChild(new UiSelector()
                .className("android.widget.LinearLayout")
                .instance(2));
        UiObject blockAnalyticTrackerValue = blockAnalyticTrackerEntry.getChild(new UiSelector()
                .className("android.widget.Switch"));

        UiObject blockSocialTrackerEntry = TestHelper.settingsList.getChild(new UiSelector()
                .className("android.widget.LinearLayout")
                .instance(4));
        UiObject blockSocialTrackerValue = blockSocialTrackerEntry.getChild(new UiSelector()
                .className("android.widget.Switch"));

        // Let's go to an actual URL which is http://
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime);
        TestHelper.inlineAutocompleteEditText.clearTextField();
        TestHelper.inlineAutocompleteEditText.setText(webServer.url(TEST_PATH).toString());
        TestHelper.hint.waitForExists(waitingTime);
        TestHelper.pressEnterKey();
        TestHelper.waitForWebContent();
        assertTrue (TestHelper.browserURLbar.getText().contains(webServer.url(TEST_PATH).toString()));
        assertTrue (!TestHelper.lockIcon.exists());

        /* Go to settings and disable everything */
        openSettings();
        privacyHeading.waitForExists(waitingTime);
        privacyHeading.click();
        blockAdTrackerEntry.click();
        assertTrue(blockAdTrackerValue.getText().equals("OFF"));
        blockAnalyticTrackerEntry.click();
        assertTrue(blockAnalyticTrackerValue.getText().equals("OFF"));
        blockSocialTrackerEntry.click();
        assertTrue(blockSocialTrackerValue.getText().equals("OFF"));

        // Turn back on
        blockAdTrackerEntry.click();
        blockAnalyticTrackerEntry.click();
        blockSocialTrackerEntry.click();

        //Back to the webpage
        TestHelper.pressBackKey();
        TestHelper.pressBackKey();
        TestHelper.waitForWebContent();
        assertTrue (TestHelper.browserURLbar.getText().contains(webServer.url(TEST_PATH).toString()));
        assertTrue (!TestHelper.lockIcon.exists());
    }
}
