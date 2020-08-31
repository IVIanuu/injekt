package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject

class AstProvider {

    lateinit var stubGenerator: Psi2AstStubGenerator
    lateinit var psi2AstVisitor: Psi2AstVisitor
    lateinit var storage: Psi2AstStorage

    fun <T : AstElement> get(descriptor: DeclarationDescriptor): T {
        when (descriptor) {
            is FakeCallableDescriptorForObject -> get<T>(descriptor.classDescriptor)

            is ClassDescriptor -> storage.classes[descriptor.original]
            is FunctionDescriptor -> storage.functions[descriptor.original]
            is PropertyDescriptor -> storage.properties[descriptor.original]
            is TypeParameterDescriptor -> storage.typeParameters[descriptor.original]
            is VariableDescriptor -> storage.valueParameters[descriptor.original]
            is TypeAliasDescriptor -> storage.typeAliases[descriptor.original]
            else -> null
        }?.let { return it as T }

        if (descriptor.findPsi() == null) return stubGenerator.get(descriptor)

        error("No declaration found for $descriptor")
    }

}
