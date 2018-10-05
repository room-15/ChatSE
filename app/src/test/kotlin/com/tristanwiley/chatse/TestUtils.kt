package com.tristanwiley.chatse

import android.content.SharedPreferences
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever

fun getMockPrefs(): SharedPreferences {
    val prefs = mock<SharedPreferences>()
    val editor = object : SharedPreferences.Editor {
        override fun clear() = this

        override fun putLong(key: String?, value: Long) = this

        override fun putInt(key: String?, value: Int) = this

        override fun remove(key: String?) = this

        override fun putBoolean(key: String?, value: Boolean) = this

        override fun putStringSet(key: String?, values: MutableSet<String>?) = this

        override fun commit() = false

        override fun putFloat(key: String?, value: Float) = this

        override fun apply() = Unit

        override fun putString(key: String?, value: String?) = this

    }

    whenever(prefs.edit()).doReturn(editor)
    return prefs
}
