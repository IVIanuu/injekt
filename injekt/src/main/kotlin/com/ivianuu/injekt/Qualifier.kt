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

interface Qualifier {

    fun <R> foldIn(initial: R, operation: (R, Element) -> R): R

    fun <R> foldOut(initial: R, operation: (Element, R) -> R): R

    operator fun plus(other: Qualifier): Qualifier =
        if (other === None) this else foldOut(other, ::CombinedQualifier)

    object None : Qualifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = initial
        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R = initial
        override operator fun plus(other: Qualifier): Qualifier = other
        override fun toString() = "Qualifier.None"
    }

    // todo remove if possible
    interface Element : Qualifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R =
            operation(initial, this)

        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R =
            operation(this, initial)
    }
}

private class CombinedQualifier(
    private val element: Qualifier.Element,
    private val wrapped: Qualifier
) : Qualifier {
    override fun <R> foldIn(initial: R, operation: (R, Qualifier.Element) -> R): R =
        wrapped.foldIn(operation(initial, element), operation)

    override fun <R> foldOut(initial: R, operation: (Qualifier.Element, R) -> R): R =
        operation(element, wrapped.foldOut(initial, operation))

    override fun equals(other: Any?): Boolean =
        other is CombinedQualifier && element == other.element && wrapped == other.wrapped

    override fun hashCode(): Int = wrapped.hashCode() + 31 * element.hashCode()

    override fun toString() = "[" + foldIn("") { acc, element ->
        if (acc.isEmpty()) element.toString() else "$acc, $element"
    } + "]"
}

/**
 * Marks the annotated class as a qualifier which can be used to differentiate between instances of the same type
 * The annotated class must have an companion object which implements Qualifier
 *
 * ´´´
 * @QualifierMarker
 * annotation class UserId {
 *     companion object : Qualifier.Element
 * }
 * ´´´
 *
 * We can then use the name in the dsl as follows:
 *
 * ´´´
 * factory {
 *     MyViewModel(userId = get(qualifier = UserId))
 * }
 * ´´´
 *
 * And also in @Factory or @Single annotated classes like this:
 *
 * ´´´
 * @Factory
 * class MyViewModel(@UserId private val userId: String)
 * ´´´
 *
 * @see Component.get
 * @see Factory
 * @see Single
 */
@Target(AnnotationTarget.CLASS)
annotation class QualifierMarker
