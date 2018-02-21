/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.web;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.mozilla.focus.R;
import org.mozilla.focus.session.Session;
import org.mozilla.gecko.GeckoSession;
import org.mozilla.gecko.GeckoSessionSettings;
import org.mozilla.gecko.GeckoView;

import java.util.Locale;

/**
 * WebViewProvider implementation for creating a Gecko based implementation of IWebView.
 */
public class WebViewProvider {
    public static void preload(final Context context) {
        GeckoSession.preload(context);
    }

    public static View create(Context context, AttributeSet attrs) {
        final GeckoView geckoView = new GeckoWebView(context, attrs);
        return geckoView;
    }

    public static void performCleanup(final Context context) {
        // Nothing: does Gecko need extra private mode cleanup?
    }

    public static void performNewBrowserSessionCleanup() {
        // Nothing: a WebKit work-around.
    }

    public static class GeckoWebView extends NestedGeckoView implements IWebView, SharedPreferences.OnSharedPreferenceChangeListener {
        private static final String LOGTAG = "GeckoWebView";
        private Callback callback;
        private String currentUrl = "about:blank";
        private boolean canGoBack;
        private boolean canGoForward;
        private boolean isSecure;
        private GeckoSession geckoSession;
        private String webViewTitle;
        private boolean socialTrackersBlocked;
        private boolean adTrackersBlocked;
        private boolean analyticTrackersBlocked;
        private boolean contentTrackersBlocked;

        public GeckoWebView(Context context, AttributeSet attrs) {
            super(context, attrs);

            final GeckoSessionSettings settings = new GeckoSessionSettings();
            settings.setBoolean(GeckoSessionSettings.USE_MULTIPROCESS, false);
            settings.setBoolean(GeckoSessionSettings.USE_PRIVATE_MODE, true);

            geckoSession = new GeckoSession(settings);

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            context.getSharedPreferences(context.getResources().getString(R.string.pref_key_privacy_block_social), Context.MODE_PRIVATE);
            socialTrackersBlocked = prefs.getBoolean(context.getResources().getString(R.string.pref_key_privacy_block_social), true);
            analyticTrackersBlocked = prefs.getBoolean(context.getResources().getString(R.string.pref_key_privacy_block_analytics), true);
            adTrackersBlocked = prefs.getBoolean(context.getResources().getString(R.string.pref_key_privacy_block_ads), true);
            contentTrackersBlocked = prefs.getBoolean(context.getResources().getString(R.string.pref_key_privacy_block_other), false);
            updateBlocking();
            prefs.registerOnSharedPreferenceChangeListener(this);

            geckoSession.setContentListener(createContentListener());
            geckoSession.setProgressListener(createProgressListener());
            geckoSession.setNavigationListener(createNavigationListener());
            geckoSession.setTrackingProtectionDelegate(createTrackingProtectionDelegate());
            geckoSession.setPromptDelegate(createPromptDelegate());
            geckoSession.setPermissionDelegate(createPermissionDelegate());
            setSession(geckoSession);
        }

        @Override
        public void setCallback(Callback callback) {
            this.callback = callback;
        }

        @Override
        public void onPause() {

        }

        @Override
        public void goBack() {
            geckoSession.goBack();
        }

        @Override
        public void goForward() {
            geckoSession.goForward();
        }

        @Override
        public void reload() {
            geckoSession.reload();
        }

        @Override
        public void destroy() {

        }

        @Override
        public void onResume() {
        }

        @Override
        public void stopLoading() {
            geckoSession.stop();
            callback.onPageFinished(isSecure);
        }

        @Override
        public String getUrl() {
            return currentUrl;
        }

        @Override
        public void loadUrl(final String url) {
            currentUrl = url;
            geckoSession.loadUri(currentUrl);
            callback.onProgress(10);
        }

        @Override
        public void cleanup() {
            // We're running in a private browsing window, so nothing to do
        }

        @Override
        public void setBlockingEnabled(boolean enabled) {
            if (enabled) {
                updateBlocking();
            } else {
                if (geckoSession != null) {
                    geckoSession.disableTrackingProtection();
                }
            }
            if (callback != null) {
                callback.onBlockingStateChanged(enabled);
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String prefName) {
            if (!prefName.isEmpty() && sharedPreferences != null) {
                if (prefName.equals(getContext().getResources().getString(R.string.pref_key_privacy_block_social))) {
                    socialTrackersBlocked = sharedPreferences.getBoolean(prefName, true);
                } else if (prefName.equals(getContext().getResources().getString(R.string.pref_key_privacy_block_ads))) {
                    adTrackersBlocked = sharedPreferences.getBoolean(prefName, true);
                } else if (prefName.equals(getContext().getResources().getString(R.string.pref_key_privacy_block_analytics))) {
                    analyticTrackersBlocked = sharedPreferences.getBoolean(prefName, true);
                } else if (prefName.equals(getContext().getResources().getString(R.string.pref_key_privacy_block_other))) {
                    contentTrackersBlocked = sharedPreferences.getBoolean(prefName, false);
                }
            }
            updateBlocking();
        }

        private void updateBlocking() {
            int categories = 0;
            if (socialTrackersBlocked) {
                categories += GeckoSession.TrackingProtectionDelegate.CATEGORY_SOCIAL;
            }
            if (adTrackersBlocked) {
                categories += GeckoSession.TrackingProtectionDelegate.CATEGORY_AD;
            }
            if (analyticTrackersBlocked) {
                categories += GeckoSession.TrackingProtectionDelegate.CATEGORY_ANALYTIC;
            }
            if (contentTrackersBlocked) {
                categories += GeckoSession.TrackingProtectionDelegate.CATEGORY_CONTENT;
            }
            if (geckoSession != null) {
                geckoSession.enableTrackingProtection(categories);
            }
        }

        private GeckoSession.ContentListener createContentListener() {
            return new GeckoSession.ContentListener() {
                @Override
                public void onTitleChange(GeckoSession session, String title) {
                    webViewTitle = title;
                }

                @Override
                public void onFullScreen(GeckoSession session, boolean fullScreen) {
                    if (fullScreen) {
                        callback.onEnterFullScreen(new FullscreenCallback() {
                            @Override
                            public void fullScreenExited() {
                                geckoSession.exitFullScreen();
                            }
                        }, null);
                    } else {
                        callback.onExitFullScreen();
                    }
                }

                @Override
                public void onContextMenu(GeckoSession session, int screenX, int screenY, String uri, String elementSrc) {
                    if (elementSrc != null && uri != null) {
                        callback.onLongPress(new HitTarget(true, uri, true, elementSrc));
                    } else if (elementSrc != null) {
                        callback.onLongPress(new HitTarget(false, null, true, elementSrc));
                    } else if (uri != null) {
                        callback.onLongPress(new HitTarget(true, uri, false, null));
                    }
                }

                @Override
                public void onFocusRequest(GeckoSession geckoSession) {
                }
            };
        }

        private GeckoSession.ProgressListener createProgressListener() {
            return new GeckoSession.ProgressListener() {
                @Override
                public void onPageStart(GeckoSession session, String url) {
                    if (callback != null) {
                        callback.onPageStarted(url);
                        callback.resetBlockedTrackers();
                        callback.onProgress(25);
                        isSecure = false;
                    }
                }

                @Override
                public void onPageStop(GeckoSession session, boolean success) {
                    if (callback != null) {
                        if (success) {
                            callback.onProgress(100);
                            callback.onPageFinished(isSecure);
                        }
                    }
                }

                @Override
                public void onSecurityChange(GeckoSession session,
                                             GeckoSession.ProgressListener.SecurityInformation securityInfo) {
                    // TODO: Split current onPageFinished() callback into two: page finished + security changed
                    isSecure = securityInfo.isSecure;
                }
            };
        }

        private GeckoSession.NavigationListener createNavigationListener() {
            return new GeckoSession.NavigationListener() {
                public void onLocationChange(GeckoSession session, String url) {
                    currentUrl = url;
                    if (callback != null) {
                        callback.onURLChanged(url);
                    }
                }

                public void onCanGoBack(GeckoSession session, boolean canGoBack) {
                    GeckoWebView.this.canGoBack = canGoBack;
                }

                public void onCanGoForward(GeckoSession session, boolean canGoForward) {
                    GeckoWebView.this.canGoForward = canGoForward;
                }

                @Override
                public boolean onLoadUri(GeckoSession session, String uri, GeckoSession.NavigationListener.TargetWindow where) {
                    // If this is trying to load in a new tab, just load it in the current one
                    if (where == GeckoSession.NavigationListener.TargetWindow.NEW) {
                        geckoSession.loadUri(uri);
                        return true;
                    }

                    // Check if we should handle an internal link
                    if (LocalizedContentGecko.INSTANCE.handleInternalContent(uri, session, getContext())) {
                        return true;
                    }

                    // Otherwise allow the load to continue normally
                    return false;
                }
            };
        }

        private GeckoSession.TrackingProtectionDelegate createTrackingProtectionDelegate() {
            return new GeckoSession.TrackingProtectionDelegate() {
                @Override
                public void onTrackerBlocked(GeckoSession geckoSession, String s, int i) {
                    if (callback != null) {
                        callback.countBlockedTracker();
                    }
                }
            };
        }

        private GeckoSession.PermissionDelegate createPermissionDelegate() {
            return new GeckoSession.PermissionDelegate() {

                public int androidPermissionRequestCode = 2;
                private Callback mCallback;

                public void onRequestPermissionsResult(final String[] permissions,
                                                       final int[] grantResults) {
                    if (mCallback == null) {
                        return;
                    }

                    final Callback cb = mCallback;
                    mCallback = null;
                    for (final int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            // At least one permission was not granted.
                            cb.reject();
                            return;
                        }
                    }
                    cb.grant();
                }

                @Override
                public void requestAndroidPermissions(GeckoSession session, String[] permissions, Callback callback) {
                    if (Build.VERSION.SDK_INT < 23) {
                        // requestPermissions was introduced in API 23.
                        callback.grant();
                    } else {
                        mCallback = callback;
                        ((Activity) getContext()).requestPermissions(permissions, androidPermissionRequestCode);
                    }
                }

                @Override
                public void requestContentPermission(GeckoSession session, String uri, String type, String access, Callback callback) {
                    final int resId;
                    if ("geolocation".equals(type)) {
                        resId = R.string.request_geolocation;
                    } else if ("desktop-notification".equals(type)) {
                        resId = R.string.request_notification;
                    } else {
                        Log.w(LOGTAG, "Unknown permission: " + type);
                        callback.reject();
                        return;
                    }

                    final String title = getContext().getString(resId, Uri.parse(uri).getAuthority());
                    final GeckoViewPrompt prompt = (GeckoViewPrompt)
                            geckoSession.getPromptDelegate();
                    prompt.promptForPermission(session, title, callback);
                }

                private void normalizeMediaName(final MediaSource[] sources) {
                    if (sources == null) {
                        return;
                    }
                    for (final MediaSource source : sources) {
                        final int mediaSource = source.source;
                        String name = source.name;
                        if (MediaSource.SOURCE_CAMERA == mediaSource) {
                            if (name.toLowerCase(Locale.ENGLISH).contains("front")) {
                                name = getContext().getString(R.string.media_front_camera);
                            } else {
                                name = getContext().getString(R.string.media_back_camera);
                            }
                        } else if (!name.isEmpty()) {
                            continue;
                        } else if (MediaSource.SOURCE_MICROPHONE == mediaSource) {
                            name = getContext().getString(R.string.media_microphone);
                        } else {
                            name = getContext().getString(R.string.media_other);
                        }
                        source.name = name;
                    }
                }

                @Override
                public void requestMediaPermission(GeckoSession session, String uri, MediaSource[] video, MediaSource[] audio, MediaCallback callback) {
                    final String host = Uri.parse(uri).getAuthority();
                    final String title;
                    if (audio == null) {
                        title = getContext().getString(R.string.request_video, host);
                    } else if (video == null) {
                        title = getContext().getString(R.string.request_audio, host);
                    } else {
                        title = getContext().getString(R.string.request_media, host);
                    }

                    normalizeMediaName(video);
                    normalizeMediaName(audio);

                    final GeckoViewPrompt prompt = (GeckoViewPrompt)
                            geckoSession.getPromptDelegate();
                    prompt.promptForMedia(session, title, video, audio, callback);
                }
            };
        }

        private GeckoSession.PromptDelegate createPromptDelegate() {
            return new GeckoViewPrompt((Activity) getContext());
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
            return webViewTitle;
        }

        @Override
        public void exitFullscreen() {
            geckoSession.exitFullScreen();
        }
    }
}
