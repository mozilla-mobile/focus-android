package org.mozilla.focus.searchsuggestions

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchSuggestionsViewModelTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var observer: Observer<String>

    private lateinit var lifecycle: LifecycleRegistry
    private lateinit var viewModel: SearchSuggestionsViewModel

    @Before
    fun setup() {
        lifecycle = LifecycleRegistry(mock(LifecycleOwner::class.java))
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        MockitoAnnotations.initMocks(this)

        viewModel = SearchSuggestionsViewModel(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun setSearchQuery() {
        viewModel.searchQuery.observeForever(observer)

        viewModel.setSearchQuery("Mozilla")
        verify(observer).onChanged("Mozilla")
    }

    @Test
    fun alwaysSearchSelected() {
        viewModel.selectedSearchSuggestion.observeForever(observer)

        viewModel.selectSearchSuggestion("mozilla.com", true)
        verify(observer).onChanged("mozilla.com")
        assertEquals(true, viewModel.alwaysSearch)
    }
}
