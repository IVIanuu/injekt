package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstNamedFunctionSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstNamedFunction : AstFunction<AstNamedFunction>, AstMemberDeclaration(), AstCallableDeclaration<AstNamedFunction>, AstTypeParametersOwner {
    abstract override val context: AstContext
    abstract override val annotations: List<AstFunctionCall>
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val dispatchReceiverType: AstType?
    abstract override val extensionReceiverType: AstType?
    abstract override val returnType: AstType
    abstract override val valueParameters: List<AstValueParameter>
    abstract override val body: AstBlock?
    abstract override val name: Name
    abstract override val visibility: Visibility
    abstract override val modality: Modality
    abstract override val platformStatus: PlatformStatus
    abstract override val typeParameters: List<AstTypeParameter>
    abstract val isExternal: Boolean
    abstract val isSuspend: Boolean
    abstract val isOperator: Boolean
    abstract val isInfix: Boolean
    abstract val isInline: Boolean
    abstract val isTailrec: Boolean
    abstract override val symbol: AstNamedFunctionSymbol

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitNamedFunction(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceDispatchReceiverType(newDispatchReceiverType: AstType?)

    abstract override fun replaceExtensionReceiverType(newExtensionReceiverType: AstType?)

    abstract override fun replaceReturnType(newReturnType: AstType)

    abstract override fun replaceValueParameters(newValueParameters: List<AstValueParameter>)

    abstract override fun replaceBody(newBody: AstBlock?)

    abstract override fun replaceVisibility(newVisibility: Visibility)

    abstract override fun replaceModality(newModality: Modality)

    abstract override fun replacePlatformStatus(newPlatformStatus: PlatformStatus)

    abstract override fun replaceTypeParameters(newTypeParameters: List<AstTypeParameter>)

    abstract fun replaceIsExternal(newIsExternal: Boolean)

    abstract fun replaceIsSuspend(newIsSuspend: Boolean)

    abstract fun replaceIsOperator(newIsOperator: Boolean)

    abstract fun replaceIsInfix(newIsInfix: Boolean)

    abstract fun replaceIsInline(newIsInline: Boolean)

    abstract fun replaceIsTailrec(newIsTailrec: Boolean)
}
