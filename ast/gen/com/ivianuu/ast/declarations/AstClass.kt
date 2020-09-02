package com.ivianuu.ast.declarations

import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.symbols.impl.AstClassSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.ClassKind
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface AstClass<F : AstClass<F>> : AstClassLikeDeclaration<F>, AstStatement {
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    override val symbol: AstClassSymbol<F>
    val classKind: ClassKind
    val superTypes: List<AstType>
    val declarations: List<AstDeclaration>
    override val annotations: List<AstFunctionCall>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitClass(this, data)
}
