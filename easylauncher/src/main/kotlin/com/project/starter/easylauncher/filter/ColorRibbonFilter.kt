package com.project.starter.easylauncher.filter

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Suppress("MagicNumber")
class ColorRibbonFilter(
    private val label: String,
    ribbonColor: Color? = null,
    labelColor: Color? = null,
    gravity: Gravity? = null,
    private val textSizeRatio: Float? = null,
    fontName: String? = null,
    fontResource: File? = null,
    private val drawingOptions: Set<DrawingOption> = emptySet(),
    adaptivePadding: Boolean? = null,
) : EasyLauncherFilter {

    enum class Gravity {
        TOP, BOTTOM, TOPLEFT, TOPRIGHT
    }

    enum class DrawingOption {
        IGNORE_TRANSPARENT_PIXELS,
        ADD_EXTRA_PADDING,
    }

    private val ribbonColor = ribbonColor ?: Color(0, 0x72, 0, 0x99)
    private val labelColor = labelColor ?: Color.WHITE
    private val gravity = gravity ?: Gravity.TOPLEFT
    private val font = fontResource?.takeIf { it.exists() }
        ?.let { Font.createFont(Font.TRUETYPE_FONT, it) }
        ?: Font(fontName, Font.PLAIN, 1)
    private val adaptivePadding = adaptivePadding ?: true
    private val addExtraPadding
        get() = drawingOptions.contains(DrawingOption.ADD_EXTRA_PADDING)

    @Suppress("ComplexMethod")
    override fun apply(image: BufferedImage, adaptive: Boolean) {
        val graphics = image.graphics as Graphics2D

        val viewportWidth = image.getViewportWidth(adaptive)
        val viewportHeight = image.getViewportHeight(adaptive)
        val extraPadding = if ((adaptivePadding && adaptive) || addExtraPadding) viewportHeight / 10f else 0f
        val verticalPadding = ((image.height - viewportHeight) / 2f + extraPadding).roundToInt()

        when (gravity) {
            Gravity.TOP, Gravity.BOTTOM -> Unit
            Gravity.TOPRIGHT -> graphics.transform = AffineTransform.getRotateInstance(
                Math.toRadians(45.0),
                image.width / 2.0,
                image.height / 2.0
            )
            Gravity.TOPLEFT -> graphics.transform = AffineTransform.getRotateInstance(
                Math.toRadians(-45.0),
                image.width / 2.0,
                image.height / 2.0
            )
        }

        // calculate label size
        val frc = FontRenderContext(graphics.transform, true, true)
        val maxLabelWidth = calculateMaxLabelWidth(viewportWidth)
        val maxLabelHeight = (viewportHeight / 6f).roundToInt()
        graphics.font = getFont(viewportHeight, maxLabelWidth, maxLabelHeight, frc)

        val textBounds = graphics.font.getStringBounds(label, frc)
        val textHeight = textBounds.height.toFloat()
        val textPadding = textHeight / 10f
        val labelHeight = (textHeight + textPadding * 2f).roundToInt()

        // calculate the drawing rectangle
        val background = when (gravity) {
            Gravity.TOP,
            Gravity.TOPLEFT,
            Gravity.TOPRIGHT ->
                Rectangle(0, verticalPadding, image.width, labelHeight)

            Gravity.BOTTOM ->
                Rectangle(0, image.height - labelHeight - verticalPadding, image.width, labelHeight)
        }

        // draw the ribbon
        if (drawingOptions.contains(DrawingOption.IGNORE_TRANSPARENT_PIXELS) && !adaptive) {
            graphics.composite = AlphaComposite.getInstance(AlphaComposite.SRC_IN, 1f)
        }
        graphics.color = ribbonColor
        graphics.fillRect(background.x, background.y, background.width, background.height)

        // draw the label
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.color = labelColor
        graphics.drawString(
            label,
            (background.centerX - textBounds.centerX).toFloat(),
            (background.centerY - textBounds.centerY).toFloat(),
        )
        graphics.dispose()
    }

    private fun getFont(imageHeight: Int, maxLabelWidth: Int, maxLabelHeight: Int, frc: FontRenderContext): Font {
        // User-defined text size
        if (textSizeRatio != null) {
            return font.deriveFont((imageHeight * textSizeRatio).roundToInt().toFloat())
        }

        return (maxLabelHeight downTo 0).asSequence()
            .map { size -> font.deriveFont(size.toFloat()) }
            .first { font ->
                val bounds = font.getStringBounds(label, frc)
                bounds.width < maxLabelWidth
            }
    }

    companion object {
        private fun calculateMaxLabelWidth(y: Int) =
            (y * sqrt(2.0)).roundToInt()
    }
}
