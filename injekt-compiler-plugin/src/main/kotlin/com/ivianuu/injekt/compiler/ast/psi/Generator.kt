package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.ast.extension.AstBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice

interface Generator {

    val context: GeneratorContext

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

    fun KotlinType.toAstType() = context.typeMapper.translate(this)

}

class GeneratorContext(
    val module: ModuleDescriptor,
    val bindingContext: BindingContext,
    val builtIns: AstBuiltIns,
    val kotlinBuiltIns: KotlinBuiltIns,
    val storage: Psi2AstStorage,
    val typeMapper: TypeMapper,
    val astProvider: AstProvider
)
