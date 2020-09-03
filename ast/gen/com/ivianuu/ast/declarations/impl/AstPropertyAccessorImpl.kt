package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstPropertyAccessorSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstPropertyAccessorImpl(
    override val annotations: MutableList<AstFunctionCall>,
    override val origin: AstDeclarationOrigin,
    override var returnType: AstType,
    override val valueParameters: MutableList<AstValueParameter>,
    override var body: AstBlock?,
    override var name: Name,
    override var visibility: Visibility,
    override var modality: Modality,
    override var symbol: AstPropertyAccessorSymbol,
    override var isSetter: Boolean,
) : AstPropertyAccessor() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override val dispatchReceiverType: AstType? get() = null
    override val extensionReceiverType: AstType? get() = null
    override val platformStatus: PlatformStatus get() = PlatformStatus.DEFAULT
    override val typeParameters: List<AstTypeParameter> get() = emptyList()

    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        returnType.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstPropertyAccessorImpl {
        annotations.transformInplace(transformer, data)
        returnType = returnType.transformSingle(transformer, data)
        valueParameters.transformInplace(transformer, data)
        body = body?.transformSingle(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceDispatchReceiverType(newDispatchReceiverType: AstType?) {}

    override fun replaceExtensionReceiverType(newExtensionReceiverType: AstType?) {}

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

    override fun replacePlatformStatus(newPlatformStatus: PlatformStatus) {}

    override fun replaceTypeParameters(newTypeParameters: List<AstTypeParameter>) {}

    override fun replaceIsSetter(newIsSetter: Boolean) {
        isSetter = newIsSetter
    }
}
