package com.b1nar10.ml_face_recognition.ui.utils

import android.graphics.Rect
import android.util.Size

fun Rect.mapToViewCoordinates(
    cameraSize: Size,
    viewSize: Size,
    rotationDegrees: Int
): Rect {

    // Depending on the rotation, the width and height of the camera image might be swapped.
    // This is because for portrait mode, the camera's natural orientation is landscape.
    val rotatedCameraSize = if (rotationDegrees == 90 || rotationDegrees == 270) {
        Size(cameraSize.height, cameraSize.width)
    } else {
        cameraSize
    }

    // Calculate the scale factor. Since the aspect ratios match, we can use a simple scaling factor
    // based on the view's dimensions and the rotated camera's dimensions.
    val scaleX = viewSize.width.toFloat() / cameraSize.width.toFloat()
    val scaleY = viewSize.height.toFloat() / cameraSize.height.toFloat()

    // Scale the face bounding box.
    // This transforms the face bounding box from the camera image coordinates to the view coordinates.
    val left = this.left * scaleX
    val top = this.top * scaleY
    val right = this.right * scaleX
    val bottom = this.bottom * scaleY

    // Create and return the new face bounding box in the view's coordinate system.
    return Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
}

fun Rect.mapToCameraCoordinates(
    cameraSize: Size,
    viewSize: Size,
    rotationDegrees: Int
): Rect {
    // Depending on the rotation, the width and height of the camera image might be swapped.
    val rotatedCameraSize = if (rotationDegrees == 90 || rotationDegrees == 270) {
        Size(cameraSize.height, cameraSize.width)
    } else {
        cameraSize
    }

    // Calculate the scaling factors
    val scaleX = rotatedCameraSize.width.toFloat() / viewSize.width.toFloat()
    val scaleY = rotatedCameraSize.height.toFloat() / viewSize.height.toFloat()

    // The inverse operation: Instead of scaling up, we scale down the bounding box coordinates
    val left = (this.left * scaleX).toInt()
    val top = (this.top * scaleY).toInt()
    val right = (this.right * scaleX).toInt()
    val bottom = (this.bottom * scaleY).toInt()

    return Rect(left, top, right, bottom)
}

fun transformBoundingBoxToCameraCoordinates(
    boundingBox: Rect,
    cameraSize: Size,
    viewSize: Size,
    rotationDegrees: Int
): Rect {
    // Convert bounding box to float for transformations
    val left = boundingBox.left.toFloat()
    val top = boundingBox.top.toFloat()
    val right = boundingBox.right.toFloat()
    val bottom = boundingBox.bottom.toFloat()

    val widthRatio = cameraSize.width.toFloat() / viewSize.width
    val heightRatio = cameraSize.height.toFloat() / viewSize.height

    // Scaling the bounding box to match the camera's aspect ratio
    val scaledLeft = left * widthRatio
    val scaledTop = top * heightRatio
    val scaledRight = right * widthRatio
    val scaledBottom = bottom * heightRatio

    // Apply rotation if needed
    return when (rotationDegrees) {
        90 -> Rect(
            scaledTop.toInt(),
            cameraSize.width - scaledRight.toInt(),
            scaledBottom.toInt(),
            cameraSize.width - scaledLeft.toInt()
        )
        180 -> Rect(
            cameraSize.width - scaledRight.toInt(),
            cameraSize.height - scaledBottom.toInt(),
            cameraSize.width - scaledLeft.toInt(),
            cameraSize.height - scaledTop.toInt()
        )
        270 -> Rect(
            cameraSize.height - scaledBottom.toInt(),
            scaledLeft.toInt(),
            cameraSize.height - scaledTop.toInt(),
            scaledRight.toInt()
        )
        else -> Rect(
            scaledLeft.toInt(),
            scaledTop.toInt(),
            scaledRight.toInt(),
            scaledBottom.toInt()
        )
    }
}
