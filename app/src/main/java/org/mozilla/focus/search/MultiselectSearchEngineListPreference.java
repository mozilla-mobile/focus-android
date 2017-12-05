/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.search;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.widget.CompoundButton;

import org.mozilla.focus.R;

import java.util.HashSet;

import java.util.Set;

public class MultiselectSearchEngineListPreference extends SearchEngineListPreference {

    public MultiselectSearchEngineListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MultiselectSearchEngineListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int getItemResId() {
        return R.layout.search_engine_checkbox_button;
    }

    @Override
    protected void updateDefaultItem(CompoundButton defaultButton) {
        defaultButton.setClickable(false);
        // Showing the default engine as disabled requires a StateListDrawable, but since there
        // is no state_clickable and state_enabled seems to require a default drawable state,
        // use state_activated instead to designate the default search engine.
        defaultButton.setActivated(true);
    }

    public Set<String> getCheckedEngineIds() {
        final Set<String> engineIdSet = new HashSet<>();

        for (int i = 0; i < searchEngineGroup.getChildCount(); i++) {
            final CompoundButton engineButton = (CompoundButton) searchEngineGroup.getChildAt(i);
            if (engineButton.isChecked()) {
                engineIdSet.add((String) engineButton.getTag());
            }
        }
        return engineIdSet;
    }

    // Whenever an engine is checked or unchecked, we adapt the MenuItem state (enable or disable) given in param
    // Though some conditions are added just for optimisation
    public void bindEngineCheckboxesToMenuItem(final MenuItem menuDeleteItem, final int alpha_enabled, final int alpha_disabled) {
        for (int i = 0; i < searchEngineGroup.getChildCount(); i++) {
            final CompoundButton engineButton = (CompoundButton) searchEngineGroup.getChildAt(i);
            engineButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked || atLeastOneEngineChecked()) {
                        if (!menuDeleteItem.isEnabled()) {
                            menuDeleteItem.setEnabled(true).getIcon().mutate().setAlpha(alpha_enabled);
                        }
                    } else {
                        if (menuDeleteItem.isEnabled()) {
                            menuDeleteItem.setEnabled(false).getIcon().mutate().setAlpha(alpha_disabled);
                        }
                    }
                }
            });
        }
    }

    private boolean atLeastOneEngineChecked() {
        for (int i = 0; i < searchEngineGroup.getChildCount(); i++) {
            final CompoundButton engineButton = (CompoundButton) searchEngineGroup.getChildAt(i);
            if (engineButton.isChecked()) {
               return true;
            }
        }
        return false;
    }
}
