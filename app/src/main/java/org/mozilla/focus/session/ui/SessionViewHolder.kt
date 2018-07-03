/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.session.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import org.mozilla.focus.R
import org.mozilla.focus.session.Session
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.ext.beautifyUrl
import java.lang.ref.WeakReference

class SessionViewHolder internal constructor(
    private val fragment: SessionsSheetFragment,
    private val textView: TextView
) : RecyclerView.ViewHolder(textView), View.OnClickListener {
    companion object {
        @JvmField
        internal val LAYOUT_ID = R.layout.item_session
    }

    private var sessionReference: WeakReference<Session> = WeakReference<Session>(null)

    init {
        textView.setOnClickListener(this)
    }

    fun bind(session: Session) {
        this.sessionReference = WeakReference(session)

        updateTitle(session)

        val isCurrentSession = SessionManager.getInstance().isCurrentSession(session)

        val color = ContextCompat.getColor(textView.context, R.color.session_active)
        if (isCurrentSession) {
            this.textView.setBackgroundColor(color)
        }
    }

    private fun updateTitle(session: Session) {
        textView.text = session.pageTitle.value ?: session.url.value.beautifyUrl()
    }

    override fun onClick(view: View) {
        val session = sessionReference.get() ?: return
        selectSession(session)
    }

    private fun selectSession(session: Session) {
        fragment.animateAndDismiss().addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                SessionManager.getInstance().selectSession(session)

                TelemetryWrapper.switchTabInTabsTrayEvent()
            }
        })
    }
}
