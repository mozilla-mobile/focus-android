/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.shortcut

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.support.annotation.VisibleForTesting
import android.support.v4.content.pm.ShortcutInfoCompat
import android.support.v4.content.pm.ShortcutManagerCompat
import android.support.v4.graphics.drawable.IconCompat
import android.text.TextUtils

import org.mozilla.focus.activity.IntentReceiverActivity
import org.mozilla.focus.utils.UrlUtils

import java.util.UUID

object HomeScreen {
  val ADD_TO_HOMESCREEN_TAG = "add_to_homescreen"
  val BLOCKING_ENABLED = "blocking_enabled"
  val REQUEST_DESKTOP = "request_desktop"

  /**
   * Create a shortcut for the given website on the device's home screen.
   */
  fun installShortCut(
    context: Context,
    icon: Bitmap,
    url: String,
    title: String,
    blockingEnabled: Boolean,
    requestDesktop: Boolean
  ) {
    var titleString = title
    if (TextUtils.isEmpty(title.trim { it <= ' ' })) {
      titleString = generateTitleFromUrl(url).toString()
    }

    installShortCutViaManager(context, icon, url, titleString, blockingEnabled, requestDesktop)

    // Creating shortcut flow is different on Android up to 7, so we want to go
    // to the home screen manually where the user will see the new shortcut appear
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
      goToHomeScreen(context)
    }
  }

  /**
   * Create a shortcut via the AppCompat's shortcut manager.
   *
   *
   * On Android versions up to 7 shortcut will be created via system broadcast internally.
   *
   *
   * On Android 8+ the user will have the ability to add the shortcut manually
   * or let the system place it automatically.
   */
  private fun installShortCutViaManager(
    context: Context,
    bitmap: Bitmap,
    url: String,
    title: String,
    blockingEnabled: Boolean,
    requestDesktop: Boolean
  ) {
    if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
      val icon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        IconCompat.createWithAdaptiveBitmap(bitmap)
      else
        IconCompat.createWithBitmap(bitmap)
      val shortcut = ShortcutInfoCompat.Builder(context, UUID.randomUUID().toString())
          .setShortLabel(title)
          .setLongLabel(title)
          .setIcon(icon)
          .setIntent(createShortcutIntent(context, url, blockingEnabled, requestDesktop))
          .build()
      ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }
  }

  private fun createShortcutIntent(
    context: Context,
    url: String,
    blockingEnabled: Boolean,
    requestDesktop: Boolean
  ): Intent {
    val shortcutIntent = Intent(context, IntentReceiverActivity::class.java)
    shortcutIntent.action = Intent.ACTION_VIEW
    shortcutIntent.data = Uri.parse(url)
    shortcutIntent.putExtra(BLOCKING_ENABLED, blockingEnabled)
    shortcutIntent.putExtra(REQUEST_DESKTOP, requestDesktop)
    shortcutIntent.putExtra(ADD_TO_HOMESCREEN_TAG, ADD_TO_HOMESCREEN_TAG)
    return shortcutIntent
  }

  @VisibleForTesting internal fun generateTitleFromUrl(url: String): String? {
    // For now we just use the host name and strip common subdomains like "www" or "m".
    return UrlUtils.stripCommonSubdomains(Uri.parse(url).host)
  }

  /**
   * Switch to the the default home screen activity (launcher).
   */
  private fun goToHomeScreen(context: Context) {
    val intent = Intent(Intent.ACTION_MAIN)

    intent.addCategory(Intent.CATEGORY_HOME)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
  }
}
