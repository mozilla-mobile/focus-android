/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.menu.trackingprotection

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.view.View
import android.webkit.URLUtil
import android.widget.ImageView
import android.widget.TextView
import mozilla.components.browser.session.Session
import mozilla.components.support.ktx.android.graphics.drawable.toBitmap
import mozilla.components.support.utils.DrawableUtils
import org.mozilla.focus.R
import org.mozilla.focus.fragment.BrowserFragment
import java.net.MalformedURLException
import java.net.URL

internal class SecurityItemViewHolder(itemView: View, fragment: BrowserFragment) :
    TrackingProtectionMenuViewHolder(itemView) {

    init {
        setSecurityInfo(fragment.requireContext(), fragment.session)
    }

    fun setSecurityInfo(context: Context, session: Session) {
        val isSecure = session.securityInfo.secure
        val url = session.url
        val res = context.resources

        val origin = session.securityInfo.host
        val verifierInfo = session.securityInfo.issuer

        val hostInfo = itemView.findViewById<TextView>(R.id.site_identity_title)
        val securityInfoIcon = itemView.findViewById<ImageView>(R.id.site_identity_icon)
        val verifier = itemView.findViewById<TextView>(R.id.verifier)

        setOrigin(hostInfo, origin, url)

        val identityState = itemView.findViewById<TextView>(R.id.site_identity_state)
        identityState.setText(
            if (isSecure)
                R.string.security_popup_secure_connection
            else
                R.string.security_popup_insecure_connection
        )

        if (isSecure) {
            setSecurityInfoSecure(
                context, identityState, verifierInfo, verifier,
                securityInfoIcon
            )
        } else {
            verifier.visibility = View.GONE
            setSecurityInfoInsecure(context, identityState, url, securityInfoIcon)
        }

        identityState.compoundDrawablePadding = res.getDimension(
            R.dimen.doorhanger_drawable_padding
        ).toInt()
    }

    private fun setOrigin(hostInfo: TextView, origin: String?, url: String) {
        hostInfo.text = if (!TextUtils.isEmpty(origin)) {
            origin
        } else {
            try {
                URL(url).host
            } catch (e: MalformedURLException) {
                url
            }
        }
    }

    private fun setSecurityInfoSecure(
        context: Context,
        identityState: TextView,
        verifierInfo: String?,
        verifier: TextView,
        securityInfoIcon: ImageView
    ) {
        val photonGreen = ContextCompat.getColor(context, R.color.photonGreen60)
        val checkIcon = DrawableUtils.loadAndTintDrawable(context, R.drawable.ic_check, photonGreen)
        identityState.setCompoundDrawables(
            getScaledDrawable(context, R.dimen.doorhanger_small_icon, checkIcon), null,
            null, null
        )
        identityState.setTextColor(photonGreen)
        if (!TextUtils.isEmpty(verifierInfo)) {
            verifier.text = context.getString(
                R.string.security_popup_security_verified,
                verifierInfo
            )
            verifier.visibility = View.VISIBLE
        } else {
            verifier.visibility = View.GONE
        }
        securityInfoIcon.setImageResource(R.drawable.ic_lock)
        securityInfoIcon.setColorFilter(photonGreen)
    }

    private fun setSecurityInfoInsecure(
        context: Context,
        identityState: TextView,
        url: String,
        securityInfoIcon: ImageView
    ) {
        identityState.setTextColor(Color.WHITE)

        val inactiveColor = ContextCompat.getColor(context, R.color.colorTextInactive)
        val photonYellow = ContextCompat.getColor(context, R.color.photonYellow60)

        val securityIcon: Drawable
        if (URLUtil.isHttpUrl(url)) {
            securityInfoIcon.setImageResource(R.drawable.ic_internet)
            securityInfoIcon.setColorFilter(inactiveColor)
        } else {
            securityInfoIcon.setImageResource(R.drawable.ic_warning)
            securityInfoIcon.setColorFilter(photonYellow)
            securityIcon = DrawableUtils.loadAndTintDrawable(
                context, R.drawable.ic_warning,
                photonYellow
            )
            identityState.setCompoundDrawables(
                getScaledDrawable(
                    context, R.dimen.doorhanger_small_icon,
                    securityIcon
                ),
                null,
                null, null
            )
        }
    }

    private fun getScaledDrawable(context: Context, size: Int, drawable: Drawable): Drawable {
        val iconSize = context.resources.getDimension(size).toInt()
        val icon = BitmapDrawable(context.resources, drawable.toBitmap())
        icon.setBounds(0, 0, iconSize, iconSize)
        return icon
    }

    companion object {
        val LAYOUT_ID = R.layout.menu_security
    }
}
