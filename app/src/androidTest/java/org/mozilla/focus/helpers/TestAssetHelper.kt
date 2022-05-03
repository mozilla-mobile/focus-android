/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helpers

import okhttp3.mockwebserver.MockWebServer

/**
 * Helper for hosting web pages locally for testing purposes.
 */
object TestAssetHelper {
    data class TestAsset(val url: String, val content: String, val title: String)

    /**
     * Hosts simple websites, found at androidTest/assets/tab[1|2|3].html
     * Returns a list of TestAsset, which can be used to navigate to each and
     * assert that the correct information is being displayed.
     *
     * Content for these pages all follow the same pattern. See [tab1.html] for
     * content implementation details.
     */
    fun getGenericTabAsset(server: MockWebServer, pageNum: Int): TestAsset {
        val url = server.url("tab$pageNum.html").toString()
        val content = "Tab $pageNum"
        val title = "tab$pageNum"

        return TestAsset(url, content, title)
    }

    fun getGenericAsset(server: MockWebServer): TestAsset {
        val url = server.url("genericPage.html").toString()
        val content = "focus test page"
        val title = "GenericPage"

        return TestAsset(url, content, title)
    }

    fun getHTMLControlsPageAsset(server: MockWebServer): TestAsset {
        val url = server.url("htmlControls.html").toString()
        val content = ""
        val title = "Html_Control_Form"

        return TestAsset(url, content, title)
    }

    fun getEnhancedTrackingProtectionAsset(server: MockWebServer, pageTitle: String): TestAsset {
        val url = server.url("etpPages/$pageTitle.html").toString()
        val content = ""

        return TestAsset(url, content, pageTitle)
    }

    fun getImageTestAsset(server: MockWebServer): TestAsset {
        val url = server.url("image_test.html").toString()

        return TestAsset(url, "", "")
    }

    fun getStorageTestAsset(server: MockWebServer, pageTitle: String): TestAsset {
        val url = server.url(pageTitle).toString()

        return TestAsset(url, "", "")
    }

    fun getMediaTestAsset(server: MockWebServer, pageTitle: String): TestAsset {
        val url = server.url("$pageTitle.html").toString()

        return TestAsset(url, "", pageTitle)
    }
}
