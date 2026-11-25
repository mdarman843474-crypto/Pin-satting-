package com.pinsettings.app

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.SystemClock

/**
 * Original Settings ko lock karta hai (pehle suspend, na ho to hide).
 *
 * Row dabane par: Settings unlock + Home / Recents band (lock task mode).
 * Back dabake app me wapas aate hi: Home wapas + Settings turant dobara lock.
 * Timer (Max session time) sirf safety ke liye hai.
 */
object SettingsGuard {

    const val ACTION_RELOCK = "com.pinsettings.app.RELOCK"

    /** Jo apps lock hongi. */
    private val PACKAGES = arrayOf("com.android.settings")

    /** Settings ke andar se khul sakne wali apps (system apps ki list ke saath judti hai). */
    private val KNOWN = listOf(
        "android",
        "com.android.settings",
        "com.android.settings.intelligence",
        "com.android.phone",
        "com.android.systemui",
        "com.android.bluetooth",
        "com.android.emergency",
        "com.android.captiveportallogin",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.miui.packageinstaller",
        "com.miui.securitycenter",
        "com.miui.securitycore",
        "com.lbe.security.miui",
        "com.xiaomi.misettings"
    )

    /** Aakhri koshish ka natija (dikkat aaye to dikhane ke liye). */
    var lastDetail: String = ""
        private set

    /** Aakhri row kholte waqt Home band ho paya ya nahi. */
    var homeLocked: Boolean = false
        private set

    private var openedAt = 0L

    private fun prefs(c: Context) = c.getSharedPreferences("guard", Context.MODE_PRIVATE)

    private fun dpm(c: Context): DevicePolicyManager =
        c.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private fun admin(c: Context) = ComponentName(c, AdminReceiver::class.java)

    fun isEnabled(c: Context): Boolean = prefs(c).getBoolean("enabled", false)

    fun minutes(c: Context): Int = prefs(c).getInt("minutes", 10)

    fun setMinutes(c: Context, m: Int) {
        prefs(c).edit().putInt("minutes", m).apply()
    }

    private fun isOpen(c: Context): Boolean = prefs(c).getBoolean("open", false)

    private fun markOpen(c: Context, value: Boolean, until: Long) {
        prefs(c).edit().putBoolean("open", value).putLong("until", until).apply()
    }

    /** Lock ON. Lock na lag paye to enabled nahi rakhta. */
    fun enable(c: Context): Boolean {
        if (!Policy.isOwner(c)) return false
        prefs(c).edit().putBoolean("enabled", true).apply()
        if (lock(c)) return true
        prefs(c).edit().putBoolean("enabled", false).apply()
        unlockNow(c)
        return false
    }

    /** Lock OFF. Settings wapas poori tarah khul jati hai. */
    fun disable(c: Context) {
        prefs(c).edit().putBoolean("enabled", false).apply()
        try {
            cancelAlarm(c)
        } catch (e: Exception) {
            // koi baat nahi
        }
        clearHomeLock(c)
        unlockNow(c)
        markOpen(c, false, 0L)
    }

    /** Row kholne se pehle: Settings unlock + Home band. true = aage badh sakte ho. */
    fun prepare(a: Activity): Boolean {
        homeLocked = false
        if (!isEnabled(a) || !Policy.isOwner(a)) return true
        if (!unlockNow(a)) return false
        val until = System.currentTimeMillis() + minutes(a) * 60_000L
        markOpen(a, true, until)
        schedule(a, until)
        openedAt = SystemClock.elapsedRealtime()
        startHomeLock(a)
        return true
    }

    /** Settings se wapas aate hi: Home wapas aur Settings turant dobara lock. */
    fun endSession(a: Activity, force: Boolean = false) {
        if (!isEnabled(a) || !isOpen(a)) return
        if (!force && SystemClock.elapsedRealtime() - openedAt < 1500L) return
        try {
            a.stopLockTask()
        } catch (e: Exception) {
            // lock task me nahi the
        }
        relock(a)
    }

    /** Timer khatam hone par ya boot par. */
    fun relock(c: Context) {
        if (isEnabled(c) && Policy.isOwner(c)) lock(c)
    }

    private fun lock(c: Context): Boolean {
        try {
            cancelAlarm(c)
        } catch (e: Exception) {
            // koi baat nahi
        }
        markOpen(c, false, 0L)
        clearHomeLock(c)
        return try {
            val d = dpm(c)
            val a = admin(c)
            val failed = d.setPackagesSuspended(a, PACKAGES, true)
            val sb = StringBuilder("suspend: ")
            sb.append(if (failed.isEmpty()) "ok" else "nahi hua")
            for (p in failed) {
                // suspend nahi hua: hide try karo
                val h = d.setApplicationHidden(a, p, true)
                sb.append(", hide: ").append(if (h) "ok" else "nahi hua")
            }
            lastDetail = sb.toString()
            PACKAGES.all { p -> d.isApplicationHidden(a, p) || d.isPackageSuspended(a, p) }
        } catch (e: Exception) {
            lastDetail = "error: " + e.javaClass.simpleName + " " + (e.message ?: "")
            false
        }
    }

    private fun unlockNow(c: Context): Boolean {
        return try {
            val d = dpm(c)
            val a = admin(c)
            for (p in PACKAGES) {
                if (d.isApplicationHidden(a, p)) d.setApplicationHidden(a, p, false)
            }
            d.setPackagesSuspended(a, PACKAGES, false)
            PACKAGES.none { p -> d.isApplicationHidden(a, p) || d.isPackageSuspended(a, p) }
        } catch (e: Exception) {
            false
        }
    }

    // ------------------------------------------------------------ Home band (lock task)

    private fun startHomeLock(a: Activity) {
        try {
            val d = dpm(a)
            val ad = admin(a)
            d.setLockTaskPackages(ad, allowedPackages(a))
            // Home, Recents, notification shade band. Lock screen, power menu aur clock chalu.
            d.setLockTaskFeatures(
                ad,
                DevicePolicyManager.LOCK_TASK_FEATURE_KEYGUARD or
                    DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS or
                    DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO
            )
            if (d.isLockTaskPermitted(a.packageName)) {
                a.startLockTask()
                homeLocked = true
            }
        } catch (e: Exception) {
            lastDetail = "home band: " + e.javaClass.simpleName + " " + (e.message ?: "")
        }
    }

    private fun clearHomeLock(c: Context) {
        try {
            dpm(c).setLockTaskPackages(admin(c), arrayOf<String>())
        } catch (e: Exception) {
            // koi baat nahi
        }
    }

    /** Is app + saari system apps (launcher ke bina). User ki install ki hui apps band rahengi. */
    private fun allowedPackages(c: Context): Array<String> {
        val set = LinkedHashSet<String>()
        set.add(c.packageName)
        try {
            val mask = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
            for (ai in c.packageManager.getInstalledApplications(0)) {
                if ((ai.flags and mask) != 0) set.add(ai.packageName)
            }
        } catch (e: Exception) {
            // list na mile to KNOWN list kaam aayegi
        }
        set.addAll(KNOWN)
        try {
            val home = Intent(Intent.ACTION_MAIN)
            home.addCategory(Intent.CATEGORY_HOME)
            val ri = c.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
            val hp = ri?.activityInfo?.packageName
            if (hp != null && hp != "android" && hp != c.packageName) set.remove(hp)
        } catch (e: Exception) {
            // koi baat nahi
        }
        return set.toTypedArray()
    }

    // ------------------------------------------------------------ safety timer

    private fun alarmIntent(c: Context): PendingIntent {
        val i = Intent(c, GuardReceiver::class.java)
        i.action = ACTION_RELOCK
        return PendingIntent.getBroadcast(
            c, 7, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun schedule(c: Context, at: Long) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarmIntent(c))
    }

    private fun cancelAlarm(c: Context) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(alarmIntent(c))
    }
}
