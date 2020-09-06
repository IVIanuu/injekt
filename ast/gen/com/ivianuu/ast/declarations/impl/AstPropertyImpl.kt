package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstPropertyImpl(
    override val context: AstContext,
    override val annotations: MutableList<AstFunctionCall>,
    override val origin: AstDeclarationOrigin,
    override var dispatchReceiverType: AstType?,
    override var extensionReceiverType: AstType?,
    override var returnType: AstType,
    override var name: Name,
    override var initializer: AstExpression?,
    override var delegate: AstExpression?,
    override var isVar: Boolean,
    override var getter: AstPropertyAccessor?,
    override var setter: AstPropertyAccessor?,
    override val typeParameters: MutableList<AstTypeParameter>,
    override var visibility: Visibility,
    override var modality: Modality,
    override var platformStatus: PlatformStatus,
    override var symbol: AstPropertySymbol,
    override var isLocal: Boolean,
    override var isInline: Boolean,
    override var isConst: Boolean,
    override var isLateinit: Boolean,
    override var isExternal: Boolean,
    override val overriddenProperties: MutableList<AstPropertySymbol>,
) : AstProperty() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()

    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        dispatchReceiverType?.accept(visitor, data)
        extensionReceiverType?.accept(visitor, data)
        returnType.accept(visitor, data)
        initializer?.accept(visitor, data)
        delegate?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstPropertyImpl {
        annotations.transformInplace(transformer, data)
        dispatchReceiverType = dispatchReceiverType?.transformSingle(transformer, data)
        extensionReceiverType = extensionReceiverType?.transformSingle(transformer, data)
        returnType = returnType.transformSingle(transformer, data)
        initializer = initializer?.transformSingle(transformer, data)
        delegate = delegate?.transformSingle(transformer, data)
        getter = getter?.transformSingle(transformer, data)
        setter = setter?.transformSingle(transformer, data)
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
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

    override fun replaceInitializer(newInitializer: AstExpression?) {
        initializer = newInitializer
    }

    override fun replaceDelegate(newDelegate: AstExpression?) {
        delegate = newDelegate
    }

    override fun replaceIsVar(newIsVar: Boolean) {
        isVar = newIsVar
    }

    override fun replaceGetter(newGetter: AstPropertyAccessor?) {
        getter = newGetter
    }

    override fun replaceSetter(newSetter: AstPropertyAccessor?) {
        setter = newSetter
    }

    override fun replaceTypeParameters(newTypeParameters: List<AstTypeParameter>) {
        typeParameters.clear()
        typeParameters.addAll(newTypeParameters)
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

    override fun replaceIsLocal(newIsLocal: Boolean) {
        isLocal = newIsLocal
    }

    override fun replaceIsInline(newIsInline: Boolean) {
        isInline = newIsInline
    }

    override fun replaceIsConst(newIsConst: Boolean) {
        isConst = newIsConst
    }

    override fun replaceIsLateinit(newIsLateinit: Boolean) {
        isLateinit = newIsLateinit
    }

    override fun replaceIsExternal(newIsExternal: Boolean) {
        isExternal = newIsExternal
    }

    override fun replaceOverriddenProperties(newOverriddenProperties: List<AstPropertySymbol>) {
        overriddenProperties.clear()
        overriddenProperties.addAll(newOverriddenProperties)
    }
}
