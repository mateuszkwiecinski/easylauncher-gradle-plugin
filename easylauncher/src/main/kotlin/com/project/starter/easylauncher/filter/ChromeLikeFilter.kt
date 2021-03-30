package com.project.starter.easylauncher.filter

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.roundToInt

class ChromeLikeFilter(
    private val label: String,
    ribbonColor: Color? = null,
    labelColor: Color? = null,
    private val labelPadding: Int? = null,
    overlayRatio: Float? = null,
    gravity: Gravity?,
    private val textSizeRatio: Float? = null,
    fontName: String? = null,
    fontResource: File? = null,
    adaptivePadding: Boolean? = null,
) : EasyLauncherFilter {

    enum class Gravity {
        TOP, BOTTOM
    }

    private val ribbonColor = ribbonColor ?: Color.DARK_GRAY
    private val labelColor = labelColor ?: Color.WHITE
    private val font = fontResource?.takeIf { it.exists() }
        ?.let { Font.createFont(Font.TRUETYPE_FONT, it) }
        ?: Font(fontName, Font.PLAIN, 1)
    private val overlayRatio = overlayRatio ?: OVERLAY_RATIO
    private val gravity = gravity ?: Gravity.BOTTOM
    private val adaptivePadding = adaptivePadding ?: true

    override fun apply(image: BufferedImage, adaptive: Boolean) {
        val graphics = image.graphics as Graphics2D

        val viewportWidth = image.getViewportWidth(adaptive)
        val viewportHeight = image.getViewportHeight(adaptive)
        val verticalPadding = ((image.height - viewportHeight) / 2f).roundToInt()

        // calculate the rectangle where the label is rendered
        val overlayHeight = (viewportHeight * overlayRatio).roundToInt()
        val backgroundHeight = overlayHeight + verticalPadding
        val background = when (gravity) {
            Gravity.TOP -> Rectangle(0, 0, image.width, backgroundHeight)
            Gravity.BOTTOM -> Rectangle(0, image.height - backgroundHeight, image.width, backgroundHeight)
        }

        // Calculate label size
        val frc = FontRenderContext(graphics.transform, true, true)
        val extraPadding = if (adaptivePadding && adaptive) viewportHeight / 10f else 0f
        graphics.font = getFont(
            imageHeight = viewportHeight,
            maxLabelWidth = (viewportWidth - extraPadding).roundToInt(),
            maxLabelHeight = (overlayHeight - extraPadding).roundToInt(),
            frc = frc
        )
        val textBounds = graphics.font.getStringBounds(label, frc)

        // draw the ribbon
        graphics.color = ribbonColor
        if (!adaptive) {
            graphics.composite = AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 1f)
        }
        graphics.fillRect(background.x, background.y, background.width, background.height)

        // draw the label
        graphics.setPaintMode()
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.color = labelColor
        val fm = graphics.fontMetrics
        when (gravity) {
            Gravity.TOP ->
                graphics.drawString(
                    label,
                    (background.centerX - textBounds.centerX).toFloat(),
                    background.maxY.toFloat() - fm.descent - (labelPadding ?: 0)
                )
            Gravity.BOTTOM ->
                graphics.drawString(
                    label,
                    (background.centerX - textBounds.centerX).toFloat(),
                    background.y.toFloat() + fm.ascent + (labelPadding ?: 0)
                )
        }
        graphics.dispose()
    }

    private fun getFont(imageHeight: Int, maxLabelWidth: Int, maxLabelHeight: Int, frc: FontRenderContext): Font {
        if (textSizeRatio != null) {
            return font.deriveFont((imageHeight * textSizeRatio).roundToInt().toFloat())
        }

        return (imageHeight downTo 0).asSequence()
            .map { size -> font.deriveFont(size.toFloat()) }
            .first { font ->
                val bounds = font.getStringBounds(label, frc)
                bounds.width < maxLabelWidth && bounds.height < maxLabelHeight
            }
    }

    companion object {
        private const val OVERLAY_RATIO = 0.4f
    }
}
