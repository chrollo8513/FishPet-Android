package com.example.fishpet

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(32,32,32,32); setBackgroundColor(Color.rgb(13,38,60)) }
        val title = TextView(this).apply { text = "FishPet 🐠"; textSize = 28f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        val fish = ImageView(this)
        val info = TextView(this).apply { textSize = 17f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        fun redraw() { val state = FishStore.update(this); fish.setImageBitmap(FishArt.render(state)); info.text = "${state.stage} • ${state.mood}\nHunger ${state.hunger.toInt()}% • Health ${state.health.toInt()}%\n\nLong-press your home screen → Widgets → FishPet"; FishWidget.refresh(this) }
        val feed = Button(this).apply { text = "Feed"; setOnClickListener { FishStore.update(this@MainActivity,feed=true); redraw() } }
        val reset = Button(this).apply { text = "Start New Fish (Reset)"; setOnClickListener { FishStore.update(this@MainActivity,reset=true); redraw() } }
        root.addView(title); root.addView(fish,LinearLayout.LayoutParams(-1,280)); root.addView(info); root.addView(feed); root.addView(reset)
        setContentView(root); redraw()
    }
}
