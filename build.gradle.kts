fun properties(key: String) = project.findProperty(key).toString()

buildscript {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
        maven { url = uri("https://oss.sonatype.org/content/repositories/releases/") }
    }
}

//2.1 插件配置
// 这两个插件是必备
// 核心的组件使用 简称 例如 java
// 第三方的组件使用全名 
plugins {
    `java-library`
    id("org.jetbrains.intellij") version "1.5.3"
}


group = properties("pluginGroup")
version = properties("pluginVersion")


repositories {
    mavenCentral()
    maven { url = uri("https://plugins.gradle.org/m2/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/releases/") }
}

intellij {
//    插件名称
    pluginName.set(properties("pluginName"))
    // 沙箱目录位置，用于保存IDEA的设置，默认在build文件下面，防止clean，放在根目录下。
    sandboxDir.set("${rootProject.rootDir}/idea-sandbox")
    // 开发环境运行时使用的版本
    version.set("2022.1")
    type.set("IU")
    // 各种版本去这里找
    // https://www.jetbrains.com/intellij-repository/releases
    // 依赖的插件
    plugins.set(listOf("java", "DatabaseTools", "Velocity"))
    //Disables updating since-build attribute in plugin.xml
    //updateSinceUntilBuild false
    //downloadSources = true
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.2.2")
    compileOnly("org.projectlombok:lombok:1.18.22")
    annotationProcessor("org.projectlombok:lombok:1.18.22")
    testImplementation("junit:junit:4.13.1")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.22")
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.compileTestJava{
    options.encoding = "UTF-8"
}