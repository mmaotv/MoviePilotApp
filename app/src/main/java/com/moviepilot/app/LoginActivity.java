package com.moviepilot.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Login Activity - Custom login page with server URL, username, and password fields.
 * Credentials are saved locally for auto-fill on subsequent launches.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etServerUrl;
    private EditText etUsername;
    private EditText etPassword;
    private CheckBox cbRemember;
    private Button btnLogin;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Soft keyboard adjustment
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        prefs = getSharedPreferences(ConfigManager.PREF_NAME, Context.MODE_PRIVATE);

        etServerUrl = findViewById(R.id.et_server_url);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        cbRemember = findViewById(R.id.cb_remember);
        btnLogin = findViewById(R.id.btn_login);

        // Load saved credentials
        loadCredentials();

        // Check if already logged in
        if (prefs.getBoolean(ConfigManager.KEY_IS_LOGGED_IN, false)) {
            String serverUrl = prefs.getString(ConfigManager.KEY_SERVER_URL, ConfigManager.DEFAULT_SERVER_URL);
            if (!serverUrl.isEmpty()) {
                launchMainActivity(serverUrl);
                return;
            }
        }

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });
    }

    private void loadCredentials() {
        String serverUrl = prefs.getString(ConfigManager.KEY_SERVER_URL, ConfigManager.DEFAULT_SERVER_URL);
        String username = prefs.getString(ConfigManager.KEY_USERNAME, "");
        boolean remember = prefs.getBoolean(ConfigManager.KEY_REMEMBER, true);

        etServerUrl.setText(serverUrl);
        etUsername.setText(username);
        cbRemember.setChecked(remember);

        if (remember) {
            String password = prefs.getString(ConfigManager.KEY_PASSWORD, "");
            etPassword.setText(password);
        }
    }

    private void performLogin() {
        String serverUrl = etServerUrl.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();
        boolean remember = cbRemember.isChecked();

        // Validate inputs
        if (serverUrl.isEmpty()) {
            Toast.makeText(this, "请输入服务器地址", Toast.LENGTH_SHORT).show();
            etServerUrl.requestFocus();
            return;
        }

        if (username.isEmpty()) {
            Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show();
            etUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
            etPassword.requestFocus();
            return;
        }

        // Normalize server URL (remove trailing slash)
        if (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }

        // Save credentials and login state
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(ConfigManager.KEY_SERVER_URL, serverUrl);
        editor.putString(ConfigManager.KEY_USERNAME, username);
        editor.putBoolean(ConfigManager.KEY_IS_LOGGED_IN, true);
        if (remember) {
            editor.putString(ConfigManager.KEY_PASSWORD, password);
        } else {
            editor.remove(ConfigManager.KEY_PASSWORD);
        }
        editor.putBoolean(ConfigManager.KEY_REMEMBER, remember);
        editor.apply();

        // Launch main activity
        launchMainActivity(serverUrl);
    }

    private void launchMainActivity(String serverUrl) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("server_url", serverUrl);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Exit app on back press from login screen
        super.onBackPressed();
    }
}
