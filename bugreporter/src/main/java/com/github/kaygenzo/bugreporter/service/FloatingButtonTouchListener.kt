package com.github.kaygenzo.bugreporter.service

import android.annotation.SuppressLint
import android.graphics.Point
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.github.kaygenzo.bugreporter.databinding.FloatingWidgetBinding
import kotlin.math.abs

internal class FloatingButtonTouchListener(
    private val windowManager: WindowManager,
    private val binding: FloatingWidgetBinding,
    private val params: WindowManager.LayoutParams,
    private val size: Point = Point(),
    private val clickListener: () -> Unit
) : View.OnTouchListener {

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    init {
        val display = windowManager.defaultDisplay
        display.getSize(size)
    }

    @SuppressLint("ClickableViewAccessibility")
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
                    clickListener()
                }

                val middle = size.x / 2
                val centerView = params.x + (binding.floatingWidgetRoot.measuredWidth / 2)
                val nearestXWall = if (centerView >= middle) size.x else 0.toFloat()
                params.x = nearestXWall.toInt()

                windowManager.updateViewLayout(binding.root, params)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val Xdiff = event.rawX - initialTouchX
                val Ydiff = event.rawY - initialTouchY

                //Calculate the X and Y coordinates of the view.
                params.x = initialX + Xdiff.toInt()
                params.y = initialY + Ydiff.toInt()

                //Update the layout with new X & Y coordinates
                windowManager.updateViewLayout(binding.root, params)
                return true
            }
        }
        return false
    }
}