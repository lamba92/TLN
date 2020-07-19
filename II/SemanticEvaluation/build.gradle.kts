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
    implementation(project(":II"))
    implementation("com.github.lamba92", "kresourceloader", "1.1.1")
    implementation("io.ktor", "ktor-client-cio", "1.3.2-1.4-M3")
    implementation("io.ktor", "ktor-client-serialization", "1.3.2-1.4-M3")
    implementation("io.ktor", "ktor-client-json", "1.3.2-1.4-M3")
}
