package com.telen.library.bugreporter

import android.app.Application
import android.content.Intent
import android.widget.Toast
import com.github.kaygenzo.bugreporter.BugReporter
import com.github.kaygenzo.bugreporter.ReportMethod
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable

class DemoApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        BugReporter.Builder()
                .setCompressionQuality(75)
                .setImagePreviewScale(0.3f)
                .setFields(null)
                .setEmail("developer@telen.fr")
                .setReportMethods(listOf(ReportMethod.SHAKE, ReportMethod.FLOATING_BUTTON))
                .setReportFloatingImage(R.drawable.images)
                .observeResult(object : Observer<Intent> {
                    override fun onSubscribe(d: Disposable?) {

                    }

                    override fun onNext(intent: Intent?) {
                        Toast.makeText(applicationContext, "Bug report result: ${intent?.getStringExtra(Intent.EXTRA_TEXT)}", Toast.LENGTH_SHORT).show()
                        //startActivity(Intent.createChooser(intent, getString(R.string.chooser_title)))
                        startActivity(intent)
                    }

                    override fun onError(e: Throwable?) {
                        e?.printStackTrace()
                    }

                    override fun onComplete() {

                    }

                })
                .build()
                .init(this)
    }
}