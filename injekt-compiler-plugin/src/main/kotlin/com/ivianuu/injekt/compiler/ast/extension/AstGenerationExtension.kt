package com.ivianuu.injekt.compiler.ast.extension

import com.ivianuu.injekt.compiler.ast.AstGeneratorContext
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstModuleFragment
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor

interface AstGenerationExtension {
    companion object : ProjectExtensionDescriptor<AstGenerationExtension>(
        "com.ivianuu.injekt.compiler.ast.extension.AstGenerationExtension",
        AstGenerationExtension::class.java
    )

    fun generate(moduleFragment: AstModuleFragment, context: AstGeneratorContext)
}
