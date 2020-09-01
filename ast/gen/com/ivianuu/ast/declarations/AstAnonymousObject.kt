package com.ivianuu.ast.declarations

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.symbols.impl.AstAnonymousObjectSymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.descriptors.ClassKind

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstAnonymousObject : AstClass<AstAnonymousObject>, AstExpression() {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val typeParameters: List<AstTypeParameterRef>
    abstract override val classKind: ClassKind
    abstract override val superTypeRefs: List<AstTypeRef>
    abstract override val declarations: List<AstDeclaration>
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val typeRef: AstTypeRef
    abstract override val symbol: AstAnonymousObjectSymbol

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitAnonymousObject(this, data)

    abstract override fun replaceSuperTypeRefs(newSuperTypeRefs: List<AstTypeRef>)

    abstract override fun replaceTypeRef(newTypeRef: AstTypeRef)

    abstract override fun <D> transformTypeParameters(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnonymousObject

    abstract override fun <D> transformSuperTypeRefs(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnonymousObject

    abstract override fun <D> transformDeclarations(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnonymousObject

    abstract override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnonymousObject
}
