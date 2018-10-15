/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.os.StrictMode
import android.support.v7.preference.PreferenceManager
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withTimeoutOrNull
import mozilla.components.service.fretboard.Fretboard
import mozilla.components.service.fretboard.source.kinto.KintoExperimentSource
import mozilla.components.service.fretboard.storage.flatfile.FlatFileExperimentStorage
import org.mozilla.focus.locale.LocaleAwareApplication
import org.mozilla.focus.session.NotificationSessionObserver
import org.mozilla.focus.session.VisibilityLifeCycleCallback
import org.mozilla.focus.telemetry.SentryWrapper
import org.mozilla.focus.telemetry.TelemetrySessionObserver
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.AdjustHelper
import org.mozilla.focus.utils.AppConstants
import org.mozilla.focus.utils.EXPERIMENTS_BASE_URL
import org.mozilla.focus.utils.EXPERIMENTS_BUCKET_NAME
import org.mozilla.focus.utils.EXPERIMENTS_COLLECTION_NAME
import org.mozilla.focus.utils.EXPERIMENTS_JSON_FILENAME
import org.mozilla.focus.utils.StethoWrapper
import org.mozilla.focus.utils.TLDExtractor
import org.mozilla.focus.web.CleanupSessionObserver
import org.mozilla.focus.web.WebViewProvider
import java.io.File
import java.util.concurrent.Executors

val IO: CoroutineDispatcher by lazy {
    Executors.newCachedThreadPool().asCoroutineDispatcher()
}

class FocusApplication : LocaleAwareApplication() {
    lateinit var fretboard: Fretboard

    companion object {
        private const val FRETBOARD_BLOCKING_NETWORK_READ_TIMEOUT = 10000
    }

    val components: Components by lazy { Components() }

    var visibilityLifeCycleCallback: VisibilityLifeCycleCallback? = null
        private set

    override fun onCreate() {
        super.onCreate()

        SentryWrapper.init(this)
        StethoWrapper.init(this)

        PreferenceManager.setDefaultValues(this, R.xml.settings, false)

        runBlocking {
            loadExperiments()

            enableStrictMode()

            components.searchEngineManager.apply {
                launch(IO) {
                    load(this@FocusApplication)
                }

                registerForLocaleUpdates(this@FocusApplication)
            }

            TelemetryWrapper.init(this@FocusApplication)
            AdjustHelper.setupAdjustIfNeeded(this@FocusApplication)
            launch {
                TLDExtractor.init(this@FocusApplication)
            }

            visibilityLifeCycleCallback = VisibilityLifeCycleCallback(this@FocusApplication)
            registerActivityLifecycleCallbacks(visibilityLifeCycleCallback)

            components.sessionManager.apply {
                register(NotificationSessionObserver(this@FocusApplication))
                register(TelemetrySessionObserver())
                register(CleanupSessionObserver(this@FocusApplication))
            }
        }
    }

    private suspend fun loadExperiments() {
        val experimentsFile = File(filesDir, EXPERIMENTS_JSON_FILENAME)
        val experimentSource = KintoExperimentSource(
                EXPERIMENTS_BASE_URL, EXPERIMENTS_BUCKET_NAME, EXPERIMENTS_COLLECTION_NAME)
        fretboard = Fretboard(experimentSource, FlatFileExperimentStorage(experimentsFile))
        fretboard.loadExperiments()
        WebViewProvider.determineEngine(this@FocusApplication)
        launch(IO) {
            withTimeoutOrNull(FRETBOARD_BLOCKING_NETWORK_READ_TIMEOUT) {
                fretboard.updateExperiments() // then update disk and memory from the network
            }
        }
    }

    private fun enableStrictMode() {
        // Android/WebView sometimes commit strict mode violations, see e.g.
        // https://github.com/mozilla-mobile/focus-android/issues/660
        if (AppConstants.isReleaseBuild) {
            return
        }

        val threadPolicyBuilder = StrictMode.ThreadPolicy.Builder().detectAll()
        val vmPolicyBuilder = StrictMode.VmPolicy.Builder().detectAll()

        threadPolicyBuilder.penaltyLog()
        vmPolicyBuilder.penaltyLog()

        StrictMode.setThreadPolicy(threadPolicyBuilder.build())
        StrictMode.setVmPolicy(vmPolicyBuilder.build())
    }
}
