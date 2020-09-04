package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.AstSymbolOwner
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstTypeParameter : AstPureAbstractElement(), AstNamedDeclaration, AstSymbolOwner<AstTypeParameter> {
    abstract override val annotations: List<AstFunctionCall>
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val name: Name
    abstract override val symbol: AstTypeParameterSymbol
    abstract val variance: Variance
    abstract val isReified: Boolean
    abstract val bounds: List<AstType>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitTypeParameter(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract fun replaceVariance(newVariance: Variance)

    abstract fun replaceIsReified(newIsReified: Boolean)

    abstract fun replaceBounds(newBounds: List<AstType>)
}
