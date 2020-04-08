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

import com.ivianuu.injekt.internal.PropertyName
import com.ivianuu.injekt.internal.SyntheticAnnotationMarker
import com.ivianuu.injekt.internal.propertyName

/**
 *
 * A qualifier can help to distinct between bindings of the same type
 *
 * For example:
 *
 * ´´´
 * val component = Component {
 *     factory<CreditCardHandler>(qualifier = Paypal) { ... }
 *     factory<CreditCardHandler>(qualifier = Amazon) { ... }
 * }
 *
 * val creditCardHandler: CreditCardHandler = if (usePaypal) {
 *     component.get(qualifier = Paypal)
 * } else {
 *     component.get(qualifier = Amazon)
 * }
 * ´´´
 *
 * A qualifier can be declared as follows
 *
 * ´´´
 * @QualifierMarker
 * annotation class UserId {
 *     companion object : Qualifier.Element
 * }
 *
 * Qualifier can be used like this with the annotation api
 *
 * ´´´
 * @Factory
 * class MyViewModel(@UserId private val userId: String)
 * ´´´
 *
 * It's also possible to combine multiple Qualifier
 *
 * ´´´
 * val combinedQualifier = PaypalQualifier + MockedQualifier
 * ´´´
 *
 * @see Component.get
 * @see Factory
 * @see Single
 */
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

    interface Element : Qualifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R =
            operation(initial, this)

        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R =
            operation(this, initial)
    }
}

/**
 * Returns a qualifier which uses [name] for comparisons
 */
fun Qualifier(@PropertyName name: Any = propertyName()): Qualifier = DefaultQualifier(name = name)

/**
 * Annotating a [Qualifier] property allows to use it as an annotation
 */
@SyntheticAnnotationMarker
@Target(AnnotationTarget.PROPERTY)
annotation class QualifierMarker

private data class DefaultQualifier(val name: Any) : Qualifier.Element

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
