/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParentIndex
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.UiSelector
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertTrue
import org.mozilla.focus.R
import org.mozilla.focus.helpers.SessionLoadedIdlingResource
import org.mozilla.focus.helpers.TestHelper.mDevice
import org.mozilla.focus.helpers.TestHelper.packageName
import org.mozilla.focus.helpers.TestHelper.webPageLoadwaitingTime

class BrowserRobot {

    val progressBar =
        mDevice.findObject(
            UiSelector().resourceId("$packageName:id/progress")
        )

    fun verifyBrowserView() =
        assertTrue(mDevice.findObject(UiSelector().resourceId("$packageName:id/webview"))
            .waitForExists(webPageLoadwaitingTime)
        )

    fun verifyPageContent(expectedText: String) {
        val sessionLoadedIdlingResource = SessionLoadedIdlingResource()

        mDevice.findObject(UiSelector().resourceId("$packageName:id/webview"))
            .waitForExists(webPageLoadwaitingTime)

        runWithIdleRes(sessionLoadedIdlingResource) {
            assertTrue(
                mDevice.findObject(UiSelector().textContains(expectedText))
                    .waitForExists(webPageLoadwaitingTime)
            )
        }
    }

    fun verifyPageURL(expectedText: String) {
        val sessionLoadedIdlingResource = SessionLoadedIdlingResource()

        browserURLbar.waitForExists(webPageLoadwaitingTime)

        mDevice.findObject(UiSelector().resourceId("$packageName:id/webview"))
            .waitForExists(webPageLoadwaitingTime)

        runWithIdleRes(sessionLoadedIdlingResource) {
            assertTrue(
                browserURLbar.text.contains(expectedText, ignoreCase = true)
            )
        }
    }

    fun verifyFloatingEraseButton(): ViewInteraction = floatingEraseButton.check(matches(isDisplayed()))

    fun longPressLink(linkText: String) {
        val link = mDevice.findObject(UiSelector().text(linkText))
        link.waitForExists(webPageLoadwaitingTime)
        link.longClick()
    }

    fun openLinkInNewTab() {
        onView(withText(R.string.mozac_feature_contextmenu_open_link_in_private_tab))
            .perform(click())
    }

    fun verifyNumberOfTabsOpened(tabsCount: Int) {
        tabsCounter.check(matches(withContentDescription("Tabs open: $tabsCount")))
    }

    fun verifyTabsOrder(vararg tabTitle: String) {
        for (tab in tabTitle.indices) {
            onView(withId(R.id.sessions)).check(
                matches(
                    hasDescendant(
                        allOf(
                            withText(tabTitle[tab]),
                            withParentIndex(tab)
                        )
                    )
                )
            )
        }
    }

    fun openTabsTray(): ViewInteraction = tabsCounter.perform(click())

    fun selectTab(tabTitle: String): ViewInteraction = onView(withText(tabTitle)).perform(click())

    fun verifyShareAppsListOpened() = assertTrue(shareAppsList.waitForExists(webPageLoadwaitingTime))

    class Transition {
        fun openSearchBar(interact: SearchRobot.() -> Unit): SearchRobot.Transition {
            browserURLbar.waitForExists(webPageLoadwaitingTime)
            browserURLbar.click()

            SearchRobot().interact()
            return SearchRobot.Transition()
        }

        fun clearBrowsingData(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            floatingEraseButton.perform(click())

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun eraseBrowsingHistoryFromTabsTray(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            tabsCounter.perform(click())
            tabsTrayEraseHistoryButton.perform(click())

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun openMainMenu(interact: ThreeDotMainMenuRobot.() -> Unit): ThreeDotMainMenuRobot.Transition {
            browserURLbar.waitForExists(webPageLoadwaitingTime)
            mainMenu
                .check(matches(isDisplayed()))
                .perform(click())

            ThreeDotMainMenuRobot().interact()
            return ThreeDotMainMenuRobot.Transition()
        }
    }
}

fun browserScreen(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
    BrowserRobot().interact()
    return BrowserRobot.Transition()
}

inline fun runWithIdleRes(ir: IdlingResource?, pendingCheck: () -> Unit) {
    try {
        IdlingRegistry.getInstance().register(ir)
        pendingCheck()
    } finally {
        IdlingRegistry.getInstance().unregister(ir)
    }
}

private val browserURLbar = mDevice.findObject(UiSelector().resourceId("$packageName:id/display_url"))

private val floatingEraseButton = onView(allOf(withId(R.id.erase), isDisplayed()))

private val tabsCounter = onView(withId(R.id.tabs))

private val tabsTrayEraseHistoryButton = onView(withText(R.string.tabs_tray_action_erase))

private val mainMenu = onView(withId(R.id.menuView))

private val shareAppsList =
    mDevice.findObject(UiSelector().resourceId("android:id/resolver_list"))
