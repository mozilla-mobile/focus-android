/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.menu.trackingprotection

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import mozilla.components.browser.session.Session
import org.mozilla.focus.R
import org.mozilla.focus.fragment.BrowserFragment
import org.mozilla.focus.utils.ViewUtils

/**
 * The overflow menu shown in the BrowserFragment containing page actions like "Refresh", "Share" etc.
 */
class TrackingProtectionMenu(context: Context, fragment: BrowserFragment) : PopupWindow() {
    private val adapter: TrackingProtectionMenuAdapter

    init {
        @SuppressLint("InflateParams") // This View will have it's params ignored anyway:
        val view = LayoutInflater.from(context).inflate(R.layout.menu, null)
        contentView = view

        adapter = TrackingProtectionMenuAdapter(context, this, fragment)

        val menuList = view.findViewById<RecyclerView>(R.id.list)
        menuList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        menuList.adapter = adapter
        val dividerItemDecoration =
            DividerItemDecorator(ContextCompat.getDrawable(context, R.drawable.divider)!!)
        menuList.addItemDecoration(dividerItemDecoration)

        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        isFocusable = true

        height = ViewGroup.LayoutParams.WRAP_CONTENT
        width = ViewGroup.LayoutParams.WRAP_CONTENT

        elevation = context.resources.getDimension(R.dimen.menu_elevation)
    }

    fun updateTrackers(trackers: Int) {
        adapter.updateTrackers(trackers)
    }

    fun updateBlocking(isBlockingEnabled: Boolean) {
        adapter.updateBlocking(isBlockingEnabled)
    }

    fun updateSecurity(session: Session) {
        adapter.updateSecurity(session)
    }

    fun show(anchor: View) {
        val xOffset = if (ViewUtils.isRTL(anchor)) -anchor.width else 0

        super.showAsDropDown(anchor, xOffset, -(anchor.height + anchor.paddingBottom))
    }

    internal inner class DividerItemDecorator(private val mDivider: Drawable) :
        RecyclerView.ItemDecoration() {

        override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val dividerLeft = parent.paddingLeft
            val dividerRight = parent.width - parent.paddingRight

            val childCount = parent.childCount
            for (i in 0..childCount - 2) {
                val child = parent.getChildAt(i)

                val params = child.layoutParams as RecyclerView.LayoutParams

                val dividerTop = child.bottom + params.bottomMargin
                val dividerBottom = dividerTop + mDivider.intrinsicHeight

                mDivider.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom)
                mDivider.draw(canvas)
            }
        }
    }
}
