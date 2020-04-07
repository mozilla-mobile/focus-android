package org.mozilla.focus.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.fragment.FirstrunFragment;
import org.mozilla.focus.helpers.TestHelper;
import org.mozilla.focus.shortcut.IconGenerator;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static android.view.KeyEvent.KEYCODE_SPACE;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.mozilla.focus.fragment.FirstrunFragment.FIRSTRUN_PREF;
import static org.mozilla.focus.helpers.EspressoHelper.childAtPosition;
import static org.mozilla.focus.helpers.TestHelper.suggestion;
import static org.mozilla.focus.helpers.TestHelper.suggestionList;
import static org.mozilla.focus.helpers.TestHelper.waitingTime;
import static org.mozilla.focus.helpers.TestHelper.webPageLoadwaitingTime;

public class SearchEngineShortcutTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void searchEngineShortcutIsDisplayedTest() throws UiObjectNotFoundException {
        TestHelper.inlineAutocompleteEditText.clearTextField();
        TestHelper.inlineAutocompleteEditText.setText("@");
        onView(withId(R.id.suggestionList))
            .check(matches(isDisplayed()));
    }

    @Test
    public void iconMatchingTest() throws UiObjectNotFoundException{
        // display the shortcut list
        TestHelper.inlineAutocompleteEditText.clearTextField();
        TestHelper.inlineAutocompleteEditText.setText("@");
              /*UiScrollable searchEngineList = new UiScrollable(new UiSelector()
                  .resourceId(TestHelper.getAppName() + ":id/suggestionList").enabled(true));
              // extract the icon from the search engine into a bitmap
               UiObject googleEngine = searchEngineList.getChildByDescription(new UiSelector(), "google");*/
        // this is my attempt to get work with recyclerview directly
        RecyclerView suggestionList = mActivityTestRule.getActivity().findViewById(R.id.suggestionList);  // this is not null but is empty for some reason
        Log.d("print", ""+suggestionList.getLayoutManager().getItemCount());
        View suggestionItem = suggestionList.getLayoutManager().findViewByPosition(1);
        Context context = mActivityTestRule.getActivity().getApplicationContext();

        //bitmap function generates an icon based on the first character of the url passed in
        Bitmap googleBitmap1 = IconGenerator.generateLauncherIcon(context, "google.ca");
        //attempt to convert drawable into bitmap
        Bitmap searchIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_search);

        Log.d("print", (searchIcon == null) + "");
        Log.d("print", googleBitmap1.toString());
        // compare the icon extracted to the search icon
        assertFalse(googleBitmap1.sameAs(searchIcon));
    }
}
