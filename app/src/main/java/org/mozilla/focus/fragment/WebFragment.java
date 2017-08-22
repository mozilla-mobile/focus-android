/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import org.mozilla.focus.R;
import org.mozilla.focus.locale.LocaleAwareFragment;
import org.mozilla.focus.locale.LocaleManager;
import org.mozilla.focus.session.Session;
import org.mozilla.focus.web.IWebView;

import java.util.Locale;

/**
 * Base implementation for fragments that use an IWebView instance. Based on Android's WebViewFragment.
 */
public abstract class WebFragment extends LocaleAwareFragment {
    private IWebView webView;
    private boolean isWebViewAvailable;

    /**
     * Inflate a layout for this fragment. The layout needs to contain a view implementing IWebView
     * with the id set to "webview".
     */
    @NonNull
    public abstract View inflateLayout(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

    public abstract IWebView.Callback createCallback();

    public abstract Session getSession();

    /**
     * Get the initial URL to load after the view has been created.
     */
    @Nullable
    public abstract String getInitialUrl();

    /**
     * Adds ability to add methods to onCreateView without override because onCreateView is final.
     */
    public abstract void onCreateViewCalled();

    @Override
    public final View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflateLayout(inflater, container, savedInstanceState);

        webView = (IWebView) view.findViewById(R.id.webview);
        isWebViewAvailable = true;
        webView.setCallback(createCallback());

        if (savedInstanceState == null) {
            final String url = getInitialUrl();
            if (url != null) {
                webView.loadUrl(url);
            }
        } else {
            webView.restoreWebViewState(getSession(), savedInstanceState);
        }

        onCreateViewCalled();
        return view;
    }

    @Override
    public void applyLocale() {
        Context context = getContext();
        final LocaleManager localeManager = LocaleManager.getInstance();
        if (!localeManager.isMirroringSystemLocale(context)) {
            final Locale currentLocale = localeManager.getCurrentLocale(context);
            Locale.setDefault(currentLocale);
            final Resources resources = context.getResources();
            final Configuration config = resources.getConfiguration();
            config.setLocale(currentLocale);
            context.getResources().updateConfiguration(config, null);
        }
        // We create and destroy a new WebView here to force the internal state of WebView to know
        // about the new language. See issue #666.
        final WebView unneeded = new WebView(getContext());
        unneeded.destroy();
    }

    @Override
    public void onPause() {
        webView.onPause();

        super.onPause();
    }

    @Override
    public void onResume() {
        webView.onResume();

        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        webView.saveWebViewState(getSession(), outState);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        if (webView != null) {
            webView.setCallback(null);
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        isWebViewAvailable = false;

        super.onDestroyView();
    }

    @Nullable
    protected IWebView getWebView() {
        return isWebViewAvailable ? webView : null;
    }
}
