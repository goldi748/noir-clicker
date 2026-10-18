package com.noir.autoclicker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat

/**
 * Draws two small floating controls on top of whatever app is open:
 *  - a gold dot you drag to the exact spot you want tapped (Single mode),
 *  - a pill bubble that starts/stops the automation.
 * The actual tap/type calls are delegated to ClickerAccessibilityService.
 */
class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private var targetView: View? = null
    private var controlView: TextView? = null
    private var targetParams: WindowManager.LayoutParams? = null

    private var running = false
    private val handler = Handler(Looper.getMainLooper())
    private var tickRunnable: Runnable? = null

    private var mode = "single"
    private var cps = 20
    private var text = ""
    private var loop = true
    private var typedSoFar = StringBuilder()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mode = intent?.getStringExtra("mode") ?: "single"
        cps = intent?.getIntExtra("cps", 20) ?: 20
        text = intent?.getStringExtra("text") ?: ""
        loop = intent?.getBooleanExtra("loop", true) ?: true

        startForegroundWithNotification()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (targetView == null) addTargetReticle()
        if (controlView == null) addControlBubble()
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val channelId = "noir_clicker"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            mgr.createNotificationChannel(
                NotificationChannel(channelId, "Noir Clicker", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Noir Clicker is running")
            .setContentText("Drag the gold dot, then tap the pill to start/stop")
            .setSmallIcon(android.R.drawable.presence_online)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }

    private fun addTargetReticle() {
        val dot = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#D4AF37"))
                setStroke(3, Color.BLACK)
            }
        }
        val size = 56
        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 400
        params.y = 800
        targetParams = params

        dot.setOnTouchListener(object : View.OnTouchListener {
            var dx = 0f
            var dy = 0f
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dx = params.x - e.rawX
                        dy = params.y - e.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = (e.rawX + dx).toInt()
                        params.y = (e.rawY + dy).toInt()
                        wm.updateViewLayout(dot, params)
                    }
                }
                return true
            }
        })
        wm.addView(dot, params)
        targetView = dot
    }

    private fun addControlBubble() {
        val bubble = TextView(this).apply {
            text = "\u25B6"
            textSize = 18f
            setTextColor(Color.parseColor("#1A1406"))
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#D4AF37"))
            }
        }
        val size = 72
        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 40
        params.y = 800

        bubble.setOnTouchListener(object : View.OnTouchListener {
            var dx = 0f
            var dy = 0f
            var moved = false
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dx = params.x - e.rawX
                        dy = params.y - e.rawY
                        moved = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = (e.rawX + dx).toInt()
                        params.y = (e.rawY + dy).toInt()
                        wm.updateViewLayout(bubble, params)
                        moved = true
                    }
                    MotionEvent.ACTION_UP -> if (!moved) toggle(bubble)
                }
                return true
            }
        })
        wm.addView(bubble, params)
        controlView = bubble
    }

    private fun toggle(bubble: TextView) {
        running = !running
        bubble.text = if (running) "\u25A0" else "\u25B6"
        if (running) startLoop() else stopLoop()
    }

    private fun startLoop() {
        val delay = (1000 / cps.coerceIn(1, 100)).coerceAtLeast(8).toLong()
        typedSoFar = StringBuilder()
        tickRunnable = object : Runnable {
            override fun run() {
                if (!running) return
                val svc = ClickerAccessibilityService.instance
                if (svc != null) {
                    if (mode == "single") {
                        val p = targetParams
                        if (p != null) svc.tap((p.x + 28).toFloat(), (p.y + 28).toFloat())
                    } else {
                        if (typedSoFar.length < text.length) {
                            typedSoFar.append(text[typedSoFar.length])
                        } else if (loop) {
                            typedSoFar = StringBuilder()
                        }
                        svc.typeIntoFocusedField(typedSoFar.toString())
                    }
                }
                handler.postDelayed(this, delay)
            }
        }
        handler.post(tickRunnable!!)
    }

    private fun stopLoop() {
        tickRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLoop()
        if (::wm.isInitialized) {
            targetView?.let { wm.removeView(it) }
            controlView?.let { wm.removeView(it) }
        }
    }
}
