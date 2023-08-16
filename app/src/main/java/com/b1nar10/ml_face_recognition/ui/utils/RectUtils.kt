package com.b1nar10.ml_face_recognition.ui.utils

import android.graphics.PointF
import android.graphics.Rect
import android.util.Size

fun Rect.mapToViewCoordinates(
    cameraSize: Size,
    viewSize: Size,
    faceRect: Rect,
    rotationDegrees: Int
): Rect {
    // Depending on the rotation, the width and height of the camera image might be swapped.
    // This is because for portrait mode, the camera's natural orientation is landscape.
    val rotatedCameraSize = if (rotationDegrees == 90 || rotationDegrees == 270) {
        Size(cameraSize.height, cameraSize.width)
    } else {
        cameraSize
    }

    // Calculate the aspect ratio of the camera image and the view
    val cameraAspectRatio =
        rotatedCameraSize.width.toFloat() / rotatedCameraSize.height.toFloat()
    val viewAspectRatio = viewSize.width.toFloat() / viewSize.height.toFloat()

    val scale: PointF
    val padding: PointF

    // The camera image will be scaled to fit within the view. Depending on the aspect ratios,
    // the image will fit either the width or the height of the view.
    if (cameraAspectRatio > viewAspectRatio) {
        // Camera image is wider than the view. Scale based on width.
        scale = PointF(
            viewSize.width.toFloat() / rotatedCameraSize.width.toFloat(),
            viewSize.width.toFloat() / rotatedCameraSize.width.toFloat()
        )
        // Calculate the padding to be added on the top and bottom of the image
        padding = PointF(
            0f,
            (viewSize.height - scale.y * rotatedCameraSize.height.toFloat()) / 2
        )
    } else {
        // Camera image is taller than the view. Scale based on height.
        scale = PointF(
            viewSize.height.toFloat() / rotatedCameraSize.height.toFloat(),
            viewSize.height.toFloat() / rotatedCameraSize.height.toFloat()
        )
        // Calculate the padding to be added on the sides of the image
        padding = PointF(
            (viewSize.width - scale.x * rotatedCameraSize.width.toFloat()) / 2,
            0f
        )
    }

    // Scale the face bounding box and adjust for padding.
    // This transforms the face bounding box from the camera image coordinates to the view coordinates.
    val left = faceRect.left * scale.x + padding.x
    val top = faceRect.top * scale.y + padding.y
    val right = faceRect.right * scale.x + padding.x
    val bottom = faceRect.bottom * scale.y + padding.y

    // Create and return the new face bounding box in the view's coordinate system.
    return Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
}