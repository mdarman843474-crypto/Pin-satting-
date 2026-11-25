package com.pinsettings.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.SystemClock
import android.os.UserManager
import android.provider.Settings
import android.text.InputFilter
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    companion object {
        private var unlocked = false
        private var leftAt = 0L
        private const val OWNER_CMD =
            "adb shell dpm set-device-owner com.pinsettings.app/.AdminReceiver"
    }

    private val bg = Color.BLACK
    private val cardColor = Color.parseColor("#242424")
    private val white = Color.WHITE
    private val gray = Color.parseColor("#8E8E93")
    private val blue = Color.parseColor("#2F80ED")
    private val green = Color.parseColor("#1FBF5B")
    private val purple = Color.parseColor("#8B7CF6")
    private val orange = Color.parseColor("#F2531D")
    private val teal = Color.parseColor("#0FA5A0")
    private val red = Color.parseColor("#E5484D")
    private val slate = Color.parseColor("#5B5B66")

    private val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
    private val match = ViewGroup.LayoutParams.MATCH_PARENT

    private var scroll: ScrollView? = null

    // ---------------------------------------------------------------- lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        route()
    }

    override fun onStart() {
        super.onStart()
        SettingsGuard.endSession(this)
        if (!unlocked) return
        if (leftAt != 0L && SystemClock.elapsedRealtime() - leftAt > 60_000L) {
            // 1 minute se zyada bahar rahe: dobara PIN
            unlocked = false
            route()
        } else if (scroll != null) {
            refresh()
        }
    }

    override fun onStop() {
        super.onStop()
        leftAt = SystemClock.elapsedRealtime()
    }

    private fun route() {
        if (!PinStore.isSet(this)) {
            showPin(true)
        } else if (!unlocked) {
            showPin(false)
        } else {
            showHome()
        }
    }

    private fun refresh() {
        val y = scroll?.scrollY ?: 0
        showHome()
        scroll?.post { scroll?.scrollTo(0, y) }
    }

    // ---------------------------------------------------------------- PIN screen

    private fun showPin(setup: Boolean) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER
        box.setBackgroundColor(bg)
        box.setPadding(dp(32), dp(32), dp(32), dp(32))

        box.addView(label(if (setup) "PIN set karo" else "PIN dalo", 28f, white, true))

        val sub = label(
            if (setup) "4 se 8 digit. PIN yaad rakhna, iske bina protection nahi hategi."
            else "Settings kholne ke liye PIN dalo",
            14f, gray
        )
        sub.gravity = Gravity.CENTER
        sub.setPadding(0, dp(8), 0, dp(24))
        box.addView(sub)

        val e1 = pinField("PIN")
        e1.gravity = Gravity.CENTER
        box.addView(e1, LinearLayout.LayoutParams(match, wrap))

        val e2 = pinField("PIN dobara")
        e2.gravity = Gravity.CENTER
        if (setup) box.addView(e2, LinearLayout.LayoutParams(match, wrap))

        val btn = Button(this)
        btn.text = if (setup) "Save" else "Unlock"
        val bp = LinearLayout.LayoutParams(match, wrap)
        bp.topMargin = dp(16)
        box.addView(btn, bp)

        btn.setOnClickListener {
            val p = e1.text.toString()
            if (setup) {
                if (p.length < 4) {
                    toast("Kam se kam 4 digit chahiye")
                } else if (p != e2.text.toString()) {
                    toast("Dono PIN match nahi hue")
                } else {
                    PinStore.save(this, p)
                    unlocked = true
                    showHome()
                }
            } else if (checkPin(p)) {
                unlocked = true
                showHome()
            }
        }

        val last = if (setup) e2 else e1
        last.imeOptions = EditorInfo.IME_ACTION_DONE
        last.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE) {
                btn.performClick()
                true
            } else {
                false
            }
        }

        setContentView(box)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        e1.requestFocus()
    }

    private fun checkPin(pin: String): Boolean {
        val wait = PinStore.waitMs(this)
        if (wait > 0L) {
            toast("Ruko, ${wait / 1000 + 1} second baad try karo")
            return false
        }
        val ok = PinStore.check(this, pin)
        if (!ok) toast("Galat PIN")
        return ok
    }

    // ---------------------------------------------------------------- Home

    private fun showHome() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        val owner = Policy.isOwner(this)
        if (owner) applyDefaultsOnce()

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(28), dp(16), dp(36))

        val head = LinearLayout(this)
        head.gravity = Gravity.CENTER_VERTICAL
        head.addView(label("Settings", 34f, white, true), LinearLayout.LayoutParams(0, wrap, 1f))
        val lockBtn = label("Lock", 16f, Color.parseColor("#4C9AFF"))
        lockBtn.setPadding(dp(12), dp(8), dp(12), dp(8))
        lockBtn.setOnClickListener {
            unlocked = false
            route()
        }
        head.addView(lockBtn)
        col.addView(head)
        col.addView(space(18))

        col.addView(
            card(
                row(R.drawable.ic_wifi, blue, "Wi-Fi", wifiState()) { openPage(Links.wifi) },
                row(R.drawable.ic_bluetooth, blue, "Bluetooth", btState()) { openPage(Links.bluetooth) },
                row(R.drawable.ic_mobile, green, "Mobile networks", null) { openPage(Links.mobile) },
                row(R.drawable.ic_hotspot, blue, "Portable hotspot", null) { openPage(Links.hotspot) }
            )
        )
        col.addView(space(14))

        col.addView(
            card(
                row(R.drawable.ic_fingerprint, purple, "Fingerprints, face data, and screen lock", null) {
                    openPage(Links.lock)
                },
                row(R.drawable.ic_safety, orange, "Safety and emergency", null) { openPage(Links.safety) },
                row(R.drawable.ic_privacy, blue, "Privacy protection", null) { openPage(Links.privacy) },
                row(R.drawable.ic_apps, blue, "Apps", null) { openPage(Links.apps) }
            )
        )
        col.addView(space(22))

        col.addView(section("Original Settings"))
        col.addView(
            card(
                switchRow(
                    R.drawable.ic_lock, orange, "Original Settings lock",
                    if (SettingsGuard.isEnabled(this)) "Band hai. Row kholne par Home band, wapas aate hi lock"
                    else "Off. Original Settings khuli hai",
                    SettingsGuard.isEnabled(this), owner
                ) { on -> toggleGuard(on) },
                row(R.drawable.ic_timer, slate, "Max session time", "${SettingsGuard.minutes(this)} min") {
                    pickMinutes()
                }
            )
        )
        col.addView(space(22))

        col.addView(section("Protection"))
        col.addView(
            card(
                row(R.drawable.ic_privacy, teal, "Device owner", if (owner) "Active" else "Not set") {
                    if (owner) toast("Device owner active hai") else ownerHelp()
                },
                switchRow(
                    R.drawable.ic_trash, red, "Uninstall protection",
                    "Is app ko uninstall hone se rokta hai",
                    Policy.isUninstallBlocked(this), owner
                ) { on -> Policy.setUninstallBlocked(this, on) },
                switchRow(
                    R.drawable.ic_reset, orange, "Factory reset lock",
                    "Settings se reset block. Recovery se nahi",
                    Policy.hasRestriction(this, UserManager.DISALLOW_FACTORY_RESET), owner
                ) { on -> Policy.setRestriction(this, UserManager.DISALLOW_FACTORY_RESET, on) },
                switchRow(
                    R.drawable.ic_lock, purple, "App control lock",
                    "Clear data, force stop, disable block",
                    Policy.hasRestriction(this, UserManager.DISALLOW_APPS_CONTROL), owner
                ) { on -> Policy.setRestriction(this, UserManager.DISALLOW_APPS_CONTROL, on) },
                row(R.drawable.ic_key, slate, "Change PIN", null) { changePin() },
                row(R.drawable.ic_cancel, red, "Remove protection", null) { removeProtection() }
            )
        )

        val s = ScrollView(this)
        s.setBackgroundColor(bg)
        s.addView(col)
        scroll = s
        setContentView(s)
    }

    private fun applyDefaultsOnce() {
        val p = getSharedPreferences("app", Context.MODE_PRIVATE)
        if (!p.getBoolean("defaults_done", false)) {
            Policy.setUninstallBlocked(this, true)
            p.edit().putBoolean("defaults_done", true).apply()
        }
    }

    // ---------------------------------------------------------------- actions

    private fun openPage(list: List<Intent>) {
        // Original Settings lock ho to: Settings kholo aur Home band karo
        if (!SettingsGuard.prepare(this)) {
            toast("Original Settings unlock nahi ho payi")
            return
        }
        val locked = SettingsGuard.isEnabled(this)
        if (locked && !SettingsGuard.homeLocked) {
            toast("Home band nahi ho paya")
        }
        for (i in list) {
            try {
                startActivity(i)
                return
            } catch (e: Exception) {
                // agla intent try karo
            }
        }
        if (locked) {
            // lock ON ho to poori Settings mat kholo
            SettingsGuard.endSession(this, true)
            toast("Ye page is phone par nahi khula. Mujhe batao kaunsa")
            return
        }
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (e: Exception) {
            toast("Ye screen khul nahi saki")
        }
    }

    private fun wifiState(): String {
        return try {
            val w = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (w.isWifiEnabled) "On" else "Off"
        } catch (e: Exception) {
            ""
        }
    }

    private fun btState(): String {
        return try {
            when (Settings.Global.getInt(contentResolver, "bluetooth_on", -1)) {
                1 -> "On"
                0 -> "Off"
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun ownerHelp() {
        dialog()
            .setTitle("Device owner set karo")
            .setMessage(
                "Phone par koi Google / Mi account nahi hona chahiye (baad me wapas laga sakte ho). " +
                    "PC ya dusre phone se ye command ek baar chalao:\n\n" + OWNER_CMD
            )
            .setPositiveButton("Copy command") { _, _ ->
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("cmd", OWNER_CMD))
                toast("Copy ho gaya")
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun changePin() {
        askPin("Purana PIN") { old ->
            if (checkPin(old)) {
                askPin("Naya PIN (4 se 8 digit)") { fresh ->
                    if (fresh.length < 4) {
                        toast("Kam se kam 4 digit chahiye")
                    } else {
                        PinStore.save(this, fresh)
                        toast("PIN badal gaya")
                    }
                }
            }
        }
    }

    private fun removeProtection() {
        if (!Policy.isOwner(this)) {
            toast("Device owner active nahi hai")
            return
        }
        askPin("PIN dalo") { pin ->
            if (checkPin(pin)) {
                dialog()
                    .setTitle("Protection hatani hai?")
                    .setMessage("Device owner hat jayega. Uninstall aur factory reset ke lock band ho jayenge.")
                    .setPositiveButton("Haan, hatao") { _, _ ->
                        if (Policy.removeOwner(this)) {
                            toast("Protection hat gayi")
                        } else {
                            toast("Hat nahi saki")
                        }
                        refresh()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun toggleGuard(on: Boolean): Boolean {
        if (on) {
            val ok = SettingsGuard.enable(this)
            if (ok) {
                toast("Original Settings band. Ab sirf is app ki rows se khulegi")
            } else {
                dialog()
                    .setTitle("Lock nahi laga")
                    .setMessage(
                        "Is phone par Original Settings suspend ya hide nahi ho payi.\n\n" +
                            SettingsGuard.lastDetail + "\n\nYe message bhej do."
                    )
                    .setPositiveButton("OK", null)
                    .show()
            }
            return ok
        }
        SettingsGuard.disable(this)
        return true
    }

    private fun pickMinutes() {
        val opts = intArrayOf(3, 5, 10, 15, 30)
        val names = opts.map { "$it min" }.toTypedArray()
        val cur = opts.indexOf(SettingsGuard.minutes(this)).coerceAtLeast(0)
        dialog()
            .setTitle("Settings zyada se zyada kitni der khuli rahe")
            .setSingleChoiceItems(names, cur) { d, which ->
                SettingsGuard.setMinutes(this, opts[which])
                d.dismiss()
                refresh()
            }
            .show()
    }

    // ---------------------------------------------------------------- dialogs

    private fun dialog(): AlertDialog.Builder =
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)

    private fun pinField(hint: String): EditText {
        val e = EditText(this)
        e.hint = hint
        e.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        e.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(8))
        e.setTextColor(white)
        e.setHintTextColor(gray)
        return e
    }

    private fun askPin(title: String, ok: (String) -> Unit) {
        val e = pinField("PIN")
        val box = FrameLayout(this)
        box.setPadding(dp(24), dp(8), dp(24), 0)
        box.addView(e, FrameLayout.LayoutParams(match, wrap))
        dialog()
            .setTitle(title)
            .setView(box)
            .setPositiveButton("OK") { _, _ -> ok(e.text.toString()) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---------------------------------------------------------------- view helpers

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun toast(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    }

    private fun label(s: String, size: Float, color: Int, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.text = s
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        t.setTextColor(color)
        if (bold) t.typeface = Typeface.DEFAULT_BOLD
        return t
    }

    private fun space(h: Int): View {
        val v = View(this)
        v.layoutParams = LinearLayout.LayoutParams(1, dp(h))
        return v
    }

    private fun section(s: String): View {
        val t = label(s, 13f, gray)
        t.setPadding(dp(12), 0, 0, dp(8))
        return t
    }

    private fun selectable(): Int {
        val v = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, v, true)
        return v.resourceId
    }

    private fun card(vararg rows: View): View {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        val g = GradientDrawable()
        g.setColor(cardColor)
        g.cornerRadius = dp(22).toFloat()
        c.background = g
        c.outlineProvider = ViewOutlineProvider.BACKGROUND
        c.clipToOutline = true
        for (r in rows) c.addView(r)
        return c
    }

    private fun tile(icon: Int, color: Int): View {
        val f = FrameLayout(this)
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(11).toFloat()
        f.background = g
        val iv = ImageView(this)
        iv.setImageResource(icon)
        f.addView(iv, FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER))
        f.layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
        return f
    }

    private fun row(icon: Int, color: Int, title: String, value: String?, click: () -> Unit): View {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.setPadding(dp(16), dp(13), dp(14), dp(13))
        r.setBackgroundResource(selectable())
        r.setOnClickListener { click() }
        r.addView(tile(icon, color))

        val lp = LinearLayout.LayoutParams(0, wrap, 1f)
        lp.leftMargin = dp(16)
        r.addView(label(title, 17f, white), lp)

        if (!value.isNullOrEmpty()) {
            val v = label(value, 15f, gray)
            v.setPadding(0, 0, dp(6), 0)
            r.addView(v)
        }
        r.addView(label("\u203A", 26f, gray))
        return r
    }

    private fun switchRow(
        icon: Int,
        color: Int,
        title: String,
        sub: String,
        on: Boolean,
        enabled: Boolean,
        change: (Boolean) -> Boolean
    ): View {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.setPadding(dp(16), dp(13), dp(14), dp(13))
        r.addView(tile(icon, color))

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.addView(label(title, 17f, white))
        col.addView(label(if (enabled) sub else "Pehle device owner banao", 13f, gray))
        val lp = LinearLayout.LayoutParams(0, wrap, 1f)
        lp.leftMargin = dp(16)
        lp.rightMargin = dp(8)
        r.addView(col, lp)

        val sw = Switch(this)
        sw.isChecked = on
        sw.isEnabled = enabled
        sw.setOnCheckedChangeListener { _, v ->
            if (!change(v)) toast("Ho nahi paya")
            refresh()
        }
        r.addView(sw)

        if (!enabled) r.alpha = 0.55f
        return r
    }
}
