package com.moviepilot.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Main Activity - WebView that loads the MoviePilot web interface.
 * Handles authentication injection, back navigation, and native features.
 */
public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SharedPreferences prefs;
    private String serverUrl;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Fullscreen immersive mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        }

        prefs = getSharedPreferences(ConfigManager.PREF_NAME, Context.MODE_PRIVATE);

        // Get server URL from intent
        serverUrl = getIntent().getStringExtra("server_url");
        if (serverUrl == null || serverUrl.isEmpty()) {
            serverUrl = prefs.getString(ConfigManager.KEY_SERVER_URL, ConfigManager.DEFAULT_SERVER_URL);
        }

        progressBar = findViewById(R.id.progress_bar);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        webView = findViewById(R.id.web_view);

        // Configure WebView settings
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setUserAgentString(settings.getUserAgentString() + " MoviePilotApp/1.0");

        // Enable and persist Cookie so session survives app restarts
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        cookieManager.flush(); // Ensure cookies are written to disk

        // Register JS interface
        webView.addJavascriptInterface(new WebAppInterface(this, prefs), "AndroidApp");

        // Set WebView clients
        webView.setWebViewClient(new MoviePilotWebViewClient());
        webView.setWebChromeClient(new MoviePilotWebChromeClient());

        // Enable file download support
        settings.setAllowFileAccess(true);

        // Swipe to refresh
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_purple,
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light
        );
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webView.reload();
            }
        });

        // Check login status - if not logged in (detected by redirect to login), go back
        // Load the web app
        webView.loadUrl(serverUrl);
    }

    /**
     * Custom WebViewClient that handles:
     * - Loading progress
     * - URL navigation within the app
     * - Detecting logout (redirect to login page)
     */
    private class MoviePilotWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setEnabled(false);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setEnabled(true);
            swipeRefreshLayout.setRefreshing(false);

            // If we reached dashboard or any page other than login, mark as logged in
            if (!url.contains("login") && !url.endsWith(serverUrl) && !url.endsWith(serverUrl + "/")) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(ConfigManager.KEY_IS_LOGGED_IN, true);
                editor.apply();
                CookieManager.getInstance().flush();
            } else {
                // On login page - inject credentials if saved
                String username = prefs.getString(ConfigManager.KEY_USERNAME, "");
                String password = prefs.getString(ConfigManager.KEY_PASSWORD, "");
                boolean remember = prefs.getBoolean(ConfigManager.KEY_REMEMBER, true);

                if (!username.isEmpty() && remember && !password.isEmpty()) {
                    injectCredentials(view, username, password);
                }
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();

            // Handle external URLs (tel:, mailto:, etc.)
            if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("geo:")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            }

            // Allow all other URLs to load in WebView
            return false;
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            // Let all requests pass through
            return null;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            if (errorCode == HttpURLConnection.HTTP_NOT_FOUND ||
                    errorCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }

    /**
     * Inject username and password into the MoviePilot login form, then auto-submit.
     */
    private void injectCredentials(WebView view, String username, String password) {
        String js = "javascript:" +
                "(function() {" +
                "  try {" +
                "    var u = document.getElementById('username');" +
                "    var p = document.getElementById('password');" +
                "    if (u && p) {" +
                "      var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                "      setter.call(u, '" + escapeJs(username) + "');" +
                "      u.dispatchEvent(new Event('input', { bubbles: true }));" +
                "      u.dispatchEvent(new Event('change', { bubbles: true }));" +
                "      setter.call(p, '" + escapeJs(password) + "');" +
                "      p.dispatchEvent(new Event('input', { bubbles: true }));" +
                "      p.dispatchEvent(new Event('change', { bubbles: true }));" +
                "      setTimeout(function() {" +
                "        var btn = document.querySelector('button[type=submit]');" +
                "        if (!btn) btn = Array.from(document.querySelectorAll('button')).find(function(b) { return b.textContent.includes('登录'); });" +
                "        if (btn) btn.click();" +
                "      }, 800);" +
                "    }" +
                "  } catch(e) { console.log('Injection error: ' + e); }" +
                "})();";

        // Delay injection to ensure DOM is ready
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                view.evaluateJavascript(js, null);
            }
        }, 500);
    }

    /**
     * Escape special characters for JavaScript strings.
     */
    private String escapeJs(String str) {
        return str.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Custom WebChromeClient for progress bar and console logging.
     */
    private class MoviePilotWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (newProgress == 100) {
                progressBar.setVisibility(View.GONE);
            } else {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(newProgress);
            }
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            if (consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                android.util.Log.e("WebView", "Console: " + consoleMessage.message());
            }
            return true;
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            // Block new windows
            return false;
        }
    }

    /**
     * Handle back button for WebView navigation.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Handle back press with modern API.
     */
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            // At the root page, just exit the app (keep cookies/session alive)
            finishAffinity();
        }
    }

    /**
     * Logout: clear session and return to login activity.
     * Only called when session actually expires (server redirects to login).
     */
    private void logout() {
        // Clear login state
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(ConfigManager.KEY_IS_LOGGED_IN, false);
        editor.apply();

        // Clear cookies and WebView data
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();

        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();

        // Navigate back to login
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Save current session state (called on pause/stop) so we can restore next time.
     */
    private void saveSessionState() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(ConfigManager.KEY_IS_LOGGED_IN, true);
        editor.putString(ConfigManager.KEY_SERVER_URL, serverUrl);
        editor.apply();

        // Flush cookies to disk so they persist across app restarts
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
        saveSessionState();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
