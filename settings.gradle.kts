pluginManagement {
    repositories {
        // 注：刻意不含 gradlePluginPortal —— 其 CDN 在本网络下会长时间停滞；
        // 所需插件均能由 google()/mavenCentral()/腾讯镜像解析。
        google()
        mavenCentral()
        maven {
            name = "TencentNexusMaven"
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
        maven {
            name = "TencentNexusPlugins"
            url = uri("https://mirrors.tencent.com/nexus/repository/gradle-plugins/")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            name = "TencentNexusMaven"
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
    }
}

rootProject.name = "snap-capsule"

include(":shared")
include(":androidApp")
include(":h5App")
