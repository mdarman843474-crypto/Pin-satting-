package com.pinsettings.app

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.provider.Settings

/**
 * Har row ke liye asli system Settings page ke intents.
 * Pehla jo khul jaye wahi use hota hai. Koi na khule to main Settings khulti hai.
 */
object Links {

    private fun act(action: String) = Intent(action)

    private fun cls(pkg: String, name: String) = Intent().setClassName(pkg, name)

    val wifi = listOf(act(Settings.ACTION_WIFI_SETTINGS))

    val bluetooth = listOf(act(Settings.ACTION_BLUETOOTH_SETTINGS))

    val mobile = listOf(
        act(Settings.ACTION_DATA_ROAMING_SETTINGS),
        act(Settings.ACTION_NETWORK_OPERATOR_SETTINGS),
        act(Settings.ACTION_WIRELESS_SETTINGS)
    )

    val hotspot = listOf(
        cls("com.android.settings", "com.android.settings.TetherSettings"),
        cls("com.android.settings", "com.android.settings.Settings\$TetherSettingsActivity"),
        cls("com.android.settings", "com.android.settings.Settings\$WifiApSettingsActivity"),
        act(Settings.ACTION_WIRELESS_SETTINGS)
    )

    val lock = listOf(
        act(Settings.ACTION_SECURITY_SETTINGS),
        act(DevicePolicyManager.ACTION_SET_NEW_PASSWORD)
    )

    val safety = listOf(
        act("android.intent.action.SAFETY_CENTER")
    )

    val privacy = listOf(
        act(Settings.ACTION_PRIVACY_SETTINGS),
        act(Settings.ACTION_SECURITY_SETTINGS)
    )

    val apps = listOf(
        act(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS),
        act(Settings.ACTION_APPLICATION_SETTINGS)
    )
}
