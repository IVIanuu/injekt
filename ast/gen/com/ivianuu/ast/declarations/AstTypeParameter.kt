package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.AstSymbolOwner
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstTypeParameter : AstPureAbstractElement(), AstTypeParameterRef,
    AstAnnotatedDeclaration, AstSymbolOwner<AstTypeParameter> {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract val name: Name
    abstract override val symbol: AstTypeParameterSymbol
    abstract val variance: Variance
    abstract val isReified: Boolean
    abstract val bounds: List<AstTypeRef>
    abstract override val annotations: List<AstAnnotationCall>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitTypeParameter(this, data)

    abstract fun replaceBounds(newBounds: List<AstTypeRef>)

    abstract override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstTypeParameter
}
