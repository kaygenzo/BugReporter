package com.github.kaygenzo.bugreporter.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import com.github.kaygenzo.bugreporter.UnsupportedSensorException
import com.github.kaygenzo.bugreporter.api.ReportMethod
import com.github.kaygenzo.bugreporter.databinding.FloatingWidgetBinding
import com.github.kaygenzo.bugreporter.internal.BugReporterImpl
import com.github.kaygenzo.bugreporter.shake.OnShakeListener
import com.github.kaygenzo.bugreporter.shake.ShakeDetectorKotlin
import com.github.kaygenzo.bugreporter.utils.PermissionsUtils
import kotlinx.android.synthetic.main.floating_widget.view.*
import timber.log.Timber

internal class FloatingWidgetService : Service(), OnShakeListener {

    companion object {
        const val ACTION_ENTER_REPORT = "com.telen.library.action.ENTER_REPORT"
        const val ACTION_EXIT_REPORT = "com.telen.library.action.EXIT_REPORT"
    }

    private val mLock = Any()
    private var shakeDisabled = false
    private val shakeDetector: ShakeDetectorKotlin? by lazy {
        try {
            ShakeDetectorKotlin.create(this, this)
        } catch (e: UnsupportedSensorException) {
            Timber.e(e)
            null
        }
    }
    private lateinit var binding: FloatingWidgetBinding

    private val params: WindowManager.LayoutParams

    init {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            //Specify the view position
            gravity = Gravity.TOP or Gravity.START //Initially view will be added to top-left corner
            x = 0
            y = 100
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        binding = FloatingWidgetBinding.inflate(LayoutInflater.from(this), null, false)
        binding.fabHead.setImageResource(BugReporterImpl.reportFloatingImage)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasFloatingButtonMethod() && !hasShakeMethod()) {
            stopSelf()
            return START_NOT_STICKY
        }

        intent?.action.run {
            when (this) {
                ACTION_ENTER_REPORT -> {
                    hideFloatingButton()
                    synchronized(mLock) {
                        shakeDisabled = true
                    }
                }

                ACTION_EXIT_REPORT -> {
                    showFloatingButton()
                    synchronized(mLock) {
                        shakeDisabled = false
                    }
                }

                else -> {
                    Timber.d("Not managed action $this ...")
                    if (hasFloatingButtonMethod()) {
                        setFloatingWidget()
                    }
                    if (hasShakeMethod()) {
                        shakeDetector?.start()
                    } else {
                        shakeDetector?.stop()
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (hasFloatingButtonMethod()) {
            removeFloatingWidget()
        }
        if (hasShakeMethod()) {
            shakeDetector?.stop()
        }
    }

    private fun hasShakeMethod(): Boolean {
        return BugReporterImpl.reportingMethods.contains(ReportMethod.SHAKE)
    }

    private fun hasFloatingButtonMethod(): Boolean {
        return BugReporterImpl.reportingMethods.contains(ReportMethod.FLOATING_BUTTON)
    }

    override fun onShake() {
        synchronized(mLock) {
            if (!shakeDisabled) {
                shakeDisabled = true
                Timber.d("OnShake!")
                launchReport()
            }
        }
    }

    private fun hideFloatingButton() {
        binding.floatingWidgetRoot.visibility = View.GONE
    }

    private fun showFloatingButton() {
        if (hasFloatingButtonMethod()) {
            binding.floatingWidgetRoot.visibility = View.VISIBLE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setFloatingWidget() {
        if (PermissionsUtils.hasPermissionOverlay(this)) {
            (getSystemService(WINDOW_SERVICE) as? WindowManager)?.let { windowManager ->
                windowManager.addView(binding.root, params)
                val listener = FloatingButtonTouchListener(windowManager, binding, params) {
                    launchReport()
                }
                binding.floatingWidgetRoot.setOnTouchListener(listener)
            }
        }
    }

    private fun removeFloatingWidget() {
        if (PermissionsUtils.hasPermissionOverlay(this)) {
            (getSystemService(WINDOW_SERVICE) as? WindowManager)?.removeView(binding.root)
        }
    }

    private fun launchReport() {
        BugReporterImpl.getCurrentActivity()?.let { activity ->
            BugReporterImpl.startReport(activity)
        } ?: run {
            Timber.e("No activity found to start report")
        }
    }
}