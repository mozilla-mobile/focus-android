package org.mozilla.focus.utils

import android.content.Context
import org.mozilla.focus.R

object HardwareUtils {

    fun isTablet(context: Context): Boolean {
        return context.resources.getBoolean(R.bool.is_tablet)
    }

}