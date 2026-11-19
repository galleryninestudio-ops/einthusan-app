package com.bench.einthusan.ui.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Explicit PendingIntent destination. Kept private to this app by the manifest. */
class PipActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Pip.handleAction(intent.action)
    }
}
