package org.mozilla.focus.activity;


import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import android.webkit.WebView;
import android.widget.EditText;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
public class FBTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void FBTest() throws UiObjectNotFoundException {
        final UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        final int timeOut = 1000 * 20;


        ViewInteraction appCompatTextView = onView(
                allOf(withId(R.id.url), withText("Search or enter address"), isDisplayed()));
        appCompatTextView.perform(click());

        ViewInteraction inlineAutocompleteEditText = onView(
                allOf(withId(R.id.url_edit), isDisplayed()));
        inlineAutocompleteEditText.perform(replaceText("www.facebook.com"), closeSoftKeyboard());

        ViewInteraction inlineAutocompleteEditText2 = onView(
                allOf(withId(R.id.url_edit), withText("www.facebook.com")));
        inlineAutocompleteEditText2.perform(pressImeActionButton());

        mDevice.wait(Until.findObject(By.clazz(WebView.class)), timeOut);

        // Set Login
        UiObject emailInput = mDevice.findObject(new UiSelector()
                .instance(0)
                .className(EditText.class));

        emailInput.waitForExists(timeOut);
        emailInput.setText("android_wppcmrq_facebook@tfbnw.net");

        // Set Password
        UiObject passwordInput = mDevice.findObject(new UiSelector()
                .instance(1)
                .className(EditText.class));

        passwordInput.waitForExists(timeOut);
        passwordInput.setText("testPassword");

        // Confirm Button Click
        UiObject buttonLogin = mDevice.findObject(new UiSelector()
                //.instance(0)
                //.className(Button.class));
                .description("Log In "));
        buttonLogin.waitForExists(timeOut);
        /*
        buttonLogin.clickAndWaitForNewWindow();
        */
    }

}
