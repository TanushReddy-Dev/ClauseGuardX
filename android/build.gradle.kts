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
    }

    toolchain {
        kotlin {
            version = "1.9.0"
        }
    }
}

dependencies {
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
}

sqldelight {
    database("ClauseGuardDatabase") {
        package = "com.clauseguard.core.db"
    }
}