package com.noir.autoclicker

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(Bridge(), "Android")
        webView.loadUrl("file:///android_asset/index.html")
        setContentView(webView)
    }

    override fun onResume() {
        super.onResume()
        // Let the page re-check permission status whenever the user comes back
        // from the Accessibility / Overlay settings screens.
        webView.evaluateJavascript("if(window.onPermissionsMaybeChanged) window.onPermissionsMaybeChanged();", null)
    }

    inner class Bridge {

        @JavascriptInterface
        fun isAccessibilityEnabled(): Boolean {
            val expected = "$packageName/${ClickerAccessibilityService::class.java.name}"
            val enabled = Settings.Secure.getString(
                contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            return enabled.split(":").any { it.equals(expected, ignoreCase = true) }
        }

        @JavascriptInterface
        fun openAccessibilitySettings() {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        @JavascriptInterface
        fun hasOverlayPermission(): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this@MainActivity)

        @JavascriptInterface
        fun requestOverlayPermission() {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        @JavascriptInterface
        fun startOverlay(mode: String, cps: Int, text: String, loop: Boolean) {
            val i = Intent(this@MainActivity, OverlayService::class.java)
            i.putExtra("mode", mode)
            i.putExtra("cps", cps)
            i.putExtra("text", text)
            i.putExtra("loop", loop)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
        }

        @JavascriptInterface
        fun stopOverlay() {
            stopService(Intent(this@MainActivity, OverlayService::class.java))
        }
    }
}
