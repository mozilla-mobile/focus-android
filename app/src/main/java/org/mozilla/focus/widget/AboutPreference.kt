package org.mozilla.focus.widget

import android.content.Context
import androidx.preference.Preference
import android.util.AttributeSet

import org.mozilla.focus.R

class AboutPreference : Preference{
    constructor (context: Context, attrs: AttributeSet, defStyleAttr: Int):
        super(context, attrs, defStyleAttr){

        val appName = getContext().resources.getString(R.string.app_name)
        val title = getContext().resources.getString(R.string.preference_about, appName)

        setTitle(title)
    }

    constructor (context: Context, attrs: AttributeSet):
            this(context, attrs, 0)

}