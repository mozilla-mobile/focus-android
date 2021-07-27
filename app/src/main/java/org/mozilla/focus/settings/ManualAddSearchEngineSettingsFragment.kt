/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.settings

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.WorkerThread
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.Request.Redirect.FOLLOW
import mozilla.components.feature.search.ext.createSearchEngine
import org.mozilla.focus.R
import org.mozilla.focus.ext.requireComponents
import org.mozilla.focus.search.ManualAddSearchEnginePreference
import org.mozilla.focus.shortcut.IconGenerator
import org.mozilla.focus.state.AppAction
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.Settings
import org.mozilla.focus.utils.SupportUtils
import org.mozilla.focus.utils.UrlUtils
import org.mozilla.focus.utils.ViewUtils
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit

@Suppress("TooManyFunctions")
class ManualAddSearchEngineSettingsFragment : BaseSettingsFragment() {
    override fun onCreatePreferences(p0: Bundle?, p1: String?) {
        setHasOptionsMenu(true)

        addPreferencesFromResource(R.xml.manual_add_search_engine)
    }

    private var scope: CoroutineScope? = null
    private var menuItemForActiveAsyncTask: MenuItem? = null
    private var job: Job? = null

    override fun onResume() {
        super.onResume()

        updateTitle(R.string.action_option_add_search_engine)
    }

    override fun onPause() {
        super.onPause()
        setUiIsValidatingAsync(false, menuItemForActiveAsyncTask)
        menuItemForActiveAsyncTask = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_search_engine_manual_add, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val openLearnMore = {
            val tabId = requireComponents.tabsUseCases.addTab(
                SupportUtils.getSumoURLForTopic(requireContext(), SupportUtils.SumoTopic.ADD_SEARCH_ENGINE),
                selectTab = true,
                private = true
            )

            TelemetryWrapper.addSearchEngineLearnMoreEvent()

            requireComponents.appStore.dispatch(AppAction.OpenTab(tabId))
        }

        val saveSearchEngine = {
            val engineName = requireView().findViewById<EditText>(R.id.edit_engine_name).text.toString()
            val searchQuery = requireView().findViewById<EditText>(R.id.edit_search_string).text.toString()

            val pref = findManualAddSearchEnginePreference(R.string.pref_key_manual_add_search_engine)
            val engineValid = pref?.validateEngineNameAndShowError(engineName) ?: false
            val searchValid = pref?.validateSearchQueryAndShowError(searchQuery) ?: false
            val isPartialSuccess = engineValid && searchValid

            if (isPartialSuccess) {
                ViewUtils.hideKeyboard(view)
                setUiIsValidatingAsync(true, item)

                menuItemForActiveAsyncTask = item
                scope?.launch {
                    validateSearchEngine(engineName, searchQuery, requireComponents.client)
                }
            } else {
                TelemetryWrapper.saveCustomSearchEngineEvent(false)
            }
        }

        when (item.itemId) {
            R.id.learn_more -> openLearnMore()
            R.id.menu_save_search_engine -> saveSearchEngine()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        scope = CoroutineScope(Dispatchers.IO)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onDestroyView() {
        scope?.cancel()
        super.onDestroyView()
        if (view != null) ViewUtils.hideKeyboard(view)
    }

    private fun setUiIsValidatingAsync(isValidating: Boolean, saveMenuItem: MenuItem?) {
        val pref = findManualAddSearchEnginePreference(R.string.pref_key_manual_add_search_engine)

        if (isValidating) {
            view?.alpha = DISABLED_ALPHA
            // Delay showing the loading indicator to prevent it flashing on the screen
            job = scope?.launch {
                delay(LOADING_INDICATOR_DELAY)
                pref?.setProgressViewShown(isValidating)
            }
        } else {
            view?.alpha = 1f
            job?.cancel()
            pref?.setProgressViewShown(isValidating)
        }

        // Disable text entry until done validating
        val viewGroup = view as ViewGroup
        enableAllSubviews(!isValidating, viewGroup)

        saveMenuItem?.isEnabled = !isValidating
    }

    private fun enableAllSubviews(shouldEnable: Boolean, viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is ViewGroup) {
                enableAllSubviews(shouldEnable, child)
            } else {
                child.isEnabled = shouldEnable
            }
        }
    }

    private fun findManualAddSearchEnginePreference(id: Int): ManualAddSearchEnginePreference? {
        return findPreference(getString(id)) as? ManualAddSearchEnginePreference
    }

    companion object {
        private const val LOGTAG = "ManualAddSearchEngine"
        private const val SEARCH_QUERY_VALIDATION_TIMEOUT_MILLIS = 4000
        private const val VALID_RESPONSE_CODE_UPPER_BOUND = 300
        private const val DISABLED_ALPHA = 0.5f
        private const val LOADING_INDICATOR_DELAY: Long = 1000

        @WorkerThread
        @JvmStatic
        fun isValidSearchQueryURL(client: Client, query: String): Boolean {
            // we should share the code to substitute and normalize the search string (see SearchEngine.buildSearchUrl).
            val encodedTestQuery = Uri.encode("testSearchEngineValidation")

            val normalizedHttpsSearchURLStr = UrlUtils.normalize(query)
            val searchURLStr = normalizedHttpsSearchURLStr.replace("%s".toRegex(), encodedTestQuery)

            try { URL(searchURLStr) } catch (e: MalformedURLException) {
                // Don't log exception to avoid leaking URL.
                Log.d(LOGTAG, "Failure to get response code from server: returning invalid search query")
                return false
            }

            val request = Request(
                url = searchURLStr,
                connectTimeout = SEARCH_QUERY_VALIDATION_TIMEOUT_MILLIS.toLong() to TimeUnit.MILLISECONDS,
                readTimeout = SEARCH_QUERY_VALIDATION_TIMEOUT_MILLIS.toLong() to TimeUnit.MILLISECONDS,
                redirect = FOLLOW,
                private = true
            )

            return try {
                client.fetch(request).status < VALID_RESPONSE_CODE_UPPER_BOUND
            } catch (e: IOException) {
                Log.d(LOGTAG, "Failure to get response code from server: returning invalid search query")
                false
            }
        }
    }

    private suspend fun validateSearchEngine(engineName: String, query: String, client: Client) {
        val isValidSearchQuery = isValidSearchQueryURL(client, query)
        TelemetryWrapper.saveCustomSearchEngineEvent(isValidSearchQuery)

        withContext(Dispatchers.Main) {
            if (!isActive) {
                return@withContext
            }

            if (isValidSearchQuery) {
                requireComponents.searchUseCases.addSearchEngine(
                    createSearchEngine(
                        engineName,
                        query.toSearchUrl(),
                        IconGenerator.generateSearchEngineIcon(requireContext())
                    )
                )

                Snackbar.make(requireView(), R.string.search_add_confirmation, Snackbar.LENGTH_SHORT).show()
                Settings.getInstance(requireActivity()).setDefaultSearchEngineByName(engineName)

                requireComponents.appStore.dispatch(
                    AppAction.NavigateUp(requireComponents.store.state.selectedTabId)
                )
            } else {
                showServerError()
            }

            setUiIsValidatingAsync(false, menuItemForActiveAsyncTask)
            menuItemForActiveAsyncTask = null
        }
    }

    private fun showServerError() {
        val pref = findManualAddSearchEnginePreference(R.string.pref_key_manual_add_search_engine)
        pref?.setSearchQueryErrorText(getString(R.string.error_hostLookup_title))
    }
}

private fun String.toSearchUrl(): String {
    return replace("%s", "{searchTerms}")
}
