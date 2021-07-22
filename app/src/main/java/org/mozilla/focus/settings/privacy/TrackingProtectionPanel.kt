/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.settings.privacy

import android.content.Context
import androidx.core.text.HtmlCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.dialog_tracking_protection_sheet.description
import kotlinx.android.synthetic.main.dialog_tracking_protection_sheet.trackers_count_note as trackersCountNote
import kotlinx.android.synthetic.main.dialog_tracking_protection_sheet.trackers_count as trackersCount
import kotlinx.android.synthetic.main.dialog_tracking_protection_sheet.enhanced_tracking as enhancedTrackingProtection
import kotlinx.android.synthetic.main.dialog_tracking_protection_sheet.advertising as advertisingProtection
import kotlinx.android.synthetic.main.dialog_tracking_protection_sheet.analytics as analyticsProtection
import kotlinx.android.synthetic.main.dialog_tracking_protection_sheet.social as socialProtection
import kotlinx.android.synthetic.main.dialog_tracking_protection_sheet.content as contentProtection
import kotlinx.android.synthetic.main.dialog_tracking_protection_sheet.more_settings as moreSettings
import org.mozilla.focus.R
import org.mozilla.focus.state.AppAction
import org.mozilla.focus.state.AppStore
import org.mozilla.focus.state.Screen

class TrackingProtectionPanel(
    private val appStore: AppStore,
    context: Context
) : BottomSheetDialog(context) {
    var blockedTrackersModel = BlockedTrackersModel()
        set(value) {
            field = value
            updateTrackersBlocked()
        }

    init {
        setContentView(R.layout.tracking_protection_panel)

        // call this method here just for testing. In a real scenario it will called from set value method
        updateTrackersBlocked()

        setDescription()
        setListeners()
    }

    private fun setListeners() {
        enhancedTrackingProtection.setOnCheckedChangeListener { _, _ ->
            // Empty
        }
        advertisingProtection.setOnCheckedChangeListener { _, _ ->
            // Empty
        }
        analyticsProtection.setOnCheckedChangeListener { _, _ ->
            // Empty
        }
        socialProtection.setOnCheckedChangeListener { _, _ ->
            // Empty
        }
        contentProtection.setOnCheckedChangeListener { _, _ ->
            // Empty
        }
        moreSettings.setOnClickListener {
            appStore.dispatch(
                AppAction.OpenSettings(page = Screen.Settings.Page.Start)
            )
            dismiss()
        }
    }

    private fun updateTrackersBlocked() {
        with(blockedTrackersModel) {
            trackersCount.text = trackersBlocked
            trackersCountNote.text = context.getString(R.string.trackers_count_note, monitoringDate)
        }
    }

    private fun setDescription() {
        description.text = HtmlCompat.fromHtml(
            context.getString(R.string.tracking_protection_description),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }
}
