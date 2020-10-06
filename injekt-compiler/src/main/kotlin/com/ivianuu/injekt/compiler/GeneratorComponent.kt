package com.ivianuu.injekt.compiler

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Component

@Component
abstract class GeneratorComponent(
    @Binding protected val codeGenerator: CodeGenerator,
    @Binding protected val resolver: Resolver
) {
    abstract val functionAliasGenerator: FunctionAliasGenerator
    abstract val bindingModuleGenerator: BindingModuleGenerator
    abstract val componentGenerator: ComponentGenerator
    abstract val indexGenerator: IndexGenerator
}
