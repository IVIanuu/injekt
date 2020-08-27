package com.ivianuu.injekt.compiler.ast

import com.ivianuu.injekt.compiler.ast.stub.AstDescriptorStubGenerator
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId

class AstContext(
    private val moduleDescriptor: ModuleDescriptor
) {

    private val classesByFqName = mutableMapOf<String, AstClass?>()
    private val stubGenerator = AstDescriptorStubGenerator()

    fun referenceClass(fqName: String): AstClass? = classesByFqName.getOrPut(fqName) {
        moduleDescriptor.findClassAcrossModuleDependencies(ClassId.fromString(fqName))
            ?.let { stubGenerator.generateClass(it) }
    }

}
