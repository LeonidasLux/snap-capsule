// Root build configuration for 闪念胶囊 (Snap Capsule)
// Version matrix (Kuikly 2.27.0-2.1.21):
//   Kotlin 2.1.21 · AGP 7.4.2 · KSP 2.1.21-2.0.1 · Gradle 7.6.3 (wrapper) · JDK 17

plugins {
    kotlin("multiplatform") version "2.1.21" apply false
    kotlin("android") version "2.1.21" apply false
    kotlin("plugin.compose") version "2.1.21" apply false
    kotlin("plugin.serialization") version "2.1.21" apply false
    id("com.android.application") version "7.4.2" apply false
    id("com.android.library") version "7.4.2" apply false
    id("com.google.devtools.ksp") version "2.1.21-2.0.1" apply false
    // Kuikly gradle 插件。官方仓库 buildSrc 亦固定此版本（配 Kotlin 2.1.x）；
    // 更新的插件(2.27)其 .kotlin_module 与 Gradle7.6 内嵌 Kotlin(1.8) 的脚本编译器不兼容。
    id("com.tencent.kuikly-open.kuikly") version "2.14.1-2.0.21" apply false
}

allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
        // AGP 7.4 vs Kotlin 2.1 jvmTarget 校验放宽，避免构建失败（官方同款处理）
        jvmTargetValidationMode.set(org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode.WARNING)
    }
}
