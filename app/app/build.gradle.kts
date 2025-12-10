plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.alessandrolattao.lanotifica"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.alessandrolattao.lanotifica"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        checkAllWarnings = true
        checkDependencies = true

        // Strict rules for modern apps
        enable += listOf(
            "Interoperability",
            "NewerVersionAvailable",
            "ObsoleteSdkInt",
            "Performance",
            "Security",
            "Usability",
        )

        // Disable rules that conflict with our setup
        disable += listOf(
            "MissingTranslation",       // Single language app
            "UnusedResources",          // False positives with Compose
            "IconMissingDensityFolder", // Using vector drawables
            "GradleDependency",         // We manage versions manually
            "NewerVersionAvailable",    // We manage versions manually
            "AndroidGradlePluginVersion", // We manage Gradle version manually
            "OldTargetApi",             // We target the SDK we tested with
            "BatteryLife",              // App needs battery optimization bypass
            "LogConditional",           // Debug logs are useful for this app
            "SyntheticAccessor",        // Minor performance, not critical
            "UseKtx",                   // Style preference, not critical
            "StaticFieldLeak",          // HealthMonitor uses applicationContext
            "UnsafeOptInUsageError",    // Camera API experimental features are stable
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    implementation(libs.datastore.preferences)

    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.mlkit.barcode)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}