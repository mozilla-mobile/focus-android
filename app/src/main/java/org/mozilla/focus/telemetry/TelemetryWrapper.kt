/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.telemetry

import android.content.Context
import android.net.http.SslError
import android.os.StrictMode
import android.preference.PreferenceManager
import android.support.annotation.CheckResult
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.R
import org.mozilla.focus.search.SearchEngineManager
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.utils.AppConstants
import mozilla.components.ui.autocomplete.InlineAutocompleteEditText.AutocompleteResult
import org.mozilla.telemetry.Telemetry
import org.mozilla.telemetry.TelemetryHolder
import org.mozilla.telemetry.config.TelemetryConfiguration
import org.mozilla.telemetry.event.TelemetryEvent
import org.mozilla.telemetry.measurement.DefaultSearchMeasurement
import org.mozilla.telemetry.measurement.SearchesMeasurement
import org.mozilla.telemetry.net.HttpURLConnectionTelemetryClient
import org.mozilla.telemetry.ping.TelemetryCorePingBuilder
import org.mozilla.telemetry.ping.TelemetryEventPingBuilder
import org.mozilla.telemetry.schedule.jobscheduler.JobSchedulerTelemetryScheduler
import org.mozilla.telemetry.serialize.JSONPingSerializer
import org.mozilla.telemetry.storage.FileTelemetryStorage

@Suppress(
        // Yes, this a large class with a lot of functions. But it's very simple and still easy to read.
        "TooManyFunctions",
        "LargeClass"
)
object TelemetryWrapper {
    private const val TELEMETRY_APP_NAME_FOCUS = "Focus"
    private const val TELEMETRY_APP_NAME_KLAR = "Klar"
    private const val TELEMETRY_APP_ENGINE_GECKOVIEW = "GeckoView"

    private const val MAXIMUM_CUSTOM_TAB_EXTRAS = 10

    private val isEnabledByDefault: Boolean
        get() = !AppConstants.isKlarBuild()

    private object Category {
        val ACTION = "action"
        val ERROR = "error"
    }

    private object Method {
        val TYPE_URL = "type_url"
        val TYPE_QUERY = "type_query"
        val TYPE_SELECT_QUERY = "select_query"
        val CLICK = "click"
        val SWIPE = "swipe"
        val CANCEL = "cancel"
        val LONG_PRESS = "long_press"
        val CHANGE = "change"
        val FOREGROUND = "foreground"
        val BACKGROUND = "background"
        val SHARE = "share"
        val SAVE = "save"
        val COPY = "copy"
        val OPEN = "open"
        val INSTALL = "install"
        val INTENT_URL = "intent_url"
        val INTENT_CUSTOM_TAB = "intent_custom_tab"
        val TEXT_SELECTION_INTENT = "text_selection_intent"
        val SHOW = "show"
        val HIDE = "hide"
        val SHARE_INTENT = "share_intent"
        val REMOVE = "remove"
        val REORDER = "reorder"
        val RESTORE = "restore"
        val PAGE = "page"
        val RESOURCE = "resource"
    }

    private object Object {
        val SEARCH_BAR = "search_bar"
        val ERASE_BUTTON = "erase_button"
        val SETTING = "setting"
        val APP = "app"
        val MENU = "menu"
        val BACK_BUTTON = "back_button"
        val NOTIFICATION = "notification"
        val NOTIFICATION_ACTION = "notification_action"
        val SHORTCUT = "shortcut"
        val BLOCKING_SWITCH = "blocking_switch"
        val BROWSER = "browser"
        val BROWSER_CONTEXTMENU = "browser_contextmenu"
        val CUSTOM_TAB_CLOSE_BUTTON = "custom_tab_close_but"
        val CUSTOM_TAB_ACTION_BUTTON = "custom_tab_action_bu"
        val FIRSTRUN = "firstrun"
        val DOWNLOAD_DIALOG = "download_dialog"
        val ADD_TO_HOMESCREEN_DIALOG = "add_to_homescreen_dialog"
        val HOMESCREEN_SHORTCUT = "homescreen_shortcut"
        val TABS_TRAY = "tabs_tray"
        val RECENT_APPS = "recent_apps"
        val APP_ICON = "app_icon"
        val AUTOCOMPLETE_DOMAIN = "autocomplete_domain"
        val AUTOFILL = "autofill"
        val SEARCH_ENGINE_SETTING = "search_engine_setting"
        val ADD_SEARCH_ENGINE_LEARN_MORE = "search_engine_learn_more"
        val CUSTOM_SEARCH_ENGINE = "custom_search_engine"
        val REMOVE_SEARCH_ENGINES = "remove_search_engines"
    }

    private object Value {
        val DEFAULT = "default"
        val FIREFOX = "firefox"
        val SELECTION = "selection"
        val ERASE = "erase"
        val ERASE_AND_OPEN = "erase_open"
        val ERASE_TO_HOME = "erase_home"
        val ERASE_TO_APP = "erase_app"
        val IMAGE = "image"
        val LINK = "link"
        val CUSTOM_TAB = "custom_tab"
        val SKIP = "skip"
        val FINISH = "finish"
        val OPEN = "open"
        val DOWNLOAD = "download"
        val URL = "url"
        val SEARCH = "search"
        val CANCEL = "cancel"
        val ADD_TO_HOMESCREEN = "add_to_homescreen"
        val TAB = "tab"
        val WHATS_NEW = "whats_new"
        val RESUME = "resume"
        val RELOAD = "refresh"
        val FULL_BROWSER = "full_browser"
    }

    private object Extra {
        val FROM = "from"
        val TO = "to"
        val TOTAL = "total"
        val SELECTED = "selected"
        val HIGHLIGHTED = "highlighted"
        val AUTOCOMPLETE = "autocomplete"
        val SOURCE = "source"
        val SUCCESS = "success"
        val ERROR_CODE = "error_code"
        val AVERAGE = "average"
    }

    @JvmStatic
    fun isTelemetryEnabled(context: Context): Boolean {
        // The first access to shared preferences will require a disk read.
        val threadPolicy = StrictMode.allowThreadDiskReads()
        try {
            val resources = context.resources
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)

            return preferences.getBoolean(
                    resources.getString(R.string.pref_key_telemetry), isEnabledByDefault) && !AppConstants.isDevBuild()
        } finally {
            StrictMode.setThreadPolicy(threadPolicy)
        }
    }

    @JvmStatic
    fun setTelemetryEnabled(context: Context, enabled: Boolean) {
        val resources = context.resources
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)

        preferences.edit()
                .putBoolean(resources.getString(R.string.pref_key_telemetry), enabled)
                .apply()

        TelemetryHolder.get()
                .configuration
                .setUploadEnabled(enabled).isCollectionEnabled = enabled
    }

    @JvmStatic
    fun init(context: Context) {
        // When initializing the telemetry library it will make sure that all directories exist and
        // are readable/writable.
        val threadPolicy = StrictMode.allowThreadDiskWrites()
        try {
            val resources = context.resources

            val telemetryEnabled = isTelemetryEnabled(context)

            val configuration = TelemetryConfiguration(context)
                    .setServerEndpoint("https://incoming.telemetry.mozilla.org")
                    .setAppName(if (AppConstants.isKlarBuild()) TELEMETRY_APP_NAME_KLAR else TELEMETRY_APP_NAME_FOCUS)
                    .setUpdateChannel(BuildConfig.BUILD_TYPE)
                    .setPreferencesImportantForTelemetry(
                            resources.getString(R.string.pref_key_search_engine),
                            resources.getString(R.string.pref_key_privacy_block_ads),
                            resources.getString(R.string.pref_key_privacy_block_analytics),
                            resources.getString(R.string.pref_key_privacy_block_social),
                            resources.getString(R.string.pref_key_privacy_block_other),
                            resources.getString(R.string.pref_key_performance_block_javascript),
                            resources.getString(R.string.pref_key_performance_block_webfonts),
                            resources.getString(R.string.pref_key_locale),
                            resources.getString(R.string.pref_key_secure),
                            resources.getString(R.string.pref_key_default_browser),
                            resources.getString(R.string.pref_key_autocomplete_preinstalled),
                            resources.getString(R.string.pref_key_autocomplete_custom))
                    .setSettingsProvider(TelemetrySettingsProvider(context))
                    .setCollectionEnabled(telemetryEnabled)
                    .setUploadEnabled(telemetryEnabled)
                    .setBuildId(TelemetryConfiguration(context).buildId +
                    (if (AppConstants.isGeckoBuild())
                        ("-" + TELEMETRY_APP_ENGINE_GECKOVIEW) else ""))

            val serializer = JSONPingSerializer()
            val storage = FileTelemetryStorage(configuration, serializer)
            val client = HttpURLConnectionTelemetryClient()
            val scheduler = JobSchedulerTelemetryScheduler()

            TelemetryHolder.set(Telemetry(configuration, storage, client, scheduler)
                    .addPingBuilder(TelemetryCorePingBuilder(configuration))
                    .addPingBuilder(TelemetryEventPingBuilder(configuration))
                    .setDefaultSearchProvider(createDefaultSearchProvider(context)))
        } finally {
            StrictMode.setThreadPolicy(threadPolicy)
        }
    }

    private fun createDefaultSearchProvider(context: Context): DefaultSearchMeasurement.DefaultSearchEngineProvider {
        return DefaultSearchMeasurement.DefaultSearchEngineProvider {
            SearchEngineManager.getInstance()
                    .getDefaultSearchEngine(context)
                    .identifier
        }
    }

    /**
     * Add the position of the current session and total number of sessions as extras to the event
     * and return it.
     */
    @CheckResult
    private fun withSessionCounts(event: TelemetryEvent): TelemetryEvent {
        val sessionManager = SessionManager.getInstance()

        event.extra(Extra.SELECTED, sessionManager.positionOfCurrentSession.toString())
        event.extra(Extra.TOTAL, sessionManager.numberOfSessions.toString())

        return event
    }

    @JvmStatic
    fun startSession() {
        TelemetryHolder.get().recordSessionStart()

        TelemetryEvent.create(Category.ACTION, Method.FOREGROUND, Object.APP).queue()
    }

    private var numLoads: Int = 0
    private var averageTime: Double = 0.0

    @JvmStatic
    fun addLoadToAverage(newLoadTime: Long) {
        numLoads++
        averageTime += (newLoadTime - averageTime) / numLoads
    }

    @JvmStatic
    private fun resetAverageLoad() {
        numLoads = 0
        averageTime = 0.0
    }

    @JvmStatic
    fun stopSession() {
        TelemetryHolder.get().recordSessionEnd()

        if (numLoads > 0) {
            TelemetryEvent.create(Category.ACTION, Method.FOREGROUND, Object.BROWSER)
                    .extra(Extra.AVERAGE, averageTime.toString()).queue()
            resetAverageLoad()
        }

        TelemetryEvent.create(Category.ACTION, Method.BACKGROUND, Object.APP).queue()
    }

    @JvmStatic
    fun stopMainActivity() {
        TelemetryHolder.get()
                .queuePing(TelemetryCorePingBuilder.TYPE)
                .queuePing(TelemetryEventPingBuilder.TYPE)
                .scheduleUpload()
    }

    @JvmStatic
    fun urlBarEvent(isUrl: Boolean, autocompleteResult: AutocompleteResult) {
        if (isUrl) {
            TelemetryWrapper.browseEvent(autocompleteResult)
        } else {
            TelemetryWrapper.searchEnterEvent()
        }
    }

    private fun browseEvent(autocompleteResult: AutocompleteResult) {
        val event = TelemetryEvent.create(Category.ACTION, Method.TYPE_URL, Object.SEARCH_BAR)
                .extra(Extra.AUTOCOMPLETE, (!autocompleteResult.isEmpty).toString())

        if (!autocompleteResult.isEmpty) {
            event.extra(Extra.TOTAL, autocompleteResult.totalItems.toString())
            event.extra(Extra.SOURCE, autocompleteResult.source)
        }

        event.queue()
    }

    @JvmStatic
    fun browseIntentEvent() {
        TelemetryEvent.create(Category.ACTION, Method.INTENT_URL, Object.APP).queue()
    }

    @JvmStatic
    fun shareIntentEvent(isSearch: Boolean) {
        if (isSearch) {
            TelemetryEvent.create(Category.ACTION, Method.SHARE_INTENT, Object.APP, Value.SEARCH).queue()
        } else {
            TelemetryEvent.create(Category.ACTION, Method.SHARE_INTENT, Object.APP, Value.URL).queue()
        }
    }

    /**
     * Sends a list of the custom tab options that a custom-tab intent made use of.
     */
    @JvmStatic
    fun customTabsIntentEvent(options: List<String>) {
        val event = TelemetryEvent.create(Category.ACTION, Method.INTENT_CUSTOM_TAB, Object.APP)

        // We can send at most 10 extras per event - we just ignore the rest if there are too many
        val extrasCount: Int = if (options.size > MAXIMUM_CUSTOM_TAB_EXTRAS) {
            MAXIMUM_CUSTOM_TAB_EXTRAS
        } else {
            options.size
        }

        for (option in options.subList(0, extrasCount)) {
            event.extra(option, "true")
        }

        event.queue()
    }

    @JvmStatic
    fun downloadDialogDownloadEvent(sentToDownload: Boolean) {
        if (sentToDownload) {
            TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.DOWNLOAD_DIALOG, Value.DOWNLOAD).queue()
        } else {
            TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.DOWNLOAD_DIALOG, Value.CANCEL).queue()
        }
    }

    @JvmStatic
    fun closeCustomTabEvent() {
        withSessionCounts(TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.CUSTOM_TAB_CLOSE_BUTTON))
                .queue()
    }

    @JvmStatic
    fun customTabActionButtonEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.CUSTOM_TAB_ACTION_BUTTON).queue()
    }

    @JvmStatic
    fun customTabMenuEvent() {
        TelemetryEvent.create(Category.ACTION, Method.OPEN, Object.MENU, Value.CUSTOM_TAB).queue()
    }

    @JvmStatic
    fun textSelectionIntentEvent() {
        TelemetryEvent.create(Category.ACTION, Method.TEXT_SELECTION_INTENT, Object.APP).queue()
    }

    private fun searchEnterEvent() {
        val telemetry = TelemetryHolder.get()

        TelemetryEvent.create(Category.ACTION, Method.TYPE_QUERY, Object.SEARCH_BAR).queue()

        val searchEngine = SearchEngineManager.getInstance().getDefaultSearchEngine(
                telemetry.configuration.context)

        telemetry.recordSearch(SearchesMeasurement.LOCATION_ACTIONBAR, searchEngine.identifier)
    }

    @JvmStatic
    fun searchSelectEvent() {
        val telemetry = TelemetryHolder.get()

        TelemetryEvent.create(Category.ACTION, Method.TYPE_SELECT_QUERY, Object.SEARCH_BAR).queue()

        val searchEngine = SearchEngineManager.getInstance().getDefaultSearchEngine(
                telemetry.configuration.context)

        telemetry.recordSearch(SearchesMeasurement.LOCATION_SUGGESTION, searchEngine.identifier)
    }

    @JvmStatic
    fun eraseEvent() {
        withSessionCounts(TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.ERASE_BUTTON))
                .queue()
    }

    @JvmStatic
    fun eraseBackToHomeEvent() {
        withSessionCounts(TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.BACK_BUTTON, Value.ERASE_TO_HOME))
                .queue()
    }

    @JvmStatic
    fun eraseBackToAppEvent() {
        withSessionCounts(TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.BACK_BUTTON, Value.ERASE_TO_APP))
                .queue()
    }

    @JvmStatic
    fun eraseNotificationEvent() {
        withSessionCounts(TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.NOTIFICATION, Value.ERASE))
                .queue()
    }

    @JvmStatic
    fun eraseAndOpenNotificationActionEvent() {
        withSessionCounts(TelemetryEvent.create(
                Category.ACTION,
                Method.CLICK,
                Object.NOTIFICATION_ACTION,
                Value.ERASE_AND_OPEN)
        ).queue()
    }

    @JvmStatic
    fun openNotificationActionEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.NOTIFICATION_ACTION, Value.OPEN).queue()
    }

    @JvmStatic
    fun openHomescreenShortcutEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.HOMESCREEN_SHORTCUT, Value.OPEN).queue()
    }

    @JvmStatic
    fun addToHomescreenShortcutEvent() {
        TelemetryEvent.create(
                Category.ACTION,
                Method.CLICK,
                Object.ADD_TO_HOMESCREEN_DIALOG,
                Value.ADD_TO_HOMESCREEN
        ).queue()
    }

    @JvmStatic
    fun cancelAddToHomescreenShortcutEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.ADD_TO_HOMESCREEN_DIALOG, Value.CANCEL).queue()
    }

    @JvmStatic
    fun eraseShortcutEvent() {
        withSessionCounts(TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.SHORTCUT, Value.ERASE))
                .queue()
    }

    @JvmStatic
    fun eraseTaskRemoved() {
        withSessionCounts(TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.RECENT_APPS, Value.ERASE))
                .queue()
    }

    @JvmStatic
    fun settingsEvent(key: String, value: String) {
        TelemetryEvent.create(Category.ACTION, Method.CHANGE, Object.SETTING, key)
                .extra(Extra.TO, value)
                .queue()
    }

    @JvmStatic
    fun shareEvent() {
        TelemetryEvent.create(Category.ACTION, Method.SHARE, Object.MENU).queue()
    }

    @JvmStatic
    fun shareLinkEvent() {
        TelemetryEvent.create(Category.ACTION, Method.SHARE, Object.BROWSER_CONTEXTMENU, Value.LINK).queue()
    }

    @JvmStatic
    fun shareImageEvent() {
        TelemetryEvent.create(Category.ACTION, Method.SHARE, Object.BROWSER_CONTEXTMENU, Value.IMAGE).queue()
    }

    @JvmStatic
    fun saveImageEvent() {
        TelemetryEvent.create(Category.ACTION, Method.SAVE, Object.BROWSER_CONTEXTMENU, Value.IMAGE).queue()
    }

    @JvmStatic
    fun copyLinkEvent() {
        TelemetryEvent.create(Category.ACTION, Method.COPY, Object.BROWSER_CONTEXTMENU, Value.LINK).queue()
    }

    @JvmStatic
    fun copyImageEvent() {
        TelemetryEvent.create(Category.ACTION, Method.COPY, Object.BROWSER_CONTEXTMENU, Value.IMAGE).queue()
    }

    @JvmStatic
    fun openLinkInNewTabEvent() {
        withSessionCounts(TelemetryEvent.create(Category.ACTION, Method.OPEN, Object.BROWSER_CONTEXTMENU, Value.TAB))
                .queue()
    }

    @JvmStatic
    fun openWebContextMenuEvent() {
        TelemetryEvent.create(Category.ACTION, Method.LONG_PRESS, Object.BROWSER).queue()
    }

    @JvmStatic
    fun cancelWebContextMenuEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CANCEL, Object.BROWSER_CONTEXTMENU).queue()
    }

    @JvmStatic
    fun openDefaultAppEvent() {
        TelemetryEvent.create(Category.ACTION, Method.OPEN, Object.MENU, Value.DEFAULT).queue()
    }

    /**
     * Switching from a custom tab to the full-featured browser (regular tab).
     */
    @JvmStatic
    fun openFullBrowser() {
        TelemetryEvent.create(Category.ACTION, Method.OPEN, Object.MENU, Value.FULL_BROWSER).queue()
    }

    @JvmStatic
    fun openFromIconEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.APP_ICON, Value.OPEN).queue()
    }

    @JvmStatic
    fun resumeFromIconEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.APP_ICON, Value.RESUME).queue()
    }

    @JvmStatic
    fun openFirefoxEvent() {
        TelemetryEvent.create(Category.ACTION, Method.OPEN, Object.MENU, Value.FIREFOX).queue()
    }

    @JvmStatic
    fun installFirefoxEvent() {
        TelemetryEvent.create(Category.ACTION, Method.INSTALL, Object.APP, Value.FIREFOX).queue()
    }

    @JvmStatic
    fun openSelectionEvent() {
        TelemetryEvent.create(Category.ACTION, Method.OPEN, Object.MENU, Value.SELECTION).queue()
    }

    @JvmStatic
    fun blockingSwitchEvent(isBlockingEnabled: Boolean) {
        TelemetryEvent.create(
                Category.ACTION,
                Method.CLICK,
                Object.BLOCKING_SWITCH,
                isBlockingEnabled.toString()
        ).queue()
    }

    @JvmStatic
    fun showFirstRunPageEvent(page: Int) {
        TelemetryEvent.create(Category.ACTION, Method.SHOW, Object.FIRSTRUN, page.toString()).queue()
    }

    @JvmStatic
    fun skipFirstRunEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.FIRSTRUN, Value.SKIP).queue()
    }

    @JvmStatic
    fun finishFirstRunEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.FIRSTRUN, Value.FINISH).queue()
    }

    @JvmStatic
    fun openTabsTrayEvent() {
        TelemetryEvent.create(Category.ACTION, Method.SHOW, Object.TABS_TRAY).queue()
    }

    @JvmStatic
    fun openWhatsNewEvent(highlighted: Boolean) {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.MENU, Value.WHATS_NEW)
                .extra(Extra.HIGHLIGHTED, highlighted.toString())
                .queue()
    }

    @JvmStatic
    fun closeTabsTrayEvent() {
        TelemetryEvent.create(Category.ACTION, Method.HIDE, Object.TABS_TRAY).queue()
    }

    @JvmStatic
    fun switchTabInTabsTrayEvent() {
        withSessionCounts(TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.TABS_TRAY, Value.TAB))
                .queue()
    }

    @JvmStatic
    fun eraseInTabsTrayEvent() {
        withSessionCounts(TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.TABS_TRAY, Value.ERASE))
                .queue()
    }

    @JvmStatic
    fun swipeReloadEvent() {
        TelemetryEvent.create(Category.ACTION, Method.SWIPE, Object.BROWSER, Value.RELOAD).queue()
    }

    @JvmStatic
    fun menuReloadEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.MENU, Value.RELOAD).queue()
    }

    @JvmStatic
    fun sslErrorEvent(fromPage: Boolean, error: SslError) {
        // SSL Errors from https://developer.android.com/reference/android/net/http/SslError.html
        val primaryErrorMessage = when (error.primaryError) {
            SslError.SSL_DATE_INVALID -> "SSL_DATE_INVALID"
            SslError.SSL_EXPIRED -> "SSL_EXPIRED"
            SslError.SSL_IDMISMATCH -> "SSL_IDMISMATCH"
            SslError.SSL_NOTYETVALID -> "SSL_NOTYETVALID"
            SslError.SSL_UNTRUSTED -> "SSL_UNTRUSTED"
            SslError.SSL_INVALID -> "SSL_INVALID"
            else -> "Undefined SSL Error"
        }
        TelemetryEvent.create(Category.ERROR, if (fromPage) Method.PAGE else Method.RESOURCE, Object.BROWSER)
                .extra(Extra.ERROR_CODE, primaryErrorMessage)
                .queue()
    }

    fun saveAutocompleteDomainEvent() {
        TelemetryEvent.create(Category.ACTION, Method.SAVE, Object.AUTOCOMPLETE_DOMAIN).queue()
    }

    fun removeAutocompleteDomainsEvent(count: Int) {
        TelemetryEvent.create(Category.ACTION, Method.REMOVE, Object.AUTOCOMPLETE_DOMAIN)
                .extra(Extra.TOTAL, count.toString())
                .queue()
    }

    fun reorderAutocompleteDomainEvent(from: Int, to: Int) {
        TelemetryEvent.create(Category.ACTION, Method.REORDER, Object.AUTOCOMPLETE_DOMAIN)
                .extra(Extra.FROM, from.toString())
                .extra(Extra.TO, to.toString())
                .queue()
    }

    fun autofillShownEvent() {
        TelemetryEvent.create(Category.ACTION, Method.SHOW, Object.AUTOFILL).queue()
    }

    @JvmStatic
    fun autofillPerformedEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.AUTOFILL).queue()
    }

    @JvmStatic
    fun setDefaultSearchEngineEvent(source: String) {
        TelemetryEvent.create(Category.ACTION, Method.SAVE, Object.SEARCH_ENGINE_SETTING)
                .extra(Extra.SOURCE, source)
                .queue()
    }

    @JvmStatic
    fun openSearchSettingsEvent() {
        TelemetryEvent.create(Category.ACTION, Method.OPEN, Object.SEARCH_ENGINE_SETTING).queue()
    }

    @JvmStatic
    fun menuRemoveEnginesEvent() {
        TelemetryEvent.create(Category.ACTION, Method.REMOVE, Object.SEARCH_ENGINE_SETTING).queue()
    }

    @JvmStatic
    fun menuRestoreEnginesEvent() {
        TelemetryEvent.create(Category.ACTION, Method.RESTORE, Object.SEARCH_ENGINE_SETTING).queue()
    }

    @JvmStatic
    fun menuAddSearchEngineEvent() {
        TelemetryEvent.create(Category.ACTION, Method.SHOW, Object.CUSTOM_SEARCH_ENGINE).queue()
    }

    @JvmStatic
    fun saveCustomSearchEngineEvent(success: Boolean) {
        TelemetryEvent.create(Category.ACTION, Method.SAVE, Object.CUSTOM_SEARCH_ENGINE)
                .extra(Extra.SUCCESS, success.toString())
                .queue()
    }

    @JvmStatic
    fun removeSearchEnginesEvent(selected: Int) {
        TelemetryEvent.create(Category.ACTION, Method.REMOVE, Object.REMOVE_SEARCH_ENGINES)
                .extra(Extra.SELECTED, selected.toString())
                .queue()
    }

    @JvmStatic
    fun addSearchEngineLearnMoreEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.ADD_SEARCH_ENGINE_LEARN_MORE).queue()
    }
}
