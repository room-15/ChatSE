package com.tristanwiley.chatse.extensions

import android.view.View

fun View.show() = apply {
    visibility = View.VISIBLE
}

fun View.hide() = apply {
    visibility = View.GONE
}

inline fun View.showIf(predicate: () -> Boolean) {
    visibility = if (predicate.invoke()) View.VISIBLE else View.GONE
}