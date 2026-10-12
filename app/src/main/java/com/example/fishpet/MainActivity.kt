package com.example.fishpet

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import kotlin.math.max

class MainActivity : Activity() {
    private lateinit var fish: AquariumPreviewView
    private lateinit var info: TextView
    private lateinit var feed: Button
    private lateinit var rename: Button
    private lateinit var sound: Switch
    private var deathTonePlayed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val padding = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(padding, padding, padding, padding)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(4, 30, 58), Color.rgb(6, 67, 103), Color.rgb(4, 27, 55)),
            )
        }
        val title = TextView(this).apply {
            text = "MheyFish · Live Aquarium"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val subtitle = TextView(this).apply {
            text = "Your widget pet, swimming live"
            textSize = 14f
            setTextColor(Color.rgb(176, 231, 246))
            gravity = Gravity.CENTER
        }
        fish = AquariumPreviewView(this).apply { contentDescription = "Animated preview of your virtual aquarium" }
        info = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.rgb(213, 244, 255))
            gravity = Gravity.CENTER
            setLineSpacing(5f, 1f)
            setPadding(padding / 2, padding / 2, padding / 2, padding / 2)
            background = GradientDrawable().apply {
                setColor(Color.argb(118, 5, 35, 61))
                cornerRadius = 18f * resources.displayMetrics.density
                setStroke((1 * resources.displayMetrics.density).toInt(), Color.argb(105, 142, 222, 242))
            }
        }
        feed = Button(this).apply { text = "Feed" }
        rename = Button(this).apply { text = "Rename Fish" }
        val reset = Button(this).apply { text = "Start a New Baby Fish" }
        sound = Switch(this).apply {
            text = "Sound effects"
            setTextColor(Color.rgb(213, 244, 255))
            isChecked = FishSoundSettings.isEnabled(this@MainActivity)
        }

        feed.setOnClickListener { redraw(FishWidget.feedAndRefresh(this).state, refreshWidget = false) }
        rename.setOnClickListener { showRenameDialog() }
        reset.setOnClickListener {
            val fresh = FishStore.reset(this)
            FishSounds.play(this, FishSoundEffect.HATCH)
            redraw(fresh)
        }
        sound.setOnCheckedChangeListener { _, enabled -> FishSoundSettings.setEnabled(this, enabled) }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))
        root.addView(subtitle, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = (8 * resources.displayMetrics.density).toInt() })
        root.addView(fish, LinearLayout.LayoutParams(-1, (310 * resources.displayMetrics.density).toInt()))
        root.addView(info, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = (8 * resources.displayMetrics.density).toInt() })
        root.addView(sound, LinearLayout.LayoutParams(-2, -2))
        root.addView(feed, LinearLayout.LayoutParams(-2, -2))
        root.addView(rename, LinearLayout.LayoutParams(-2, -2))
        root.addView(reset, LinearLayout.LayoutParams(-2, -2))
        setContentView(root)
        redraw()
    }

    override fun onResume() {
        super.onResume()
        if (::fish.isInitialized) redraw()
    }

    private fun showRenameDialog() {
        val current = FishStore.refresh(this)
        val input = EditText(this).apply {
            setText(current.name)
            selectAll()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            hint = "Fish name"
        }
        AlertDialog.Builder(this)
            .setTitle("Rename ${current.name}")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ -> redraw(FishStore.rename(this, input.text.toString())) }
            .show()
    }

    private fun redraw(state: FishState = FishStore.refresh(this), refreshWidget: Boolean = true) {
        val now = System.currentTimeMillis() / 1000L
        fish.fishState = state
        info.text = buildString {
            append("${state.name} · ${state.stage().label} ${state.color.label}\n")
            append("${state.mood.label} · Age ${ageText(state.ageSeconds(now))} · Growth ${state.growth.toInt()}%\n")
            append("Hunger ${state.hungerStatus.label} ${state.hunger.toInt()}%  •  Health ${state.health.toInt()}%\n")
            append("Meals served ${state.foodEaten}\n\n")
            append(if (state.dead) "${state.name} has passed on. Start a new baby fish when you are ready." else "The live view keeps swimming while this screen is open. Feed in either place to sync the same pet.")
            append("\n\nWidget: long-press home screen → Widgets → MheyFish. Tap the fish for a short reaction.")
        }
        feed.isEnabled = !state.dead
        feed.text = if (state.dead) "${state.name} is resting" else "Feed ${state.name}"
        rename.text = "Rename ${state.name}"
        if (state.dead && !deathTonePlayed) {
            FishSounds.play(this, FishSoundEffect.DEATH)
            deathTonePlayed = true
        } else if (!state.dead) {
            deathTonePlayed = false
        }
        sound.isChecked = FishSoundSettings.isEnabled(this)
        if (refreshWidget) FishWidget.refresh(this)
    }

    private fun ageText(seconds: Long): String = when {
        seconds >= 86_400L -> "${seconds / 86_400L} days"
        seconds >= 3_600L -> "${seconds / 3_600L} hours"
        else -> "${max(0L, seconds / 60L)} minutes"
    }
}
