package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.symbols.impl.AstTypeAliasSymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstTypeAlias : AstPureAbstractElement(), AstClassLikeDeclaration<AstTypeAlias>, AstMemberDeclaration, AstTypeParametersOwner {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val status: AstDeclarationStatus
    abstract override val typeParameters: List<AstTypeParameter>
    abstract val name: Name
    abstract override val symbol: AstTypeAliasSymbol
    abstract val expandedTypeRef: AstTypeRef
    abstract override val annotations: List<AstAnnotationCall>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitTypeAlias(this, data)

    abstract fun replaceExpandedTypeRef(newExpandedTypeRef: AstTypeRef)

    abstract override fun <D> transformStatus(transformer: AstTransformer<D>, data: D): AstTypeAlias

    abstract override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstTypeAlias

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstTypeAlias
}
