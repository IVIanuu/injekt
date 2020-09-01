package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstDeclarationStatus
import com.ivianuu.ast.declarations.AstRegularClass
import com.ivianuu.ast.declarations.AstTypeParameterRef
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstRegularClassImpl(
    override val origin: AstDeclarationOrigin,
    override val annotations: MutableList<AstAnnotationCall>,
    override val typeParameters: MutableList<AstTypeParameterRef>,
    override var status: AstDeclarationStatus,
    override val classKind: ClassKind,
    override val declarations: MutableList<AstDeclaration>,
    override val name: Name,
    override val symbol: AstRegularClassSymbol,
    override var companionObject: AstRegularClass?,
    override val superTypes: MutableList<AstType>,
) : AstRegularClass() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override val hasLazyNestedClassifiers: Boolean get() = false

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
        declarations.forEach { it.accept(visitor, data) }
        superTypes.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstRegularClassImpl {
        transformAnnotations(transformer, data)
        transformTypeParameters(transformer, data)
        transformStatus(transformer, data)
        transformDeclarations(transformer, data)
        companionObject = declarations.asSequence().filterIsInstance<AstRegularClass>().firstOrNull { it.status.isCompanion }
        transformSuperTypes(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstRegularClassImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstRegularClassImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: AstTransformer<D>, data: D): AstRegularClassImpl {
        status = status.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformDeclarations(transformer: AstTransformer<D>, data: D): AstRegularClassImpl {
        declarations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformCompanionObject(transformer: AstTransformer<D>, data: D): AstRegularClassImpl {
        companionObject = companionObject?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformSuperTypes(transformer: AstTransformer<D>, data: D): AstRegularClassImpl {
        superTypes.transformInplace(transformer, data)
        return this
    }

    override fun replaceSuperTypes(newSuperTypes: List<AstType>) {
        superTypes.clear()
        superTypes.addAll(newSuperTypes)
    }
}
