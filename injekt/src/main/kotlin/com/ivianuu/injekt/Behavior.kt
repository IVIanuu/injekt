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

import com.ivianuu.injekt.internal.DeclarationName
import com.ivianuu.injekt.internal.SyntheticAnnotationMarker
import com.ivianuu.injekt.internal.declarationName

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
fun Behavior(@DeclarationName name: Any = declarationName()): Behavior = DefaultBehavior(name)

/**
 * Annotating a [Behavior] allows to use it as an annotation
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
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class BehaviorMarker

/**
 * A behavior which allows to do something when ever a [Binding] gets added to a [ComponentBuilder]
 */
class SideEffectBehavior(
    @DeclarationName val name: Any = declarationName(),
    val onBindingAdded: ComponentBuilder.(Binding<*>) -> Unit
) : Behavior.Element {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SideEffectBehavior

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = name.toString()
}

@ModuleMarker
private val SideEffectBehaviorRunnerModule = Module(AnyScope, invokeOnInit = true) {
    onBindingAdded { binding ->
        binding.behavior.foldIn(Unit) { _, element ->
            if (element is SideEffectBehavior) element.onBindingAdded(this, binding)
        }
    }
}

/**
 * A behavior which allows to modify [Binding]s before they get added to a [ComponentBuilder]
 *
 * @see Bound
 * @see Factory
 * @see Single
 *
 */
class InterceptingBehavior(
    @DeclarationName val name: Any = declarationName(),
    val intercept: ComponentBuilder.(Binding<Any?>) -> Binding<Any?>?
) : Behavior.Element {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InterceptingBehavior

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = name.toString()
}

@ModuleMarker
private val InterceptingBehaviorRunnerModule = Module(AnyScope, invokeOnInit = true) {
    bindingInterceptor { binding ->
        binding.behavior.foldIn(binding) { currentBinding: Binding<Any?>?, element ->
            if (currentBinding != null && element is InterceptingBehavior) element.intercept(
                this,
                currentBinding
            )
            else currentBinding
        }
    }
}

private class CombinedBehavior(
    private val element: Behavior.Element,
    private val wrapped: Behavior
) : Behavior {

    override fun contains(behavior: Behavior): Boolean {
        val left = mutableSetOf<Behavior.Element>()
        foldIn(Unit) { _, element -> left += element }
        val right = mutableSetOf<Behavior.Element>()
        behavior.foldIn(Unit) { _, element -> right += element }
        return left.containsAll(right)
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
