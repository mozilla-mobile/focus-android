/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.settings

import android.app.Fragment
import android.content.Context
import android.preference.PreferenceFragment
import org.mozilla.focus.R

abstract class BaseSettingsFragment : PreferenceFragment() {

    interface ActionBarUpdater {
        fun updateTitle(titleResId: Int)
        fun updateIcon(iconResId: Int)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (activity !is ActionBarUpdater) {
            throw IllegalArgumentException("Parent activity must implement ActionBarUpdater")
        }
    }

    protected fun getActionBarUpdater() = activity as ActionBarUpdater

    protected fun navigateToFragment(fragment: Fragment) {
        fragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
