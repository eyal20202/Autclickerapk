package com.example.visibleautoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Color
import android.graphics.Path
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class ClickService : AccessibilityService() {
    companion object { var instance: ClickService? = null; private set }
    private lateinit var wm: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var target: TextView? = null
    private var controls: LinearLayout? = null
    private var targetParams: WindowManager.LayoutParams? = null
    private var controlParams: WindowManager.LayoutParams? = null
    private var running = false
    private var interval = 1000L
    private val diameter by lazy { dp(56) }
    private val clickLoop: Runnable by lazy { object : Runnable {
        override fun run() {
            if (!running) return
            val p = targetParams ?: return
            val cx = p.x + diameter / 2f
            val cy = p.y + diameter / 2f
            val path = Path().apply { moveTo(cx, cy) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 45))
                .build()
            target?.animate()?.cancel()
            target?.alpha = 1f
            target?.animate()?.alpha(0.28f)?.setDuration(130)?.withEndAction {
                target?.animate()?.alpha(0.8f)?.setDuration(130)?.start()
            }?.start()
            if (!dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (running) handler.postDelayed(clickLoop, interval)
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    stopClicking()
                }
            }, handler)) stopClicking()
        }
    } }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        serviceInfo = serviceInfo.apply { flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS }
        showOverlays()
    }
    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {}
    override fun onInterrupt() { stopClicking() }
    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && running) {
            if (event.action == KeyEvent.ACTION_DOWN) stopClicking()
            return true
        }
        return super.onKeyEvent(event)
    }
    private fun circle(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke(dp(3), Color.WHITE)
    }
    private fun params(width: Int, height: Int, x: Int, y: Int, flags: Int) =
        WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.LEFT; this.x = x; this.y = y }

    fun showOverlays() {
        if (!::wm.isInitialized) return
        if (target != null) return
        target = TextView(this).apply {
            text = "+"; textSize = 30f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            background = circle(Color.rgb(0, 171, 133)); alpha = 0.8f
        }
        targetParams = params(diameter, diameter, dp(120), dp(290), 0)
        wm.addView(target, targetParams)
        target?.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0; var startY = 0; var touchX = 0f; var touchY = 0f
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                if (running) return false
                val p = targetParams ?: return false
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { startX = p.x; startY = p.y; touchX = e.rawX; touchY = e.rawY; return true }
                    MotionEvent.ACTION_MOVE -> {
                        p.x = (startX + (e.rawX - touchX)).toInt().coerceAtLeast(0)
                        p.y = (startY + (e.rawY - touchY)).toInt().coerceAtLeast(0)
                        wm.updateViewLayout(v, p); return true
                    }
                    MotionEvent.ACTION_UP -> return true
                }
                return false
            }
        })
        controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(6), dp(4), dp(6), dp(4))
            setBackgroundColor(Color.argb(220, 31, 41, 55))
        }
        val start = Button(this).apply { text = "START"; setOnClickListener { startClicking() } }
        val stop = Button(this).apply { text = "STOP"; setOnClickListener { stopClicking() } }
        val hide = Button(this).apply { text = "HIDE"; setOnClickListener { stopClicking(); hideOverlays() } }
        controls?.addView(start); controls?.addView(stop); controls?.addView(hide)
        controlParams = params(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, dp(8), dp(90), 0)
        wm.addView(controls, controlParams)
    }
    private fun startClicking() {
        if (running) return
        interval = getSharedPreferences("prefs", MODE_PRIVATE).getLong("interval", 1000L).coerceIn(100, 3_600_000)
        running = true
        targetParams?.let { p ->
            p.flags = p.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            target?.let { wm.updateViewLayout(it, p) }
        }
        handler.post(clickLoop)
    }
    private fun stopClicking() {
        running = false
        handler.removeCallbacks(clickLoop)
        targetParams?.let { p ->
            p.flags = p.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            target?.let { wm.updateViewLayout(it, p) }
        }
        target?.animate()?.cancel(); target?.alpha = 0.8f
    }
    private fun hideOverlays() {
        target?.let { wm.removeView(it) }; controls?.let { wm.removeView(it) }
        target = null; controls = null; targetParams = null; controlParams = null
    }
    override fun onDestroy() {
        stopClicking()
        if (::wm.isInitialized) hideOverlays()
        instance = null
        super.onDestroy()
    }
}
