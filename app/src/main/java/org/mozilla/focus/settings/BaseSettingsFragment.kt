package org.mozilla.focus.settings

import android.app.Fragment
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceFragment
import org.mozilla.focus.R
import org.mozilla.focus.search.SearchEngineManager

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

    protected fun getSearchEngineSharedPreferences(): SharedPreferences {
        return activity.getSharedPreferences(SearchEngineManager.PREF_FILE_SEARCH_ENGINES, Context.MODE_PRIVATE)
    }

    protected fun pushFragment(fragment: Fragment) {
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack(null)
                .commit()
    }
}
