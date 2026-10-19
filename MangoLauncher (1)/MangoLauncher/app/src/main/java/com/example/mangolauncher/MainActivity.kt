package com.example.mangolauncher

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast

/**
 * A simple Google TV style home screen:
 *  - big clock and date
 *  - Favorites row (long-press / hold OK on an app to add or remove)
 *  - Apps row with every installed TV app
 *  - System row with a Settings shortcut
 * No ads, no recommendations, no external libraries.
 */
class MainActivity : Activity() {

    private class Entry(
        val pkg: String?,
        val label: String,
        val icon: Drawable?,
        val banner: Drawable?,
        val intent: Intent
    )

    private lateinit var content: LinearLayout
    private val prefs by lazy { getSharedPreferences("launcher", MODE_PRIVATE) }

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics
        ).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        scroll.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#0A0F1F"), Color.parseColor("#1B2748"))
        )
        scroll.isFillViewport = true
        scroll.isVerticalScrollBarEnabled = false

        content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(48), dp(28), dp(48), dp(28))
        scroll.addView(content)

        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    // The Home screen should not close when Back is pressed.
    @Deprecated("Home screen stays open")
    override fun onBackPressed() {
        // intentionally empty
    }

    private fun render() {
        content.removeAllViews()
        content.addView(header())

        val apps = loadApps()
        val favs = favorites()
        val favApps = apps.filter { favs.contains(it.pkg) }

        if (favApps.isNotEmpty()) {
            content.addView(row("Favorites", favApps.map { tile(it) }))
        }
        content.addView(row("Apps", apps.map { tile(it) }))

        val settings = Entry(
            null,
            "Settings",
            getDrawable(android.R.drawable.ic_menu_preferences),
            null,
            Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        content.addView(row("System", listOf(tile(settings))))

        content.requestFocus()
    }

    // ---------- data ----------

    private fun loadApps(): List<Entry> {
        val pm = packageManager
        val seen = HashSet<String>()
        val out = ArrayList<Entry>()

        // TV apps first, then ordinary launcher apps that have no TV entry.
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (ri in pm.queryIntentActivities(query, 0)) {
                val ai = ri.activityInfo
                if (ai.packageName == packageName) continue
                if (!seen.add(ai.packageName)) continue

                val launch = Intent(Intent.ACTION_MAIN)
                    .addCategory(category)
                    .setClassName(ai.packageName, ai.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

                out.add(
                    Entry(
                        ai.packageName,
                        ri.loadLabel(pm).toString(),
                        ri.loadIcon(pm),
                        ai.loadBanner(pm),
                        launch
                    )
                )
            }
        }
        return out.sortedBy { it.label.lowercase() }
    }

    private fun favorites(): Set<String> =
        prefs.getStringSet("fav", emptySet())?.toSet() ?: emptySet()

    private fun toggleFavorite(e: Entry) {
        val pkg = e.pkg ?: return
        val set = favorites().toMutableSet()
        val added = set.add(pkg)
        if (!added) set.remove(pkg)
        prefs.edit().putStringSet("fav", set).apply()
        Toast.makeText(
            this,
            if (added) "${e.label} added to Favorites" else "${e.label} removed from Favorites",
            Toast.LENGTH_SHORT
        ).show()
        render()
    }

    private fun launch(e: Entry) {
        try {
            startActivity(e.intent)
        } catch (ex: Exception) {
            Toast.makeText(this, "Can't open ${e.label}", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------- views ----------

    private fun header(): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(8), 0, 0, dp(8))

        val time = TextClock(this)
        time.format12Hour = "h:mm a"
        time.format24Hour = "HH:mm"
        time.setTextColor(Color.WHITE)
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 56f)
        time.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)

        val date = TextClock(this)
        date.format12Hour = "EEEE, d MMMM"
        date.format24Hour = "EEEE, d MMMM"
        date.setTextColor(Color.parseColor("#B8C2E0"))
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)

        box.addView(time)
        box.addView(date)
        return box
    }

    private fun row(title: String, tiles: List<View>): View {
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.VERTICAL
        wrap.clipChildren = false
        wrap.clipToPadding = false

        val heading = TextView(this)
        heading.text = title
        heading.setTextColor(Color.parseColor("#B8C2E0"))
        heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        heading.typeface = Typeface.DEFAULT_BOLD
        heading.setPadding(dp(8), dp(18), 0, dp(2))

        val strip = LinearLayout(this)
        strip.orientation = LinearLayout.HORIZONTAL
        strip.clipChildren = false
        tiles.forEach { strip.addView(it) }

        val hs = HorizontalScrollView(this)
        hs.isHorizontalScrollBarEnabled = false
        hs.clipChildren = false
        hs.clipToPadding = false
        hs.addView(strip)

        wrap.addView(heading)
        wrap.addView(hs)
        return wrap
    }

    private fun cardBackground(focused: Boolean): GradientDrawable {
        val d = GradientDrawable()
        d.cornerRadius = dp(16).toFloat()
        d.setColor(Color.parseColor(if (focused) "#3A4A7A" else "#14FFFFFF"))
        d.setStroke(if (focused) dp(3) else 0, Color.WHITE)
        return d
    }

    private fun tile(e: Entry): View {
        val isFav = e.pkg != null && favorites().contains(e.pkg)

        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.CENTER_HORIZONTAL
        card.setPadding(dp(8), dp(8), dp(8), dp(8))
        card.isFocusable = true
        card.isClickable = true
        card.isLongClickable = e.pkg != null
        card.background = cardBackground(false)

        val lp = LinearLayout.LayoutParams(dp(192), LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.setMargins(dp(8), dp(12), dp(8), dp(12))
        card.layoutParams = lp

        val img = ImageView(this)
        val hasBanner = e.banner != null
        img.setImageDrawable(e.banner ?: e.icon)
        if (hasBanner) {
            img.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            img.scaleType = ImageView.ScaleType.FIT_CENTER
            img.setPadding(dp(18), dp(10), dp(18), dp(10))
            img.setBackgroundColor(Color.parseColor("#26FFFFFF"))
        }
        img.clipToOutline = true
        img.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, dp(10).toFloat())
            }
        }
        card.addView(img, LinearLayout.LayoutParams(dp(176), dp(99)))

        val label = TextView(this)
        label.text = (if (isFav) "★ " else "") + e.label
        label.setTextColor(Color.WHITE)
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        label.maxLines = 1
        label.ellipsize = TextUtils.TruncateAt.END
        label.gravity = Gravity.CENTER
        label.setPadding(0, dp(8), 0, 0)
        card.addView(label, LinearLayout.LayoutParams(dp(176), LinearLayout.LayoutParams.WRAP_CONTENT))

        card.setOnFocusChangeListener { v, hasFocus ->
            v.background = cardBackground(hasFocus)
            v.translationZ = if (hasFocus) dp(8).toFloat() else 0f
            val s = if (hasFocus) 1.1f else 1f
            v.animate().scaleX(s).scaleY(s).setDuration(120).start()
        }
        card.setOnClickListener { launch(e) }
        if (e.pkg != null) {
            card.setOnLongClickListener {
                toggleFavorite(e)
                true
            }
        }
        return card
    }
}
