/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.screenshots;

import android.content.Context;
import android.os.SystemClock;

import androidx.preference.PreferenceManager;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.Espresso;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;

import java.util.Collections;

import mozilla.components.browser.domains.CustomDomains;
import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withResourceName;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mozilla.focus.fragment.FirstrunFragment.FIRSTRUN_PREF;
import static org.mozilla.focus.helpers.EspressoHelper.assertToolbarMatchesText;
import static org.mozilla.focus.helpers.EspressoHelper.openSettings;

@RunWith(AndroidJUnit4.class)
public class SettingsScreenshots extends ScreenshotTest {

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
                Collections.emptyList());

        final Context appContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(appContext)
                .edit()
                .putBoolean(FIRSTRUN_PREF, true)
                .apply();
    }

    @Test
    public void takeScreenShotsOfSettings() throws Exception {
        SystemClock.sleep(5000);
        openSettings();

        Screengrab.screenshot("Settings_View_Top");

        /* General Settings */
        onView(withText(R.string.preference_category_general))
                .perform(click());

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
        Screengrab.screenshot("General_Submenu");
        Espresso.pressBack();

        /* Search Engine List */
        onView(withText(R.string.preference_category_search))
                .perform(click());
        Screengrab.screenshot("Search_Submenu");
        onView(allOf(withText(R.string.preference_search_engine_label),
                withResourceName("title")))
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
        onView(withText(addEngineLabel))
                .check(matches(isEnabled()))
                .perform(click());
        onView(withId(R.id.edit_engine_name))
                .check(matches(isEnabled()));
        Screengrab.screenshot("SearchEngine_Add_Search_Engine");
        onView(withId(R.id.menu_save_search_engine))
                .check(matches(isEnabled()))
                .perform(click());
        Screengrab.screenshot("SearchEngine_Add_Search_Engine_Warning");
        onView(withClassName(containsString("ImageButton")))
                .check(matches(isEnabled()))
                .perform(click());

        device.waitForIdle();
        Espresso.pressBack();
        device.waitForIdle();

        /* Tap autocomplete menu */
        /*  TODO: Reenable after fixing AndroidX migration issues
        onView(
                allOf(withId(R.id.recycler_view),
                        childAtPosition(
                                withId(android.R.id.list_container),
                                0)))
            .perform(actionOnItemAtPosition(2, click()));

        onView(withText(getString(R.string.preference_autocomplete_subitem_manage_sites)))
                .check(matches(isDisplayed()));
        Screengrab.screenshot("Autocomplete_Menu_Item");
        */

        /* Add custom URL */
        /* TODO: Reenable after fixing AndroidX migration issues
        onView(childAtPosition(withId(R.id.recycler_view), 4)).perform(click());

        //        onView(childAtPosition(withId(R.id.recycler_view), 0)).perform(actionOnItemAtPosition(4, click()));

        final String addCustomURLAction = getString(R.string.preference_autocomplete_action_add);
        onView(withText(addCustomURLAction))
                .check(matches(isDisplayed()));
        Screengrab.screenshot("Autocomplete_Custom_URL_List");

        onView(withText(addCustomURLAction))
                .perform(click());
        onView(withId(R.id.domainView))
                .check(matches(isDisplayed()));
        Screengrab.screenshot("Autocomplete_Add_Custom_URL_Dialog");
        onView(withId(R.id.save))
                .perform(click());
       Screengrab.screenshot("Autocomplete_Add_Custom_URL_Error_Popup");

        onView(withId(R.id.domainView))
                .perform(replaceText("screenshot.com"), closeSoftKeyboard());
        onView(withId(R.id.save))
                .perform(click());
        SystemClock.sleep(500);
        Screengrab.screenshot("Autocomplete_Add_Custom_URL_Saved_Popup");
        onView(withText(addCustomURLAction))
                .check(matches(isDisplayed()));
        */

        /* Remove menu */
        /* TODO: Reenable after fixing AndroidX migration issues
        final String removeMenu = getString(R.string.preference_autocomplete_menu_remove);
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getContext());
        device.waitForIdle();   // wait until dialog fully appears
        onView(withText(removeMenu))
                .check(matches(isDisplayed()));
        Screengrab.screenshot("Autocomplete_Custom_URL_Remove_Menu_Item");
        onView(withText(removeMenu))
                .perform(click());
        device.waitForIdle();   // wait until dialog fully disappears
        */
        /* Remove dialog */
        /* TODO: Reenable after fixing AndroidX migration issues
        onView(withText(getString(R.string.preference_autocomplete_title_remove)))
                .check(matches(isDisplayed()));
        Screengrab.screenshot("Autocomplete_Custom_URL_Remove_Dialog");
        Espresso.pressBack();
        onView(withText(addCustomURLAction))
                .check(matches(isDisplayed()));
        Espresso.pressBack();
        Espresso.pressBack();
        Espresso.pressBack();
        */

        // "Mozilla" submenu
        onView(withText(R.string.preference_category_mozilla))
                .perform(click());
        Screengrab.screenshot("Mozilla_Submenu");

        // "About" screen
        final String aboutLabel = getString(R.string.preference_about, getString(R.string.app_name));

        onView(withText(aboutLabel))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withId(R.id.infofragment))
                .check(matches(isDisplayed()));
        Screengrab.screenshot("About_Page");

        // leave about page, tap menu and go to settings again
        device.pressBack();

        // "Your rights" screen
        final String yourRightsLabel = getString(R.string.menu_rights);

        onView(withText(yourRightsLabel))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withId(R.id.infofragment))
                .check(matches(isDisplayed()));

        Screengrab.screenshot("YourRights_Page");
        device.pressBack();
        device.pressBack();

        // "Privacy & Security" submenu
        onView(withText(R.string.preference_privacy_and_security_header))
                .perform(click());
        Screengrab.screenshot("Privacy_Security_Submenu_top");
        UiScrollable settingsView = new UiScrollable(new UiSelector().scrollable(true));
        if (settingsView.exists()) {        // On tablet, this will not be found
            settingsView.scrollToEnd(5);
            Screengrab.screenshot("Privacy_Security_Submenu_bottom");
        }

        // Block Cookies dialog
        onView(withResourceName("recycler_view"))
                .perform(scrollToPosition(5));
        onView(withText(R.string.preference_privacy_category_cookies))
                .perform(click());
        CancelBtn.waitForExists(waitingTime);
        Screengrab.screenshot("Block_cookies_dialog");
        CancelBtn.click();
        onView(withText(R.string.preference_privacy_and_security_header))
                .check(matches(isDisplayed()));
        device.pressBack();

        // Advanced
        onView(withText(R.string.preference_category_advanced))
                .perform(click());
        onView(withText(R.string.preference_remote_debugging))
                .check(matches(isDisplayed()));
        Screengrab.screenshot("Advanced_Page");
    }
}
