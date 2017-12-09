/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.InfoActivity;
import org.mozilla.focus.search.ManualAddSearchEnginePreference;
import org.mozilla.focus.search.SearchEngineManager;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.SupportUtils;
import org.mozilla.focus.utils.ViewUtils;

public class ManualAddSearchEngineSettingsFragment extends PreferenceFragment {
    private static String LOGTAG = "ManualAddSearchEngine";



    /**
     * A reference to an active async task, if applicable, used to manage the task for lifecycle changes.
     * See {@link #onPause()} for details.
     */
    @Nullable AsyncTask activeAsyncTask;
    @Nullable MenuItem menuItemForActiveAsyncTask;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.manual_add_search_engine);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        // We've checked that this cast is legal in super.onAttach.
        ((SettingsFragment.ActionBarUpdater) getActivity()).updateIcon(R.drawable.ic_close);
   }

    @Override
    public void onPause() {
        super.onPause();

        // This is a last minute change and we want to keep the async task management simple: onPause is the
        // first required callback for various lifecycle changes: a dialog is shown, the user
        // leaves the app, the app rotates, etc. To keep things simple, we do our AsyncTask management here,
        // before it gets more complex (e.g. reattaching the AsyncTask to a new fragment).
        //
        // We cancel the AsyncTask also to keep things simple: if the task is cancelled, it will:
        // - Likely end immediately and we don't need to handle it returning after the lifecycle changes
        // - Get onPostExecute scheduled on the UI thread, which must run after onPause (since it also runs on
        // the UI thread), and we check if the AsyncTask is cancelled there before we perform any other actions.
        if (activeAsyncTask != null) {
            activeAsyncTask.cancel(true);
            setUiIsValidatingAsync(false, menuItemForActiveAsyncTask);

            activeAsyncTask = null;
            menuItemForActiveAsyncTask = null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_search_engine_manual_add, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.learn_more:
                final Context context = getActivity();
                final String url = SupportUtils.getSumoURLForTopic(context, "add-search-engine");

                final String title = ((AppCompatActivity) getActivity()).getSupportActionBar().getTitle().toString();
                final Intent intent = InfoActivity.getIntentFor(context, url, title);
                context.startActivity(intent);

                TelemetryWrapper.addSearchEngineLearnMoreEvent();
                return true;

            case R.id.menu_save_search_engine:
                final View rootView = getView();
                final String engineName = ((EditText) rootView.findViewById(R.id.edit_engine_name)).getText().toString();
                final String searchQuery = ((EditText) rootView.findViewById(R.id.edit_search_string)).getText().toString();

                final ManualAddSearchEnginePreference pref = (ManualAddSearchEnginePreference) findPreference(getString(R.string.pref_key_manual_add_search_engine));
                final boolean engineValid = pref.validateEngineNameAndShowError(engineName);
                final boolean searchValid = pref.validateSearchQueryAndShowError(searchQuery);
                final boolean isPartialSuccess = engineValid && searchValid;

                if (isPartialSuccess) {
                    // Hide the keyboard because:
                    // - It's awkward to show the keyboard while waiting for a response
                    // - We want it hidden when we return to the previous screen (on success)
                    // - An expanded keyboard hides the success snackbar
                    ViewUtils.hideKeyboard(rootView);
                    setUiIsValidatingAsync(true, item);
                    activeAsyncTask = new ValidateSearchEngineAsyncTask(this, engineName, searchQuery).execute();
                    menuItemForActiveAsyncTask = item;
                } else {
                    TelemetryWrapper.saveCustomSearchEngineEvent(false);
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void setUiIsValidatingAsync(final boolean isValidating, final MenuItem saveMenuItem) {
        final ManualAddSearchEnginePreference pref =
                (ManualAddSearchEnginePreference) findPreference(getString(R.string.pref_key_manual_add_search_engine));
        pref.setProgressViewShown(isValidating);

        saveMenuItem.setEnabled(!isValidating);
    }

    SharedPreferences getSearchEngineSharedPreferences() {
        return getActivity().getSharedPreferences(SearchEngineManager.PREF_FILE_SEARCH_ENGINES, Context.MODE_PRIVATE);
    }


}
