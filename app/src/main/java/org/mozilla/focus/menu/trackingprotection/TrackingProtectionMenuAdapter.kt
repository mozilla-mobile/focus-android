/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.menu.trackingprotection

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import mozilla.components.browser.session.Session
import org.mozilla.focus.R
import org.mozilla.focus.fragment.BrowserFragment
import java.lang.ref.WeakReference

class TrackingProtectionMenuAdapter(
    private val context: Context,
    private val menu: TrackingProtectionMenu,
    private val fragment: BrowserFragment
) : RecyclerView.Adapter<TrackingProtectionMenuViewHolder>() {
    sealed class MenuItem {
        open val viewType = 0

        object BlockingSwitch : MenuItem() {
            override val viewType = BlockingItemViewHolder.LAYOUT_ID
        }

        object Security : MenuItem() {
            override val viewType = SecurityItemViewHolder.LAYOUT_ID
        }
    }

    private var items = mutableListOf<MenuItem>()
    private var blockingItemViewHolderReference = WeakReference<BlockingItemViewHolder>(null)
    private var securityItemViewHolderReference = WeakReference<SecurityItemViewHolder>(null)

    init {
        initializeMenu()
    }

    private fun initializeMenu() {
        items.add(MenuItem.BlockingSwitch)
        items.add(MenuItem.Security)
    }

    fun updateTrackers(trackers: Int) {
        val blockingItemViewHolder = blockingItemViewHolderReference.get() ?: return
        blockingItemViewHolder.updateTrackers(trackers)
    }

    fun updateBlocking(isBlockingEnabled: Boolean) {
        val blockingItemViewHolder = blockingItemViewHolderReference.get() ?: return
        blockingItemViewHolder.addOrRemoveURL(isBlockingEnabled)
    }

    fun updateSecurity(session: Session) {
        val securityItemViewHolder = securityItemViewHolderReference.get() ?: return
        securityItemViewHolder.setSecurityInfo(context, session)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TrackingProtectionMenuViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            BlockingItemViewHolder.LAYOUT_ID -> {
                val blockingItemViewHolder = BlockingItemViewHolder(
                    inflater.inflate(R.layout.menu_blocking_switch, parent, false), fragment
                )
                blockingItemViewHolderReference = WeakReference(blockingItemViewHolder)
                blockingItemViewHolder
            }
            SecurityItemViewHolder.LAYOUT_ID -> {
                val securityItemViewHolder = SecurityItemViewHolder(
                    inflater.inflate(R.layout.menu_security, parent, false), fragment
                )
                securityItemViewHolderReference = WeakReference(securityItemViewHolder)
                securityItemViewHolder
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: TrackingProtectionMenuViewHolder, position: Int) {
        holder.menu = menu
        holder.setOnClickListener(fragment)
    }

    override fun getItemViewType(position: Int): Int = items[position].viewType
    override fun getItemCount(): Int = items.size
}
