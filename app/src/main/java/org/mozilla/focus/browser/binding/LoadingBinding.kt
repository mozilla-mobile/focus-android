package org.mozilla.focus.browser.binding

import android.util.Log
import android.view.View
import android.widget.FrameLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import mozilla.components.browser.session.Session
import mozilla.components.browser.state.selector.findTabOrCustomTabOrSelectedTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged
import org.mozilla.focus.animation.TransitionDrawableGroup
import org.mozilla.focus.fragment.BrowserFragment
import org.mozilla.focus.widget.AnimatedProgressBar

private const val INITIAL_PROGRESS = 5
private const val ANIMATION_DURATION = 300

class LoadingBinding(
    store: BrowserStore,
    private val tabId: String,
    private val transitionGetter: () -> TransitionDrawableGroup,
    private val progressBar: AnimatedProgressBar,
    private val blockView: FrameLayout
) : AbstractBinding(store) {
    override suspend fun onState(flow: Flow<BrowserState>) {
        flow.mapNotNull { state -> state.findTabOrCustomTabOrSelectedTab(tabId) }
            .ifChanged { tab -> tab.content.loading }
            .collect { tab -> onLoadingStateChanged(tab) }
    }

    private fun onLoadingStateChanged(tab: SessionState) {
        Log.w("SKDBG", "onLoadingStateChanged(${tab.content.url}, ${tab.content.loading})")

        val transition = transitionGetter.invoke()

        if (tab.content.loading) {
            transition.resetTransition()

            progressBar.progress = INITIAL_PROGRESS
            progressBar.visibility = View.VISIBLE
        } else {
            if (progressBar.visibility == View.VISIBLE) {
                // We start a transition only if a page was just loading before
                // allowing to avoid issue #1179
                transition.startTransition(ANIMATION_DURATION)
                progressBar.visibility = View.GONE
            }

            // updateSecurityIcon(session)
        }

        blockView.visibility = if (tab.content.loading || tab.trackingProtection.enabled) View.GONE else View.VISIBLE


        // TODO: Separate binding?
        // updateToolbarButtonStates(loading)

//          TODO: Forward to menu - or better: Let menu subscribe itself
//        val menu = menuWeakReference!!.get()
//        menu?.updateLoading(loading)
    }
}