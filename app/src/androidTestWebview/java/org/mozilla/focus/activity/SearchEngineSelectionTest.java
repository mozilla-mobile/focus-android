/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;
import android.widget.RadioButton;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mozilla.focus.helpers.TestHelper;

import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mozilla.focus.fragment.FirstrunFragment.FIRSTRUN_PREF;
import static org.mozilla.focus.helpers.EspressoHelper.openSettings;
import static org.mozilla.focus.helpers.TestHelper.waitingTime;
import static org.mozilla.focus.helpers.TestHelper.webPageLoadwaitingTime;

// This test checks the search engine can be changed
@RunWith(Parameterized.class)
public class SearchEngineSelectionTest {
    @Parameterized.Parameter
    public String mSearchEngine;

    @Parameterized.Parameters
    public static Iterable<? extends Object> data() {
        return Arrays.asList("Google", "DuckDuckGo");
    }

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

    @After
    public void tearDown() throws Exception {
        mActivityTestRule.getActivity().finishAndRemoveTask();
    }

    @Test
    public void SearchTest() throws UiObjectNotFoundException {
        UiObject settingsMenu = TestHelper.settingsList.getChild(new UiSelector()
                .className("android.widget.LinearLayout")
                .instance(1));

        UiObject searchEngineSelectorLabel = settingsMenu.getChild(new UiSelector()
                .resourceId("android:id/title")
                .text("Search")
                .enabled(true));

        String searchString = String.format("mozilla focus - %s Search", mSearchEngine);
        UiObject googleWebView = TestHelper.mDevice.findObject(new UiSelector()
                .description(searchString)
                .className("android.webkit.WebView"));

        // Open [settings menu] and select Search
        assertTrue(TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime));
        openSettings();
        TestHelper.settingsHeading.waitForExists(waitingTime);
        searchEngineSelectorLabel.click();

        // Open [settings menu] > [search engine menu] and click "Default" label
        // note: we don't click on default search engine itself cause this will change
        UiObject defaultSearchEngineLabel = TestHelper.mDevice.findObject(new UiSelector()
                .text("Default")
                .resourceId("android:id/summary"));
        defaultSearchEngineLabel.waitForExists(waitingTime);
        defaultSearchEngineLabel.click();

        // Open [settings menu] > [search engine menu] > [search engine list menu]
        // then select desired search engine
        UiScrollable searchEngineList = new UiScrollable(new UiSelector()
                .resourceId(TestHelper.getAppName() + ":id/search_engine_group").enabled(true));
        UiObject defaultEngineSelection = searchEngineList.getChildByText(new UiSelector()
                .className(RadioButton.class), mSearchEngine);
        defaultEngineSelection.waitForExists(waitingTime);
        assertTrue(defaultEngineSelection.getText().equals(mSearchEngine));
        defaultEngineSelection.click();
        TestHelper.pressBackKey();
        TestHelper.settingsHeading.waitForExists(waitingTime);
        UiObject defaultSearchEngine = TestHelper.mDevice.findObject(new UiSelector()
                .text(mSearchEngine)
                .resourceId("android:id/title"));
        assertTrue(defaultSearchEngine.getText().equals(mSearchEngine));
        TestHelper.pressBackKey();
        TestHelper.pressBackKey();

        // Do search on blank spaces and press enter for search (should not do anything)
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime);
        TestHelper.inlineAutocompleteEditText.clearTextField();
        TestHelper.inlineAutocompleteEditText.setText("   ");
        TestHelper.pressEnterKey();
        assertTrue(TestHelper.inlineAutocompleteEditText.exists());

        // Do search on text string
        TestHelper.inlineAutocompleteEditText.clearTextField();
        TestHelper.inlineAutocompleteEditText.setText("mozilla focus");
        TestHelper.hint.waitForExists(waitingTime);
        assertTrue(TestHelper.inlineAutocompleteEditText.exists());

        // Check that search hint bar correctly displayed
        assertTrue(TestHelper.hint.getText().equals("Search for mozilla focus"));
        TestHelper.hint.click();
        googleWebView.waitForExists(waitingTime);
        TestHelper.progressBar.waitForExists(webPageLoadwaitingTime);
        //Assert.assertTrue(TestHelper.progressBar.waitUntilGone(webPageLoadwaitingTime));
        TestHelper.progressBar.waitUntilGone(webPageLoadwaitingTime);
        // Search for words: <Google|DuckDuckGo|etc.>, mozilla, focus
        assertTrue(TestHelper.browserURLbar.getText().contains(mSearchEngine.toLowerCase()));
        assertTrue(TestHelper.browserURLbar.getText().contains("mozilla"));
        assertTrue(TestHelper.browserURLbar.getText().contains("focus"));

        // Tap URL bar, check it displays search term (instead of URL)
        TestHelper.browserURLbar.click();
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime);
        assertEquals(TestHelper.inlineAutocompleteEditText.getText(), "mozilla focus");
        TestHelper.pressEnterKey();
        googleWebView.waitForExists(waitingTime);
        TestHelper.progressBar.waitUntilGone(webPageLoadwaitingTime);
        // Search for words: <Google|DuckDuckGo|etc.>, mozilla, focus
        assertTrue(TestHelper.browserURLbar.getText().contains(mSearchEngine.toLowerCase()));
        assertTrue(TestHelper.browserURLbar.getText().contains("mozilla"));
        assertTrue(TestHelper.browserURLbar.getText().contains("focus"));

        // Do another search on text string
        TestHelper.browserURLbar.click();
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime);
        TestHelper.inlineAutocompleteEditText.clearTextField();
        TestHelper.inlineAutocompleteEditText.setText("mozilla focus");
        TestHelper.hint.waitForExists(waitingTime);

        // Check search hint bar correctly displayed
        assertTrue(TestHelper.hint.getText().equals("Search for mozilla focus"));
        TestHelper.hint.click();
    }
}
