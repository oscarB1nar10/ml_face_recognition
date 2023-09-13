package com.b1nar10.ml_face_recognition.ui.utils

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.mlkit.vision.face.Face

class FaceContourGraphic(overlay: GraphicOverlay) : GraphicOverlay.Graphic(overlay) {

    companion object {
        private const val FACE_POSITION_RADIUS = 10.0f
        private const val ID_TEXT_SIZE = 70.0f
        private const val ID_Y_OFFSET = 80.0f
        private const val ID_X_OFFSET = -70.0f
        private const val BOX_STROKE_WIDTH = 5.0f

        private val COLOR_CHOICES = arrayOf(
            Color.BLUE, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.RED, Color.WHITE, Color.YELLOW
        )
        private var currentColorIndex = 0
    }

    private val facePositionPaint: Paint
    private val idPaint: Paint
    private val boxPaint: Paint

    @Volatile
    private var face: Face? = null

    init {
        currentColorIndex = (currentColorIndex + 1) % COLOR_CHOICES.size
        val selectedColor = COLOR_CHOICES[currentColorIndex]

        facePositionPaint = Paint().apply { color = selectedColor }

        idPaint = Paint().apply {
            color = selectedColor
            textSize = ID_TEXT_SIZE
        }

        boxPaint = Paint().apply {
            color = selectedColor
            style = Paint.Style.STROKE
            strokeWidth = BOX_STROKE_WIDTH
        }
    }

    fun updateFace(face: Face) {
        this.face = face
        postInvalidate()
    }

    override fun draw(canvas: Canvas) {
        val face = this.face ?: return

        // Translate the bounding box coordinates to fit the overlay view.
        val left = translateX(face.boundingBox.left.toFloat())
        val top = translateY(face.boundingBox.top.toFloat())
        val right = translateX(face.boundingBox.right.toFloat())
        val bottom = translateY(face.boundingBox.bottom.toFloat())

        // Draw the bounding box on the canvas.
        canvas.drawRect(left, top, right, bottom, boxPaint)
    }
}
