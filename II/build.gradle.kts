plugins {
    kotlin("jvm")
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
}
