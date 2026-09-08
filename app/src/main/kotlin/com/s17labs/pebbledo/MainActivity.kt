package com.s17labs.pebbledo

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

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
    // Wrapper around the WebView. Static safe-area padding goes here — NOT on
    // the WebView itself, whose web content ignores View padding.
    private lateinit var rootContainer: FrameLayout

    // Set once the page has finished loading. Until then, back presses
    // exit immediately instead of dispatching into a not-yet-ready page.
    internal var pageReady = false

    private lateinit var backCallback: OnBackPressedCallback

    // Current theme background (int) + whether a web dialog is open. The
    // native status/nav strips are painted from these so they dim together
    // with the web dialog overlay (see setScrim).
    private var themeBg: Int = Color.parseColor("#F0F2F5")
    private var scrimOn: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restore the background saved from the web app's last theme so the
        // launch frame matches the selected theme — no default-theme flash.
        val startBg = loadSavedBackground()
        themeBg = startBg

        // Draw behind system bars for edge-to-edge display.
        // Static safe areas are applied as container padding in setupEdgeToEdge(),
        // so the web content needs no env(safe-area-inset-*) handling.
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
                    // Insets aren't dispatched on initial load — request them so
                    // the container gets its safe-area padding below.
                    rootContainer.requestApplyInsets()
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

        rootContainer = FrameLayout(this).apply {
            // Same theme color so the safe-area strips blend in.
            setBackgroundColor(startBg)
            addView(
                webView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

        setContentView(rootContainer)
        setupEdgeToEdge()
        setupBackNavigation()
    }

    /**
     * Edge-to-edge safe areas + keyboard signal.
     *
     * The app draws behind the status/nav bars (setDecorFitsSystemWindows(false)).
     * Static safe areas (systemBars/displayCutout) are applied as padding on the
     * wrapper container — NOT on the WebView, whose web content ignores View
     * padding — so they are correct from the very first frame: no flash of the
     * top bar under the status bar while the page loads. The handled types are
     * zeroed before passing insets on, so the WebView doesn't apply them a
     * second time via CSS env(safe-area-inset-*).
     *
     * The keyboard is the exception: old WebViews with edge-to-edge give the page
     * NO web signal at all (no layout resize, no visual-viewport resize — the
     * keyboard just overlays). So the native IME inset is forwarded as
     * window.__nativeKb (CSS px) with a nudge to re-scroll / blur. Newer WebViews
     * will ALSO fire visualViewport resizes — both write the same padding value,
     * so no double. IME insets pass through unmodified so the WebView's own
     * visual-viewport handling keeps working.
     */
    private fun setupEdgeToEdge() {
        var lastImeDp = 0f
        ViewCompat.setOnApplyWindowInsetsListener(rootContainer) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            v.setPadding(
                maxOf(bars.left, cutout.left),
                maxOf(bars.top, cutout.top),
                maxOf(bars.right, cutout.right),
                maxOf(bars.bottom, cutout.bottom)
            )
            val density = resources.displayMetrics.density
            val imeDp = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom / density
            val imeStr = if (imeDp == imeDp.toInt().toFloat()) imeDp.toInt().toString() else "%.2f".format(imeDp)
            val prevStr = if (lastImeDp == lastImeDp.toInt().toFloat()) lastImeDp.toInt().toString() else "%.2f".format(lastImeDp)
            val js = "window.__nativeKb=$imeStr;" +
                "(function(){var kb=$imeStr,prev=$prevStr;" +
                "if(kb>80){var r=document.querySelector('.row.editing');if(r&&window.keepEditVisible)keepEditVisible(r);}" +
                "else if(prev>80){var l=document.getElementById('tList');if(l)l.style.paddingBottom='';" +
                "var ta=document.querySelector('.inp');if(ta)ta.blur();}})();"
            webView.evaluateJavascript(js, null)
            lastImeDp = imeDp
            WindowInsetsCompat.Builder(insets)
                .setInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
                    Insets.NONE
                )
                .build()
        }
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
            themeBg = Color.parseColor(color)
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_BACKGROUND, color)
                .apply()
            renderBackground()
        } catch (_: IllegalArgumentException) {
        }
    }

    /**
     * Dim or undim the native status/nav strips while a web dialog is open.
     * The web overlay (.ov) can't paint outside the WebView, so the shell dims
     * its own background to the exact same value (see dimForScrim).
     */
    internal fun setScrim(on: Boolean) {
        scrimOn = on
        renderBackground()
    }

    private fun renderBackground() {
        val c = if (scrimOn) dimForScrim(themeBg) else themeBg
        webView.setBackgroundColor(c)
        rootContainer.setBackgroundColor(c)
        window.setBackgroundDrawable(ColorDrawable(c))
    }

    /** Match the web dialog overlay rgba(0,0,0,.6): out = src * (1 - .6). */
    private fun dimForScrim(color: Int): Int {
        val f = 0.4f
        return Color.rgb(
            ((color shr 16 and 0xFF) * f).toInt(),
            ((color shr 8 and 0xFF) * f).toInt(),
            ((color and 0xFF) * f).toInt()
        )
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
