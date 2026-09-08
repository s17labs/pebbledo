package com.s17labs.pebbledo

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
    // Edge-to-edge chrome: thin strips above/below the WebView painting the
    // safe areas. The top strip matches the web top bar (--nb) so the header
    // reads as one continuous surface; the bottom strip matches the page bg.
    private lateinit var rootContainer: LinearLayout
    private lateinit var topStrip: View
    private lateinit var bottomStrip: View

    // Set once the page has finished loading. Until then, back presses
    // exit immediately instead of dispatching into a not-yet-ready page.
    internal var pageReady = false

    private lateinit var backCallback: OnBackPressedCallback

    // Current theme colors (ints). The native strips are painted from these so
    // the header stays continuous with the web top bar.
    private var themeBg: Int = Color.parseColor("#F0F2F5")
    private var themeNav: Int = Color.parseColor("#e4e7ec")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restore the background saved from the web app's last theme so the
        // launch frame matches the selected theme — no default-theme flash.
        val startBg = loadSavedBackground()
        themeBg = startBg
        themeNav = loadSavedNav()

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

        rootContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // Page bg behind everything; the strips cover the safe areas.
            setBackgroundColor(startBg)
        }
        topStrip = View(this).apply { setBackgroundColor(themeNav) }
        bottomStrip = View(this).apply { setBackgroundColor(startBg) }
        rootContainer.addView(
            topStrip,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
        )
        rootContainer.addView(
            webView,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )
        rootContainer.addView(
            bottomStrip,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
        )

        setContentView(rootContainer)
        setupEdgeToEdge()
        setupBackNavigation()
    }

    /**
     * Edge-to-edge safe areas + keyboard signal.
     *
     * The app draws behind the status/nav bars (setDecorFitsSystemWindows(false)).
     * Static safe areas (systemBars/displayCutout) size the top/bottom strips
     * and the container's side padding — plain Views, so they are correct from
     * the very first frame: no flash of the top bar under the status bar while
     * the page loads. The handled types are zeroed before passing insets on,
     * so the WebView doesn't apply them a second time via
     * CSS env(safe-area-inset-*).
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
                0,
                maxOf(bars.right, cutout.right),
                0
            )
            setStripHeight(topStrip, maxOf(bars.top, cutout.top))
            setStripHeight(bottomStrip, maxOf(bars.bottom, cutout.bottom))
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

    /** Resize a safe-area strip; no-op when the height is unchanged. */
    private fun setStripHeight(strip: View, px: Int) {
        val lp = strip.layoutParams
        if (lp.height != px) {
            lp.height = px
            strip.layoutParams = lp
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
     * Update the native chrome to match the current web theme and persist it,
     * so the next launch starts with the same colors and no default-theme
     * flash is visible. The top strip follows the web top bar color so the
     * header reads as one continuous surface.
     */
    internal fun applyBackground(color: String, nav: String) {
        var changed = false
        try {
            themeBg = Color.parseColor(color)
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_BACKGROUND, color)
                .apply()
            changed = true
        } catch (_: IllegalArgumentException) {
        }
        if (nav.isNotEmpty()) {
            try {
                themeNav = Color.parseColor(nav)
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(KEY_NAV, nav)
                    .apply()
                changed = true
            } catch (_: IllegalArgumentException) {
            }
        }
        if (changed) renderBackground()
    }

    private fun renderBackground() {
        webView.setBackgroundColor(themeBg)
        window.setBackgroundDrawable(ColorDrawable(themeBg))
        rootContainer.setBackgroundColor(themeBg)
        topStrip.setBackgroundColor(themeNav)
        bottomStrip.setBackgroundColor(themeBg)
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

    /** Top-bar color saved by the web app on a previous run (or the default). */
    private fun loadSavedNav(): Int {
        val saved = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_NAV, null)
        return try {
            Color.parseColor(saved ?: DEFAULT_NAV)
        } catch (_: IllegalArgumentException) {
            Color.parseColor(DEFAULT_NAV)
        }
    }

    companion object {
        private const val DEFAULT_BACKGROUND = "#F0F2F5" // slate theme bg
        private const val DEFAULT_NAV = "#e4e7ec" // slate theme top bar
        private const val PREFS_NAME = "webshell"
        private const val KEY_BACKGROUND = "background"
        private const val KEY_NAV = "nav"
    }
}
