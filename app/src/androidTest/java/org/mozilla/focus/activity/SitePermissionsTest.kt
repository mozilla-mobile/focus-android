/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.test.filters.SdkSuppress
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mozilla.focus.activity.robots.homeScreen
import org.mozilla.focus.activity.robots.searchScreen
import org.mozilla.focus.helpers.FeatureSettingsHelper
import org.mozilla.focus.helpers.MainActivityFirstrunTestRule
import org.mozilla.focus.helpers.MockWebServerHelper
import org.mozilla.focus.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.focus.helpers.TestAssetHelper.getMediaTestAsset
import org.mozilla.focus.helpers.TestHelper.exitToTop
import org.mozilla.focus.helpers.TestHelper.getTargetContext
import org.mozilla.focus.helpers.TestHelper.grantAppPermission
import org.mozilla.focus.helpers.TestHelper.waitingTime
import org.mozilla.focus.testAnnotations.SmokeTest

class SitePermissionsTest {
    private lateinit var webServer: MockWebServer
    private val featureSettingsHelper = FeatureSettingsHelper()
    /* Test page created and handled by the Mozilla mobile test-eng team */
    private val permissionsPage = "https://mozilla-mobile.github.io/testapp/permissions"
    private val testPageSubstring = "https://mozilla-mobile.github.io:443"
    private val cameraManager = getTargetContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    @get: Rule
    val mActivityTestRule = MainActivityFirstrunTestRule(showFirstRun = false)

    @Before
    fun setUp() {
        featureSettingsHelper.setCfrForTrackingProtectionEnabled(false)
        webServer = MockWebServer().apply {
            dispatcher = MockWebServerHelper.AndroidAssetDispatcher()
            start()
        }
    }

    @After
    fun tearDown() {
        webServer.shutdown()
        featureSettingsHelper.resetAllFeatureFlags()
    }

    @Test
    fun sitePermissionsSettingsItemsTest() {
        homeScreen {
        }.openMainMenu {
        }.openSettings {
        }.openPrivacySettingsMenu {
        }.clickSitePermissionsSettings {
            verifySitePermissionsItems()
        }
    }

    @SmokeTest
    @Test
    fun autoplayPermissionsSettingsItemsTest() {
        homeScreen {
        }.openMainMenu {
        }.openSettings {
        }.openPrivacySettingsMenu {
        }.clickSitePermissionsSettings {
            openAutoPlaySettings()
            verifyAutoplaySection()
        }
    }

    @SmokeTest
    @Test
    // Tests the default autoplay setting: Block audio only on a video with autoplay attribute and not muted
    fun blockAudioAutoplayPermissionTest() {
        val videoPage = getMediaTestAsset(webServer, "videoPage")

        searchScreen {
        }.loadPage(videoPage.url) {
            progressBar.waitUntilGone(waitingTime)
            // an un-muted video won't be able to autoplay with this setting, so we have to press play
            clickPlayButton()
            waitForPlaybackToStart()
        }
    }

    @SmokeTest
    @Test
    // Tests the default autoplay setting: Block audio only on a video with autoplay and muted attributes
    fun blockAudioAutoplayPermissionOnMutedVideoTest() {
        val mutedVideoPage = getMediaTestAsset(webServer, "mutedVideoPage")

        searchScreen {
        }.loadPage(mutedVideoPage.url) {
            // a muted video will autoplay with this setting
            waitForPlaybackToStart()
        }
    }

    @SmokeTest
    @Test
    // Tests the autoplay setting: Allow audio and video on a video with autoplay attribute and not muted
    fun allowAudioVideoAutoplayPermissionTest() {
        val videoPage = getMediaTestAsset(webServer, "videoPage")

        homeScreen {
        }.openMainMenu {
        }.openSettings {
        }.openPrivacySettingsMenu {
        }.clickSitePermissionsSettings {
            openAutoPlaySettings()
            selectAllowAudioVideoAutoplay()
            exitToTop()
        }
        searchScreen {
        }.loadPage(videoPage.url) {
            waitForPlaybackToStart()
        }
    }

    @SmokeTest
    @Test
    // Tests the autoplay setting: Allow audio and video on a video with autoplay and muted attributes
    fun allowAudioVideoAutoplayPermissionOnMutedVideoTest() {
        val genericPage = getGenericAsset(webServer)
        val mutedVideoPage = getMediaTestAsset(webServer, "mutedVideoPage")

        homeScreen {
        }.openMainMenu {
        }.openSettings {
        }.openPrivacySettingsMenu {
        }.clickSitePermissionsSettings {
            openAutoPlaySettings()
            selectAllowAudioVideoAutoplay()
            exitToTop()
        }
        searchScreen {
        }.loadPage(genericPage.url) {
        }.clearBrowsingData {}
        searchScreen {
        }.loadPage(mutedVideoPage.url) {
            waitForPlaybackToStart()
        }
    }

    @SmokeTest
    @Test
    // Tests the autoplay setting: Block audio and video
    fun blockAudioVideoAutoplayPermissionTest() {
        val videoPage = getMediaTestAsset(webServer, "videoPage")

        homeScreen {
        }.openMainMenu {
        }.openSettings {
        }.openPrivacySettingsMenu {
        }.clickSitePermissionsSettings {
            openAutoPlaySettings()
            selectBlockAudioVideoAutoplay()
            exitToTop()
        }
        searchScreen {
        }.loadPage(videoPage.url) {
            clickPlayButton()
            waitForPlaybackToStart()
        }
    }

    @SmokeTest
    @Test
    fun cameraPermissionsSettingsItemsTest() {
        homeScreen {
        }.openMainMenu {
        }.openSettings {
        }.openPrivacySettingsMenu {
        }.clickSitePermissionsSettings {
            openCameraPermissionsSettings()
            verifyPermissionsStateSettings()
            verifyAskToAllowChecked()
            verifyBlockedByAndroidState()
        }
    }

    @SmokeTest
    @Test
    fun locationPermissionsSettingsItemsTest() {
        homeScreen {
        }.openMainMenu {
        }.openSettings {
        }.openPrivacySettingsMenu {
        }.clickSitePermissionsSettings {
            openLocationPermissionsSettings()
            verifyPermissionsStateSettings()
            verifyAskToAllowChecked()
            verifyBlockedByAndroidState()
        }
    }

    @Test
    fun testLocationSharingNotAllowed() {
        searchScreen {
        }.loadPage(permissionsPage) {
            clickGetLocationButton()
            verifyLocationPermissionPrompt(testPageSubstring)
            denySitePermissionRequest()
            verifyPageContent("User denied geolocation prompt")
        }
    }

    @Ignore(
        "Needs mocking location for Firebase " +
            "- to do: https://github.com/mozilla-mobile/mobile-test-eng/issues/585"
    )
    @SmokeTest
    @Test
    fun testLocationSharingAllowed() {
        searchScreen {
        }.loadPage(permissionsPage) {
            clickGetLocationButton()
            verifyLocationPermissionPrompt(testPageSubstring)
            allowSitePermissionRequest()
            grantAppPermission()
            verifyPageContent("longitude")
            verifyPageContent("latitude")
        }
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.P, codeName = "P")
    @SmokeTest
    @Test
    fun allowCameraPermissionsTest() {
        // skips the test if the AVD doesn't have a camera
        assumeTrue(cameraManager.cameraIdList.isNotEmpty())

        searchScreen {
        }.loadPage(permissionsPage) {
            clickGetCameraButton()
            grantAppPermission()
            verifyCameraPermissionPrompt(testPageSubstring)
            allowSitePermissionRequest()
            verifyPageContent("Camera allowed")
        }
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.P, codeName = "P")
    @SmokeTest
    @Test
    fun denyCameraPermissionsTest() {
        // skips the test if the AVD doesn't have a camera
        assumeTrue(cameraManager.cameraIdList.isNotEmpty())

        searchScreen {
        }.loadPage(permissionsPage) {
            clickGetCameraButton()
            grantAppPermission()
            verifyCameraPermissionPrompt(testPageSubstring)
            denySitePermissionRequest()
            verifyPageContent("Camera not allowed")
        }
    }
}
