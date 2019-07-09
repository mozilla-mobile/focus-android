/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.session.ui

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import org.mozilla.focus.ext.requireComponents

/**
 * Adapter implementation to show a list of active browsing sessions and an "erase" button at the end.
 */
@Suppress("TooManyFunctions")
//The adapter does one thing well, the "large" number of methods comes from listening to the
//many session events (added/removed) etc.
class SessionsAdapter internal constructor(
    private val fragment: SessionsSheetFragment,
    private var sessions: List<Session> = emptyList()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), SessionManager.Observer {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            EraseViewHolder.LAYOUT_ID -> EraseViewHolder(
                fragment,
                inflater.inflate(EraseViewHolder.LAYOUT_ID, parent, false)
            )
            SessionViewHolder.LAYOUT_ID -> SessionViewHolder(
                fragment,
                inflater.inflate(SessionViewHolder.LAYOUT_ID, parent, false) as TextView
            )
            else -> throw IllegalStateException("Unknown viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            EraseViewHolder.LAYOUT_ID -> { /* Nothing to do */ }
            SessionViewHolder.LAYOUT_ID -> (holder as SessionViewHolder).bind(sessions[position])
            else -> throw IllegalStateException("Unknown viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isErasePosition(position)) {
            EraseViewHolder.LAYOUT_ID
        } else {
            SessionViewHolder.LAYOUT_ID
        }
    }

    private fun isErasePosition(position: Int): Boolean {
        return position == sessions.size
    }

    override fun getItemCount(): Int {
        return sessions.size + 1
    }

    override fun onSessionAdded(session: Session) {
        onUpdate(fragment.requireComponents.sessionManager.sessions)
    }

    override fun onSessionRemoved(session: Session) {
        onUpdate(fragment.requireComponents.sessionManager.sessions)
    }

    override fun onSessionSelected(session: Session) {
        onUpdate(fragment.requireComponents.sessionManager.sessions)
    }

    override fun onAllSessionsRemoved() {
        onUpdate(fragment.requireComponents.sessionManager.sessions)
    }

    private fun onUpdate(sessions: List<Session>) {
        this.sessions = sessions
        notifyDataSetChanged()
    }

    fun onItemDismiss(adapterPosition: Int) {
        fragment.requireComponents.sessionManager.remove(sessions[adapterPosition])
    }
}
