package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstField
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstFieldImpl(
    override val origin: AstDeclarationOrigin,
    override var returnType: AstType,
    override val name: Name,
    override val symbol: AstVariableSymbol<AstField>,
    override val isVar: Boolean,
    override val annotations: MutableList<AstFunctionCall>,
    override val typeParameters: MutableList<AstTypeParameter>,
) : AstField() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override val receiverType: AstType? get() = null
    override val initializer: AstExpression? get() = null
    override val delegate: AstExpression? get() = null
    override val isVal: Boolean get() = !isVar
    override val getter: AstPropertyAccessor? get() = null
    override val setter: AstPropertyAccessor? get() = null

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        returnType.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstFieldImpl {
        returnType = returnType.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        typeParameters.transformInplace(transformer, data)
        return this
    }
}
