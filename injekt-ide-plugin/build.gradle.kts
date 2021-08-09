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
  id("org.jetbrains.intellij") version "1.1.4"
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-compiler-args.gradle")

intellij {
  version.set("2020.3.1")
  pluginName.set("Injekt ide plugin")
  updateSinceUntilBuild.set(false)
  plugins.addAll("org.jetbrains.kotlin:203-1.5.21-release-328-AS7717.8", "gradle", "gradle-java", "java")
  //localPath.set("/home/manu/android-studio")
}

tasks {
  instrumentCode {
    compilerVersion.set("201.7846.76")
  }
  runIde {
    jbrVersion.set("11_0_3b360.2")
  }
  buildSearchableOptions {
    jbrVersion.set("11_0_3b360.2")
  }
}

/*tasks.withType<PublishTask> {
  token(project.property("ideaToken") as String)
}*/

dependencies {
  api(project(":injekt-compiler-plugin", "shadow"))
  api(Deps.KotlinSerialization.json)
}
