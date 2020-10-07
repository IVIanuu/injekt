package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.ChildComponent
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

interface Generator {
    fun generate(files: List<KtFile>)
}

@ChildComponent
abstract class GenerationComponent(
    @Binding protected val moduleDescriptor: ModuleDescriptor,
    @Binding protected val bindingContext: BindingContext
) {
    abstract val funBindingGenerator: FunBindingGenerator
    abstract val fileManager: FileManager
    abstract val bindingModuleGenerator: BindingModuleGenerator
    abstract val componentGenerator: ComponentGenerator
    abstract val implBindingGenerator: ImplBindingGenerator
    abstract val indexGenerator: IndexGenerator
}
