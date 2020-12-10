package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt.compiler.resolution.uniqueTypeName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

fun DeclarationDescriptor.uniqueKey(): String {
    val original = this.original
    return when (original) {
        is ConstructorDescriptor -> "constructor:${original.constructedClass.fqNameSafe}:${
            original.valueParameters
                .joinToString(",") { it.type.toTypeRef().uniqueTypeName().asString() }
        }"
        is ClassDescriptor -> "class:$fqNameSafe"
        is FunctionDescriptor -> "function:$fqNameSafe:${
            listOfNotNull(
                original.dispatchReceiverParameter, original.extensionReceiverParameter)
                .plus(original.valueParameters)
                .joinToString(",") { it.type.toTypeRef().uniqueTypeName().asString() }
        }"
        is PropertyDescriptor -> "property:$fqNameSafe:${
            listOfNotNull(
                original.dispatchReceiverParameter, original.extensionReceiverParameter)
                .joinToString(",") { it.type.toTypeRef().uniqueTypeName().asString() }
        }"
        is ParameterDescriptor -> ""
        is ValueParameterDescriptor -> ""
        else -> error("Unexpected declaration $this")
    }
}
