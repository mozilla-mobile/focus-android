/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.navigation

import android.os.Build
import android.os.Bundle
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.autocomplete.AutocompleteAddFragment
import org.mozilla.focus.autocomplete.AutocompleteListFragment
import org.mozilla.focus.autocomplete.AutocompleteRemoveFragment
import org.mozilla.focus.autocomplete.AutocompleteSettingsFragment
import org.mozilla.focus.biometrics.BiometricAuthenticationFragment
import org.mozilla.focus.exceptions.ExceptionsListFragment
import org.mozilla.focus.exceptions.ExceptionsRemoveFragment
import org.mozilla.focus.ext.components
import org.mozilla.focus.ext.settings
import org.mozilla.focus.fragment.BrowserFragment
import org.mozilla.focus.fragment.FirstrunFragment
import org.mozilla.focus.fragment.UrlInputFragment
import org.mozilla.focus.fragment.about.AboutFragment
import org.mozilla.focus.fragment.onboarding.OnboardingFirstFragment
import org.mozilla.focus.fragment.onboarding.OnboardingSecondFragment
import org.mozilla.focus.fragment.onboarding.OnboardingStep
import org.mozilla.focus.fragment.onboarding.OnboardingStorage
import org.mozilla.focus.locale.screen.LanguageFragment
import org.mozilla.focus.nimbus.FocusNimbus
import org.mozilla.focus.nimbus.Onboarding
import org.mozilla.focus.searchwidget.SearchWidgetUtils
import org.mozilla.focus.settings.GeneralSettingsFragment
import org.mozilla.focus.settings.InstalledSearchEnginesSettingsFragment
import org.mozilla.focus.settings.ManualAddSearchEngineSettingsFragment
import org.mozilla.focus.settings.MozillaSettingsFragment
import org.mozilla.focus.settings.RemoveSearchEnginesSettingsFragment
import org.mozilla.focus.settings.SearchSettingsFragment
import org.mozilla.focus.settings.SettingsFragment
import org.mozilla.focus.settings.advanced.AdvancedSettingsFragment
import org.mozilla.focus.settings.advanced.SecretSettingsFragment
import org.mozilla.focus.settings.permissions.SitePermissionsFragment
import org.mozilla.focus.settings.permissions.permissionoptions.SitePermission
import org.mozilla.focus.settings.permissions.permissionoptions.SitePermissionOptionsFragment
import org.mozilla.focus.settings.privacy.PrivacySecuritySettingsFragment
import org.mozilla.focus.settings.privacy.studies.StudiesFragment
import org.mozilla.focus.state.AppAction
import org.mozilla.focus.state.Screen
import org.mozilla.focus.utils.ViewUtils
import kotlin.collections.forEach as withEach

/**
 * Class performing the actual navigation in [MainActivity] by performing fragment transactions if
 * needed.
 */
@Suppress("TooManyFunctions")
class MainActivityNavigation(
    private val activity: MainActivity,
) {
    /**
     * Home screen.
     */
    fun home() {
        val fragmentManager = activity.supportFragmentManager
        val browserFragment = fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG) as BrowserFragment?

        val isShowingBrowser = browserFragment != null
        val crashReporterIsVisible = browserFragment?.crashReporterIsVisible() ?: false

        if (isShowingBrowser && !crashReporterIsVisible) {
            showPromoteSearchWidgetDialogOrBrandedSnackbar()
        }

        // We add the url input fragment to the layout if it doesn't exist yet.
        val transaction = fragmentManager
            .beginTransaction()

        // We only want to play the animation if a browser fragment is added and resumed.
        // If it is not resumed then the application is currently in the process of resuming
        // and the session was removed while the app was in the background (e.g. via the
        // notification). In this case we do not want to show the content and remove the
        // browser fragment immediately.
        val shouldAnimate = isShowingBrowser && browserFragment!!.isResumed

        if (shouldAnimate) {
            transaction.setCustomAnimations(0, R.anim.erase_animation)
        }

        showStartBrowsingCfr()
        // Currently this callback can get invoked while the app is in the background. Therefore we are using
        // commitAllowingStateLoss() here because we can't do a fragment transaction while the app is in the
        // background - like we already do in showBrowserScreenForCurrentSession().
        // Ideally we'd make it possible to pause observers while the app is in the background:
        // https://github.com/mozilla-mobile/android-components/issues/876
        transaction
            .replace(
                R.id.container,
                UrlInputFragment.createWithoutSession(),
                UrlInputFragment.FRAGMENT_TAG,
            )
            .commitAllowingStateLoss()
    }

    private fun showStartBrowsingCfr() {
        val onboardingConfig = FocusNimbus.features.onboarding.value(activity)
        if (
            onboardingConfig.isCfrEnabled &&
            !activity.settings.isFirstRun &&
            activity.settings.shouldShowStartBrowsingCfr
        ) {
            FocusNimbus.features.onboarding.recordExposure()
            activity.components.appStore.dispatch(AppAction.ShowStartBrowsingCfrChange(true))
        }
    }

    /**
     * Display the widget promo at first data clearing action and if it wasn't added after 5th Focus session
     * or display branded snackbar when widget promo is not shown.
     */
    @Suppress("MagicNumber")
    private fun showPromoteSearchWidgetDialogOrBrandedSnackbar() {
        val onboardingFeature = FocusNimbus.features.onboarding
        val onboardingConfig = onboardingFeature.value(activity)

        val clearBrowsingSessions = activity.components.settings.getClearBrowsingSessions()
        activity.components.settings.addClearBrowsingSessions(1)

        if (shouldShowPromoteSearchWidgetDialog(onboardingConfig) &&
            (
                clearBrowsingSessions == 0 || clearBrowsingSessions == 4
                )
        ) {
            onboardingFeature.recordExposure()
            SearchWidgetUtils.showPromoteSearchWidgetDialog(activity)
        } else {
            ViewUtils.showBrandedSnackbar(
                activity.findViewById(android.R.id.content),
                R.string.feedback_erase2,
                activity.resources.getInteger(R.integer.erase_snackbar_delay),
            )
        }
    }

    private fun shouldShowPromoteSearchWidgetDialog(onboadingConfig: Onboarding): Boolean {
        return (
            onboadingConfig.isPromoteSearchWidgetDialogEnabled &&
                !activity.components.settings.searchWidgetInstalled
            )
    }

    /**
     * Show browser for tab with the given [tabId].
     */
    fun browser(tabId: String) {
        val fragmentManager = activity.supportFragmentManager

        val urlInputFragment = fragmentManager.findFragmentByTag(UrlInputFragment.FRAGMENT_TAG) as UrlInputFragment?
        if (urlInputFragment != null) {
            fragmentManager
                .beginTransaction()
                .remove(urlInputFragment)
                .commitAllowingStateLoss()
        }

        val browserFragment = fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG) as BrowserFragment?
        if (browserFragment == null || browserFragment.tab.id != tabId) {
            fragmentManager
                .beginTransaction()
                .replace(R.id.container, BrowserFragment.createForTab(tabId), BrowserFragment.FRAGMENT_TAG)
                .commitAllowingStateLoss()
        }
    }

    /**
     * Edit URL of tab with the given [tabId].
     */
    fun edit(
        tabId: String,
    ) {
        val fragmentManager = activity.supportFragmentManager

        val urlInputFragment = fragmentManager.findFragmentByTag(UrlInputFragment.FRAGMENT_TAG) as UrlInputFragment?
        if (urlInputFragment != null && urlInputFragment.tab?.id == tabId) {
            // There's already an UrlInputFragment for this tab.
            return
        }

        val urlFragment = UrlInputFragment.createWithTab(tabId)

        fragmentManager
            .beginTransaction()
            .add(R.id.container, urlFragment, UrlInputFragment.FRAGMENT_TAG)
            .commit()
    }

    /**
     * Show first run onBoarding.
     */
    fun firstRun() {
        val onboardingFragment = if (activity.settings.isNewOnboardingEnable) {
            FocusNimbus.features.onboarding.recordExposure()
            val onBoardingStorage = OnboardingStorage(activity)
            when (onBoardingStorage.getCurrentOnboardingStep()) {
                OnboardingStep.ON_BOARDING_FIRST_SCREEN -> {
                    OnboardingFirstFragment()
                }
                OnboardingStep.ON_BOARDING_SECOND_SCREEN -> {
                    OnboardingSecondFragment()
                }
            }
        } else {
            FirstrunFragment.create()
        }

        activity.supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, onboardingFragment, onboardingFragment::class.java.simpleName)
            .commit()
    }

    fun showOnBoardingSecondScreen() {
        activity.supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, OnboardingSecondFragment(), OnboardingSecondFragment::class.java.simpleName)
            .commit()
    }

    /**
     * Lock app.
     *
     * @param bundle it is used for app navigation. If the user can unlock with success he should
     * be redirected to a certain screen. It comes from the external intent.
     */
    fun lock(bundle: Bundle? = null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw IllegalStateException("Trying to lock unsupported device")
        }

        val fragmentManager = activity.supportFragmentManager
        if (fragmentManager.findFragmentByTag(BiometricAuthenticationFragment.FRAGMENT_TAG) != null) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInPictureInPictureMode) {
            return
        }

        val transaction = fragmentManager
            .beginTransaction()

        fragmentManager.fragments.withEach { fragment ->
            transaction.remove(fragment)
        }

        fragmentManager
            .beginTransaction()
            .replace(
                R.id.container,
                BiometricAuthenticationFragment.createWithDestinationData(bundle),
                BiometricAuthenticationFragment.FRAGMENT_TAG,
            )
            .commit()
    }

    @Suppress("ComplexMethod")
    fun settings(page: Screen.Settings.Page) {
        val fragment = when (page) {
            Screen.Settings.Page.Start -> SettingsFragment()
            Screen.Settings.Page.General -> GeneralSettingsFragment()
            Screen.Settings.Page.Privacy -> PrivacySecuritySettingsFragment()
            Screen.Settings.Page.Search -> SearchSettingsFragment()
            Screen.Settings.Page.Advanced -> AdvancedSettingsFragment()
            Screen.Settings.Page.Mozilla -> MozillaSettingsFragment()
            Screen.Settings.Page.PrivacyExceptions -> ExceptionsListFragment()
            Screen.Settings.Page.PrivacyExceptionsRemove -> ExceptionsRemoveFragment()
            Screen.Settings.Page.SitePermissions -> SitePermissionsFragment()
            Screen.Settings.Page.Studies -> StudiesFragment()
            Screen.Settings.Page.SecretSettings -> SecretSettingsFragment()
            Screen.Settings.Page.SearchList -> InstalledSearchEnginesSettingsFragment()
            Screen.Settings.Page.SearchRemove -> RemoveSearchEnginesSettingsFragment()
            Screen.Settings.Page.SearchAdd -> ManualAddSearchEngineSettingsFragment()
            Screen.Settings.Page.SearchAutocomplete -> AutocompleteSettingsFragment()
            Screen.Settings.Page.SearchAutocompleteList -> AutocompleteListFragment()
            Screen.Settings.Page.SearchAutocompleteAdd -> AutocompleteAddFragment()
            Screen.Settings.Page.SearchAutocompleteRemove -> AutocompleteRemoveFragment()
            Screen.Settings.Page.About -> AboutFragment()
            Screen.Settings.Page.Locale -> LanguageFragment()
        }

        val tag = "settings_" + fragment::class.java.simpleName

        val fragmentManager = activity.supportFragmentManager
        if (fragmentManager.findFragmentByTag(tag) != null) {
            return
        }

        fragmentManager.beginTransaction()
            .replace(R.id.container, fragment, tag)
            .commit()
    }

    fun sitePermissionOptionsFragment(sitePermission: SitePermission) {
        val fragmentManager = activity.supportFragmentManager
        fragmentManager.beginTransaction()
            .replace(
                R.id.container,
                SitePermissionOptionsFragment.addSitePermission(sitePermission = sitePermission),
                SitePermissionOptionsFragment.FRAGMENT_TAG,
            )
            .commit()
    }
}
