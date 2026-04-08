package com.moviepilot.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * JavaScript Interface for MoviePilot WebView.
 * Provides native Android features to the web application.
 */
public class WebAppInterface {
    private static final String TAG = "WebAppInterface";
    private final Context context;
    private final SharedPreferences prefs;
    private final ExecutorService executor;

    public WebAppInterface(Context context, SharedPreferences prefs) {
        this.context = context;
        this.prefs = prefs;
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Get app version info
     */
    @JavascriptInterface
    public String getAppVersion() {
        try {
            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            JSONObject json = new JSONObject();
            json.put("versionName", info.versionName);
            json.put("versionCode", info.versionCode);
            json.put("packageName", info.packageName);
            return json.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Get device info
     */
    @JavascriptInterface
    public String getDeviceInfo() {
        try {
            JSONObject json = new JSONObject();
            json.put("brand", Build.BRAND);
            json.put("model", Build.MODEL);
            json.put("device", Build.DEVICE);
            json.put("sdk", Build.VERSION.SDK_INT);
            json.put("release", Build.VERSION.RELEASE);
            return json.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Get saved credentials (called from JS if needed)
     */
    @JavascriptInterface
    public String getCredentials() {
        try {
            JSONObject json = new JSONObject();
            json.put("serverUrl", prefs.getString(ConfigManager.KEY_SERVER_URL, ConfigManager.DEFAULT_SERVER_URL));
            json.put("username", prefs.getString(ConfigManager.KEY_USERNAME, ""));
            boolean remember = prefs.getBoolean(ConfigManager.KEY_REMEMBER, true);
            if (remember) {
                json.put("password", prefs.getString(ConfigManager.KEY_PASSWORD, ""));
            } else {
                json.put("password", "");
            }
            return json.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Save credentials after login
     */
    @JavascriptInterface
    public void saveCredentials(String serverUrl, String username, String password, boolean remember) {
        SharedPreferences.Editor editor = prefs.edit();
        if (serverUrl != null && !serverUrl.isEmpty()) {
            editor.putString(ConfigManager.KEY_SERVER_URL, serverUrl);
        }
        editor.putString(ConfigManager.KEY_USERNAME, username != null ? username : "");
        if (remember) {
            editor.putString(ConfigManager.KEY_PASSWORD, password != null ? password : "");
        } else {
            editor.remove(ConfigManager.KEY_PASSWORD);
        }
        editor.putBoolean(ConfigManager.KEY_REMEMBER, remember);
        editor.putBoolean(ConfigManager.KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    /**
     * Clear login state (logout)
     */
    @JavascriptInterface
    public void logout() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(ConfigManager.KEY_IS_LOGGED_IN, false);
        // Clear cookies
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        editor.apply();
    }

    /**
     * Check if logged in
     */
    @JavascriptInterface
    public boolean isLoggedIn() {
        return prefs.getBoolean(ConfigManager.KEY_IS_LOGGED_IN, false);
    }

    /**
     * Get cookies as string
     */
    @JavascriptInterface
    public String getCookies() {
        return CookieManager.getInstance().getCookie(
                prefs.getString(ConfigManager.KEY_SERVER_URL, ConfigManager.DEFAULT_SERVER_URL)
        );
    }

    /**
     * Vibrate device (haptic feedback)
     */
    @JavascriptInterface
    public void vibrate(int milliseconds) {
        try {
            android.os.Vibrator v = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                v.vibrate(milliseconds);
            }
        } catch (Exception e) {
            Log.e(TAG, "Vibrate failed", e);
        }
    }
}
