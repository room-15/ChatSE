package com.tristanwiley.chatse.about.pokos

import android.view.View

/**
 * Created by mauker on 31/08/17.
 * Class that represents an icon, it carries a click listener.
 */
data class AboutIconPoko(val iconResource: Int, val message: String, val clickListener: View.OnClickListener)