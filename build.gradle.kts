import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") apply false
}

allprojects {
    group = "com.github.lamba92"
    version = "1.0-SNAPSHOT"

    repositories {
        jcenter()
        maven("https://jitpack.io")
        maven("https://dl.bintray.com/mipt-npm/scientifik")
    }
}

subprojects {
    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}
