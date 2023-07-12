package com.github.kaygenzo.bugreporter.utils

import android.content.Context
import android.content.Intent

fun Context.service(intent: Intent) {
    startService(intent)
}