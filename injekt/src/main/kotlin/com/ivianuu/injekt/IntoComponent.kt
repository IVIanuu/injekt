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

package com.ivianuu.injekt

/**
 * Runs this function on each [ComponentBuilder]
 *
 * Optionally annotate this function with a [Scope] annotation to ensure that this
 * function gets only invoked for [ComponentBuilder]s with a matching scope
 *
 * ```
 * @ActivityScope
 * @IntoComponent
 * private fun ComponentBuilder.myActivityBindings() {
 *     factory { get<MyActivity>().resources }
 * }
 * ```
 *
 */
@Target(AnnotationTarget.FUNCTION)
annotation class IntoComponent(
    /**
     * By default the annotated function will invoked right before the component gets build
     *
     * Setting this flag to true will invoke this function when the builder gets created
     */
    val invokeOnInit: Boolean = false
)
