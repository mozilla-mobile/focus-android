/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.exceptions

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import kotlinx.android.synthetic.main.fragment_exceptions_domains.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl
import org.mozilla.focus.R
import org.mozilla.focus.ext.components
import org.mozilla.focus.ext.requireComponents
import org.mozilla.focus.state.AppAction
import org.mozilla.focus.telemetry.TelemetryWrapper
import kotlin.collections.forEach as withEach

class ExceptionsRemoveFragment : ExceptionsListFragment() {

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_autocomplete_remove, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.remove -> {
            removeSelectedDomains(requireActivity().applicationContext)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun removeSelectedDomains(context: Context) {
        val exceptions = (exceptionList.adapter as DomainListAdapter).selection()
        TelemetryWrapper.removeExceptionDomains(exceptions.size)
        if (exceptions.isNotEmpty()) {
            launch(Main) {
                withContext(Dispatchers.IO) {
                    val domains = exceptions.map { it.url.tryGetHostFromUrl() }
                    ExceptionDomains.remove(context, domains)
                }
                exceptions.withEach { exception ->
                    context.components.trackingProtectionUseCases.removeException(exception)
                }

                requireComponents.appStore.dispatch(
                    AppAction.NavigateUp(requireComponents.store.state.selectedTabId)
                )
            }
        }
    }

    override fun isSelectionMode() = true

    override fun onResume() {
        super.onResume()

        updateTitle(R.string.preference_autocomplete_title_remove)
    }
}
