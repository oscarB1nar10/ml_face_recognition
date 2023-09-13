package com.b1nar10.ml_face_recognition.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Looper
import android.util.AttributeSet
import android.view.View

class CustomCameraPreview(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val rectangles: MutableList<Rect> = mutableListOf()
    private val handler = android.os.Handler(Looper.getMainLooper())
    private var aspectRatio : Float = 0f


    private val paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (rect in rectangles) {
            canvas.drawRect(rect, paint)
        }
    }

    fun updateFaces(faces: List<Rect>) {
        rectangles.clear()
        rectangles.addAll(faces)
        invalidate() // This will trigger onDraw
    }

}