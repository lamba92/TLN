plugins {
    kotlin("jvm") version "1.3.61"
}

dependencies {

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.graphstream", "gs-core", "1.3")
    implementation("org.graphstream", "gs-ui", "1.3")
    implementation("com.github.lamba92", "kresourceloader", "1.1.1")

}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs = listOf("-Xinline-classes")
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}