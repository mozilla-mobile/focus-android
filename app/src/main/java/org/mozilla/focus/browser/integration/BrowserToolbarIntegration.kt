/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser.integration

import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.browser.toolbar.display.DisplayToolbar
import mozilla.components.feature.customtabs.CustomTabsToolbarFeature
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.CustomTabsUseCases
import mozilla.components.feature.toolbar.ToolbarPresenter
import mozilla.components.support.base.feature.LifecycleAwareFeature
import org.mozilla.focus.GleanMetrics.TrackingProtection
import org.mozilla.focus.R
import org.mozilla.focus.fragment.BrowserFragment
import org.mozilla.focus.menu.browser.CustomTabMenu
import org.mozilla.focus.utils.HardwareUtils

@Suppress("LongParameterList")
class BrowserToolbarIntegration(
    store: BrowserStore,
    toolbar: BrowserToolbar,
    fragment: BrowserFragment,
    controller: BrowserMenuController,
    sessionUseCases: SessionUseCases,
    customTabsUseCases: CustomTabsUseCases,
    private val onUrlLongClicked: () -> Boolean,
    customTabId: String? = null
) : LifecycleAwareFeature {
    private val presenter = ToolbarPresenter(
        toolbar,
        store,
        customTabId
    )

    private var customTabsFeature: CustomTabsToolbarFeature? = null
    private var navigationButtonsIntegration: NavigationButtonsIntegration? = null

    init {
        val context = toolbar.context

        toolbar.display.apply {
            colors = colors.copy(
                hint = ContextCompat.getColor(toolbar.context, R.color.photonLightGrey05),
                text = ContextCompat.getColor(toolbar.context, R.color.primaryText)
            )

            indicators = listOf(
                DisplayToolbar.Indicators.TRACKING_PROTECTION
            )

            displayIndicatorSeparator = false

            setOnSiteSecurityClickedListener {
                fragment.showSecurityPopUp()
            }

            onUrlClicked = {
                fragment.edit()
                false // Do not switch to edit mode
            }

            setOnUrlLongClickListener { onUrlLongClicked() }

            icons = icons.copy(
                trackingProtectionTrackersBlocked = AppCompatResources.getDrawable(
                    context,
                    R.drawable.mozac_ic_shield
                )!!,
                trackingProtectionNothingBlocked = AppCompatResources.getDrawable(
                    context,
                    R.drawable.mozac_ic_shield
                )!!,
                trackingProtectionException = AppCompatResources.getDrawable(
                    context,
                    R.drawable.mozac_ic_shield_disabled
                )!!
            )
        }

        toolbar.display.setOnTrackingProtectionClickedListener {
            TrackingProtection.toolbarShieldClicked.add()
            fragment.showTrackingProtectionPanel()
        }

        // Use the same background for display/edit modes.
        val urlBackground = ResourcesCompat.getDrawable(
            fragment.resources,
            R.drawable.toolbar_url_background,
            fragment.context?.theme
        )

        toolbar.display.setUrlBackground(urlBackground)
        toolbar.edit.setUrlBackground(urlBackground)

        if (customTabId != null) {
            val menu = CustomTabMenu(
                context = fragment.requireContext(),
                store = store,
                currentTabId = customTabId,
                onItemTapped = { controller.handleMenuInteraction(it) }
            )
            customTabsFeature = CustomTabsToolbarFeature(
                store,
                toolbar,
                sessionId = customTabId,
                useCases = customTabsUseCases,
                menuBuilder = menu.menuBuilder,
                menuItemIndex = menu.menuBuilder.items.size - 1,
                closeListener = { fragment.closeCustomTab() }
            )
        }

        if (HardwareUtils.isTablet(context)) {
            navigationButtonsIntegration = NavigationButtonsIntegration(
                context,
                store,
                toolbar,
                sessionUseCases,
                customTabId
            )
        }
    }

    override fun start() {
        presenter.start()

        customTabsFeature?.start()
        navigationButtonsIntegration?.start()
    }

    override fun stop() {
        presenter.stop()

        customTabsFeature?.stop()
        navigationButtonsIntegration?.stop()
    }
}
