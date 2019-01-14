package com.tristanwiley.chatse.util

import android.app.Activity
import android.view.inputmethod.InputMethodManager

object Utilities {

    // hide soft keyboard
    fun hideKeyboard(act: Activity?) {
        if (act != null && act.currentFocus != null) {
            val inputMethodManager = act.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(act.currentFocus!!.windowToken, 0)
        }
    }

}