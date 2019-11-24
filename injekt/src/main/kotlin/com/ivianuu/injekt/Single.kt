/*
 * Copyright 2019 Manuel Wrage
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
 * Makes the annotated class injectable by generating a single binding for it
 * @see Factory
 * @see Name
 * @see Scope
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
annotation class Single

/**
 * Used by generated code
 */
interface IsSingle

/**
 * Ensures that a instance is only created once
 * Afterwards a cached value will be returned
 *
 * @see Module.single
 */
fun <T> Binding<T>.asSingle(): Binding<T> = when (this) {
    is LinkedSingleBinding, is UnlinkedSingleBinding -> this
    is LinkedBinding -> LinkedSingleBinding(this)
    else -> UnlinkedSingleBinding(this)
}

private class UnlinkedSingleBinding<T>(private val binding: Binding<T>) : UnlinkedBinding<T>() {
    override fun link(linker: Linker): LinkedBinding<T> =
        LinkedSingleBinding(binding.performLink(linker))
}

private class LinkedSingleBinding<T>(private val binding: LinkedBinding<T>) : LinkedBinding<T>() {
    private var _value: Any? = this

    override fun invoke(parameters: ParametersDefinition?): T {
        var value = _value
        if (value === this) {
            synchronized(this) {
                value = _value
                if (value === this) {
                    _value = binding(parameters)
                    value = _value
                }
            }
        }

        return value as T
    }
}