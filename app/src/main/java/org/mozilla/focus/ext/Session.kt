/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.os.Bundle
import mozilla.components.browser.session.Session
import org.mozilla.geckoview.GeckoSession
import java.util.WeakHashMap

// Extension methods on the Session class. This is used for additional session data that is not part
// of the upstream browser-session component yet.

private val extensions = WeakHashMap<Session, SessionExtension>()

private fun getOrPutExtension(session: Session): SessionExtension {
    extensions[session]?.let { return it }

    return SessionExtension().also {
        extensions[session] = it
    }
}

private class SessionExtension {
    var savedGeckoSession: GeckoSession? = null
    var savedWebViewState: Bundle? = null
    var shouldRequestDesktopSite: Boolean = false
}

/**
 * Saving the Gecko Session attached to the component session. We need this now because we can no
 * longer serialize a gecko session into a [Bundle] as we did before.
 *
 * Temporary solution until we can use the browser-engine component.
 *
 * Component upstream issue:
 * https://github.com/mozilla-mobile/android-components/issues/408
 */
var Session.savedGeckoSession: GeckoSession?
    get() = getOrPutExtension(this).savedGeckoSession
    set(value) { getOrPutExtension(this).savedGeckoSession = value }

/**
 * Saving the state attached ot a session.
 *
 * Temporary solution until we can use the browser-engine component.
 *
 * Component upstream issue:
 * https://github.com/mozilla-mobile/android-components/issues/408
 */
var Session.savedWebViewState: Bundle?
    get() = getOrPutExtension(this).savedWebViewState
    set(value) { getOrPutExtension(this).savedWebViewState = value }

/**
 * Whether to use desktop mode for this session.
 *
 * Component upstream issue:
 * https://github.com/mozilla-mobile/android-components/issues/701
 */
var Session.shouldRequestDesktopSite: Boolean
    get() = getOrPutExtension(this).shouldRequestDesktopSite
    set(value) { getOrPutExtension(this).shouldRequestDesktopSite = value }
