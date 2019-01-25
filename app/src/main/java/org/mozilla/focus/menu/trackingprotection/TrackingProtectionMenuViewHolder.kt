/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.menu.trackingprotection

import android.support.v7.widget.RecyclerView
import android.view.View

import org.mozilla.focus.fragment.BrowserFragment

abstract class TrackingProtectionMenuViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView),
    View.OnClickListener {
    protected var browserFragment: BrowserFragment? = null
    var menu: TrackingProtectionMenu? = null

    fun setOnClickListener(browserFragment: BrowserFragment) {
        this.browserFragment = browserFragment
    }

    override fun onClick(view: View) {
        if (menu != null) {
            menu!!.dismiss()
        }

        if (browserFragment != null) {
            browserFragment!!.onClick(view)
        }
    }
}
