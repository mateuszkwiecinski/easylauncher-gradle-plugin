package com.project.starter.easylauncher.filter

import java.awt.image.BufferedImage
import kotlin.math.roundToInt

internal const val ADAPTIVE_SCALE = 72 / 108f

/**
 * Calculate the width of the viewport (e.g. the visible area of the image).
 *
 *  For legacy icons, this is simply the full image width, but for adaptive icons this is only the inner 72dp of a 108dp icon.
 */
fun BufferedImage.getViewportWidth(adaptive: Boolean) = (width * if (adaptive) ADAPTIVE_SCALE else 1f).roundToInt()

/**
 * Calculate the height of the viewport (e.g. the visible area of the image).
 *
 *  For legacy icons, this is simply the full image height, but for adaptive icons this is only the inner 72dp of a 108dp icon.
 */
fun BufferedImage.getViewportHeight(adaptive: Boolean) = (height * if (adaptive) ADAPTIVE_SCALE else 1f).roundToInt()
