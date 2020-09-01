package com.ivianuu.ast.declarations

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.symbols.impl.AstClassSymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.descriptors.ClassKind

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstClass<F : AstClass<F>> : AstClassLikeDeclaration<F>, AstStatement,
    AstTypeParameterRefsOwner {
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    override val typeParameters: List<AstTypeParameterRef>
    override val symbol: AstClassSymbol<F>
    val classKind: ClassKind
    val superTypeRefs: List<AstTypeRef>
    val declarations: List<AstDeclaration>
    override val annotations: List<AstAnnotationCall>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitClass(this, data)

    fun replaceSuperTypeRefs(newSuperTypeRefs: List<AstTypeRef>)

    override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstClass<F>

    fun <D> transformSuperTypeRefs(transformer: AstTransformer<D>, data: D): AstClass<F>

    fun <D> transformDeclarations(transformer: AstTransformer<D>, data: D): AstClass<F>

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstClass<F>
}
