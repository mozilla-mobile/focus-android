package org.mozilla.focus.utils

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream


fun Bitmap.getBase64EncodedDataUriFromBitmap(): String {
    val stream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.PNG, 100, stream)
    val encodedImage = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
    return "data:image/png;base64,$encodedImage"
}