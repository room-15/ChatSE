package me.shreyasr.chatse;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class App extends Application {

    public static final String PREF_EMAIL = "email";
    public static final String PREF_HAS_CREDS = "creds";

    public static final String EXTRA_ROOM_NUM = "room";
    public static final String EXTRA_SITE = "site";
    public static final String EXTRA_FKEY = "fkey";

    private static App inst;
    public static App get() { return inst; }

    @Override
    public void onCreate() {
        super.onCreate();
        inst = this;
    }

    private static SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(inst);
    }

    public static SharedPreferences getPrefs(Context context) {
        return getPrefs();
    }
}
