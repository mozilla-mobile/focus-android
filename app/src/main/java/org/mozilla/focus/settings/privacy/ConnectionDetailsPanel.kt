/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.settings.privacy

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import mozilla.components.browser.icons.IconRequest
import mozilla.components.support.ktx.android.view.putCompoundDrawablesRelativeWithIntrinsicBounds
import org.mozilla.focus.R
import org.mozilla.focus.databinding.ConnectionDetailsBinding
import org.mozilla.focus.ext.components

@SuppressWarnings("LongParameterList")
class ConnectionDetailsPanel(
    context: Context,
    private val tabTitle: String,
    private val tabUrl: String,
    private val isConnectionSecure: Boolean,
    private val goBack: () -> Unit
) : BottomSheetDialog(context) {

    private var binding: ConnectionDetailsBinding =
        ConnectionDetailsBinding.inflate(layoutInflater, null, false)

    init {
        setContentView(binding.root)
        expandBottomSheet()

        updateSiteInfo()
        updateConnectionState()
        setListeners()
    }

    private fun expandBottomSheet() {
        val bottomSheet =
            findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
        BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun updateSiteInfo() {
        binding.siteTitle.text = tabTitle
        binding.siteFullUrl.text = tabUrl

        context.components.icons.loadIntoView(
            binding.siteFavicon,
            IconRequest(tabUrl, isPrivate = true)
        )
    }

    private fun updateConnectionState() {
        binding.securityInfo.text = if (isConnectionSecure) {
            context.getString(R.string.secure_connection)
        } else {
            context.getString(R.string.insecure_connection)
        }

        val securityIcon = if (isConnectionSecure) {
            AppCompatResources.getDrawable(context, R.drawable.mozac_ic_lock)
        } else {
            AppCompatResources.getDrawable(context, R.drawable.mozac_ic_warning)
        }

        binding.securityInfo.putCompoundDrawablesRelativeWithIntrinsicBounds(
            start = securityIcon,
            end = null,
            top = null,
            bottom = null
        )
    }

    private fun setListeners() {
        binding.detailsBack.setOnClickListener {
            goBack.invoke()
            dismiss()
        }
    }
}
