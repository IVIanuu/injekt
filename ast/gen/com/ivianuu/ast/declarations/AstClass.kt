package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstClassLikeSymbol
import com.ivianuu.ast.symbols.impl.AstClassSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.ClassKind
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstClass<F : AstClass<F>> : AstClassLikeDeclaration<F>, AstDeclarationContainer {
    override val context: AstContext
    override val annotations: List<AstFunctionCall>
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    override val declarations: List<AstDeclaration>
    override val symbol: AstClassSymbol<F>
    val classKind: ClassKind
    val superTypes: List<AstType>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitClass(this, data)

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    override fun replaceDeclarations(newDeclarations: List<AstDeclaration>)

    fun replaceClassKind(newClassKind: ClassKind)

    fun replaceSuperTypes(newSuperTypes: List<AstType>)
}
