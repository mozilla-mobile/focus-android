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

object WebViewProvider {
    var engineName: String = Config.renderer.name
    var engine : IWebViewProvider? = null

    fun writeEnginePref(context: Context, newEngineName: String = engineName) {
        requireNotNull(context)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(ENGINE_PREF_STRING_KEY, newEngineName).apply()
    }

    fun readEnginePref(context: Context) {
        requireNotNull(context)
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

    fun preload(context: Context) {
        requireNotNull(context)
        requireNotNull(engine)
        engine!!.preload(context)
    }

    fun create(context: Context, attributeSet: AttributeSet?): View {
        requireNotNull(context)
        requireNotNull(engine)
        return engine!!.create(context, attributeSet)
    }

    fun performCleanup(context: Context) {
        requireNotNull(context)
        requireNotNull(engine)
        engine!!.performCleanup(context)
    }

    fun performNewBrowserSessionCleanup() {
        requireNotNull(engine)
        engine!!.performNewBrowserSessionCleanup()
    }

    fun requestMobileSite(context: Context, webSettings: WebSettings) {
        requireNotNull(context)
        requireNotNull(engine)
        engine!!.requestMobileSite(context, webSettings)
    }

    fun requestDesktopSite(webSettings: WebSettings) {
        requireNotNull(webSettings)
        requireNotNull(engine)
        engine!!.requestDesktopSite(webSettings)
    }

    fun applyAppSettings(context: Context, webSettings: WebSettings, systemWebView: SystemWebView) {
        requireNotNull(context)
        requireNotNull(engine)
        engine!!.applyAppSettings(context, webSettings, systemWebView)
    }

    fun disableBlocking(webSettings: WebSettings, systemWebView: SystemWebView) {
        requireNotNull(engine)
        engine!!.disableBlocking(webSettings, systemWebView)
    }
}