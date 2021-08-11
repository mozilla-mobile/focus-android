/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.menu.browser

import android.content.Context
import androidx.core.content.ContextCompat
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.BrowserMenuHighlight
import mozilla.components.browser.menu.item.BrowserMenuDivider
import mozilla.components.browser.menu.item.BrowserMenuHighlightableItem
import mozilla.components.browser.menu.item.BrowserMenuImageSwitch
import mozilla.components.browser.menu.item.BrowserMenuImageText
import mozilla.components.browser.menu.item.BrowserMenuItemToolbar
import mozilla.components.browser.menu.item.SimpleBrowserMenuHighlightableItem
import mozilla.components.browser.state.selector.findCustomTab
import mozilla.components.browser.state.state.CustomTabSessionState
import mozilla.components.browser.state.store.BrowserStore
import org.mozilla.focus.R
import org.mozilla.focus.menu.ToolbarMenu
import org.mozilla.focus.theme.resolveAttribute

class CustomTabMenu(
    private val context: Context,
    private val store: BrowserStore,
    private val currentTabId: String,
    private val onItemTapped: (ToolbarMenu.Item) -> Unit = {}
) : ToolbarMenu {

    private val selectedSession: CustomTabSessionState?
        get() = store.state.findCustomTab(currentTabId)

    override val menuBuilder by lazy {
        BrowserMenuBuilder(
            items = menuItems
        )
    }

    override val menuToolbar by lazy {
        val back = BrowserMenuItemToolbar.TwoStateButton(
            primaryImageResource = R.drawable.ic_back,
            primaryContentDescription = context.getString(R.string.content_description_back),
            primaryImageTintResource = context.theme.resolveAttribute(R.attr.primaryText),
            isInPrimaryState = {
                selectedSession?.content?.canGoBack ?: false
            },
            secondaryImageTintResource = context.theme.resolveAttribute(R.attr.disabled),
            disableInSecondaryState = true,
            longClickListener = { onItemTapped.invoke(ToolbarMenu.Item.Back) }
        ) {
            onItemTapped.invoke(ToolbarMenu.Item.Back)
        }

        val forward = BrowserMenuItemToolbar.TwoStateButton(
            primaryImageResource = R.drawable.ic_forward,
            primaryContentDescription = context.getString(R.string.content_description_forward),
            primaryImageTintResource = context.theme.resolveAttribute(R.attr.primaryText),
            isInPrimaryState = {
                selectedSession?.content?.canGoForward ?: true
            },
            secondaryImageTintResource = context.theme.resolveAttribute(R.attr.disabled),
            disableInSecondaryState = true,
            longClickListener = { onItemTapped.invoke(ToolbarMenu.Item.Forward) }
        ) {
            onItemTapped.invoke(ToolbarMenu.Item.Forward)
        }

        val refresh = BrowserMenuItemToolbar.TwoStateButton(
            primaryImageResource = R.drawable.ic_refresh,
            primaryContentDescription = context.getString(R.string.content_description_reload),
            primaryImageTintResource = context.theme.resolveAttribute(R.attr.primaryText),
            isInPrimaryState = {
                selectedSession?.content?.loading == false
            },
            secondaryImageResource = R.drawable.ic_stop,
            secondaryContentDescription = context.getString(R.string.content_description_stop),
            secondaryImageTintResource = context.theme.resolveAttribute(R.attr.primaryText),
            disableInSecondaryState = false,
            longClickListener = { onItemTapped.invoke(ToolbarMenu.Item.Reload) }
        ) {
            if (selectedSession?.content?.loading == true) {
                onItemTapped.invoke(ToolbarMenu.Item.Stop)
            } else {
                onItemTapped.invoke(ToolbarMenu.Item.Reload)
            }
        }
        BrowserMenuItemToolbar(listOf(back, forward, refresh))
    }

    private val menuItems by lazy {
        val findInPage = BrowserMenuImageText(
            label = context.getString(R.string.find_in_page),
            imageResource = R.drawable.ic_search
        ) {
            onItemTapped.invoke(ToolbarMenu.Item.FindInPage)
        }

        val desktopMode = BrowserMenuImageSwitch(
            imageResource = R.drawable.ic_device_desktop,
            label = context.getString(R.string.preference_performance_request_desktop_site2),
            initialState = {
                selectedSession?.content?.desktopMode ?: true
            }
        ) { checked ->
            onItemTapped.invoke(ToolbarMenu.Item.RequestDesktop(checked))
        }

        val addToHomescreen = BrowserMenuImageText(
            label = context.getString(R.string.menu_add_to_home_screen),
            imageResource = R.drawable.ic_add_to_home_screen
        ) {
            onItemTapped.invoke(ToolbarMenu.Item.AddToHomeScreen)
        }

        val openInApp = BrowserMenuHighlightableItem(
            label = context.getString(R.string.menu_open_with_a_browser2),
            startImageResource = R.drawable.ic_help,
            textColorResource = context.theme.resolveAttribute(R.attr.primaryText),
            highlight = BrowserMenuHighlight.HighPriority(
                backgroundTint = ContextCompat.getColor(context, R.color.mvp_browser_menu_bg),
                canPropagate = false
            )
        ) {
            onItemTapped.invoke(ToolbarMenu.Item.OpenInApp)
        }

        val poweredBy = SimpleBrowserMenuHighlightableItem(
            label = context.getString(R.string.menu_custom_tab_branding, context.getString(R.string.app_name)),
            textSize = CAPTION_TEXT_SIZE,
            textColorResource = context.theme.resolveAttribute(R.attr.primaryText),
            backgroundTint = ContextCompat.getColor(context, R.color.colorPoweredBy)
        )

        listOfNotNull(
            menuToolbar,
            BrowserMenuDivider(),
            findInPage,
            desktopMode,
            BrowserMenuDivider(),
            addToHomescreen,
            openInApp,
            poweredBy
        )
    }

    companion object {
        private const val CAPTION_TEXT_SIZE = 12f
    }
}
