import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "es.altamirano.foto"
    compileSdk = 35

    defaultConfig {
        applicationId = "es.altamirano.foto"
        minSdk = 35          // Android 15: sólo para el Pixel 11 Pro (Android 17), sin compatibilidad hacia atrás
        targetSdk = 35
        versionCode = 2
        versionName = "0.2"
        ndk { abiFilters += "arm64-v8a" }   // FFmpeg trae librerías nativas: sólo las del Pixel
    }

    // La clave de release vive fuera del repositorio (ver CLAUDE.md).
    // Sin ella, la release sale sin firmar y la de depuración sigue funcionando.
    val keyFile = File(System.getProperty("user.home"), ".android/claves/fotodroid-release.properties")
    val releaseKey = if (keyFile.exists()) Properties().apply { keyFile.inputStream().use { load(it) } } else null
    signingConfigs {
        if (releaseKey != null) create("release") {
            storeFile = File(releaseKey.getProperty("storeFile"))
            storePassword = releaseKey.getProperty("storePassword")
            keyAlias = releaseKey.getProperty("keyAlias")
            keyPassword = releaseKey.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            if (releaseKey != null) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { jniLibs { useLegacyPackaging = false } }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.biometric)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.coroutines.android)
    implementation(libs.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.video)
    implementation(libs.telephoto.coil)
    implementation(libs.telephoto.zoomable)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ffmpeg)
    implementation(libs.reorderable)
    testImplementation(libs.junit)
}
