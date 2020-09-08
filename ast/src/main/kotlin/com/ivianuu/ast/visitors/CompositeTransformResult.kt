package com.ivianuu.ast.visitors

import com.ivianuu.ast.AstElement

sealed class CompositeTransformResult<out T : Any> {

    class Single<out T : Any>(val _element: T) : CompositeTransformResult<T>()

    class Multiple<out T : Any>(val _elements: List<T>) : CompositeTransformResult<T>()

    companion object {
        fun <T : Any> empty() = Multiple(emptyList<T>())
        fun <T : Any> single(t: T) = Single(t)
        fun <T : Any> many(l: List<T>) = Multiple(l)
    }

    @Suppress("UNCHECKED_CAST")
    val elements: List<T>
        get() = when (this) {
            is Multiple<*> -> _elements as List<T>
            is Single<*> -> listOf(_element) as List<T>
        }


    @Suppress("UNCHECKED_CAST")
    val single: T
        get() = when (this) {
            is Multiple<*> -> _elements.single() as T
            is Single<*> -> _element as T
        }

    val isSingle
        get() = this is Single<*> || this.elements.size == 1

    val isEmpty
        get() = this is Multiple<*> && this.elements.isEmpty()
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T : AstElement> T.compose() = CompositeTransformResult.single(this)
