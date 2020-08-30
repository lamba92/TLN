pluginManagement {
    repositories {
        jcenter()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {

            val kotlinVersion: String by settings

            fun kotlin(name: String) =
                "org.jetbrains.kotlin.$name"

            when (requested.id.id) {
                kotlin("jvm"), kotlin("multiplatform") ->
                    useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
                kotlin("plugin.serialization") ->
                    useModule("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
            }
        }
    }
}

rootProject.name = "TLN"

include(
    "LanguageTransfer",
    "SemanticResources",
    "SemanticResources:ConceptualSimilarity",
    "SemanticResources:SemanticEvaluation",
    "SemanticResources:AutomaticSummarization"
)
