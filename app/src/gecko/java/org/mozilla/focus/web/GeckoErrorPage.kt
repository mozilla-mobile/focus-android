/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.web

import android.content.Context
import android.support.v4.util.ArrayMap
import android.support.v4.util.Pair
import android.webkit.WebViewClient
import org.mozilla.focus.R
import org.mozilla.focus.utils.HtmlLoader
import org.mozilla.geckoview.GeckoSession
import java.util.HashMap

object GeckoErrorPage {

    private val errorDescriptionMap: HashMap<Int, Pair<Int, Int>> = HashMap()

    init {
        // Chromium's mapping (internal error code, to Android WebView error code) is described at:
        // https://chromium.googlesource.com/chromium/src.git/+/master/android_webview/java/src/org/chromium/android_webview/ErrorCodeConversionHelper.java
        errorDescriptionMap[WebViewClient.ERROR_UNKNOWN] = Pair(
            R.string.error_connectionfailure_title, R.string.error_connectionfailure_message)

        // This is probably the most commonly shown error. If there's no network, we inevitably
        // show this.
        errorDescriptionMap[WebViewClient.ERROR_HOST_LOOKUP] = Pair(R.string.error_hostLookup_title,
            R.string.error_hostLookup_message)

//        WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME
        // TODO: we don't actually handle this in firefox - does this happen in real life?

//        WebViewClient.ERROR_AUTHENTICATION
        // TODO: there's no point in implementing this until we actually support http auth (#159)

        errorDescriptionMap[WebViewClient.ERROR_CONNECT] = Pair(R.string.error_connect_title,
            R.string.error_connect_message)

        // It's unclear what this actually means - it's not well documented. Based on looking at
        // ErrorCodeConversionHelper this could happen if networking is disabled during load, in which
        // case the generic error is good enough:
        errorDescriptionMap[WebViewClient.ERROR_IO] = Pair(R.string.error_connectionfailure_title,
            R.string.error_connectionfailure_message)

        errorDescriptionMap[WebViewClient.ERROR_TIMEOUT] = Pair(R.string.error_timeout_title,
            R.string.error_timeout_message)

        errorDescriptionMap[WebViewClient.ERROR_REDIRECT_LOOP] = Pair(
            R.string.error_redirectLoop_title, R.string.error_redirectLoop_message)

        // We already try to handle external URLs if possible (i.e. we offer to open the corresponding
        // app, if available for a given scheme). If we end up here that means no app exists.
        // We could consider showing an "open google play" link here, but ultimately it's hard
        // to know whether that's the right step, especially if there are no good apps for actually
        // handling such a protocol there - moreover there doesn't seem to be a good way to search
        // google play for apps supporting a given scheme.
        errorDescriptionMap[WebViewClient.ERROR_UNSUPPORTED_SCHEME] = Pair(
            R.string.error_unsupportedprotocol_title, R.string.error_unsupportedprotocol_message)

        errorDescriptionMap[WebViewClient.ERROR_FAILED_SSL_HANDSHAKE] = Pair(
            R.string.error_sslhandshake_title, R.string.error_sslhandshake_message)

        errorDescriptionMap[WebViewClient.ERROR_BAD_URL] = Pair(R.string.error_malformedURI_title,
            R.string.error_malformedURI_message)

        // WebView returns ERROR_UNKNOWN when we try to access a file:/// on Android (with the error string
        // containing access denied), so I'm not too sure why these codes exist:
        // sure why these error codes exit
        // WebViewClient.ERROR_FILE;
        // WebViewClient.ERROR_FILE_NOT_FOUND;

        // Seems to be an indication of OOM, insufficient resources, or too many queued DNS queries
        errorDescriptionMap[WebViewClient.ERROR_TOO_MANY_REQUESTS] = Pair(
            R.string.error_generic_title, R.string.error_generic_message)
    }

    fun loadErrorPage(context: Context, geckoSession: GeckoSession, desiredURL: String, errorCode:
    Int) {
        val errorResourceIDs = errorDescriptionMap[errorCode] ?: throw IllegalArgumentException(
            "Cannot load error description for unsupported errorcode=" + errorCode)

        // This is quite hacky: ideally we'd just load the css file directly using a '<link rel="stylesheet"'.
        // However WebView thinks it's still loading the original page, which can be an https:// page.
        // If mixed content blocking is enabled (which is probably what we want in Focus), then webkit
        // will block file:///android_res/ links from being loaded - which blocks our css from being loaded.
        // We could hack around that by enabling mixed content when loading an error page (and reenabling it
        // once that's loaded), but doing that correctly and reliably isn't particularly simple. Loading
        // the css data and stuffing it into our html is much simpler, especially since we're already doing
        // string substitutions.
        // As an added bonus: file:/// URIs are broken if the app-ID != app package, see:
        // https://code.google.com/p/android/issues/detail?id=211768 (this breaks loading css via file:///
        // references when running debug builds, and probably klar too) - which means this wouldn't
        // be possible even if we hacked around the mixed content issues.
        val cssString = HtmlLoader.loadResourceFile(context, R.raw.errorpage_style, null)

        val substitutionMap = ArrayMap<String, String>()

        val resources = context.resources

        substitutionMap["%page-title%"] = resources.getString(R.string.errorpage_title)
        substitutionMap["%button%"] = resources.getString(R.string.errorpage_refresh)

        substitutionMap["%messageShort%"] = resources.getString(errorResourceIDs.first!!)
        substitutionMap["%messageLong%"] = resources.getString(errorResourceIDs.second!!,
            desiredURL)

        substitutionMap["%css%"] = cssString

        val errorPage = HtmlLoader.loadResourceFile(context, R.raw.errorpage, substitutionMap)

        // We could load the raw html file directly into the webview using a file:///android_res/
        // URI - however we'd then need to do some JS hacking to do our String substitutions. Moreover
        // we'd have to deal with the mixed-content issues detailed above in that case.
        geckoSession.loadData(errorPage.toByteArray(Charsets.UTF_8), "text/html", desiredURL)
    }
}