package com.pinsettings.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Boot ke baad aur timer khatam hone par Original Settings ko dobara lock karta hai. */
class GuardReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val a = intent.action
        if (a == Intent.ACTION_BOOT_COMPLETED || a == SettingsGuard.ACTION_RELOCK) {
            SettingsGuard.relock(context)
        }
    }
}
