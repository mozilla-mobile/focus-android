/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.activity.robots

import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import org.junit.Assert.assertTrue
import org.mozilla.focus.R
import org.mozilla.focus.helpers.TestHelper.getStringResource
import org.mozilla.focus.helpers.TestHelper.isPackageInstalled
import org.mozilla.focus.helpers.TestHelper.mDevice
import org.mozilla.focus.helpers.TestHelper.packageName
import org.mozilla.focus.helpers.TestHelper.waitingTime
import org.mozilla.focus.helpers.TestHelper.webPageLoadwaitingTime
import org.mozilla.focus.idlingResources.SessionLoadedIdlingResource

class DownloadRobot {
    fun verifyDownloadDialog(fileName: String) {
        assertTrue(downloadDialogTitle.waitForExists(waitingTime))
        assertTrue(downloadCancelBtn.exists())
        assertTrue(downloadBtn.exists())
        assertTrue(downloadFileName.text.contains(fileName))
    }

    fun verifyDownloadDialogGone() = assertTrue(downloadDialogTitle.waitUntilGone(waitingTime))

    fun verifyDownloadNotification() = NotificationRobot().verifySystemNotificationExists(downloadNotificationText)

    fun verifyPhotosOpens() = assertPhotosOpens()

    fun clickDownloadIconAsset() {
        val sessionLoadedIdlingResource = SessionLoadedIdlingResource()
        runWithIdleRes(sessionLoadedIdlingResource) {
            downloadIconAsset.waitForExists(webPageLoadwaitingTime)
            downloadIconAsset.click()
        }
    }

    fun clickDownloadButton() {
        downloadBtn.waitForExists(waitingTime)
        downloadBtn.click()
    }

    fun clickCancelDownloadButton() {
        downloadCancelBtn.waitForExists(waitingTime)
        downloadCancelBtn.click()
    }

    fun openDownloadedFile() {
        val snackBarButton = mDevice.findObject(UiSelector().resourceId("$packageName:id/snackbar_action"))
        snackBarButton.waitForExists(waitingTime)
        snackBarButton.clickAndWaitForNewWindow(webPageLoadwaitingTime)
    }

    class Transition
}

fun downloadRobot(interact: DownloadRobot.() -> Unit): DownloadRobot.Transition {
    DownloadRobot().interact()
    return DownloadRobot.Transition()
}

val downloadIconAsset: UiObject = mDevice.findObject(
    UiSelector()
        .resourceId("download")
)

private val downloadDialogTitle = mDevice.findObject(
    UiSelector()
        .resourceId("$packageName:id/title")
)

private val downloadFileName = mDevice.findObject(
    UiSelector()
        .resourceId("$packageName:id/filename")
)

private val downloadCancelBtn = mDevice.findObject(
    UiSelector()
        .resourceId("$packageName:id/close_button")
)

private val downloadBtn = mDevice.findObject(
    UiSelector()
        .resourceId("$packageName:id/download_button")
)

private val downloadNotificationText = getStringResource(R.string.mozac_feature_downloads_completed_notification_text2)

private fun assertPhotosOpens() {
    val googleAppsPhotos = "com.google.android.apps.photos"
    if (isPackageInstalled(googleAppsPhotos)) {
        Intents.intended(IntentMatchers.toPackage(googleAppsPhotos))
    } else {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        mDevice.findObject(UiSelector().textContains("No app found")).waitForExists(waitingTime)
    }
}
