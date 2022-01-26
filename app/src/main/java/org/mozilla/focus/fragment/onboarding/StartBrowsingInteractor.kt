/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.fragment.onboarding

import android.content.Context
import androidx.preference.PreferenceManager
import org.mozilla.focus.GleanMetrics.Onboarding
import org.mozilla.focus.state.AppAction
import org.mozilla.focus.state.AppStore

class StartBrowsingInteractor(val appStore: AppStore) {
    fun invoke(context: Context, selectedTabId: String?) {
        Onboarding.finishButtonTapped.record(Onboarding.FinishButtonTappedExtra())
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(OnboardingFragment.ONBOARDING_PREF, true)
            .apply()

        appStore.dispatch(AppAction.FinishOnboarding(selectedTabId))
    }
}
