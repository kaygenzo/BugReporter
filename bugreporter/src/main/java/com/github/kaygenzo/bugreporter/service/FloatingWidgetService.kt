package com.github.kaygenzo.bugreporter.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageView
import com.github.kaygenzo.bugreporter.R
import com.github.kaygenzo.bugreporter.UnsupportedSensorException
import com.github.kaygenzo.bugreporter.api.ReportMethod
import com.github.kaygenzo.bugreporter.internal.BugReporterImpl
import com.github.kaygenzo.bugreporter.shake.OnShakeListener
import com.github.kaygenzo.bugreporter.shake.ShakeDetectorKotlin
import com.github.kaygenzo.bugreporter.utils.PermissionsUtils
import com.github.kaygenzo.bugreporter.utils.startAsForeground
import kotlinx.android.synthetic.main.floating_widget.view.*
import timber.log.Timber
import kotlin.math.abs


internal class FloatingWidgetService : Service(), OnShakeListener {

    companion object {
        const val ACTION_ENTER_REPORT = "com.telen.library.action.ENTER_REPORT"
        const val ACTION_EXIT_REPORT = "com.telen.library.action.EXIT_REPORT"
        const val ACTION_ENABLE_FLOATING_BUTTON = "com.telen.library.action.ENABLE_FLOATING_BUTTON"
        const val ACTION_DISABLE_FLOATING_BUTTON =
            "com.telen.library.action.DISABLE_FLOATING_BUTTON"
        const val NOTIFICATION_ID = 9876
    }

    private var mOverlayView: View? = null
    private var floatingWidget: ImageView? = null
    private val mLock = Any()
    private var shaked = false
    private var shakeDetector: ShakeDetectorKotlin? = null

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
        startAsForeground(
            getString(R.string.bug_reporter_channel_id),
            getString(R.string.bug_reporter_channel_name),
            NOTIFICATION_ID
        )
        mOverlayView = LayoutInflater.from(this).inflate(R.layout.floating_widget, null)
        floatingWidget = mOverlayView?.fabHead?.apply {
            setImageResource(BugReporterImpl.reportFloatingImage)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let {
            when (it) {
                ACTION_ENTER_REPORT -> {
                    hideFloatingButton()
                    synchronized(mLock) {
                        shaked = true
                    }
                }
                ACTION_EXIT_REPORT -> {
                    showFloatingButton()
                    synchronized(mLock) {
                        shaked = false
                    }
                }
                ACTION_DISABLE_FLOATING_BUTTON -> {
                    stopSelf()
                }
                else -> {
                    Timber.d("Not managed action $it ...")
                }
            }
        } ?: run {
            if (hasFloatingButtonMethod()) {
                setFloatingWidget()
            }
            if (hasShakeMethod()) {
                try {
                    shakeDetector = ShakeDetectorKotlin.create(this, this)
                } catch(e: UnsupportedSensorException) {
                    Timber.e(e)
                }
                shakeDetector?.start()
            }

            if (!hasFloatingButtonMethod() && !hasShakeMethod()) {
                stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setFloatingWidget() {
        if (PermissionsUtils.hasPermissionOverlay(this)) {
            (getSystemService(WINDOW_SERVICE) as? WindowManager)?.let { windowManager ->
                windowManager.addView(mOverlayView, params)
                val layout = mOverlayView?.floating_widget_root
                val listener = FloatingButtonTouchListener(
                    windowManager = windowManager,
                    layout = layout,
                    params = params
                )
                floatingWidget?.setOnTouchListener(listener)
            }
        }
    }

    private fun removeFloatingWidget() {
        if (PermissionsUtils.hasPermissionOverlay(this)) {
            (getSystemService(WINDOW_SERVICE) as? WindowManager)?.removeView(mOverlayView)
        }
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

    inner class FloatingButtonTouchListener(
        private val windowManager: WindowManager,
        private val layout: View?,
        private val params: WindowManager.LayoutParams,
        private val size: Point = Point()
    ) : View.OnTouchListener {

        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f

        init {
            val display = windowManager.defaultDisplay
            display.getSize(size)
        }

        override fun onTouch(v: View?, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {

                    //remember the initial position.
                    initialX = params.x
                    initialY = params.y

                    //get the touch location
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY

                    return true
                }
                MotionEvent.ACTION_UP -> {
                    // xDiff and yDiff contain the minor changes in position when the view is
                    // clicked.
                    val xDiff = event.rawX - initialTouchX
                    val yDiff = event.rawY - initialTouchY

                    if ((abs(xDiff) < 5) && (abs(yDiff) < 5)) {
                        launchReport()
                    }

                    val middle = size.x / 2
                    val centerView = params.x + ((layout?.measuredWidth ?: 0) / 2)
                    val nearestXWall = if (centerView >= middle) size.x else 0.toFloat()
                    params.x = nearestXWall.toInt()

                    windowManager.updateViewLayout(mOverlayView, params)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val Xdiff = event.rawX - initialTouchX
                    val Ydiff = event.rawY - initialTouchY

                    //Calculate the X and Y coordinates of the view.
                    params.x = initialX + Xdiff.toInt()
                    params.y = initialY + Ydiff.toInt()

                    //Update the layout with new X & Y coordinates
                    windowManager.updateViewLayout(mOverlayView, params)
                    return true
                }
            }
            return false
        }
    }

    private fun launchReport() {
        BugReporterImpl.getCurrentActivity()?.let { activity ->
            BugReporterImpl.startReport(activity)
        } ?: run {
            Timber.e("No activity found to start report")
        }
    }

    override fun onShake() {
        synchronized(mLock) {
            if (!shaked) {
                shaked = true
                Timber.d("OnShake!")
                launchReport()
            }
        }
    }

    private fun hideFloatingButton() {
        mOverlayView?.floating_widget_root?.visibility = View.GONE
    }

    private fun showFloatingButton() {
        if (hasFloatingButtonMethod()) {
            mOverlayView?.floating_widget_root?.visibility = View.VISIBLE
        }
    }
}