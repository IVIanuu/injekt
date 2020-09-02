package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.Visibility
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

abstract class AstNamedFunction : AstPureAbstractElement(), AstFunction<AstNamedFunction>, AstCallableMemberDeclaration<AstNamedFunction>, AstTypeParametersOwner {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val returnType: AstType
    abstract override val receiverType: AstType?
    abstract override val valueParameters: List<AstValueParameter>
    abstract override val body: AstBlock?
    abstract val name: Name
    abstract val visibility: Visibility
    abstract val isExpect: Boolean
    abstract val isActual: Boolean
    abstract val modality: Modality
    abstract val isExternal: Boolean
    abstract val isSuspend: Boolean
    abstract val isOperator: Boolean
    abstract val isInfix: Boolean
    abstract val isInline: Boolean
    abstract val isTailrec: Boolean
    abstract override val symbol: AstFunctionSymbol<AstNamedFunction>
    abstract override val annotations: List<AstFunctionCall>
    abstract override val typeParameters: List<AstTypeParameter>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitNamedFunction(this, data)
}
