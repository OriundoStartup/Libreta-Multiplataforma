plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.buildkonfig) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

allprojects {
    configurations.all {
        resolutionStrategy {
            eachDependency {
                if (requested.group == "org.jetbrains.kotlin" && 
                    (requested.name.startsWith("kotlin-stdlib") || 
                     requested.name.startsWith("kotlin-reflect") ||
                     requested.name.startsWith("kotlin-test") ||
                     requested.name.startsWith("kotlin-wasm-stdlib-wasm-js"))) {
                    useVersion(libs.versions.kotlin.get())
                }
                if (requested.name == "kotlinx-datetime") {
                    useVersion("0.6.0")
                }
            }
        }
    }
}
