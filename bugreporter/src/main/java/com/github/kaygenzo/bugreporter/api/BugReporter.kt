package com.github.kaygenzo.bugreporter.api

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.annotation.DrawableRes
import com.github.kaygenzo.bugreporter.R
import com.github.kaygenzo.bugreporter.internal.BugReporterImpl
import com.github.kaygenzo.bugreporter.internal.InternalConstants
import com.github.kaygenzo.bugreporter.screens.FieldType
import io.reactivex.rxjava3.core.Observer
import java.lang.ref.WeakReference

interface BugReporter {

    fun startReport(activity: Activity)
    fun askOverlayPermission(activity: Activity, requestCode: Int)

    class Builder {
        private val reportFields = mutableListOf<FieldType>()
        private var compressionQuality = InternalConstants.DEFAULT_JPEG_COMPRESSION_QUALITY
        private var previewScale = InternalConstants.DEFAULT_PREVIEW_SCALE

        private var developerEmailAddress: String? = null
        private val reportingMethods: MutableList<ReportMethod> = mutableListOf()
        private var resultObserver: Observer<Intent>? = null
        private var application: WeakReference<Application?> = WeakReference(null)

        @DrawableRes
        private var reportFloatingImage = R.drawable.ic_baseline_bug_report_24

        fun setFields(fields: List<FieldType>?): Builder {
            this.reportFields.apply {
                clear()
                addAll(fields ?: FieldType.values().toList())
            }
            return this
        }

        fun setCompressionQuality(compressionQuality: Int): Builder {
            this.compressionQuality = compressionQuality
            return this
        }

        fun setImagePreviewScale(scale: Float): Builder {
            previewScale = scale
            return this
        }

        fun setEmail(email: String): Builder {
            this.developerEmailAddress = email
            return this
        }

        fun setReportMethods(methods: List<ReportMethod>): Builder {
            this.reportingMethods.apply {
                clear()
                addAll(methods)
            }
            return this
        }

        fun observeResult(observer: Observer<Intent>): Builder {
            this.resultObserver = observer
            return this
        }

        fun setReportFloatingImage(@DrawableRes image: Int): Builder {
            this.reportFloatingImage = image
            return this
        }

        fun setApplication(application: Application): Builder {
            this.application = WeakReference(application)
            return this
        }

        @Throws(IllegalArgumentException::class)
        fun build(): BugReporter {
            return BugReporterImpl.also { reporter ->
                application.get()?.let {
                    reporter.init(it)
                } ?: throw IllegalArgumentException("Application must be set")
                reporter.reportFields.addAll(reportFields)
                reporter.compressionQuality = compressionQuality
                reporter.previewScale = previewScale
                reporter.reportingMethods.addAll(reportingMethods)
                reporter.developerEmailAddress = developerEmailAddress
                resultObserver?.let { reporter.resultSubject.subscribe(it) }
            }
        }
    }
}