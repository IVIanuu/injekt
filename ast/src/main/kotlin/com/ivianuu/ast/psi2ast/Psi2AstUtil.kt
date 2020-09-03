package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibilities
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities as KotlinVisibilities
import org.jetbrains.kotlin.descriptors.Visibility as KotlinVisibility
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice

interface Generator {

    val context: Psi2AstGeneratorContext

    fun <T : DeclarationDescriptor> KtElement.descriptor(): T =
        getOrFail(BindingContext.DECLARATION_TO_DESCRIPTOR, this).original as T

    fun <K, V : Any> get(slice: ReadOnlySlice<K, V>, key: K): V? =
        context.bindingContext[slice, key]

    fun <K, V : Any> getOrFail(slice: ReadOnlySlice<K, V>, key: K): V =
        context.bindingContext[slice, key] ?: throw RuntimeException("No $slice for $key")

    fun <K, V : Any> getOrFail(slice: ReadOnlySlice<K, V>, key: K, message: (K) -> String): V =
        context.bindingContext[slice, key] ?: throw RuntimeException(message(key))

    fun KtExpression.getTypeInferredByFrontend(): KotlinType? =
        this@Generator.context.bindingContext.getType(this)

    fun KtExpression.getTypeInferredByFrontendOrFail(): KotlinType =
        getTypeInferredByFrontend() ?: throw RuntimeException("No type for expression: $text")

    fun KtExpression.getExpressionTypeWithCoercionToUnit(): KotlinType? =
        if (isUsedAsExpression(this@Generator.context.bindingContext))
            getTypeInferredByFrontend()
        else this@Generator.context.kotlinBuiltIns.unitType

    fun KtExpression.getExpressionTypeWithCoercionToUnitOrFail(): KotlinType =
        getExpressionTypeWithCoercionToUnit()
            ?: throw RuntimeException("No type for expression: $text")

    fun KtElement.getResolvedCall(): ResolvedCall<out CallableDescriptor>? =
        getResolvedCall(this@Generator.context.bindingContext)

    fun KotlinType.toAstType() = context.typeConverter.convert(this)

}

fun KotlinVisibility.toAstVisibility() = when (this) {
    KotlinVisibilities.PUBLIC -> Visibilities.Public
    KotlinVisibilities.INTERNAL -> Visibilities.Internal
    KotlinVisibilities.PROTECTED -> Visibilities.Protected
    KotlinVisibilities.PRIVATE -> Visibilities.Private
    KotlinVisibilities.LOCAL -> Visibilities.Local
    else -> Visibilities.Local
}

fun platformStatusOf(
    isActual: Boolean,
    isExpect: Boolean
) = when {
    isActual -> PlatformStatus.ACTUAL
    isExpect -> PlatformStatus.EXPECT
    else -> PlatformStatus.DEFAULT
}

val UninitializedType = object : AstType {
    override val annotations: List<AstFunctionCall>
        get() = emptyList()
    override val isMarkedNullable: Boolean
        get() = false

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
    }

    override fun replaceIsMarkedNullable(newIsMarkedNullable: Boolean) {
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstElement {
        return this
    }
}
