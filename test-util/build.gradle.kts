import com.ivianuu.injekt.gradle.*

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
     kotlin("multiplatform")
 }

kotlin {
    jvm {
        withJava()
        compilations.forEach {
            if (it.name == "test") it.withGivenCalls()
            it.kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    sourceSets {
        named("jvmMain") {
            dependencies {
                api(project(":injekt-scope"))
                configurations.getByName("kotlinCompilerPluginClasspath")
                    .dependencies.add(project(":injekt-compiler-plugin"))
                api(project(":injekt-compiler-plugin"))

                api(Deps.AndroidX.Compose.compiler)

                api(Deps.Coroutines.core)
                api(Deps.Coroutines.test)

                api(Deps.Kotlin.compilerEmbeddable)
                api(Deps.kotlinCompileTesting)

                api(Deps.kotestAssertions)

                api(Deps.junit)
                api(Deps.AndroidX.Test.core)
                api(Deps.AndroidX.Test.junit)
                api(Deps.roboelectric)
            }
        }
        named("jvmTest") {
            dependencies {
                implementation(Deps.junit)
                implementation(Deps.kotestAssertions)
            }
        }
    }
}

plugins.apply("com.vanniktech.maven.publish")
