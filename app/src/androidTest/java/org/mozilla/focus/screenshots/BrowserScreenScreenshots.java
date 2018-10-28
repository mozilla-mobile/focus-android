/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.screenshots;

import android.os.SystemClock;
import android.support.test.espresso.ViewInteraction;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.helpers.TestHelper;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasFocus;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mozilla.focus.helpers.EspressoHelper.childAtPosition;
import static org.mozilla.focus.helpers.EspressoHelper.openSettings;

@RunWith(AndroidJUnit4.class)
public class BrowserScreenScreenshots extends ScreenshotTest {


    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    private MockWebServer webServer;

    @Before
    public void setUpWebServer() throws IOException {
        webServer = new MockWebServer();

        // Test page
        webServer.enqueue(new MockResponse()
                .setBody(TestHelper.readTestAsset("image_test.html")));
        webServer.enqueue(new MockResponse()
                .setBody(TestHelper.readTestAsset("rabbit.jpg")));
        webServer.enqueue(new MockResponse()
                .setBody(TestHelper.readTestAsset("download.jpg")));
        // Download
        webServer.enqueue(new MockResponse()
                .setBody(TestHelper.readTestAsset("download.jpg")));
    }

    @After
    public void tearDownWebServer() {
        try {
            webServer.close();
            webServer.shutdown();
        } catch (IOException e) {
            throw new AssertionError("Could not stop web server", e);
        }
    }

    @Test
    public void takeScreenshotsOfBrowsingScreen() throws Exception {
        SystemClock.sleep(5000);
        takeScreenshotsOfBrowsingView();
        takeScreenshotsOfMenu();
        takeScreenshotsOfOpenWithAndShare();
        takeAddToHomeScreenScreenshot();
        takeScreenshotofInsecureCon();
        takeScreenshotOfFindDialog();
        takeScreenshotOfTabsTrayAndErase();
        takeScreenshotofSecureCon();
    }

    private void takeScreenshotsOfBrowsingView() {
        onView(withId(R.id.urlView))
                .check(matches(isDisplayed()));

        /* Autocomplete View */
        onView(withId(R.id.urlView))
                .check(matches(isDisplayed()))
                .check(matches(hasFocus()))
                .perform(click(), replaceText("mozilla"));

        assertTrue(TestHelper.hint.waitForExists(waitingTime));

        onView(withId(R.id.urlView))
                .check(matches(isDisplayed()))
                .check(matches(hasFocus()))
                .perform(click(), replaceText(webServer.url("/").toString()));

        Screengrab.screenshot("Suggestion_accept_dialog");

        // click yes, then go into search dialog and change to twitter
        onView(withId(R.id.enable_search_suggestions_button))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withId(R.id.clearView))
                .check(matches(isDisplayed()))
                .perform(click());

        openSettings();
        onView(withText(R.string.preference_category_search))
                .perform(click());
        onView(withText(R.string.preference_search_engine_default))
                .perform(click());
        onView(withText(R.string.preference_search_installed_search_engines))
                .check(matches(isDisplayed()));

        onView(allOf(childAtPosition(
                withId(R.id.search_engine_group), 3),
                isDisplayed()))
                .perform(click());

        device.pressBack();
        device.pressBack();
        device.pressBack();

        onView(withId(R.id.urlView))
                .check(matches(isDisplayed()))
                .check(matches(hasFocus()))
                .perform(click(), replaceText(webServer.url("/").toString()));

        ViewInteraction dismissbutton = onView(withId(R.id.dismiss_no_suggestions_message));

        dismissbutton.check(matches(isDisplayed()));

        Screengrab.screenshot("Suggestion_unavailable_dialog");

        dismissbutton.perform(click());

        onView(withId(R.id.urlView))
                .check(matches(isDisplayed()))
                .check(matches(hasFocus()))
                .perform(pressImeActionButton());

        device.findObject(new UiSelector()
                .resourceId(TestHelper.getAppName() + ":id/webview")
                .enabled(true))
                .waitForExists(waitingTime);

        onView(withId(R.id.display_url))
                .check(matches(isDisplayed()))
                .check(matches(withText(containsString(webServer.getHostName()))));
    }

    private void takeScreenshotsOfMenu() {
        TestHelper.menuButton.perform(click());
        Screengrab.screenshot("BrowserViewMenu");
    }

    private void takeScreenshotsOfOpenWithAndShare() throws Exception {
        /* Open_With View */
        UiObject openWithBtn = device.findObject(new UiSelector()
                .resourceId(TestHelper.getAppName() + ":id/open_select_browser")
                .enabled(true));
        assertTrue(openWithBtn.waitForExists(waitingTime));
        openWithBtn.click();
        UiObject shareList = device.findObject(new UiSelector()
                .resourceId(TestHelper.getAppName() + ":id/apps")
                .enabled(true));
        assertTrue(shareList.waitForExists(waitingTime));
        Screengrab.screenshot("OpenWith_Dialog");

        /* Share View */
        UiObject shareBtn = device.findObject(new UiSelector()
                .resourceId(TestHelper.getAppName() + ":id/share")
                .enabled(true));
        device.pressBack();
        TestHelper.menuButton.perform(click());
        assertTrue(shareBtn.waitForExists(waitingTime));
        shareBtn.click();
        TestHelper.shareAppList.waitForExists(waitingTime);
        Screengrab.screenshot("Share_Dialog");

        device.pressBack();
    }

    private void takeAddToHomeScreenScreenshot() throws UiObjectNotFoundException {
        TestHelper.menuButton.perform(click());

        TestHelper.AddtoHSmenuItem.waitForExists(waitingTime);
        TestHelper.AddtoHSmenuItem.click();

        TestHelper.AddtoHSCancelBtn.waitForExists(waitingTime);
        Screengrab.screenshot("AddtoHSDialog");

        device.pressBack();
        device.pressBack();
        Assert.assertTrue(TestHelper.browserURLbar.waitForExists(waitingTime));
    }

    private void takeScreenshotOfTabsTrayAndErase() throws Exception {
        final UiObject mozillaImage = device.findObject(new UiSelector()
                .resourceId("download")
                .enabled(true));

        UiObject imageMenuTitle = device.findObject(new UiSelector()
                .resourceId(TestHelper.getAppName() + ":id/topPanel")
                .enabled(true));
        UiObject openNewTabTitle = device.findObject(new UiSelector()
                .resourceId(TestHelper.getAppName() + ":id/design_menu_item_text")
                .index(0)
                .enabled(true));
        UiObject multiTabBtn = device.findObject(new UiSelector()
                .resourceId(TestHelper.getAppName() + ":id/tabs")
                .enabled(true));
        UiObject eraseHistoryBtn = device.findObject(new UiSelector()
                .text(getString(R.string.tabs_tray_action_erase))
                .enabled(true));

        Assert.assertTrue(mozillaImage.waitForExists(waitingTime));
        mozillaImage.dragTo(mozillaImage, 7);
        assertTrue(imageMenuTitle.waitForExists(waitingTime));
        Assert.assertTrue(imageMenuTitle.exists());
        Screengrab.screenshot("Image_Context_Menu");

        //Open a new tab
        openNewTabTitle.click();

        assertTrue(multiTabBtn.waitForExists(waitingTime));
        multiTabBtn.click();
        assertTrue(eraseHistoryBtn.waitForExists(waitingTime));
        Screengrab.screenshot("Multi_Tab_Menu");

        eraseHistoryBtn.click();

        device.wait(Until.findObject(By.res(TestHelper.getAppName(), "snackbar_text")), waitingTime);

        Screengrab.screenshot("YourBrowsingHistoryHasBeenErased");
    }

    private void takeScreenshotOfFindDialog() throws Exception {
        UiObject findinpageMenuItem = device.findObject(new UiSelector()
                .resourceId(TestHelper.getAppName() + ":id/find_in_page")
                .enabled(true));
        UiObject findinpageCloseBtn = device.findObject(new UiSelector()
                .resourceId(TestHelper.getAppName() + ":id/close_find_in_page")
                .enabled(true));

        TestHelper.menuButton.perform(click());
        findinpageMenuItem.waitForExists(waitingTime);
        findinpageMenuItem.click();

        findinpageCloseBtn.waitForExists(waitingTime);
        Screengrab.screenshot("Find_In_Page_Dialog");
        findinpageCloseBtn.click();
    }

    private void takeScreenshotofInsecureCon() throws Exception {

        TestHelper.securityInfoIcon.click();
        TestHelper.identityState.waitForExists(waitingTime);
        Screengrab.screenshot("insecure_connection");
        device.pressBack();
    }

    // This test requires external internet connection
    private void takeScreenshotofSecureCon() throws Exception {

        // take the security info of google.com for https connection
        onView(withId(R.id.urlView))
                .check(matches(isDisplayed()))
                .check(matches(hasFocus()))
                .perform(click(), replaceText("www.google.com"), pressImeActionButton());
        TestHelper.waitForWebContent();
        TestHelper.progressBar.waitUntilGone(waitingTime);
        TestHelper.securityInfoIcon.click();
        TestHelper.identityState.waitForExists(waitingTime);
        Screengrab.screenshot("secure_connection");
        device.pressBack();
    }
}
