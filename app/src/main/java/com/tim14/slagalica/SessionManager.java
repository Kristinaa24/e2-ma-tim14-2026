package com.tim14.slagalica;

import android.content.Context;
import android.content.SharedPreferences;

import com.tim14.slagalica.model.User;

public class SessionManager {

    private static final String PREF_NAME = "slagalica_session";
    private static final String KEY_UID = "uid";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_LOGGED_IN = "logged_in";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveLogin(String uid, String email, String username) {
        prefs.edit()
                .putString(KEY_UID, uid)
                .putString(KEY_EMAIL, email)
                .putString(KEY_USERNAME, username)
                .putBoolean(KEY_LOGGED_IN, true)
                .apply();
    }

    public void saveUser(User user) {
        if (user == null) {
            return;
        }

        saveLogin(user.id, user.email, user.username);
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public String getUid() {
        return prefs.getString(KEY_UID, null);
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    public void logout() {
        prefs.edit().clear().apply();
    }
}
