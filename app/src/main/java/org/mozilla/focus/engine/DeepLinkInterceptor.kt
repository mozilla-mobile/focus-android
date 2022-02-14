/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.engine

import android.content.Context
import android.content.Intent
import android.util.Log
import org.mozilla.focus.ext.components
import org.mozilla.focus.state.AppAction
import org.mozilla.focus.state.Screen

class DeepLinkInterceptor(private val context: Context) {
    companion object {
        const val DESTINATION_KEY = "destination"
        val TAG = DeepLinkInterceptor::class.qualifiedName
        const val DESTINATION_VALUE_PRIVACY = "privacy"
    }

    /**
     * Redirect the user to a screen from the app base on S.destination from string file
     *
     * @param intent that comes from string file
     */
    fun createDeepLinkIntent(intent: Intent) {
        when (intent.extras?.get(DESTINATION_KEY)) {
            DESTINATION_VALUE_PRIVACY -> {
                context.components.appStore.dispatch(
                    AppAction.OpenSettings(
                        page = Screen.Settings.Page.Privacy
                    )
                )
            }
            else -> {
                Log.e(TAG, "$DESTINATION_KEY not supported")
            }
        }
    }
}
