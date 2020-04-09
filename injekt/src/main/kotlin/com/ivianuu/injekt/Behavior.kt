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
 * Behavior applies scoping or such to [BindingProvider]s
 *
 * @see Bound
 * @see Eager
 * @see Single
 */
interface Behavior {

    operator fun contains(behavior: Behavior): Boolean

    fun <R> foldIn(initial: R, operation: (R, Element) -> R): R

    fun <R> foldOut(initial: R, operation: (Element, R) -> R): R

    operator fun plus(other: Behavior): Behavior =
        if (other === None) this else foldOut(other, ::CombinedBehavior)

    object None : Behavior {
        override fun contains(behavior: Behavior): Boolean = false
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = initial
        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R = initial
        override operator fun plus(other: Behavior): Behavior = other
        override fun toString() = "Behavior.None"
    }

    interface Element : Behavior {

        override fun contains(behavior: Behavior): Boolean = this == behavior

        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R =
            operation(initial, this)

        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R =
            operation(this, initial)
    }
}

/**
 * Returns a [Behavior] which uses [name] for comparisons
 */
fun Behavior(@PropertyName name: Any = propertyName()): Behavior = DefaultBehavior(name)

/**
 * Returns a new [Behavior] for [name] and invokes [onBindingAdded] when ever
 * a [Binding] with the returned [Behavior] was added to a [ComponentBuilder] with [scopes]
 */
fun sideEffectBehavior(
    @PropertyName name: Any = propertyName(),
    scopes: List<Scope> = listOf(AnyScope),
    onBindingAdded: ComponentBuilder.(Binding<*>) -> Unit
): Behavior {
    val behavior = Behavior(name)
    Injekt {
        module(scopes, invokeOnInit = true) {
            onBindingAdded {
                if (behavior in it.behavior) {
                    onBindingAdded(it)
                }
            }
        }
    }
    return behavior
}

/**
 * Returns a new [Behavior] for [name] and invokes [intercept] when ever
 * a binding with the [Behavior] was added to a [ComponentBuilder] with [scopes]
 *
 * @see Bound
 * @see Factory
 * @see Single
 *
 */
fun interceptingBehavior(
    @PropertyName name: Any = propertyName(),
    scopes: List<Scope> = listOf(AnyScope),
    intercept: ComponentBuilder.(Binding<Any?>) -> Binding<Any?>
): Behavior {
    val behavior = Behavior(name)
    Injekt {
        module(scopes, invokeOnInit = true) {
            bindingInterceptor {
                if (behavior in it.behavior) intercept(it) else it
            }
        }
    }
    return behavior
}

/**
 * Annotating a [Behavior] property allows to use it as an annotation
 *
 * For example:
 *
 * ´´´
 * @BehaviorMarker val BindWorker = Behavior()
 *
 * ´´´
 *
 * In dsl:
 *
 * ´´´
 * factory(behavior = BindWorker) { ... }
 *
 * ```
 *
 * And as annotation
 *
 * ´´´
 * @BindWorker
 * class MyWorker
 * ´´´
 *
 */
@SyntheticAnnotationMarker
@Target(AnnotationTarget.PROPERTY)
annotation class BehaviorMarker

private class CombinedBehavior(
    private val element: Behavior.Element,
    private val wrapped: Behavior
) : Behavior {

    override fun contains(behavior: Behavior): Boolean {
        val left = mutableSetOf<Behavior.Element>()
        foldIn(Unit) { _, element -> left += element }
        val right = mutableSetOf<Behavior.Element>()
        behavior.foldIn(Unit) { _, element -> right += element }
        return right.all { it in left }
    }

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

private data class DefaultBehavior(val name: Any) : Behavior.Element {
    override fun toString(): String = name.toString()
}
