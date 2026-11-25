package com.pinsettings.app

import android.content.Context
import java.security.MessageDigest
import java.util.UUID

object PinStore {

    private fun prefs(c: Context) = c.getSharedPreferences("pin_store", Context.MODE_PRIVATE)

    private fun hash(salt: String, pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("$salt:$pin".toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun isSet(c: Context): Boolean = prefs(c).getString("hash", null) != null

    fun save(c: Context, pin: String) {
        val salt = UUID.randomUUID().toString()
        prefs(c).edit()
            .putString("salt", salt)
            .putString("hash", hash(salt, pin))
            .putInt("fails", 0)
            .putLong("until", 0L)
            .apply()
    }

    /** Kitne millisecond baad dobara try kar sakte ho (0 = abhi kar sakte ho). */
    fun waitMs(c: Context): Long =
        (prefs(c).getLong("until", 0L) - System.currentTimeMillis()).coerceAtLeast(0L)

    fun check(c: Context, pin: String): Boolean {
        if (waitMs(c) > 0L) return false
        val p = prefs(c)
        val salt = p.getString("salt", null) ?: return false
        if (hash(salt, pin) == p.getString("hash", null)) {
            p.edit().putInt("fails", 0).putLong("until", 0L).apply()
            return true
        }
        val fails = p.getInt("fails", 0) + 1
        val e = p.edit().putInt("fails", fails)
        if (fails % 5 == 0) {
            val step = (fails / 5 - 1).coerceAtMost(5)
            e.putLong("until", System.currentTimeMillis() + 30_000L * (1L shl step))
        }
        e.apply()
        return false
    }
}
