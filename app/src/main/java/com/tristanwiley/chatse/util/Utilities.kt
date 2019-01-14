package com.tristanwiley.chatse.util

import android.app.Activity
import android.view.inputmethod.InputMethodManager
import org.jetbrains.anko.act


     // extension function to hide soft keyboard
    fun Activity.hideKeyboard() {
        if (this.currentFocus != null) {
            val inputMethodManager = act.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(act.currentFocus!!.windowToken, 0)
        }
    }
