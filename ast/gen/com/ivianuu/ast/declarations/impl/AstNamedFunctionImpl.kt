package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstNamedFunction
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstValueParameter
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

open class AstNamedFunctionImpl @AstImplementationDetail constructor(
    override val context: AstContext,
    override val annotations: MutableList<AstFunctionCall>,
    override var origin: AstDeclarationOrigin,
    override var attributes: AstDeclarationAttributes,
    override var dispatchReceiverType: AstType?,
    override var extensionReceiverType: AstType?,
    override var returnType: AstType,
    override val valueParameters: MutableList<AstValueParameter>,
    override var body: AstBlock?,
    override var name: Name,
    override var visibility: Visibility,
    override var modality: Modality,
    override var platformStatus: PlatformStatus,
    override val typeParameters: MutableList<AstTypeParameter>,
    override var isExternal: Boolean,
    override var isSuspend: Boolean,
    override var isOperator: Boolean,
    override var isInfix: Boolean,
    override var isInline: Boolean,
    override var isTailrec: Boolean,
    override val overriddenFunctions: MutableList<AstNamedFunctionSymbol>,
    override var symbol: AstNamedFunctionSymbol,
) : AstNamedFunction() {
    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        dispatchReceiverType?.accept(visitor, data)
        extensionReceiverType?.accept(visitor, data)
        returnType.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstNamedFunctionImpl {
        annotations.transformInplace(transformer, data)
        dispatchReceiverType = dispatchReceiverType?.transformSingle(transformer, data)
        extensionReceiverType = extensionReceiverType?.transformSingle(transformer, data)
        returnType = returnType.transformSingle(transformer, data)
        valueParameters.transformInplace(transformer, data)
        body = body?.transformSingle(transformer, data)
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceOrigin(newOrigin: AstDeclarationOrigin) {
        origin = newOrigin
    }

    override fun replaceAttributes(newAttributes: AstDeclarationAttributes) {
        attributes = newAttributes
    }

    override fun replaceDispatchReceiverType(newDispatchReceiverType: AstType?) {
        dispatchReceiverType = newDispatchReceiverType
    }

    override fun replaceExtensionReceiverType(newExtensionReceiverType: AstType?) {
        extensionReceiverType = newExtensionReceiverType
    }

    override fun replaceReturnType(newReturnType: AstType) {
        returnType = newReturnType
    }

    override fun replaceValueParameters(newValueParameters: List<AstValueParameter>) {
        valueParameters.clear()
        valueParameters.addAll(newValueParameters)
    }

    override fun replaceBody(newBody: AstBlock?) {
        body = newBody
    }

    override fun replaceVisibility(newVisibility: Visibility) {
        visibility = newVisibility
    }

    override fun replaceModality(newModality: Modality) {
        modality = newModality
    }

    override fun replacePlatformStatus(newPlatformStatus: PlatformStatus) {
        platformStatus = newPlatformStatus
    }

    override fun replaceTypeParameters(newTypeParameters: List<AstTypeParameter>) {
        typeParameters.clear()
        typeParameters.addAll(newTypeParameters)
    }

    override fun replaceIsExternal(newIsExternal: Boolean) {
        isExternal = newIsExternal
    }

    override fun replaceIsSuspend(newIsSuspend: Boolean) {
        isSuspend = newIsSuspend
    }

    override fun replaceIsOperator(newIsOperator: Boolean) {
        isOperator = newIsOperator
    }

    override fun replaceIsInfix(newIsInfix: Boolean) {
        isInfix = newIsInfix
    }

    override fun replaceIsInline(newIsInline: Boolean) {
        isInline = newIsInline
    }

    override fun replaceIsTailrec(newIsTailrec: Boolean) {
        isTailrec = newIsTailrec
    }

    override fun replaceOverriddenFunctions(newOverriddenFunctions: List<AstNamedFunctionSymbol>) {
        overriddenFunctions.clear()
        overriddenFunctions.addAll(newOverriddenFunctions)
    }
}
