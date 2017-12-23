package org.mozilla.focus.settings;


import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;
import android.support.design.widget.Snackbar;
import android.util.Log;

import org.mozilla.focus.R;
import org.mozilla.focus.search.ManualAddSearchEnginePreference;
import org.mozilla.focus.search.SearchEngineManager;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.UrlUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;




public class ValidateSearchEngineAsyncTask extends AsyncTask<Void, Void, Boolean> {
    private static String LOGTAG = "ValidateSearchEngineAsyncTask ";

    // Set so the user doesn't have to wait *too* long. It's used twice: once for connecting and once for reading.
    private static final int SEARCH_QUERY_VALIDATION_TIMEOUT_MILLIS = 4000;

    private final WeakReference<ManualAddSearchEngineSettingsFragment> thatWeakReference;
    private final String engineName;
    private final String query;

    public ValidateSearchEngineAsyncTask(final ManualAddSearchEngineSettingsFragment that, final String engineName,
                                         final String query) {
        this.thatWeakReference = new WeakReference<>(that);
        this.engineName = engineName;
        this.query = query;
    }

    @Override
    protected Boolean doInBackground(final Void... voids) {
        final boolean isValidSearchQuery = isValidSearchQueryURL(query);
        TelemetryWrapper.saveCustomSearchEngineEvent(isValidSearchQuery);
        return isValidSearchQuery;
    }

    @Override
    protected void onPostExecute(final Boolean isValidSearchQuery) {
        super.onPostExecute(isValidSearchQuery);

        if (isCancelled()) {
            Log.d(LOGTAG, "ValidateSearchEngineAsyncTask has been cancelled");
            return;
        }

        final ManualAddSearchEngineSettingsFragment that = thatWeakReference.get();
        if (that == null) {
            Log.d(LOGTAG, "Fragment or menu item no longer exists when search query validation async task returned.");
            return;
        }

        if (isValidSearchQuery) {
            final SharedPreferences sharedPreferences = that.getSearchEngineSharedPreferences();
            SearchEngineManager.addSearchEngine(sharedPreferences, that.getActivity(), engineName, query);
            Snackbar.make(that.getView(), R.string.search_add_confirmation, Snackbar.LENGTH_SHORT).show();
            that.getFragmentManager().popBackStack();
        } else {
            showServerError(that);
        }

        that.setUiIsValidatingAsync(false, that.menuItemForActiveAsyncTask);
        that.activeAsyncTask = null;
        that.menuItemForActiveAsyncTask = null;
    }

    private void showServerError(final ManualAddSearchEngineSettingsFragment that) {
        final ManualAddSearchEnginePreference pref = (ManualAddSearchEnginePreference) that.findPreference(
                that.getString(R.string.pref_key_manual_add_search_engine));
        pref.setSearchQueryErrorText(that.getString(R.string.error_hostLookup_title));
    }


    @SuppressFBWarnings("DE_MIGHT_IGNORE")
    @WorkerThread // makes network request.
    @VisibleForTesting static boolean isValidSearchQueryURL(final String query) {
        // TODO: we should share the code to substitute and normalize the search string (see SearchEngine.buildSearchUrl).
        final String encodedTestQuery = Uri.encode("test");

        final String normalizedHttpsSearchURLStr = UrlUtils.normalize(query);
        final String searchURLStr = normalizedHttpsSearchURLStr.replaceAll("%s", encodedTestQuery);

        final URL searchURL;
        try {
            searchURL = new URL(searchURLStr);
        } catch (final MalformedURLException e) {
            // Don't log exception to avoid leaking URL.
            Log.d(LOGTAG, "Malformed URL: returning invalid search query");
            return false;
        }

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) searchURL.openConnection();
            connection.setConnectTimeout(SEARCH_QUERY_VALIDATION_TIMEOUT_MILLIS);
            connection.setReadTimeout(SEARCH_QUERY_VALIDATION_TIMEOUT_MILLIS);

            // A non-error HTTP response is good enough as a sanity check, some search engines redirect to https.
            return connection.getResponseCode() < 400;

        } catch (final IOException e) {
            // Don't log exception to avoid leaking URL.
            Log.d(LOGTAG, "Failure to get response code from server: returning invalid search query");
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.getInputStream().close(); // HttpURLConnection.getResponseCode opens the InputStream.
                } catch (final IOException e) { } // Whatever.
                connection.disconnect();
            }
        }
    }
}

