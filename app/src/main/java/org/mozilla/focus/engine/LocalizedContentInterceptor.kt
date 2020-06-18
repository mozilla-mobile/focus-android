/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.engine

import android.content.Context
import mozilla.components.browser.errorpages.ErrorPages
import mozilla.components.browser.errorpages.ErrorType
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor
import org.mozilla.focus.browser.LocalizedContent

class LocalizedContentInterceptor(
    private val context: Context
) : RequestInterceptor {
    override fun onLoadRequest(
        engineSession: EngineSession,
        uri: String,
        hasUserGesture: Boolean,
        isSameDomain: Boolean
    ): RequestInterceptor.InterceptionResponse? {
        return when (uri) {
            LocalizedContent.URL_ABOUT -> RequestInterceptor.InterceptionResponse.Content(
                LocalizedContent.loadAbout(context), encoding = "base64"
            )

            LocalizedContent.URL_RIGHTS -> RequestInterceptor.InterceptionResponse.Content(
                LocalizedContent.loadRights(context), encoding = "base64"
            )

            LocalizedContent.URL_GPL -> RequestInterceptor.InterceptionResponse.Content(
                LocalizedContent.loadGPL(context), encoding = "base64"
            )

            LocalizedContent.URL_LICENSES -> RequestInterceptor.InterceptionResponse.Content(
                LocalizedContent.loadLicenses(context), encoding = "base64"
            )

            else -> null
        }
    }

    override fun onErrorRequest(
        session: EngineSession,
        errorType: ErrorType,
        uri: String?
    ): RequestInterceptor.ErrorResponse? {
        val errorPage = ErrorPages.createUrlEncodedErrorPage(context, errorType, uri)
        return RequestInterceptor.ErrorResponse.Uri(errorPage)
    }

    override fun interceptsAppInitiatedRequests() = true
}
