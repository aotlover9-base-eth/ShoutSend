import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "app.shoutsend"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.shoutsend"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    flavorDimensions += "audience"
    productFlavors {
        create("dev") {
            dimension = "audience"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "ShoutSend (Dev)")
        }
        create("user") {
            dimension = "audience"
            resValue("string", "app_name", "ShoutSend")
        }
    }

    val signingPropsFile = file("${System.getProperty("user.home")}/.shoutsend-signing/signing.properties")
    val hasReleaseSigning = signingPropsFile.exists() || (
        System.getenv("RELEASE_STORE_FILE") != null &&
        System.getenv("RELEASE_STORE_PASSWORD") != null &&
        System.getenv("RELEASE_KEY_ALIAS") != null &&
        System.getenv("RELEASE_KEY_PASSWORD") != null
    )

    val releaseSigning = if (hasReleaseSigning) {
        val props = Properties()
        if (signingPropsFile.exists()) {
            signingPropsFile.inputStream().use { props.load(it) }
        }
        signingConfigs.create("release") {
            val storePath = props.getProperty("RELEASE_STORE_FILE") ?: System.getenv("RELEASE_STORE_FILE") ?: ""
            storeFile = file(storePath)
            storePassword = props.getProperty("RELEASE_STORE_PASSWORD") ?: System.getenv("RELEASE_STORE_PASSWORD") ?: ""
            keyAlias = props.getProperty("RELEASE_KEY_ALIAS") ?: System.getenv("RELEASE_KEY_ALIAS") ?: ""
            keyPassword = props.getProperty("RELEASE_KEY_PASSWORD") ?: System.getenv("RELEASE_KEY_PASSWORD") ?: ""
        }
    } else {
        null
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = releaseSigning
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        resValues = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

tasks.register("assembleBothDebug") {
    dependsOn("assembleDevDebug", "assembleUserDebug")
    description = "Builds both Dev and User debug APKs"
    group = "build"
}

dependencies {
    implementation(project(":modem"))
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.datastore.preferences)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.compose.ui.tooling)
}

val checkNoHardcodedStyle = tasks.register<app.shoutsend.buildtools.tasks.CheckNoHardcodedStyleTask>("checkNoHardcodedStyle") {
    sourceDir.set(file("src"))
    allowlistFile.set(rootProject.file("config/style-allowlist.txt"))
}

val verifyUserApkClean = tasks.register<app.shoutsend.buildtools.tasks.VerifyUserApkCleanTask>("verifyUserApkClean") {
    dependsOn("assembleUserDebug")
    apkFile.set(layout.buildDirectory.file("outputs/apk/user/debug/app-user-debug.apk"))
    markersFile.set(rootProject.file("config/dev-markers.txt"))
}

val verifyPermissions = tasks.register<app.shoutsend.buildtools.tasks.VerifyPermissionsTask>("verifyPermissions") {
    dependsOn("processDevDebugManifest", "processUserDebugManifest")
    manifestFiles.from(
        layout.buildDirectory.file("intermediates/merged_manifests/devDebug/processDevDebugManifest/AndroidManifest.xml"),
        layout.buildDirectory.file("intermediates/merged_manifests/userDebug/processUserDebugManifest/AndroidManifest.xml")
    )
}

val verifyChirpRules = tasks.register("verifyChirpRules") {
    group = "verification"
    description = "Runs all ShoutSend architectural guards (style, user APK purity, and offline permissions)."
    dependsOn(checkNoHardcodedStyle, verifyUserApkClean, verifyPermissions, ":modem:checkModemPurity")
}

tasks.named("check") {
    dependsOn(verifyChirpRules)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
