/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import org.mozilla.focus.R;
import org.mozilla.focus.search.SearchEngineManager;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.UrlUtils;

import java.util.Collections;

public class ManualAddSearchEngineSettingsFragment extends SettingsFragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        // We've checked that this cast is legal in super.onAttach.
        ((ActionBarUpdater) getActivity()).updateIcon(R.drawable.ic_close);
   }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_search_engine_manual_add, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save_search_engine:
                final View rootView = getView();
                final String engineName = ((EditText) rootView.findViewById(R.id.edit_engine_name)).getText().toString();
                final String searchQuery = ((EditText) rootView.findViewById(R.id.edit_search_string)).getText().toString();

                final SharedPreferences sharedPreferences = getSearchEngineSharedPreferences();
                boolean isSuccess = false;
                if (TextUtils.isEmpty(engineName)) {
                    showSnackbar(R.string.search_add_error_empty_name);
                } else if (TextUtils.isEmpty(searchQuery)) {
                    showSnackbar(R.string.search_add_error_empty_search);
                } else if (!UrlUtils.isValidSearchQueryUrl(searchQuery)) {
                    showSnackbar(R.string.search_add_error_format);
                } else if (isDuplicateSearchEngine(engineName, searchQuery, sharedPreferences)) {
                    showSnackbar(R.string.search_add_error_duplicate);
                } else {
                    SearchEngineManager.addSearchEngine(sharedPreferences, getActivity(), engineName, searchQuery);
                    isSuccess = true;
                    showSnackbar(R.string.search_add_confirmation);
                    getFragmentManager().popBackStack();
                }
                TelemetryWrapper.saveCustomSearchEngineEvent(isSuccess);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showSnackbar(@StringRes final int errStrRes) {
        Snackbar.make(getView(), errStrRes, Snackbar.LENGTH_SHORT).show();
    }

    private static boolean isDuplicateSearchEngine(final String engineName, final String searchString,
            final SharedPreferences sharedPreferences) {
        return sharedPreferences.getStringSet(SearchEngineManager.PREF_KEY_CUSTOM_SEARCH_ENGINES,
                Collections.<String>emptySet()).contains(engineName);
    }
}
