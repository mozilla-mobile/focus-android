/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.open

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import org.mozilla.focus.R

class AppViewHolder/* package */(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private var titleView: TextView
    private var iconView: ImageView

    init {
        titleView = itemView.findViewById(R.id.title)
        iconView = itemView.findViewById(R.id.icon)
        LAYOUT_ID = R.layout.menu_item
    }

    fun bind (app: AppAdapter.App, listener: AppAdapter.OnAppSelectedListener) {
        titleView.setText(app.label)

        iconView.setImageDrawable(app.loadIcon())

        titleView.setOnClickListener(createListenerWrapper(app, listener))
    }

    fun createListenerWrapper(app: AppAdapter.App, listener: AppAdapter.OnAppSelectedListener) : View.OnClickListener {
        return View.OnClickListener {
            run {
                listener.onAppSelected(app)
            }
        }
    }

    companion object {
        lateinit var LAYOUT_ID: Number
    }
}
