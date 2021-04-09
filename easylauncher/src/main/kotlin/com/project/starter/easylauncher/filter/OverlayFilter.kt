package com.project.starter.easylauncher.filter

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Overlay another image over the icon.
 *
 * @param fgFile The overlay image to render.
 * @param adaptivePadding `true` to automatically scale the square image into the adaptive icon; `false` for no scaling.
 */
@Suppress("MagicNumber")
class OverlayFilter(
    private val fgFile: File,
    adaptivePadding: Boolean? = null,
) : EasyLauncherFilter {

    private val adaptivePadding = adaptivePadding ?: true

    override fun apply(image: BufferedImage, adaptive: Boolean) {
        try {
            ImageIO.read(fgFile)?.let { fgImage ->
                val viewportWidth = image.getViewportWidth(adaptive)
                val viewportHeight = image.getViewportHeight(adaptive)
                val fgWidth = fgImage.getWidth(null).toFloat()
                val fgHeight = fgImage.getHeight(null).toFloat()

                var scale = (viewportWidth / fgWidth).coerceAtMost(viewportHeight / fgHeight)
                if (adaptivePadding && adaptive) {
                    // Scale the square image into the adaptive circle
                    scale *= sqrt(0.5f)
                }
                val scaledWidth = (fgWidth * scale).roundToInt()
                val scaledHeight = (fgHeight * scale).roundToInt()
                val fgImageScaled = fgImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH)
                val graphics = image.createGraphics()

                // TODO allow to choose the gravity for the overlay
                // TODO allow to choose the scaling type
                graphics.drawImage(
                    fgImageScaled,
                    ((image.width - scaledWidth) / 2f).roundToInt(),
                    ((image.height - scaledHeight) / 2f).roundToInt(),
                    null
                )
                graphics.dispose()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
