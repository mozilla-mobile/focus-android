/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.search;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.UrlUtils;

public class ManualAddSearchEnginePreference extends Preference {
    private EditText engineNameEditText;
    private EditText searchQueryEditText;
    private TextInputLayout engineNameErrorLayout;
    private TextInputLayout searchQueryErrorLayout;
    private ProgressBar progressView;

    private String querySearchString;
    private String engineNameString;

    public ManualAddSearchEnginePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ManualAddSearchEnginePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();

        final PreferenceSavedState preferenceSavedState = new PreferenceSavedState(superState);
        preferenceSavedState.engineName = engineNameEditText.getText().toString();
        preferenceSavedState.searchQuery = searchQueryEditText.getText().toString();

        return preferenceSavedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        PreferenceSavedState preferenceSavedState = (PreferenceSavedState) state;
        super.onRestoreInstanceState(preferenceSavedState.getSuperState());

        engineNameString = preferenceSavedState.engineName;
        querySearchString = preferenceSavedState.searchQuery;
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        final View view = super.onCreateView(parent);

        engineNameErrorLayout = view.findViewById(R.id.edit_engine_name_layout);
        searchQueryErrorLayout = view.findViewById(R.id.edit_search_string_layout);

        engineNameEditText = view.findViewById(R.id.edit_engine_name);
        searchQueryEditText = view.findViewById(R.id.edit_search_string);

        restoreViewState();

        progressView = view.findViewById(R.id.progress);

        engineNameEditText.addTextChangedListener(buildTextWatcherForErrorLayout(engineNameErrorLayout));
        searchQueryEditText.addTextChangedListener(buildTextWatcherForErrorLayout(searchQueryErrorLayout));

        return view;
    }

    /**
     * Restores the saved state of the UI
     */
    private void restoreViewState() {
        if (!TextUtils.isEmpty(engineNameString)) {
            engineNameEditText.setText(engineNameString);
        }
        if (!TextUtils.isEmpty(querySearchString)) {
            searchQueryEditText.setText(querySearchString);
        }
    }

    private boolean engineNameIsUnique(String engineName) {
        final SharedPreferences sharedPreferences = getContext().getSharedPreferences(SearchEngineManager.PREF_FILE_SEARCH_ENGINES, Context.MODE_PRIVATE);
        return !sharedPreferences.contains(engineName);
    }

    public boolean validateEngineNameAndShowError(String engineName) {
        if (TextUtils.isEmpty(engineName) || !engineNameIsUnique(engineName)) {
            engineNameErrorLayout.setError(getContext().getString(R.string.search_add_error_empty_name));
            return false;
        } else {
            engineNameErrorLayout.setError(null);
            return true;
        }
    }

    public boolean validateSearchQueryAndShowError(String searchQuery) {
        if (TextUtils.isEmpty(searchQuery)) {
            searchQueryErrorLayout.setError(getContext().getString(R.string.search_add_error_empty_search));
            return false;
        } else if (!UrlUtils.isValidSearchQueryUrl(searchQuery)) {
            searchQueryErrorLayout.setError(getContext().getString(R.string.search_add_error_format));
            return false;
        } else {
            searchQueryErrorLayout.setError(null);
            return true;
        }
    }

    private TextWatcher buildTextWatcherForErrorLayout(final TextInputLayout errorLayout) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                errorLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        };
    }

    public void setSearchQueryErrorText(final String err) {
        searchQueryErrorLayout.setError(err);
    }

    public void setProgressViewShown(final boolean isShown) {
        progressView.setVisibility(isShown ? View.VISIBLE : View.GONE);
    }

    /**
     * Class for saving the state of the UI - search engine name and search string
     */
    private static class PreferenceSavedState extends Preference.BaseSavedState {
        private String engineName;
        private String searchQuery;

        PreferenceSavedState(Parcelable superState) {
            super(superState);
        }

        PreferenceSavedState(Parcel source) {
            super(source);
            // Get the current preference's value
            engineName = source.readString();
            searchQuery = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            // Write the preference's value
            dest.writeString(engineName);
            dest.writeString(searchQuery);
        }

        // Standard creator object using an instance of this class
        public static final Parcelable.Creator<PreferenceSavedState>
                CREATOR =
                new Parcelable.Creator<PreferenceSavedState>() {

                    public PreferenceSavedState createFromParcel(
                            Parcel in) {
                        return new PreferenceSavedState(in);
                    }

                    public PreferenceSavedState[] newArray(int size) {
                        return new PreferenceSavedState[size];
                    }
                };
    }
}
