/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.fragment

import android.content.Context
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import org.mozilla.focus.R
import org.mozilla.focus.ext.requireComponents
import org.mozilla.focus.firstrun.FirstrunPagerAdapter
import org.mozilla.focus.state.AppAction
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.StatusBarUtils

class FirstrunFragment : Fragment(), View.OnClickListener {

    private var viewPager: ViewPager? = null

    private var background: View? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val transition = TransitionInflater.from(context).inflateTransition(R.transition.firstrun_exit)

        exitTransition = transition

        // We will send a telemetry event whenever a new firstrun page is shown. However this page
        // listener won't fire for the initial page we are showing. So we are going to firing here.
        TelemetryWrapper.showFirstRunPageEvent(0)
    }

    @Suppress("MagicNumber")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_firstrun, container, false)

        view.findViewById<View>(R.id.skip).setOnClickListener(this)

        background = view.findViewById(R.id.background)

        val adapter = FirstrunPagerAdapter(container!!.context, this)

        viewPager = view.findViewById(R.id.pager)
        viewPager!!.contentDescription = adapter.getPageAccessibilityDescription(0)
        viewPager!!.isFocusable = true

        viewPager!!.setPageTransformer(true) { page, position -> page.alpha = 1 - 0.5f * Math.abs(position) }

        viewPager!!.clipToPadding = false
        viewPager!!.adapter = adapter
        viewPager!!.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageSelected(position: Int) {
                TelemetryWrapper.showFirstRunPageEvent(position)

                viewPager!!.contentDescription = adapter.getPageAccessibilityDescription(position)
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageScrollStateChanged(state: Int) {}
        })

        val tabLayout = view.findViewById<TabLayout>(R.id.tabs)
        tabLayout.setupWithViewPager(viewPager, true)

        return view
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.next -> viewPager!!.currentItem = viewPager!!.currentItem + 1

            R.id.skip -> {
                finishFirstrun()
                TelemetryWrapper.skipFirstRunEvent()
            }

            R.id.finish -> {
                finishFirstrun()
                TelemetryWrapper.finishFirstRunEvent()
            }

            else -> throw IllegalArgumentException("Unknown view")
        }
    }

    private fun finishFirstrun() {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .edit()
            .putBoolean(FIRSTRUN_PREF, true)
            .apply()

        val selectedTabId = requireComponents.store.state.selectedTabId
        requireComponents.appStore.dispatch(AppAction.FinishFirstRun(selectedTabId))
    }

    override fun onResume() {
        super.onResume()
        StatusBarUtils.getStatusBarHeight(background) { statusBarHeight ->
            background!!.setPadding(
                0,
                statusBarHeight,
                0,
                0
            )
        }
    }

    companion object {
        const val FRAGMENT_TAG = "firstrun"
        const val FIRSTRUN_PREF = "firstrun_shown"

        fun create(): FirstrunFragment {
            val arguments = Bundle()

            val fragment = FirstrunFragment()
            fragment.arguments = arguments

            return fragment
        }
    }
}
