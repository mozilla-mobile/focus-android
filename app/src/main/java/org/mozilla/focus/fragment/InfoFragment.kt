/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.os.bundleOf
import mozilla.components.browser.session.Session

import org.mozilla.focus.R
import org.mozilla.focus.web.Download
import org.mozilla.focus.web.IWebView

class InfoFragment : WebFragment() {
    private var progressView: ProgressBar? = null
    private var webView: View? = null

    override val session: Session?
        get() = null

    override val initialUrl: String?
        get() = arguments?.getString(ARGUMENT_URL)

    override fun inflateLayout(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_info, container, false)
        progressView = view.findViewById(R.id.progress)
        webView = view.findViewById<View?>(R.id.webview)
        val url = initialUrl
        if (url != null && !(url.startsWith("http://") || url.startsWith("https://"))) {
            // Make webview background transparent if we're loading built in about/rights/etc
            // pages: this avoid a white flash (on slower devices) between the screen appearing,
            // and the about/right/etc content appearing. We don't do this for SUMO and other
            // external pages, because they are both light-coloured, and significantly slower loading.
            webView?.setBackgroundColor(Color.TRANSPARENT)
        }
        applyLocale()
        return view
    }

    override fun onCreateViewCalled() {}

    @Suppress("ComplexMethod")
    override fun createCallback(): IWebView.Callback {
        return object : IWebView.Callback {
            override fun onPageStarted(url: String) {
                progressView?.apply {
                    announceForAccessibility(getString(R.string.accessibility_announcement_loading))
                    visibility = View.VISIBLE
                }
            }

            override fun onPageFinished(isSecure: Boolean) {
                progressView?.apply {
                    announceForAccessibility(getString(R.string.accessibility_announcement_loading_finished))
                    visibility = View.INVISIBLE
                }
            }

            override fun onSecurityChanged(
                isSecure: Boolean,
                host: String?,
                organization: String?
            ) {
            }

            override fun onProgress(progress: Int) {
                progressView?.progress = progress
            }

            override fun onDownloadStart(download: Download) {}

            override fun onLongPress(hitTarget: IWebView.HitTarget) {}

            override fun onURLChanged(url: String) {}

            override fun onTitleChanged(title: String) {}

            override fun onRequest(isTriggeredByUserGesture: Boolean) {}

            override fun onEnterFullScreen(callback: IWebView.FullscreenCallback, view: View?) {}

            override fun onExitFullScreen() {}

            override fun countBlockedTracker() {}

            override fun resetBlockedTrackers() {}

            override fun onBlockingStateChanged(isBlockingEnabled: Boolean) {}

            override fun onHttpAuthRequest(
                callback: IWebView.HttpAuthCallback,
                host: String,
                realm: String
            ) {}

            override fun onRequestDesktopStateChanged(shouldRequestDesktop: Boolean) {}
        }
    }

    companion object {
        private const val ARGUMENT_URL = "url"

        fun create(url: String): InfoFragment {
            return InfoFragment().apply {
                arguments = bundleOf(ARGUMENT_URL to url)
            }
        }
    }
}
