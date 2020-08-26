package com.ivianuu.injekt.compiler.ast

import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult

interface AstTransformer<D> : AstVisitor<AstTransformResult<AstElement>, D> {

    override fun visitElement(element: AstElement, data: D): AstTransformResult<AstElement> =
        transformElement(element, data)

    fun <E : AstElement> transformElement(element: E, data: D): AstTransformResult<E>

    override fun visitFile(declaration: AstFile, data: D): AstTransformResult<AstDeclaration> =
        transformFile(declaration, data)

    fun transformFile(declaration: AstFile, data: D): AstTransformResult<AstDeclaration> =
        transformElement(declaration, data)

    override fun visitClass(declaration: AstClass, data: D): AstTransformResult<AstElement> =
        transformClass(declaration, data)

    fun transformClass(declaration: AstClass, data: D): AstTransformResult<AstDeclaration> =
        transformElement(declaration, data)

}

interface AstTransformerVoid : AstTransformer<Nothing?> {

    override fun <E : AstElement> transformElement(
        element: E,
        data: Nothing?
    ): AstTransformResult<E> = transformElement(element)

    fun <E : AstElement> transformElement(element: AstElement): AstTransformResult<E>

    override fun transformFile(
        declaration: AstFile,
        data: Nothing?
    ): AstTransformResult<AstDeclaration> =
        transformFile(declaration)

    fun transformFile(declaration: AstFile): AstTransformResult<AstDeclaration> =
        transformElement(declaration)

    override fun transformClass(
        declaration: AstClass,
        data: Nothing?
    ): AstTransformResult<AstDeclaration> = transformClass(declaration)

    fun transformClass(declaration: AstClass): AstTransformResult<AstDeclaration> =
        transformElement(declaration)

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


fun <T : AstElement, D> T.transformSingle(transformer: AstTransformer<D>, data: D): T {
    return transform<T, D>(transformer, data).single
}

fun <T : AstElement, D> MutableList<T>.transformInplace(transformer: AstTransformer<D>, data: D) {
    val iterator = this.listIterator()
    while (iterator.hasNext()) {
        val next = iterator.next() as AstElement
        val result = next.transform<T, D>(transformer, data)
        if (result.isSingle) {
            iterator.set(result.single)
        } else {
            val resultIterator = result.list.listIterator()
            if (!resultIterator.hasNext()) {
                iterator.remove()
            } else {
                iterator.set(resultIterator.next())
            }
            while (resultIterator.hasNext()) {
                iterator.add(resultIterator.next())
            }
        }

    }
}
