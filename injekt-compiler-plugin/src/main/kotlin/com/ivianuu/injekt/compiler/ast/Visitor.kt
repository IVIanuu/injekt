package com.ivianuu.injekt.compiler.ast

import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult

interface AstTransformer {

    fun transform(element: AstElement): AstTransformResult<AstElement> {
        return when (element) {
            is AstFile -> {
                element.annotations.transformInplace(this)
                element.declarations.transformInplace(this)
                element.compose()
            }
            is AstClass -> {
                element.annotations.transformInplace(this)
                element.declarations.transformInplace(this)
                element.typeParameters.transformInplace(this)
                element.compose()
            }
            is AstSimpleFunction -> {
                element.annotations.transformInplace(this)
                element.typeParameters.transformInplace(this)
                element.valueParameters.transformInplace(this)
                element.compose()
            }
            is AstConstructor -> {
                element.annotations.transformInplace(this)
                element.valueParameters.transformInplace(this)
                element.compose()
            }
            is AstTypeParameter -> {
                element.compose()
            }
            is AstValueParameter -> {
                element.compose()
            }
            is AstCall -> element.compose()
        }
    }

}

sealed class AstTransformResult<out T : AstElement> {
    class Single<out T : AstElement>(val element: T) : AstTransformResult<T>()
    class Multiple<out T : AstElement>(val elements: List<T>) : AstTransformResult<T>()
    companion object {
        fun <T : AstElement> Empty() = Multiple<T>(emptyList())
    }
}

val <T : AstElement> AstTransformResult<T>.list: List<T>
    get() = when (this) {
        is CompositeTransformResult.Multiple<*> -> _list as List<T>
        else -> error("!")
    }


val <T : AstElement> AstTransformResult<T>.single: T
    get() = when (this) {
        is CompositeTransformResult.Single<*> -> _single as T
        else -> error("!")
    }

val <T : AstElement> AstTransformResult<T>.isSingle
    get() = this is CompositeTransformResult.Single<*>

val <T : AstElement> AstTransformResult<T>.isEmpty
    get() = this is CompositeTransformResult.Multiple<*> && this.list.isEmpty()

fun <T : AstElement> T.compose() =
    AstTransformResult.Single(this)

fun <T : AstElement> T.transformSingle(transformer: AstTransformer): T {
    return transformer.transform(this).single as T
}

fun <T : AstElement> MutableList<T>.transformInplace(transformer: AstTransformer) {
    val iterator = listIterator()
    while (iterator.hasNext()) {
        val next = iterator.next()
        val result = transformer.transform(next)
        if (result.isSingle) {
            iterator.set(result.single as T)
        } else {
            val resultIterator = result.list.listIterator()
            if (!resultIterator.hasNext()) {
                iterator.remove()
            } else {
                iterator.set(resultIterator.next() as T)
            }
            while (resultIterator.hasNext()) {
                iterator.add(resultIterator.next() as T)
            }
        }
    }
}
