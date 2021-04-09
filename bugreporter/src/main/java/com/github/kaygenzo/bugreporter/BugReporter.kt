package com.github.kaygenzo.bugreporter

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.github.kaygenzo.bugreporter.screens.BugReportActivity
import com.github.kaygenzo.bugreporter.screens.FieldType
import com.github.kaygenzo.bugreporter.screens.PaintActivity
import com.github.kaygenzo.bugreporter.service.FloatingWidgetService
import com.github.kaygenzo.bugreporter.utils.PermissionsUtils
import com.tarek360.instacapture.Instacapture
import com.tarek360.instacapture.listener.SimpleScreenCapturingListener
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

enum class ReportMethod {
    SHAKE,
    FLOATING_BUTTON
}

object BugReporter {

    private const val TAG = "BugReporter"

    private const val SCREENSHOT_DIR = "screenshots"
    private const val DEFAULT_JPEG_COMPRESSION_QUALITY = 75
    private const val DEFAULT_PREVIEW_SCALE = 0.3f

    private val reportFields = mutableListOf<FieldType>()
    private var compressionQuality = DEFAULT_JPEG_COMPRESSION_QUALITY
    private var previewScale = DEFAULT_PREVIEW_SCALE

    private var activityTracker: Application.ActivityLifecycleCallbacks? = null
    private var lifecycleListener: LifecycleObserver? = null
    private var currentActivity: WeakReference<Activity>? = null
    var developerEmailAddress: String? = null
    val reportingMethods: MutableList<ReportMethod> = mutableListOf()
    private val debugTree: Timber.Tree = Timber.DebugTree()

    class Builder {

        fun setFields(fields: List<FieldType>?): Builder {
            reportFields.apply {
                clear()
                fields?.let {
                    addAll(it)
                } ?: run {
                    addAll(FieldType.values())
                }

            }
            return this
        }

        fun setCompressionQuality(compressionQuality: Int): Builder {
            BugReporter.compressionQuality = compressionQuality
            return this
        }

        fun setImagePreviewScale(scale: Float): Builder {
            previewScale = scale
            return this
        }

        fun setEmail(email: String): Builder {
            developerEmailAddress = email
            return this
        }

        fun setReportMethods(methods: List<ReportMethod>): Builder {
            reportingMethods.apply {
                clear()
                addAll(methods)
            }
            return this
        }

        fun build(): BugReporter {
            return BugReporter
        }
    }

    fun askOverlayPermission(activity: Activity, requestCode: Int) {
        PermissionsUtils.askOverlayPermission(activity = activity, requestCode)
    }

    fun init(application: Application) {
        if (BuildConfig.DEBUG) {
            Timber.plant(debugTree)
        }
        initTrackers(application)
    }

    fun startReport(activity: Activity) {
        hideFloatingButton(activity)
        Completable.timer(500, TimeUnit.MILLISECONDS).subscribe {
            Instacapture.capture(activity, object : SimpleScreenCapturingListener() {
                override fun onCaptureComplete(bitmap: Bitmap) {
                    val width = bitmap.width
                    val height = bitmap.height
                    getScreenshotFile(context = activity, bitmap = bitmap)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            activity.startActivity(
                                    BugReportActivity.getIntent(
                                            activity,
                                            it.absolutePath,
                                            width,
                                            height,
                                            previewScale,
                                            reportFields
                                    )
                            )
                        }, {
                            //TODO
                            it.printStackTrace()
                        })
                }
            })
        }
    }

    private fun getScreenshotFile(context: Context): File {
        //TODO use instant
        val dateFormat: DateFormat = SimpleDateFormat("dd-MM-yyyy_HH-mm-ss.SS", Locale.getDefault())
        val fileName = "screenshot-" + dateFormat.format(Date()) + ".jpg"
        val screenshotsDir = File(context.cacheDir, SCREENSHOT_DIR)
        screenshotsDir.mkdirs()
        return File(screenshotsDir, fileName)
    }

    private fun getScreenshotFile(context: Context, bitmap: Bitmap): Single<File> {
        return Single.create { emitter ->
            var outputStream: BufferedOutputStream? = null
            try {
                val screenshotFile: File = getScreenshotFile(context)
                outputStream = BufferedOutputStream(FileOutputStream(screenshotFile))
                bitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, outputStream)
                outputStream.flush()
                emitter.onSuccess(screenshotFile)
            }
            catch (e: IOException) {
                emitter.onError(e)
            }
            finally {
                try {
                    outputStream?.close()
                } catch (exception: IOException) {
                    //TODO
                    exception.printStackTrace()
                }
            }
        }
    }

    private fun startReportingTool(applicationContext: Context) {
        applicationContext.startService(getServiceIntent(applicationContext))
    }

    private fun stopReportingTool(applicationContext: Context) {
        applicationContext.stopService(getServiceIntent(applicationContext))
    }

    private fun showFloatingButton(context: Context) {
        val intent = getServiceIntent(context).apply {
            action = FloatingWidgetService.ACTION_EXIT_REPORT
        }
        context.startService(intent)
    }

    private fun hideFloatingButton(context: Context) {
        val intent = getServiceIntent(context).apply {
            action = FloatingWidgetService.ACTION_ENTER_REPORT
        }
        context.startService(intent)
    }

    private fun getServiceIntent(context: Context): Intent {
        return Intent(context, FloatingWidgetService::class.java)
    }

    fun getCurrentActivity(): Activity? {
        return currentActivity?.get()
    }

    private fun initTrackers(application: Application) {
        activityTracker = object: Application.ActivityLifecycleCallbacks {

            override fun onActivityResumed(activity: Activity) {
                Timber.d("onActivityResumed :: activity=$activity")
                val oldIsFromReportingTool = currentActivity?.get() is PaintActivity || currentActivity?.get() is BugReportActivity
                val newIsFromReportingTool = activity is PaintActivity || activity is BugReportActivity
                currentActivity = WeakReference(activity)

                if(!oldIsFromReportingTool && newIsFromReportingTool){
                    hideFloatingButton(application)
                }
                else if(oldIsFromReportingTool && !newIsFromReportingTool) {
                    showFloatingButton(application)
                }
            }

            override fun onActivityPaused(activity: Activity) {
                Timber.d("onActivityPaused :: activity=$activity")
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) { }
            override fun onActivityStarted(activity: Activity) { }
            override fun onActivityStopped(activity: Activity) { }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) { }
            override fun onActivityDestroyed(activity: Activity) { }
        }

        lifecycleListener = object: LifecycleObserver {
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