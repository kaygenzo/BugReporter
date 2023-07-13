package com.github.kaygenzo.bugreporter.internal.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat

internal object PermissionsUtils {
    fun hasPermissionOverlay(context: Context): Boolean {
        return !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context))
    }

    @SuppressLint("InlinedApi")
    fun askOverlayPermission(activity: Activity, requestCode: Int) {
        if (!hasPermissionOverlay(activity)) {
            //If the draw over permission is not available open the settings screen
            //to grant the permission.
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.packageName))
            ActivityCompat.startActivityForResult(activity, intent, requestCode, null)
        }
    }
}