/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.webkit.URLUtil
import org.mozilla.focus.browser.LocalizedContent
import org.mozilla.focus.ext.components

import java.net.URI
import java.net.URISyntaxException
import java.net.URL

@Suppress("TooManyFunctions")
object UrlUtils {
    fun normalize(input: String): String {
        val trimmedInput = input.trim { it <= ' ' }
        var uri = Uri.parse(trimmedInput)

        if (TextUtils.isEmpty(uri.scheme)) {
            uri = Uri.parse("http://$trimmedInput")
        }

        return uri.toString()
    }

    /**
     * Is the given string a URL or should we perform a search?
     *
     * TODO: This is a super simple and probably stupid implementation.
     */
    fun isUrl(url: String): Boolean {
        val trimmedUrl = url.trim { it <= ' ' }
        return if (trimmedUrl.contains(" ")) {
            false
        } else trimmedUrl.contains(".") || trimmedUrl.contains(":")
    }

    fun isValidSearchQueryUrl(url: String): Boolean {
        var trimmedUrl = url.trim { it <= ' ' }
        if (!trimmedUrl.matches("^.+?://.+?".toRegex())) {
            // UI hint url doesn't have http scheme, so add it if necessary
            trimmedUrl = "http://$trimmedUrl"
        }

        val isNetworkUrl = URLUtil.isNetworkUrl(trimmedUrl)
        val containsToken = trimmedUrl.contains("%s")

        return isNetworkUrl && containsToken
    }

    fun isHttpOrHttps(url: String): Boolean {
        return if (TextUtils.isEmpty(url)) {
            false
        } else url.startsWith("http:") || url.startsWith("https:")
    }

    fun createSearchUrl(context: Context, searchTerm: String): String {
        val defaultIdentifier = Settings.getInstance(context).defaultSearchEngineName

        val searchEngine = context.components.searchEngineManager
            .getDefaultSearchEngine(context, defaultIdentifier)

        return searchEngine.buildSearchUrl(searchTerm)
    }

    fun stripUserInfo(url: String?): String? {
        if (TextUtils.isEmpty(url)) {
            return ""
        }

        try {
            var uri = URI(url!!)

            val userInfo = uri.userInfo
            return if (userInfo == null) {
                url
            } else {
                // Strip the userInfo to minimise spoofing ability. This only affects what's shown
                // during browsing, this information isn't used when we start editing the URL:
                uri = URI(uri.scheme, null, uri.host, uri.port, uri.path, uri.query, uri.fragment)

                uri.toString()
            }
        } catch (e: URISyntaxException) {
            // We might be trying to display a user-entered URL (which could plausibly contain errors),
            // in this case its safe to just return the raw input.
            // There are also some special cases that URI can't handle, such as "http:" by itself.
            return url
        }
    }

    fun isPermittedResourceProtocol(scheme: String?): Boolean {
        return scheme != null && (scheme.startsWith("http") ||
                scheme.startsWith("https") ||
                scheme.startsWith("file") ||
                scheme.startsWith("data") ||
                scheme.startsWith("javascript") ||
                scheme.startsWith("about"))
    }

    fun isSupportedProtocol(scheme: String?): Boolean {
        return scheme != null && (isPermittedResourceProtocol(scheme) || scheme.startsWith("error"))
    }

    fun isInternalErrorURL(url: String): Boolean {
        return "data:text/html;charset=utf-8;base64," == url
    }

    /**
     * Checks that urls are non-null and are the same aside from a trailing slash.
     *
     * @return true if urls are the same except for trailing slash, or if either url is null.
     */
    fun urlsMatchExceptForTrailingSlash(url1: String?, url2: String?): Boolean {
        // This is a hack to catch a NPE in issue #26.
        if (url1 == null || url2 == null) {
            return false
        }

        val lengthDifference = url1.length - url2.length

        return when (lengthDifference) {
            0 -> // The simplest case:
                url1.equals(url2, ignoreCase = true)
            1 -> // url1 is longer:
                url1[url1.length - 1] == '/' && url1.regionMatches(0, url2, 0, url2.length, ignoreCase = true)
            -1 -> url2[url2.length - 1] == '/' && url2.regionMatches(0, url1, 0, url1.length, ignoreCase = true)
            else -> false
        }
    }

    @Suppress("MagicNumber")
    fun stripCommonSubdomains(host: String?): String? {
        if (host == null) {
            return null
        }

        // In contrast to desktop, we also strip mobile subdomains,
        // since its unlikely users are intentionally typing them
        var start = 0

        when {
            host.startsWith("www.") -> start = 4
            host.startsWith("mobile.") -> start = 7
            host.startsWith("m.") -> start = 2
        }

        return host.substring(start)
    }

    @Suppress("MagicNumber")
    fun stripScheme(url: String?): String? {
        if (url == null) {
            return null
        }

        var start = 0
        when {
            url.startsWith("http://") -> start = 7
            url.startsWith("https://") -> start = 8
        }

        return url.substring(start)
    }

    fun stripSchemeAndSubDomain(url: String): String {
        return normalize(stripCommonSubdomains(stripScheme(url))!!)
    }

    fun isLocalizedContent(url: String?): Boolean {
        return url != null &&
                (url == LocalizedContent.URL_ABOUT ||
                        url == LocalizedContent.URL_RIGHTS ||
                        url == "about:blank"
                        )
    }

    fun getIndexOfTLD(url: String): Int {
        val tld = getTopLevelDomain(url) ?: ""
        return url.indexOf(tld, 0, true) + tld.length
    }

    fun getTopLevelDomain(url: String): String? {
        return TLDExtractor.extractTLD(URL(url).host)
    }
}
