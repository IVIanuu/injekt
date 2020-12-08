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

package com.ivianuu.injekt.merge

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class MergeComponent

@Target(AnnotationTarget.CLASS)
annotation class MergeChildComponent

@Target(AnnotationTarget.CLASS)
annotation class MergeInto(val component: KClass<*>)

@Suppress("UNCHECKED_CAST")
fun <T> Any.mergeComponent(): T = this as T

fun <T> Any.get(): T = error("Must be compiled with the injekt compiler.")
