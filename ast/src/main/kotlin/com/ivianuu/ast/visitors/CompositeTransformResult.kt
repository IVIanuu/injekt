package com.ivianuu.ast.visitors

import com.ivianuu.ast.AstElement

sealed class CompositeTransformResult<out T : Any> {

    class Single<out T : Any>(val _single: T) : CompositeTransformResult<T>()

    class Multiple<out T : Any>(val _list: List<T>) : CompositeTransformResult<T>()

    companion object {
        fun <T : Any> empty() = Multiple(emptyList<T>())
        fun <T : Any> single(t: T) = Single(t)
        fun <T : Any> many(l: List<T>) = Multiple(l)
    }

    @Suppress("UNCHECKED_CAST")
    val list: List<T>
        get() = when (this) {
            is Multiple<*> -> _list as List<T>
            else -> error("!")
        }


    @Suppress("UNCHECKED_CAST")
    val single: T
        get() = when (this) {
            is Single<*> -> _single as T
            else -> error("!")
        }

    val isSingle
        get() = this is Single<*>

    val isEmpty
        get() = this is Multiple<*> && this.list.isEmpty()
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T : AstElement> T.compose() = CompositeTransformResult.single(this)
