package org.mozilla.focus.activity;


import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.GeneralSwipeAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Swipe;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.widget.FloatingActionMenu;
import org.mozilla.focus.widget.FloatingExpandButton;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {
    ViewInteraction floatingExpandButton;
    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void setup(){
        //in case focus is being opeend for the first time (we need to skip user instructions)
        try{
            ViewInteraction appCompatButton = onView(
                    allOf(withId(R.id.skip), withText("Skip"),
                            childAtPosition(
                                    allOf(withId(R.id.background),
                                            childAtPosition(
                                                    withId(R.id.container),
                                                    1)),
                                    0),
                            isDisplayed()));
            appCompatButton.perform(click());
        } catch (Exception e) {
            //e.printStackTrace();
        } finally {
            //type in google.com
            ViewInteraction inlineAutocompleteEditText2 = onView(
                    allOf(withId(R.id.urlView),
                            childAtPosition(
                                    childAtPosition(
                                            withId(R.id.urlInputContainerView),
                                            1),
                                    0),
                            isDisplayed()));
            inlineAutocompleteEditText2.perform(replaceText("google.com"), closeSoftKeyboard());

            //press enter to search
            ViewInteraction inlineAutocompleteEditText3 = onView(
                    allOf(withId(R.id.urlView), withText("google.com"),
                            childAtPosition(
                                    childAtPosition(
                                            withId(R.id.urlInputContainerView),
                                            1),
                                    0),
                            isDisplayed()));
            inlineAutocompleteEditText3.perform(pressImeActionButton());

            //get floating expand button object
            floatingExpandButton = onView(
                    allOf(withId(R.id.erase),
                            childAtPosition(
                                    allOf(withId(R.id.main_content),
                                            childAtPosition(
                                                    withId(R.id.browser_container),
                                                    1)),
                                    4),
                            isDisplayed()));
            floatingExpandButton.perform(swipe());
        }
    }

    @Test
    public void attractToClosestSideTest() {
        floatingExpandButton.check(new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noViewFoundException) {
                FloatingExpandButton button = (FloatingExpandButton)view;
                FloatingActionMenu menu = button.getActionMenu();
                int rangeHeight = button.getRangeHeight();
                int rangeWidth  = button.getRangeWidth();

                //attract right
                button.attractToClosestSide(rangeWidth/2,rangeHeight/2);
                Assert.assertEquals(90, menu.getStartAngle());
                Assert.assertEquals(180, menu.getEndAngle());
                button.attractToClosestSide(rangeWidth/2,rangeHeight/2-1);
                Assert.assertEquals(90, menu.getStartAngle());
                Assert.assertEquals(180, menu.getEndAngle());
                button.attractToClosestSide(rangeWidth/2+1,rangeHeight/2);
                Assert.assertEquals(90, menu.getStartAngle());
                Assert.assertEquals(180, menu.getEndAngle());
                button.attractToClosestSide(rangeWidth/2+1,rangeHeight/2-1);
                Assert.assertEquals(90, menu.getStartAngle());
                Assert.assertEquals(180, menu.getEndAngle());
                button.attractToClosestSide(rangeWidth/2+1,rangeHeight/2+1);
                Assert.assertEquals(-180, menu.getStartAngle());
                Assert.assertEquals(-90, menu.getEndAngle());
                button.attractToClosestSide(rangeWidth/2,rangeHeight/2+1);
                Assert.assertEquals(-180, menu.getStartAngle());
                Assert.assertEquals(-90, menu.getEndAngle());

                //attract left
                button.attractToClosestSide(rangeWidth/2-1,rangeHeight/2);
                Assert.assertEquals(90, menu.getStartAngle());
                Assert.assertEquals(0, menu.getEndAngle());
                button.attractToClosestSide(rangeWidth/2-1,rangeHeight/2-1);
                Assert.assertEquals(90, menu.getStartAngle());
                Assert.assertEquals(0, menu.getEndAngle());
                button.attractToClosestSide(rangeWidth/2-1,rangeHeight/2+1);
                Assert.assertEquals(0, menu.getStartAngle());
                Assert.assertEquals(-90, menu.getEndAngle());

            }

        });

        delay(10000);

    }

    @Test
    public void setDragLocationTest() {
        floatingExpandButton.check(new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noViewFoundException) {
                FloatingExpandButton button = (FloatingExpandButton)view;
                FloatingActionMenu menu = button.getActionMenu();
                int rangeHeight = button.getRangeHeight();
                int rangeWidth  = button.getRangeWidth();
                int EDGEDIS = button.getEdgedis();

                button.setDragLocation(500, 500);
                Assert.assertEquals(500, button.getX(), 0);
                Assert.assertEquals(500, button.getY(), 0);
                button.setDragLocation(-100,-100);
                Assert.assertEquals(0, button.getX(), 0);
                Assert.assertEquals(0, button.getY(),0);
                button.setDragLocation(0,0);
                Assert.assertEquals(0, button.getX(),0);
                Assert.assertEquals(0, button.getY(),0);
                button.setDragLocation(rangeWidth - button.getWidth() + 1,0);
                Assert.assertEquals(rangeWidth-button.getWidth(), button.getX(),0);
                Assert.assertEquals(0, button.getY(),0);
                button.setDragLocation(0,rangeHeight - EDGEDIS-button.getHeight() + 1);
                Assert.assertEquals(0, button.getX(),0);
                Assert.assertEquals(rangeHeight - button.getHeight() - EDGEDIS, button.getY(),0);
            }

        });
        delay(10000);
    }

     @After
     public void tearDown(){
         //open pie menu
         ViewInteraction floatingExpandButton = onView(
                 allOf(withId(R.id.erase), withContentDescription("Erase browsing history"),
                         childAtPosition(
                                 allOf(withId(R.id.main_content),
                                         childAtPosition(
                                                 withId(R.id.browser_container),
                                                 1)),
                                 4),
                         isDisplayed()));
         floatingExpandButton.perform(click());

         //erase browsing history, to go back to main screen
         ViewInteraction subActionButton = onView(
                 allOf(withId(R.id.erase),
                         childAtPosition(
                                 allOf(withId(android.R.id.content),
                                         childAtPosition(
                                                 withId(R.id.action_bar_root),
                                                 1)),
                                 1),
                         isDisplayed()));
         subActionButton.perform(click());


     }
    public static ViewAction swipe() {
        return new GeneralSwipeAction(Swipe.FAST, GeneralLocation.BOTTOM_RIGHT,
                GeneralLocation.TOP_CENTER, Press.FINGER);
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }

    private static void delay(int time){
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
