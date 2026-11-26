plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.fnshield"
    buildFeatures { buildConfig = true }
    compileSdk = 34
    defaultConfig {
        applicationId = "com.fnshield"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "2.1.0"
        buildConfigField("Boolean", "NATION_MODE", (project.findProperty("fncMode") == "nation").toString())
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles("proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        kotlinOptions { jvmTarget = "17" }
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
}
