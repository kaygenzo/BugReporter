package com.github.kaygenzo.bugreporter.internal

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.github.kaygenzo.bugreporter.BuildConfig
import com.github.kaygenzo.bugreporter.R
import com.github.kaygenzo.bugreporter.api.BugReporter
import com.github.kaygenzo.bugreporter.api.ReportMethod
import com.github.kaygenzo.bugreporter.screens.BugReportActivity
import com.github.kaygenzo.bugreporter.screens.BugReportFormActivity
import com.github.kaygenzo.bugreporter.screens.FieldType
import com.github.kaygenzo.bugreporter.service.FloatingWidgetService
import com.github.kaygenzo.bugreporter.utils.PermissionsUtils
import com.github.kaygenzo.bugreporter.utils.service
import com.tarek360.instacapture.Instacapture
import com.tarek360.instacapture.listener.SimpleScreenCapturingListener
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

internal object BugReporterImpl : BugReporter, Application.ActivityLifecycleCallbacks,
    LifecycleObserver {

    val reportFields = mutableListOf<FieldType>()
    var compressionQuality = InternalConstants.DEFAULT_JPEG_COMPRESSION_QUALITY
    var previewScale = InternalConstants.DEFAULT_PREVIEW_SCALE
    var developerEmailAddress: String? = null
    val reportingMethods: MutableList<ReportMethod> = mutableListOf()

    @DrawableRes
    var reportFloatingImage = R.drawable.ic_baseline_bug_report_24

    private var currentActivity: WeakReference<Activity>? = null
    private var application: WeakReference<Application>? = null
    private val disposables = CompositeDisposable()

    val resultSubject: Subject<Intent> = PublishSubject.create()
    private val activityResumedSubject: Subject<Activity> = PublishSubject.create()
    private val foregroundSubject: Subject<Boolean> = PublishSubject.create()

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy_HH-mm-ss.SS", Locale.getDefault())

    private val debugTree = Timber.DebugTree()
    private var isDisabled = false

    override fun askOverlayPermission(activity: Activity, requestCode: Int) {
        PermissionsUtils.askOverlayPermission(activity = activity, requestCode)
    }

    override fun setReportMethods(methods: List<ReportMethod>) {
        reportingMethods.clear()
        reportingMethods.addAll(methods)
    }

    fun init(application: Application) {
        if (BuildConfig.DEBUG) {
            Timber.plant(debugTree)
        }
        this.application = WeakReference(application)
        application.registerActivityLifecycleCallbacks(this)
        start()
    }

    override fun disable() {
        isDisabled = true
        disposables.clear()
        application?.get()?.let { showFloatingButton(it, false) }

    }

    override fun release() {
        this.application?.get()?.run {
            showFloatingButton(this, false)
            unregisterActivityLifecycleCallbacks(this@BugReporterImpl)
        }
        this.application = null
        stop()
    }

    private fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        application?.get()?.let { registerActivityListener(it) }
    }

    private fun stop() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        unregisterActivityListener()
    }

    override fun restart() {
        stop()
        start()
        isDisabled = false
        refresh()
    }

    override fun refresh() {
        application?.get()?.let { showFloatingButton(it, true) }
    }

    override fun startReport(activity: Activity) {
        if (isDisabled) {
            return
        }
        disposables.add(
            //Add some delay to not take the floating button in screenshot
            Completable.fromAction { showFloatingButton(activity, false) }
                .andThen(Completable.timer(500, TimeUnit.MILLISECONDS))
                .andThen(captureScreen(activity))
                .flatMap { bitmap ->
                    val width = bitmap.width
                    val height = bitmap.height
                    getScreenshotFile(context = activity, bitmap = bitmap)
                        .map {
                            val imagePath = it.absolutePath
                            BugReportFormActivity.getIntent(
                                activity,
                                imagePath,
                                width,
                                height,
                                previewScale,
                                reportFields
                            )
                        }
                }.subscribe({
                    activity.startActivity(it)
                }, {
                    Timber.e(it)
                })
        )
    }

    private fun captureScreen(activity: Activity): Single<Bitmap> {
        return Single.create { emitter ->
            Instacapture.capture(activity, object : SimpleScreenCapturingListener() {
                override fun onCaptureComplete(bitmap: Bitmap) {
                    emitter.onSuccess(bitmap)
                }

                override fun onCaptureFailed(e: Throwable) {
                    emitter.onError(e)
                }
            })
        }
    }

    private fun getScreenshotFile(context: Context): File {
        //TODO use instant
        val fileName = "screenshot-" + dateFormat.format(Date()) + ".jpg"
        val screenshotsDir = File(context.cacheDir, InternalConstants.SCREENSHOT_DIR)
        screenshotsDir.mkdirs()
        return File(screenshotsDir, fileName)
    }

    private fun getScreenshotFile(context: Context, bitmap: Bitmap): Single<File> {
        return Single.create { emitter ->
            try {
                val screenshotFile: File = getScreenshotFile(context)
                BufferedOutputStream(FileOutputStream(screenshotFile)).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, outputStream)
                    outputStream.flush()
                }
                emitter.onSuccess(screenshotFile)
            } catch (e: IOException) {
                emitter.onError(e)
            }
        }
    }

    internal fun getCurrentActivity(): Activity? {
        return currentActivity?.get()
    }

    private fun registerActivityListener(context: Context) {
        disposables.clear()
        disposables.add(activityResumedSubject.subscribe({
            currentActivity = WeakReference(it)
            showFloatingButton(context, it !is BugReportActivity)
        }, {
            Timber.e(it)
        }))
        disposables.add(foregroundSubject.subscribe({ movedToForeground ->
            setReportingTool(context, movedToForeground)
        }, {
            Timber.e(it)
        }))
    }

    private fun unregisterActivityListener() {
        disposables.clear()
    }

    override fun onActivityResumed(activity: Activity) {
        activityResumedSubject.onNext(activity)
    }

    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        foregroundSubject.onNext(true)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onMoveToBackground() {
        foregroundSubject.onNext(false)
    }

    private fun setReportingTool(context: Context, enabled: Boolean) {
        if (enabled) {
            if (isDisabled) {
                return
            }
            Timber.d("startReportingTool")
            context.service(getServiceIntent(context))
        } else {
            Timber.d("stopReportingTool")
            context.stopService(getServiceIntent(context))
        }
    }

    private fun showFloatingButton(context: Context, show: Boolean) {
        if (!show) {
            Timber.d("Hide floating button")
            val intent = getServiceIntent(context).apply {
                action = FloatingWidgetService.ACTION_STOP
            }
            context.service(intent)
        } else {
            if (isDisabled) {
                return
            }
            Timber.d("Show floating button")
            val intent = getServiceIntent(context).apply {
                action = FloatingWidgetService.ACTION_START
            }
            context.service(intent)
        }
    }

    private fun getServiceIntent(context: Context): Intent {
        return Intent(context, FloatingWidgetService::class.java)
    }
}