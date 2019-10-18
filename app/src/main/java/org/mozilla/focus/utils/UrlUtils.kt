package org.mozilla.focus.utils

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.webkit.URLUtil
import org.mozilla.focus.browser.LocalizedContent
import org.mozilla.focus.ext.components
import java.net.URI
import java.net.URISyntaxException


object UrlUtils {

    @JvmStatic
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
    @JvmStatic
    fun isUrl(url: String): Boolean {
        val trimmedUrl = url.trim { it <= ' ' }
        return !trimmedUrl.contains(" ") && (trimmedUrl.contains(".") || trimmedUrl.contains(":"))
    }

    @JvmStatic
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

    @JvmStatic
    fun isHttpOrHttps(url: String): Boolean {
        return !TextUtils.isEmpty(url) && (url.startsWith("http:") || url.startsWith("https:"))
    }

    fun createSearchUrl(context: Context, searchTerm: String): String {
        val defaultIdentifier = Settings.getInstance(context).defaultSearchEngineName

        val searchEngine = context.components.searchEngineManager
                .getDefaultSearchEngine(context, defaultIdentifier)

        return searchEngine.buildSearchUrl(searchTerm)
    }


    @JvmStatic
    fun stripUserInfo(url: String?): String? {
        if (TextUtils.isEmpty(url)) {
            return ""
        }

        try {
            var uri = URI(url)

            uri.userInfo ?: return url

            // Strip the userInfo to minimise spoofing ability. This only affects what's shown
            // during browsing, this information isn't used when we start editing the URL:
            uri = URI(uri.scheme, null, uri.host, uri.port, uri.path, uri.query, uri.fragment)

            return uri.toString()
        } catch (e: URISyntaxException) {
            // We might be trying to display a user-entered URL (which could plausibly contain errors),
            // in this case its safe to just return the raw input.
            // There are also some special cases that URI can't handle, such as "http:" by itself.
            return url
        }
    }

    @JvmStatic
    fun isPermittedResourceProtocol(scheme: String?): Boolean {
        return scheme != null && (scheme.startsWith("http") ||
                scheme.startsWith("https") ||
                scheme.startsWith("file") ||
                scheme.startsWith("data") ||
                scheme.startsWith("javascript") ||
                scheme.startsWith("about"))
    }

    @JvmStatic
    fun isSupportedProtocol(scheme: String?): Boolean {
        return scheme != null && (isPermittedResourceProtocol(scheme) || scheme.startsWith("error"))
    }

    @JvmStatic
    fun isInternalErrorURL(url: String): Boolean {
        return "data:text/html;charset=utf-8;base64," == url
    }

    /**
     * Checks that urls are non-null and are the same aside from a trailing slash.
     *
     * @return true if urls are the same except for trailing slash, or if either url is null.
     */
    @JvmStatic
    fun urlsMatchExceptForTrailingSlash(url1: String?, url2: String?): Boolean {
        // This is a hack to catch a NPE in issue #26.
        if (url1 == null || url2 == null) {
            return false
        }
        val lengthDifference = url1.length - url2.length

        if (lengthDifference == 0) {
            // The simplest case:
            return url1.equals(url2, ignoreCase = true)
        } else if (lengthDifference == 1) {
            // url1 is longer:
            return url1[url1.length - 1] == '/' && url1.regionMatches(0, url2, 0, url2.length, ignoreCase = true)
        } else if (lengthDifference == -1) {
            return url2[url2.length - 1] == '/' && url2.regionMatches(0, url1, 0, url1.length, ignoreCase = true)
        }

        return false
    }

    @JvmStatic
    fun stripCommonSubdomains(host: String): String {

        // In contrast to desktop, we also strip mobile subdomains,
        // since its unlikely users are intentionally typing them
        var start = 0

        if (host.startsWith("www.")) {
            start = 4
        } else if (host.startsWith("mobile.")) {
            start = 7
        } else if (host.startsWith("m.")) {
            start = 2
        }

        return host.substring(start)
    }

    fun stripScheme(url: String): String {

        var start = 0

        if (url.startsWith("http://")) {
            start = 7
        } else if (url.startsWith("https://")) {
            start = 8
        }

        return url.substring(start)
    }

    fun stripSchemeAndSubDomain(url: String): String {
        return normalize(stripCommonSubdomains(stripScheme(url)))
    }

    @JvmStatic
    fun isLocalizedContent(url: String?): Boolean {
        return url != null && (url == LocalizedContent.URL_ABOUT || url == LocalizedContent.URL_RIGHTS || url == "about:blank")
    }
}
