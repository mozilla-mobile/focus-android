/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.cfr

import android.content.Context
import androidx.core.net.toUri
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.action.TrackingProtectionAction
import mozilla.components.browser.state.selector.findTabOrCustomTabOrSelectedTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import org.mozilla.focus.ext.components
import org.mozilla.focus.ext.truncatedHost
import org.mozilla.focus.nimbus.FocusNimbus
import org.mozilla.focus.nimbus.Onboarding
import org.mozilla.focus.state.AppAction

/**
 * Middleware used to intercept browser store actions in order to decide when should we display a specific CFR
 */
class CfrMiddleware(private val appContext: Context) : Middleware<BrowserState, BrowserAction> {
    private val onboardingFeature = FocusNimbus.features.onboarding
    private lateinit var onboardingConfig: Onboarding
    private val components = appContext.components
    private var isCurrentTabSecure = false
    private var tpExposureAlreadyRecorded = false

    override fun invoke(
        context: MiddlewareContext<BrowserState, BrowserAction>,
        next: (BrowserAction) -> Unit,
        action: BrowserAction,
    ) {
        onboardingConfig = onboardingFeature.value(context = appContext)
        if (onboardingConfig.isCfrEnabled) {
            if (action is ContentAction.UpdateSecurityInfoAction) {
                isCurrentTabSecure = action.securityInfo.secure
            }

            next(action)

            showTrackingProtectionCfr(action, context)
        } else {
            next(action)
        }
    }

    private fun showTrackingProtectionCfr(
        action: BrowserAction,
        context: MiddlewareContext<BrowserState, BrowserAction>,
    ) {
        if (shouldShowCfrForTrackingProtection(action = action, browserState = context.state)) {
            if (!tpExposureAlreadyRecorded) {
                FocusNimbus.features.onboarding.recordExposure()
                tpExposureAlreadyRecorded = true
            }

            components.appStore.dispatch(
                AppAction.ShowTrackingProtectionCfrChange(
                    mapOf((action as TrackingProtectionAction.TrackerBlockedAction).tabId to true),
                ),
            )
        }
    }

    private fun isMozillaUrl(browserState: BrowserState): Boolean {
        return browserState.findTabOrCustomTabOrSelectedTab(
            browserState.selectedTabId,
        )?.content?.url?.toUri()?.truncatedHost()?.substringBefore(".") == ("mozilla")
    }

    private fun isActionSecure(action: BrowserAction) =
        action is TrackingProtectionAction.TrackerBlockedAction && isCurrentTabSecure

    private fun shouldShowCfrForTrackingProtection(
        action: BrowserAction,
        browserState: BrowserState,
    ) = (
        isActionSecure(action = action) &&
            !isMozillaUrl(browserState = browserState) &&
            components.settings.shouldShowCfrForTrackingProtection &&
            !components.appStore.state.showEraseTabsCfr
        )
}
