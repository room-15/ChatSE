package me.shreyasr.chatse.util

import android.util.Log

/**
 * Utility class for logging.
 */
object Logger {

    /**
     * Log an exception.

     * @param source  The class the exception came from.
     * *
     * @param message A message to give context on the exception.
     * *
     * @param e       The exception (or throwable) itself.
     */
    fun exception(source: Class<*>, message: String, e: Throwable) {
        Log.e(source.simpleName, message, e)
    }

    /**
     * Log a message or event.

     * @param source  The class the message came from.
     * *
     * @param message The content of the message.
     */
    fun message(source: Class<*>, message: String) {
        Log.d(source.simpleName, message)
    }

    fun event(source: Class<*>, message: String) {
        Log.i(source.simpleName, message)
    }

    fun debug(source: Class<*>, message: String) {
        Log.d(source.simpleName, message)
    }
}
