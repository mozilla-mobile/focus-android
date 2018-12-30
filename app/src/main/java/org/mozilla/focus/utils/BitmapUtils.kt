/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.utils

import android.graphics.Bitmap
import android.util.Base64

import java.io.ByteArrayOutputStream

object BitmapUtils {
    private const val QUALITY = 100

    fun getBase64EncodedDataUriFromBitmap(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, QUALITY, stream)
        val encodedImage = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
        return "data:image/png;base64,$encodedImage"
    }
}
