package com.ivianuu.ast.deepcopy

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.visitors.AstTransformerVoid
import com.ivianuu.ast.visitors.CompositeTransformResult

fun <T : AstElement> T.deepCopy(symbolRemapper: SymbolRemapper = SymbolRemapper().also { accept(it) }): T =
    transform<T>(DeepCopyTransformerImpl(symbolRemapper)).single

abstract class DeepCopyTransformer(protected val symbolRemapper: SymbolRemapper) : AstTransformerVoid() {
    protected fun <T : AstElement> T.transform() =
        transform<T>(this@DeepCopyTransformer).single

    override fun <E : AstElement> transformElement(element: E): CompositeTransformResult<E> {
        error("Unhandled element $element")
    }
}
