/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import mozilla.components.browser.state.state.CustomTabSessionState
import mozilla.components.browser.state.state.SessionState
import mozilla.components.concept.engine.content.blocking.TrackingProtectionException
import mozilla.components.feature.session.TrackingProtectionUseCases

/**
 * Returns this [SessionState] cast to [CustomTabSessionState] if possible. Otherwise returns `null`.
 */
fun SessionState.ifCustomTab(): CustomTabSessionState? {
    if (this is CustomTabSessionState) {
        return this
    }
    return null
}

/**
 * Returns `true` if this [SessionState] is a custom tab (an instance of [CustomTabSessionState]).
 */
fun SessionState.isCustomTab(): Boolean {
    return this is CustomTabSessionState
}

/**
 * Removes a [TrackingProtectionException] if one is set for the current session.
 */
fun SessionState.removeFromExceptions(trackingProtectionUseCases: TrackingProtectionUseCases) {
    var exceptions: List<TrackingProtectionException>
    trackingProtectionUseCases.fetchExceptions.invoke {
        exceptions = it.filter { exception ->
            exception.url == this.content.url
        }
        exceptions.firstOrNull()?.let {
            trackingProtectionUseCases.removeException(exceptions.first())
        }
    }
}
