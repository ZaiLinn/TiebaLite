pluginManagement {
    repositories {
        // 国内镜像优先 (国际源间歇被掐: 09-16 CI 全红根因), 未命中自动回落官方源
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/google")
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
plugins {
    id("com.highcapable.sweetdependency") version "1.0.4"
    id("com.highcapable.sweetproperty") version "1.0.5"
}
sweetProperty {
    isEnable = true
    global {
        all {
            isEnableTypeAutoConversion = true
            propertiesFileNames(
                "keystore.properties",
                "application.properties",
                isAddDefault = true
            )
            permanentKeyValues(
                "keystore.file" to "",
                "keystore.password" to "",
                "keystore.key.alias" to "",
                "keystore.key.password" to "",
            )
            generateFrom(CURRENT_PROJECT, ROOT_PROJECT)
        }
        buildScript {
            extensionName = "property"
        }
    }
}

rootProject.name = "TiebaLite"
include(":app")
