package com.b1nar10.ml_face_recognition.ui.utils

import android.content.Context
import android.graphics.Canvas
import android.hardware.camera2.CameraCharacteristics
import android.util.AttributeSet
import android.view.View

/**
 * A view which renders a series of custom graphics (e.g., face contours) to be overlayed on top of an associated preview
 * (i.e., the camera preview). Supports scaling and mirroring of the graphics relative the camera's
 * preview properties.
 */
class GraphicOverlay(context: Context, attrs: AttributeSet) : View(context, attrs) {
    // Synchronization lock for safe access to mutable data
    private val lock = Any()

    // Width of the camera preview
    private var previewWidth = 0

    // Factor by which graphic coordinates should be scaled in width
    internal var widthScaleFactor = 1.0f

    // Height of the camera preview
    private var previewHeight = 0

    // Factor by which graphic coordinates should be scaled in height
    internal var heightScaleFactor = 1.0f

    // The camera is facing back by default
    internal var facing = CameraCharacteristics.LENS_FACING_BACK

    // Collection of graphics to be drawn on top of the camera preview
    private val graphics: MutableSet<Graphic> = mutableSetOf()


    // Remove all graphics from the overlay
    fun clear() {
        synchronized(lock) {
            graphics.clear()
        }
        postInvalidate()
    }

    // Adds a graphic to the overlay
    fun add(graphic: Graphic) {
        synchronized(lock) {
            graphics.add(graphic)
        }
        postInvalidate()
    }

    // Updates camera-related properties for the overlay
    fun setCameraInfo(previewWidth: Int, previewHeight: Int, facing: Int) {
        synchronized(lock) {
            this.previewWidth = previewWidth
            this.previewHeight = previewHeight
            this.facing = facing
        }
        postInvalidate()
    }

    // Draws the overlay with it's associated graphics
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        synchronized(lock) {
            if (previewWidth != 0 && previewHeight != 0) {
                widthScaleFactor = width.toFloat() / previewWidth
                heightScaleFactor = height.toFloat() / previewHeight
            }

            // Draw each graphic on the canvas
            for (graphic in graphics) {
                graphic.draw(canvas)
            }
        }
    }
}
