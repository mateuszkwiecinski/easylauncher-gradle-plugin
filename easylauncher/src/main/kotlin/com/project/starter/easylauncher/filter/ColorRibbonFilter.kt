package com.project.starter.easylauncher.filter

import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import kotlin.math.pow
import kotlin.math.sqrt

@Suppress("MagicNumber")
class ColorRibbonFilter @JvmOverloads constructor(
    private val label: String?,
    private val ribbonColor: Color,
    private val labelColor: Color = Color.WHITE,
    private val gravity: Gravity = Gravity.TOPLEFT,
    private val textSizeRatio: Float = -1f
) : EasyLauncherFilter {

    private val fontName = "DEFAULT"
    private val fontStyle = Font.PLAIN

    enum class Gravity {
        TOP, BOTTOM, TOPLEFT, TOPRIGHT
    }

    @Suppress("ComplexMethod")
    override fun apply(image: BufferedImage, adaptive: Boolean) {
        val graphics = image.graphics as Graphics2D
        when (gravity) {
            Gravity.TOP, Gravity.BOTTOM -> Unit
            Gravity.TOPRIGHT -> graphics.transform = AffineTransform.getRotateInstance(
                Math.toRadians(45.0),
                image.width.toDouble(),
                0.0
            )
            Gravity.TOPLEFT -> graphics.transform = AffineTransform.getRotateInstance(Math.toRadians(-45.0))
        }
        val frc = FontRenderContext(graphics.transform, true, true)
        // calculate the rectangle where the label is rendered
        val maxLabelWidth = calculateMaxLabelWidth(image.height / 2)
        graphics.font = getFont(image.height, maxLabelWidth, frc)
        val textBounds = graphics.font.getStringBounds(label ?: "", frc)
        val textHeight = textBounds.height.toInt()
        val textPadding = textHeight / 10
        val labelHeight = textHeight + textPadding * 2

        // update y gravity after calculating font size
        val yGravity = when (gravity) {
            Gravity.TOP -> if (adaptive) image.height / 4 else 0
            Gravity.BOTTOM -> image.height - labelHeight - (if (adaptive) image.height / 4 else 0)
            Gravity.TOPRIGHT, Gravity.TOPLEFT -> image.height / (if (adaptive) 2 else 4)
        }

        // draw the ribbon
        graphics.color = ribbonColor
        if (gravity == Gravity.TOP || gravity == Gravity.BOTTOM) {
            graphics.fillRect(0, yGravity, image.width, labelHeight)
        } else if (gravity == Gravity.TOPRIGHT) {
            graphics.fillRect(0, yGravity, image.width * 2, labelHeight)
        } else {
            graphics.fillRect(-image.width, yGravity, image.width * 2, labelHeight)
        }
        if (label != null) {
            // draw the label
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.color = labelColor
            val fm = graphics.fontMetrics
            if (gravity == Gravity.TOP || gravity == Gravity.BOTTOM) {
                graphics.drawString(
                    label,
                    image.width / 2 - textBounds.width.toInt() / 2,
                    yGravity + fm.ascent
                )
            } else if (gravity == Gravity.TOPRIGHT) {
                graphics.drawString(
                    label,
                    image.width - textBounds.width.toInt() / 2,
                    yGravity + fm.ascent
                )
            } else {
                graphics.drawString(
                    label,
                    (-textBounds.width).toInt() / 2,
                    yGravity + fm.ascent
                )
            }
        }
        graphics.dispose()
    }

    private fun getFont(imageHeight: Int, maxLabelWidth: Int, frc: FontRenderContext): Font {
        // User-defined text size
        if (textSizeRatio != -1f) {
            return Font(fontName, fontStyle, (imageHeight * textSizeRatio).toInt())
        }
        var max = imageHeight / 8
        var min = 0

        // Label not set
        if (label == null) {
            return Font(fontName, fontStyle, max / 2)
        }

        // Automatic calculation: as big as possible
        var size = max
        for (i in 0..9) {
            val mid = (max + min) / 2
            if (mid == size) {
                break
            }
            val font = Font(fontName, fontStyle, mid)
            val labelBounds = font.getStringBounds(label, frc)
            if (labelBounds.width.toInt() > maxLabelWidth) {
                max = mid
            } else {
                min = mid
            }
            size = mid
        }
        return Font(fontName, fontStyle, size)
    }

    companion object {
        private fun calculateMaxLabelWidth(y: Int): Int {
            return sqrt(y.toDouble().pow(2.0) * 2).toInt()
        }
    }
}
