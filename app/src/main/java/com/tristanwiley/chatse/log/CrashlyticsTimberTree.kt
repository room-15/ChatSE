package com.tristanwiley.chatse.log

import android.util.Log
import android.util.Log.ASSERT
import android.util.Log.WARN
import android.util.Log.ERROR
import android.util.Log.INFO
import com.crashlytics.android.Crashlytics
import timber.log.Timber

/**
 * Created by mauker.
 * This class will handle the Timber logs and send the WARN, ERROR and ASSERT logs directly
 * to the crashlytics.
 */
object CrashlyticsTimberTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        when(priority) {
            WARN, ERROR, ASSERT ->  {
                Crashlytics.log(message)
                Crashlytics.logException(t)
            }
            INFO -> Log.println(priority, tag, message)
        }
    }
}
