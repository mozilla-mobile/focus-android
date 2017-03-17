/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.web;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.webkit.NestedWebView;
import org.mozilla.focus.webkit.TrackingProtectionWebViewClient;

/**
 * WebViewProvider for creating a WebKit based IWebVIew implementation.
 */
public class WebViewProvider {
    /**
     * Preload webview data. This allows the webview implementation to load resources and other data
     * it might need, in advance of intialising the view (at which time we are probably wanting to
     * show a website immediately).
     */
    public static void preload(final Context context) {
        TrackingProtectionWebViewClient.triggerPreload(context);
    }

    public static View create(Context context, AttributeSet attrs) {
        final WebkitView webkitView = new WebkitView(context, attrs);

        setupView(webkitView);
        configureSettings(context, webkitView.getSettings());

        return webkitView;
    }

    private static void setupView(WebView webView) {
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(true);
    }

    @SuppressLint("SetJavaScriptEnabled") // We explicitly want to enable JavaScript
    private static void configureSettings(Context context, WebSettings settings) {
        final Settings appSettings = new Settings(context);

        settings.setJavaScriptEnabled(true);

        // Enabling built in zooming shows the controls by default
        settings.setBuiltInZoomControls(true);

        // So we hide the controls after enabling zooming
        settings.setDisplayZoomControls(false);

        // Disable access to arbitrary local files by webpages - assets can still be loaded
        // via file:///android_asset/res, so at least error page images won't be blocked.
        settings.setAllowFileAccess(false);

        settings.setBlockNetworkImage(appSettings.shouldBlockImages());
    }

    private static class WebkitView extends NestedWebView implements IWebView {
        private Callback callback;
        private TrackingProtectionWebViewClient client;

        public WebkitView(Context context, AttributeSet attrs) {
            super(context, attrs);

            client = createWebViewClient();

            setWebViewClient(client);
            setWebChromeClient(createWebChromeClient());

            if (BuildConfig.DEBUG) {
                setWebContentsDebuggingEnabled(true);
            }
        }

        @Override
        public void setCallback(Callback callback) {
            this.callback = callback;
        }

        public void loadUrl(String url) {
            // We need to check external URL handling here - shouldOverrideUrlLoading() is only
            // called by webview when clicking on a link, and not when opening a new page for the
            // first time using loadUrl().
            if (!client.shouldOverrideUrlLoading(this, url)) {
                super.loadUrl(url);
            }

            client.notifyCurrentURL(url);
        }

        @Override
        public void cleanup() {
            clearFormData();
            clearHistory();
            clearMatches();
            clearSslPreferences();
            clearCache(true);

            // We don't care about the callback - we just want to make sure cookies are gone
            CookieManager.getInstance().removeAllCookies(null);

            WebStorage.getInstance().deleteAllData();

            final WebViewDatabase webViewDatabase = WebViewDatabase.getInstance(getContext());
            // It isn't entirely clear how this differs from WebView.clearFormData()
            webViewDatabase.clearFormData();
            webViewDatabase.clearHttpAuthUsernamePassword();
        }

        private TrackingProtectionWebViewClient createWebViewClient() {
            return new TrackingProtectionWebViewClient(getContext().getApplicationContext()) {
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    if (callback != null) {
                        callback.onPageStarted(url);
                    }
                    super.onPageStarted(view, url, favicon);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    if (callback != null) {
                        callback.onPageFinished(view.getCertificate() != null);
                    }
                    super.onPageFinished(view, url);
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    // shouldOverrideUrlLoading() is called for both the main frame, and iframes.
                    // That can get problematic if an iframe tries to load an unsupported URL.
                    // We then try to either handle that URL (ask to open relevant app), or extract
                    // a fallback URL from the intent (or worst case fall back to an error page). In the
                    // latter 2 cases, we explicitly open the fallback/error page in the main view.
                    // Websites probably shouldn't use unsupported URLs in iframes, but we do need to
                    // be careful to handle all valid schemes here to avoid redirecting due to such an iframe
                    // (e.g. we don't want to redirect to a data: URI just because an iframe shows such
                    // a URI).
                    // (The API 24+ version of shouldOverrideUrlLoading() lets us determine whether
                    // the request is for the main frame, and if it's not we could then completely
                    // skip the external URL handling.)
                    if ((!url.startsWith("http://")) &&
                            (!url.startsWith("https://")) &&
                            (!url.startsWith("file://")) &&
                            (!url.startsWith("data:"))) {
                        callback.handleExternalUrl(url);
                        return true;
                    }

                    return super.shouldOverrideUrlLoading(view, url);
                }
            };
        }

        private WebChromeClient createWebChromeClient() {
            return new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    if (callback != null) {
                        callback.onProgress(newProgress);
                    }
                }
            };
        }
    }
}
