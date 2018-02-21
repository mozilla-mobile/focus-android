/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.web;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;

import org.mozilla.focus.session.Session;
import org.mozilla.gecko.GeckoView;
import org.mozilla.gecko.GeckoViewSettings;

/**
 * WebViewProvider implementation for creating a Gecko based implementation of IWebView.
 */
public class WebViewProvider {
    public static void preload(final Context context) {
        GeckoView.preload(context);
    }

    public static View create(Context context, AttributeSet attrs) {
        final GeckoViewSettings settings = new GeckoViewSettings();
        settings.setBoolean(GeckoViewSettings.USE_MULTIPROCESS, false);
        settings.setBoolean(GeckoViewSettings.USE_PRIVATE_MODE, true);
        settings.setBoolean(GeckoViewSettings.USE_TRACKING_PROTECTION, true);
        final GeckoView geckoView = new GeckoWebView(context, attrs, settings);

        return geckoView;
    }

    public static void performCleanup(final Context context) {
        // Gecko doesn't need private mode cleanup
    }

    public static void performNewBrowserSessionCleanup() {
        // Nothing: a WebKit work-around.
    }

    public static class GeckoWebView extends NestedGeckoView implements IWebView {
        private Callback callback;
        private String currentUrl = "about:blank";
        private boolean canGoBack;
        private boolean canGoForward;
        private boolean isSecure;
        private String title;

        public GeckoWebView(Context context, AttributeSet attrs, GeckoViewSettings settings) {
            super(context, attrs, settings);

            setContentListener(createContentListener());
            setProgressListener(createProgressListener());
            setNavigationListener(createNavigationListener());
        }

        @Override
        public void setCallback(Callback callback) {
            this.callback =  callback;
        }

        @Override
        public void onPause() {

        }

        @Override
        public void onResume() {

        }

        @Override
        public void stopLoading() {
            this.stop();
            callback.onPageFinished(isSecure);
        }

        @Override
        public String getUrl() {
            return currentUrl;
        }

        @Override
        public void loadUrl(final String url) {
            currentUrl = url;
            loadUri(currentUrl);
            callback.onProgress(10);
        }

        @Override
        public void cleanup() {
            // We're running in a private browsing window, so nothing to do
        }

        @Override
        public void setBlockingEnabled(boolean enabled) {
            getSettings().setBoolean(GeckoViewSettings.USE_TRACKING_PROTECTION, enabled);
        }

        private ContentListener createContentListener() {
            return new ContentListener() {
                @Override
                public void onTitleChange(GeckoView geckoView, String s) {
                    title = s;
                }

                @Override
                public void onFullScreen(GeckoView geckoView, boolean fullScreen) {
                    if (fullScreen) {
                        callback.onEnterFullScreen(new FullscreenCallback() {
                            @Override
                            public void fullScreenExited() {
                                exitFullScreen();
                            }
                        }, null);
                    } else {
                        callback.onExitFullScreen();
                    }
                }

                @Override
                public void onContextMenu(GeckoView geckoView, int i, int i1, String linkUrl, String imageUrl) {
                    callback.onLongPress(new HitTarget(linkUrl != null, linkUrl, imageUrl != null, imageUrl));
                }
            };
        }

        private ProgressListener createProgressListener() {
            return new ProgressListener() {
                @Override
                public void onPageStart(GeckoView geckoView, String url) {
                    if (callback != null) {
                        callback.onPageStarted(url);
                        callback.onProgress(25);
                        isSecure = false;
                    }
                }

                @Override
                public void onPageStop(GeckoView geckoView, boolean success) {
                    if (callback != null) {
                        if (success) {
                            callback.onProgress(100);
                            callback.onPageFinished(isSecure);
                        }
                    }
                }

                @Override
                public void onSecurityChange(GeckoView geckoView, SecurityInformation securityInformation) {
                    isSecure = securityInformation.isSecure;
                }
            };
        }

        private NavigationListener createNavigationListener() {
            return new NavigationListener() {
                public void onLocationChange(GeckoView view, String url) {
                    currentUrl = url;
                    if (callback != null) {
                        callback.onURLChanged(url);
                    }
                }

                @Override
                public boolean onLoadUri(GeckoView geckoView, String s, TargetWindow targetWindow) {
                    // If this is trying to load in a new tab, just load it in the current one
                    if (targetWindow == TargetWindow.NEW) {
                        loadUri(s);
                        return true;
                    }

                    // Otherwise allow the load to continue normally
                    return false;
                }

                public void onCanGoBack(GeckoView view, boolean canGoBack) {
                    GeckoWebView.this.canGoBack =  canGoBack;
                }

                public void onCanGoForward(GeckoView view, boolean canGoForward) {
                    GeckoWebView.this.canGoForward = canGoForward;
                }
            };
        }

        @Override
        public boolean canGoForward() {
            return canGoForward;
        }

        @Override
        public boolean canGoBack() {
            return canGoBack;
        }

        @Override
        public void restoreWebViewState(Session session) {
            // TODO: restore navigation history, and reopen previously opened page
        }

        @Override
        public void saveWebViewState(@NonNull Session session) {
            // TODO: save anything needed for navigation history restoration.
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public void exitFullscreen() {
            exitFullScreen();
        }
    }
}
