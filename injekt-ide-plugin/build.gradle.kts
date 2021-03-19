/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij") version "0.6.5"
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-compiler-args.gradle")

intellij {
    version = "2020.3.1"
    pluginName = "Injekt ide plugin"
    updateSinceUntilBuild = false
    setPlugins("org.jetbrains.kotlin:203-1.4.31-release-IJ7148.5", "gradle", "gradle-java", "java")
    localPath = "/home/manu/android-studio"
}

dependencies {
    compile(project(":injekt-compiler-plugin", "shadow"))
    compile(Deps.Moshi.moshi)
    compile(Deps.Moshi.adapters)
    compile(Deps.Moshi.sealedRuntime)
}
