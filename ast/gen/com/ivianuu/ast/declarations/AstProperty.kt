package com.ivianuu.ast.declarations

import com.ivianuu.ast.Visibility
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstBackingFieldSymbol
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstProperty : AstVariable<AstProperty>(), AstTypeParametersOwner, AstCallableDeclaration<AstProperty> {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val receiverType: AstType?
    abstract override val returnType: AstType
    abstract override val name: Name
    abstract override val initializer: AstExpression?
    abstract override val delegate: AstExpression?
    abstract override val isVar: Boolean
    abstract override val isVal: Boolean
    abstract override val getter: AstPropertyAccessor?
    abstract override val setter: AstPropertyAccessor?
    abstract override val annotations: List<AstFunctionCall>
    abstract override val typeParameters: List<AstTypeParameter>
    abstract override val symbol: AstPropertySymbol
    abstract val backingFieldSymbol: AstBackingFieldSymbol
    abstract val isLocal: Boolean
    abstract val visibility: Visibility
    abstract val isExpect: Boolean
    abstract val isActual: Boolean
    abstract val modality: Modality
    abstract val isInline: Boolean
    abstract val isConst: Boolean
    abstract val isLateinit: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitProperty(this, data)
}
