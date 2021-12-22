/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.view.accessibility.AccessibilityManager
import androidx.preference.PreferenceManager
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.feature.sitepermissions.SitePermissionsRules
import org.mozilla.focus.R
import org.mozilla.focus.ext.components
import org.mozilla.focus.fragment.FirstrunFragment
import org.mozilla.focus.searchsuggestions.SearchSuggestionsPreferences
import org.mozilla.focus.settings.permissions.AutoplayOption
import org.mozilla.focus.settings.permissions.getValueByPrefKey

/**
 * A simple wrapper for SharedPreferences that makes reading preference a little bit easier.
 * This class is designed to have a lot of (simple) functions
 */
@Suppress("TooManyFunctions", "LargeClass")
class Settings(
    private val context: Context
) {

    companion object {
        // Default value is block cross site cookies.
        const val DEFAULT_COOKIE_OPTION_INDEX = 3
        const val NO_VALUE = "no value"
    }

    private val accessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager?

    /**
     * Check each active accessibility service to see if it can perform gestures, if any can,
     * then it is *likely* a switch service is enabled.
     */
    private val switchServiceIsEnabled: Boolean
        get() {
            accessibilityManager?.getEnabledAccessibilityServiceList(0)?.let { activeServices ->
                for (service in activeServices) {
                    if (service.capabilities.and(AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES) == 1) {
                        return true
                    }
                }
            }

            return false
        }

    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun createTrackingProtectionPolicy(
        shouldBlockCookiesValue: String = shouldBlockCookiesValue()
    ): EngineSession.TrackingProtectionPolicy {
        val trackingCategories: MutableList<EngineSession.TrackingProtectionPolicy.TrackingCategory> =
            mutableListOf(EngineSession.TrackingProtectionPolicy.TrackingCategory.SCRIPTS_AND_SUB_RESOURCES)

        if (shouldBlockSocialTrackers()) {
            trackingCategories.add(EngineSession.TrackingProtectionPolicy.TrackingCategory.SOCIAL)
        }
        if (shouldBlockAdTrackers()) {
            trackingCategories.add(EngineSession.TrackingProtectionPolicy.TrackingCategory.AD)
        }
        if (shouldBlockAnalyticTrackers()) {
            trackingCategories.add(EngineSession.TrackingProtectionPolicy.TrackingCategory.ANALYTICS)
        }
        if (shouldBlockOtherTrackers()) {
            trackingCategories.add(EngineSession.TrackingProtectionPolicy.TrackingCategory.CONTENT)
        }

        val cookiePolicy = getCookiePolicy(shouldBlockCookiesValue)

        return EngineSession.TrackingProtectionPolicy.select(
            cookiePolicy = cookiePolicy,
            trackingCategories = trackingCategories.toTypedArray(),
            strictSocialTrackingProtection = shouldBlockSocialTrackers()
        )
    }

    private fun getCookiePolicy(shouldBlockCookiesValue: String) =
        when (shouldBlockCookiesValue) {
            context.getString(R.string.yes) ->
                EngineSession.TrackingProtectionPolicy.CookiePolicy.ACCEPT_NONE

            context.getString(R.string.third_party_tracker) ->
                EngineSession.TrackingProtectionPolicy.CookiePolicy.ACCEPT_NON_TRACKERS

            context.getString(R.string.third_party_only) ->
                EngineSession.TrackingProtectionPolicy.CookiePolicy.ACCEPT_ONLY_FIRST_PARTY

            context.getString(R.string.cross_site) ->
                EngineSession.TrackingProtectionPolicy.CookiePolicy.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS

            context.getString(R.string.no) -> {
                EngineSession.TrackingProtectionPolicy.CookiePolicy.ACCEPT_ALL
            }

            NO_VALUE -> {
                // Ending up here means that the cookie preference has not been yet modified.
                // We should set it to the default value.
                setBlockCookiesValue(
                    resources.getStringArray(R.array.cookies_options_entry_values)[DEFAULT_COOKIE_OPTION_INDEX]
                )
                EngineSession.TrackingProtectionPolicy.CookiePolicy.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS
            }

            else -> {
                // Ending up here means that the cookie preference has already been stored in another locale.
                // We will have identify the existing option and set the preference to the corresponding value.
                // See https://github.com/mozilla-mobile/focus-android/issues/5996.

                val cookieOptionIndex =
                    resources.getStringArray(R.array.cookies_options_entries)
                        .asList().indexOf(shouldBlockCookiesValue())

                val correspondingValue =
                    resources.getStringArray(R.array.cookies_options_entry_values).getOrNull(cookieOptionIndex)
                        ?: resources.getStringArray(R.array.cookies_options_entry_values)[DEFAULT_COOKIE_OPTION_INDEX]

                setBlockCookiesValue(correspondingValue)

                // Get the updated cookie policy for the corresponding value
                when (shouldBlockCookiesValue) {
                    context.getString(R.string.yes) ->
                        EngineSession.TrackingProtectionPolicy.CookiePolicy.ACCEPT_NONE

                    context.getString(R.string.third_party_tracker) ->
                        EngineSession.TrackingProtectionPolicy.CookiePolicy.ACCEPT_NON_TRACKERS

                    context.getString(R.string.third_party_only) ->
                        EngineSession.TrackingProtectionPolicy.CookiePolicy.ACCEPT_ONLY_FIRST_PARTY

                    context.getString(R.string.cross_site) ->
                        EngineSession.TrackingProtectionPolicy.CookiePolicy.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS

                    else -> {
                        // Fallback to the default value.
                        EngineSession.TrackingProtectionPolicy.CookiePolicy.ACCEPT_ALL
                    }
                }
            }
        }

    fun setupSafeBrowsing(engine: Engine, shouldUseSafeBrowsing: Boolean = shouldUseSafeBrowsing()) {
        if (shouldUseSafeBrowsing) {
            engine.settings.safeBrowsingPolicy = arrayOf(EngineSession.SafeBrowsingPolicy.RECOMMENDED)
        } else {
            engine.settings.safeBrowsingPolicy = arrayOf(EngineSession.SafeBrowsingPolicy.NONE)
        }
    }

    private val resources: Resources = context.resources
    val hasAddedToHomeScreen: Boolean
        get() = preferences.getBoolean(getPreferenceKey(R.string.has_added_to_home_screen), false)

    @Deprecated("This is no longer used. Read search engines from BrowserStore instead")
    val defaultSearchEngineName: String
        get() = preferences.getString(getPreferenceKey(R.string.pref_key_search_engine), "")!!

    val openLinksInExternalApp: Boolean
        get() = preferences.getBoolean(
            getPreferenceKey(R.string.pref_key_open_links_in_external_app),
            false
        )

    var isExperimentationEnabled: Boolean
        get() = preferences.getBoolean(getPreferenceKey(R.string.pref_key_studies), true)
        set(value) {
            preferences.edit()
                .putBoolean(getPreferenceKey(R.string.pref_key_studies), value)
                .commit()
        }

    fun shouldBlockImages(): Boolean =
        // Not shipping in v1 (#188)
            /* preferences.getBoolean(
                    resources.getString(R.string.pref_key_performance_block_images),
                    false); */
        false

    private var autoplayPrefKey: String? = preferences.getString(
        getPreferenceKey(R.string.pref_key_autoplay),
        context.getString(R.string.pref_key_block_autoplay_audio_only)
    )

    fun updateAutoplayPrefKey(prefKey: String) {
        preferences.edit()
            .putString(getPreferenceKey(R.string.pref_key_autoplay), prefKey)
            .apply()
        context.components.sessionUseCases.reload.invoke(context.components.store.state.selectedTabId)
        currentAutoplayOption = getValueByPrefKey(autoplayPrefKey = prefKey, context = context)
    }

    var currentAutoplayOption = getValueByPrefKey(autoplayPrefKey = autoplayPrefKey, context = context)

    fun shouldEnableRemoteDebugging(): Boolean =
        preferences.getBoolean(
            getPreferenceKey(R.string.pref_key_remote_debugging),
            false
        )

    fun shouldDisplayHomescreenTips() =
        preferences.getBoolean(
            getPreferenceKey(R.string.pref_key_homescreen_tips),
            true
        )

    fun shouldShowSearchSuggestions(): Boolean =
        preferences.getBoolean(
            getPreferenceKey(R.string.pref_key_show_search_suggestions),
            false
        )

    fun shouldBlockWebFonts(): Boolean =
        preferences.getBoolean(
            getPreferenceKey(R.string.pref_key_performance_block_webfonts),
            false
        )

    fun shouldBlockJavaScript(): Boolean =
        preferences.getBoolean(
            getPreferenceKey(R.string.pref_key_performance_block_javascript),
            false
        )

    fun shouldBlockCookiesValue(): String =
        preferences.getString(
            getPreferenceKey(
                R.string
                    .pref_key_performance_enable_cookies
            ),
            NO_VALUE
        )!!

    private fun setBlockCookiesValue(newValue: String) {
        preferences.edit()
            .putString(getPreferenceKey(R.string.pref_key_performance_enable_cookies), newValue)
            .apply()
    }

    fun shouldShowFirstrun(): Boolean =
        !preferences.getBoolean(FirstrunFragment.FIRSTRUN_PREF, false)

    fun shouldUseBiometrics(): Boolean =
        preferences.getBoolean(getPreferenceKey(R.string.pref_key_biometric), false)

    fun shouldUseSecureMode(): Boolean =
        preferences.getBoolean(getPreferenceKey(R.string.pref_key_secure), false)

    fun setDefaultSearchEngineByName(name: String) {
        preferences.edit()
            .putString(getPreferenceKey(R.string.pref_key_search_engine), name)
            .apply()
    }

    fun shouldAutocompleteFromShippedDomainList() =
        preferences.getBoolean(
            getPreferenceKey(R.string.pref_key_autocomplete_preinstalled),
            true
        )

    fun shouldAutocompleteFromCustomDomainList() =
        preferences.getBoolean(
            getPreferenceKey(R.string.pref_key_autocomplete_custom),
            true
        )

    fun shouldBlockAdTrackers() =
        preferences.getBoolean(
            getPreferenceKey(R.string.pref_key_privacy_block_ads),
            true
        )

    fun shouldUseSafeBrowsing() =
        preferences.getBoolean(
            getPreferenceKey(R.string.pref_key_safe_browsing),
            true
        )

    fun shouldBlockAnalyticTrackers() =
        preferences.getBoolean(
            getPreferenceKey(R.string.pref_key_privacy_block_analytics),
            true
        )

    fun shouldBlockSocialTrackers() =
        preferences.getBoolean(
            getPreferenceKey(R.string.pref_key_privacy_block_social),
            true
        )

    fun shouldBlockOtherTrackers() =
        preferences.getBoolean(
            getPreferenceKey(R.string.pref_key_privacy_block_other3),
            false
        )

    /**
     * This is automatically inferred based on the current system status. Not a setting in our app.
     */
    fun isAccessibilityEnabled() =
        accessibilityManager?.isTouchExplorationEnabled ?: false || switchServiceIsEnabled

    fun userHasToggledSearchSuggestions(): Boolean =
        preferences.getBoolean(SearchSuggestionsPreferences.TOGGLED_SUGGESTIONS_PREF, false)

    fun userHasDismissedNoSuggestionsMessage(): Boolean =
        preferences.getBoolean(SearchSuggestionsPreferences.DISMISSED_NO_SUGGESTIONS_PREF, false)

    fun isDefaultBrowser() = preferences.getBoolean(
        getPreferenceKey(R.string.pref_key_default_browser),
        false
    )

    fun hasOpenedInNewTab() = preferences.getBoolean(
        getPreferenceKey(R.string.has_opened_new_tab),
        false
    )

    fun hasRequestedDesktop() = preferences.getBoolean(
        getPreferenceKey(R.string.has_requested_desktop),
        false
    )

    fun getAppLaunchCount() = preferences.getInt(
        getPreferenceKey(R.string.app_launch_count),
        0
    )

    fun getTotalBlockedTrackersCount() = preferences.getInt(
        getPreferenceKey(R.string.pref_key_privacy_total_trackers_blocked_count),
        0
    )

    fun hasSocialBlocked() = preferences.getBoolean(
        getPreferenceKey(R.string.pref_key_privacy_block_social),
        true
    )

    fun hasAdvertisingBlocked() = preferences.getBoolean(
        getPreferenceKey(R.string.pref_key_privacy_block_ads),
        true
    )

    fun hasAnalyticsBlocked() = preferences.getBoolean(
        getPreferenceKey(R.string.pref_key_privacy_block_analytics),
        true
    )

    fun hasContentBlocked() = preferences.getBoolean(
        getPreferenceKey(R.string.pref_key_privacy_block_other3),
        true
    )

    var lightThemeSelected = preferences.getBoolean(
        getPreferenceKey(R.string.pref_key_light_theme),
        false
    )

    var darkThemeSelected = preferences.getBoolean(
        getPreferenceKey(R.string.pref_key_dark_theme),
        false
    )

    var useDefaultThemeSelected = preferences.getBoolean(
        getPreferenceKey(R.string.pref_key_default_theme),
        false
    )

    fun getHttpsOnlyMode(): Engine.HttpsOnlyMode {
        return if (
            Features.HTTPS_ONLY_MODE &&
            preferences.getBoolean(getPreferenceKey(R.string.pref_key_https_only), true)
        ) {
            Engine.HttpsOnlyMode.ENABLED
        } else {
            Engine.HttpsOnlyMode.DISABLED
        }
    }

    fun getSitePermissionsSettingsRules() = SitePermissionsRules(
        notification = SitePermissionsRules.Action.BLOCKED,
        microphone = SitePermissionsRules.Action.BLOCKED,
        location = SitePermissionsRules.Action.BLOCKED,
        camera = SitePermissionsRules.Action.BLOCKED,
        autoplayAudible = getAutoplayRules().first,
        autoplayInaudible = getAutoplayRules().second,
        persistentStorage = SitePermissionsRules.Action.BLOCKED,
        mediaKeySystemAccess = SitePermissionsRules.Action.BLOCKED,
        crossOriginStorageAccess = SitePermissionsRules.Action.BLOCKED
    )

    private fun getAutoplayRules(): Pair<SitePermissionsRules.AutoplayAction, SitePermissionsRules.AutoplayAction> {
        return when (currentAutoplayOption) {
            is AutoplayOption.AllowAudioVideo -> Pair(
                SitePermissionsRules.AutoplayAction.ALLOWED,
                SitePermissionsRules.AutoplayAction.ALLOWED
            )

            is AutoplayOption.BlockAudioVideo -> Pair(
                SitePermissionsRules.AutoplayAction.BLOCKED,
                SitePermissionsRules.AutoplayAction.BLOCKED
            )

            else -> Pair(
                SitePermissionsRules.AutoplayAction.BLOCKED,
                SitePermissionsRules.AutoplayAction.ALLOWED
            )
        }
    }

    private fun getPreferenceKey(resourceId: Int): String =
        context.getString(resourceId)
}
