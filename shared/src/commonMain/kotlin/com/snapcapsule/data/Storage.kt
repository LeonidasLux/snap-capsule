package com.snapcapsule.data

/**
 * 平台 JSON 文件读写。同一份 JSON 即「存储 / 导出 / 导入」的统一格式。
 *
 * 平台落点：
 *  - Android：app filesDir/capsules.json（原子写 tmp+rename），需壳工程先注入 AppContext
 *  - JS (H5)：window.localStorage["snap_capsules"]
 *  - iOS / 鸿蒙：随目标启用后补充（见 docs/PLATFORMS.md 的示例实现）
 */
expect object CapsuleFileStorage {
    /** 读取胶囊 JSON；不存在或不可读返回 null。 */
    fun read(): String?

    /** 写入胶囊 JSON；返回是否成功。 */
    fun write(text: String): Boolean
}
