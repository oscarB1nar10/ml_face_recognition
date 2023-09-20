package com.b1nar10.ml_face_recognition.ui.utils

import android.util.Pair
import com.b1nar10.ml_face_recognition.ui.MainActivity

fun MainActivity.getTargetedWidthHeight(): Pair<Int, Int> {
    val maxWidthForPortraitMode: Int = viewBinding.viewFinder.width
    val maxHeightForPortraitMode: Int = viewBinding.viewFinder.height
    return Pair(maxWidthForPortraitMode, maxHeightForPortraitMode)
}