/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.locale

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import java.util.Locale

import org.junit.Assert.assertEquals

@RunWith(RobolectricTestRunner::class)
class LocalesTest {
    @Test
    fun testLanguage() {
        assertEquals("en", Locales.getLanguage(Locale.getDefault()))
    }

    @Test
    fun testHebrewIsrael() {
        val locale = Locale("iw", "IL")

        assertEquals("he", Locales.getLanguage(locale))
        assertEquals("he-IL", Locales.getLanguageTag(locale))
    }

    @Test
    fun testIndonesianIndonesia() {
        val locale = Locale("in", "ID")

        assertEquals("id", Locales.getLanguage(locale))
        assertEquals("id-ID", Locales.getLanguageTag(locale))
    }

    @Test
    fun testYiddishUnitedStates() {
        val locale = Locale("ji", "US")

        assertEquals("yi", Locales.getLanguage(locale))
        assertEquals("yi-US", Locales.getLanguageTag(locale))
    }

    @Test
    fun testEmptyCountry() {
        val locale = Locale("en")

        assertEquals("en", Locales.getLanguage(locale))
        assertEquals("en", Locales.getLanguageTag(locale))
    }
}