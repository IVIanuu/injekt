package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.AstBuiltIns
import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibilities
import com.ivianuu.ast.builder.AstBuilder
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice
import org.jetbrains.kotlin.descriptors.Visibilities as KotlinVisibilities
import org.jetbrains.kotlin.descriptors.Visibility as KotlinVisibility

interface Generator : AstBuilder {

    override val context: Psi2AstGeneratorContext
    val builtIns: AstBuiltIns get() = context.builtIns
    val symbolTable: DescriptorSymbolTable get() = context.symbolTable
    val stubGenerator: DeclarationStubGenerator get() = context.stubGenerator
    val module: ModuleDescriptor get() = context.module

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

    fun KtTypeReference.toAstType() = getOrFail(BindingContext.TYPE, this).toAstType()

}

fun KotlinVisibility.toAstVisibility() = when (this) {
    KotlinVisibilities.PUBLIC -> Visibilities.Public
    KotlinVisibilities.INTERNAL -> Visibilities.Internal
    KotlinVisibilities.PROTECTED -> Visibilities.Protected
    KotlinVisibilities.PRIVATE -> Visibilities.Private
    KotlinVisibilities.LOCAL -> Visibilities.Local
    else -> Visibilities.Local
}

val MemberDescriptor.platformStatus get() = when {
    isActual -> PlatformStatus.ACTUAL
    isExpect -> PlatformStatus.EXPECT
    else -> PlatformStatus.DEFAULT
}
