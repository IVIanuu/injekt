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

package com.ivianuu.injekt.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.internal.artifacts.ArtifactAttributes

open class InjektGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val transformed =
            Attribute.of("com.ivianuu.injekt.transformed", Boolean::class.javaObjectType)

        val transformableTypes = listOf(
            "jar", "processed-jar", "aar", "processed-aar"
        )

        project.dependencies.attributesSchema
            .attribute(transformed)

        transformableTypes.forEach {
            project.dependencies.artifactTypes.findByName(it)
                ?.attributes?.attribute(transformed, false)
        }

        project.configurations.all { configuration ->
            project.afterEvaluate {
                configuration.attributes.attribute(transformed, true)
            }
            configuration.dependencies.all { dependency ->
                println("lalala ${configuration.name} -> $dependency")
            }
        }

        transformableTypes.forEach { type ->
            project.dependencies.registerTransform(
                GradleTransform::class.java
            ) {
                it.from
                    .attribute(transformed, false)
                    .attribute(ArtifactAttributes.ARTIFACT_FORMAT, type)
                it.to
                    .attribute(transformed, true)
                    .attribute(ArtifactAttributes.ARTIFACT_FORMAT, type)
            }
        }
    }
}
