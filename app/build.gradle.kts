import java.net.URL

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val assetsDir = file("src/main/assets/www")

tasks.register<DefaultTask>("downloadAssets") {
    group = "build"
    description = "Downloads external font assets locally"
    notCompatibleWithConfigurationCache("Downloads external files")

    val faUrl = "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/7.0.0/css/all.min.css"
    val faWebfonts = listOf(
        "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/7.0.0/webfonts/fa-brands-400.woff2" to "fa-brands-400.woff2",
        "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/7.0.0/webfonts/fa-solid-900.woff2" to "fa-solid-900.woff2",
        "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/7.0.0/webfonts/fa-regular-400.woff2" to "fa-regular-400.woff2"
    )
    val interUrls = listOf(
        "https://fonts.gstatic.com/s/inter/v20/UcC73FwrK3iLTeHuS_nVMrMxCp50SjIa1ZL7.woff2" to "inter-400.woff2",
        "https://fonts.gstatic.com/s/inter/v20/UcC73FwrK3iLTeHuS_nVMrMxCp50SjIa25L7SUc.woff2" to "inter-500.woff2",
        "https://fonts.gstatic.com/s/inter/v20/UcC73FwrK3iLTeHuS_nVMrMxCp50SjIa1pL7SUc.woff2" to "inter-600.woff2"
    )

    doFirst {
        val faDest = file("${assetsDir}/font-awesome.min.css")
        println("Downloading Font Awesome...")
        faDest.parentFile?.mkdirs()
        // Download using URL directly
        val urlConnection = URL(faUrl).openConnection()
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0")
        faDest.writeBytes(urlConnection.getInputStream().readBytes())

        // Download Font Awesome webfonts
        val faFontsDir = file("${assetsDir}/webfonts")
        faFontsDir.mkdirs()
        println("Downloading Font Awesome webfonts...")
        for ((url, name) in faWebfonts) {
            val dest = file("${faFontsDir}/$name")
            val urlConnection = URL(url).openConnection()
            urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0")
            dest.writeBytes(urlConnection.getInputStream().readBytes())
        }
        // Update CSS paths to point to local webfonts (keep both woff2 and ttf)
        println("Fixing Font Awesome CSS paths...")
        val faCss = faDest.readText()
        val fixedCss = faCss.replace("../webfonts/", "webfonts/")
        faDest.writeText(fixedCss)
    }

    doLast {
        val fontDir = file("${assetsDir}/fonts")
        fontDir.mkdirs()
        println("Downloading Inter fonts...")
        for ((url, name) in interUrls) {
            val dest = file("${fontDir}/$name")
            val urlConnection = URL(url).openConnection()
            urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0")
            dest.writeBytes(urlConnection.getInputStream().readBytes())
        }
        println("Assets downloaded successfully!")
    }
}

tasks.matching { it.name.startsWith("merge") || it.name.startsWith("assemble") }.configureEach {
    dependsOn("downloadAssets")
}

android {
    // ── Change these to match your app ────────────────
    namespace = "com.s17labs.pebbledo"
    // ──────────────────────────────────────────────────

    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // ── Change these to match your app ────────────
        applicationId = "com.s17labs.pebbledo"
        versionCode = 1
        versionName = "1.0.0"
        // ──────────────────────────────────────────────

        minSdk = 26
        targetSdk = 35
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    // WebView is part of the Android SDK — no extra dependency needed
}
