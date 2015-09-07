package me.shreyasr.chatse.util;

import android.util.Log;

/**
 * Utility class for logging.
 */
public class Logger {

    /**
     * Log an exception.
     *
     * @param source The class the exception came from.
     * @param message A message to give context on the exception.
     * @param e The exception (or throwable) itself.
     */
    public static void exception(Class source, String message, Throwable e) {
        Log.e(source.getSimpleName(), message, e);
    }

    /**
     * Log a message or event.
     *
     * @param source The class the message came from.
     * @param message The content of the message.
     */
    public static void message(Class source, String message) {
        Log.d(source.getSimpleName(), message);
    }
}
