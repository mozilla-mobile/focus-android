/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;
import android.text.format.DateUtils;

import org.mozilla.focus.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static org.hamcrest.Matchers.allOf;

// This test visits each page and checks whether some essential elements are being displayed
public final class TestHelper {

    static UiDevice mDevice =  UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());;
    static final long waitingTime = DateUtils.SECOND_IN_MILLIS * 4;

    /********* First View Locators ***********/
    static UiObject firstViewBtn = mDevice.findObject(new UiSelector()
            .resourceId("org.mozilla.focus.debug:id/firstrun_exitbutton")
            .enabled(true));

    /********* Main View Locators ***********/
    static UiObject urlBar = mDevice.findObject(new UiSelector()
            .resourceId("org.mozilla.focus.debug:id/fake_urlbar")
            .clickable(true));
    static ViewInteraction menuButton = onView(
            allOf(withId(R.id.menu),
                    isDisplayed()));

    /********* Web View Locators ***********/
    static UiObject browserURLbar = mDevice.findObject(new UiSelector()
            .resourceId("org.mozilla.focus.debug:id/display_url")
            .clickable(true));
    static UiObject inlineAutocompleteEditText = mDevice.findObject(new UiSelector()
            .resourceId("org.mozilla.focus.debug:id/url_edit")
            .focused(true)
            .enabled(true));
    static UiObject hint = mDevice.findObject(new UiSelector()
            .resourceId("org.mozilla.focus.debug:id/search_hint")
            .clickable(true));
    static UiObject webView = mDevice.findObject(new UiSelector()
            .className("android.webkit.Webview")
            .enabled(true));
    static UiObject tryAgainBtn = mDevice.findObject(new UiSelector()
            .description("Try Again")
            .clickable(true));
    static ViewInteraction floatingEraseButton = onView(
            allOf(withId(R.id.erase), isDisplayed()));
    static UiObject notFoundMsg = mDevice.findObject(new UiSelector()
            .description("The address wasn’t understood")
            .enabled(true));
    static UiObject notFounddetailedMsg = mDevice.findObject(new UiSelector()
            .description("You might need to install other software to open this address.")
            .enabled(true));
    static UiObject browserViewSettingsMenuItem = mDevice.findObject(new UiSelector()
            .resourceId("org.mozilla.focus.debug:id/settings")
            .clickable(true));
    static UiObject erasedMsg = TestHelper.mDevice.findObject(new UiSelector()
            .text("Your browsing history has been erased.")
            .resourceId("org.mozilla.focus.debug:id/snackbar_text")
            .enabled(true));
    static UiObject lockIcon = mDevice.findObject(new UiSelector()
            .resourceId("org.mozilla.focus.debug:id/lock")
            .description("Secure connection"));


    /********* Main View Menu Item Locators ***********/
    static UiObject RightsItem = mDevice.findObject(new UiSelector()
            .className("android.widget.LinearLayout")
            .instance(2));
    static UiObject AboutItem = mDevice.findObject(new UiSelector()
            .className("android.widget.LinearLayout")
            .instance(0)
            .enabled(true));
    static UiObject HelpItem = mDevice.findObject(new UiSelector()
            .className("android.widget.LinearLayout")
            .instance(1)
            .enabled(true));
    static UiObject settingsMenuItem = mDevice.findObject(new UiSelector()
            .className("android.widget.LinearLayout")
            .instance(3));

    /********* Settings Menu Item Locators ***********/
    static UiScrollable settingsList = new UiScrollable(new UiSelector()
            .resourceId("android:id/list").scrollable(true));
    static UiObject settingsHeading = mDevice.findObject(new UiSelector()
            .resourceId("org.mozilla.focus.debug:id/toolbar")
            .enabled(true));
    static UiObject navigateUp = mDevice.findObject(new UiSelector()
            .description("Navigate up"));

    private TestHelper () throws UiObjectNotFoundException {
    }

    static void waitForIdle() {
        mDevice.waitForIdle(waitingTime);
    }
    static void pressEnterKey() {
        mDevice.pressKeyCode(KEYCODE_ENTER);
    }
    static void pressBackKey() {
        mDevice.pressBack();
    }

    static void swipeDownNotificationBar () {
        int dHeight = mDevice.getDisplayHeight();
        int dWidth = mDevice.getDisplayWidth();
        int xScrollPosition = dWidth/2;
        int yScrollStop = dHeight/4 * 3;
        mDevice.swipe(
                xScrollPosition,
                yScrollStop,
                xScrollPosition,
                0,
                20
        );
    }

}
