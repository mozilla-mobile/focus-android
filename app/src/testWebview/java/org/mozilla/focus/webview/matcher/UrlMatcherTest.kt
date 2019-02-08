/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.webview.matcher

import android.content.SharedPreferences
import android.net.Uri
import android.preference.PreferenceManager
import junit.framework.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.webview.matcher.util.FocusString
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.HashMap
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
class UrlMatcherTest {
    @Test
    @Throws(Exception::class)
    fun matches() {
        val matcher = UrlMatcher(arrayOf("bcd.random"))

        assertTrue(
            matcher.matches(
                Uri.parse("http://bcd.random/something"),
                Uri.parse("http://mozilla.org")
            )
        )
        assertTrue(matcher.matches(Uri.parse("http://bcd.random"), Uri.parse("http://mozilla.org")))
        assertTrue(
            matcher.matches(
                Uri.parse("http://www.bcd.random"),
                Uri.parse("http://mozilla.org")
            )
        )
        assertTrue(
            matcher.matches(
                Uri.parse("http://www.bcd.random/something"),
                Uri.parse("http://mozilla.org")
            )
        )
        assertTrue(
            matcher.matches(
                Uri.parse("http://foobar.bcd.random"),
                Uri.parse("http://mozilla.org")
            )
        )
        assertTrue(
            matcher.matches(
                Uri.parse("http://foobar.bcd.random/something"),
                Uri.parse("http://mozilla.org")
            )
        )

        assertTrue(
            !matcher.matches(
                Uri.parse("http://other.random"),
                Uri.parse("http://mozilla.org")
            )
        )
        assertTrue(
            !matcher.matches(
                Uri.parse("http://other.random/something"),
                Uri.parse("http://mozilla.org")
            )
        )
        assertTrue(
            !matcher.matches(
                Uri.parse("http://www.other.random"),
                Uri.parse("http://mozilla.org")
            )
        )
        assertTrue(
            !matcher.matches(
                Uri.parse("http://www.other.random/something"),
                Uri.parse("http://mozilla.org")
            )
        )
        assertTrue(
            !matcher.matches(
                Uri.parse("http://bcd.specific"),
                Uri.parse("http://mozilla.org")
            )
        )
        assertTrue(
            !matcher.matches(
                Uri.parse("http://bcd.specific/something"),
                Uri.parse("http://mozilla.org")
            )
        )
        assertTrue(
            !matcher.matches(
                Uri.parse("http://www.bcd.specific"),
                Uri.parse("http://mozilla.org")
            )
        )
        assertTrue(
            !matcher.matches(
                Uri.parse("http://www.bcd.specific/something"),
                Uri.parse("http://mozilla.org")
            )
        )
        // Check that we still do matching for data: URIs
        assertFalse(
            matcher.matches(
                Uri.parse("http://mozilla.org/resource"),
                Uri.parse("data:text/html;stuff here")
            )
        )
        assertTrue(
            matcher.matches(
                Uri.parse("http://bcd.random/resource"),
                Uri.parse("data:text/html;stuff here")
            )
        )
    }

    @Test
    fun categoriesWork() {
        // Test that category enabling/disabling works correctly. We test this by creating
        // 4 categories, each with only one domain. We then iterate over all permutations of categories,
        // and test that only the expected domains are actually blocked.
        // (This is an important test, since we do some caching in UrlMatcher, and we need to make
        // sure that the caching doesn't break when categories are enabled/disabled at runtime.)
        val categories = HashMap()
        val categoryPrefMap = HashMap()
        // Number of categories we want to test with.
        val CAT_COUNT = 4
        val preferences =
            PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)

        run {
            // Setup for category tests
            val editor = preferences.edit()

            for (i in 0 until CAT_COUNT) {
                val domain = "category$i.com"
                val trie = Trie.createRootNode()
                trie.put(FocusString.create(domain).reverse())
                val categoryName = "category$i"
                categories.put(categoryName, trie)

                editor.putBoolean(categoryName, false)

                categoryPrefMap.put(categoryName, categoryName)
            }
            editor.apply()
        }
        val matcher = UrlMatcher(RuntimeEnvironment.application, categoryPrefMap, categories, null)
        // We can test every permutation by iterating over every value of a 4-bit integer (each bit
        // indicates whether a given category is enabled or disabled).
        // N categories -> N bits == (2^N - 1) == '1111...'
        // 4 categories -> 4 bits == 15 == 2^N-1 = '1111'
        val allEnabledPattern = (1 shl CAT_COUNT) - 1
        for (categoryPattern in 0..allEnabledPattern) {
            val editor = preferences.edit()
            // Ensure all the correct categories enabled
            for (currentCategory in 0 until CAT_COUNT) {
                val currentBit = 1 shl currentCategory
                val enabled = currentBit and categoryPattern == currentBit

                editor.putBoolean("category$currentCategory", enabled)
                // Sanity checks: just make sure our category enabling code actually sets the correct
                // values for a few known combinations (i.e. we're doing a test within the test...)
                if (categoryPattern == 0) {
                    assertFalse("All categories should be disabled for categorypattern==0", enabled)
                } else if (categoryPattern == allEnabledPattern) {
                    assertTrue(
                        "All categories should be enabled for categorypattern=='111....'",
                        enabled
                    )
                } else if (categoryPattern == Integer.parseInt("1100", 2)) {
                    if (currentCategory < 2) {
                        assertFalse("Categories 0/1 expected to be disabled", enabled)
                    } else {
                        assertTrue("Categories >= 2 expected to be enabled", enabled)
                    }
                }
            }
            editor.apply()

            for (currentCategory in 0 until CAT_COUNT) {
                val currentBit = 1 shl currentCategory
                val enabled = currentBit and categoryPattern == currentBit
                val url = "http://category$currentCategory.com"

                Assert.assertEquals(
                    "Incorrect category matched for combo=$categoryPattern url=$url",
                    enabled, matcher.matches(Uri.parse(url), Uri.parse("http://www.mozilla.org"))
                )
            }
        }
    }


}