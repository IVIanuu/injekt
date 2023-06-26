/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  kotlin("jvm")
  id("org.jetbrains.intellij") version "1.11.0"
}

//apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-compiler-args.gradle")

intellij {
  pluginName.set("Injekt ide plugin")
  updateSinceUntilBuild.set(false)
  plugins.addAll("org.jetbrains.kotlin", "gradle", "gradle-java", "java")
  localPath.set("/usr/local/bin/android-studio")
}

/*tasks.withType<PublishTask> {
  token(project.property("ideaToken") as String)
}*/

tasks {
  buildSearchableOptions {
    enabled = false
  }
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

dependencies {
  api(project(":injekt-compiler-plugin", "shadow"))
  api(Deps.KotlinSerialization.json)
}
