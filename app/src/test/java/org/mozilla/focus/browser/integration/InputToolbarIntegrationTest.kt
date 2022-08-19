/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.browser.integration

import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import mozilla.components.browser.domains.autocomplete.CustomDomainsProvider
import mozilla.components.browser.domains.autocomplete.ShippedDomainsProvider
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.whenever
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mozilla.focus.fragment.UrlInputFragment
import org.mozilla.focus.input.InputToolbarIntegration
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InputToolbarIntegrationTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var toolbar: BrowserToolbar

    @Mock
    private lateinit var fragment: UrlInputFragment
    @Mock
    private lateinit var shippedDomainsProvider: ShippedDomainsProvider
    @Mock
    private lateinit var customDomainProviderResult: CustomDomainsProvider
    @Mock
    private lateinit var fragmentView: View

    private lateinit var inputToolbarIntegration: InputToolbarIntegration

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)

        toolbar = BrowserToolbar(testContext)
        whenever(fragment.resources).thenReturn(testContext.resources)
        whenever(fragment.context).thenReturn(testContext)
        whenever(fragment.view).thenReturn(fragmentView)

        inputToolbarIntegration = InputToolbarIntegration(
            toolbar,
            fragment,
            shippedDomainsProvider,
            customDomainProviderResult
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN app fresh install WHEN input toolbar integration is starting THEN start browsing scope is populated`() {
        inputToolbarIntegration.start()

        assertNotEquals(null, inputToolbarIntegration.startBrowsingCfrScope)
    }

    @Test
    fun `GIVEN app fresh install WHEN input toolbar integration is stoping THEN start browsing scope is canceled`() {
        inputToolbarIntegration.stop()

        assertEquals(null, inputToolbarIntegration.startBrowsingCfrScope?.cancel())
    }
}
