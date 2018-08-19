/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.search

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceViewHolder
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioGroup
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import mozilla.components.browser.search.SearchEngine
import org.mozilla.focus.Components
import org.mozilla.focus.R
import org.mozilla.focus.utils.Settings

abstract class SearchEngineListPreference : Preference {
    protected var searchEngines: List<SearchEngine> = emptyList()
    protected var searchEngineGroup: RadioGroup? = null

    protected abstract val itemResId: Int

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        layoutResource = R.layout.preference_search_engine_chooser
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        layoutResource = R.layout.preference_search_engine_chooser
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        searchEngineGroup = holder!!.itemView.findViewById(R.id.search_engine_group)
        val context = searchEngineGroup!!.context

        searchEngines = Components.searchEngineManager.getSearchEngines(context)
            .sortedBy { it.name }

        refreshSearchEngineViews(context)
    }

    protected abstract fun updateDefaultItem(defaultButton: CompoundButton)

    fun refetchSearchEngines() {
        launch (UI) {
            searchEngines = Components.searchEngineManager
                    .load(this@SearchEngineListPreference.context)
                    .await()
                    .sortedBy { it.name }
            refreshSearchEngineViews(this@SearchEngineListPreference.context)
        }
    }

    private fun refreshSearchEngineViews(context: Context) {
        if (searchEngineGroup == null) {
            // We want to refresh the search engine list of this preference in onResume,
            // but the first time this preference is created onResume is called before onCreateView
            // so searchEngineGroup is not set yet.
            return
        }

        val defaultSearchEngine = Components.searchEngineManager.getDefaultSearchEngine(
                context,
                Settings.getInstance(context).defaultSearchEngineName
        ).identifier

        searchEngineGroup!!.removeAllViews()

        val layoutInflater = LayoutInflater.from(context)
        val layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)

        for (i in searchEngines.indices) {
            val engine = searchEngines[i]
            val engineId = engine.identifier
            val engineItem = makeButtonFromSearchEngine(engine, layoutInflater, context.resources)
            engineItem.id = i
            engineItem.tag = engineId
            if (engineId == defaultSearchEngine) {
                updateDefaultItem(engineItem)
            }
            searchEngineGroup!!.addView(engineItem, layoutParams)
        }
    }

    private fun makeButtonFromSearchEngine(
        engine: SearchEngine,
        layoutInflater: LayoutInflater,
        res: Resources
    ): CompoundButton {
        val buttonItem = layoutInflater.inflate(itemResId, null) as CompoundButton
        buttonItem.text = engine.name
        val iconSize = res.getDimension(R.dimen.preference_icon_drawable_size).toInt()
        val engineIcon = BitmapDrawable(res, engine.icon)
        engineIcon.setBounds(0, 0, iconSize, iconSize)
        val drawables = buttonItem.compoundDrawables
        buttonItem.setCompoundDrawables(engineIcon, null, drawables[2], null)
        return buttonItem
    }
}
