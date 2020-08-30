plugins {
    kotlin("jvm")
}

dependencies {

    val graphstreamVersion: String by project
    val kresourceloaderVersion: String by project

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.graphstream", "gs-core", graphstreamVersion)
    implementation("org.graphstream", "gs-ui", graphstreamVersion)
    implementation("com.github.lamba92", "kresourceloader", kresourceloaderVersion)

}
