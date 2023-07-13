package com.telen.library.bugreporter

import android.app.Application
import android.content.Intent
import android.widget.Toast
import com.github.kaygenzo.bugreporter.api.BugReporter
import com.github.kaygenzo.bugreporter.api.ReportMethod
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable

class DemoApplication : Application(), Observer<Intent> {

    lateinit var reporter: BugReporter

    override fun onCreate() {
        super.onCreate()

        reporter = BugReporter.Builder()
            .setCompressionQuality(75)
            .setImagePreviewScale(0.3f)
            .setFields(null)
            .setEmail("developer@telen.fr")
            .setReportFloatingImage(R.drawable.images)
            .observeResult(this)
            .setDebug(false)
            .build(this)
    }

    override fun onSubscribe(d: Disposable) {}

    override fun onNext(intent: Intent) {
        Toast.makeText(
            applicationContext,
            "Bug report result: ${intent.getStringExtra(Intent.EXTRA_TEXT)}",
            Toast.LENGTH_SHORT
        ).show()
        startActivity(intent)
    }

    override fun onError(e: Throwable) {
        e.printStackTrace()
    }

    override fun onComplete() {}
}