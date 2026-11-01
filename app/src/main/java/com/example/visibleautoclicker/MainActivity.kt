package com.example.visibleautoclicker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scale = resources.displayMetrics.density
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24*scale).toInt(), (32*scale).toInt(), (24*scale).toInt(), 0)
            gravity = Gravity.TOP
        }
        val heading = TextView(this).apply { text = "Visible Auto Clicker"; textSize = 24f }
        val description = TextView(this).apply { text = "1. Enable the accessibility service.\n2. Drag the green circle to the target.\n3. Tap START on the floating control.\n4. Tap STOP or Volume Down to stop.\nThe circle pulses on each tap."; textSize = 16f }
        val input = EditText(this).apply {
            hint = "Interval in milliseconds (e.g. 1000)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(getSharedPreferences("prefs", MODE_PRIVATE).getLong("interval", 1000L).toString())
        }
        val save = Button(this).apply {
            text = "Save interval"
            setOnClickListener {
                val interval = input.text.toString().toLongOrNull()
                if (interval == null || interval < 100 || interval > 3_600_000) {
                    Toast.makeText(this@MainActivity, "Enter 100–3600000 ms", Toast.LENGTH_LONG).show()
                } else {
                    getSharedPreferences("prefs", MODE_PRIVATE).edit().putLong("interval", interval).apply()
                    Toast.makeText(this@MainActivity, "Saved. Applied on next START", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val enable = Button(this).apply {
            text = "Open Accessibility Settings"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        val show = Button(this).apply {
            text = "Show floating controls"
            setOnClickListener {
                val service = ClickService.instance
                if (service == null) Toast.makeText(this@MainActivity, "Enable the service first", Toast.LENGTH_LONG).show()
                else { service.showOverlays(); moveTaskToBack(true) }
            }
        }
        listOf(heading, description, input, save, enable, show).forEach { layout.addView(it) }
        setContentView(layout)
    }
}
