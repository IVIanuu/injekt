package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstDelegateInitializer
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstClassSymbol
import com.ivianuu.ast.symbols.impl.AstEnumEntrySymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstEnumEntry : AstPureAbstractElement(), AstClass<AstEnumEntry>, AstNamedDeclaration {
    abstract override val context: AstContext
    abstract override val annotations: List<AstFunctionCall>
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val declarations: List<AstDeclaration>
    abstract override val classKind: ClassKind
    abstract override val superTypes: List<AstType>
    abstract override val delegateInitializers: List<AstDelegateInitializer>
    abstract override val name: Name
    abstract override val symbol: AstEnumEntrySymbol

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitEnumEntry(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceOrigin(newOrigin: AstDeclarationOrigin)

    abstract override fun replaceAttributes(newAttributes: AstDeclarationAttributes)

    abstract override fun replaceDeclarations(newDeclarations: List<AstDeclaration>)

    abstract override fun replaceClassKind(newClassKind: ClassKind)

    abstract override fun replaceSuperTypes(newSuperTypes: List<AstType>)

    abstract override fun replaceDelegateInitializers(newDelegateInitializers: List<AstDelegateInitializer>)
}
