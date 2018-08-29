/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.SeekBar
import kotlinx.android.synthetic.main.fragment_tab_switcher.*
import org.mozilla.focus.R
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.session.Source
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.widget.TabPagerAdapter
import kotlin.math.floor

class TabSwitcherFragment : Fragment() {

    companion object {
        const val FRAGMENT_TAG = "tab_switcher"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tab_switcher, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        tabViewPager.adapter = TabPagerAdapter(this)
        tabViewPager.currentItem = SessionManager.getInstance().positionOfCurrentSession
        seekBar.progress = tabViewPager.currentItem

        addButton.setOnClickListener {
            SessionManager.getInstance().createSession(Source.VIEW, "")
        }

        erase.setOnClickListener {
            TelemetryWrapper.eraseEvent()
            SessionManager.getInstance().removeAllSessions()
        }

        SessionManager.getInstance().sessions.observe(this, Observer {
            if (it != null && it.size > 1) {
                seekBar.visibility = VISIBLE
                seekBar.max = it.size - 1
            } else {
                seekBar.visibility = GONE
            }
        })

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                seekBar?.apply {
                    val numSessions = SessionManager.getInstance().numberOfSessions
                    tabViewPager.currentItem = floor(progress.toFloat() / max * numSessions).toInt()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
    }
}
