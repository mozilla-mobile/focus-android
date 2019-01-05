/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.view.View


class StatusBarUtils {

    companion object {
        private var STATUS_BAR_HEIGHT = -1

        fun getStatusBarHeight(view: View?, listener: (Int) -> Unit) {
            if (StatusBarUtils.STATUS_BAR_HEIGHT > 0) {
                listener(STATUS_BAR_HEIGHT)
            }

            view?.let {
                view.setOnApplyWindowInsetsListener { _, insets ->
                    STATUS_BAR_HEIGHT = insets.systemWindowInsetTop
                    listener(STATUS_BAR_HEIGHT)
                    insets
                }
            }
        }
    }
}
