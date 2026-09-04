plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.snapcapsule.app"
    compileSdk = 34
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "com.snapcapsule.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

val kuiklyVersion = "2.27.0-2.1.21"

dependencies {
    implementation(project(":shared"))
    implementation("com.tencent.kuikly-open:core:$kuiklyVersion")
    implementation("com.tencent.kuikly-open:core-render-android:$kuiklyVersion")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
}
