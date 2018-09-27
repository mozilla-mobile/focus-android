package org.mozilla.focus.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import org.mozilla.focus.session.SessionManager
import org.mozilla.focus.telemetry.TelemetryWrapper

class EraseAndOpenShortcutActivity: Activity(){

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    SessionManager.getInstance().removeAllSessions()

    TelemetryWrapper.eraseAndOpenShortcutEvent()

    val intent = Intent(this, MainActivity::class.java)
    intent.action = MainActivity.ACTION_OPEN
    startActivity(intent)

  }

}