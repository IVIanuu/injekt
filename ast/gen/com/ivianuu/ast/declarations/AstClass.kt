package com.ivianuu.ast.declarations

import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.symbols.impl.AstClassSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.ClassKind
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstClass<F : AstClass<F>> : AstClassLikeDeclaration<F>, AstStatement, AstTypeParameterRefsOwner {
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    override val typeParameters: List<AstTypeParameterRef>
    override val symbol: AstClassSymbol<F>
    val classKind: ClassKind
    val superTypes: List<AstType>
    val declarations: List<AstDeclaration>
    override val annotations: List<AstCall>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitClass(this, data)

    fun replaceSuperTypes(newSuperTypes: List<AstType>)

    override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstClass<F>

    fun <D> transformSuperTypes(transformer: AstTransformer<D>, data: D): AstClass<F>

    fun <D> transformDeclarations(transformer: AstTransformer<D>, data: D): AstClass<F>

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstClass<F>
}
