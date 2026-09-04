import java.nio.file.Paths

plugins {
    kotlin("multiplatform")
}

// 注：不在此声明 repositories —— 由 settings.gradle.kts 的 dependencyResolutionManagement 统一提供（含腾讯镜像）
val kuiklyVersion = "2.27.0-2.1.21"

kotlin {
    js(IR) {
        browser {
            webpackTask {
                outputFileName = "h5App.js"
            }
            commonWebpackConfig {
                output?.library = null
            }
        }
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation("com.tencent.kuikly-open.core-render-web:base:$kuiklyVersion")
                implementation("com.tencent.kuikly-open.core-render-web:h5:$kuiklyVersion")
            }
        }
    }
}

// 业务模块（shared）产出目录下的 zip 名
val businessPathName = "shared"
val bundleName = "nativevue2"

/**
 * Dev：把 shared Debug 业务包解压进 h5App 的 processedResources/page，
 * 使 jsBrowserDevelopmentRun 的同源 dev-server 能直接提供 page/nativevue2.js。
 * 用法（同一次调用，shared 打包先执行）：
 *   ./gradlew :shared:packLocalJsBundleDebug :h5App:prepareDevBusiness
 */
val prepareDevBusiness by tasks.registering {
    group = "kuikly"
    doLast {
        val zip = Paths.get(
            project.rootDir.absolutePath, businessPathName,
            "build", "outputs", "kuikly", "js", "debug", "local", "$bundleName.zip"
        ).toFile()
        val pageDir = Paths.get(project.buildDir.absolutePath, "processedResources", "js", "main", "page").toFile()
        if (pageDir.exists()) pageDir.deleteRecursively()
        pageDir.mkdirs()
        if (zip.exists()) {
            project.copy {
                from(project.zipTree(zip))
                into(pageDir)
            }
            println("prepared dev bundle -> ${pageDir.absolutePath}")
        } else {
            println("WARN dev bundle not found: ${zip.absolutePath}")
        }
    }
}

/**
 * Release 静态包：shared Release 业务包 + h5App production → build/dist/js/productionExecutable
 * 产物用任意静态服务器托管即可（如 npx http-server -p 8080 .）
 * 用法：先 :shared:packLocalJSBundleRelease，再 :h5App:publishLocalJSBundle
 */
val publishLocalJSBundle by tasks.registering {
    group = "kuikly"
    dependsOn("jsBrowserDistribution")
    doLast {
        val distDir = Paths.get(project.buildDir.absolutePath, "dist", "js", "productionExecutable").toFile()
        distDir.mkdirs()
        // 1) 解压 shared release bundle 到 page/
        val zip = Paths.get(
            project.rootDir.absolutePath, businessPathName,
            "build", "outputs", "kuikly", "js", "release", "local", "$bundleName.zip"
        ).toFile()
        val pageDir = Paths.get(distDir.absolutePath, "page").toFile()
        if (pageDir.exists()) pageDir.deleteRecursively()
        pageDir.mkdirs()
        if (zip.exists()) {
            project.copy {
                from(project.zipTree(zip))
                into(pageDir)
            }
        } else {
            println("WARN release bundle not found: ${zip.absolutePath}")
        }
        // 2) 拷贝业务 assets
        val assetsSrc = Paths.get(project.rootDir.absolutePath, businessPathName, "build", "outputs", "kuikly", "assets").toFile()
        if (assetsSrc.exists()) {
            project.copy {
                from(assetsSrc)
                into(Paths.get(distDir.absolutePath, "assets").toFile())
            }
        }
        println("published H5 static bundle -> ${distDir.absolutePath}")
    }
}
