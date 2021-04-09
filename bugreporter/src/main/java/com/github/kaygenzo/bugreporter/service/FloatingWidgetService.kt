package com.github.kaygenzo.bugreporter.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.view.*
import com.github.kaygenzo.bugreporter.BugReporter
import com.github.kaygenzo.bugreporter.R
import com.github.kaygenzo.bugreporter.ReportMethod
import com.github.kaygenzo.bugreporter.shake.OnShakeListener
import com.github.kaygenzo.bugreporter.shake.ShakeDetectorKotlin
import com.github.kaygenzo.bugreporter.utils.PermissionsUtils
import kotlinx.android.synthetic.main.floating_widget.view.*
import timber.log.Timber
import kotlin.math.abs


internal class FloatingWidgetService : Service(), OnShakeListener {

    companion object {
        const val ACTION_ENTER_REPORT = "com.telen.library.action.ENTER_REPORT"
        const val ACTION_EXIT_REPORT = "com.telen.library.action.EXIT_REPORT"
        const val ACTION_ENABLE_FLOATING_BUTTON = "com.telen.library.action.ENABLE_FLOATING_BUTTON"
        const val ACTION_DISABLE_FLOATING_BUTTON = "com.telen.library.action.DISABLE_FLOATING_BUTTON"
    }

    private var mOverlayView: View? = null
    private var floatingWidget: View? = null
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
        mOverlayView = LayoutInflater.from(this).inflate(R.layout.floating_widget, null)
        floatingWidget = mOverlayView?.fabHead
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let {
            when(it) {
                ACTION_ENTER_REPORT -> {
                    hideFloatingButton()
                    shaked = true
                }
                ACTION_EXIT_REPORT -> {
                    showFloatingButton()
                    shaked = false
                }
                ACTION_DISABLE_FLOATING_BUTTON -> {
                    stopSelf()
                }
                else -> {
                    Timber.d("Not managed action $it ...")
                }
            }
        } ?: run {
            if(hasFloatingButtonMethod() && PermissionsUtils.hasPermissionOverlay(this)) {
                (getSystemService(WINDOW_SERVICE) as? WindowManager)?.let { windowManager ->
                    windowManager.addView(mOverlayView, params)
                    val layout = mOverlayView?.floating_widget_root
                    floatingWidget?.setOnTouchListener(
                        FloatingButtonTouchListener(
                            windowManager = windowManager,
                            layout = layout,
                            params = params
                        )
                    )
                }
            }

            if(hasShakeMethod()) {
                shakeDetector = ShakeDetectorKotlin.create(this, this)
                shakeDetector?.start()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        if(hasFloatingButtonMethod() && PermissionsUtils.hasPermissionOverlay(this)) {
            (getSystemService(WINDOW_SERVICE) as? WindowManager)?.removeView(mOverlayView)
        }
        if(hasShakeMethod()) {
            shakeDetector?.stop()
        }
    }

    private fun hasShakeMethod(): Boolean {
        return BugReporter.reportingMethods.contains(ReportMethod.SHAKE)
    }

    private fun hasFloatingButtonMethod(): Boolean {
        return BugReporter.reportingMethods.contains(ReportMethod.FLOATING_BUTTON)
    }

    inner class FloatingButtonTouchListener(
        private val windowManager: WindowManager,
        private val layout: View?,
        private val params: WindowManager.LayoutParams,
        private val size: Point = Point()
    ): View.OnTouchListener {

        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f

        init {
            val display: Display = windowManager.defaultDisplay
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
                    //xDiff and yDiff contain the minor changes in position when the view is clicked.
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
        BugReporter.getCurrentActivity()?.let { activity ->
            BugReporter.startReport(activity)
        } ?: run {
            //TODO
        }
    }

    override fun onShake() {
        synchronized(shaked) {
            if(!shaked) {
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
        mOverlayView?.floating_widget_root?.visibility = View.VISIBLE
    }
}