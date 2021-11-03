import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

/*
 * Copyright 2021 Manuel Wrage
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
  id("com.github.johnrengelman.shadow")
  id("com.vanniktech.maven.publish")
}

dependencies {
  implementation(project(":injekt-gradle-plugin")) {
    exclude(group = "org.jetbrains.kotlin")
  }
}

val shadowJar = tasks.getByName<ShadowJar>("shadowJar") {
  archiveClassifier.set("")
  relocate("com.ivianuu.injekt", "com.ivianuu.shaded_injekt")
  mergeServiceFiles()
}

artifacts {
  runtimeOnly(shadowJar)
  archives(shadowJar)
}
