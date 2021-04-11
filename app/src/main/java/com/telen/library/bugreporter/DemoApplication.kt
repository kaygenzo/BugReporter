package com.telen.library.bugreporter

import android.app.Application
import com.github.kaygenzo.bugreporter.BugReporter
import com.github.kaygenzo.bugreporter.ReportMethod
import com.github.kaygenzo.bugreporter.ReportResult

class DemoApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        BugReporter.Builder()
            .setCompressionQuality(75)
            .setImagePreviewScale(0.3f)
            .setFields(null)
            .setEmail("developer@telen.fr")
            .setReportMethods(listOf(ReportMethod.SHAKE, ReportMethod.FLOATING_BUTTON))
            .build()
            .init(this)
    }
}