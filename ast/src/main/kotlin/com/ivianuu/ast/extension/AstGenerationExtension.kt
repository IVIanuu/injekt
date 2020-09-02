package com.ivianuu.ast.extension

import com.ivianuu.ast.declarations.AstModuleFragment
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor

interface AstGenerationExtension {
    companion object : ProjectExtensionDescriptor<AstGenerationExtension>(
        "AstGenerationExtension",
        AstGenerationExtension::class.java
    )

    fun generate(moduleFragment: AstModuleFragment, context: AstGeneratorContext)
}
