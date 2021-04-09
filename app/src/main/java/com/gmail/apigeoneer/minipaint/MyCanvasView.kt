package com.gmail.apigeoneer.minipaint

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.res.ResourcesCompat
import kotlin.math.abs

class MyCanvasView(context: Context) : View(context) {

    companion object {
        private const val STROKE_WIDTH = 12f       // has to be in float
    }

    // for caching what has been drawn before
    private lateinit var extraCanvas: Canvas

    private lateinit var extraBitmap: Bitmap
    private val backgroundColor = ResourcesCompat.getColor(resources, R.color.colorBackground, null)

    private val drawColor = ResourcesCompat.getColor(resources, R.color.colorPaint, null)
    private val paint = Paint().apply {
        color = drawColor
        // smooth out edges of what is drawn, w/o affecting the shape
        isAntiAlias = true
        // dithering effects how colors w/ precision higher than the device are down-sampled
        isDither = true
        style = Paint.Style.STROKE          // default: FILL
        strokeJoin = Paint.Join.ROUND       // default: MITER
        strokeCap = Paint.Cap.ROUND         // default: BUTT
        strokeWidth = STROKE_WIDTH          // default: Hairline-width (really thin)
    }

    private var path = Path()

    // to cache the current x & y values
    private var motionTouchEventX = 0f
    private var motionTouchEventY = 0f

    // to cache the latest x & y values
    private var currentX = 0f
    private var currentY = 0f

    // for the touchMove() method
    private var touchTolerance = ViewConfiguration.get(context).scaledTouchSlop

    // rectangular frame
    private lateinit var frame: Rect

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // super.onSizeChanged(w, h, oldw, oldh)
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
        extraCanvas.drawColor(backgroundColor)

        // recycle the extra Bitmap before creating a new one
        // if (::extraBitmap.isInitialized) extraBitmap.recycle()                gives error

        // Calculate a rectangular frame around the pic
        val inset = 40
        frame = Rect(inset, inset, width-inset, height-inset)

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawBitmap(extraBitmap, 0f, 0f, null)
        // Draw a frame around the canvas
        canvas?.drawRect(frame, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        motionTouchEventX = event.x
        motionTouchEventY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> touchStart()
            MotionEvent.ACTION_MOVE -> touchMove()
            MotionEvent.ACTION_UP -> touchUp()
        }
        return true
    }

    private fun touchStart() {
        path.reset()
        path.moveTo(motionTouchEventX, motionTouchEventY)
        currentX = motionTouchEventX
        currentY = motionTouchEventY
    }

    private fun touchMove() {
        // calculate the travelled distance (dx, dy)
        val dx = abs(motionTouchEventX - currentX)
        val dy = abs(motionTouchEventY - currentY)

        if (dx >= touchTolerance || dy >= touchTolerance) {
            /**
             * Create a curve b/w the 2 points & store it in path
             * QuadTo() adds a quadratic bezier from the last point,
             * approaching control point (x1, y1) & ending at (x2, y2)
             */
            path.quadTo(currentX, currentY, (motionTouchEventX + currentX) / 2, (motionTouchEventY + currentY) / 2)
            // Update the running currentX & currentY tally
            currentX = motionTouchEventX
            currentY = motionTouchEventY
            // Draw the path
            extraCanvas.drawPath(path, paint)
        }
        // to force redrawing of the screen with the updated path
        invalidate()
    }

    private fun touchUp() {
        // Reset the path so it doesn't get drawn again
        path.reset()
    }

}