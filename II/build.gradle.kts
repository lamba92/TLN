plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

kotlin.target {
    compilations.all {
        kotlinOptions.jvmTarget = "1.8"
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api("net.sf.extjwnl", "extjwnl", "2.0.2")
    api("net.sf.extjwnl", "extjwnl-data-wn31", "1.2")
    api("org.nield", "kotlin-statistics", "1.2.1")
    api("io.ktor", "ktor-client-cio", "1.3.2-1.4-M3")
    api("io.ktor", "ktor-client-serialization", "1.3.2-1.4-M3")
    api("io.ktor", "ktor-client-json", "1.3.2-1.4-M3")
    api("io.ktor", "ktor-client-logging", "1.3.2-1.4-M3")
    api("com.github.lamba92", "kresourceloader", "1.1.1")
    api("org.jetbrains.kotlinx", "kotlinx-coroutines-android", "1.3.7-1.4-M3")
    api("org.jetbrains.exposed", "exposed-core", "0.26.1")
    api("org.jetbrains.exposed", "exposed-dao", "0.26.1")
    api("org.jetbrains.exposed", "exposed-jdbc", "0.26.1")
    api("ch.qos.logback", "logback-classic", "1.2.3")
    api("org.xerial", "sqlite-jdbc", "3.32.3.1")
    api("com.h2database", "h2", "1.4.200")
}
