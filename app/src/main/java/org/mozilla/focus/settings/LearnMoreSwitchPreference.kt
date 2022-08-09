package org.mozilla.focus.settings

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import org.mozilla.focus.R

open class LearnMoreSwitchPreference(context: Context?, attrs: AttributeSet?) :
    SwitchPreferenceCompat(context, attrs) {
    var description: String? = null
    var onLearMoreClick: () -> Unit = {}

    init {
        layoutResource = R.layout.preference_switch_learn_more
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        description?.let {
            val summaryView = holder.findViewById(android.R.id.summary) as TextView
            summaryView.text = it
            summaryView.visibility = View.VISIBLE
        }

        val learnMoreLink = holder.findViewById(R.id.link) as TextView
        learnMoreLink.setOnClickListener {
            onLearMoreClick.invoke()
        }

        val backgroundDrawableArray =
            context.obtainStyledAttributes(intArrayOf(R.attr.selectableItemBackground))
        val backgroundDrawable = backgroundDrawableArray.getDrawable(0)
        backgroundDrawableArray.recycle()
        learnMoreLink.background = backgroundDrawable
    }
}
