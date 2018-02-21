/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.web

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import org.mozilla.focus.R
import org.mozilla.focus.permissions.IPermissionsDelegate
import org.mozilla.geckoview.GeckoSession
import java.util.Locale

class GeckoViewPermissionsDelegate(
        private val context: Context,
        private val geckoSession: GeckoSession
) : GeckoSession.PermissionDelegate,
        IPermissionsDelegate {
    private var callback: GeckoSession.PermissionDelegate.Callback? = null

    override fun onRequestPermissionsResult(permissions: Array<String>, grantResults: IntArray) {
        val cb = callback ?: return
        callback = null
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                // At least one permission was not granted.
                cb.reject()
                return
            }
        }
        cb.grant()
    }

    override fun requestAndroidPermissions(session: GeckoSession, permissions: Array<String>,
                                           callback: GeckoSession.PermissionDelegate.Callback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // requestPermissions was introduced in API 23.
            callback.grant()
        } else {
            this.callback = callback
            (context as Activity).requestPermissions(permissions, androidPermissionRequestCode)
        }
    }

    override fun requestContentPermission(session: GeckoSession, uri: String,
                                          type: String, access: String,
                                          callback: GeckoSession.PermissionDelegate.Callback) {
        val resId = when (type) {
            "geolocation" -> R.string.request_geolocation
            "desktop-notification" -> R.string.request_notification
            else -> {
                Log.w(LOGTAG, "Unknown permission: " + type)
                callback.reject()
                return
            }
        }

        val title = context.getString(resId, Uri.parse(uri).authority)
        val prompt = geckoSession.promptDelegate as GeckoViewPrompt
        prompt.promptForPermission(session, title, callback)
    }

    private fun normalizeMediaName(sources: Array<GeckoSession.PermissionDelegate.MediaSource>?) {
        if (sources == null) {
            return
        }
        for (source in sources) {
            val mediaSource = source.source
            var name = source.name

            name = when (mediaSource) {
                GeckoSession.PermissionDelegate.MediaSource.SOURCE_CAMERA ->
                    if (name.toLowerCase(Locale.ENGLISH).contains("front"))
                        context.getString(R.string.media_front_camera)
                    else context.getString(R.string.media_back_camera)
                GeckoSession.PermissionDelegate.MediaSource.SOURCE_MICROPHONE ->
                    context.getString(R.string.media_microphone)
                else -> context.getString(R.string.media_other)
            }
            source.name = name
        }
    }

    override fun requestMediaPermission(session: GeckoSession, uri: String,
                                        video: Array<GeckoSession.PermissionDelegate.MediaSource>?,
                                        audio: Array<GeckoSession.PermissionDelegate.MediaSource>?,
                                        callback: GeckoSession.PermissionDelegate.MediaCallback) {
        val host = Uri.parse(uri).authority
        val title: String
        title = when {
            audio == null -> context.getString(R.string.request_video, host)
            video == null -> context.getString(R.string.request_audio, host)
            else -> context.getString(R.string.request_media, host)
        }

        normalizeMediaName(video)
        normalizeMediaName(audio)

        val prompt = geckoSession.promptDelegate as GeckoViewPrompt
        prompt.promptForMedia(session, title, video, audio, callback)
    }

    companion object {
        const val LOGTAG = "GeckoPermissions"
        const val androidPermissionRequestCode = 303
    }
}
