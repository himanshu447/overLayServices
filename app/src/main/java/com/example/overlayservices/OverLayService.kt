package com.example.overlayservices

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView

class OverLayService : Service() {

    private val overlayViewParams by lazy {
        val parms = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        parms.gravity = Gravity.TOP or Gravity.START
        parms.x = 0
        parms.y = 100
        parms
    }

    private val overlayRemoveViewParams by lazy {
        val parms = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        parms.gravity = Gravity.BOTTOM or Gravity.CENTER
        parms.x = 0
        parms.y = 100

        // parms.windowAnimations = R.style.Animation_AppCompat_DropDownUp
        parms
    }

    private val windowManager by lazy {
        getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val vibrator by lazy {
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val overlayRemoveView by lazy {
        LayoutInflater.from(this).inflate(R.layout.remove_view, null)
    }

    private val parentViewForRemoveViewAnimation by lazy {
        FrameLayout(this)
    }

    private var overLayView: View? = null
    private var isLongPress: Boolean = false
    private var activity_background: Boolean? = null
    private var buttonWidth = 0
    private var buttonHeight = 0
//    private lateinit var parentView: View

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent != null) {
            activity_background = intent.getBooleanExtra("activity_background", false)
        }

        if (overLayView == null) {

            /**
             * inflate & attach overLay view to windowManager
             */
            overLayView = LayoutInflater.from(this).inflate(R.layout.overlayserviceview, null)

            windowManager.addView(overLayView, overlayViewParams)


            val btn = (overLayView as View).findViewById<Button>(R.id.moveBTN)
            val iv = (overlayRemoveView as View).findViewById<ImageView>(R.id.removeIV)

            /**
             * find height & with of overlay View button
             */
            btn.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    btn.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    buttonWidth = btn.measuredWidth
                    buttonHeight = btn.measuredHeight
                }
            })

            btn.setOnTouchListener(object : View.OnTouchListener {

                var initialX: Int? = null
                var initialY: Int? = null
                var initialTouchX: Float? = null
                var initialTouchY: Float? = null

                override fun onTouch(v: View?, event: MotionEvent?): Boolean {

                    return when (event?.action) {
                        MotionEvent.ACTION_DOWN -> {

                            initialX = overlayViewParams.x
                            initialY = overlayViewParams.y

                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            false
                        }
                        MotionEvent.ACTION_MOVE -> {

                            /* initialX?.let { initialX ->
                                 initialTouchX?.let { initialTouchX ->
                                     overlayViewParams.x = initialX + (event.rawX - initialTouchX).toInt()
                                 }
                             }

                             initialY?.let { initialY ->
                                 initialTouchY?.let { initialTouchY ->
                                     overlayViewParams.y = initialY + (event.rawX - initialTouchY).toInt()
                                 }
                             }*/

                            val xDiff = Math.round(event.rawX - initialTouchX!!)
                            val yDiff = Math.round(event.rawY - initialTouchY!!)

                            //Calculate the X and Y coordinates of the view.
                            overlayViewParams.x = initialX!! + xDiff
                            overlayViewParams.y = initialY!! + yDiff

                            windowManager.updateViewLayout(overLayView, overlayViewParams)
                            false
                        }

                        MotionEvent.ACTION_UP -> {
                            if (isLongPress) {

                                /**
                                 * get position of removeView on screen
                                 */
                                val location = IntArray(2)
                                iv?.getLocationOnScreen(location)
                                val IVX = location[0]
                                val IVY = location[1]

                                /**
                                 * create Rectangular for checking that over view is overLap with Remove View or Not
                                 */
                                val rc = Rect(
                                    event.rawX.toInt(),
                                    event.rawY.toInt(),
                                    event.rawX.toInt() + buttonWidth,
                                    event.rawY.toInt() + buttonHeight
                                )

                                if (rc.contains(IVX, IVY) ||
                                    rc.contains(IVX + iv.width, IVY) ||
                                    rc.contains(IVX, IVY + iv.height) ||
                                    rc.contains(IVX + iv.width, IVY + iv.height)
                                )
                                    removeViews(true)
                                else {
                                    Log.e("TAG ", "NOT CONTAIN")
                                }
                                isLongPress = false
                                removeViews(false)
                            }
                            false
                        }
                        else -> {
                            false
                        }

                    }
                }
            })

            btn?.setOnLongClickListener {
                Log.e("TAG", "Long Press")
                isLongPress = true
                vibrator.vibrate(500)
                /**
                 * add removeIcon view in window manager we can not directly gien animation to windowManger.
                 * so for animation we create parentView of over view and then we assign the parentView to windowManger.
                 * https://stackoverflow.com/a/17758858
                 */
                windowManager.addView(parentViewForRemoveViewAnimation, overlayRemoveViewParams)

                overlayRemoveView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in))
                parentViewForRemoveViewAnimation.addView(overlayRemoveView)

                true
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun removeViews(isOverlayViewRemove: Boolean) {

        if (isOverlayViewRemove) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(200)
            }
            windowManager.removeView(overLayView)
            stopService(Intent(this, OverLayService::class.java))
        }

        overlayRemoveView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out))
        parentViewForRemoveViewAnimation.removeView(overlayRemoveView)
        windowManager.removeView(parentViewForRemoveViewAnimation)
    }
}