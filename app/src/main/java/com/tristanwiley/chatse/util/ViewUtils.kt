package com.tristanwiley.chatse.util

import android.view.View

fun View.show() = apply {
    visibility = View.VISIBLE
}

fun View.hide() = apply {
    visibility = View.GONE
}
