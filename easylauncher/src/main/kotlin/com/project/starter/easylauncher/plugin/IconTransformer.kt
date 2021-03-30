package com.project.starter.easylauncher.plugin

import com.project.starter.easylauncher.filter.EasyLauncherFilter
import com.project.starter.easylauncher.plugin.models.toSize
import groovy.util.XmlSlurper
import java.awt.image.BufferedImage
import java.io.File
import java.util.Locale
import javax.imageio.ImageIO
import kotlin.math.roundToInt

internal fun File.transformPng(outputFile: File, filters: List<EasyLauncherFilter>, adaptive: Boolean) {
    val image = ImageIO.read(this)
    filters.forEach { it.apply(image, adaptive = adaptive) }
    outputFile.parentFile.mkdirs()
    ImageIO.write(image, "png", outputFile)
}

internal fun File.transformXml(outputFile: File, minSdkVersion: Int, filters: List<EasyLauncherFilter>) {
    val iconXml = XmlSlurper().parse(this)
    val width = iconXml.property("@android:width")?.toSize().let(::requireNotNull)
    val height = iconXml.property("@android:height")?.toSize().let(::requireNotNull)

    val drawableRoot = outputFile.parentFile // eg. debug/drawable/

    val layers = filters.mapIndexed { index, filter ->
        val filterId = "${filter::class.java.simpleName.toLowerCase(Locale.ROOT)}_$index"
        val resourceName = "${filterId}_${outputFile.nameWithoutExtension}"

        densities.forEach { (qualifier, multiplier) ->
            val overlay = BufferedImage(
                (width.value * multiplier).roundToInt(),
                (height.value * multiplier).roundToInt(),
                BufferedImage.TYPE_INT_ARGB
            )
            filter.apply(overlay, adaptive = true)

            val qualifiedRoot = drawableRoot.parentFile.resolve("${drawableRoot.normalizedName}-$qualifier")
            val qualifiedOverlayFile = qualifiedRoot.resolve("$resourceName.png").also { it.mkdirs() }
            ImageIO.write(overlay, "png", qualifiedOverlayFile)
        }

        resourceName
    }
        .joinToString(separator = "\n") {
            """
            |    <item android:drawable="@${outputFile.parentFile.normalizedName}/$it" />
            |""".trimMargin()
        }
    val versionSuffix = if (minSdkVersion >= ANDROID_OREO) "" else "-v26"
    val v26DrawableRoot = drawableRoot.parentFile.resolve("${drawableRoot.normalizedName}-anydpi$versionSuffix")

    copyTo(v26DrawableRoot.resolve("easy_$name"), overwrite = true)
    v26DrawableRoot.resolve(outputFile.name).writeText(
        """
        |<?xml version="1.0" encoding="utf-8"?>
        |<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
        |
        |    <item android:drawable="@drawable/easy_$nameWithoutExtension" />
        |
        |$layers
        |</layer-list>
        |""".trimMargin()
    )
}

/**
 * Fallback for a setup currently considered invalid. Just to avoid failing the build.
 * Reference: https://github.com/usefulness/easylauncher-gradle-plugin/issues/78
 */
private val File.normalizedName
    get() = when {
        name.contains("-v2") -> "drawable"
        name.contains("dpi") -> "drawable"
        else -> name
    }

internal const val ANDROID_OREO = 26

@Suppress("MagicNumber")
private val densities = mapOf(
    "ldpi" to 0.75,
    "mdpi" to 1.00,
    "hdpi" to 1.5,
    "xhdpi" to 2.00,
    "xxhdpi" to 3.00,
    "xxxhdpi" to 4.00
)
