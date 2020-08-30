plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {

    val apacheCompressVersion: String by project
    val apacheIoVersion: String by project

    implementation(project(":SemanticResources"))
    implementation("org.apache.commons", "commons-compress", apacheCompressVersion)
    implementation("commons-io", "commons-io", apacheIoVersion)
}
