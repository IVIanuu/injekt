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
 * Makes the annotated class injectable and generates a single binding for it
 * The class will be created once per [Component]
 *
 * @see Factory
 * @see Name
 * @see Scope
 * @see InjektConstructor
 * @see ModuleBuilder.single
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
annotation class Single

/**
 * Ensures that a instance is only created once
 * Afterwards a cached value will be returned
 *
 * @see ModuleBuilder.single
 */
fun <T> Binding<T>.asSingle(): Binding<T> = when (this) {
    is LinkedSingleBinding, is UnlinkedSingleBinding -> this
    is LinkedBinding -> LinkedSingleBinding(this)
    else -> UnlinkedSingleBinding(this)
}

private class UnlinkedSingleBinding<T>(private val binding: Binding<T>) : UnlinkedBinding<T>() {
    override fun link(component: Component): LinkedBinding<T> =
        LinkedSingleBinding(binding.performLink(component))
}

private class LinkedSingleBinding<T>(private val provider: Provider<T>) : LinkedBinding<T>() {
    private var _value: Any? = this

    override fun invoke(parameters: Parameters): T {
        var value = _value
        if (value === this) {
            synchronized(this) {
                value = _value
                if (value === this) {
                    _value = provider(parameters)
                    value = _value
                }
            }
        }

        return value as T
    }
}
