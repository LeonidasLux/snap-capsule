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

// ── 构建信息注入：编译前生成 BuildInfo.kt（BUILD_TIME = 本次构建时刻），供 header 在 debug 下显示 ──
val buildInfoDir = layout.buildDirectory.dir("generated/buildInfo/kotlin")

val generateBuildInfo by tasks.registering {
    val outDir = buildInfoDir
    outputs.dir(outDir)
    doLast {
        // 注：Kotlin DSL 脚本作用域无法解析 java.time/java.util 等（本环境），故只注入 epoch 毫秒，
        // 展示文本由 commonMain 用 kotlinx-datetime 格式化（见 ui/util/TimeText.stamp）。
        val epoch = System.currentTimeMillis()
        val file = outDir.get().file("com/snapcapsule/platform/BuildInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            // 由 Gradle generateBuildInfo 自动生成：最近一次编译的 epoch 毫秒。请勿手改。
            package com.snapcapsule.platform

            object BuildInfo {
                const val BUILD_EPOCH_MS: Long = ${epoch}L
            }
            """.trimIndent() + "\n"
        )
    }
}

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
            kotlin.srcDir(buildInfoDir)  // 注入的 BuildInfo.kt 编译进 commonMain（android + js 目标都可见）
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

// 编译 / KSP 前先跑 generateBuildInfo，避免 UI 引用到尚未生成的 BuildInfo.kt
tasks.configureEach {
    val n = name
    if ((n.startsWith("compile") && n.contains("Kotlin")) || n.startsWith("ksp")) {
        dependsOn(generateBuildInfo)
    }
}
