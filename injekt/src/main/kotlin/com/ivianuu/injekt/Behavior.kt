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
 * Behavior applies scoping or such to [BindingProvider]s
 *
 * @see BoundBehavior
 * @see EagerBehavior
 * @see SingleBehavior
 */
interface Behavior {

    fun <T> apply(provider: BindingProvider<T>): BindingProvider<T>

    fun <R> foldIn(initial: R, operation: (R, Element) -> R): R

    fun <R> foldOut(initial: R, operation: (Element, R) -> R): R

    operator fun plus(other: Behavior): Behavior =
        if (other === None) this else foldOut(other, ::CombinedBehavior)

    object None : Behavior {
        override fun <T> apply(provider: BindingProvider<T>): BindingProvider<T> = provider
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = initial
        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R = initial
        override operator fun plus(other: Behavior): Behavior = other
        override fun toString() = "Behavior.None"
    }

    interface Element : Behavior {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R =
            operation(initial, this)

        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R =
            operation(this, initial)
    }
}

private class CombinedBehavior(
    private val element: Behavior.Element,
    private val wrapped: Behavior
) : Behavior {
    override fun <T> apply(provider: BindingProvider<T>): BindingProvider<T> = provider

    override fun <R> foldIn(initial: R, operation: (R, Behavior.Element) -> R): R =
        wrapped.foldIn(operation(initial, element), operation)

    override fun <R> foldOut(initial: R, operation: (Behavior.Element, R) -> R): R =
        operation(element, wrapped.foldOut(initial, operation))

    override fun equals(other: Any?): Boolean =
        other is CombinedBehavior && element == other.element && wrapped == other.wrapped

    override fun hashCode(): Int = wrapped.hashCode() + 31 * element.hashCode()

    override fun toString() = "[" + foldIn("") { acc, element ->
        if (acc.isEmpty()) element.toString() else "$acc, $element"
    } + "]"
}
