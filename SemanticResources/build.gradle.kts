plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {

    val extjwnlVersion: String by project
    val extjwnlDataVersion: String by project
    val kotlinStatisticsVersion: String by project
    val ktorVersion: String by project
    val kresourceloaderVersion: String by project
    val coroutinesVersion: String by project
    val exposedVersion: String by project
    val logbackVersion: String by project
    val sqliteVersion: String by project
    val h2databaseVersion: String by project

    api("net.sf.extjwnl", "extjwnl", extjwnlVersion)
    api("net.sf.extjwnl", "extjwnl-data-wn31", extjwnlDataVersion)
    api("org.nield", "kotlin-statistics", kotlinStatisticsVersion)
    api("io.ktor", "ktor-client-cio", ktorVersion)
    api("io.ktor", "ktor-client-serialization", ktorVersion)
    api("io.ktor", "ktor-client-json", ktorVersion)
    api("io.ktor", "ktor-client-logging", ktorVersion)
    api("com.github.lamba92", "kresourceloader", kresourceloaderVersion)
    api("org.jetbrains.kotlinx", "kotlinx-coroutines-core", coroutinesVersion)
    api("org.jetbrains.exposed", "exposed-core", exposedVersion)
    api("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    api("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
    api("ch.qos.logback", "logback-classic", logbackVersion)
    api("org.xerial", "sqlite-jdbc", sqliteVersion)
    api("com.h2database", "h2", h2databaseVersion)
}
