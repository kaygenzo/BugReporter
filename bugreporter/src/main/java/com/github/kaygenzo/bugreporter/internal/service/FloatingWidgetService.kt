package com.github.kaygenzo.bugreporter.internal.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import com.github.kaygenzo.bugreporter.api.ReportMethod
import com.github.kaygenzo.bugreporter.api.UnsupportedSensorException
import com.github.kaygenzo.bugreporter.databinding.FloatingWidgetBinding
import com.github.kaygenzo.bugreporter.internal.BugReporterImpl
import com.github.kaygenzo.bugreporter.internal.shake.OnShakeListener
import com.github.kaygenzo.bugreporter.internal.shake.ShakeDetectorKotlin
import com.github.kaygenzo.bugreporter.internal.utils.PermissionsUtils
import kotlinx.android.synthetic.main.floating_widget.view.*
import timber.log.Timber

internal class FloatingWidgetService : Service(), OnShakeListener {

    companion object {
        const val ACTION_STOP = "com.telen.library.action.ACTION_STOP"
        const val ACTION_START = "com.telen.library.action.ACTION_START"
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
        setFloatingWidget()
        shakeDetector?.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasFloatingButtonMethod() && !hasShakeMethod()) {
            stopSelf()
            return START_NOT_STICKY
        }

        intent?.action.run {
            when (this) {
                ACTION_STOP -> {
                    hideFloatingButton()
                    synchronized(mLock) {
                        shakeDisabled = true
                    }
                }

                ACTION_START -> {
                    if(hasFloatingButtonMethod()) {
                        showFloatingButton()
                    } else {
                        hideFloatingButton()
                    }
                    synchronized(mLock) {
                        shakeDisabled = !hasShakeMethod()
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingWidget()
        shakeDetector?.stop()
    }

    private fun hasShakeMethod(): Boolean {
        val enabled = BugReporterImpl.reportingMethods.contains(ReportMethod.SHAKE)
        Timber.d("hasShakeMethod: $enabled")
        return enabled
    }

    private fun hasFloatingButtonMethod(): Boolean {
        val enabled = BugReporterImpl.reportingMethods.contains(ReportMethod.FLOATING_BUTTON)
        Timber.d("hasFloatingButtonMethod: $enabled")
        return enabled
    }

    override fun onShake() {
        if(hasShakeMethod()) {
            synchronized(mLock) {
                if (!shakeDisabled) {
                    shakeDisabled = true
                    Timber.d("OnShake!")
                    launchReport()
                }
            }
        }
    }

    private fun hideFloatingButton() {
        Timber.d("hideFloatingButton")
        binding.floatingWidgetRoot.visibility = View.GONE
    }

    private fun showFloatingButton() {
        Timber.d("showFloatingButton")
        if (hasFloatingButtonMethod()) {
            binding.floatingWidgetRoot.visibility = View.VISIBLE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setFloatingWidget() {
        val granted = PermissionsUtils.hasPermissionOverlay(this)
        if (granted) {
            (getSystemService(WINDOW_SERVICE) as? WindowManager)?.let { windowManager ->
                binding.floatingWidgetRoot.visibility = View.INVISIBLE
                windowManager.addView(binding.root, params)
                val listener = FloatingButtonTouchListener(windowManager, binding, params) {
                    if(hasFloatingButtonMethod()) {
                        launchReport()
                    }
                }
                binding.floatingWidgetRoot.setOnTouchListener(listener)
            }
        }
    }

    private fun removeFloatingWidget() {
        if (PermissionsUtils.hasPermissionOverlay(this)) {
            try {
                (getSystemService(WINDOW_SERVICE) as? WindowManager)?.removeView(binding.root)
            } catch (e: IllegalArgumentException) {
                Timber.d(e.toString())
            }
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