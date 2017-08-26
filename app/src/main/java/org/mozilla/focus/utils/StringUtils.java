/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;

import org.mozilla.focus.R;

public class StringUtils {

    public static SpannableString createSpannableSearchHint(final Context context, final String searchText) {
        final String hint = context.getString(R.string.search_hint, searchText);

        final SpannableString content = new SpannableString(hint);
        content.setSpan(new StyleSpan(Typeface.BOLD), hint.length() - searchText.length(), hint.length(), 0);

        return content;
    }
}
