package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstNamedFunction
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

open class AstNamedFunctionImpl @AstImplementationDetail constructor(
    override val origin: AstDeclarationOrigin,
    override var receiverType: AstType?,
    override var returnType: AstType,
    override val valueParameters: MutableList<AstValueParameter>,
    override var body: AstBlock?,
    override val name: Name,
    override val visibility: Visibility,
    override val isExpect: Boolean,
    override val isActual: Boolean,
    override val modality: Modality,
    override val isExternal: Boolean,
    override val isSuspend: Boolean,
    override val isOperator: Boolean,
    override val isInfix: Boolean,
    override val isInline: Boolean,
    override val isTailrec: Boolean,
    override val symbol: AstFunctionSymbol<AstNamedFunction>,
    override val annotations: MutableList<AstFunctionCall>,
    override val typeParameters: MutableList<AstTypeParameter>,
) : AstNamedFunction() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        receiverType?.accept(visitor, data)
        returnType.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstNamedFunctionImpl {
        receiverType = receiverType?.transformSingle(transformer, data)
        returnType = returnType.transformSingle(transformer, data)
        valueParameters.transformInplace(transformer, data)
        body = body?.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        typeParameters.transformInplace(transformer, data)
        return this
    }
}
