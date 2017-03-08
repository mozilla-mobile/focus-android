/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import org.mozilla.focus.R;
import org.mozilla.focus.fragment.FirstrunFragment;

/**
 * A simple wrapper for SharedPreferences that makes reading preference a little bit easier.
 */
public class Settings {
    private final SharedPreferences preferences;
    private final Resources resources;

    public static abstract class BooleanPrefListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        private final Settings settings;

        private String prefKey;

        public BooleanPrefListener(final Settings settings) {
            this.settings = settings;
        }

        public abstract void onPrefChanged(final boolean newValue);

        @Override
        public final void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(prefKey)) {
                onPrefChanged(settings.shouldBlockImages());
            }
        }

        /* package-private */ final void subscribeToPreference(final String prefKey) {
            if (prefKey != null) {
                throw new IllegalStateException("PrefListener is already subscribed");
            }

            this.prefKey = prefKey;
            settings.preferences.registerOnSharedPreferenceChangeListener(this);
        }

        public final void unsubscribe() {
            if (prefKey == null) {
                throw new IllegalStateException("Unable to unsubscribe a non-subscribed PrefListener");
            }

            settings.preferences.unregisterOnSharedPreferenceChangeListener(this);
            this.prefKey = null;
        }
    }

    public Settings(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        resources = context.getResources();
    }

    public boolean shouldBlockImages() {
        return preferences.getBoolean(
                resources.getString(R.string.pref_key_performance_block_images),
                false);
    }

    public void subscribeToBlockImagesPreference(final BooleanPrefListener listener) {
        listener.subscribeToPreference(resources.getString(R.string.pref_key_performance_block_images));
    }

    public boolean shouldShowFirstrun() {
        return !preferences.getBoolean(FirstrunFragment.FIRSTRUN_PREF, false);
    }
}
