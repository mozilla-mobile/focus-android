package org.mozilla.focus.widget

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import org.mozilla.focus.R

class AboutPreference @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int = 0) : Preference(context,
        attrs,
        defStyleAttr
) {
    init {

        val appName = getContext().resources.getString(R.string.app_name)
        val title = getContext().resources.getString(R.string.preference_about, appName)

        setTitle(title)
    }
}
