package com.ivianuu.injekt.compiler.ast.extension

import com.ivianuu.injekt.compiler.ast.psi.Psi2AstStubGenerator
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstModuleFragment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

interface AstGenerationExtension {
    companion object : ProjectExtensionDescriptor<AstGenerationExtension>(
        "com.ivianuu.injekt.compiler.ast.extension.AstGenerationExtension",
        AstGenerationExtension::class.java
    )

    fun generate(moduleFragment: AstModuleFragment, pluginContext: AstPluginContext)
}

class AstPluginContext(
    private val moduleDescriptor: ModuleDescriptor,
    private val stubGenerator: Psi2AstStubGenerator
) {

    private val classesByFqName = mutableMapOf<FqName, AstClass?>()

    fun referenceClass(fqName: FqName): AstClass? = classesByFqName.getOrPut(fqName) {
        moduleDescriptor.findClassAcrossModuleDependencies(ClassId.fromString(fqName.asString()))
            ?.let { stubGenerator.get(it) as AstClass }
    }

}
