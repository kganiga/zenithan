package com.zennature.wallpaper

import com.zennature.wallpaper.data.ASPECT_RATIOS
import com.zennature.wallpaper.data.AspectRatioType
import com.zennature.wallpaper.data.CircadianId
import com.zennature.wallpaper.data.EditorSettings
import com.zennature.wallpaper.data.LicenseInfo
import com.zennature.wallpaper.data.MeshPresetId
import com.zennature.wallpaper.data.ProviderType
import com.zennature.wallpaper.data.WallpaperItem
import com.zennature.wallpaper.data.circadianSlot
import com.zennature.wallpaper.data.resolutionForRatio
import com.zennature.wallpaper.export.attributionLine
import com.zennature.wallpaper.export.generateAttributionText
import com.zennature.wallpaper.export.sanitizedTitle
import com.zennature.wallpaper.render.MESH_PRESETS
import com.zennature.wallpaper.render.generateMeshColors
import com.zennature.wallpaper.render.generateMeshNodes
import com.zennature.wallpaper.render.hexToHsl
import com.zennature.wallpaper.render.hexToRgb
import com.zennature.wallpaper.render.hslToHex
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Port of src/tests/runTests.ts, plus serialization compatibility with the web build's JSON. */
class CoreLogicTest {

    private fun item(id: String, provider: ProviderType, license: LicenseInfo, title: String = "Misty Cedar Forest") = WallpaperItem(
        id = id, title = title, thumbnailUrl = "https://example.com/$id-thumb.jpg", fullUrl = "https://example.com/$id.jpg",
        provider = provider, sourcePageUrl = "https://commons.wikimedia.org/wiki/File:Misty_Cedar.jpg",
        width = 1200, height = 1800, aspectRatio = 2.0 / 3, category = "forests-moss", license = license,
    )

    private val ccby = LicenseInfo(
        code = "CC-BY-4.0", name = "Creative Commons Attribution 4.0 International",
        url = "https://creativecommons.org/licenses/by/4.0/", author = "Hiroshi Tanaka",
        attributionRequired = true, commercialUseAllowed = true, derivativeWorksAllowed = true, verified = true,
    )
    private val cc0 = LicenseInfo(
        code = "CC0", name = "Creative Commons Zero", url = "https://creativecommons.org/publicdomain/zero/1.0/",
        author = "Open Marine Library", attributionRequired = false, commercialUseAllowed = true, derivativeWorksAllowed = true, verified = true,
    )

    @Test fun aspectRatioResolutions() {
        assertEquals(1440 to 2560, resolutionForRatio(AspectRatioType.MOBILE).let { it.width to it.height })
        assertEquals(1080 to 2400, resolutionForRatio(AspectRatioType.FLAGSHIP).let { it.width to it.height })
        assertEquals(1536 to 2048, resolutionForRatio(AspectRatioType.TABLET).let { it.width to it.height })
        assertEquals(1440 to 1440, resolutionForRatio(AspectRatioType.SQUARE).let { it.width to it.height })
        assertEquals(1080 to 2400, resolutionForRatio(AspectRatioType.FLAGSHIP, highRes = false).let { it.width to it.height })
    }

    @Test fun ccByAttribution() {
        val text = generateAttributionText(item("test-1", ProviderType.WIKIMEDIA, ccby))
        assertTrue(text.contains("Misty Cedar Forest"))
        assertTrue(text.contains("Hiroshi Tanaka"))
        assertTrue(text.contains("CC-BY-4.0"))
        assertTrue(text.contains("https://commons.wikimedia.org/wiki/File:Misty_Cedar.jpg"))
        assertTrue(text.contains("YES (Attribution included in this file)"))
        assertTrue(text.contains("Source Provider: WIKIMEDIA"))
    }

    @Test fun publicDomainAttribution() {
        val text = generateAttributionText(item("test-2", ProviderType.LOCAL, cc0))
        assertTrue(text.contains("NO (Public Domain / CC0)"))
        assertTrue(text.contains("Allowed"))
    }

    @Test fun attributionLineAndSlug() {
        val w = item("x", ProviderType.NASA, ccby)
        assertEquals(
            "Image: \"Misty Cedar Forest\" by Hiroshi Tanaka via NASA (Creative Commons Attribution 4.0 International). https://commons.wikimedia.org/wiki/File:Misty_Cedar.jpg",
            attributionLine(w),
        )
        assertEquals("misty-cedar-forest", sanitizedTitle("Misty  Cedar Forest"))
        assertEquals(32, sanitizedTitle("a".repeat(50)).length)
    }

    @Test fun aspectRatioCatalog() {
        assertEquals(5, ASPECT_RATIOS.size)
        val ids = ASPECT_RATIOS.map { it.id.id }
        assertTrue(ids.contains("9:16") && ids.contains("20:9"))
    }

    @Test fun circadianSlots() {
        assertEquals(CircadianId.DAWN, circadianSlot(5).id)
        assertEquals(CircadianId.DAWN, circadianSlot(8).id)
        assertEquals(CircadianId.DAYLIGHT, circadianSlot(9).id)
        assertEquals(CircadianId.DAYLIGHT, circadianSlot(16).id)
        assertEquals(CircadianId.TWILIGHT, circadianSlot(17).id)
        assertEquals(CircadianId.NIGHT, circadianSlot(23).id)
        assertEquals(CircadianId.NIGHT, circadianSlot(0).id)
        for (h in 0..23) {
            val s = circadianSlot(h)
            assertTrue(s.label.isNotEmpty() && s.hourRange.isNotEmpty() && s.recommendedCategory.isNotEmpty() && s.japaneseTitle.isNotEmpty())
        }
    }

    @Test fun meshGenerator() {
        assertEquals(6, MESH_PRESETS.size)
        val ids = MESH_PRESETS.map { it.id }
        assertTrue(ids.containsAll(listOf(MeshPresetId.BIOPHILIC, MeshPresetId.DAWN, MeshPresetId.GOLDEN)))

        val hex = "#556952"
        val hsl = hexToHsl(hex)
        assertTrue(hsl.h > 90 && hsl.h < 130)
        val a = hexToRgb(hex)
        val b = hexToRgb(hslToHex(hsl.h.toDouble(), hsl.s.toDouble(), hsl.l.toDouble()))
        assertTrue(maxOf(abs(a.r - b.r), abs(a.g - b.g), abs(a.b - b.b)) <= 2)

        MeshPresetId.entries.forEach { p ->
            val colors = generateMeshColors(hex, p)
            assertEquals(5, colors.size)
            colors.forEach { assertTrue("bad hex $it", Regex("^#[0-9a-fA-F]{6}$").matches(it)) }
        }
        assertEquals(hex.lowercase(), generateMeshColors(hex, MeshPresetId.BIOPHILIC)[0].lowercase())
        val n42 = generateMeshNodes(hex, MeshPresetId.TWILIGHT, 42)
        val n99 = generateMeshNodes(hex, MeshPresetId.TWILIGHT, 99)
        assertEquals(5, n42.size)
        assertTrue(n42[0].x != n99[0].x || n42[0].y != n99[0].y)
        assertEquals(n42, generateMeshNodes(hex, MeshPresetId.TWILIGHT, 42))
    }

    /** Projects saved by the web build used these exact wire values; the native models must read them. */
    @Test fun editorSettingsWireFormat() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val encoded = json.encodeToString(EditorSettings.serializer(), EditorSettings())
        listOf("\"20:9\"", "\"ruleOfThirds\"", "\"lockscreen\"", "\"soft-light\"", "\"biophilic\"", "\"serif\"", "\"bottom\"", "\"md\"", "\"none\"")
            .forEach { assertTrue("missing $it in $encoded", encoded.contains(it)) }
        val web = """{"cropRatio":"9:16","zoom":1.4,"panX":-12,"panY":8,"brightness":110,"contrast":95,"saturation":80,
            "warmth":-6,"blur":2,"vignette":20,"grain":4,"washPreset":"twilight","washIntensity":40,
            "meshGradient":{"enabled":true,"preset":"golden","intensity":70,"blur":30,"blendMode":"backdrop","seed":59,"customPrimaryColor":"#aabbcc"},
            "textOverlay":{"enabled":true,"text":"Hi","subtext":"","kanji":"静寂","fontStyle":"sans","position":"top","size":"lg","color":"#FFFFFF","opacity":0.9,"letterSpacing":"wide","showJapaneseMark":true},
            "showClockPreview":true,"showWidgetsPreview":false,"gridOverlay":"goldenRatio","uiPreviewMode":"homescreen",
            "washiTexture":10,"komorebiRays":20,"mistGradient":"top","mistIntensity":50}"""
        val s = json.decodeFromString(EditorSettings.serializer(), web)
        assertEquals(AspectRatioType.MOBILE, s.cropRatio)
        assertEquals(1.4f, s.zoom)
        assertEquals(MeshPresetId.GOLDEN, s.meshGradient.preset)
        assertEquals("#aabbcc", s.meshGradient.customPrimaryColor)
        assertEquals("静寂", s.textOverlay.kanji)
    }
}
