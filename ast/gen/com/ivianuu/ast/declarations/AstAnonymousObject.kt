package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstDelegateInitializer
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstAnonymousObjectSymbol
import com.ivianuu.ast.symbols.impl.AstClassSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.ClassKind
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstAnonymousObject : AstPureAbstractElement(), AstClass<AstAnonymousObject>, AstExpression {
    abstract override val context: AstContext
    abstract override val annotations: List<AstFunctionCall>
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val declarations: List<AstDeclaration>
    abstract override val classKind: ClassKind
    abstract override val superTypes: List<AstType>
    abstract override val delegateInitializers: List<AstDelegateInitializer>
    abstract override val type: AstType
    abstract override val symbol: AstAnonymousObjectSymbol

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitAnonymousObject(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceOrigin(newOrigin: AstDeclarationOrigin)

    abstract override fun replaceAttributes(newAttributes: AstDeclarationAttributes)

    abstract override fun replaceDeclarations(newDeclarations: List<AstDeclaration>)

    abstract override fun replaceClassKind(newClassKind: ClassKind)

    abstract override fun replaceSuperTypes(newSuperTypes: List<AstType>)

    abstract override fun replaceDelegateInitializers(newDelegateInitializers: List<AstDelegateInitializer>)

    abstract override fun replaceType(newType: AstType)
}
