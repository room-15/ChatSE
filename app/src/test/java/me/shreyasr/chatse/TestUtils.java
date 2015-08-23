package me.shreyasr.chatse;

import android.content.SharedPreferences;

import org.mockito.Mockito;

import java.util.Set;

import static org.mockito.Mockito.mock;

public class TestUtils {

    public static SharedPreferences getMockPrefs() {
        SharedPreferences prefs = mock(SharedPreferences.class);
        SharedPreferences.Editor editor = new SharedPreferences.Editor() {
            @Override public SharedPreferences.Editor putString(String key, String value) { return this; }
            @Override public SharedPreferences.Editor putStringSet(String key, Set<String> values) { return this; }
            @Override public SharedPreferences.Editor putInt(String key, int value) { return this; }
            @Override public SharedPreferences.Editor putLong(String key, long value) { return this; }
            @Override public SharedPreferences.Editor putFloat(String key, float value) { return this; }
            @Override public SharedPreferences.Editor putBoolean(String key, boolean value) { return this; }
            @Override public SharedPreferences.Editor remove(String key) { return this; }
            @Override public SharedPreferences.Editor clear() { return this; }
            @Override public boolean commit() { return false; }
            @Override public void apply() { }
        };
        Mockito.doReturn(editor).when(prefs).edit();
        return prefs;
    }
}
