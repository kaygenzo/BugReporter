package com.github.kaygenzo.bugreporter.internal

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
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
import com.github.kaygenzo.bugreporter.screens.FieldType
import com.github.kaygenzo.bugreporter.screens.PaintActivity
import com.github.kaygenzo.bugreporter.service.FloatingWidgetService
import com.github.kaygenzo.bugreporter.utils.PermissionsUtils
import com.tarek360.instacapture.Instacapture
import com.tarek360.instacapture.listener.SimpleScreenCapturingListener
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.PublishSubject
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

internal object BugReporterImpl : BugReporter {

    val reportFields = mutableListOf<FieldType>()
    var compressionQuality = InternalConstants.DEFAULT_JPEG_COMPRESSION_QUALITY
    var previewScale = InternalConstants.DEFAULT_PREVIEW_SCALE
    var developerEmailAddress: String? = null
    val reportingMethods: MutableList<ReportMethod> = mutableListOf()
    val resultSubject = PublishSubject.create<Intent>()

    @DrawableRes
    var reportFloatingImage = R.drawable.ic_baseline_bug_report_24

    var activityTracker: Application.ActivityLifecycleCallbacks? = null
    var lifecycleListener: LifecycleObserver? = null
    var currentActivity: WeakReference<Activity>? = null

    private val debugTree = Timber.DebugTree()

    override fun askOverlayPermission(activity: Activity, requestCode: Int) {
        PermissionsUtils.askOverlayPermission(activity = activity, requestCode)
    }

    fun init(application: Application) {
        if (BuildConfig.DEBUG) {
            Timber.plant(debugTree)
        }
        initTrackers(application)
    }

    override fun startReport(activity: Activity) {
        Completable.defer {
            hideFloatingButton(activity)
            Completable.timer(500, TimeUnit.MILLISECONDS)
        }
            .andThen(captureScreen(activity))
            .flatMap { bitmap ->
                val width = bitmap.width
                val height = bitmap.height
                getScreenshotFile(context = activity, bitmap = bitmap)
                    .map {
                        val imagePath = it.absolutePath
                        BugReportActivity.getIntent(
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
        val dateFormat = SimpleDateFormat("dd-MM-yyyy_HH-mm-ss.SS", Locale.getDefault())
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

    private fun startReportingTool(applicationContext: Context) {
        val intent = getServiceIntent(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }
    }

    private fun stopReportingTool(applicationContext: Context) {
        applicationContext.stopService(getServiceIntent(applicationContext))
    }

    private fun showFloatingButton(context: Context) {
        val intent = getServiceIntent(context).apply {
            action = FloatingWidgetService.ACTION_EXIT_REPORT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun hideFloatingButton(context: Context) {
        val intent = getServiceIntent(context).apply {
            action = FloatingWidgetService.ACTION_ENTER_REPORT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun getServiceIntent(context: Context): Intent {
        return Intent(context, FloatingWidgetService::class.java)
    }

    internal fun getCurrentActivity(): Activity? {
        return currentActivity?.get()
    }

    private fun initTrackers(application: Application) {
        activityTracker = object : Application.ActivityLifecycleCallbacks {

            override fun onActivityResumed(activity: Activity) {
                Timber.d("onActivityResumed :: activity=$activity")
                val oldIsFromReportingTool =
                    currentActivity?.get() is PaintActivity
                            || currentActivity?.get() is BugReportActivity
                val newIsFromReportingTool =
                    activity is PaintActivity || activity is BugReportActivity
                currentActivity = WeakReference(activity)

                if (!oldIsFromReportingTool && newIsFromReportingTool) {
                    Timber.d("Hide floating button")
                    hideFloatingButton(application)
                } else if (oldIsFromReportingTool && !newIsFromReportingTool) {
                    Timber.d("Show floating button")
                    showFloatingButton(application)
                }
            }

            override fun onActivityPaused(activity: Activity) {
                Timber.d("onActivityPaused :: activity=$activity")
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        }

        lifecycleListener = object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun onMoveToForeground() {
                Timber.d("Returning to foreground…")
                startReportingTool(application)
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun onMoveToBackground() {
                Timber.d("Moving to background…")
                stopReportingTool(application)
            }
        }

        lifecycleListener?.let { ProcessLifecycleOwner.get().lifecycle.addObserver(it) }
        application.registerActivityLifecycleCallbacks(activityTracker)
    }
}