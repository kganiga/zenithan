import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val appVersionName: String = (findProperty("zenVersionName") as String?)?.removePrefix("v")?.takeIf { it.isNotBlank() } ?: "2.0.0"
val appVersionCode: Int = appVersionName.split(".").map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
    .let { p -> p.getOrElse(0) { 0 } * 10000 + p.getOrElse(1) { 0 } * 100 + p.getOrElse(2) { 0 } }

// Output files are named after the app: ZenNature-2.0.0-release.apk / .aab
extensions.configure<org.gradle.api.plugins.BasePluginExtension> { archivesName.set("ZenNature-$appVersionName") }

android {
    namespace = "com.zennature.wallpaper"
    compileSdk = 36

    defaultConfig {
        // Same applicationId as the Capacitor build so this installs as an update.
        applicationId = "com.zennature.wallpaper"
        minSdk = 24
        targetSdk = 36
        // CI passes -PzenVersionName from the git tag (v2.1.0 -> 2.1.0). versionCode is derived from it
        // (major*10000 + minor*100 + patch) so every release is strictly increasing without manual bumps.
        versionName = appVersionName
        versionCode = appVersionCode
    }

    // Release signing: CI supplies ZEN_KEYSTORE_* environment variables (from GitHub secrets); locally the
    // gitignored keystore.properties is used. With neither, the release APK is left unsigned.
    val keystorePropsFile = rootProject.file("keystore.properties")
    val envKeystore = System.getenv("ZEN_KEYSTORE_FILE")?.takeIf { it.isNotBlank() && file(it).exists() }
    val releaseSigning = when {
        envKeystore != null -> signingConfigs.create("release") {
            storeFile = file(envKeystore)
            storePassword = System.getenv("ZEN_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ZEN_KEY_ALIAS")
            keyPassword = System.getenv("ZEN_KEY_PASSWORD")
        }
        keystorePropsFile.exists() -> {
            val props = Properties().apply { keystorePropsFile.inputStream().use { load(it) } }
            signingConfigs.create("release") {
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
        else -> null
    }

    buildTypes {
        release {
            signingConfig = releaseSigning
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Device-size screenshots (Robolectric + Roborazzi), run only with -PzenScreenshots.
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.withType<Test>().configureEach {
    if (project.hasProperty("zenScreenshots")) {
        systemProperty("roborazzi.test.record", "true")
        filter { includeTestsMatching("com.zennature.wallpaper.screenshots.*") }
        maxHeapSize = "3g"
    } else {
        exclude("**/screenshots/**")
    }
}
