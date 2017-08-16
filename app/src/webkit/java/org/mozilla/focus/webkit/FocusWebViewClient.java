/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.webkit;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.mozilla.focus.R;
import org.mozilla.focus.locale.Locales;
import org.mozilla.focus.utils.HtmlLoader;
import org.mozilla.focus.utils.SupportUtils;
import org.mozilla.focus.utils.UrlUtils;
import org.mozilla.focus.web.IWebView;

import java.util.Map;

/**
 * WebViewClient layer that handles browser specific WebViewClient functionality, such as error pages
 * and external URL handling.
 */
/* package */ class FocusWebViewClient extends TrackingProtectionWebViewClient {
    private final static String ERROR_PROTOCOL = "error:";

    private IWebView.Callback callback;
    private boolean errorReceived;
    private Context context;

    public FocusWebViewClient(Context context) {
        super(context);
        this.context = context;
    }

    public void setCallback(IWebView.Callback callback) {
        this.callback = callback;
    }

    /**
     * Always ensure the following is wrapped in an anonymous function before execution.
     * (We don't wrap here, since this code might be run as part of a larger function, see
     * e.g. onLoadResource().)
     */
    private static final String CLEAR_VISITED_CSS =
            "let nSheets = document.styleSheets.length;" +
            "for (s=0; s < nSheets; s++) {" +
            "  let stylesheet = document.styleSheets[s];" +
            "  let nRules = stylesheet.cssRules ? stylesheet.cssRules.length : 0;" +
            // rules need to be removed by index. That modifies the whole list - it's easiest
            // to therefore process the list from the back, so that we don't need to care about
            // indexes changing after deletion (all indexes before the removed item are unchanged,
            // so by moving towards the start we'll always process all previously unprocessed items -
            // moving in the other direction we'd need to remember to process a given index
            // again which is more complicated).
            "  for (i = nRules - 1; i >= 0; i--) {" +
            "    let cssRule = stylesheet.cssRules[i];" +
            // Depending on style type, there might be no selector
            "    if (cssRule.selectorText && cssRule.selectorText.includes(':visited')) {" +
            "      let tokens = cssRule.selectorText.split(',');" +
            "      let j = tokens.length;" +
            "      while (j--) {" +
            "        if (tokens[j].includes(':visited')) {" +
            "          tokens.splice(j, 1);" +
            "        }" +
            "      }" +
            "      if (tokens.length == 0) {" +
            "        stylesheet.deleteRule(i);" +
            "      } else {" +
            "        cssRule.selectorText = tokens.join(',');" +
            "      }" +
            "    }" +
            "  }" +
            "}";

    @Override
    public void onLoadResource(WebView view, String url) {
        // We can't access the webview during shouldInterceptRequest(), however onLoadResource()
        // is called on the UI thread so we're allowed to do this now:
        view.evaluateJavascript(
                "(function() {" +

                "function cleanupVisited() {" +
                CLEAR_VISITED_CSS +
                "}" +

                // Add an onLoad() listener so that we run the cleanup script every time
                // a <link>'d css stylesheet is loaded:
                "let links = document.getElementsByTagName('link');" +
                "for (i = 0; i < links.length; i++) {" +
                "  link = links[i];" +
                "  if (link.rel == 'stylesheet') {" +
                "    link.addEventListener('load', cleanupVisited, false);" +
                "  }" +
                "}" +

                "})();",

                null);

        super.onLoadResource(view, url);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        // Only update the user visible URL if:
        // 1. The purported site URL has actually been requested
        // 2. And it's being loaded for the main frame (and not a fake/hidden/iframe request)
        // Note also: shouldInterceptRequest() runs on a background thread, so we can't actually
        // query WebView.getURL().
        // We update the URL when loading has finished too (redirects can happen after a request has been
        // made in which case we don't get shouldInterceptRequest with the final URL), but this
        // allows us to update the URL during loading.
        if (request.isForMainFrame()) {

            // WebView will always add a trailing / to the request URL, but currentPageURL may or may
            // not have a trailing URL (usually no trailing / when a link is entered via UrlInputFragment),
            // hence we do a somewhat convoluted test:
            final String requestURL = request.getUrl().toString();
            if (UrlUtils.urlsMatchExceptForTrailingSlash(currentPageURL, requestURL)) {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onURLChanged(currentPageURL);
                        }
                    }
                });
            }
        }

        return super.shouldInterceptRequest(view, request);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (errorReceived) {
            // When dealing with error pages, webkit sometimes sends onPageStarted()
            // without a matching onPageFinished(). We hack around that by using
            // a flag to ignore the first onPageStarted() after onReceivedError() has
            // been called. (The usual chain is: onPageStarted(url), onReceivedError(url),
            // onPageFinished(url), onPageStarted(url), finally and only sometimes: onPageFinished().
            // Since the final onPageFinished isn't guaranteed (and we know we're showing an error
            // page already), we don't need to send the onPageStarted() callback a second time anyway.
            errorReceived = false;
        } else if (callback != null) {
            callback.onPageStarted(url);
        }

        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, final String url) {
        if (callback != null) {
            callback.onPageFinished(view.getCertificate() != null);
            // The URL which is supplied in onPageFinished() could be fake (see #301), but webview's
            // URL is always correct _except_ for error pages
            final String viewURL = view.getUrl();
            if (!UrlUtils.isInternalErrorURL(viewURL)) {
                callback.onURLChanged(view.getUrl());
            }
        }
        super.onPageFinished(view, url);

        view.evaluateJavascript(
                "(function() {" +

                CLEAR_VISITED_CSS +

                "})();",

                null);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.equals("focusabout:")) {
            loadAbout(view);
            return true;
        }

        // Allow pages to blank themselves by loading about:blank. While it's a little incorrect to let pages
        // access our internal URLs, Chrome allows loads to about:blank and, to ensure our behavior conforms
        // to the behavior that most of the web is developed against, we do too.
        if (url.equals("about:blank")) {
            return false;
        }

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
        final Uri uri = Uri.parse(url);
        if (!UrlUtils.isSupportedProtocol(uri.getScheme()) &&
                callback != null &&
                callback.handleExternalUrl(url)) {
            return true;
        }

        return super.shouldOverrideUrlLoading(view, url);
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        handler.cancel();

        // Webkit can try to load the favicon for a bad page when you set a new URL. If we then
        // loadErrorPage() again, webkit tries to load the favicon again. We end up in onReceivedSSlError()
        // again, and we get an infinite loop of reloads (we also erroneously show the favicon URL
        // in the toolbar, but that's less noticeable). Hence we check whether this error is from
        // the desired page, or a page resource:
        if (error.getUrl().equals(currentPageURL)) {
            ErrorPage.loadErrorPage(view, error.getUrl(), WebViewClient.ERROR_FAILED_SSL_HANDSHAKE);
        }
    }

    @Override
    public void onReceivedError(final WebView webView, int errorCode,
                                final String description, String failingUrl) {
        errorReceived = true;

        // This is a hack: onReceivedError(WebView, WebResourceRequest, WebResourceError) is API 23+ only,
        // - the WebResourceRequest would let us know if the error affects the main frame or not. As a workaround
        // we just check whether the failing URL is the current URL, which is enough to detect an error
        // in the main frame.

        // WebView swallows odd pages and only sends an error (i.e. it doesn't go through the usual
        // shouldOverrideUrlLoading), so we need to handle special pages here:
        // about: urls are even more odd: webview doesn't tell us _anything_, hence the use of
        // a different prefix:
        if (failingUrl.startsWith(ERROR_PROTOCOL)) {
            // format: error:<error_code>
            final int errorCodePosition = ERROR_PROTOCOL.length();
            final String errorCodeString = failingUrl.substring(errorCodePosition);

            int desiredErrorCode;
            try {
                desiredErrorCode = Integer.parseInt(errorCodeString);

                if (!ErrorPage.supportsErrorCode(desiredErrorCode)) {
                    // I don't think there's any good way of showing an error if there's an error
                    // in requesting an error page?
                    desiredErrorCode = WebViewClient.ERROR_BAD_URL;
                }
            } catch (final NumberFormatException e) {
                desiredErrorCode = WebViewClient.ERROR_BAD_URL;
            }
            ErrorPage.loadErrorPage(webView, failingUrl, desiredErrorCode);
            return;
        }


        // The API 23+ version also return a *slightly* more usable description, via WebResourceError.getError();
        // e.g.. "There was a network error.", whereas this version provides things like "net::ERR_NAME_NOT_RESOLVED"
        if (failingUrl.equals(currentPageURL) &&
                ErrorPage.supportsErrorCode(errorCode)) {
            ErrorPage.loadErrorPage(webView, currentPageURL, errorCode);
            return;
        }

        super.onReceivedError(webView, errorCode, description, failingUrl);
    }

    private void loadAbout(final WebView webView) {
        final Resources resources = Locales.getLocalizedResources(webView.getContext());

        final Map<String, String> substitutionMap = new ArrayMap<>();
        final String appName = webView.getContext().getResources().getString(R.string.app_name);
        final String learnMoreURL = SupportUtils.getManifestoURL();

        String aboutVersion = "";
        try {
            final PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            aboutVersion = String.format("%s (Build #%s)", packageInfo.versionName, packageInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            // Nothing to do if we can't find the package name.
        }
        substitutionMap.put("%about-version%", aboutVersion);

        final String aboutContent = resources.getString(R.string.about_content, appName, learnMoreURL);
        substitutionMap.put("%about-content%", aboutContent);

        final String wordmark = HtmlLoader.loadPngAsDataURI(webView.getContext(), R.drawable.wordmark);
        substitutionMap.put("%wordmark%", wordmark);

        ViewCompat.setLayoutDirection(webView, View.LAYOUT_DIRECTION_LOCALE);
        final int layoutDirection = ViewCompat.getLayoutDirection(webView);

        final String direction;

        if (layoutDirection == View.LAYOUT_DIRECTION_LTR) {
            direction = "ltr";
        } else if (layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            direction = "rtl";
        } else {
            direction = "auto";
        }
        substitutionMap.put("%dir%", direction);

        final String data = HtmlLoader.loadResourceFile(webView.getContext(), R.raw.about, substitutionMap);
        // We use a file:/// base URL so that we have the right origin to load file:/// css and
        // image resources.
        webView.loadDataWithBaseURL("file:///android_res/raw/about.html", data, "text/html", "UTF-8", null);
    }

}
