/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.graphics.Rect
import android.os.Build
import android.view.View
import androidx.core.view.WindowInsetsCompat
import mozilla.components.support.utils.ext.bottom
import org.mozilla.focus.R

fun View.isKeyboardVisible(): Boolean {
    // Since we have insets in M and above, we don't need to guess what the keyboard height is.
    // Otherwise, we make a guess at the minimum height of the keyboard to account for the
    // navigation bar.
    val minimumKeyboardHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        0
    } else {
        resources.getDimensionPixelSize(R.dimen.minimum_keyboard_height)
    }
    return getKeyboardHeight() > minimumKeyboardHeight
}

internal fun View.getWindowVisibleDisplayFrame(): Rect = with(Rect()) {
    getWindowVisibleDisplayFrame(this)
    this
}

internal fun View.getKeyboardHeight(): Int {
    val windowRect = getWindowVisibleDisplayFrame()
    val statusBarHeight = windowRect.top
    var keyboardHeight = rootView.height - (windowRect.height() + statusBarHeight)
    getWindowInsets()?.let {
        keyboardHeight -= it.bottom()
    }

    return keyboardHeight
}


/**
 * Fills a [Rect] with data about a view's location in the screen.
 *
 * @see View.getLocationOnScreen
 * @see View.getRectWithViewLocation for a version of this that is relative to a window
 */
fun View.getRectWithScreenLocation(): Rect {
    val locationOnScreen = IntArray(2).apply { getLocationOnScreen(this) }
    return Rect(
        locationOnScreen[0],
        locationOnScreen[1],
        locationOnScreen[0] + width,
        locationOnScreen[1] + height,
    )
}

/**
 * A safer version of [ViewCompat.getRootWindowInsets] that does not throw a NullPointerException
 * if the view is not attached.
 */
fun View.getWindowInsets(): WindowInsetsCompat? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        rootWindowInsets?.let {
            WindowInsetsCompat.toWindowInsetsCompat(it)
        }
    } else {
        null
    }
}