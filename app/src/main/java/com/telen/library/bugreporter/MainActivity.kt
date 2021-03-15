package com.telen.library.bugreporter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        reportBug.setOnClickListener {
            BugReporter.Builder()
                .setCompressionQuality(75)
                .setImagePreviewScale(0.3f)
                .setFields(listOf(
                    FieldType.DATE_TIME,
                    FieldType.MANUFACTURER,
                    FieldType.BRAND,
                    FieldType.MODEL,
                    FieldType.APP_VERSION,
                    FieldType.ANDROID_VERSION,
                    FieldType.LOCALE,
                    FieldType.BT_STATUS,
                    FieldType.WIFI_STATUS,
                    FieldType.NETWORK_STATUS,
                    FieldType.SCREEN_DENSITY,
                    FieldType.SCREEN_RESOLUTION,
                    FieldType.ORIENTATION,
                    FieldType.BATTERY_STATUS
                ))
                .build()
                .startReport(this)
        }
    }
}