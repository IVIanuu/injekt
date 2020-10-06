pluginManagement {
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "symbol-processing" ->
                    useModule("com.google.devtools.ksp:symbol-processing:${requested.version}")
            }
        }
    }

    repositories {
        gradlePluginPortal()
        google()
    }
}

include(
    ":android-integration-tests",
    ":injekt-android",
    ":injekt-android-work",
    ":injekt-compiler",
    ":injekt-core",
    ":injekt-merge",
    ":integration-tests",
    ":test-util",
    "samples:android-app",
    "samples:coffee-maker",
    "samples:performance-comparison"
)