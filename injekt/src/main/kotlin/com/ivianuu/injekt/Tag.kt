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

import com.ivianuu.injekt.internal.SyntheticAnnotationMarker

/**
 * Tag applies scoping or such to [BindingProvider]s
 *
 * @see Bound
 * @see Eager
 * @see Single
 */
interface Tag {

    operator fun contains(tag: Tag): Boolean

    fun <R> foldIn(initial: R, operation: (R, Element) -> R): R

    fun <R> foldOut(initial: R, operation: (Element, R) -> R): R

    operator fun plus(other: Tag): Tag =
        if (other === None) this else foldOut(other, ::CombinedTag)

    object None : Tag {
        override fun contains(tag: Tag): Boolean = false
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = initial
        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R = initial
        override operator fun plus(other: Tag): Tag = other
        override fun toString() = "Tag.None"
    }

    interface Element : Tag {

        override fun contains(tag: Tag): Boolean = this == tag

        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R =
            operation(initial, this)

        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R =
            operation(this, initial)
    }
}

/**
 * Returns a tag which uses [name] for comparisons
 */
fun Tag(name: Any): Tag = DefaultTag(name)

/**
 * Returns a new [Tag] for [name] and invokes [onBindingAdded] when ever
 * a [Binding] with the returned tag was added to a [ComponentBuilder] with [scope]
 */
fun sideEffectTag(
    name: Any,
    scope: Scope? = null,
    onBindingAdded: ComponentBuilder.(Binding<*>) -> Unit
): Tag {
    val tag = Tag(name)
    Injekt {
        module(scope = scope, invokeOnInit = true) {
            onBindingAdded {
                if (tag in it.tag) {
                    onBindingAdded(it)
                }
            }
        }
    }
    return tag
}

/**
 * Returns a new [Tag] for [name] and invokes [intercept] when ever
 * a binding with the tag was added to a [ComponentBuilder] with [scope]
 *
 * @see Bound
 * @see Factory
 * @see Single
 *
 */
fun interceptingTag(
    name: Any,
    scope: Scope? = null,
    intercept: ComponentBuilder.(Binding<Any?>) -> Binding<Any?>
): Tag {
    val tag = Tag(name)
    Injekt {
        module(scope = scope, invokeOnInit = true) {
            bindingInterceptor {
                if (tag in it.tag) intercept(it) else it
            }
        }
    }
    return tag
}

/**
 * Annotating a [Tag] property allows to use it as an annotation
 *
 * For example:
 *
 * ´´´
 * @TagMarker val BindWorker = Tag()
 *
 * ´´´
 *
 * In dsl:
 *
 * ´´´
 * factory(tag = BindWorker) { ... }
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
@SyntheticAnnotationMarker(Tag::class)
@Target(AnnotationTarget.PROPERTY)
annotation class TagMarker

private class CombinedTag(
    private val element: Tag.Element,
    private val wrapped: Tag
) : Tag {

    override fun contains(tag: Tag): Boolean = tag == element || tag in wrapped

    override fun <R> foldIn(initial: R, operation: (R, Tag.Element) -> R): R =
        wrapped.foldIn(operation(initial, element), operation)

    override fun <R> foldOut(initial: R, operation: (Tag.Element, R) -> R): R =
        operation(element, wrapped.foldOut(initial, operation))

    override fun equals(other: Any?): Boolean =
        other is CombinedTag && element == other.element && wrapped == other.wrapped

    override fun hashCode(): Int = wrapped.hashCode() + 31 * element.hashCode()

    override fun toString() = "[" + foldIn("") { acc, element ->
        if (acc.isEmpty()) element.toString() else "$acc, $element"
    } + "]"
}

private data class DefaultTag(val name: Any) : Tag.Element

