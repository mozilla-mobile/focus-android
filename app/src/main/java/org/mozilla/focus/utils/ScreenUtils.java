/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.view.Window;

/** Utilities for managing the screen. */
public class ScreenUtils {
    private ScreenUtils() {}

    // via https://stackoverflow.com/a/3410200/2219998.
    public static int getStatusBarHeight(@NonNull final Window window) {
        final Rect appContentRect = new Rect();
        window.getDecorView().getWindowVisibleDisplayFrame(appContentRect);
        return appContentRect.top;
    }
}
