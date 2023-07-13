package com.github.kaygenzo.bugreporter.internal.utils

import android.content.Context
import android.content.Intent

internal fun Context.service(intent: Intent) {
    startService(intent)
}