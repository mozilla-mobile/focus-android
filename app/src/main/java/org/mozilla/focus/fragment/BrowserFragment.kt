/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.webkit.MimeTypeMap
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_browser.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.browser.state.selector.privateTabs
import mozilla.components.browser.state.state.CustomTabConfig
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.engine.HitResult
import mozilla.components.feature.app.links.AppLinksFeature
import mozilla.components.feature.contextmenu.ContextMenuFeature
import mozilla.components.feature.downloads.AbstractFetchDownloadService
import mozilla.components.feature.downloads.DownloadsFeature
import mozilla.components.feature.downloads.manager.FetchDownloadManager
import mozilla.components.feature.downloads.share.ShareDownloadFeature
import mozilla.components.feature.prompts.PromptFeature
import mozilla.components.feature.session.SessionFeature
import mozilla.components.feature.tabs.WindowFeature
import mozilla.components.feature.top.sites.TopSitesConfig
import mozilla.components.feature.top.sites.TopSitesFeature
import mozilla.components.lib.crash.Crash
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl
import org.mozilla.focus.GleanMetrics.TabCount
import org.mozilla.focus.GleanMetrics.TrackingProtection
import org.mozilla.focus.R
import org.mozilla.focus.activity.InstallFirefoxActivity
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.browser.binding.TabCountBinding
import org.mozilla.focus.browser.integration.BrowserMenuController
import org.mozilla.focus.browser.integration.BrowserToolbarIntegration
import org.mozilla.focus.browser.integration.FindInPageIntegration
import org.mozilla.focus.browser.integration.FullScreenIntegration
import org.mozilla.focus.contextmenu.ContextMenuCandidates
import org.mozilla.focus.downloads.DownloadService
import org.mozilla.focus.engine.EngineSharedPreferencesListener
import org.mozilla.focus.exceptions.ExceptionDomains
import org.mozilla.focus.ext.accessibilityManager
import org.mozilla.focus.ext.components
import org.mozilla.focus.ext.disableDynamicBehavior
import org.mozilla.focus.ext.enableDynamicBehavior
import org.mozilla.focus.ext.ifCustomTab
import org.mozilla.focus.ext.isCustomTab
import org.mozilla.focus.ext.requireComponents
import org.mozilla.focus.ext.settings
import org.mozilla.focus.ext.showAsFixed
import org.mozilla.focus.ext.titleOrDomain
import org.mozilla.focus.menu.browser.DefaultBrowserMenu
import org.mozilla.focus.open.OpenWithFragment
import org.mozilla.focus.settings.privacy.ConnectionDetailsPanel
import org.mozilla.focus.settings.privacy.TrackingProtectionPanel
import org.mozilla.focus.state.AppAction
import org.mozilla.focus.state.Screen
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.topsites.DefaultTopSitesStorage.Companion.TOP_SITES_MAX_LIMIT
import org.mozilla.focus.topsites.DefaultTopSitesView
import org.mozilla.focus.utils.Browsers
import org.mozilla.focus.utils.FocusSnackbar
import org.mozilla.focus.utils.FocusSnackbarDelegate
import org.mozilla.focus.utils.Settings
import org.mozilla.focus.utils.StatusBarUtils
import org.mozilla.focus.utils.SupportUtils
import org.mozilla.focus.widget.FloatingEraseButton
import org.mozilla.focus.widget.FloatingSessionsButton

/**
 * Fragment for displaying the browser UI.
 */
@Suppress("LargeClass", "TooManyFunctions")
class BrowserFragment :
    BaseFragment(),
    View.OnClickListener,
    AccessibilityManager.AccessibilityStateChangeListener {

    private var statusBar: View? = null
    private var popupTint: FrameLayout? = null

    private lateinit var engineView: EngineView
    private lateinit var toolbar: BrowserToolbar
    private lateinit var eraseFab: FloatingEraseButton
    private lateinit var sessionsFab: FloatingSessionsButton

    private val findInPageIntegration = ViewBoundFeatureWrapper<FindInPageIntegration>()
    private val fullScreenIntegration = ViewBoundFeatureWrapper<FullScreenIntegration>()

    private val sessionFeature = ViewBoundFeatureWrapper<SessionFeature>()
    private val promptFeature = ViewBoundFeatureWrapper<PromptFeature>()
    private val contextMenuFeature = ViewBoundFeatureWrapper<ContextMenuFeature>()
    private val downloadsFeature = ViewBoundFeatureWrapper<DownloadsFeature>()
    private val shareDownloadFeature = ViewBoundFeatureWrapper<ShareDownloadFeature>()
    private val windowFeature = ViewBoundFeatureWrapper<WindowFeature>()
    private val appLinksFeature = ViewBoundFeatureWrapper<AppLinksFeature>()
    private val topSitesFeature = ViewBoundFeatureWrapper<TopSitesFeature>()

    private val toolbarIntegration = ViewBoundFeatureWrapper<BrowserToolbarIntegration>()

    private val tabCountBinding = ViewBoundFeatureWrapper<TabCountBinding>()
    private lateinit var trackingProtectionPanel: TrackingProtectionPanel
    /**
     * The ID of the tab associated with this fragment.
     */
    private val tabId: String
        get() = requireArguments().getString(ARGUMENT_SESSION_UUID)
            ?: throw IllegalAccessError("No session ID set on fragment")

    /**
     * The tab associated with this fragment.
     */
    val tab: SessionState
        get() = requireComponents.store.state.findTabOrCustomTab(tabId)
            // Workaround for tab not existing temporarily.
            ?: createTab("about:blank")

    @Suppress("LongMethod", "ComplexMethod")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_browser, container, false)

        statusBar = view.findViewById(R.id.status_bar_background)

        popupTint = view.findViewById(R.id.popup_tint)

        requireContext().accessibilityManager.addAccessibilityStateChangeListener(this)

        return view
    }

    @Suppress("ComplexCondition", "LongMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val components = requireComponents

        engineView = (view.findViewById<View>(R.id.engineView) as EngineView)
        toolbar = view.findViewById(R.id.browserToolbar)
        eraseFab = view.findViewById(R.id.erase)
        sessionsFab = view.findViewById(R.id.tabs)

        findInPageIntegration.set(
            FindInPageIntegration(
                components.store,
                view.findViewById(R.id.find_in_page),
                engineView
            ),
            this, view
        )

        fullScreenIntegration.set(
            FullScreenIntegration(
                requireActivity(),
                components.store,
                tab.id,
                components.sessionUseCases,
                requireContext().settings,
                toolbar,
                statusBar!!,
                engineView,
                eraseFab,
                sessionsFab
            ),
            this, view
        )

        contextMenuFeature.set(
            ContextMenuFeature(
                parentFragmentManager,
                components.store,
                ContextMenuCandidates.get(
                    requireContext(),
                    components.tabsUseCases,
                    components.contextMenuUseCases,
                    components.appLinksUseCases,
                    view,
                    FocusSnackbarDelegate(view)
                ),
                engineView,
                requireComponents.contextMenuUseCases,
                tabId,
                additionalNote = { hitResult -> getAdditionalNote(hitResult) }
            ),
            this, view
        )

        sessionFeature.set(
            SessionFeature(
                components.store,
                components.sessionUseCases.goBack,
                engineView,
                tab.id
            ),
            this, view
        )

        promptFeature.set(
            PromptFeature(
                fragment = this,
                store = components.store,
                customTabId = tryGetCustomTabId(),
                fragmentManager = parentFragmentManager,
                onNeedToRequestPermissions = { permissions ->
                    requestInPlacePermissions(permissions) { result ->
                        promptFeature.get()?.onPermissionsResult(
                            result.keys.toTypedArray(),
                            result.values.map {
                                when (it) {
                                    true -> PackageManager.PERMISSION_GRANTED
                                    false -> PackageManager.PERMISSION_DENIED
                                }
                            }.toIntArray()
                        )
                    }
                }
            ),
            this, view
        )

        downloadsFeature.set(
            DownloadsFeature(
                requireContext().applicationContext,
                components.store,
                components.downloadsUseCases,
                fragmentManager = childFragmentManager,
                downloadManager = FetchDownloadManager(
                    requireContext().applicationContext,
                    components.store,
                    DownloadService::class
                ),
                onNeedToRequestPermissions = { permissions ->
                    requestInPlacePermissions(permissions) { result ->
                        Log.d("Blabla", "Browser fragment")
                        downloadsFeature.get()?.onPermissionsResult(
                            result.keys.toTypedArray(),
                            result.values.map {
                                when (it) {
                                    true -> PackageManager.PERMISSION_GRANTED
                                    false -> PackageManager.PERMISSION_DENIED
                                }
                            }.toIntArray()
                        )
                    }
                },
                onDownloadStopped = { state, _, status ->
                    showDownloadSnackbar(state, status)
                }
            ),
            this, view
        )

        shareDownloadFeature.set(
            ShareDownloadFeature(
                context = requireContext().applicationContext,
                httpClient = components.client,
                store = components.store,
                tabId = tab.id
            ),
            this, view
        )

        appLinksFeature.set(
            feature = AppLinksFeature(
                requireContext(),
                store = components.store,
                sessionId = tabId,
                fragmentManager = parentFragmentManager,
                launchInApp = { requireContext().settings.openLinksInExternalApp },
                loadUrlUseCase = requireContext().components.sessionUseCases.loadUrl
            ),
            owner = this,
            view = view
        )

        topSitesFeature.set(
            feature = TopSitesFeature(
                view = DefaultTopSitesView(requireComponents.appStore),
                storage = requireComponents.topSitesStorage,
                config = {
                    TopSitesConfig(
                        totalSites = TOP_SITES_MAX_LIMIT,
                        frecencyConfig = null
                    )
                }
            ),
            owner = this,
            view = view
        )

        customizeToolbar(view)

        val customTabConfig = tab.ifCustomTab()?.config
        if (customTabConfig != null) {
            initialiseCustomTabUi(customTabConfig)

            // TODO Add custom tabs window feature support
            // We to add support for Custom Tabs here, however in order to send the window request
            // back to us through the intent system, we need to register a unique schema that we
            // can handle. For example, Fenix Nighlyt does this today with `fenix-nightly://`.
        } else {
            initialiseNormalBrowserUi(view)

            windowFeature.set(
                feature = WindowFeature(
                    store = components.store,
                    tabsUseCases = components.tabsUseCases
                ),
                owner = this,
                view = view
            )
        }
    }

    override fun onAccessibilityStateChanged(enabled: Boolean) = when (enabled) {
        false -> toolbar.enableDynamicBehavior(requireContext(), engineView)
        true -> {
            toolbar.disableDynamicBehavior(engineView)
            toolbar.showAsFixed(requireContext(), engineView)
        }
    }

    private fun getAdditionalNote(hitResult: HitResult): String? {
        return if ((hitResult is HitResult.IMAGE_SRC || hitResult is HitResult.IMAGE) &&
            hitResult.src.isNotEmpty()
        ) {
            getString(R.string.contextmenu_erased_images_note2, getString(R.string.app_name))
        } else {
            null
        }
    }

    private fun customizeToolbar(view: View) {
        val browserToolbar = view.findViewById<BrowserToolbar>(R.id.browserToolbar)
        val controller = BrowserMenuController(
            requireComponents.sessionUseCases,
            requireComponents.appStore,
            requireComponents.store,
            requireComponents.topSitesUseCases,
            tabId,
            ::shareCurrentUrl,
            ::setShouldRequestDesktop,
            ::showAddToHomescreenDialog,
            ::showFindInPageBar,
            ::openSelectBrowser,
            ::openInBrowser
        )

        if (tab.ifCustomTab()?.config == null) {
            val browserMenu = DefaultBrowserMenu(
                context = requireContext(),
                appStore = requireComponents.appStore,
                store = requireComponents.store,
                isPinningSupported = ShortcutManagerCompat.isRequestPinShortcutSupported(
                    requireContext()
                ),
                onItemTapped = { controller.handleMenuInteraction(it) }
            )
            browserToolbar.display.menuBuilder = browserMenu.menuBuilder
        }

        toolbarIntegration.set(
            BrowserToolbarIntegration(
                requireComponents.store,
                toolbar = browserToolbar,
                fragment = this,
                controller = controller,
                customTabId = tryGetCustomTabId(),
                customTabsUseCases = requireComponents.customTabsUseCases,
                sessionUseCases = requireComponents.sessionUseCases,
                onUrlLongClicked = ::onUrlLongClicked
            ),
            owner = this,
            view = browserToolbar
        )
    }

    private fun initialiseNormalBrowserUi(view: View) {
        if (!requireContext().settings.isAccessibilityEnabled()) {
            toolbar.enableDynamicBehavior(requireContext(), engineView)
        } else {
            toolbar.showAsFixed(requireContext(), engineView)
        }

        val eraseButton = view.findViewById<FloatingEraseButton>(R.id.erase)
        eraseButton.setOnClickListener(this)

        val tabsButton = view.findViewById<FloatingSessionsButton>(R.id.tabs)
        tabsButton.setOnClickListener(this)

        tabCountBinding.set(
            TabCountBinding(
                requireComponents.store,
                eraseButton,
                tabsButton
            ),
            owner = this,
            view = eraseButton
        )
    }

    private fun initialiseCustomTabUi(customTabConfig: CustomTabConfig) {
        // Unfortunately there's no simpler way to have the FAB only in normal-browser mode.
        // - ViewStub: requires splitting attributes for the FAB between the ViewStub, and actual FAB layout file.
        //             Moreover, the layout behaviour just doesn't work unless you set it programatically.
        // - View.GONE: doesn't work because the layout-behaviour makes the FAB visible again when scrolling.
        // - Adding at runtime: works, but then we need to use a separate layout file (and you need
        //   to set some attributes programatically, same as ViewStub).
        val eraseContainer = eraseFab.parent as ViewGroup
        eraseContainer.removeView(eraseFab)

        eraseContainer.removeView(sessionsFab)

        if (customTabConfig.enableUrlbarHiding && !requireContext().settings.isAccessibilityEnabled()) {
            toolbar.enableDynamicBehavior(requireContext(), engineView)
        } else {
            toolbar.showAsFixed(requireContext(), engineView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireContext().accessibilityManager.removeAccessibilityStateChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        // This fragment might get destroyed before the user left immersive mode (e.g. by opening another URL from an
        // app). In this case let's leave immersive mode now when the fragment gets destroyed.
        fullScreenIntegration.get()?.exitImmersiveModeIfNeeded()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        promptFeature.withFeature { it.onActivityResult(requestCode, data, resultCode) }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun showCrashReporter(crash: Crash) {
        val fragmentManager = requireActivity().supportFragmentManager

        if (crashReporterIsVisible()) {
            // We are already displaying the crash reporter
            // No need to show another one.
            return
        }

        val crashReporterFragment = CrashReporterFragment.create()

        crashReporterFragment.onCloseTabPressed = { sendCrashReport ->
            if (sendCrashReport) {
                val crashReporter = requireComponents.crashReporter

                GlobalScope.launch(Dispatchers.IO) { crashReporter.submitReport(crash) }
            }

            requireComponents.sessionUseCases.crashRecovery.invoke()
            erase()
            hideCrashReporter()
        }

        fragmentManager
            .beginTransaction()
            .addToBackStack(null)
            .add(R.id.crash_container, crashReporterFragment, CrashReporterFragment.FRAGMENT_TAG)
            .commit()

        crash_container.visibility = View.VISIBLE
        tabs.hide()
        erase.hide()
    }

    private fun hideCrashReporter() {
        val fragmentManager = requireActivity().supportFragmentManager
        val fragment = fragmentManager.findFragmentByTag(CrashReporterFragment.FRAGMENT_TAG)
            ?: return

        fragmentManager
            .beginTransaction()
            .remove(fragment)
            .commit()

        crash_container.visibility = View.GONE
        tabs.show()
        erase.show()
    }

    fun crashReporterIsVisible(): Boolean = requireActivity().supportFragmentManager.let {
        it.findFragmentByTag(CrashReporterFragment.FRAGMENT_TAG)?.isVisible ?: false
    }

    private fun showDownloadSnackbar(
        state: DownloadState,
        status: DownloadState.Status
    ) {
        if (status != DownloadState.Status.COMPLETED) {
            // We currently only show an in-app snackbar for completed downloads.
            return
        }

        val snackbar = FocusSnackbar.make(
            requireView(),
            (requireView().findViewById(R.id.tabs) as? FloatingSessionsButton)?.visibility == View.VISIBLE,
            Snackbar.LENGTH_LONG
        )

        snackbar.setText(
            String.format(
                requireContext().getString(R.string.download_snackbar_finished),
                state.fileName
            )
        )

        snackbar.setAction(getString(R.string.download_snackbar_open)) {
            val opened = AbstractFetchDownloadService.openFile(
                applicationContext = requireContext().applicationContext,
                download = state
            )

            if (!opened) {
                val extension = MimeTypeMap.getFileExtensionFromUrl(state.filePath)

                Toast.makeText(
                    context,
                    getString(
                        mozilla.components.feature.downloads.R.string.mozac_feature_downloads_open_not_supported1,
                        extension
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        snackbar.show()
    }

    private fun showAddToHomescreenDialog() {
        val fragmentManager = childFragmentManager

        if (fragmentManager.findFragmentByTag(AddToHomescreenDialogFragment.FRAGMENT_TAG) != null) {
            // We are already displaying a homescreen dialog fragment (Probably a restored fragment).
            // No need to show another one.
            return
        }

        val requestDesktop = tab.content.desktopMode

        val addToHomescreenDialogFragment = AddToHomescreenDialogFragment.newInstance(
            tab.content.url,
            tab.content.titleOrDomain,
            tab.trackingProtection.enabled,
            requestDesktop = requestDesktop
        )

        try {
            addToHomescreenDialogFragment.show(
                fragmentManager,
                AddToHomescreenDialogFragment.FRAGMENT_TAG
            )
        } catch (e: IllegalStateException) {
            // It can happen that at this point in time the activity is already in the background
            // and onSaveInstanceState() has already been called. Fragment transactions are not
            // allowed after that anymore. It's probably safe to guess that the user might not
            // be interested in adding to homescreen now.
        }
    }

    override fun onResume() {
        super.onResume()

        StatusBarUtils.getStatusBarHeight(statusBar) { statusBarHeight ->
            statusBar!!.layoutParams.height = statusBarHeight
        }
    }

    @Suppress("ComplexMethod", "ReturnCount")
    fun onBackPressed(): Boolean {
        if (findInPageIntegration.onBackPressed()) {
            return true
        } else if (fullScreenIntegration.onBackPressed()) {
            return true
        } else if (sessionFeature.get()?.onBackPressed() == true) {
            return true
        } else if (tab.source is SessionState.Source.Internal.TextSelection) {
            erase()
            return true
        } else {
            if (tab.source is SessionState.Source.External || tab.isCustomTab()) {
                TelemetryWrapper.eraseBackToAppEvent()

                // This session has been started from a VIEW intent. Go back to the previous app
                // immediately and erase the current browsing session.
                erase()

                // If there are no other sessions then we remove the whole task because otherwise
                // the old session might still be partially visible in the app switcher.
                if (requireComponents.store.state.privateTabs.isEmpty()) {
                    requireActivity().finishAndRemoveTask()
                } else {
                    requireActivity().finish()
                }
                // We can't show a snackbar outside of the app. So let's show a toast instead.
                Toast.makeText(context, R.string.feedback_erase_custom_tab, Toast.LENGTH_SHORT).show()
            } else {
                // Just go back to the home screen.
                TelemetryWrapper.eraseBackToHomeEvent()

                erase()
            }
        }

        return true
    }

    fun erase() {
        val context = context

        // Notify the user their session has been erased if Talk Back is enabled:
        if (context != null) {
            val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            if (manager.isEnabled) {
                val event = AccessibilityEvent.obtain()
                event.eventType = AccessibilityEvent.TYPE_ANNOUNCEMENT
                event.className = javaClass.name
                event.packageName = requireContext().packageName
                event.text.add(getString(R.string.feedback_erase2))
            }
        }

        requireComponents.tabsUseCases.removeTab(tab.id)
        requireComponents.appStore.dispatch(
            AppAction.NavigateUp(
                requireComponents.store.state.selectedTabId
            )
        )
    }

    private fun shareCurrentUrl() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, tab.content.url)

        val title = tab.content.title
        if (title.isNotEmpty()) {
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, title)
        }

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_dialog_title)))

        TelemetryWrapper.shareEvent()
    }

    private fun openInBrowser() {
        // Release the session from this view so that it can immediately be rendered by a different view
        sessionFeature.get()?.release()

        requireComponents.customTabsUseCases.migrate(tab.id)

        val intent = Intent(context, MainActivity::class.java)
        intent.action = Intent.ACTION_MAIN
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)

        TelemetryWrapper.openFullBrowser()

        val activity = activity
        activity?.finish()
    }

    internal fun edit() {
        requireComponents.appStore.dispatch(
            AppAction.EditAction(tab.id)
        )
    }

    @Suppress("ComplexMethod")
    override fun onClick(view: View) {
        val openedTabs = view.context.components.store.state.tabs.size
        when (view.id) {
            R.id.erase -> {
                TabCount.eraseButtonTapped.record(TabCount.EraseButtonTappedExtra(openedTabs))
                erase()
            }

            R.id.tabs -> {
                requireComponents.appStore.dispatch(AppAction.ShowTabs)

                TabCount.sessionButtonTapped.record(TabCount.SessionButtonTappedExtra(openedTabs))
            }

            R.id.open_in_firefox_focus -> {
                openInBrowser()
            }

            R.id.share -> {
                shareCurrentUrl()
            }

            R.id.settings -> {
                requireComponents.appStore.dispatch(
                    AppAction.OpenSettings(page = Screen.Settings.Page.Start)
                )
            }

            R.id.open_default -> {
                val browsers = Browsers(requireContext(), tab.content.url)

                val defaultBrowser = browsers.defaultBrowser
                    ?: throw IllegalStateException("<Open with \$Default> was shown when no default browser is set")
                // We only add this menu item when a third party default exists, in
                // BrowserMenuAdapter.initializeMenu()

                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tab.content.url))
                intent.setPackage(defaultBrowser.packageName)
                startActivity(intent)

                if (browsers.isFirefoxDefaultBrowser) {
                    TelemetryWrapper.openFirefoxEvent()
                } else {
                    TelemetryWrapper.openDefaultAppEvent()
                }
            }

            R.id.open_select_browser -> { openSelectBrowser() }

            R.id.help -> {
                requireComponents.tabsUseCases.addTab(
                    SupportUtils.HELP_URL,
                    source = SessionState.Source.Internal.Menu,
                    selectTab = true,
                    private = true
                )
            }

            R.id.stop -> {
                requireComponents.sessionUseCases.stopLoading(tabId)
            }

            R.id.refresh -> {
                requireComponents.sessionUseCases.reload(tabId)
            }

            R.id.forward -> {
                requireComponents.sessionUseCases.goForward(tabId)
            }

            R.id.add_to_homescreen -> { showAddToHomescreenDialog() }

            R.id.report_site_issue -> {
                val reportUrl = String.format(SupportUtils.REPORT_SITE_ISSUE_URL, tab.content.url)
                requireComponents.tabsUseCases.addTab(
                    reportUrl,
                    source = SessionState.Source.Internal.Menu,
                    selectTab = true,
                    private = true
                )

                TelemetryWrapper.reportSiteIssueEvent()
            }

            R.id.find_in_page -> { showFindInPageBar() }

            else -> throw IllegalArgumentException("Unhandled menu item in BrowserFragment")
        }
    }

    private fun showFindInPageBar() {
        findInPageIntegration.get()?.show(tab)
        TelemetryWrapper.findInPageMenuEvent()
    }

    private fun openSelectBrowser() {
        val browsers = Browsers(requireContext(), tab.content.url)

        val apps = browsers.installedBrowsers
        val store = if (browsers.hasFirefoxBrandedBrowserInstalled())
            null
        else
            InstallFirefoxActivity.resolveAppStore(requireContext())

        val fragment = OpenWithFragment.newInstance(
            apps,
            tab.content.url,
            store
        )
        @Suppress("DEPRECATION")
        fragment.show(requireFragmentManager(), OpenWithFragment.FRAGMENT_TAG)

        TelemetryWrapper.openSelectionEvent()
    }

    internal fun closeCustomTab() {
        TelemetryWrapper.closeCustomTabEvent()

        requireComponents.customTabsUseCases.remove(tab.id)

        requireActivity().finish()

        TelemetryWrapper.closeCustomTabEvent()
    }

    fun setShouldRequestDesktop(enabled: Boolean) {
        if (enabled) {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(
                    requireContext().getString(R.string.has_requested_desktop),
                    true
                ).apply()
        }
        TelemetryWrapper.desktopRequestCheckEvent(enabled)
        requireComponents.sessionUseCases.requestDesktopSite(enabled, tab.id)
    }

    fun showTrackingProtectionPanel() {
        trackingProtectionPanel = TrackingProtectionPanel(
            context = requireContext(),
            tabUrl = tab.content.url,
            isTrackingProtectionOn = tab.trackingProtection.ignoredOnTrackingProtection.not(),
            isConnectionSecure = tab.content.securityInfo.secure,
            blockedTrackersCount = requireContext().settings
                .getTotalBlockedTrackersCount(),
            toggleTrackingProtection = ::toggleTrackingProtection,
            updateTrackingProtectionPolicy = { tracker, isEnabled ->
                EngineSharedPreferencesListener(requireContext())
                    .updateTrackingProtectionPolicy(
                        source = EngineSharedPreferencesListener.ChangeSource.PANEL.source,
                        tracker = tracker,
                        isEnabled = isEnabled
                    )
                reloadCurrentTab()
            },
            showConnectionInfo = ::showConnectionInfo
        )
        trackingProtectionPanel.show()
    }

    private fun reloadCurrentTab() {
        requireComponents.sessionUseCases.reload(tab.id)
    }

    private fun showConnectionInfo() {
        val connectionInfoPanel = ConnectionDetailsPanel(
            context = requireContext(),
            tabTitle = tab.content.title,
            tabUrl = tab.content.url,
            isConnectionSecure = tab.content.securityInfo.secure,
            goBack = { trackingProtectionPanel.show() }
        )
        trackingProtectionPanel.hide()
        connectionInfoPanel.show()
    }

    private fun toggleTrackingProtection(enable: Boolean) {
        val context = requireContext()
        with(requireComponents) {
            if (enable) {
                ExceptionDomains.remove(context, listOf(tab.content.url.tryGetHostFromUrl()))
                trackingProtectionUseCases.removeException(tab.id)
            } else {
                ExceptionDomains.add(context, tab.content.url.tryGetHostFromUrl())
                trackingProtectionUseCases.addException(tab.id)
            }
        }

        reloadCurrentTab()

        TrackingProtection.hasEverChangedEtp.set(true)
        TrackingProtection.trackingProtectionChanged.record(
            TrackingProtection.TrackingProtectionChangedExtra(
                isEnabled = enable
            )
        )
    }

    private fun onUrlLongClicked(): Boolean {
        val context = activity ?: return false

        return if (tab.isCustomTab()) {
            val clipBoard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val uri = Uri.parse(tab.content.url)
            clipBoard.setPrimaryClip(ClipData.newRawUri("Uri", uri))
            Toast.makeText(context, getString(R.string.custom_tab_copy_url_action), Toast.LENGTH_SHORT).show()
            true
        } else {
            false
        }
    }

    private fun tryGetCustomTabId() = if (tab.isCustomTab()) {
        tab.id
    } else {
        null
    }

    fun handleTabCrash(crash: Crash) {
        showCrashReporter(crash)
    }

    companion object {
        const val FRAGMENT_TAG = "browser"

        private const val ARGUMENT_SESSION_UUID = "sessionUUID"

        fun createForTab(tabId: String): BrowserFragment {
            val fragment = BrowserFragment()
            fragment.arguments = Bundle().apply {
                putString(ARGUMENT_SESSION_UUID, tabId)
            }
            return fragment
        }
    }
}
