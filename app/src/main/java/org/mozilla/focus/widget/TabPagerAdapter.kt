/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.support.v4.app.Fragment
import android.support.v4.view.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_switcher_tab.view.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.mozilla.focus.R
import org.mozilla.focus.fragment.BrowserFragment
import org.mozilla.focus.session.Session
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.web.Download
import org.mozilla.focus.web.IWebView

@Suppress("TooManyFunctions") // This class is designed to have a lot of (simple) functions
class TabPagerAdapter(val fragment: Fragment) : PagerAdapter(), IWebView.Callback {

    val sessions: List<Session> = SessionManager.getInstance().sessions.value
    init {
        notifyDataSetChanged()
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val inflater = LayoutInflater.from(fragment.context)
        val layout = inflater.inflate(R.layout.item_switcher_tab, container, false)
        launch(UI) {
            with(sessions[position]) {
                layout.webview_tab.setCallback(this@TabPagerAdapter)
                layout.webview_tab.setBlockingEnabled(isBlockingEnabled)
                layout.webview_tab.setRequestDesktop(shouldRequestDesktopSite())
                if (hasWebViewState()) {
                    layout.webview_tab.restoreWebViewState(this)
                } else {
                    layout.webview_tab.loadUrl(url.value)
                }
                layout.webview_tab.onResume()
                layout.textView_title_tab.text = if (getPageTitle(position).isNullOrEmpty()) {
                    "about:blank"
                } else {
                    getPageTitle(position)
                }
                layout.button_close_tab.setOnClickListener {
                    SessionManager.getInstance().removeRegularSession(uuid)
                }
                layout.webview_touch_delegate.setOnLongClickListener(null)
                layout.webview_touch_delegate.setOnClickListener {
                    SessionManager.getInstance().selectSession(this)
                    fragment.fragmentManager?.beginTransaction()
                            ?.replace(R.id.container, BrowserFragment.createForSession(this))
                            ?.commit()
                }
            }
        }
        container.addView(layout)
        return layout
    }

    override fun destroyItem(container: ViewGroup, position: Int, item: Any) {
        container.removeView(item as View?)
    }

    override fun isViewFromObject(p0: View, p1: Any): Boolean {
        return p0 == p1
    }

    override fun getCount(): Int {
        return sessions.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return sessions[position].pageTitle.value
    }

    override fun onPageStarted(url: String?) {
    }

    override fun onPageFinished(isSecure: Boolean) {
    }

    override fun onSecurityChanged(isSecure: Boolean, host: String?, organization: String?) {
    }

    override fun onProgress(progress: Int) {
    }

    override fun onURLChanged(url: String?) {
    }

    override fun onTitleChanged(title: String?) {
    }

    override fun onRequest(isTriggeredByUserGesture: Boolean) {
    }

    override fun onDownloadStart(download: Download?) {
    }

    override fun onLongPress(hitTarget: IWebView.HitTarget?) {
    }

    override fun onEnterFullScreen(callback: IWebView.FullscreenCallback, view: View?) {
    }

    override fun onExitFullScreen() {
    }

    override fun countBlockedTracker() {
    }

    override fun resetBlockedTrackers() {
    }

    override fun onBlockingStateChanged(isBlockingEnabled: Boolean) {
    }

    override fun onHttpAuthRequest(callback: IWebView.HttpAuthCallback, host: String?, realm: String?) {
    }

    override fun onRequestDesktopStateChanged(shouldRequestDesktop: Boolean) {
    }
}
