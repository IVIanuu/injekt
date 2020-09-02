package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstTypeAliasSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstTypeAlias : AstPureAbstractElement(), AstClassLikeDeclaration<AstTypeAlias>, AstDeclaration, AstTypeParametersOwner {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val typeParameters: List<AstTypeParameter>
    abstract val name: Name
    abstract val visibility: Visibility
    abstract val isExpect: Boolean
    abstract val isActual: Boolean
    abstract val modality: Modality
    abstract override val symbol: AstTypeAliasSymbol
    abstract val expandedType: AstType
    abstract override val annotations: List<AstFunctionCall>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitTypeAlias(this, data)
}
