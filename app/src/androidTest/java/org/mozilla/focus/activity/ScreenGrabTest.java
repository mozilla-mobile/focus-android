package org.mozilla.focus.activity;

import android.content.Context;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import android.text.format.DateUtils;

import junit.framework.Assert;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static android.support.test.espresso.action.ViewActions.click;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static junit.framework.Assert.assertTrue;
import static org.mozilla.focus.activity.TestHelper.browserURLbar;
import static org.mozilla.focus.fragment.FirstrunFragment.FIRSTRUN_PREF;

@RunWith(AndroidJUnit4.class)
public class ScreenGrabTest {

    private MockWebServer webServer;
    private static final String TEST_PATH = "/";

    private enum ErrorTypes {
        ERROR_UNKNOWN (-1),
        ERROR_HOST_LOOKUP (-2),
        ERROR_CONNECT (-6),
        ERROR_TIMEOUT (-8),
        ERROR_REDIRECT_LOOP (-9),
        ERROR_UNSUPPORTED_SCHEME (-10),
        ERROR_FAILED_SSL_HANDSHAKE (-11),
        ERROR_BAD_URL (-12),
        ERROR_TOO_MANY_REQUESTS (-15);
        private int value;

        ErrorTypes(int value) {
            this.value = value;
        }
    }

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule
            = new ActivityTestRule<MainActivity>(MainActivity.class) {

        @Override
        protected void beforeActivityLaunched() {
            super.beforeActivityLaunched();

            Context appContext = InstrumentationRegistry.getInstrumentation()
                    .getTargetContext()
                    .getApplicationContext();
            Resources resources = appContext.getResources();

            PreferenceManager.getDefaultSharedPreferences(appContext)
                    .edit()
                    .putBoolean(FIRSTRUN_PREF, false)
                    .putBoolean(resources.getString(R.string.pref_key_secure), false)
                    .apply();

            webServer = new MockWebServer();

            try {
                webServer.enqueue(new MockResponse()
                        .setBody(TestHelper.readTestAsset("image_test.html"))
                        .addHeader("Set-Cookie", "sphere=battery; Expires=Wed, 21 Oct 2035 07:28:00 GMT;"));
                webServer.enqueue(new MockResponse()
                        .setBody(TestHelper.readTestAsset("rabbit.jpg")));
                webServer.enqueue(new MockResponse()
                        .setBody(TestHelper.readTestAsset("download.jpg")));

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

    public static void swipeDownNotificationBar (UiDevice deviceInstance) {
        int dHeight = deviceInstance.getDisplayHeight();
        int dWidth = deviceInstance.getDisplayWidth();
        int xScrollPosition = dWidth/2;
        int yScrollStop = dHeight/4 * 3;
        deviceInstance.swipe(
                xScrollPosition,
                yScrollStop,
                xScrollPosition,
                0,
                20
        );
    }

    @Test
    public void screenGrabTest() throws InterruptedException, UiObjectNotFoundException {
        UiDevice mDevice;
        final long waitingTime = DateUtils.SECOND_IN_MILLIS * 2;
        final String marketURL = "market://details?id=org.mozilla.firefox&referrer=utm_source%3D" +
                "mozilla%26utm_medium%3DReferral%26utm_campaign%3Dmozilla-org";

        // Initialize UiDevice instance
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());
        Screengrab.screenshot("IGNORE");

        /* Wait for app to load, and take the First View screenshot */
        TestHelper.firstSlide.waitForExists(waitingTime);
        Screengrab.screenshot("Onboarding_1_View");
        TestHelper.nextBtn.click();
        TestHelper.secondSlide.waitForExists(waitingTime);
        Screengrab.screenshot("Onboarding_2_View");
        TestHelper.nextBtn.click();
        TestHelper.thirdSlide.waitForExists(waitingTime);
        Screengrab.screenshot("Onboarding_3_View");
        TestHelper.nextBtn.click();
        TestHelper.lastSlide.waitForExists(waitingTime);
        Screengrab.screenshot("Onboarding_last_View");
        TestHelper.finishBtn.click();

        /* Home View*/
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime);
        Screengrab.screenshot("Home_View");

        /* Main View Menu */
        TestHelper.menuButton.perform(click());

        TestHelper.RightsItem.waitForExists(waitingTime);
        Screengrab.screenshot("MainViewMenu");

        /* Your Rights Page */
        TestHelper.RightsItem.click();
        TestHelper.webView.waitForExists(waitingTime);
        Screengrab.screenshot("YourRights_Page");

        /* About Page */
        mDevice.pressBack();
        TestHelper.menuButton.perform(click());
        TestHelper.AboutItem.click();
        TestHelper.webView.waitForExists(waitingTime);
        Screengrab.screenshot("About_Page");

        /* Help Page */
        mDevice.pressBack();
        TestHelper.menuButton.perform(click());
        TestHelper.HelpItem.click();
        TestHelper.webView.waitForExists(waitingTime*5);
        Screengrab.screenshot("Help_Page");

        /* Location Bar View */
        mDevice.pressBack();
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime);
        Screengrab.screenshot("LocationBarEmptyState");

        /* Autocomplete View */
        TestHelper.inlineAutocompleteEditText.clearTextField();
        TestHelper.inlineAutocompleteEditText.setText("mozilla");
        TestHelper.hint.waitForExists(waitingTime);
        Screengrab.screenshot("SearchFor");

        /* Browser View Menu */
        mDevice.pressKeyCode(KEYCODE_ENTER);
        TestHelper.webView.waitForExists(waitingTime);
        TestHelper.menuButton.perform(click());
        Screengrab.screenshot("BrowserViewMenu");

        /* Open_With View */
        UiObject openWithBtn = mDevice.findObject(new UiSelector()
                .resourceId("org.mozilla.focus.debug:id/open_select_browser")
                .enabled(true));
        openWithBtn.waitForExists(waitingTime);
        openWithBtn.click();
        UiObject shareList = mDevice.findObject(new UiSelector()
                .resourceId("org.mozilla.focus.debug:id/apps")
                .enabled(true));
        shareList.waitForExists(waitingTime);
        Screengrab.screenshot("OpenWith_Dialog");

        /* Share View */
        UiObject shareBtn = TestHelper.mDevice.findObject(new UiSelector()
                .resourceId("org.mozilla.focus.debug:id/share")
                .enabled(true));
        mDevice.pressBack();
        TestHelper.menuButton.perform(click());
        shareBtn.waitForExists(waitingTime);
        shareBtn.click();
        TestHelper.shareAppList.waitForExists(waitingTime);
        Screengrab.screenshot("Share_Dialog");

        /* Check Add to homescreen */
        mDevice.pressBack();
        TestHelper.menuButton.perform(click());
        TestHelper.AddtoHSmenuItem.waitForExists(waitingTime);
        TestHelper.AddtoHSmenuItem.click();
        TestHelper.AddtoHSCancelBtn.waitForExists(waitingTime);
        Screengrab.screenshot("AddtoHSDialog");
        TestHelper.pressBackKey();
        TestHelper.pressBackKey();

        /* Notification bar caption */
        mDevice.openNotification();
        TestHelper.notificationBarDeleteItem.waitForExists(waitingTime);
        Screengrab.screenshot("DeleteHistory_NotificationBar");

        /* History Erase Notification */
        mDevice.pressBack();
        TestHelper.floatingEraseButton.perform(click());
        mDevice.wait(Until.findObject(By.res("org.mozilla.focus.debug","snackbar_text")), waitingTime);
        Screengrab.screenshot("YourBrowingHistoryHasBeenErased");

        /* Take Settings View */
        TestHelper.menuButton.perform(click());
        TestHelper.settingsMenuItem.click();
        TestHelper.settingsHeading.waitForExists(waitingTime);
        Screengrab.screenshot("Settings_View_Top");

        /* Language List (First page only */
        UiObject LanguageSelection = TestHelper.settingsList.getChild(new UiSelector()
                .className("android.widget.LinearLayout")
                .instance(0));
        LanguageSelection.click();
        TestHelper.settingsHeading.waitUntilGone(waitingTime);

        UiObject CancelBtn =  mDevice.findObject(new UiSelector()
                .resourceId("android:id/button2")
                .enabled(true));
        Screengrab.screenshot("Language_Selection");
        CancelBtn.click();
        TestHelper.settingsHeading.waitForExists(waitingTime);

        /* Search Engine List */
        UiObject SearchEngineSelection = TestHelper.settingsList.getChild(new UiSelector()
                .className("android.widget.LinearLayout")
                .instance(1));
        SearchEngineSelection.click();
        TestHelper.settingsHeading.waitUntilGone(waitingTime);
        UiObject SearchEngineList = new UiScrollable(new UiSelector()
                .resourceId("android:id/select_dialog_listview").enabled(true));
        UiObject FirstSelection = SearchEngineList.getChild(new UiSelector()
                .className("android.widget.LinearLayout")
                .instance(0));
        Screengrab.screenshot("SearchEngine_Selection");

        /* scroll down */
        FirstSelection.click();
        TestHelper.settingsHeading.waitForExists(waitingTime);
        UiScrollable settingsView = new UiScrollable(new UiSelector().scrollable(true));
        settingsView.scrollToEnd(4);
        Screengrab.screenshot("Settings_View_Bottom");

        // Go back
        mDevice.pressBack();

        /* Take image context menu screenshot */
        UiObject titleMsg = TestHelper.mDevice.findObject(new UiSelector()
                .description("focus test page")
                .enabled(true));
        UiObject mozillaImage = TestHelper.mDevice.findObject(new UiSelector()
                .description("download icon")
                .enabled(true));
        UiObject imageMenuTitle = TestHelper.mDevice.findObject(new UiSelector()
                .resourceId("org.mozilla.focus.debug:id/topPanel")
                .enabled(true));
        UiObject openNewTabTitle = TestHelper.mDevice.findObject(new UiSelector()
                .resourceId("org.mozilla.focus.debug:id/design_menu_item_text")
                .index(0)
                .enabled(true));
        UiObject multiTabBtn = TestHelper.mDevice.findObject(new UiSelector()
                .resourceId("org.mozilla.focus.debug:id/tabs")
                .enabled(true));
        UiObject eraseHistoryBtn = TestHelper.mDevice.findObject(new UiSelector()
                .className("android.widget.TextView")
                .index(2)
                .enabled(true));

        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime);
        TestHelper.inlineAutocompleteEditText.clearTextField();
        TestHelper.inlineAutocompleteEditText.setText(webServer.url(TEST_PATH).toString());
        TestHelper.pressEnterKey();
        TestHelper.webView.waitForExists(waitingTime);
        assertTrue("Website title loaded", titleMsg.exists());
        Assert.assertTrue(mozillaImage.exists());
        mozillaImage.dragTo(mozillaImage,7);
        imageMenuTitle.waitForExists(waitingTime);
        Assert.assertTrue(imageMenuTitle.exists());
        Screengrab.screenshot("Image_Context_Menu");

        //Open a new tab
        openNewTabTitle.click();
        TestHelper.webView.waitForExists(waitingTime);
        multiTabBtn.click();
        eraseHistoryBtn.waitForExists(waitingTime);
        Screengrab.screenshot("Multi_Tab_Menu");
        eraseHistoryBtn.click();

        /* Go to google play market */
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime);
        TestHelper.inlineAutocompleteEditText.setText(marketURL);
        mDevice.pressKeyCode(KEYCODE_ENTER);

        UiObject cancelBtn = mDevice.findObject(new UiSelector()
                .resourceId("android:id/button2"));
        UiObject alert = mDevice.findObject(new UiSelector()
                .resourceId("android:id/alertTitle"));

        alert.waitForExists(waitingTime);
        cancelBtn.waitForExists(waitingTime);
        Screengrab.screenshot("Redirect_Outside");
        cancelBtn.click();
        TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime);
        mDevice.pressBack();
        UiObject tryAgainBtn = mDevice.findObject(new UiSelector()
                .resourceId("errorTryAgain")
                .clickable(true));
        for (ScreenGrabTest.ErrorTypes error: ScreenGrabTest.ErrorTypes.values()) {

            TestHelper.inlineAutocompleteEditText.waitForExists(waitingTime);
            TestHelper.inlineAutocompleteEditText.setText("error:"+ error.value);
            mDevice.pressKeyCode(KEYCODE_ENTER);
            TestHelper.webView.waitForExists(waitingTime);
            tryAgainBtn.waitForExists(waitingTime);
            Screengrab.screenshot(error.name());
            browserURLbar.click();
        }
    }
}
