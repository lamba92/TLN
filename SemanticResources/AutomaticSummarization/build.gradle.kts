plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

kotlin.target.compilations.all {
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":SemanticResources"))
    implementation("org.apache.commons", "commons-compress", "1.20")
    implementation("commons-io", "commons-io", "2.7")
}
