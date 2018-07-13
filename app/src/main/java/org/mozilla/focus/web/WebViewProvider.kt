/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.web

import android.content.Context
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.view.View
import android.webkit.WebSettings
import org.mozilla.focus.web.Renderers.WEBVIEW
import org.mozilla.focus.web.Renderers.GECKO
import org.mozilla.focus.webview.SystemWebView

const val ENGINE_PREF_STRING_KEY = "engine_choice"

object WebViewProvider : IWebViewProvider {
    var engineName: String = Config.renderer.name
    var engine : IWebViewProvider? = null

    fun writeEnginePref(context: Context, newEngineName: String = engineName) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(ENGINE_PREF_STRING_KEY, newEngineName).apply()
    }

    fun readEnginePref(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        engineName = prefs.getString(ENGINE_PREF_STRING_KEY, Config.renderer.name)
        useEngine(engineName)
    }

    private fun useEngine(name: String) {
        when (name) {
            WEBVIEW.name -> engine = ClassicWebViewProvider()
            GECKO.name -> engine = GeckoWebViewProvider()
        }
    }

    override fun preload(context: Context) {
        engine!!.preload(context)
    }

    override fun create(context: Context, attributeSet: AttributeSet?): View {
        return engine!!.create(context, attributeSet)
    }

    override fun performCleanup(context: Context) {
        engine!!.performCleanup(context)
    }

    override fun performNewBrowserSessionCleanup() {
        engine!!.performNewBrowserSessionCleanup()
    }

    override fun requestMobileSite(context: Context, webSettings: WebSettings) {
        engine!!.requestMobileSite(context, webSettings)
    }

    override fun requestDesktopSite(webSettings: WebSettings) {
        engine!!.requestDesktopSite(webSettings)
    }

    override fun applyAppSettings(context: Context, webSettings: WebSettings, systemWebView: SystemWebView) {
        engine!!.applyAppSettings(context, webSettings, systemWebView)
    }

    override fun disableBlocking(webSettings: WebSettings, systemWebView: SystemWebView) {
        engine!!.disableBlocking(webSettings, systemWebView)
    }
}