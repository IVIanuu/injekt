package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstAnonymousObject
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstTypeParameterRef
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.symbols.impl.AstAnonymousObjectSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.ClassKind
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstAnonymousObjectImpl(
    override val origin: AstDeclarationOrigin,
    override val typeParameters: MutableList<AstTypeParameterRef>,
    override val classKind: ClassKind,
    override val superTypes: MutableList<AstType>,
    override val declarations: MutableList<AstDeclaration>,
    override val annotations: MutableList<AstAnnotationCall>,
    override var type: AstType,
    override val symbol: AstAnonymousObjectSymbol,
) : AstAnonymousObject() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }
        superTypes.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
        annotations.forEach { it.accept(visitor, data) }
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstAnonymousObjectImpl {
        transformTypeParameters(transformer, data)
        transformSuperTypes(transformer, data)
        transformDeclarations(transformer, data)
        transformAnnotations(transformer, data)
        type = type.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstAnonymousObjectImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformSuperTypes(transformer: AstTransformer<D>, data: D): AstAnonymousObjectImpl {
        superTypes.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformDeclarations(transformer: AstTransformer<D>, data: D): AstAnonymousObjectImpl {
        declarations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstAnonymousObjectImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceSuperTypes(newSuperTypes: List<AstType>) {
        superTypes.clear()
        superTypes.addAll(newSuperTypes)
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }
}
