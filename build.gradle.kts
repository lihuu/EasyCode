fun properties(key: String) = project.findProperty(key).toString()

//2.1 插件配置
// 这两个插件是必备
// 核心的组件使用 简称 例如 java
// 第三方的组件使用全名 
plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

if (hasProperty("buildScan")) {
    extensions.findByName("buildScan")?.withGroovyBuilder {
        setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
        setProperty("termsOfServiceAgree", "yes")
    }
}

//intellij {
////    插件名称
//    pluginName.set(properties("pluginName"))
//    // 沙箱目录位置，用于保存IDEA的设置，默认在build文件下面，防止clean，放在根目录下。
//    sandboxDir.set("${rootProject.rootDir}/idea-sandbox")
//    // 开发环境运行时使用的版本
//    version.set("2023.1")
//    type.set("IU")
//    // 各种版本去这里找
//    // https://www.jetbrains.com/intellij-repository/releases
//    // 依赖的插件
//    plugins.set(listOf("java"))
//    updateSinceUntilBuild.set(false)
//    //Disables updating since-build attribute in plugin.xml
//    //updateSinceUntilBuild = false
//    //downloadSources = true
//}

intellijPlatform {
    projectName = project.name
    sandboxContainer.set(file("${rootProject.rootDir}/idea-sandbox"))
    pluginConfiguration {
        name = properties("pluginName")

        ideaVersion {
            sinceBuild.set("233")
            untilBuild.set("253")
        }
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2023.3")
        bundledPlugin("com.intellij.java")
    }
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    testImplementation("junit:junit:4.13.2")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.34")
}


tasks {
    compileJava {
        options.encoding = "UTF-8"
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    compileTestJava {
        options.encoding = "UTF-8"
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    wrapper {
        gradleVersion = properties("gradleVersion")
    }
}


tasks.withType<Wrapper> {
    doFirst {
        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2,TLSv1.3")
    }
}


//gradle accept termsOfServiceUrl in build.gradle.kts
//https://stackoverflow.com/questions/67600010/gradle-accept-termsofserviceurl-in-build-gradle-kts

