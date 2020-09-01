package com.ivianuu.ast.declarations

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.symbols.impl.AstAnonymousObjectSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.ClassKind
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstAnonymousObject : AstClass<AstAnonymousObject>, AstExpression() {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val typeParameters: List<AstTypeParameterRef>
    abstract override val classKind: ClassKind
    abstract override val superTypes: List<AstType>
    abstract override val declarations: List<AstDeclaration>
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val type: AstType
    abstract override val symbol: AstAnonymousObjectSymbol

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitAnonymousObject(this, data)

    abstract override fun replaceSuperTypes(newSuperTypes: List<AstType>)

    abstract override fun replaceType(newType: AstType)

    abstract override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstAnonymousObject

    abstract override fun <D> transformSuperTypes(transformer: AstTransformer<D>, data: D): AstAnonymousObject

    abstract override fun <D> transformDeclarations(transformer: AstTransformer<D>, data: D): AstAnonymousObject

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstAnonymousObject
}
