// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    autowire(libs.plugins.com.android.application) apply false
    autowire(libs.plugins.kotlin.android) apply false
    autowire(libs.plugins.kotlin.ksp) apply false
    autowire(libs.plugins.kotlin.serialization) apply false
    autowire(libs.plugins.kotlin.parcelize) apply false
    autowire(libs.plugins.kotlin.compose.compiler) apply false
    autowire(libs.plugins.hilt.android) apply false
    autowire(libs.plugins.com.squareup.wire) apply false

    autowire(libs.plugins.com.autonomousapps.dependency.analysis)
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
    // 必须连同各子模块的 buildDir 一起删：只删根目录会让 app/build/outputs 下的
    // 历史 APK 残留，被构建脚本的产物收集（find *.apk）扫进来，混进后续版本的产物
    subprojects.forEach { delete(it.buildDir) }
}