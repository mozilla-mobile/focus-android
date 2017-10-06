/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.menu.home

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import kotlinx.android.synthetic.main.menu.view.*
import org.mozilla.focus.R
import org.mozilla.focus.utils.ViewUtils

/**
 * The overflow menu shown on the start/home screen.
 */
class HomeMenu(
        val context: Context,
        val listener: View.OnClickListener
) : PopupWindow(), View.OnClickListener {
    init {
        contentView = LayoutInflater.from(context).inflate(R.layout.menu, null)

        with(contentView.list) {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = HomeMenuAdapter(context, this@HomeMenu)
        }

        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        isFocusable = true

        height = ViewGroup.LayoutParams.WRAP_CONTENT
        width = ViewGroup.LayoutParams.WRAP_CONTENT

        elevation = context.resources.getDimension(R.dimen.menu_elevation)
    }

    fun show(anchor: View) {
        val xOffset = if (ViewUtils.isRTL(anchor)) -anchor.width else 0

        super.showAsDropDown(anchor, xOffset, -(anchor.height + anchor.paddingBottom))
    }

    override fun onClick(view: View?) {
        dismiss()

        listener.onClick(view)
    }
}
