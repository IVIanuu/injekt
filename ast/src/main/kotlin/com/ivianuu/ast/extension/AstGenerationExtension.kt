package com.ivianuu.ast.extension

import com.ivianuu.ast.AstGeneratorContext
import com.ivianuu.ast.tree.declaration.AstModuleFragment
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor

interface AstGenerationExtension {
    companion object : ProjectExtensionDescriptor<AstGenerationExtension>(
        "AstGenerationExtension",
        AstGenerationExtension::class.java
    )

    fun generate(moduleFragment: AstModuleFragment, context: AstGeneratorContext)
}
