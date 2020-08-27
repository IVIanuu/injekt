package com.ivianuu.injekt.compiler.ast

interface AstTransformer {

    fun transform(element: AstElement): AstTransformResult<AstElement> {
        return when (element) {
            is AstModuleFragment -> {
                element.files = element.files.transformInplace(this)
                element.compose()
            }
            is AstFile -> {
                element.annotations = element.annotations.transformInplace(this)
                element.declarations = element.declarations.transformInplace(this)
                element.compose()
            }
            is AstClass -> {
                element.annotations = element.annotations.transformInplace(this)
                element.declarations = element.declarations.transformInplace(this)
                element.typeParameters = element.typeParameters.transformInplace(this)
                element.compose()
            }
            is AstSimpleFunction -> {
                element.annotations = element.annotations.transformInplace(this)
                element.typeParameters = element.typeParameters.transformInplace(this)
                element.valueParameters = element.valueParameters.transformInplace(this)
                element.compose()
            }
            is AstConstructor -> {
                element.annotations = element.annotations.transformInplace(this)
                element.valueParameters = element.valueParameters.transformInplace(this)
                element.compose()
            }
            is AstTypeParameter -> {
                element.compose()
            }
            is AstValueParameter -> {
                element.compose()
            }
            is AstBody -> {
                element.statements.transformInplace(this)
                element.compose()
            }
            is AstGetValueParameter -> element.compose()
            is AstCall -> element.compose()
        }
    }

}

sealed class AstTransformResult<out T : AstElement> {
    data class Single<out T : AstElement>(val element: T) : AstTransformResult<T>()
    data class Multiple<out T : AstElement>(val elements: List<T>) : AstTransformResult<T>()
    companion object {
        fun <T : AstElement> Empty() = Multiple<T>(emptyList())
    }
}

val <T : AstElement> AstTransformResult<T>.elements: List<T>
    get() = when (this) {
        is AstTransformResult.Multiple<*> -> elements as List<T>
        else -> error("Expected multi result but was $this")
    }


val <T : AstElement> AstTransformResult<T>.element: T
    get() = when (this) {
        is AstTransformResult.Single<*> -> element as T
        else -> error("Expected single result but was $this")
    }

fun <T : AstElement> T.compose() =
    AstTransformResult.Single(this)

fun <T : AstElement> T.transformSingle(transformer: AstTransformer): T {
    return transformer.transform(this).element as T
}

fun <T : AstElement> List<T>.transformInplace(transformer: AstTransformer): List<T> {
    val newList = toMutableList()
    val iterator = newList.listIterator()
    while (iterator.hasNext()) {
        val next = iterator.next()
        val result = transformer.transform(next)
        when (result) {
            is AstTransformResult.Single -> iterator.set(result.element as T)
            is AstTransformResult.Multiple -> {
                val resultIterator = result.elements.listIterator()
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
    return newList
}
