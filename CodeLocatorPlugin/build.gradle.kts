import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.6.20"
    id("org.jetbrains.intellij") version "1.6.0"
}

group = "com.bytedance.tools"
version = "2.0.5"

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.squareup.okhttp3:okhttp:3.14.9")
    implementation("com.hankcs:hanlp:portable-1.8.6")
    implementation("javazoom:jlayer:1.0.1")
    implementation(project(":CodeLocatorModel"))
    implementation("io.reactivex.rxjava3:rxjava:3.1.7")
    implementation("com.google.zxing:core:3.5.2")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2022.1")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf("com.intellij.java", "org.jetbrains.kotlin", "android", "git4idea"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    patchPluginXml {
        sinceBuild.set("213")
        untilBuild.set("999.*")
    }
}

gradle.addListener(object : TaskExecutionListener {
    override fun beforeExecute(task: Task) {
        if (task is Zip && !task.name.contains("CodeLocatorModel")) {
            copyFile(
                File("imgcopy.m"),
                File("build${File.separator}idea-sandbox${File.separator}plugins${File.separator}CodeLocatorPlugin${File.separator}imgcopy.m")
            )
            copyFile(
                File("JarModuleTemplate.zip"),
                File("build${File.separator}idea-sandbox${File.separator}plugins${File.separator}CodeLocatorPlugin${File.separator}JarModuleTemplate.zip")
            )
            copyFile(
                File("AndroidModuleTemplate.zip"),
                File("build${File.separator}idea-sandbox${File.separator}plugins${File.separator}CodeLocatorPlugin${File.separator}AndroidModuleTemplate.zip")
            )
            copyFile(
                File("codelocatorhelper.apk"),
                File("build${File.separator}idea-sandbox${File.separator}plugins${File.separator}CodeLocatorPlugin${File.separator}codelocatorhelper.apk")
            )
            copyFile(
                File("restartAndroidStudio"),
                File("build${File.separator}idea-sandbox${File.separator}plugins${File.separator}CodeLocatorPlugin${File.separator}restartAndroidStudio")
            )
        }
    }

    override fun afterExecute(task: Task, state: TaskState) {
        // 任务执行后逻辑（可选）
    }
})

tasks.named("prepareSandbox") {
    doLast {
        copyFile(
            File("imgcopy.m"),
            File("build${File.separator}idea-sandbox${File.separator}plugins${File.separator}CodeLocatorPlugin${File.separator}imgcopy.m")
        )
        copyFile(
            File("JarModuleTemplate.zip"),
            File("build${File.separator}idea-sandbox${File.separator}plugins${File.separator}CodeLocatorPlugin${File.separator}JarModuleTemplate.zip")
        )
        copyFile(
            File("AndroidModuleTemplate.zip"),
            File("build${File.separator}idea-sandbox${File.separator}plugins${File.separator}CodeLocatorPlugin${File.separator}AndroidModuleTemplate.zip")
        )
        copyFile(
            File("codelocatorhelper.apk"),
            File("build${File.separator}idea-sandbox${File.separator}plugins${File.separator}CodeLocatorPlugin${File.separator}codelocatorhelper.apk")
        )
        copyFile(
            File("restartAndroidStudio"),
            File("build${File.separator}idea-sandbox${File.separator}plugins${File.separator}CodeLocatorPlugin${File.separator}restartAndroidStudio")
        )
    }
}

fun copyFile(sourceFile: File, targetFile: File) {
    var inputStream: InputStream? = null
    var outputStream: OutputStream? = null
    try {
        inputStream = FileInputStream(sourceFile)
        outputStream = FileOutputStream(targetFile)
        var buffer = ByteArray(8192)
        var len = 0
        len = inputStream.read(buffer)
        while (len > 0) {
            outputStream.write(buffer, 0, len)
            len = inputStream.read(buffer)
        }
        inputStream.close()
        outputStream.close()
    } catch (e: Exception) {
        System.out.println("Copy Error " + e)
    }
}