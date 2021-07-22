/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.settings

import android.content.Context
import androidx.core.text.HtmlCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.dialog_tracking_protection_sheet.description
import kotlinx.android.synthetic.main.dialog_tracking_protection_sheet.trackers_count_note as trackersCountNote
import org.mozilla.focus.R

class TrackingProtectionBottomSheetDialog(context: Context) : BottomSheetDialog(context) {
    init {
        setContentView(R.layout.dialog_tracking_protection_sheet)
        setDescription()
    }

    private fun setDescription() {
        description.text = HtmlCompat.fromHtml(
            context.getString(R.string.tracking_protection_description),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        trackersCountNote.text = context.getString(R.string.trackers_count_note, "May 21, 2021")
    }
}
