package com.ivianuu.injekt.compiler.ast.extension

import com.ivianuu.injekt.compiler.ast.stub.AstDescriptorStubGenerator
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstModuleFragment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.name.ClassId

interface AstGenerationExtension {
    companion object : ProjectExtensionDescriptor<AstGenerationExtension>(
        "com.ivianuu.injekt.compiler.ast.extension.AstGenerationExtension",
        AstGenerationExtension::class.java
    )

    fun generate(moduleFragment: AstModuleFragment, pluginContext: AstPluginContext)
}

class AstPluginContext(
    private val moduleDescriptor: ModuleDescriptor
) {

    private val classesByFqName = mutableMapOf<String, AstClass?>()
    private val stubGenerator = AstDescriptorStubGenerator()

    fun referenceClass(fqName: String): AstClass? = classesByFqName.getOrPut(fqName) {
        moduleDescriptor.findClassAcrossModuleDependencies(ClassId.fromString(fqName))
            ?.let { stubGenerator.generateClass(it) }
    }

}
