plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {

    val apacheCompressVersion: String by project
    val apacheCodecVersion: String by project
    val apacheIoVersion: String by project

    implementation(project(":SemanticResources"))
    implementation("org.apache.commons", "commons-compress", apacheCompressVersion)
    implementation("commons-codec", "commons-codec", apacheCodecVersion)
    implementation("commons-io", "commons-io", apacheIoVersion)
}
