package com.cahue.iweco.login;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Francesco on 27/02/2015.
 */
public class AuthUtils {

    public static final String PREF_SKIPPED_LOGGED_IN = "PREF_SKIPPED_LOGGED_IN";

    public static void setSkippedLogin(Context context, boolean skipped) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PREF_SKIPPED_LOGGED_IN, skipped).apply();
    }

    public static boolean isSkippedLogin(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_SKIPPED_LOGGED_IN, false);
    }

}
