pluginManagement {
    repositories {
        jcenter()
        gradlePluginPortal()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }
    resolutionStrategy {
        eachPlugin {

            fun kotlin(name: String) =
                "org.jetbrains.kotlin.$name"

            when (requested.id.id) {
                kotlin("jvm"), kotlin("multiplatform") ->
                    useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4-M3")
                kotlin("plugin.serialization") ->
                    useModule("org.jetbrains.kotlin:kotlin-serialization:1.4-M3")
            }
        }
    }
}

rootProject.name = "TLN"

include("I", "II", "II:WSD", "II:SemanticEvaluation")

