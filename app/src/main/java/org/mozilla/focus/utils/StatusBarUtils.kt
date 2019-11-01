/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.view.View

object StatusBarUtils {
    private var STATUS_BAR_HEIGHT = -1

    interface StatusBarHeightListener {
        fun onStatusBarHeightFetched(statusBarHeight: Int)
    }

    fun getStatusBarHeight(view: View?, listener: StatusBarHeightListener) {
        if (STATUS_BAR_HEIGHT > 0) {
            listener.onStatusBarHeightFetched(STATUS_BAR_HEIGHT)
        }

        view!!.setOnApplyWindowInsetsListener { _, insets ->
            STATUS_BAR_HEIGHT = insets.systemWindowInsetTop
            listener.onStatusBarHeightFetched(STATUS_BAR_HEIGHT)
            insets
        }
    }
}
