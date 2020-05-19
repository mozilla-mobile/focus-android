/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.Manifest
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.webkit.CookieManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.fragment_browser.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.state.selector.findTab
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.engine.content.blocking.Tracker
import mozilla.components.feature.contextmenu.ContextMenuCandidate
import mozilla.components.feature.contextmenu.ContextMenuFeature
import mozilla.components.feature.findinpage.FindInPageFeature
import mozilla.components.feature.findinpage.view.FindInPageBar
import mozilla.components.feature.session.FullScreenFeature
import mozilla.components.feature.session.SessionFeature
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.lib.crash.Crash
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import mozilla.components.support.utils.ColorUtils
import mozilla.components.support.utils.DownloadUtils
import mozilla.components.support.utils.DrawableUtils
import org.mozilla.focus.R
import org.mozilla.focus.activity.InstallFirefoxActivity
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.animation.TransitionDrawableGroup
import org.mozilla.focus.biometrics.BiometricAuthenticationDialogFragment
import org.mozilla.focus.biometrics.BiometricAuthenticationHandler
import org.mozilla.focus.biometrics.Biometrics
import org.mozilla.focus.broadcastreceiver.DownloadBroadcastReceiver
import org.mozilla.focus.browser.DisplayToolbar
import org.mozilla.focus.browser.binding.*
import org.mozilla.focus.ext.isSearch
import org.mozilla.focus.ext.requireComponents
import org.mozilla.focus.findinpage.FindInPageCoordinator
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.menu.browser.BrowserMenu
import org.mozilla.focus.observer.LoadTimeObserver
import org.mozilla.focus.open.OpenWithFragment
import org.mozilla.focus.popup.PopupUtils
import org.mozilla.focus.session.removeAndCloseAllSessions
import org.mozilla.focus.session.removeAndCloseSession
import org.mozilla.focus.session.ui.SessionsSheetFragment
import org.mozilla.focus.telemetry.CrashReporterWrapper
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.*
import org.mozilla.focus.web.Download
import org.mozilla.focus.widget.AnimatedProgressBar
import org.mozilla.focus.widget.FloatingEraseButton
import org.mozilla.focus.widget.FloatingSessionsButton
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext

/**
 * Fragment for displaying the browser UI.
 */
@Suppress("LargeClass", "TooManyFunctions")
class BrowserFragment : LocaleAwareFragment(), LifecycleObserver, View.OnClickListener,
    DownloadDialogFragment.DownloadDialogListener, View.OnLongClickListener,
    BiometricAuthenticationDialogFragment.BiometricAuthenticationListener,
    CoroutineScope {

    private var pendingDownload: Download? = null
    private var backgroundTransitionGroup: TransitionDrawableGroup? = null
    private var urlView: TextView? = null
    private var blockView: FrameLayout? = null
    private var securityView: ImageView? = null
    private var menuView: ImageButton? = null
    private var statusBar: View? = null
    private var urlBar: View? = null
    private var popupTint: FrameLayout? = null
    private var menuWeakReference: WeakReference<BrowserMenu>? = WeakReference<BrowserMenu>(null)

    private var toolbarView: DisplayToolbar? = null
    private var findInPageView: FindInPageBar? = null
    private var engineView: EngineView? = null

    private val sessionFeature = ViewBoundFeatureWrapper<SessionFeature>()
    private val findInPageFeature = ViewBoundFeatureWrapper<FindInPageFeature>()
    private val fullScreenFeature = ViewBoundFeatureWrapper<FullScreenFeature>()
    private val contextMenuIntegration = ViewBoundFeatureWrapper<ContextMenuFeature>()

    private val urlBinding = ViewBoundFeatureWrapper<UrlBinding>()
    private val securityInfoBinding = ViewBoundFeatureWrapper<SecurityInfoBinding>()
    private val loadingBinding = ViewBoundFeatureWrapper<LoadingBinding>()
    private val progressBinding = ViewBoundFeatureWrapper<ProgressBinding>()
    private val menuBinding = ViewBoundFeatureWrapper<MenuBinding>()

    /**
     * Container for custom video views shown in fullscreen mode.
     */
    private var videoContainer: ViewGroup? = null

    private var isFullscreen: Boolean = false

    /**
     * Container containing the browser chrome and web content.
     */
    private var browserContainer: View? = null

    private var forwardButton: View? = null
    private var backButton: View? = null
    private var refreshButton: View? = null
    private var stopButton: View? = null

    // TODO: Fullscreen feature
    // private var fullscreenCallback: IWebView.FullscreenCallback? = null

    private var manager: DownloadManager? = null

    private var downloadBroadcastReceiver: DownloadBroadcastReceiver? = null

    private val findInPageCoordinator = FindInPageCoordinator()

    private var biometricController: BiometricAuthenticationHandler? = null

    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // The url property is used for things like sharing the current URL. We could try to use the webview,
    // but sometimes it's null, and sometimes it returns a null URL. Sometimes it returns a data:
    // URL for error pages. The URL we show in the toolbar is (A) always correct and (B) what the
    // user is probably expecting to share, so lets use that here:
    val url: String
        get() = urlView!!.text.toString()

    var openedFromExternalLink: Boolean = false

    lateinit var session: Session
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        val sessionUUID = arguments!!.getString(ARGUMENT_SESSION_UUID) ?: throw IllegalAccessError("No session exists")

        session = requireComponents.sessionManager.findSessionById(sessionUUID) ?: createTab("about:blank")

        Log.w("SKDBG", "Creating fragment for session: $sessionUUID")

        findInPageCoordinator.matches.observe(
            this,
            Observer { _ ->  })
    }

    override fun onPause() {
        super.onPause()

        if (Biometrics.isBiometricsEnabled(requireContext())) {
            biometricController?.stopListening()
            view!!.alpha = 0f
        }

        requireContext().unregisterReceiver(downloadBroadcastReceiver)

        if (isFullscreen) {
            // getWebView()?.exitFullscreen()
        }

        val menu = menuWeakReference!!.get()
        if (menu != null) {
            menu.dismiss()

            menuWeakReference!!.clear()
        }
    }

    override fun onStop() {
        job.cancel()
        super.onStop()
    }

    @Suppress("LongMethod", "ComplexMethod")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (savedInstanceState != null && savedInstanceState.containsKey(RESTORE_KEY_DOWNLOAD)) {
            // If this activity was destroyed before we could start a download (e.g. because we were waiting for a
            // permission) then restore the download object.
            pendingDownload = savedInstanceState.getParcelable(RESTORE_KEY_DOWNLOAD)
        }

        val view = inflater.inflate(R.layout.fragment_browser, container, false)

        videoContainer = view.findViewById<View>(R.id.video_container) as ViewGroup
        browserContainer = view.findViewById(R.id.browser_container)

        urlBar = view.findViewById(R.id.urlbar)
        statusBar = view.findViewById(R.id.status_bar_background)

        popupTint = view.findViewById(R.id.popup_tint)

        urlView = view.findViewById<View>(R.id.display_url) as TextView
        urlView!!.setOnLongClickListener(this)

        toolbarView = view.findViewById<DisplayToolbar>(R.id.appbar)

        LoadTimeObserver.addObservers(session, this)

        refreshButton = view.findViewById(R.id.refresh)
        refreshButton?.let { it.setOnClickListener(this) }

        stopButton = view.findViewById(R.id.stop)
        stopButton?.let { it.setOnClickListener(this) }

        forwardButton = view.findViewById(R.id.forward)
        forwardButton?.let { it.setOnClickListener(this) }

        backButton = view.findViewById(R.id.back)
        backButton?.let { it.setOnClickListener(this) }

        val blockIcon = view.findViewById<View>(R.id.block_image) as ImageView
        blockIcon.setImageResource(R.drawable.ic_tracking_protection_disabled)

        blockView = view.findViewById<View>(R.id.block) as FrameLayout

        securityView = view.findViewById(R.id.security_info)

        securityView!!.setImageResource(R.drawable.ic_internet)

        securityView!!.setOnClickListener(this)

        menuView = view.findViewById<View>(R.id.menuView) as ImageButton
        menuView!!.setOnClickListener(this)

        if (session.isCustomTabSession()) {
            initialiseCustomTabUi(view)
        } else {
            initialiseNormalBrowserUi(view)
        }

        // Pre-calculate the height of the find in page UI so that we can accurately add padding
        // to the WebView when we present it.

        // findInPageView!!.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        // findInPageViewHeight = findInPageView!!.measuredHeight

        // TODO: Update correctly somewhere...
        setBlockingUI(true)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val components = requireComponents

        findInPageView = view.findViewById<FindInPageBar>(R.id.find_in_page)

        engineView = (view.findViewById<View>(R.id.webview) as EngineView)

        val progressView = view.findViewById<View>(R.id.progress) as AnimatedProgressBar

        findInPageFeature.set(FindInPageFeature(
                components.store,
            findInPageView!!,
            engineView as EngineView
        ) {
            findInPageView?.visibility = View.GONE
        }, this, view)

        fullScreenFeature.set(FullScreenFeature(
            components.sessionManager,
            SessionUseCases(components.sessionManager),
            session.id,
            ::viewportFitChanged,
            ::fullScreenChanged
        ), this, view)

        contextMenuIntegration.set(ContextMenuFeature(
            requireFragmentManager(),
            components.store,
            ContextMenuCandidate.defaultCandidates(
                    requireContext(),
                    components.tabsUseCases,
                    components.contextMenuUseCases,
                    view
            ),
            engineView!!,
            requireComponents.contextMenuUseCases
        ), this, view)

        sessionFeature.set(SessionFeature(
            components.sessionManager,
            components.sessionUseCases,
            engineView!!,
            session.id
        ), this, view)

        urlBinding.set(
            UrlBinding(
                components.store,
                session.id,
                urlView!!
            ),
            owner = this,
            view = urlView!!
        )

        menuBinding.set(
            MenuBinding(
                components.store,
                session.id,
                menuGetter = { menuWeakReference?.get() }
            ),
            owner = this,
            view = view
        )

        securityInfoBinding.set(
            SecurityInfoBinding(
                components.store,
                session.id,
                securityView!!
            ),
            owner = this,
            view = securityView!!
        )

        loadingBinding.set(
            LoadingBinding(
                components.store,
                session.id,
                { backgroundTransitionGroup!! },
                progressView,
                blockView!!
            ),
            owner = this,
            view = view
        )

        progressBinding.set(
            ProgressBinding(
                components.store,
                session.id,
                progressView
            ),
            owner = this,
            view = progressView
        )

        manager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadBroadcastReceiver = DownloadBroadcastReceiver(browserContainer, manager)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun viewportFitChanged(viewportFit: Int) {
        requireActivity().window.attributes.layoutInDisplayCutoutMode = viewportFit
    }

    private fun fullScreenChanged(enabled: Boolean) {
        if (enabled) {
            toolbarView?.visibility = View.GONE

            toolbarView!!.setExpanded(false, true)
            statusBar!!.visibility = View.GONE

            engineView?.setDynamicToolbarMaxHeight(0)

            switchToImmersiveMode()
        } else {
            toolbarView?.visibility = View.VISIBLE

            toolbarView!!.setExpanded(true, true)
            statusBar!!.visibility = View.VISIBLE

            engineView?.setDynamicToolbarMaxHeight(toolbarView?.measuredHeight ?: 0)

            exitImmersiveModeIfNeeded()
        }
    }

    private fun initialiseNormalBrowserUi(view: View) {
        val eraseButton = view.findViewById<FloatingEraseButton>(R.id.erase)
        eraseButton.setOnClickListener(this)

        urlView!!.setOnClickListener(this)

        val tabsButton = view.findViewById<FloatingSessionsButton>(R.id.tabs)
        tabsButton.setOnClickListener(this)

        val sessionManager = requireComponents.sessionManager
        sessionManager.register(object : SessionManager.Observer {
            override fun onSessionAdded(session: Session) {
                tabsButton.updateSessionsCount(sessionManager.sessions.size)
                eraseButton.updateSessionsCount(sessionManager.sessions.size)
            }

            override fun onSessionRemoved(session: Session) {
                tabsButton.updateSessionsCount(sessionManager.sessions.size)
                eraseButton.updateSessionsCount(sessionManager.sessions.size)
            }

            override fun onAllSessionsRemoved() {
                tabsButton.updateSessionsCount(sessionManager.sessions.size)
                eraseButton.updateSessionsCount(sessionManager.sessions.size)
            }
        })

        tabsButton.updateSessionsCount(sessionManager.sessions.size)
        eraseButton.updateSessionsCount(sessionManager.sessions.size)
    }

    private fun initialiseCustomTabUi(view: View) {
        val customTabConfig = session.customTabConfig!!

        // Unfortunately there's no simpler way to have the FAB only in normal-browser mode.
        // - ViewStub: requires splitting attributes for the FAB between the ViewStub, and actual FAB layout file.
        //             Moreover, the layout behaviour just doesn't work unless you set it programatically.
        // - View.GONE: doesn't work because the layout-behaviour makes the FAB visible again when scrolling.
        // - Adding at runtime: works, but then we need to use a separate layout file (and you need
        //   to set some attributes programatically, same as ViewStub).
        val erase = view.findViewById<FloatingEraseButton>(R.id.erase)
        val eraseContainer = erase.parent as ViewGroup
        eraseContainer.removeView(erase)

        val sessions = view.findViewById<FloatingSessionsButton>(R.id.tabs)
        eraseContainer.removeView(sessions)

        val textColor: Int

        if (customTabConfig.toolbarColor != null) {
            urlBar!!.setBackgroundColor(customTabConfig.toolbarColor!!)

            textColor = ColorUtils.getReadableTextColor(customTabConfig.toolbarColor!!)
            urlView!!.setTextColor(textColor)
        } else {
            textColor = Color.WHITE
        }

        val closeButton = view.findViewById<View>(R.id.customtab_close) as ImageView

        closeButton.visibility = View.VISIBLE
        closeButton.setOnClickListener(this)

        if (customTabConfig.closeButtonIcon != null) {
            closeButton.setImageBitmap(customTabConfig.closeButtonIcon)
        } else {
            // Always set the icon in case it's been overridden by a previous CT invocation
            val closeIcon = DrawableUtils.loadAndTintDrawable(requireContext(), R.drawable.ic_close, textColor)

            closeButton.setImageDrawable(closeIcon)
        }

        if (!customTabConfig.enableUrlbarHiding) {
            val params = urlBar!!.layoutParams as AppBarLayout.LayoutParams
            params.scrollFlags = 0
        }

        if (customTabConfig.actionButtonConfig != null) {
            val actionButton = view.findViewById<View>(R.id.customtab_actionbutton) as ImageButton
            actionButton.visibility = View.VISIBLE

            actionButton.setImageBitmap(customTabConfig.actionButtonConfig!!.icon)
            actionButton.contentDescription = customTabConfig.actionButtonConfig!!.description

            val pendingIntent = customTabConfig.actionButtonConfig!!.pendingIntent

            actionButton.setOnClickListener {
                try {
                    val intent = Intent()
                    intent.data = Uri.parse(url)

                    pendingIntent.send(context, 0, intent)
                } catch (e: PendingIntent.CanceledException) {
                    // There's really nothing we can do here...
                }

                TelemetryWrapper.customTabActionButtonEvent()
            }
        } else {
            // If the third-party app doesn't provide an action button configuration then we are
            // going to disable a "Share" button in the toolbar instead.

            val shareButton = view.findViewById<ImageButton>(R.id.customtab_actionbutton)
            shareButton.visibility = View.VISIBLE
            shareButton.setImageDrawable(
                DrawableUtils.loadAndTintDrawable(
                    requireContext(),
                    R.drawable.ic_share,
                    textColor
                )
            )
            shareButton.contentDescription = getString(R.string.menu_share)
            shareButton.setOnClickListener { shareCurrentUrl() }
        }

        // We need to tint some icons.. We already tinted the close button above. Let's tint our other icons too.
        securityView!!.setColorFilter(textColor)

        val menuIcon = DrawableUtils.loadAndTintDrawable(requireContext(), R.drawable.ic_menu, textColor)
        menuView!!.setImageDrawable(menuIcon)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (pendingDownload != null) {
            // We were not able to start this download yet (waiting for a permission). Save this download
            // so that we can start it once we get restored and receive the permission.
            outState.putParcelable(RESTORE_KEY_DOWNLOAD, pendingDownload)
        }
    }

    /*
    @Suppress("ComplexMethod")
    override fun createCallback(): IWebView.Callback {
        return SessionCallbackProxy(session, object : IWebView.Callback {
            override fun onPageStarted(url: String) {}

            override fun onPageFinished(isSecure: Boolean) {}

            override fun onSecurityChanged(isSecure: Boolean, host: String, organization: String) {}

            override fun onURLChanged(url: String) {}

            override fun onTitleChanged(title: String) {}

            override fun onRequest(isTriggeredByUserGesture: Boolean) {}

            override fun onProgress(progress: Int) {}

            override fun countBlockedTracker() {}

            override fun resetBlockedTrackers() {}

            override fun onBlockingStateChanged(isBlockingEnabled: Boolean) {}

            override fun onHttpAuthRequest(callback: IWebView.HttpAuthCallback, host: String, realm: String) {
                val builder = HttpAuthenticationDialogBuilder.Builder(activity, host, realm)
                    .setOkListener { _, _, username, password -> callback.proceed(username, password) }
                    .setCancelListener { callback.cancel() }
                    .build()

                builder.createDialog()
                builder.show()
            }

            override fun onRequestDesktopStateChanged(shouldRequestDesktop: Boolean) {}

            override fun onLongPress(hitTarget: IWebView.HitTarget) {
                WebContextMenu.show(requireActivity(), this, hitTarget, session)
            }

            override fun onEnterFullScreen(callback: IWebView.FullscreenCallback, view: View?) {
                fullscreenCallback = callback
                isFullscreen = true

                // View is passed in as null for GeckoView fullscreen
                if (view != null) {
                    // Hide browser UI and web content
                    browserContainer!!.visibility = View.INVISIBLE

                    // Add view to video container and make it visible
                    val params = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    videoContainer!!.addView(view, params)
                    videoContainer!!.visibility = View.VISIBLE

                    // Switch to immersive mode: Hide system bars other UI controls
                    switchToImmersiveMode()
                } else {
                    appbar?.setExpanded(false, true)
                    (getWebView() as? NestedGeckoView)?.isNestedScrollingEnabled = false
                    // Hide status bar when entering fullscreen with GeckoView
                    statusBar!!.visibility = View.GONE
                    // Switch to immersive mode: Hide system bars other UI controls
                    switchToImmersiveMode()
                }
            }

            override fun onExitFullScreen() {
                if (AppConstants.isGeckoBuild) {
                    appbar?.setExpanded(true, true)
                    (getWebView() as? NestedGeckoView)?.isNestedScrollingEnabled = true
                }

                isFullscreen = false

                // Remove custom video views and hide container
                videoContainer!!.removeAllViews()
                videoContainer!!.visibility = View.GONE

                // Show browser UI and web content again
                browserContainer!!.visibility = View.VISIBLE

                // Show status bar again (hidden in GeckoView versions)
                statusBar!!.visibility = View.VISIBLE

                exitImmersiveModeIfNeeded()

                // Notify renderer that we left fullscreen mode.
                if (fullscreenCallback != null) {
                    fullscreenCallback!!.fullScreenExited()
                    fullscreenCallback = null
                }
            }

            override fun onDownloadStart(download: Download) {
                if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                ) {
                    // Long press image displays its own dialog and we handle other download cases here
                    if (!isDownloadFromLongPressImage(download)) {
                        showDownloadPromptDialog(download)
                    } else {
                        // Download dialog has already been shown from long press on image. Proceed with download.
                        queueDownload(download)
                    }
                } else {
                    // We do not have the permission to write to the external storage. Request the permission and start
                    // the  download from onRequestPermissionsResult().
                    val activity = activity ?: return

                    pendingDownload = download

                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_CODE_STORAGE_PERMISSION
                    )
                }
            }
        })
    }
     */

    /**
     * Hide system bars. They can be revealed temporarily with system gestures, such as swiping from
     * the top of the screen. These transient system bars will overlay app’s content, may have some
     * degree of transparency, and will automatically hide after a short timeout.
     */
    private fun switchToImmersiveMode() {
        val activity = activity ?: return

        val window = activity.window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    /**
     * Show the system bars again.
     */
    private fun exitImmersiveModeIfNeeded() {
        val activity = activity ?: return

        if (WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON and activity.window.attributes.flags == 0) {
            // We left immersive mode already.
            return
        }

        val window = activity.window
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    override fun onDestroy() {
        super.onDestroy()

        // This fragment might get destroyed before the user left immersive mode (e.g. by opening another URL from an
        // app). In this case let's leave immersive mode now when the fragment gets destroyed.
        exitImmersiveModeIfNeeded()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != REQUEST_CODE_STORAGE_PERMISSION) {
            return
        }

        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            // We didn't get the storage permission: We are not able to start this download.
            pendingDownload = null
        }

        // The actual download dialog will be shown from onResume(). If this activity/fragment is
        // getting restored then we need to 'resume' first before we can show a dialog (attaching
        // another fragment).
    }

    internal fun showDownloadPromptDialog(download: Download) {
        val fragmentManager = childFragmentManager

        if (fragmentManager.findFragmentByTag(DownloadDialogFragment.FRAGMENT_TAG) != null) {
            // We are already displaying a download dialog fragment (Probably a restored fragment).
            // No need to show another one.
            return
        }

        val downloadDialogFragment = DownloadDialogFragment.newInstance(download)

        try {
            downloadDialogFragment.show(fragmentManager, DownloadDialogFragment.FRAGMENT_TAG)
        } catch (e: IllegalStateException) {
            // It can happen that at this point in time the activity is already in the background
            // and onSaveInstanceState() has already been called. Fragment transactions are not
            // allowed after that anymore. It's probably safe to guess that the user might not
            // be interested in the download at this point. So we could just *not* show the dialog.
            // Unfortunately we can't call commitAllowingStateLoss() because committing the
            // transaction is happening inside the DialogFragment code. Therefore we just swallow
            // the exception here. Gulp!
        }
    }

    private fun showCrashReporter(crash: Crash) {
        val fragmentManager = requireActivity().supportFragmentManager

        if (crashReporterIsVisible()) {
            // We are already displaying the crash reporter
            // No need to show another one.
            return
        }

        val crashReporterFragment = CrashReporterFragment.create()

        crashReporterFragment.onCloseTabPressed = { sendCrashReport ->
            if (sendCrashReport) { CrashReporterWrapper.submitCrash(crash) }
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
        securityView?.setImageResource(R.drawable.ic_firefox)
        menuView?.visibility = View.GONE
        urlView?.text = requireContext().getString(R.string.tab_crash_report_title)
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
        securityView?.setImageResource(R.drawable.ic_internet)
        menuView?.visibility = View.VISIBLE
        urlView?.text = session.let {
            if (it.isSearch) it.searchTerms else it.url
        }
    }

    fun crashReporterIsVisible(): Boolean = requireActivity().supportFragmentManager.let {
        it.findFragmentByTag(CrashReporterFragment.FRAGMENT_TAG)?.isVisible ?: false
    }

    internal fun showAddToHomescreenDialog(url: String, title: String) {
        val fragmentManager = childFragmentManager

        if (fragmentManager.findFragmentByTag(AddToHomescreenDialogFragment.FRAGMENT_TAG) != null) {
            // We are already displaying a homescreen dialog fragment (Probably a restored fragment).
            // No need to show another one.
            return
        }

        val addToHomescreenDialogFragment = AddToHomescreenDialogFragment.newInstance(
            url,
            title,
            session.trackerBlockingEnabled,
            session.desktopMode
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

    override fun onFinishDownloadDialog(download: Download?, shouldDownload: Boolean) {
        if (shouldDownload) {
            queueDownload(download)
        }
    }

    override fun biometricCreateNewSessionWithLink() {
        for (session in requireComponents.sessionManager.sessions) {
            if (session != requireComponents.sessionManager.selectedSession) {
                requireComponents.sessionManager.remove(session)
            }
        }

        // Purposefully not calling onAuthSuccess in case we add to that function in the future
        view!!.alpha = 1f
    }

    override fun biometricCreateNewSession() {
        requireComponents.sessionManager.removeAndCloseAllSessions()
    }

    override fun onAuthSuccess() {
        view!!.alpha = 1f
    }

    override fun onResume() {
        super.onResume()

        if (job.isCancelled) {
            job = Job()
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        requireContext().registerReceiver(downloadBroadcastReceiver, filter)

        if (pendingDownload != null && PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            // There's a pending download (waiting for the storage permission) and now we have the
            // missing permission: Show the dialog to ask whether the user wants to actually proceed
            // with downloading this file.
            showDownloadPromptDialog(pendingDownload!!)
            pendingDownload = null
        }

        StatusBarUtils.getStatusBarHeight(statusBar) { statusBarHeight ->
            statusBar!!.layoutParams.height = statusBarHeight
        }

        if (Biometrics.isBiometricsEnabled(requireContext())) {
            if (biometricController == null) {
                biometricController = BiometricAuthenticationHandler(context!!)
            }

            displayBiometricPromptIfNeeded()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                biometricController?.stopListening()
            }

            biometricController = null
            view!!.alpha = 1f
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun displayBiometricPromptIfNeeded() {
        val fragmentManager = childFragmentManager

        // Check that we need to auth and that the fragment isn't already displayed
        if (biometricController!!.needsAuth || openedFromExternalLink) {
            view!!.alpha = 0f
            biometricController!!.startAuthentication(openedFromExternalLink)
            openedFromExternalLink = false

            // Are we already displaying the biometric fragment?
            if (fragmentManager.findFragmentByTag(BiometricAuthenticationDialogFragment.FRAGMENT_TAG) != null) {
                return
            }

            try {
                biometricController!!.biometricFragment!!.show(
                    fragmentManager,
                    BiometricAuthenticationDialogFragment.FRAGMENT_TAG
                )
            } catch (e: IllegalStateException) {
                // It can happen that at this point in time the activity is already in the background
                // and onSaveInstanceState() has already been called. Fragment transactions are not
                // allowed after that anymore.
            }
        } else {
            view!!.alpha = 1f
        }
    }

    /**
     * Use Android's Download Manager to queue this download.
     */
    private fun queueDownload(download: Download?) {
        if (download == null) {
            return
        }

        val fileName = if (!TextUtils.isEmpty(download.fileName))
            download.fileName
        else
            DownloadUtils.guessFileName(
                download.contentDisposition,
                null,
                download.url,
                download.mimeType
            )

        val request = DownloadManager.Request(Uri.parse(download.url))
            .addRequestHeader("Referer", url)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setMimeType(download.mimeType)

        if (!AppConstants.isGeckoBuild) {
            val cookie = CookieManager.getInstance().getCookie(download.url)
            request.addRequestHeader("Cookie", cookie)
                .addRequestHeader("User-Agent", download.userAgent)
        }

        try {
            request.setDestinationInExternalPublicDir(download.destinationDirectory, fileName)
        } catch (e: IllegalStateException) {
            Log.e(FRAGMENT_TAG, "Cannot create download directory")
            return
        }

        request.allowScanningByMediaScanner()

        @Suppress("TooGenericExceptionCaught")
        try {
            val downloadReference = manager!!.enqueue(request)
            downloadBroadcastReceiver!!.addQueuedDownload(downloadReference)
        } catch (e: RuntimeException) {
            Log.e(FRAGMENT_TAG, "Download failed: $e")
        }
    }

    @Suppress("ComplexMethod")
    fun onBackPressed(): Boolean {
        if (findInPageFeature.onBackPressed()) {
            return true
        } else if (fullScreenFeature.onBackPressed()) {
            return true
        } else if (sessionFeature.get()?.onBackPressed() == true) {
            return true
        } else {
            if (session.source == Session.Source.ACTION_VIEW || session.isCustomTabSession()) {
                TelemetryWrapper.eraseBackToAppEvent()

                // This session has been started from a VIEW intent. Go back to the previous app
                // immediately and erase the current browsing session.
                erase()

                // If there are no other sessions then we remove the whole task because otherwise
                // the old session might still be partially visible in the app switcher.
                if (requireComponents.sessionManager.sessions.isEmpty()) {
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
                event.packageName = getContext()!!.packageName
                event.text.add(getString(R.string.feedback_erase))
            }
        }

        requireComponents.sessionManager.removeAndCloseSession(session)

        // TODO: Do we need to clear more data here?
        requireComponents.engine.clearData(Engine.BrowsingData.all())
    }

    private fun shareCurrentUrl() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, session.url)

        val title = session.title
        if (title.isNotEmpty()) {
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, title)
        }

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_dialog_title)))

        TelemetryWrapper.shareEvent()
    }

    @Suppress("ComplexMethod")
    override fun onClick(view: View) {
        when (view.id) {
            R.id.menuView -> {
                val menu = BrowserMenu(requireActivity(), this, session.customTabConfig)
                menu.show(menuView!!)

                menuWeakReference = WeakReference(menu)
            }

            R.id.display_url -> if (
                    !crashReporterIsVisible() &&
                    requireComponents.sessionManager.findSessionById(session.id) != null) {
                val urlFragment = UrlInputFragment
                    .createWithSession(session, urlView!!)

                requireActivity().supportFragmentManager
                    .beginTransaction()
                    .add(R.id.container, urlFragment, UrlInputFragment.FRAGMENT_TAG)
                    .commit()
            }

            R.id.erase -> {
                TelemetryWrapper.eraseEvent()

                erase()
            }

            R.id.tabs -> {
                requireActivity().supportFragmentManager
                    .beginTransaction()
                    .add(R.id.container, SessionsSheetFragment(), SessionsSheetFragment.FRAGMENT_TAG)
                    .commit()

                TelemetryWrapper.openTabsTrayEvent()
            }

            R.id.back -> {
                requireComponents.sessionUseCases.goBack(session)
            }

            R.id.forward -> {
                requireComponents.sessionUseCases.goForward(session)
            }

            R.id.refresh -> {
                requireComponents.sessionUseCases.reload(session)

                TelemetryWrapper.menuReloadEvent()
            }

            R.id.stop -> {
                requireComponents.sessionUseCases.stopLoading(session)
            }

            R.id.open_in_firefox_focus -> {
                session.customTabConfig = null

                requireComponents.sessionManager.select(session)

                // TODOL Switching from custom tab to browser
                /*
                val webView = getWebView()
                webView?.releaseGeckoSession()
                webView?.saveWebViewState(session)
                 */

                val intent = Intent(context, MainActivity::class.java)
                intent.action = Intent.ACTION_MAIN
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)

                TelemetryWrapper.openFullBrowser()

                val activity = activity
                activity?.finish()
            }

            R.id.share -> {
                shareCurrentUrl()
            }

            R.id.settings -> (activity as LocaleAwareAppCompatActivity).openPreferences()

            R.id.open_default -> {
                val browsers = Browsers(requireContext(), url)

                val defaultBrowser = browsers.defaultBrowser
                    ?: throw IllegalStateException("<Open with \$Default> was shown when no default browser is set")
                    // We only add this menu item when a third party default exists, in
                    // BrowserMenuAdapter.initializeMenu()

                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.setPackage(defaultBrowser.packageName)
                startActivity(intent)

                if (browsers.isFirefoxDefaultBrowser) {
                    TelemetryWrapper.openFirefoxEvent()
                } else {
                    TelemetryWrapper.openDefaultAppEvent()
                }
            }

            R.id.open_select_browser -> {
                val browsers = Browsers(requireContext(), url)

                val apps = browsers.installedBrowsers
                val store = if (browsers.hasFirefoxBrandedBrowserInstalled())
                    null
                else
                    InstallFirefoxActivity.resolveAppStore(requireContext())

                val fragment = OpenWithFragment.newInstance(
                    apps,
                    url,
                    store
                )
                fragment.show(fragmentManager!!, OpenWithFragment.FRAGMENT_TAG)

                TelemetryWrapper.openSelectionEvent()
            }

            R.id.customtab_close -> {
                erase()
                requireActivity().finish()

                TelemetryWrapper.closeCustomTabEvent()
            }

            R.id.help -> {
                val session = createTab(SupportUtils.HELP_URL, source = Session.Source.MENU)
                requireComponents.sessionManager.add(session, selected = true)
            }

            R.id.help_trackers -> {
                val url = SupportUtils.getSumoURLForTopic(context!!, SupportUtils.SumoTopic.TRACKERS)
                val session = createTab(url, source = Session.Source.MENU)

                requireComponents.sessionManager.add(session, selected = true)
            }

            R.id.add_to_homescreen -> {
                showAddToHomescreenDialog(
                    session.url, session.title
                )
            }

            R.id.security_info -> if (!crashReporterIsVisible()) { showSecurityPopUp() }

            R.id.report_site_issue -> {
                val reportUrl = String.format(SupportUtils.REPORT_SITE_ISSUE_URL, url)
                val session = createTab(reportUrl, source = Session.Source.MENU)
                requireComponents.sessionManager.add(session, selected = true)

                TelemetryWrapper.reportSiteIssueEvent()
            }

            R.id.find_in_page -> {
                val sessionState = requireComponents.store.state.findTab(session.id)
                if (sessionState != null) {
                    findInPageView?.visibility = View.VISIBLE
                    findInPageFeature.get()?.bind(sessionState)
                }
                TelemetryWrapper.findInPageMenuEvent()
            }

            else -> throw IllegalArgumentException("Unhandled menu item in BrowserFragment")
        }
    }

    @Suppress("MagicNumber")
    private fun updateToolbarButtonStates(isLoading: Boolean) {
        @Suppress("ComplexCondition")
        if (forwardButton == null || backButton == null || refreshButton == null || stopButton == null) {
            return
        }

        // TODO: Tablets
        isLoading.hashCode()

        /*
        val webView = getWebView() ?: return

        val canGoForward = webView.canGoForward()
        val canGoBack = webView.canGoBack()

        forwardButton!!.isEnabled = canGoForward
        forwardButton!!.alpha = if (canGoForward) 1.0f else 0.5f
        backButton!!.isEnabled = canGoBack
        backButton!!.alpha = if (canGoBack) 1.0f else 0.5f

        refreshButton!!.visibility = if (isLoading) View.GONE else View.VISIBLE
        stopButton!!.visibility = if (isLoading) View.VISIBLE else View.GONE
         */
    }

    fun setBlockingUI(enabled: Boolean) {
        enabled.hashCode()

        /*
        val webView = getWebView()
        webView?.setBlockingEnabled(enabled)
        */

        statusBar!!.setBackgroundResource(if (enabled)
                R.drawable.animated_background
            else
                R.drawable.animated_background_disabled
        )

        backgroundTransitionGroup = if (!session.isCustomTabSession()) {
            // Only update the toolbar background if this is not a custom tab. Custom tabs set their
            // own color and we do not want to override this here.
            urlBar!!.setBackgroundResource(if (enabled)
                    R.drawable.animated_background
                else
                    R.drawable.animated_background_disabled)

            TransitionDrawableGroup(
                urlBar!!.background as TransitionDrawable,
                statusBar!!.background as TransitionDrawable
            )
        } else {
            TransitionDrawableGroup(
                statusBar!!.background as TransitionDrawable
            )
        }
    }

    fun setShouldRequestDesktop(enabled: Boolean) {
        if (enabled) {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(
                    requireContext().getString(R.string.has_requested_desktop),
                    true
                ).apply()
        }

        // getWebView()?.setRequestDesktop(enabled)
        requireComponents.sessionUseCases.requestDesktopSite(enabled, session)
    }

    private fun showSecurityPopUp() {
        // Don't show Security Popup if the page is loading
        if (session.loading) {
            return
        }
        val securityPopup = PopupUtils.createSecurityPopup(requireContext(), session)
        if (securityPopup != null) {
            securityPopup.setOnDismissListener { popupTint!!.visibility = View.GONE }
            securityPopup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            securityPopup.animationStyle = android.R.style.Animation_Dialog
            securityPopup.isTouchable = true
            securityPopup.isFocusable = true
            securityPopup.elevation = resources.getDimension(R.dimen.menu_elevation)
            val offsetY = requireContext().resources.getDimensionPixelOffset(R.dimen.doorhanger_offsetY)
            securityPopup.showAtLocation(urlBar, Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, offsetY)
            popupTint!!.visibility = View.VISIBLE
        }
    }

    override fun onLongClick(view: View): Boolean {
        // Detect long clicks on display_url
        if (view.id == R.id.display_url) {
            val context = activity ?: return false

            if (session.isCustomTabSession()) {
                val clipBoard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val uri = Uri.parse(url)
                clipBoard.primaryClip = ClipData.newRawUri("Uri", uri)
                Toast.makeText(context, getString(R.string.custom_tab_copy_url_action), Toast.LENGTH_SHORT).show()
            }
        }

        return false
    }

    override fun applyLocale() {
        activity?.supportFragmentManager
            ?.beginTransaction()
            ?.replace(
                R.id.container,
                BrowserFragment.createForSession(requireNotNull(session)),
                BrowserFragment.FRAGMENT_TAG
            )
            ?.commit()
    }

    /*
    private fun updateSecurityIcon(session: Session, securityInfo: Session.SecurityInfo = session.securityInfo) {
        if (crashReporterIsVisible()) return
        val securityView = securityView ?: return

        if (!session.loading) {
            if (securityInfo.secure) {
                securityView.setImageResource(R.drawable.ic_lock)
            } else {
                if (URLUtil.isHttpUrl(url)) {
                    // HTTP site
                    securityView.setImageResource(R.drawable.ic_internet)
                } else {
                    // Certificate is bad
                    securityView.setImageResource(R.drawable.ic_warning)
                }
            }
        } else {
            securityView.setImageResource(R.drawable.ic_internet)
        }
    }
     */

    fun handleTabCrash(crash: Crash) {
        showCrashReporter(crash)
    }

    companion object {
        const val FRAGMENT_TAG = "browser"

        private const val REQUEST_CODE_STORAGE_PERMISSION = 101
        private const val ARGUMENT_SESSION_UUID = "sessionUUID"
        private const val RESTORE_KEY_DOWNLOAD = "download"

        @JvmStatic
        fun createForSession(session: Session): BrowserFragment {
            val arguments = Bundle()
            arguments.putString(ARGUMENT_SESSION_UUID, session.id)

            val fragment = BrowserFragment()
            fragment.arguments = arguments

            return fragment
        }
    }
}
