/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.web

import android.os.Environment
import android.os.Parcel

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import org.junit.Assert.assertEquals

@RunWith(RobolectricTestRunner::class)
class DownloadTest {
    @Test
    fun testGetters() {
        val download = Download(
                "https://www.mozilla.org/image.png",
                "Focus/1.0",
                "Content-Disposition: attachment; filename=\"filename.png\"",
                "image/png",
                1024,
                Environment.DIRECTORY_DOWNLOADS,
                fileName)

        assertEquals("https://www.mozilla.org/image.png", download.url)
        assertEquals("Focus/1.0", download.userAgent)
        assertEquals("Content-Disposition: attachment; filename=\"filename.png\"", download.contentDisposition)
        assertEquals("image/png", download.mimeType)
        assertEquals(1024, download.contentLength)
        assertEquals(Environment.DIRECTORY_DOWNLOADS, download.destinationDirectory)
        assertEquals(fileName, download.fileName)
    }

    @Test
    fun testParcelable() {
        val parcel = Parcel.obtain()

        run {
            val download = Download(
                    "https://www.mozilla.org/image.png",
                    "Focus/1.0",
                    "Content-Disposition: attachment; filename=\"filename.png\"",
                    "image/png",
                    1024,
                    Environment.DIRECTORY_PICTURES,
                    fileName)
            download.writeToParcel(parcel, 0)
        }

        parcel.setDataPosition(0)

        run {
            val download = Download.CREATOR.createFromParcel(parcel)

            assertEquals("https://www.mozilla.org/image.png", download.url)
            assertEquals("Focus/1.0", download.userAgent)
            assertEquals("Content-Disposition: attachment; filename=\"filename.png\"", download.contentDisposition)
            assertEquals("image/png", download.mimeType)
            assertEquals(1024, download.contentLength)
            assertEquals(Environment.DIRECTORY_PICTURES, download.destinationDirectory)
            assertEquals(fileName, download.fileName)
        }
    }

    companion object {
        private val fileName = "filename.png"
    }
}