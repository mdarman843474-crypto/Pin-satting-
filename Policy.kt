package com.pinsettings.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager

/** Device Owner wale kaam. Device owner na ho to sab false return karte hain. */
object Policy {

    private fun dpm(c: Context): DevicePolicyManager =
        c.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private fun admin(c: Context) = ComponentName(c, AdminReceiver::class.java)

    fun isOwner(c: Context): Boolean =
        try {
            dpm(c).isDeviceOwnerApp(c.packageName)
        } catch (e: Exception) {
            false
        }

    fun hasRestriction(c: Context, key: String): Boolean =
        try {
            (c.getSystemService(Context.USER_SERVICE) as UserManager).hasUserRestriction(key)
        } catch (e: Exception) {
            false
        }

    fun setRestriction(c: Context, key: String, on: Boolean): Boolean =
        try {
            if (on) {
                dpm(c).addUserRestriction(admin(c), key)
            } else {
                dpm(c).clearUserRestriction(admin(c), key)
            }
            true
        } catch (e: Exception) {
            false
        }

    fun isUninstallBlocked(c: Context): Boolean =
        try {
            dpm(c).isUninstallBlocked(null, c.packageName)
        } catch (e: Exception) {
            false
        }

    fun setUninstallBlocked(c: Context, on: Boolean): Boolean =
        try {
            dpm(c).setUninstallBlocked(admin(c), c.packageName, on)
            true
        } catch (e: Exception) {
            false
        }

    /** Saari locks hatakar device owner hata deta hai. */
    fun removeOwner(c: Context): Boolean =
        try {
            SettingsGuard.disable(c)
            setRestriction(c, UserManager.DISALLOW_FACTORY_RESET, false)
            setRestriction(c, UserManager.DISALLOW_APPS_CONTROL, false)
            setUninstallBlocked(c, false)
            dpm(c).clearDeviceOwnerApp(c.packageName)
            try {
                dpm(c).removeActiveAdmin(admin(c))
            } catch (e: Exception) {
                // koi baat nahi
            }
            true
        } catch (e: Exception) {
            false
        }
}
