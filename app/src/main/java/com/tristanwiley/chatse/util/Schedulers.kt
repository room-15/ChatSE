package com.tristanwiley.chatse.util

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

fun ioScheduler() = Schedulers.io()

fun uiScheduler() = AndroidSchedulers.mainThread()