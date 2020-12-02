/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Context;

import androidx.preference.PreferenceManager;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewInteraction;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.UiObjectNotFoundException;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.helpers.TestHelper;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.mozilla.focus.fragment.FirstrunFragment.FIRSTRUN_PREF;
import static org.mozilla.focus.helpers.EspressoHelper.childAtPosition;
import static org.mozilla.focus.helpers.EspressoHelper.openSettings;
import static org.mozilla.focus.helpers.TestHelper.mDevice;
import static org.mozilla.focus.helpers.TestHelper.waitingTime;

// https://testrail.stage.mozaws.net/index.php?/cases/view/104577
@RunWith(AndroidJUnit4.class)
public class URLAutocompleteTest {
    private String site = "680news.com";

    // From API 24 and above
    private DataInteraction CustomURLRow = onData(anything())
            .inAdapterView(allOf(withId(android.R.id.list),
                childAtPosition(
                        withId(android.R.id.list_container),
            0)))
            .atPosition(4);

    // From API 23 and below
    private DataInteraction CustomURLRow_old = onData(anything())
            .inAdapterView(allOf(withId(android.R.id.list),
                    childAtPosition(
                            withClassName(is("android.widget.LinearLayout")),
                            0)))
            .atPosition(4);

    /* TODO: Reenable after fixing AndroidX migration issues
    private ViewInteraction AutoCompleteDialog = onView(allOf(withId(R.id.recycler_view),
            childAtPosition(withId(android.R.id.list_container), 0)));
    */
    private ViewInteraction AutoCompleteDialog = null;

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

            // This test runs on both GV and WV.
            // Klar is used to test Geckoview. make sure it's set to Gecko
            TestHelper.selectGeckoForKlar();
        }
    };

    @After
    public void tearDown() {
        mActivityTestRule.getActivity().finishAndRemoveTask();
    }

    @Test
    public void CompletionTest() throws UiObjectNotFoundException {
        /* type a partial url, and check it autocompletes*/
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime);
        TestHelper.inlineAutocompleteEditText.setText("mozilla");
        TestHelper.hint.waitForExists(waitingTime);
        assertTrue (TestHelper.inlineAutocompleteEditText.getText().equals("mozilla.org"));

        /* press x to delete the both autocomplete and suggestion */
        TestHelper.cleartextField.click();
        assertTrue (TestHelper.inlineAutocompleteEditText.getText().equals("Search or enter address"));
        assertFalse (TestHelper.hint.exists());

        /* type a full url, and check it does not autocomplete */
        TestHelper.inlineAutocompleteEditText.setText("http://www.mozilla.org");
        TestHelper.hint.waitForExists(waitingTime);
        assertTrue (TestHelper.inlineAutocompleteEditText.getText().equals("http://www.mozilla.org"));
    }

    // TODO: Reenable after fixing AndroidX migration issues
    @Ignore
    @Test
    // Add custom autocomplete, and check to see it works
    public void CustomCompletionTest() throws UiObjectNotFoundException {
        OpenCustomCompleteDialog();

        // Enable URL autocomplete, and add URL
        addAutoComplete(site);
        exitToTop();

        // Check for custom autocompletion
        checkACOn(site);
        // Remove custom autocompletion site
        OpenCustomCompleteDialog();
        removeACSite();
        exitToTop();

        // Check autocompletion
        checkACOff(site.substring(0, 3));
     }

    // TODO: Reenable after fixing AndroidX migration issues
    @Ignore
    @Test
    // add custom autocompletion site, but disable autocomplete
    public void DisableCCwithSiteTest() throws UiObjectNotFoundException {
        OpenCustomCompleteDialog();
        addAutoComplete(site);
        Espresso.pressBack();
        toggleCustomAC();  // Custom autocomplete is now off
        Espresso.pressBack();
        Espresso.pressBack();
        Espresso.pressBack();
        checkACOff(site.substring(0, 3));

        // Now enable autocomplete
        OpenCustomCompleteDialog();
        toggleCustomAC();  // Custom autocomplete is now on
        Espresso.pressBack();
        Espresso.pressBack();
        Espresso.pressBack();

        // Check autocompletion
        checkACOn(site);
        // Cleanup
        OpenCustomCompleteDialog();
        removeACSite();
    }

    // TODO: Reenable after fixing AndroidX migration issues
    @Ignore
    @Test
    public void DuplicateACSiteTest() {
        OpenCustomCompleteDialog();

        // Enable URL autocomplete, and tap Custom URL add button
        addAutoComplete(site);
        exitToTop();

        // Try to add same site again
        OpenCustomCompleteDialog();
        addAutoComplete(site, false);

        // Espresso cannot detect the "Already exists" popup.  Instead, check that it's
        // still in the same page
        onView(withId(R.id.domainView))
                .check(matches(isDisplayed()));
        onView(withId(R.id.save))
                .check(matches(isDisplayed()));
        Espresso.pressBack();
        Espresso.pressBack();

        // Cleanup
        removeACSite();
        Espresso.pressBack();
    }

    // exit to the main view from custom autocomplete dialog
    private void exitToTop() {
        Espresso.pressBack();
        Espresso.pressBack();
        Espresso.pressBack();
        Espresso.pressBack();
    }

    private void toggleTopsiteAC() {
        onView(withText("For Top sites"))
                .perform(click());
    }

    private void toggleCustomAC() {
        onView(withText("For Sites You Add"))
                .perform(click());
    }

    private void OpenCustomCompleteDialog() {
        mDevice.waitForIdle();
        openSettings();

        onView(withText("Search"))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withText("URL Autocomplete"))
                .check(matches(isDisplayed()))
                .perform(click());
        mDevice.waitForIdle();
    }

    // Check autocompletion is turned off
    private void checkACOff(String url) throws UiObjectNotFoundException {
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime);
        TestHelper.inlineAutocompleteEditText.setText(url);
        TestHelper.hint.waitForExists(waitingTime);
        assertTrue (TestHelper.inlineAutocompleteEditText.getText().equals(url));
        TestHelper.cleartextField.click();
    }

    // Check autocompletion is turned on
    private void checkACOn(String url) throws UiObjectNotFoundException {
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime);
        TestHelper.inlineAutocompleteEditText.setText(url.substring(0, 1));
        TestHelper.hint.waitForExists(waitingTime);
        assertTrue (TestHelper.inlineAutocompleteEditText.getText().equals(url));
        TestHelper.cleartextField.click();
    }

    private void removeACSite() {
        AutoCompleteDialog.perform(actionOnItemAtPosition(4, click()));

        mDevice.waitForIdle();
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getContext());
        mDevice.waitForIdle();   // wait until dialog fully appears
        onView(withText("Remove"))
                .perform(click());
        onView(withId(R.id.checkbox))
                .perform((click()));
        onView(withId(R.id.remove))
                .perform((click()));
    }

    private void addAutoComplete(String sitename, boolean... checkSuccess) {

        AutoCompleteDialog.perform(actionOnItemAtPosition(4, click()));

        mDevice.waitForIdle();

        onView(withText("+ Add custom URL"))
                .perform(click());
        onView(withId(R.id.domainView))
                .check(matches(isDisplayed()));

        mDevice.waitForIdle();


        onView(withId(R.id.domainView))
                .perform(replaceText(sitename), closeSoftKeyboard());
        onView(withId(R.id.save))
                .perform(click());

        if (checkSuccess == null) { // Verify new entry appears in the list
            onView(withText("Custom URLs"))
                    .check(matches(isDisplayed()));

            onView(allOf(withText(sitename), withId(R.id.domainView)))
                    .check(matches(isDisplayed()));
            mDevice.waitForIdle();
        }
    }
}
