/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.os.Build;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;

import org.mozilla.focus.locale.Locales;
import org.mozilla.focus.session.SessionManager;
import org.mozilla.focus.session.Source;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

public class SupportUtils {
    public static final String HELP_URL = "https://support.mozilla.org/kb/what-firefox-focus-android";
    public static final String DEFAULT_BROWSER_URL = "https://support.mozilla.org/kb/set-firefox-focus-default-browser-android";
    public static final String REPORT_SITE_ISSUE_URL = "https://webcompat.com/issues/new?url=%s&label=browser-focus-geckoview";

    public static final String PRIVACY_NOTICE_URL = "https://www.mozilla.org/privacy/firefox-focus/";
    public static final String PRIVACY_NOTICE_KLAR_URL = "https://www.mozilla.org/de/privacy/firefox-klar/";

    public enum SumoTopic {
        ADD_SEARCH_ENGINE("add-search-engine"),
        AUTOCOMPLETE("autofill-domain-android"),
        TRACKERS("trackers"),
        USAGE_DATA("usage-data"),
        WHATS_NEW("whats-new-focus-android-6");

        /** The final path segment for a SUMO URL - see {@see #getSumoURLForTopic} */
        @VisibleForTesting final String topicStr;

        SumoTopic(final String topicStr) {
            this.topicStr = topicStr;
        }
    }

    public static String getSumoURLForTopic(final Context context, final SumoTopic topic) {
        final String escapedTopic = getEncodedTopicUTF8(topic.topicStr);
        final String appVersion = getAppVersion(context);
        final String osTarget = "Android";
        final String langTag = Locales.getLanguageTag(Locale.getDefault());
        return "https://support.mozilla.org/1/mobile/" + appVersion + "/" + osTarget + "/" + langTag + "/" + escapedTopic;
    }

    public static String getManifestoURL() {
        final String langTag = Locales.getLanguageTag(Locale.getDefault());
        return "https://www.mozilla.org/" + langTag + "/about/manifesto/";
    }

    private static String getEncodedTopicUTF8(final String topic) {
        try {
            return URLEncoder.encode(topic, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("utf-8 should always be available", e);
        }
    }

    private static String getAppVersion(final Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // This should be impossible - we should always be able to get information about ourselves:
            throw new IllegalStateException("Unable find package details for Focus", e);
        }
    }

    public static void openDefaultBrowserSumoPage(Context context) {
        SessionManager.getInstance().createSession(Source.MENU, SupportUtils.DEFAULT_BROWSER_URL);
        ((Activity) context).onBackPressed();
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static void openDefaultAppsSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // In some cases, a matching Activity may not exist (according to the Android docs).
            openDefaultBrowserSumoPage(context);
        }
    }
}
