/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.content.Context
import android.os.Build
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceViewHolder
import android.util.AttributeSet
import android.widget.Switch
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.Browsers
import org.mozilla.focus.utils.SupportUtils

class DefaultBrowserPreference : Preference {
    private var switchView: Switch? = null

    // Instantiated from XML
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    // Instantiated from XML
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        widgetLayoutResource = R.layout.preference_default_browser

        val appName = context.resources.getString(R.string.app_name)
        val title = context.resources.getString(R.string.preference_default_browser2, appName)

        setTitle(title)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        switchView = holder.findViewById(R.id.switch_widget) as Switch

        update()
    }

    fun update() {
        if (switchView != null) {
            val browsers = Browsers(context, Browsers.TRADITIONAL_BROWSER_URL)
            switchView?.isChecked = browsers.isDefaultBrowser(context)
        }
    }

    public override fun onClick() {
        val context = context

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SupportUtils.openDefaultAppsSettings(context)
            TelemetryWrapper.makeDefaultBrowserSettings()
        } else {
            SupportUtils.openDefaultBrowserSumoPage(context)
        }
    }
}
