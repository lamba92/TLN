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
    implementation(project(":SemanticResources"))
}
