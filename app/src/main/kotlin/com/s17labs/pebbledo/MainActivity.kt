package com.s17labs.pebbledo

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

/**
 * WebShell — MainActivity
 *
 * The single Activity that hosts your web app in a full-screen WebView.
 *
 * You generally don't need to modify this file. The two files you'll
 * actually work with are:
 *   - assets/www/   → your HTML/CSS/JS app
 *   - NativeBridge.kt → native API methods callable from JS
 */
class MainActivity : AppCompatActivity() {

    // Exposed so NativeBridge can call evaluateJavascript on it.
    // Used when native code needs to push data/events back to JS.
    internal lateinit var webView: WebView
    private lateinit var bridge: NativeBridge

    // Set once the page has finished loading. Until then, back presses
    // exit immediately instead of dispatching into a not-yet-ready page.
    internal var pageReady = false

    private lateinit var backCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restore the background saved from the web app's last theme so the
        // launch frame matches the selected theme — no default-theme flash.
        val startBg = loadSavedBackground()

        // Draw behind system bars for edge-to-edge display.
        // In your CSS, use env(safe-area-inset-*) to add padding.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawable(ColorDrawable(startBg))

        bridge = NativeBridge(this)

        webView = WebView(this).apply {
            settings.apply {
                // Required: your web app won't work without this
                javaScriptEnabled = true

                // Enables localStorage and sessionStorage
                domStorageEnabled = true

                // file:///android_asset works regardless; this only governs
                // the broader file system, so keep it off for security
                allowFileAccess = false

                // Optional: disable zoom controls for a more app-like feel
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
            }

            // Attach the native bridge.
            // Accessible in JS as window.Native.*
            addJavascriptInterface(bridge, "Native")

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val url = request.url
                    val scheme = url.scheme ?: return true // block if no scheme

                    // Only allow local asset navigation inside the WebView
                    if (scheme == "file") return false

                    // For safe external schemes, open in the system browser
                    if (scheme == "https" || scheme == "http" || scheme == "mailto" || scheme == "tel") {
                        bridge.openUrl(url.toString())
                    }
                    // All other schemes (intent://, javascript:, market://, etc.) are silently blocked
                    return true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    pageReady = true
                }
            }

            // Match the restored theme background so the first drawn frame
            // already has the right color. The web app keeps this in sync at
            // runtime via Native.emit("bg", {color}).
            setBackgroundColor(startBg)

            // Enable Chrome DevTools inspection in debug builds.
            // NEVER ship with this enabled in production.
            if (BuildConfig.DEBUG) {
                WebView.setWebContentsDebuggingEnabled(true)
            }

            // Load the entry point of your web app.
            loadUrl("file:///android_asset/www/index.html")
        }

        setContentView(webView)
        setupBackNavigation()
    }

    /**
     * Hardware/gesture back button.
     *
     * The web app is a single-page app with internal UI state (dialogs,
     * selection mode, archive view), so before leaving we hand the press
     * to JavaScript via the __bridge_backButton event. If there is nothing
     * left to close, JS calls Native.emit("exit") which lands in
     * requestExitApp() below.
     */
    private fun setupBackNavigation() {
        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!pageReady) {
                    requestExitApp()
                    return
                }
                webView.evaluateJavascript(
                    "if(window.__bridge_backButton)window.__bridge_backButton();",
                    null
                )
            }
        }
        onBackPressedDispatcher.addCallback(this, backCallback)
    }

    /** Exit the app — invoked from JS via Native.emit("exit"). */
    internal fun requestExitApp() {
        backCallback.isEnabled = false
        onBackPressedDispatcher.onBackPressed()
    }

    /**
     * Update the WebView + window background to match the current web theme
     * and persist it, so the next launch starts with the same color and no
     * default-theme flash is visible.
     */
    internal fun applyBackground(color: String) {
        try {
            val c = Color.parseColor(color)
            webView.setBackgroundColor(c)
            window.setBackgroundDrawable(ColorDrawable(c))
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_BACKGROUND, color)
                .apply()
        } catch (_: IllegalArgumentException) {
        }
    }

    /** Background color saved by the web app on a previous run (or the default). */
    private fun loadSavedBackground(): Int {
        val saved = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_BACKGROUND, null)
        return try {
            Color.parseColor(saved ?: DEFAULT_BACKGROUND)
        } catch (_: IllegalArgumentException) {
            Color.parseColor(DEFAULT_BACKGROUND)
        }
    }

    companion object {
        private const val DEFAULT_BACKGROUND = "#F0F2F5" // slate theme bg
        private const val PREFS_NAME = "webshell"
        private const val KEY_BACKGROUND = "background"
    }
}
