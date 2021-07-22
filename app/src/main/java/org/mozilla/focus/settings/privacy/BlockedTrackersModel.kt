/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.settings.privacy

data class BlockedTrackersModel(
    // Add custom default values just for testing
    val trackersBlocked: String = "112,663",
    val monitoringDate: String = "21, 2021"
)
