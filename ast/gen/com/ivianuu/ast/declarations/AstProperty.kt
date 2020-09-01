package com.ivianuu.ast.declarations

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.symbols.impl.AstBackingFieldSymbol
import com.ivianuu.ast.symbols.impl.AstDelegateFieldSymbol
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstProperty : AstVariable<AstProperty>(), AstTypeParametersOwner, AstCallableMemberDeclaration<AstProperty> {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val returnType: AstType
    abstract override val receiverType: AstType?
    abstract override val name: Name
    abstract override val initializer: AstExpression?
    abstract override val delegate: AstExpression?
    abstract override val delegateFieldSymbol: AstDelegateFieldSymbol<AstProperty>?
    abstract override val isVar: Boolean
    abstract override val isVal: Boolean
    abstract override val getter: AstPropertyAccessor?
    abstract override val setter: AstPropertyAccessor?
    abstract override val annotations: List<AstAnnotationCall>
    abstract override val typeParameters: List<AstTypeParameter>
    abstract override val symbol: AstPropertySymbol
    abstract val backingFieldSymbol: AstBackingFieldSymbol
    abstract val isLocal: Boolean
    abstract override val status: AstDeclarationStatus

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitProperty(this, data)

    abstract override fun replaceReturnType(newReturnType: AstType)

    abstract override fun replaceReceiverType(newReceiverType: AstType?)

    abstract override fun <D> transformReturnType(transformer: AstTransformer<D>, data: D): AstProperty

    abstract override fun <D> transformReceiverType(transformer: AstTransformer<D>, data: D): AstProperty

    abstract override fun <D> transformInitializer(transformer: AstTransformer<D>, data: D): AstProperty

    abstract override fun <D> transformDelegate(transformer: AstTransformer<D>, data: D): AstProperty

    abstract override fun <D> transformGetter(transformer: AstTransformer<D>, data: D): AstProperty

    abstract override fun <D> transformSetter(transformer: AstTransformer<D>, data: D): AstProperty

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstProperty

    abstract override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstProperty

    abstract override fun <D> transformStatus(transformer: AstTransformer<D>, data: D): AstProperty

    abstract override fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstProperty
}
