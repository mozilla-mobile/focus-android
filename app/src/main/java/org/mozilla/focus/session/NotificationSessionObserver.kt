/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.session

import android.content.Context
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import org.mozilla.focus.ext.components

class NotificationSessionObserver(
    private val context: Context
) : SessionManager.Observer {

    override fun onSessionRemoved(session: Session) {
        if (context.components.sessionManager.sessions.isEmpty()) {
            SessionNotificationService.stop(context)
        }
    }

    override fun onAllSessionsRemoved() {
        SessionNotificationService.stop(context)
    }

    override fun onSessionAdded(session: Session) {
        SessionNotificationService.start(context)
    }
}
