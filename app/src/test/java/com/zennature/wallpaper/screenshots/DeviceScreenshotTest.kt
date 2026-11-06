package com.zennature.wallpaper.screenshots

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.zennature.wallpaper.MainActivity
import com.zennature.wallpaper.ui.MainViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders every screen of the real app on several device sizes, so layout problems show up without a device.
 * Only runs with -PzenScreenshots (CI "screenshots" job); images land in app/build/screens/.
 * Network is live on CI, so remote thumbnails may or may not have loaded when a frame is captured.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class DeviceScreenshotTest(private val device: String, private val qualifiers: String) {

    @get:Rule
    val compose = createEmptyComposeRule()

    private fun shot(name: String) {
        settle()
        captureScreenRoboImage("build/screens/$device/$name.png")
    }

    private fun settle(ms: Long = 2500) {
        val end = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < end) {
            compose.waitForIdle()
            Thread.sleep(100)
        }
    }

    private fun tab(label: String) {
        compose.onNodeWithContentDescription(label).performClick()
        settle(1500)
    }

    @Test
    fun allScreens() {
        RuntimeEnvironment.setQualifiers(qualifiers)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            runCatching {
                compose.waitUntil(30_000) {
                    compose.onAllNodesWithContentDescription("Save to Favorites").fetchSemanticsNodes().size >= 2
                }
            }
            shot("1-wallpapers")
            tab("Discover"); shot("2-discover")
            tab("Studio"); shot("3-studio")
            tab("Saved"); shot("4-saved")
            tab("Wallpapers")

            lateinit var main: MainViewModel
            scenario.onActivity { main = ViewModelProvider(it)[MainViewModel::class.java] }
            compose.runOnUiThread { main.openDetail(main.featured) }
            shot("5-detail")
            compose.runOnUiThread { main.closeDetail() }
            tab("Studio")
            compose.runOnUiThread { main.openExport() }
            shot("6-export")
            compose.runOnUiThread { main.closeExport() }
            compose.runOnUiThread { main.toggleTheme() }
            tab("Wallpapers"); shot("7-wallpapers-dark")
            tab("Studio"); shot("8-studio-dark")
            compose.runOnUiThread { main.toggleTheme() }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun devices(): List<Array<String>> = listOf(
            arrayOf("a-small-phone-360x640", "w360dp-h640dp-port-xhdpi"),
            arrayOf("b-phone-411x891", "w411dp-h891dp-port-xxhdpi"),
            arrayOf("c-phone-landscape-891x411", "w891dp-h411dp-land-xxhdpi"),
            arrayOf("d-foldable-673x841", "w673dp-h841dp-port-xhdpi"),
            arrayOf("e-tablet-800x1280", "w800dp-h1280dp-port-xhdpi"),
            arrayOf("f-tablet-landscape-1280x800", "w1280dp-h800dp-land-xhdpi"),
        )
    }
}
