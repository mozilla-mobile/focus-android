/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser.integration

import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SecurityInfoState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.browser.toolbar.display.DisplayToolbar.Indicators
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.whenever
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mozilla.focus.browser.DisplayToolbar
import org.mozilla.focus.fragment.BrowserFragment
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowserToolbarIntegrationTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val selectedTab = createSecureTab()

    private lateinit var toolbar: BrowserToolbar

    @Mock
    private lateinit var fragment: BrowserFragment

    private lateinit var browserToolbarIntegration: BrowserToolbarIntegration

    @Mock
    private lateinit var toolbarView: DisplayToolbar

    @Mock
    private lateinit var fragmentView: View

    private lateinit var store: BrowserStore

    @Before
    @ExperimentalCoroutinesApi
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        store = spy(
            BrowserStore(
                initialState = BrowserState(
                    tabs = listOf(selectedTab),
                    selectedTabId = selectedTab.id
                )
            )
        )

        toolbar = BrowserToolbar(testContext)

        whenever(fragment.resources).thenReturn(testContext.resources)
        whenever(fragment.context).thenReturn(testContext)
        whenever(fragment.view).thenReturn(fragmentView)

        browserToolbarIntegration = spy(
            BrowserToolbarIntegration(
                store,
                toolbar,
                toolbarView,
                fragment,
                mock(),
                mock(),
                mock(),
                { false },
                inTesting = true
            )
        )
    }

    @After
    @ExperimentalCoroutinesApi
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `WHEN starting THEN observe security changes`() {

        doNothing().`when`(browserToolbarIntegration).observerSecurityIndicatorChanges()

        browserToolbarIntegration.start()

        verify(browserToolbarIntegration).observerSecurityIndicatorChanges()
    }

    @Test
    fun `WHEN stopping THEN stop observe security changes`() {

        doNothing().`when`(browserToolbarIntegration).stopObserverSecurityIndicatorChanges()

        browserToolbarIntegration.stop()

        verify(browserToolbarIntegration).stopObserverSecurityIndicatorChanges()
    }

    @Test
    fun `GIVEN an insecure site WHEN observing security changes THEN add the security icon`() {
        browserToolbarIntegration.start()

        updateSecurityStatus(secure = false)

        verify(browserToolbarIntegration).addSecurityIndicator()
        assertEquals(listOf(Indicators.SECURITY), toolbar.display.indicators)
    }

    @Test
    fun `GIVEN an about site WHEN observing security changes THEN DO NOT add the security icon`() {
        browserToolbarIntegration.start()

        updateTabUrl("about:")

        verify(browserToolbarIntegration, times(0)).addSecurityIndicator()
        assertEquals(listOf(Indicators.TRACKING_PROTECTION), toolbar.display.indicators)
    }

    @Test
    fun `GIVEN a secure site after a previous insecure site WHEN observing security changes THEN add the tracking protection icon`() {
        browserToolbarIntegration.start()

        updateSecurityStatus(secure = false)

        verify(browserToolbarIntegration).addSecurityIndicator()

        updateSecurityStatus(secure = true)

        verify(browserToolbarIntegration).addTrackingProtectionIndicator()
        assertEquals(listOf(Indicators.TRACKING_PROTECTION), toolbar.display.indicators)
    }

    private fun updateSecurityStatus(secure: Boolean) {
        store.dispatch(
            ContentAction.UpdateSecurityInfoAction(
                selectedTab.id,
                SecurityInfoState(
                    secure = secure,
                    host = "mozilla.org",
                    issuer = "Mozilla"
                )
            )
        ).joinBlocking()

        testDispatcher.advanceUntilIdle()
    }

    private fun updateTabUrl(url: String) {
        store.dispatch(
            ContentAction.UpdateUrlAction(selectedTab.id, url)
        ).joinBlocking()

        testDispatcher.advanceUntilIdle()
    }

    private fun createSecureTab(): TabSessionState {
        val tab = createTab("https://www.mozilla.org", id = "1")
        return tab.copy(
            content = tab.content.copy(securityInfo = SecurityInfoState(secure = true))
        )
    }
}
