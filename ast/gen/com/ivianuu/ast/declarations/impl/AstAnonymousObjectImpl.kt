package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstAnonymousObject
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstTypeParameterRef
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.symbols.impl.AstAnonymousObjectSymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import com.ivianuu.ast.visitors.transformInplace
import com.ivianuu.ast.visitors.transformSingle
import org.jetbrains.kotlin.descriptors.ClassKind

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstAnonymousObjectImpl(
    override val origin: AstDeclarationOrigin,
    override val typeParameters: MutableList<AstTypeParameterRef>,
    override val classKind: ClassKind,
    override val superTypeRefs: MutableList<AstTypeRef>,
    override val declarations: MutableList<AstDeclaration>,
    override val annotations: MutableList<AstAnnotationCall>,
    override var typeRef: AstTypeRef,
    override val symbol: AstAnonymousObjectSymbol,
) : AstAnonymousObject() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }
        superTypeRefs.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
        annotations.forEach { it.accept(visitor, data) }
        typeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnonymousObjectImpl {
        transformTypeParameters(transformer, data)
        transformSuperTypeRefs(transformer, data)
        transformDeclarations(transformer, data)
        transformAnnotations(transformer, data)
        typeRef = typeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnonymousObjectImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformSuperTypeRefs(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnonymousObjectImpl {
        superTypeRefs.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformDeclarations(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnonymousObjectImpl {
        declarations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstAnonymousObjectImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceSuperTypeRefs(newSuperTypeRefs: List<AstTypeRef>) {
        superTypeRefs.clear()
        superTypeRefs.addAll(newSuperTypeRefs)
    }

    override fun replaceTypeRef(newTypeRef: AstTypeRef) {
        typeRef = newTypeRef
    }
}
