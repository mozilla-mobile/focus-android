/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.screenshots;

import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.helpers.MainActivityFirstrunTestRule;
import org.mozilla.focus.helpers.TestHelper;

import java.util.Collections;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.PreferenceMatchers.withKey;
import static android.support.test.espresso.matcher.PreferenceMatchers.withTitleText;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import mozilla.components.domains.CustomDomains;
import static org.mozilla.focus.helpers.EspressoHelper.assertToolbarMatchesText;
import static org.mozilla.focus.helpers.EspressoHelper.openSettings;

@RunWith(AndroidJUnit4.class)
public class SettingsScreenshots extends ScreenshotTest {
    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new MainActivityFirstrunTestRule(false);

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Before
    public void clearSettings() {
        PreferenceManager.getDefaultSharedPreferences(
                InstrumentationRegistry.getInstrumentation().getTargetContext())
                .edit()
                .clear()
                .apply();
        CustomDomains.INSTANCE.save(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                Collections.<String>emptyList());
    }

    @Test
    public void takeScreenShotsOfSettings() throws Exception {
        openSettings();

        Screengrab.screenshot("Settings_View_Top");

        /* Language List (First page only */
        onView(withText(R.string.preference_language))
                .perform(click());

        // Cancel button is not translated in some locales, and there are no R.id defined
        // That can be checked in the language list dialog
        UiObject CancelBtn =  device.findObject(new UiSelector()
                .resourceId("android:id/button2")
                .enabled(true));
        CancelBtn.waitForExists(waitingTime);

        Screengrab.screenshot("Language_Selection");
        CancelBtn.click();
        onView(withText(R.string.preference_language))
                .check(matches(isDisplayed()));

        /* Search Engine List */
        onView(withText(R.string.preference_search_engine_default))
                .perform(click());
        onView(withText(R.string.preference_search_installed_search_engines))
                .check(matches(isDisplayed()));

        Screengrab.screenshot("SearchEngine_Selection");

        /* Remove Search Engine page */
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getContext());
        device.waitForIdle();       // wait until dialog fully appears
        onView(withText(R.string.preference_search_remove))
                .check(matches(isDisplayed()));
        Screengrab.screenshot("SearchEngine_Search_Engine_Menu");
        // Menu items don't have ids, so we have to match by text
        onView(withText(R.string.preference_search_remove))
                .perform(click());
        device.waitForIdle();       // wait until dialog fully disappears
        assertToolbarMatchesText(R.string.preference_search_remove_title);
        Screengrab.screenshot("SearchEngine_Remove_Search_Engines");
        Espresso.pressBack();

        /* Manual Search Engine page */
        final String addEngineLabel = getString(R.string.preference_search_add2);
        onData(withTitleText(addEngineLabel))
                .perform(click());
        onView(withId(R.id.edit_engine_name))
                .check(matches(isEnabled()));

        Screengrab.screenshot("SearchEngine_Add_Search_Engine");
        Espresso.pressBack();
        Espresso.pressBack();
        Espresso.pressBack();

        /* Tap autocomplete menu */
        final String urlAutocompletemenu = getString(R.string.preference_subitem_autocomplete);
        onData(withTitleText(urlAutocompletemenu))
                .perform(click());
        onView(withText(getString(R.string.preference_autocomplete_custom_summary)))
                .check(matches(isDisplayed()));
        Screengrab.screenshot("Autocomplete_Menu_Item");

        /* Enable Autocomplete, and enter Custom URLs */
        onView(withText(getString(R.string.preference_autocomplete_custom_summary)))
                .perform(click());
        /* Add custom URL */
        final String key = InstrumentationRegistry
                .getTargetContext()
                .getString(R.string.pref_key_screen_custom_domains);
        onData(withKey(key))
                .perform(click());

        final String addCustomURLAction = getString(R.string.preference_autocomplete_action_add);
        onView(withText(addCustomURLAction))
                .check(matches(isDisplayed()));
        Screengrab.screenshot("Autocomplete_Custom_URL_List");

        onView(withText(addCustomURLAction))
                .perform(click());
        onView(withId(R.id.domainView))
                .check(matches(isDisplayed()));
        Screengrab.screenshot("Autocomplete_Add_Custom_URL_Dialog");
        onView(withId(R.id.domainView))
                .perform(typeText("screenshot.com"), closeSoftKeyboard());
        onView(withId(R.id.save))
                .perform(click());
        onView(withText(addCustomURLAction))
                .check(matches(isDisplayed()));

        /* Remove menu */
        final String removeMenu = getString(R.string.preference_autocomplete_menu_remove);
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getContext());
        device.waitForIdle();   // wait until dialog fully appears
        onView(withText(removeMenu))
                .check(matches(isDisplayed()));
        Screengrab.screenshot("Autocomplete_Custom_URL_Remove_Menu_Item");
        onView(withText(removeMenu))
                .perform(click());
        device.waitForIdle();   // wait until dialog fully disappears
        /* Remove dialog */
        onView(withText(getString(R.string.preference_autocomplete_title_remove)))
                .check(matches(isDisplayed()));
        Screengrab.screenshot("Autocomplete_Custom_URL_Remove_Dialog");
        Espresso.pressBack();
        onView(withText(addCustomURLAction))
                .check(matches(isDisplayed()));
        Espresso.pressBack();
        onView(withText(urlAutocompletemenu))
                .check(matches(isDisplayed()));
        Espresso.pressBack();

        /* scroll down */
        assertTrue(TestHelper.settingsHeading.waitForExists(waitingTime));
        UiScrollable settingsView = new UiScrollable(new UiSelector().scrollable(true));
        settingsView.scrollToEnd(4);
        Screengrab.screenshot("Settings_View_Bottom");

        // "About" screen
        final String aboutLabel = getString(R.string.preference_about, getString(R.string.app_name));

        onData(withTitleText(aboutLabel))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(allOf(withClassName(endsWith("TextView")), withParent(withId(R.id.toolbar))))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.menu_about)));

        onWebView()
                .withElement(findElement(Locator.ID, "wordmark"))
                .perform(webClick());

        Screengrab.screenshot("About_Page");

        device.pressBack(); // Leave about page

        // "Your rights" screen

        final String yourRightsLabel = getString(R.string.menu_rights);

        onData(withTitleText(yourRightsLabel))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(allOf(withClassName(endsWith("TextView")), withParent(withId(R.id.toolbar))))
                .check(matches(isDisplayed()))
                .check(matches(withText(yourRightsLabel)));

        onWebView()
                .withElement(findElement(Locator.ID, "first"))
                .perform(webClick());

        Screengrab.screenshot("YourRights_Page");
    }
}
