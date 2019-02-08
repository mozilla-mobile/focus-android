package org.mozilla.focus.webview

import android.net.Uri
import android.os.StrictMode
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.junit.Assert.assertNull
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

// IMPORTANT NOTE - IF RUNNING TESTS USING ANDROID STUDIO:
// Read the following for a guide on how to get AS to correctly load resources, if you don't
// do this you will get Resource Loading exceptions:
// http://robolectric.org/getting-started/#note-for-linux-and-mac-users
@RunWith(RobolectricTestRunner::class)
@Config(packageName = "org.mozilla.focus")
class TrackingProtectionWebViewClientTest {
    private var trackingProtectionWebViewClient: TrackingProtectionWebViewClient? = null
    private var webView: WebView? = null

    @Before
    fun setup() {
        trackingProtectionWebViewClient =
            TrackingProtectionWebViewClient(RuntimeEnvironment.application)

        webView = mock(WebView::class.java)
        `when`(webView?.getContext()).thenReturn(RuntimeEnvironment.application)
    }

    @After
    fun cleanup() {
        // Reset strict mode: for every test, Robolectric will create FocusApplication again.
        // FocusApplication expects strict mode to be disabled (since it loads some preferences from disk),
        // before enabling it itself. If we run multiple tests, strict mode will stay enabled
        // and FocusApplication crashes during initialisation for the second test.
        // This applies across multiple Test classes, e.g. DisconnectTest can cause
        // TrackingProtectionWebViewCLientTest to fail, unless it clears StrictMode first.
        // (FocusApplicaiton is initialised before @Before methods are run.)
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().build())
    }

    @Test
    @Throws(Exception::class)
    fun shouldInterceptRequest() {
        trackingProtectionWebViewClient?.notifyCurrentURL("http://www.mozilla.org")
        // Just some generic sanity checks that a definitely not blocked domain can be loaded, and
        // definitely blocked domains can't be
        run {
            val request = createRequest("http://mozilla.org/about", false)
            val response =
                trackingProtectionWebViewClient?.shouldInterceptRequest(webView, request)
            assertResourceAllowed(response)
        }

        run {
            val request = createRequest("http://trackersimulator.org/foobar", false)
            val response =
                trackingProtectionWebViewClient?.shouldInterceptRequest(webView, request)
            assertResourceBlocked(response)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testMainFrameAllowed() {
        trackingProtectionWebViewClient?.notifyCurrentURL("http://mozilla.org")
        // Blocked sites can still be loaded if opened as the main frame
        run {
            val request = createRequest("http://trackersimulator.org/foobar", true)
            val response =
                trackingProtectionWebViewClient?.shouldInterceptRequest(webView, request)
            assertResourceAllowed(response)
        }
        // And when we're loading that site, it can load first-party resources
        trackingProtectionWebViewClient?.notifyCurrentURL("http://trackersimulator.org")
        run {
            val request = createRequest("http://trackersimulator.org/other.js", false)
            val response =
                trackingProtectionWebViewClient?.shouldInterceptRequest(webView, request)
            assertResourceAllowed(response)
        }
        // But other sites still can't load it:
        trackingProtectionWebViewClient?.notifyCurrentURL("http://mozilla.org")
        run {
            val request = createRequest("http://trackersimulator.org/foobar", false)
            val response =
                trackingProtectionWebViewClient?.shouldInterceptRequest(webView, request)
            assertResourceBlocked(response)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testFaviconBlocked() {
        trackingProtectionWebViewClient?.notifyCurrentURL("http://www.mozilla.org")

        run {
            // Webkit tries to load favicon.ico, even though it isn't used:
            val request = createRequest("http://mozilla.org/favicon.ico", false)
            val response =
                trackingProtectionWebViewClient?.shouldInterceptRequest(webView, request)
            assertResourceBlocked(response)
        }

        run {
            // But we can't block other images since they might be used by actual pages
            val request = createRequest("http://mozilla.org/favicon.png", false)
            val response =
                trackingProtectionWebViewClient?.shouldInterceptRequest(webView, request)
            assertResourceAllowed(response)
        }
    }

    private fun assertResourceAllowed(response: WebResourceResponse) {
        // shouldInterceptRequest returns null to indicate that WebView should just load the resource
        assertNull(response)
    }

    private fun assertResourceBlocked(response: WebResourceResponse) {
        // shouldInterceptRequest a valid response in other cases, e.g. with null data to indicate
        // a blocked resource.
        assertNull(response.getData())
    }

    private fun createRequest(url: String, isForMainFrame: Boolean): WebResourceRequest {
        return object : WebResourceRequest() {
            val url: Uri
                @Override
                get() = Uri.parse(url)
            val isForMainFrame: Boolean
                @Override
                get() = isForMainFrame
            val isRedirect: Boolean
                @Override
                get() = false
            val method: String?
                @Override
                get() = null
            val requestHeaders: Map<String, String>?
                @Override
                get() = null

            @Override
            fun hasGesture(): Boolean {
                return false
            }
        }
    }
}