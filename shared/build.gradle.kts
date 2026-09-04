import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("com.tencent.kuikly-open.kuikly")
}

group = "com.snapcapsule"
version = "0.1.0"

val kuiklyVersion = "2.27.0-2.1.21"

kotlin {
    androidTarget()

    js(IR) {
        // 业务 JS 包名，须与 kuikly { js { outputName } } 一致
        moduleName = "nativevue2"
        browser {
            webpackTask {
                outputFileName = "${moduleName}.js"
            }
            commonWebpackConfig {
                output?.library = null
                devtool = "source-map"
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.tencent.kuikly-open:core:$kuiklyVersion")
                implementation("com.tencent.kuikly-open:core-annotations:$kuiklyVersion")
                implementation("com.tencent.kuikly-open:compose:$kuiklyVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.9.0")
            }
        }
        val jsMain by getting
    }
}

// 与 Kuikly 官方 demo 一致的 ksp 参数：页面选择 + 本地 bundle 打包开关
ksp {
    arg("pageName", project.findProperty("pageName") as? String ?: "")
    arg("pageNameList", project.findProperty("pageNameList") as? String ?: "")
    arg("packLocalJsBundle", project.findProperty("packLocalJsBundle") as? String ?: "")
}

dependencies {
    compileOnly("com.tencent.kuikly-open:core-ksp:$kuiklyVersion") {
        add("kspAndroid", this)
        add("kspJs", this)
    }
}

android {
    namespace = "com.snapcapsule"
    compileSdk = 34
    buildToolsVersion = "36.0.0"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 21
        targetSdk = 30
    }
    sourceSets {
        named("main") {
            assets.srcDirs("src/commonMain/assets")
        }
    }
}

// Kuikly 插件配置
kuikly {
    js {
        outputName("nativevue2")
    }
}
