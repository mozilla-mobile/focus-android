package org.mozilla.focus.components

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import mozilla.components.browser.engine.gecko.GeckoEngine
import mozilla.components.concept.engine.*
import mozilla.components.concept.engine.utils.EngineVersion
import mozilla.components.lib.crash.handler.CrashHandlerService
import org.json.JSONObject
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

object EngineProvider {

    private var runtime: GeckoRuntime? = null

    @Synchronized
    private fun getOrCreateRuntime(context: Context): GeckoRuntime {
        if (runtime == null) {
            val builder = GeckoRuntimeSettings.Builder()

            builder.crashHandler(CrashHandlerService::class.java)
            builder.aboutConfigEnabled(true)

            runtime = GeckoRuntime.create(context, builder.build())
        }

        return runtime!!
    }

    fun createEngine(context: Context, defaultSettings: DefaultSettings): Engine {
        val runtime = getOrCreateRuntime(context)

        return EngineWrapper(GeckoEngine(context, defaultSettings, runtime).also {
            // WebCompatFeature.install(it)
        })
    }

    /*
    fun createClient(context: Context): Client {
        val runtime = getOrCreateRuntime(context)
        return GeckoViewFetchClient(context, runtime)
    }
     */
}

private class EngineWrapper(
    private val actual: Engine
) : Engine {
    override val settings: Settings
        get() = actual.settings

    override val version: EngineVersion
        get() = actual.version

    override fun createSession(private: Boolean, contextId: String?): EngineSession {
        return actual.createSession(private, contextId)
    }

    override fun createSessionState(json: JSONObject): EngineSessionState {
        return actual.createSessionState(json)
    }

    override fun createView(context: Context, attrs: AttributeSet?): EngineView {
        return actual.createView(context, attrs)
    }

    override fun name(): String {
        return actual.name()
    }

    override fun speculativeConnect(url: String) {
        Log.w("SKDBG", "Speculative connect: $url")
        return actual.speculativeConnect(url)
    }

}
