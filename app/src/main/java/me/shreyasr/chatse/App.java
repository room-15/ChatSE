package me.shreyasr.chatse;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class App extends Application {

    public static final String PREF_EMAIL = "email";

    private static App inst;
    public static App get() { return inst; }

    @Override
    public void onCreate() {
        super.onCreate();
        inst = this;
        Log.d("ayyy", "inst");
    }

    public static SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(inst);
    }
}
