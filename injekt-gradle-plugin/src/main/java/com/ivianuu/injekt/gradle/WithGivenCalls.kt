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

import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetsContainer
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile

fun KotlinSingleTargetExtension.withGivenCalls() {
    target.withGivenCalls()
}

fun KotlinTargetsContainer.withGivenCalls() {
    targets.forEach { it.withGivenCalls() }
}

fun KotlinTarget.withGivenCalls() {
    compilations.forEach { it.withGivenCalls() }
}

fun KotlinCompilation<*>.withGivenCalls() {
    compileKotlinTask.withGivenCalls()
}

fun KotlinCompile<*>.withGivenCalls() {
    kotlinOptions.freeCompilerArgs += listOf(
        "-P", "plugin:com.ivianuu.injekt:allowGivenCalls=true"
    )
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
    (this as AbstractKotlinCompile<*>).incremental = false
}