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
 * A binding knows how to create a concrete instance of a type
 * it also holds additional information about the declaration
 * you typically don't access this class directly but instead declare dependencies
 * via a [ModuleBuilder] or annotating classes with [Factory] or [Single]
 *
 * @see Module
 * @see Factory
 * @see Single
 */
abstract class Binding<T>(
    /**
     * Overrides existing bindings with the same key
     */
    val overrideStrategy: OverrideStrategy = OverrideStrategy.Fail,
    /**
     * Creates the instance in the moment the component get's created
     */
    val eager: Boolean = false,
    /**
     * Creates instances in the bound scope
     */
    val scoped: Boolean = false
) {
    /**
     * Returns a [Provider] to retrieve instances of [T]
     *
     * @param component the component which is used to fulfill dependencies
     */
    abstract fun link(component: Component): Provider<T>
}
