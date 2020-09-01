package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstRegularClass : AstPureAbstractElement(), AstMemberDeclaration, AstTypeParameterRefsOwner, AstClass<AstRegularClass> {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val typeParameters: List<AstTypeParameterRef>
    abstract override val status: AstDeclarationStatus
    abstract override val classKind: ClassKind
    abstract override val declarations: List<AstDeclaration>
    abstract val name: Name
    abstract override val symbol: AstRegularClassSymbol
    abstract val companionObject: AstRegularClass?
    abstract val hasLazyNestedClassifiers: Boolean
    abstract override val superTypeRefs: List<AstTypeRef>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitRegularClass(this, data)

    abstract override fun replaceSuperTypeRefs(newSuperTypeRefs: List<AstTypeRef>)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstRegularClass

    abstract override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstRegularClass

    abstract override fun <D> transformStatus(transformer: AstTransformer<D>, data: D): AstRegularClass

    abstract override fun <D> transformDeclarations(transformer: AstTransformer<D>, data: D): AstRegularClass

    abstract fun <D> transformCompanionObject(transformer: AstTransformer<D>, data: D): AstRegularClass

    abstract override fun <D> transformSuperTypeRefs(transformer: AstTransformer<D>, data: D): AstRegularClass
}
