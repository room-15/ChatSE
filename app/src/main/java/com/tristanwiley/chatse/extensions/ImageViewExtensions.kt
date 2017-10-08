package com.tristanwiley.chatse.extensions

import android.widget.ImageView
import com.koushikdutta.ion.Ion

fun ImageView.loadUrl(url: String) {
    Ion.with(context).load(url).intoImageView(this)
}