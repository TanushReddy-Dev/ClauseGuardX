plugins {
    kotlin("android")
    kotlin("serialization")
    id("app.cash.sqldelight")
}

kotlin {
    android {
        namespace = "com.clauseguard.core"
        compileSdk = 34
        defaultConfig {
            minSdk = 24
            targetSdk = 34
        }
        buildFeatures {
            compose = true
        }
        composeOptions {
            kotlinCompilerExtensionVersion = "1.5.3"
        }
    }

    toolchain {
        kotlin {
            version = "1.9.0"
        }
    }
}

dependencies {
    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Ktor - Networking
    implementation("io.ktor:ktor-client-core:2.3.9")
    implementation("io.ktor:ktor-client-android:2.3.9")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.9")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.9")

    // SQLDelight - Local Persistence
    implementation("com.squareup.sqldelight:sqldelight-android:2.0.0")
    implementation("com.squareup.sqldelight:sqlite-driver:2.0.0")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("io.ktor:ktor-client-mock:2.3.9")
    testImplementation("app.cash.sqldelight:sqldelight-h2:2.0.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

sqldelight {
    database("ClauseGuardDatabase") {
        package = "com.clauseguard.core.db"
    }
}