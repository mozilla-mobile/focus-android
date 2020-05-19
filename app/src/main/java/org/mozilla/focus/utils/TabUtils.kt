/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import mozilla.components.browser.session.Session

fun createTab(
   url: String,
   source: Session.Source = Session.Source.NONE
): Session {
    return Session(url, source = source, private = true)
}
