package com.project.starter.easylauncher.plugin

import com.project.starter.easylauncher.plugin.utils.WithGradleProjectTest
import com.project.starter.easylauncher.plugin.utils.buildScript
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

internal class EasyLauncherPluginTest : WithGradleProjectTest() {

    lateinit var moduleRoot: File

    @BeforeEach
    fun setUp() {
        rootDirectory.apply {
            resolve("settings.gradle").writeText("""include ":app" """)

            moduleRoot = resolve("app") {
                resolve("src/main/AndroidManifest.xml") {
                    writeText(
                        """
                        <manifest package="com.example.app" />
                        
                        """.trimIndent()
                    )
                }
            }
        }
    }

    @Test
    fun `applies plugin with minimal setup`() {
        moduleRoot.resolve("build.gradle").buildScript(
            androidBlock = {
                """
            buildTypes {
                debug { }
                superType { }
                release { }
            }
            flavorDimensions "version"
            productFlavors {
                demo { dimension "version" }
                full { dimension "version" }
            }
                """.trimIndent()
            }
        )
        val result = runTask("assembleDemoDebug")

        assertThat(result.task(":app:easylauncherDemoDebug")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `does not add task for non debuggable variants`() {
        moduleRoot.resolve("build.gradle").buildScript(
            androidBlock = {
                """
            buildTypes {
                debug { }
                superType { debuggable false }
                release { }
            }
            flavorDimensions "version"
            productFlavors {
                demo { dimension "version" }
                full { dimension "version" }
            }
                """.trimIndent()
            }
        )
        val result = runTask("assembleDemoRelease", "assembleFullSuperType")

        assertThat(result.task(":app:easylauncherDemoRelease")).isNull()
        assertThat(result.task(":app:easylauncherFullSuperType")).isNull()
    }

    @Test
    fun `generates proper tasks`() {
        moduleRoot.resolve("build.gradle").buildScript(
            androidBlock = {
                """
             buildTypes {
                    debug {
                        //Debuggable, will get a default ribbon in the launcher icon
                    }
                    beta {
                        //Debuggable, will get a default ribbon in the launcher icon
                        debuggable true
                    }
                    canary {
                        //Non-debuggable, will not get any default ribbon
                        debuggable false
                    }
                    release {
                        //Non-debuggable, will not get any default ribbon
                    }
                }
                flavorDimensions "xxx"
                productFlavors {
                    local { dimension "xxx" }
                    qa { dimension "xxx" }
                    staging { dimension "xxx" }
                    production { dimension "xxx" }
                }
                """.trimIndent()
            },
            easylauncherBlock = {
                """
                     productFlavors {
                        local {}
                        qa {
                            // Add one more filter to all `qa` variants
                            filters = redRibbonFilter()
                        }
                        staging {}
                        production {}
                    }
                    
                    buildTypes {
                        beta {
                            // Add two more filters to all `beta` variants
                            filters = [
                                    customColorRibbonFilter("#0000FF"),
                                    overlayFilter(new File("example-custom/launcherOverlay/beta.png"))
                            ]
                        }
                        canary {
                            // Remove ALL filters to `canary` variants
                            enable false
                        }
                        release {}
                    }
                    
                    variants {
                        productionDebug {
                            // OVERRIDE all previous filters defined for `productionDebug` variant
                            filters = orangeRibbonFilter("custom")
                        }
                    }
                """.trimIndent()
            }
        )
        val result = runTask("easylauncher")

        assertSoftly { softly ->
            listOf(
                "easylauncherLocalBeta",
                "easylauncherLocalDebug",
                "easylauncherProductionBeta",
                "easylauncherProductionDebug",
                "easylauncherQaBeta",
                "easylauncherQaDebug",
                "easylauncherQaRelease",
                "easylauncherStagingBeta",
                "easylauncherStagingDebug"
            ).forEach { taskName ->
                softly.assertThat(result.task(":app:$taskName")).isNotNull()
            }
        }
        assertSoftly { softly ->
            listOf(
                "easylauncherLocalCanary",
                "easylauncherLocalRelease",
                "easylauncherProductionCanary",
                "easylauncherProductionRelease",
                "easylauncherQaCanary",
                "easylauncherStagingCanary",
                "easylauncherStagingRelease"
            ).forEach { taskName ->
                softly.assertThat(result.task(":app:$taskName")).isNull()
            }
        }
    }
}