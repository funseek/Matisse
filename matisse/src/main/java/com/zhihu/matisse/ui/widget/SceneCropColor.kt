package com.zhihu.matisse.ui.widget

import androidx.annotation.ColorRes
import com.zhihu.matisse.R

enum class SceneCropColor(@param:ColorRes val colorRes: Int, val clearColorItem: ClearColorItem) {

    WHITE(R.color.white, ClearColorItem(1f, 1f, 1f, 1f)),
    GRAY(R.color.crop_background_gray, ClearColorItem(0.867f, 0.867f, 0.867f, 1f)),
    DARK(R.color.crop_background_dark, ClearColorItem(0.267f, 0.267f, 0.267f, 1f)),
    BLACK(R.color.black, ClearColorItem(0f, 0f, 0f, 1f)),
    PINK(R.color.crop_background_pink, ClearColorItem(1f, 0.827f, 0.87f, 1f)),
    FLESH(R.color.crop_background_flesh, ClearColorItem(1f, 0.945f, 0.768f, 1f)),
    GREEN(R.color.crop_background_green, ClearColorItem(0.905f, 1f, 0.898f, 1f)),
    BLUE(R.color.crop_background_blue, ClearColorItem(0.898f, 0.937f, 1f, 1f)),
    BROWN(R.color.crop_background_brown, ClearColorItem(0.85f, 0.807f, 0.745f, 1f))
}

class ClearColorItem(val red: Float, val green: Float, val blue: Float, val alpha: Float)